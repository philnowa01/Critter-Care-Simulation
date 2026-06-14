package com.crittercare.model;

import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Abstract base class for every animal in the zoo.
 *
 * OOP concepts demonstrated:
 *  - Abstraction  : Animal cannot be instantiated directly.
 *  - Inheritance  : Mammal, Bird, Reptile extend this class.
 *  - Polymorphism : Services call animal.tick() without knowing the subtype.
 *  - Template Method pattern : tick() is final; subclasses tune rates via
 *                              protected hook methods.
 *
 * Stat ranges:
 *   health, hunger, hydration, activityLevel → all in [0.0 .. 100.0]
 *   hunger 0 = full, 100 = starving
 */
public abstract class Animal {

    // ── Identity ─────────────────────────────────────────────────────────────

    private int       id;
    private String    name;
    private String    species;
    private int       age;

    // ── Vital stats (0–100) ──────────────────────────────────────────────────

    private double health;         // 100 = perfect health
    private double hunger;         // 0 = full, 100 = starving
    private double hydration;      // 100 = fully hydrated
    private double activityLevel;  // 100 = very active

    // ── Relationships ────────────────────────────────────────────────────────

    private int       enclosureId;
    private String    assignedZookeeper;
    private LocalDate admissionDate;

    // ── Constructors ─────────────────────────────────────────────────────────

    /** No-arg constructor required by repositories when mapping ResultSet rows. */
    public Animal() {
        this.health        = 100.0;
        this.hunger        = 0.0;
        this.hydration     = 100.0;
        this.activityLevel = 80.0;
        this.admissionDate = LocalDate.now();
    }

    /** Convenience constructor for creating a new animal via the UI. */
    public Animal(String name, String species, int age) {
        this();
        this.name    = name;
        this.species = species;
        this.age     = age;
    }

    // ── Template Method: Simulation tick ────────────────────────────────────

    /**
     * Advances this animal's state by {@code deltaHours} simulated hours.
     *
     * Called by SimulationEngine on every tick.  The method is final so the
     * simulation logic is consistent; subclasses customise behaviour through
     * the protected rate methods below.
     *
     * @param deltaHours simulated hours elapsed since the last tick (e.g. 0.5)
     */
    public final void tick(double deltaHours) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        // Hunger rises over time; ±50% random variance per tick
        hunger = clamp(hunger + getHungerRate() * deltaHours * (0.5 + rng.nextDouble()));

        // Hydration falls over time; ±50% random variance per tick
        hydration = clamp(hydration - getHydrationDecayRate() * deltaHours * (0.5 + rng.nextDouble()));

        // Health degrades when the animal is neglected
        if (isHungerCritical() || isDehydrationCritical()) {
            health = clamp(health - getHealthDecayRate() * deltaHours);
        }

        // Activity drops when hungry
        if (hunger > 70.0) {
            activityLevel = clamp(activityLevel - 1.5 * deltaHours);
        }
    }

    // ── Protected hook methods (Template Method hooks) ───────────────────────

    /**
     * Hunger increase per simulated hour (percentage points).
     * Subclasses override to reflect their metabolism.
     */
    protected double getHungerRate() {
        return 3.0;
    }

    /**
     * Hydration decrease per simulated hour (percentage points).
     */
    protected double getHydrationDecayRate() {
        return 2.0;
    }

    /**
     * Health decrease per simulated hour when neglected (percentage points).
     */
    protected double getHealthDecayRate() {
        return 2.0;
    }

    // ── Abstract methods (must be implemented by every subclass) ─────────────

    /** Returns the animal's type string matching the DB column: MAMMAL, BIRD, REPTILE. */
    public abstract String getType();

    /** Returns a short sound string (used for fun in the UI). */
    public abstract String makeSound();

    // ── Business logic helpers ────────────────────────────────────────────────

    /** True if hunger has exceeded the critical threshold (≥ 75%). */
    public boolean isHungerCritical() {
        return hunger >= 75.0;
    }

    /** True if health has fallen below the critical threshold (≤ 30%). */
    public boolean isHealthCritical() {
        return health <= 30.0;
    }

    /** True if hydration has fallen below a dangerous level (≤ 20%). */
    public boolean isDehydrationCritical() {
        return hydration <= 20.0;
    }

    /** True if any vital stat is in a warning state (triggers a non-critical alert). */
    public boolean requiresAttention() {
        return hunger >= 60.0 || health <= 50.0 || hydration <= 30.0;
    }

    // ── Utility ──────────────────────────────────────────────────────────────

    /** Clamps a value to the range [0.0, 100.0]. */
    private double clamp(double value) {
        return Math.max(0.0, Math.min(100.0, value));
    }

    @Override
    public String toString() {
        return name + " (" + species + ") – Health: " + String.format("%.0f", health) + "%";
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }

    public String getName()                     { return name; }
    public void setName(String name)            { this.name = name; }

    public String getSpecies()                  { return species; }
    public void setSpecies(String species)      { this.species = species; }

    public int getAge()                         { return age; }
    public void setAge(int age)                 { this.age = age; }

    public double getHealth()                   { return health; }
    public void setHealth(double health)        { this.health = clamp(health); }

    public double getHunger()                   { return hunger; }
    public void setHunger(double hunger)        { this.hunger = clamp(hunger); }

    public double getHydration()                { return hydration; }
    public void setHydration(double hydration)  { this.hydration = clamp(hydration); }

    public double getActivityLevel()                        { return activityLevel; }
    public void setActivityLevel(double activityLevel)      { this.activityLevel = clamp(activityLevel); }

    public int getEnclosureId()                             { return enclosureId; }
    public void setEnclosureId(int enclosureId)             { this.enclosureId = enclosureId; }

    public String getAssignedZookeeper()                    { return assignedZookeeper; }
    public void setAssignedZookeeper(String zookeeper)      { this.assignedZookeeper = zookeeper; }

    public LocalDate getAdmissionDate()                     { return admissionDate; }
    public void setAdmissionDate(LocalDate admissionDate)   { this.admissionDate = admissionDate; }
}
