package com.crittercare.model;

import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Abstract base entity representing an animal within the CritterCare system.
 * <p>
 * This class encapsulates core identity, vital statistics, and relationship data.
 * It provides the foundational simulation mechanics for vital degradation over time.
 * Subclasses can customize this behavior by overriding the metabolic rate hooks.
 * </p>
 * <p>
 * Note: All vital statistics (health, hunger, hydration, activity level) are
 * bounded within the range of 0.0 to 100.0.
 * </p>
 */
public abstract class Animal {

    private int       id;
    private String    name;
    private String    species;
    private int       age;

    private double health;         // 100 = perfect health
    private double hunger;         // 0 = full, 100 = starving
    private double hydration;      // 100 = fully hydrated
    private double activityLevel;  // 100 = very active

    private int       enclosureId;
    private String    assignedZookeeper;
    private LocalDate admissionDate;

    /**
     * Default constructor required for framework instantiation (e.g., ORM or JDBC mapping).
     * Initializes vitals to optimal default values.
     */
    public Animal() {
        this.health        = 100.0;
        this.hunger        = 0.0;
        this.hydration     = 100.0;
        this.activityLevel = 80.0;
        this.admissionDate = LocalDate.now();
    }

    /**
     * Constructs a new animal instance with specified identity attributes.
     *
     * @param name    the given name of the animal
     * @param species the species designation
     * @param age     the age of the animal in years
     */
    public Animal(String name, String species, int age) {
        this();
        this.name    = name;
        this.species = species;
        this.age     = age;
    }

    /**
     * Advances the animal's simulation state by the specified time increment.
     * <p>
     * This method implements the core simulation algorithm and is marked final
     * to guarantee consistency across the system. Subclasses adjust the outcome
     * by overriding the respective rate-provider methods.
     * </p>
     *
     * @param deltaHours the simulated time elapsed since the last tick, in hours
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
     * Determines the rate at which the animal's hunger increases.
     * Subclasses should override this to reflect specific metabolic needs.
     *
     * @return the hunger increase per simulated hour
     */
    protected double getHungerRate() {
        return 3.0;
    }

    /**
     * Determines the rate at which the animal's hydration decreases.
     *
     * @return the hydration decrease per simulated hour
     */
    protected double getHydrationDecayRate() {
        return 2.0;
    }

    /**
     * Determines the rate at which the animal's health decreases when neglected.
     *
     * @return the health decrease per simulated hour
     */
    protected double getHealthDecayRate() {
        return 2.0;
    }

    /**
     * Retrieves the persistence-mapped type identifier for this animal.
     *
     * @return the type string (e.g., "MAMMAL", "BIRD", "REPTILE")
     */
    public abstract String getType();

    /**
     * Generates a characteristic sound associated with the animal.
     *
     * @return a short sound description
     */
    public abstract String makeSound();

    /**
     * Evaluates if the animal's hunger has reached a critical state.
     *
     * @return {@code true} if hunger is critical, otherwise {@code false}
     */
    public boolean isHungerCritical() {
        return hunger >= 75.0;
    }

    /**
     * Evaluates if the animal's health has fallen to a critical state.
     *
     * @return {@code true} if health is critical, otherwise {@code false}
     */
    public boolean isHealthCritical() {
        return health <= 30.0;
    }

    /**
     * Evaluates if the animal is critically dehydrated.
     *
     * @return {@code true} if dehydration is critical, otherwise {@code false}
     */
    public boolean isDehydrationCritical() {
        return hydration <= 20.0;
    }

    /**
     * Determines if any vital statistic is currently in a warning state.
     *
     * @return {@code true} if the animal requires staff attention, otherwise {@code false}
     */
    public boolean requiresAttention() {
        return hunger >= 60.0 || health <= 50.0 || hydration <= 30.0;
    }

    /**
     * Constrains a given statistic value to the permissible limits.
     *
     * @param value the raw computed value
     * @return the clamped value between {@value #MIN_STAT_VALUE} and {@value #MAX_STAT_VALUE}
     */
    private double clamp(double value) {
        return Math.max(0.0, Math.min(100.0, value));
    }

    @Override
    public String toString() {
        return name + " (" + species + ") – Health: " + String.format("%.0f", health) + "%";
    }

    //Getters and Setters

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
        this.name = name;
    }

    public String getSpecies() {
        return species;
    }
    public void setSpecies(String species) {
        this.species = species;
    }

    public int getAge() {
        return age;
    }
    public void setAge(int age) {
        this.age = age;
    }

    public double getHealth() {
        return health;
    }
    public void setHealth(double health) {
        this.health = clamp(health);
    }

    public double getHunger() {
        return hunger;
    }
    public void setHunger(double hunger) {
        this.hunger = clamp(hunger);
    }

    public double getHydration() {
        return hydration;
    }
    public void setHydration(double hydration) {
        this.hydration = clamp(hydration);
    }

    public double getActivityLevel() {
        return activityLevel;
    }
    public void setActivityLevel(double activityLevel) {
        this.activityLevel = clamp(activityLevel);
    }

    public int getEnclosureId() {
        return enclosureId;
    }
    public void setEnclosureId(int enclosureId) {
        this.enclosureId = enclosureId;
    }

    public String getAssignedZookeeper() {
        return assignedZookeeper;
    }
    public void setAssignedZookeeper(String zookeeper) {
        this.assignedZookeeper = zookeeper;
    }

    public LocalDate getAdmissionDate() {
        return admissionDate;
    }
    public void setAdmissionDate(LocalDate admissionDate)   {
        this.admissionDate = admissionDate;
    }
}
