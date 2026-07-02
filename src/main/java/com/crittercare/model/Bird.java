package com.crittercare.model;

/**
 * Represents an avian entity within the CritterCare simulation.
 * <p>
 * Birds are characterized by a highly active metabolism, resulting in the
 * highest caloric demand among all animal classifications. Their rapid
 * hunger and hydration depletion requires careful monitoring by staff.
 * </p>
 */
public class Bird extends Animal {

    private double  wingspanCm;
    private boolean canFly;

    /**
     * Default constructor required for framework instantiation.
     * Assumes standard flight capabilities by default.
     */
    public Bird() {
        super();
        this.canFly = true;
    }

    /**
     * Constructs a new bird with core identity attributes.
     *
     * @param name    the given name of the bird
     * @param species the species designation
     * @param age     the age in years
     */
    public Bird(String name, String species, int age) {
        super(name, species, age);
        this.canFly = true;
    }

    /**
     * Constructs a fully initialized bird with specific physiological traits.
     *
     * @param name       the given name of the bird
     * @param species    the species designation
     * @param age        the age in years
     * @param wingspanCm the wingspan measured in centimeters
     * @param canFly     indicates whether the bird is capable of sustained flight
     */
    public Bird(String name, String species, int age, double wingspanCm, boolean canFly) {
        super(name, species, age);
        this.wingspanCm = wingspanCm;
        this.canFly     = canFly;
    }

    @Override
    protected double getHungerRate() {
        // Highest metabolism of all three types
        return 5.0;
    }

    @Override
    protected double getHydrationDecayRate() {
        return 3.0;
    }

    @Override
    protected double getHealthDecayRate() {
        return 2.5;
    }

    @Override
    public String getType() {
        return "BIRD";
    }

    /**
     * Generates a sound specific to the bird's species.
     * Uses pattern matching to return the appropriate vocalization.
     *
     * @return a short sound description
     */
    @Override
    public String makeSound() {
        return switch (getSpecies()) {
            case "African Penguin" -> "Bray!";
            case "Scarlet Macaw"   -> "Squawk!";
            case "Bald Eagle"      -> "Screech!";
            default                -> "Tweet!";
        };
    }

    // Getters and Setters

    public double getWingspanCm()                   { return wingspanCm; }
    public void setWingspanCm(double wingspanCm)    { this.wingspanCm = wingspanCm; }

    public boolean isCanFly()                       { return canFly; }
    public void setCanFly(boolean canFly)           { this.canFly = canFly; }
}
