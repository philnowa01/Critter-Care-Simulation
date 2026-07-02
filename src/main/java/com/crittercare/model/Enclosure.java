package com.crittercare.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Represents a physical habitat zone within the zoo infrastructure.
 * <p>
 * The enclosure manages its own environmental state, specifically hygiene levels,
 * which degrade over time based on the current animal occupancy. It maintains a
 * lightweight collection of animal identifiers to facilitate capacity management
 * without requiring full entity graphs in memory.
 * </p>
 */
public class Enclosure {

    // Cleanliness falls below this level → Warning alert
    public static final double CLEANLINESS_WARNING_THRESHOLD  = 70.0;
    // Cleanliness falls below this level → Critical alert
    public static final double CLEANLINESS_CRITICAL_THRESHOLD = 40.0;
    // Cleanliness decay rate per simulated hour, per animal
    private static final double DECAY_RATE_PER_ANIMAL_HOUR    = 1.2;

    private int           id;
    private String        name;
    private HabitatType   habitatType;
    private double        cleanliness;       // 0–100
    private int           capacity;
    private List<Integer> animalIds;
    private LocalDateTime lastCleaned;
    private String        maintenanceSchedule;

    /**
     * Default constructor required for framework instantiation.
     * Initializes the enclosure to a pristine state with default capacity.
     */
    public Enclosure() {
        this.cleanliness         = 100.0;
        this.capacity            = 10;
        this.animalIds           = new ArrayList<>();
        this.lastCleaned         = LocalDateTime.now();
        this.maintenanceSchedule = "Daily";
    }

    /**
     * Constructs a new enclosure with the specified core attributes.
     *
     * @param name        the human-readable name of the enclosure
     * @param habitatType the specific biotope or environment type
     * @param capacity    the maximum number of animals the enclosure can safely hold
     */
    public Enclosure(String name, HabitatType habitatType, int capacity) {
        this();
        this.name        = name;
        this.habitatType = habitatType;
        this.capacity    = capacity;
    }

    // ── Simulation tick ──────────────────────────────────────────────────────

    /**
     * Advances the enclosure's simulation state by degrading cleanliness.
     * <p>
     * The degradation rate scales dynamically with the current occupancy of the
     * enclosure and includes a minor randomized variance to simulate real-world conditions.
     * </p>
     *
     * @param deltaHours the simulated time elapsed since the last tick, in hours
     */
    public void tick(double deltaHours) {
        int occupancy    = animalIds.size();
        double rand      = 0.5 + ThreadLocalRandom.current().nextDouble(); // 0.5–1.5×
        double decayRate = DECAY_RATE_PER_ANIMAL_HOUR * Math.max(1, occupancy) * rand;
        cleanliness      = Math.max(0.0, cleanliness - decayRate * deltaHours);
    }

    /**
     * Evaluates whether the enclosure has reached its maximum animal capacity.
     *
     * @return {@code true} if the enclosure is at or beyond capacity, otherwise {@code false}
     */
    public boolean isFull() {
        return animalIds.size() >= capacity;
    }

    /**
     * Evaluates whether the enclosure's cleanliness has dropped below the warning threshold.
     *
     * @return {@code true} if cleaning is recommended, otherwise {@code false}
     */
    public boolean isCleaningDue() {
        return cleanliness < CLEANLINESS_WARNING_THRESHOLD;
    }

    /**
     * Evaluates whether the enclosure's cleanliness has dropped to a critical level.
     *
     * @return {@code true} if cleaning is urgently required, otherwise {@code false}
     */
    public boolean isCritical() {
        return cleanliness < CLEANLINESS_CRITICAL_THRESHOLD;
    }

    /**
     * Registers an animal within this enclosure if capacity permits.
     *
     * @param animalId the unique identifier of the animal
     * @return {@code true} if the animal was successfully added, {@code false} if the enclosure is full
     */
    public boolean addAnimal(int animalId) {
        if (isFull()) {
            return false;
        }
        animalIds.add(animalId);
        return true;
    }

    /**
     * Removes a specific animal from this enclosure.
     *
     * @param animalId the unique identifier of the animal to remove
     * @return {@code true} if the animal was present and removed, otherwise {@code false}
     */
    public boolean removeAnimal(int animalId) {
        return animalIds.remove(Integer.valueOf(animalId));
    }

    /**
     * Retrieves the current number of animals housed in the enclosure.
     *
     * @return the current occupancy count
     */
    public int getOccupancy() {
        return animalIds.size();
    }

    /**
     * Restores the enclosure's cleanliness to optimal levels and updates the maintenance timestamp.
     */
    public void clean() {
        this.cleanliness = 100.0;
        this.lastCleaned = LocalDateTime.now();
    }

    /**
     * Determines the current human-readable status of the enclosure for presentation layers.
     *
     * @return a status string corresponding to the current cleanliness state
     */
    public String getStatus() {
        if (isCritical())     return "Critical";
        if (isCleaningDue())  return "Cleaning Due";
        return "Good";
    }

    @Override
    public String toString() {
        return name + " [" + habitatType + "] – Cleanliness: "
               + String.format("%.0f", cleanliness) + "% | "
               + getOccupancy() + "/" + capacity + " animals";
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name; }

    public HabitatType getHabitatType() { return habitatType;
    }
    public void setHabitatType(HabitatType habitatType) { this.habitatType = habitatType;
    }

    public double getCleanliness() {
        return cleanliness;
    }

    /**
     * Sets the cleanliness level, ensuring it remains within the permissible boundaries.
     *
     * @param cleanliness the requested cleanliness level
     */
    public void setCleanliness(double cleanliness) { this.cleanliness = Math.max(0.0, Math.min(100.0, cleanliness)); }

    public int getCapacity() {
        return capacity;
    }
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    /**
     * Retrieves an unmodifiable view of the current animal identifiers.
     *
     * @return an unmodifiable list of animal IDs
     */
    public List<Integer> getAnimalIds() {
        return Collections.unmodifiableList(animalIds);
    }

    /**
     * Replaces the current list of animal identifiers with the provided collection.
     *
     * @param ids the new collection of animal identifiers
     */
    public void setAnimalIds(List<Integer> ids) {
        this.animalIds = new ArrayList<>(ids);
    }

    public LocalDateTime getLastCleaned() {
        return lastCleaned;
    }
    public void setLastCleaned(LocalDateTime lastCleaned) {
        this.lastCleaned = lastCleaned;
    }

    public String getMaintenanceSchedule() {
        return maintenanceSchedule;
    }
    public void setMaintenanceSchedule(String schedule) {
        this.maintenanceSchedule = schedule;
    }
}
