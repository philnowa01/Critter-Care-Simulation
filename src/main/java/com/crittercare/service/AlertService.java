package com.crittercare.service;

import com.crittercare.model.Alert;
import com.crittercare.model.AlertSeverity;
import com.crittercare.model.AlertType;
import com.crittercare.model.Animal;
import com.crittercare.model.Enclosure;
import com.crittercare.repository.AlertRepository;
import com.crittercare.repository.AnimalRepository;
import com.crittercare.repository.EnclosureRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Generates, stores, and manages system alerts.
 *
 * Alert thresholds (all values are percentages or matching enums):
 *
 *   Animal health    ≤ 30%  → CRITICAL  (LOW_HEALTH)
 *   Animal health    ≤ 60%  → HIGH      (LOW_HEALTH)
 *   Animal hunger    ≥ 75%  → CRITICAL  (CRITICAL_HUNGER)
 *   Animal hunger    ≥ 60%  → HIGH      (CRITICAL_HUNGER)
 *   Enclosure clean  ≤ 40%  → CRITICAL  (DIRTY_ENCLOSURE)
 *   Enclosure clean  ≤ 70%  → WARNING   (DIRTY_ENCLOSURE)
 *   Enclosure full          → WARNING   (OVERCROWDED)
 *
 * Deduplication: before saving a new alert, we check whether an
 * unresolved alert of the same (type, sourceId) already exists in
 * the database.  This prevents the simulation from flooding the table
 * with duplicate entries on every tick.
 */
public class AlertService {

    // ── Thresholds ────────────────────────────────────────────────────────────

    private static final double HEALTH_CRITICAL_THRESHOLD    = 30.0;
    private static final double HEALTH_HIGH_THRESHOLD        = 60.0;
    private static final double HUNGER_CRITICAL_THRESHOLD    = 75.0;
    private static final double HUNGER_HIGH_THRESHOLD        = 60.0;
    private static final double CLEAN_CRITICAL_THRESHOLD     = 40.0;
    private static final double CLEAN_WARNING_THRESHOLD      = 70.0;

    // ── Dependencies ─────────────────────────────────────────────────────────

    private final AlertRepository     alertRepo;
    private final AnimalRepository    animalRepo;
    private final EnclosureRepository enclosureRepo;
    private final CopyOnWriteArrayList<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    public AlertService(AlertRepository alertRepo,
                        AnimalRepository animalRepo,
                        EnclosureRepository enclosureRepo) {
        this.alertRepo     = alertRepo;
        this.animalRepo    = animalRepo;
        this.enclosureRepo = enclosureRepo;
    }

    // ── Queries ──────────────────────────────────────────────────────────────

    /** Returns all alerts (resolved and unresolved), most recent first. */
    public List<Alert> getAllAlerts() {
        return alertRepo.findAll();
    }

    /** Returns only unresolved alerts. */
    public List<Alert> getActiveAlerts() {
        return alertRepo.findActive();
    }

    /** Returns only unresolved CRITICAL alerts. */
    public List<Alert> getCriticalAlerts() {
        return alertRepo.findActive().stream()
                .filter(a -> a.getSeverity() == AlertSeverity.CRITICAL)
                .toList();
    }

    /** Returns the count of unresolved alerts. */
    public long getActiveAlertCount() {
        return alertRepo.findActive().size();
    }

    /** Returns the count of unresolved CRITICAL alerts. */
    public long getCriticalAlertCount() {
        return alertRepo.findActive().stream()
                .filter(a -> a.getSeverity() == AlertSeverity.CRITICAL)
                .count();
    }

    // ── Alert lifecycle ───────────────────────────────────────────────────────

    /**
     * Marks an alert as resolved and persists the change.
     *
     * @throws IllegalStateException if the alert does not exist
     */
    public void resolveAlert(int alertId) {
        Alert alert = alertRepo.findById(alertId)
                .orElseThrow(() -> new IllegalStateException(
                        "Alert id " + alertId + " not found."));
        alert.resolve();
        alertRepo.update(alert);
        notifyChangeListeners();
    }

    /**
     * Acknowledges an alert (New → Acknowledged) and persists the change.
     *
     * @throws IllegalStateException if the alert does not exist
     */
    public void acknowledgeAlert(int alertId) {
        Alert alert = alertRepo.findById(alertId)
                .orElseThrow(() -> new IllegalStateException(
                        "Alert id " + alertId + " not found."));
        alert.acknowledge();
        alertRepo.update(alert);
        notifyChangeListeners();
    }
    
    /**
     * Listener registration for UI components to refresh when alerts change.
     */
    public void addChangeListener(Runnable r) {
        if (r != null) changeListeners.addIfAbsent(r);
    }

    public void removeChangeListener(Runnable r) {
        changeListeners.remove(r);
    }

    private void notifyChangeListeners() {
        for (Runnable r : changeListeners) {
            try { r.run(); } catch (Exception ignored) {}
        }
    }
    /**
     * Resolves any active alerts for the given animal when the underlying
     * condition is no longer present.
     */
    public void resolveAlertsForAnimal(int animalId) {
        if (animalId <= 0) {
            return;
        }
        animalRepo.findById(animalId).ifPresent(animal -> {
            String sourceId = "ANM-" + animalId;
            boolean[] changed = {false};
            alertRepo.findActive().stream()
                    .filter(alert -> sourceId.equals(alert.getSourceId()))
                    .filter(alert -> !isAlertStillActive(alert, animal, null))
                    .forEach(alert -> {
                        alert.resolve();
                        alertRepo.update(alert);
                        changed[0] = true;
                    });
            if (changed[0]) notifyChangeListeners();
        });
    }

    /**
     * Resolves any active alerts for the given enclosure when the underlying
     * condition is no longer present.
     */
    public void resolveAlertsForEnclosure(int enclosureId) {
        if (enclosureId <= 0) {
            return;
        }
        enclosureRepo.findById(enclosureId).ifPresent(enclosure -> {
            String sourceId = "ENC-" + enclosureId;
            boolean[] changed = {false};
            alertRepo.findActive().stream()
                    .filter(alert -> sourceId.equals(alert.getSourceId()))
                    .filter(alert -> !isAlertStillActive(alert, null, enclosure))
                    .forEach(alert -> {
                        alert.resolve();
                        alertRepo.update(alert);
                        changed[0] = true;
                    });
            if (changed[0]) notifyChangeListeners();
        });
    }
    // ── Simulation integration ────────────────────────────────────────────────

    /**
     * Called by SimulationEngine on every tick.
     *
     * Scans all animals and enclosures for threshold breaches and
     * generates new alerts.  Existing unresolved alerts for the same
     * (type, sourceId) are skipped to avoid duplicates.
     *
     * @return list of newly created Alert objects (may be empty)
     */
    public List<Alert> checkAndGenerateAlerts() {
        autoResolveInactiveAlerts();

        // Build a set of keys for already-active alerts to deduplicate
        Set<String> existingKeys = alertRepo.findActive().stream()
                .map(a -> a.getType().name() + "_" + a.getSourceId())
                .collect(Collectors.toSet());

        List<Alert> newAlerts = new ArrayList<>();

        // Check every animal
        for (Animal animal : animalRepo.findAll()) {
            newAlerts.addAll(checkAnimalAlerts(animal, existingKeys));
        }

        // Check every enclosure
        for (Enclosure enclosure : enclosureRepo.findAll()) {
            newAlerts.addAll(checkEnclosureAlerts(enclosure, existingKeys));
        }

        // Add a few random events to keep the simulation lively
        newAlerts.addAll(generateRandomAlerts(existingKeys));

        // Persist each new alert and give it a DB id
        if (!newAlerts.isEmpty()) {
            newAlerts.forEach(alertRepo::save);
            notifyChangeListeners();
        }

        return newAlerts;
    }

    private void autoResolveInactiveAlerts() {
        boolean changed = false;
        for (Alert alert : alertRepo.findActive()) {
            if (!isAlertStillActive(alert)) {
                alert.resolve();
                alertRepo.update(alert);
                changed = true;
            }
        }
        if (changed) notifyChangeListeners();
    }

    private List<Alert> generateRandomAlerts(Set<String> existingKeys) {
        List<Alert> rand = new ArrayList<>();
        double perEntityChance = 0.03; // 3% chance per entity per tick

        for (Animal animal : animalRepo.findAll()) {
            if (Math.random() < perEntityChance) {
                String sourceId = "ANM-" + animal.getId();
                String key = AlertType.ANIMAL_SICK.name() + "_" + sourceId;
                if (!existingKeys.contains(key)) {
                    rand.add(new Alert(
                            AlertType.ANIMAL_SICK, AlertSeverity.HIGH,
                            sourceId, animal.getName() + " (" + animal.getSpecies() + ")",
                            "Sudden illness symptoms observed. Schedule a health check."));
                    existingKeys.add(key);
                }
            }
        }

        for (Enclosure enclosure : enclosureRepo.findAll()) {
            if (Math.random() < perEntityChance) {
                String sourceId = "ENC-" + enclosure.getId();
                String key = AlertType.DIRTY_ENCLOSURE.name() + "_" + sourceId;
                if (!existingKeys.contains(key)) {
                    rand.add(new Alert(
                            AlertType.DIRTY_ENCLOSURE, AlertSeverity.HIGH,
                            sourceId, enclosure.getName(),
                            "Unexpected mess or equipment issue. Cleaning recommended."));
                    existingKeys.add(key);
                }
            }
        }

        return rand;
    }

    private boolean isAlertStillActive(Alert alert) {
        if (alert == null || alert.getType() == null || alert.getSourceId() == null) {
            return true;
        }

        String sourceId = alert.getSourceId();
        if (sourceId.startsWith("ANM-")) {
            int animalId = parseSourceId(sourceId);
            return animalRepo.findById(animalId)
                    .map(animal -> isAlertStillActive(alert, animal, null))
                    .orElse(false);
        }

        if (sourceId.startsWith("ENC-")) {
            int enclosureId = parseSourceId(sourceId);
            return enclosureRepo.findById(enclosureId)
                    .map(enclosure -> isAlertStillActive(alert, null, enclosure))
                    .orElse(false);
        }

        return true;
    }

    private boolean isAlertStillActive(Alert alert, Animal animal,
                                       Enclosure enclosure) {
        return switch (alert.getType()) {
            case LOW_HEALTH -> animal != null && animal.getHealth() <= HEALTH_HIGH_THRESHOLD;
            case CRITICAL_HUNGER -> animal != null && animal.getHunger() >= HUNGER_HIGH_THRESHOLD;
            case DIRTY_ENCLOSURE -> enclosure != null
                    && enclosure.getCleanliness() <= CLEAN_WARNING_THRESHOLD;
            case OVERCROWDED -> enclosure != null && enclosure.isFull();
            default -> true;
        };
    }

    private int parseSourceId(String sourceId) {
        try {
            return Integer.parseInt(sourceId.split("-")[1]);
        } catch (Exception e) {
            return -1;
        }
    }

    // ── Private: per-animal checks ────────────────────────────────────────────

    private List<Alert> checkAnimalAlerts(Animal animal, Set<String> existingKeys) {
        List<Alert> alerts = new ArrayList<>();
        String sourceId   = "ANM-" + animal.getId();
        String sourceName = animal.getName() + " (" + animal.getSpecies() + ")";

        // Health check
        String healthKey = AlertType.LOW_HEALTH.name() + "_" + sourceId;
        if (!existingKeys.contains(healthKey)) {
            if (animal.getHealth() <= HEALTH_CRITICAL_THRESHOLD) {
                alerts.add(new Alert(
                        AlertType.LOW_HEALTH, AlertSeverity.CRITICAL,
                        sourceId, sourceName,
                        "Health has dropped to "
                        + String.format("%.0f", animal.getHealth())
                        + "%. Immediate veterinary attention required."));
                existingKeys.add(healthKey); // prevent double-entry in same tick

            } else if (animal.getHealth() <= HEALTH_HIGH_THRESHOLD) {
                alerts.add(new Alert(
                        AlertType.LOW_HEALTH, AlertSeverity.HIGH,
                        sourceId, sourceName,
                        "Health is at "
                        + String.format("%.0f", animal.getHealth())
                        + "%. Monitor closely and schedule a health check."));
                existingKeys.add(healthKey);
            }
        }

        // Hunger check
        String hungerKey = AlertType.CRITICAL_HUNGER.name() + "_" + sourceId;
        if (!existingKeys.contains(hungerKey)) {
            if (animal.getHunger() >= HUNGER_CRITICAL_THRESHOLD) {
                alerts.add(new Alert(
                        AlertType.CRITICAL_HUNGER, AlertSeverity.CRITICAL,
                        sourceId, sourceName,
                        "Hunger level is at "
                        + String.format("%.0f", animal.getHunger())
                        + "%. Animal requires immediate feeding."));
                existingKeys.add(hungerKey);

            } else if (animal.getHunger() >= HUNGER_HIGH_THRESHOLD) {
                alerts.add(new Alert(
                        AlertType.CRITICAL_HUNGER, AlertSeverity.HIGH,
                        sourceId, sourceName,
                        "Hunger level is at "
                        + String.format("%.0f", animal.getHunger())
                        + "%. Feeding is overdue."));
                existingKeys.add(hungerKey);
            }
        }

        return alerts;
    }

    // ── Private: per-enclosure checks ─────────────────────────────────────────

    private List<Alert> checkEnclosureAlerts(Enclosure enclosure,
                                             Set<String> existingKeys) {
        List<Alert> alerts = new ArrayList<>();
        String sourceId   = "ENC-" + enclosure.getId();
        String sourceName = enclosure.getName();

        // Cleanliness check
        String cleanKey = AlertType.DIRTY_ENCLOSURE.name() + "_" + sourceId;
        if (!existingKeys.contains(cleanKey)) {
            if (enclosure.getCleanliness() <= CLEAN_CRITICAL_THRESHOLD) {
                alerts.add(new Alert(
                        AlertType.DIRTY_ENCLOSURE, AlertSeverity.CRITICAL,
                        sourceId, sourceName,
                        "Cleanliness is critically low at "
                        + String.format("%.0f", enclosure.getCleanliness())
                        + "%. Immediate cleaning required."));
                existingKeys.add(cleanKey);

            } else if (enclosure.getCleanliness() <= CLEAN_WARNING_THRESHOLD) {
                alerts.add(new Alert(
                        AlertType.DIRTY_ENCLOSURE, AlertSeverity.WARNING,
                        sourceId, sourceName,
                        "Cleanliness is at "
                        + String.format("%.0f", enclosure.getCleanliness())
                        + "%. Cleaning is due."));
                existingKeys.add(cleanKey);
            }
        }

        // Overcrowding check
        String crowdKey = AlertType.OVERCROWDED.name() + "_" + sourceId;
        if (!existingKeys.contains(crowdKey) && enclosure.isFull()) {
            alerts.add(new Alert(
                    AlertType.OVERCROWDED, AlertSeverity.WARNING,
                    sourceId, sourceName,
                    "Enclosure has reached maximum capacity of "
                    + enclosure.getCapacity() + " animals. "
                    + "No further assignments are possible."));
            existingKeys.add(crowdKey);
        }

        return alerts;
    }
}
