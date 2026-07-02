package com.crittercare.simulation;

import com.crittercare.model.Alert;
import com.crittercare.model.Animal;
import com.crittercare.model.Enclosure;
import com.crittercare.service.AlertService;
import com.crittercare.service.AnimalService;
import com.crittercare.service.EnclosureService;
import javafx.application.Platform;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Background engine that drives the live zoo simulation.
 *
 * Every TICK_INTERVAL_SECONDS of real time, one tick runs:
 *   1. Load current animal and enclosure state from the DB.
 *   2. Advance each animal's hunger / hydration / health by deltaHours.
 *   3. Advance each enclosure's cleanliness by deltaHours.
 *   4. Persist the updated stats back to the DB.
 *   5. Check for new alerts via AlertService.
 *   6. Build an immutable SimulationTick snapshot.
 *   7. Hand the snapshot to all listeners on the JavaFX thread.
 *
 * Threading model:
 *   - tick() runs on a single background thread (no concurrency between ticks).
 *   - Platform.runLater() hands the snapshot to the JavaFX Application Thread
 *     before notifying listeners — controllers can update UI directly.
 *   - The listener list uses CopyOnWriteArrayList so controllers can
 *     add/remove listeners from the JavaFX thread without locking the engine.
 *
 * Speed multiplier:
 *   deltaHours = (TICK_INTERVAL_SECONDS * speedMultiplier) / 3600
 *   Default multiplier = 720  →  3 real seconds ≈ 36 simulated minutes (0.6 h)
 *   This means stats change visibly during a demo without being instant.
 */
public class SimulationEngine {

    // ── Timing constants ─────────────────────────────────────────────────────

    /** How often a real tick fires (seconds). */
    private static final long TICK_INTERVAL_SECONDS = 3;

    /**
     * Default speed multiplier.
     * 720 × 3s real = 2160s simulated = 36 simulated minutes per tick.
     * At 4 hunger%/hr for a mammal: +2.4% per tick → visible without spiking.
     */
    private static final double DEFAULT_SPEED_MULTIPLIER = 720.0;

    // ── State ────────────────────────────────────────────────────────────────

    private final ScheduledExecutorService       scheduler;
    private final CopyOnWriteArrayList<SimulationListener> listeners;
    private final AnimalService                  animalService;
    private final EnclosureService               enclosureService;
    private final AlertService                   alertService;

    private volatile boolean running       = false;
    private volatile double  speedMultiplier = DEFAULT_SPEED_MULTIPLIER;
    private          long    tickNumber    = 0;
    private          ScheduledFuture<?>    tickFuture;

    // ── Constructor ──────────────────────────────────────────────────────────

    public SimulationEngine(AnimalService    animalService,
                            EnclosureService enclosureService,
                            AlertService     alertService) {
        this.animalService    = animalService;
        this.enclosureService = enclosureService;
        this.alertService     = alertService;
        this.listeners        = new CopyOnWriteArrayList<>();

        // Daemon thread: JVM can exit without waiting for the engine to stop
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread t = new Thread(runnable, "CritterCare-SimEngine");
            t.setDaemon(true);
            return t;
        });
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    /**
     * Starts the simulation.  Safe to call multiple times — subsequent
     * calls are ignored if the engine is already running.
     */
    public void start() {
        if (running) return;
        running = true;
        tickFuture = scheduler.scheduleAtFixedRate(
                this::runTick,
                0,
                TICK_INTERVAL_SECONDS,
                TimeUnit.SECONDS);
        System.out.println("[SimEngine] Started. Tick every "
                + TICK_INTERVAL_SECONDS + "s (×" + speedMultiplier + " sim speed).");
    }

    /**
     * Pauses the simulation (cancels the scheduled future but keeps the
     * executor alive so {@link #start()} or {@link #restart()} can resume).
     */
    public void stop() {
        if (!running) return;
        running = false;
        if (tickFuture != null) {
            tickFuture.cancel(false);
            tickFuture = null;
        }
        System.out.println("[SimEngine] Paused after tick #" + tickNumber + ".");
    }

    /**
     * Stops the engine and resets the tick counter, then immediately
     * starts a fresh schedule.  Used when switching the active zookeeper.
     */
    public void restart() {
        if (running) {
            running = false;
            if (tickFuture != null) {
                tickFuture.cancel(false);
                tickFuture = null;
            }
        }
        tickNumber = 0;
        running    = true;
        tickFuture = scheduler.scheduleAtFixedRate(
                this::runTick, 0, TICK_INTERVAL_SECONDS, TimeUnit.SECONDS);
        System.out.println("[SimEngine] Restarted.");
    }

    /**
     * Permanently terminates the executor.  Call once from
     * {@code MainApp.stop()} — do not call during normal navigation.
     */
    public void shutdown() {
        stop();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("[SimEngine] Shut down after tick #" + tickNumber + ".");
    }

    public boolean isRunning() { return running; }

    // ── Listener management (Observer pattern) ────────────────────────────────

    /** Registers a listener to receive tick notifications. */
    public void addListener(SimulationListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /** Unregisters a listener. */
    public void removeListener(SimulationListener listener) {
        listeners.remove(listener);
    }

    // ── Speed control ─────────────────────────────────────────────────────────

    /**
     * Changes the simulation speed at runtime without restarting.
     * A multiplier of 1.0 means real-time; 3600.0 means 1 hour per second.
     *
     * @param multiplier must be > 0
     */
    public void setSpeedMultiplier(double multiplier) {
        if (multiplier <= 0) {
            throw new IllegalArgumentException("Speed multiplier must be > 0.");
        }
        this.speedMultiplier = multiplier;
    }

    public double getSpeedMultiplier() { return speedMultiplier; }

    // ── Core tick logic ───────────────────────────────────────────────────────

    /**
     * One simulation tick.  Runs on the background scheduler thread.
     * Any unchecked exception here is caught and logged so the scheduler
     * does not silently stop.
     */
    private void runTick() {
        if (!running) return;
        try {
            executeTick();
        } catch (Exception e) {
            System.err.println("[SimEngine] Tick #" + tickNumber
                    + " failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void executeTick() {
        tickNumber++;

        // deltaHours = how many simulated hours this tick represents
        double deltaHours = (TICK_INTERVAL_SECONDS * speedMultiplier) / 3600.0;

        // ── 1. Load current state ──────────────────────────────────────────
        List<Animal>    animals    = animalService.getAllAnimals();
        List<Enclosure> enclosures = enclosureService.getAllEnclosures();

        // ── 2 & 3. Advance stats ───────────────────────────────────────────
        for (Animal animal : animals) {
            animal.tick(deltaHours);
            animalService.updateAnimal(animal);
        }

        for (Enclosure enclosure : enclosures) {
            enclosure.tick(deltaHours);
            enclosureService.updateEnclosure(enclosure);
        }

        // ── 4. Check for new alerts ────────────────────────────────────────
        List<Alert> newAlerts = alertService.checkAndGenerateAlerts();

        // ── 5. Build immutable snapshot ────────────────────────────────────
        SimulationTick tick = new SimulationTick(
                animals, enclosures, newAlerts, LocalDateTime.now(), tickNumber);

        // ── 6. Notify listeners on the JavaFX thread ───────────────────────
        Platform.runLater(() -> {
            // Fire onAlertGenerated for each new alert first
            for (Alert alert : newAlerts) {
                for (SimulationListener listener : listeners) {
                    try {
                        listener.onAlertGenerated(alert);
                    } catch (Exception e) {
                        System.err.println("[SimEngine] Listener error in "
                                + "onAlertGenerated: " + e.getMessage());
                    }
                }
            }
            // Then fire onTick with the full snapshot
            for (SimulationListener listener : listeners) {
                try {
                    listener.onTick(tick);
                } catch (Exception e) {
                    System.err.println("[SimEngine] Listener error in "
                            + "onTick: " + e.getMessage());
                }
            }
        });

        if (tickNumber % 10 == 0) {
            System.out.println("[SimEngine] Tick #" + tickNumber
                    + " | Animals: " + animals.size()
                    + " | New alerts: " + newAlerts.size()
                    + " | deltaHours: " + String.format("%.3f", deltaHours));
        }
    }
}
