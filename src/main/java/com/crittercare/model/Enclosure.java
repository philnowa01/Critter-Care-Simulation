package com.crittercare.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Represents a physical habitat zone in the zoo.
 *
 * Enclosure tracks its own cleanliness over time via tick().
 * The list of animal IDs is kept here to support occupancy and
 * compatibility checks without needing Animal objects in memory.
 */
public class Enclosure {

    // Cleanliness falls below this level → Warning alert
    public static final double CLEANLINESS_WARNING_THRESHOLD  = 70.0;
    // Cleanliness falls below this level → Critical alert
    public static final double CLEANLINESS_CRITICAL_THRESHOLD = 40.0;
    // Cleanliness decay rate per simulated hour, per animal
    private static final double DECAY_RATE_PER_ANIMAL_HOUR    = 1.2;

    // ── Fields ───────────────────────────────────────────────────────────────

    private int           id;
    private String        name;
    private HabitatType   habitatType;
    private double        cleanliness;       // 0–100
    private int           capacity;
    private List<Integer> animalIds;
    private LocalDateTime lastCleaned;
    private String        maintenanceSchedule;

    // ── Constructors ─────────────────────────────────────────────────────────

    public Enclosure() {
        this.cleanliness         = 100.0;
        this.capacity            = 10;
        this.animalIds           = new ArrayList<>();
        this.lastCleaned         = LocalDateTime.now();
        this.maintenanceSchedule = "Daily";
    }

    public Enclosure(String name, HabitatType habitatType, int capacity) {
        this();
        this.name        = name;
        this.habitatType = habitatType;
        this.capacity    = capacity;
    }

    // ── Simulation tick ──────────────────────────────────────────────────────

    /**
     * Decreases cleanliness based on occupancy and elapsed simulated hours.
     * More animals → faster degradation.
     *
     * @param deltaHours simulated hours elapsed since last tick
     */
    public void tick(double deltaHours) {
        int occupancy    = animalIds.size();
        double rand      = 0.5 + ThreadLocalRandom.current().nextDouble(); // 0.5–1.5×
        double decayRate = DECAY_RATE_PER_ANIMAL_HOUR * Math.max(1, occupancy) * rand;
        cleanliness      = Math.max(0.0, cleanliness - decayRate * deltaHours);
    }

    // ── Business logic ───────────────────────────────────────────────────────

    /** Returns true if the enclosure has reached its animal capacity. */
    public boolean isFull() {
        return animalIds.size() >= capacity;
    }

    /** Returns true if cleanliness is below the warning threshold. */
    public boolean isCleaningDue() {
        return cleanliness < CLEANLINESS_WARNING_THRESHOLD;
    }

    /** Returns true if cleanliness is critically low. */
    public boolean isCritical() {
        return cleanliness < CLEANLINESS_CRITICAL_THRESHOLD;
    }

    /**
     * Adds an animal to this enclosure.
     * @return false if the enclosure is already full
     */
    public boolean addAnimal(int animalId) {
        if (isFull()) {
            return false;
        }
        animalIds.add(animalId);
        return true;
    }

    /** Removes an animal from this enclosure. */
    public boolean removeAnimal(int animalId) {
        return animalIds.remove(Integer.valueOf(animalId));
    }

    /** Returns the number of animals currently in this enclosure. */
    public int getOccupancy() {
        return animalIds.size();
    }

    /** Resets cleanliness to 100 and records the current time as lastCleaned. */
    public void clean() {
        this.cleanliness = 100.0;
        this.lastCleaned = LocalDateTime.now();
    }

    /** Returns a human-readable status string for the UI. */
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

    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }

    public String getName()                     { return name; }
    public void setName(String name)            { this.name = name; }

    public HabitatType getHabitatType()                         { return habitatType; }
    public void setHabitatType(HabitatType habitatType)         { this.habitatType = habitatType; }

    public double getCleanliness()                              { return cleanliness; }
    public void setCleanliness(double cleanliness)              { this.cleanliness = Math.max(0.0, Math.min(100.0, cleanliness)); }

    public int getCapacity()                    { return capacity; }
    public void setCapacity(int capacity)       { this.capacity = capacity; }

    public List<Integer> getAnimalIds()         { return Collections.unmodifiableList(animalIds); }
    public void setAnimalIds(List<Integer> ids) { this.animalIds = new ArrayList<>(ids); }

    public LocalDateTime getLastCleaned()                       { return lastCleaned; }
    public void setLastCleaned(LocalDateTime lastCleaned)       { this.lastCleaned = lastCleaned; }

    public String getMaintenanceSchedule()                      { return maintenanceSchedule; }
    public void setMaintenanceSchedule(String schedule)         { this.maintenanceSchedule = schedule; }
}
