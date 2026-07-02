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
 * Orchestrates the generation, storage, and lifecycle management of system alerts.
 * <p>
 * This service continually evaluates the state of domain entities (animals and enclosures)
 * against predefined operational thresholds. It ensures data integrity and limits noise
 * by applying deduplication strategies, preventing the persistence layer from being flooded
 * with duplicate active alerts during continuous simulation cycles.
 * </p>
 */
public class AlertService {

    private static final double HEALTH_CRITICAL_THRESHOLD    = 30.0;
    private static final double HEALTH_HIGH_THRESHOLD        = 60.0;
    private static final double HUNGER_CRITICAL_THRESHOLD    = 75.0;
    private static final double HUNGER_HIGH_THRESHOLD        = 60.0;
    private static final double CLEAN_CRITICAL_THRESHOLD     = 40.0;
    private static final double CLEAN_WARNING_THRESHOLD      = 70.0;

    private final AlertRepository     alertRepo;
    private final AnimalRepository    animalRepo;
    private final EnclosureRepository enclosureRepo;
    private final CopyOnWriteArrayList<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    /**
     * Constructs a new AlertService with the required data access components.
     *
     * @param alertRepo     the repository for alert persistence
     * @param animalRepo    the repository for animal state retrieval
     * @param enclosureRepo the repository for enclosure state retrieval
     */
    public AlertService(AlertRepository alertRepo,
                        AnimalRepository animalRepo,
                        EnclosureRepository enclosureRepo) {
        this.alertRepo     = alertRepo;
        this.animalRepo    = animalRepo;
        this.enclosureRepo = enclosureRepo;
    }

    /**
     * Retrieves a comprehensive list of all alerts, including resolved entries.
     *
     * @return a list of all alerts, ordered with the most recent first
     */
    public List<Alert> getAllAlerts() {
        return alertRepo.findAll();
    }

    /**
     * Retrieves all alerts that are currently in an active, unresolved state.
     *
     * @return a list of active alerts
     */
    public List<Alert> getActiveAlerts() {
        return alertRepo.findActive();
    }

    /**
     * Retrieves all active alerts specifically classified with critical severity.
     *
     * @return a list of active, critical alerts
     */
    public List<Alert> getCriticalAlerts() {
        return alertRepo.findActive().stream()
                .filter(a -> a.getSeverity() == AlertSeverity.CRITICAL)
                .toList();
    }

    /**
     * Calculates the total number of unresolved alerts across the system.
     *
     * @return the count of active alerts
     */
    public long getActiveAlertCount() {
        return alertRepo.findActive().size();
    }

    /**
     * Calculates the total number of unresolved alerts classified with critical severity.
     *
     * @return the count of active, critical alerts
     */
    public long getCriticalAlertCount() {
        return alertRepo.findActive().stream()
                .filter(a -> a.getSeverity() == AlertSeverity.CRITICAL)
                .count();
    }

    /**
     * Transitions a specified alert to a resolved state and synchronizes with the datastore.
     *
     * @param alertId the unique identifier of the target alert
     * @throws IllegalStateException if the specified alert identifier cannot be found
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
     * Acknowledges an active alert, updating its status to indicate staff awareness.
     *
     * @param alertId the unique identifier of the target alert
     * @throws IllegalStateException if the specified alert identifier cannot be found
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
     * Registers a listener to be notified upon any state changes within the alert lifecycle.
     *
     * @param r the {@link Runnable} callback to execute upon changes
     */
    public void addChangeListener(Runnable r) {
        if (r != null) changeListeners.addIfAbsent(r);
    }

    /**
     * Unregisters a previously registered state change listener.
     *
     * @param r the {@link Runnable} callback to remove
     */
    public void removeChangeListener(Runnable r) {
        changeListeners.remove(r);
    }

    /**
     * Triggers the execution of all registered state change listeners.
     */
    private void notifyChangeListeners() {
        for (Runnable r : changeListeners) {
            try { r.run();
            } catch (Exception ignored) {

            }
        }
    }

    /**
     * Evaluates and resolves any active alerts associated with a specific animal if the
     * underlying conditions that triggered the alerts have normalized.
     *
     * @param animalId the unique identifier of the animal entity
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
     * Evaluates and resolves any active alerts associated with a specific enclosure if the
     * underlying conditions that triggered the alerts have normalized.
     *
     * @param enclosureId the unique identifier of the enclosure entity
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

    /**
     * Executes the primary evaluation cycle, scanning all entities for threshold breaches.
     * <p>
     * New alerts are generated for identified breaches, ensuring that duplicate alerts
     * are suppressed if an unresolved alert for the same condition and entity already exists.
     * Inactive alerts are automatically resolved.
     * </p>
     *
     * @return a list of newly generated alert entities, potentially empty
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

    /**
     * Identifies and resolves alerts where the triggering conditions are no longer present.
     */
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

    /**
     * Injects randomized alert events into the system to simulate unpredictable occurrences.
     *
     * @param existingKeys the set of currently active alert deduplication keys
     * @return a list of generated random alerts
     */
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

    /**
     * Determines if a specific alert should remain active based on its source entity's current state.
     *
     * @param alert the alert to evaluate
     * @return {@code true} if the alert remains valid, otherwise {@code false}
     */
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

    /**
     * Evaluates the viability of an alert against explicit entity references.
     *
     * @param alert     the alert to evaluate
     * @param animal    the associated animal, if applicable
     * @param enclosure the associated enclosure, if applicable
     * @return {@code true} if the threshold breach is still occurring, otherwise {@code false}
     */
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

    /**
     * Extracts the numeric identifier from a formatted source string.
     *
     * @param sourceId the formatted string identifier (e.g., "ANM-5")
     * @return the extracted integer, or -1 if parsing fails
     */
    private int parseSourceId(String sourceId) {
        try {
            return Integer.parseInt(sourceId.split("-")[1]);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Evaluates a specific animal entity against operational thresholds and generates requisite alerts.
     *
     * @param animal       the animal entity to analyze
     * @param existingKeys the set of currently active alert keys for deduplication
     * @return a list of newly generated alerts for the animal
     */
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

    /**
     * Evaluates a specific enclosure entity against operational thresholds and generates requisite alerts.
     *
     * @param enclosure    the enclosure entity to analyze
     * @param existingKeys the set of currently active alert keys for deduplication
     * @return a list of newly generated alerts for the enclosure
     */
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
