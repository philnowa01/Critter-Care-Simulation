package com.crittercare.model;

/**
 * Warm-blooded, feathered animal.
 *
 * Birds have the highest hunger rate of all three subtypes — their fast
 * wingbeat metabolism demands constant caloric intake.
 *
 * Subtype-specific DB columns: wingspan_cm, can_fly
 */
public class Bird extends Animal {

    private double  wingspanCm;
    private boolean canFly;

    // ── Constructors ─────────────────────────────────────────────────────────

    public Bird() {
        super();
        this.canFly = true;
    }

    public Bird(String name, String species, int age) {
        super(name, species, age);
        this.canFly = true;
    }

    public Bird(String name, String species, int age, double wingspanCm, boolean canFly) {
        super(name, species, age);
        this.wingspanCm = wingspanCm;
        this.canFly     = canFly;
    }

    // ── Template Method hooks ────────────────────────────────────────────────

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

    // ── Abstract method implementations ──────────────────────────────────────

    @Override
    public String getType() {
        return "BIRD";
    }

    @Override
    public String makeSound() {
        return switch (getSpecies()) {
            case "African Penguin" -> "Bray!";
            case "Scarlet Macaw"   -> "Squawk!";
            case "Bald Eagle"      -> "Screech!";
            default                -> "Tweet!";
        };
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public double getWingspanCm()                   { return wingspanCm; }
    public void setWingspanCm(double wingspanCm)    { this.wingspanCm = wingspanCm; }

    public boolean isCanFly()                       { return canFly; }
    public void setCanFly(boolean canFly)           { this.canFly = canFly; }
}
