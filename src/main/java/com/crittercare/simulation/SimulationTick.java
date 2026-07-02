package com.crittercare.simulation;

import com.crittercare.model.Alert;
import com.crittercare.model.Animal;
import com.crittercare.model.Enclosure;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Immutable snapshot of the zoo state captured at the end of one
 * simulation tick.
 *
 * Why immutable?
 *  The engine hands the same SimulationTick object to every registered
 *  listener.  If a listener could mutate it, it would corrupt the view
 *  seen by subsequent listeners.  List.copyOf() guarantees safety.
 *
 * Listeners receive this object on the JavaFX Application Thread and
 * use it to refresh their TableViews, ProgressBars, and summary cards.
 */
public class SimulationTick {

    private final List<Animal>    animals;
    private final List<Enclosure> enclosures;
    private final List<Alert>     newAlerts;
    private final LocalDateTime   timestamp;
    private final long            tickNumber;

    // ── Constructor ──────────────────────────────────────────────────────────

    /**
     * @param animals     full list of animals after this tick's stat update
     * @param enclosures  full list of enclosures after this tick's stat update
     * @param newAlerts   alerts generated for the first time during this tick
     * @param timestamp   wall-clock time when the tick completed
     * @param tickNumber  monotonically increasing counter (starts at 1)
     */
    public SimulationTick(List<Animal>    animals,
                          List<Enclosure> enclosures,
                          List<Alert>     newAlerts,
                          LocalDateTime   timestamp,
                          long            tickNumber) {
        // Defensive copies — callers cannot modify these lists through the tick
        this.animals     = List.copyOf(animals);
        this.enclosures  = List.copyOf(enclosures);
        this.newAlerts   = List.copyOf(newAlerts);
        this.timestamp   = timestamp;
        this.tickNumber  = tickNumber;
    }

    // ── Convenience summary helpers ──────────────────────────────────────────

    /** Returns the number of animals whose stats require attention this tick. */
    public long getAttentionCount() {
        return animals.stream().filter(Animal::requiresAttention).count();
    }

    /** Returns the number of enclosures that need cleaning this tick. */
    public long getCleaningDueCount() {
        return enclosures.stream().filter(Enclosure::isCleaningDue).count();
    }

    /** Returns true if at least one new alert was generated this tick. */
    public boolean hasNewAlerts() {
        return !newAlerts.isEmpty();
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public List<Animal>    getAnimals()     { return animals; }
    public List<Enclosure> getEnclosures()  { return enclosures; }
    public List<Alert>     getNewAlerts()   { return newAlerts; }
    public LocalDateTime   getTimestamp()   { return timestamp; }
    public long            getTickNumber()  { return tickNumber; }
}
