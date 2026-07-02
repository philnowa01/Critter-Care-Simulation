package com.crittercare.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Represents the in-memory aggregate root for the CritterCare simulation.
 * <p>
 * This class encapsulates the live state of all animals, enclosures, and active alerts
 * within the facility. It provides a unified snapshot that allows the simulation engine
 * to process domain logic efficiently during each cycle. State synchronization with the
 * underlying persistence layer is managed externally by the dedicated service layer.
 * </p>
 */
public class Zoo {

    private String          name;
    private List<Animal>    animals;
    private List<Enclosure> enclosures;
    private List<Alert>     activeAlerts;

    /**
     * Constructs a new Zoo instance with the specified name.
     * Initializes empty collections for the domain entities.
     *
     * @param name the official name of the zoo facility
     */
    public Zoo(String name) {
        this.name         = name;
        this.animals      = new ArrayList<>();
        this.enclosures   = new ArrayList<>();
        this.activeAlerts = new ArrayList<>();
    }

    /**
     * Registers a new animal within the facility's live state.
     *
     * @param animal the animal entity to add
     */
    public void addAnimal(Animal animal) {
        animals.add(animal);
    }

    /**
     * Removes an animal from the facility based on its unique identifier.
     *
     * @param id the unique identifier of the animal
     * @return {@code true} if the animal was found and removed, otherwise {@code false}
     */
    public boolean removeAnimal(int id) {
        return animals.removeIf(a -> a.getId() == id);
    }

    /**
     * Retrieves an animal by its unique identifier.
     *
     * @param id the unique identifier of the animal
     * @return an {@link Optional} containing the animal if found, or empty if not
     */
    public Optional<Animal> getAnimalById(int id) {
        return animals.stream().filter(a -> a.getId() == id).findFirst();
    }

    /**
     * Retrieves a list of all animals currently assigned to a specific enclosure.
     *
     * @param enclosureId the unique identifier of the target enclosure
     * @return a list of animals residing in the specified enclosure
     */
    public List<Animal> getAnimalsInEnclosure(int enclosureId) {
        return animals.stream()
                .filter(a -> a.getEnclosureId() == enclosureId)
                .toList();
    }

    /**
     * Registers a new enclosure within the facility's live state.
     *
     * @param enclosure the enclosure entity to add
     */
    public void addEnclosure(Enclosure enclosure) {
        enclosures.add(enclosure);
    }

    /**
     * Removes an enclosure from the facility based on its unique identifier.
     *
     * @param id the unique identifier of the enclosure
     * @return {@code true} if the enclosure was found and removed, otherwise {@code false}
     */
    public boolean removeEnclosure(int id) {
        return enclosures.removeIf(e -> e.getId() == id);
    }

    /**
     * Retrieves an enclosure by its unique identifier.
     *
     * @param id the unique identifier of the enclosure
     * @return an {@link Optional} containing the enclosure if found, or empty if not
     */
    public Optional<Enclosure> getEnclosureById(int id) {
        return enclosures.stream().filter(e -> e.getId() == id).findFirst();
    }

    /**
     * Appends a new operational alert to the active tracking list.
     *
     * @param alert the alert to register
     */
    public void addAlert(Alert alert) {
        activeAlerts.add(alert);
    }

    /**
     * Marks a specific alert as resolved based on its identifier.
     *
     * @param alertId the unique identifier of the alert to resolve
     */
    public void resolveAlert(int alertId) {
        activeAlerts.stream()
                .filter(a -> a.getId() == alertId)
                .findFirst()
                .ifPresent(Alert::resolve);
    }

    /**
     * Purges all resolved alerts from the active tracking list.
     */
    public void clearResolvedAlerts() {
        activeAlerts.removeIf(Alert::isResolved);
    }

    /**
     * Calculates the total number of animals currently registered in the facility.
     *
     * @return the total animal count
     */
    public int getTotalAnimals() {
        return animals.size();
    }

    /**
     * Calculates the total number of enclosures currently managed by the facility.
     *
     * @return the total enclosure count
     */
    public int getTotalEnclosures() {
        return enclosures.size();
    }

    /**
     * Computes the number of unresolved alerts classified with a critical severity.
     *
     * @return the count of active critical alerts
     */
    public long getCriticalAlertCount() {
        return activeAlerts.stream()
                .filter(a -> !a.isResolved()
                        && a.getSeverity() == AlertSeverity.CRITICAL)
                .count();
    }

    /**
     * Computes the number of animals currently reporting a warning or critical vital state.
     *
     * @return the count of animals requiring attention
     */
    public long getAnimalsRequiringAttentionCount() {
        return animals.stream()
                .filter(Animal::requiresAttention)
                .count();
    }

    /**
     * Computes the number of enclosures where cleanliness has dropped below acceptable thresholds.
     *
     * @return the count of enclosures requiring maintenance
     */
    public long getEnclosuresNeedingCleaningCount() {
        return enclosures.stream()
                .filter(Enclosure::isCleaningDue)
                .count();
    }

    // Getters and Setters

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Retrieves an unmodifiable view of the facility's animals.
     *
     * @return an unmodifiable list of animals
     */
    public List<Animal> getAnimals() {
        return Collections.unmodifiableList(animals);
    }

    /**
     * Replaces the current animal collection with a defensive copy of the provided list.
     *
     * @param animals the new list of animals
     */
    public void setAnimals(List<Animal> animals) {
        this.animals = new ArrayList<>(animals);
    }

    /**
     * Retrieves an unmodifiable view of the facility's enclosures.
     *
     * @return an unmodifiable list of enclosures
     */
    public List<Enclosure> getEnclosures() {
        return Collections.unmodifiableList(enclosures);
    }

    /**
     * Replaces the current enclosure collection with a defensive copy of the provided list.
     *
     * @param enclosures the new list of enclosures
     */
    public void setEnclosures(List<Enclosure> list) {
        this.enclosures = new ArrayList<>(list);
    }

    /**
     * Retrieves an unmodifiable view of the currently active alerts.
     *
     * @return an unmodifiable list of active alerts
     */
    public List<Alert> getActiveAlerts() {
        return Collections.unmodifiableList(activeAlerts);
    }

    /**
     * Replaces the active alerts collection with a defensive copy of the provided list.
     *
     * @param activeAlerts the new list of alerts
     */
    public void setActiveAlerts(List<Alert> alerts) {
        this.activeAlerts = new ArrayList<>(alerts);
    }
}
