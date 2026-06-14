package com.crittercare.model;

/**
 * Warm-blooded animal with fur.
 *
 * Mammals have a higher hunger and hydration decay rate than reptiles
 * because warm-blooded animals burn more calories to maintain body temperature.
 *
 * Subtype-specific DB columns: has_fur, fur_color
 */
public class Mammal extends Animal {

    private boolean hasFur;
    private String  furColor;

    // ── Constructors ─────────────────────────────────────────────────────────

    public Mammal() {
        super();
        this.hasFur = true;
    }

    public Mammal(String name, String species, int age) {
        super(name, species, age);
        this.hasFur = true;
    }

    public Mammal(String name, String species, int age, boolean hasFur, String furColor) {
        super(name, species, age);
        this.hasFur   = hasFur;
        this.furColor = furColor;
    }

    // ── Template Method hooks ────────────────────────────────────────────────

    @Override
    protected double getHungerRate() {
        // Warm-blooded → higher metabolism than base Animal
        return 4.0;
    }

    @Override
    protected double getHydrationDecayRate() {
        return 2.5;
    }

    @Override
    protected double getHealthDecayRate() {
        return 2.0;
    }

    // ── Abstract method implementations ──────────────────────────────────────

    @Override
    public String getType() {
        return "MAMMAL";
    }

    @Override
    public String makeSound() {
        return switch (getSpecies()) {
            case "African Lion"   -> "Roar!";
            case "Asian Elephant" -> "Trumpet!";
            case "Giraffe"        -> "Hum!";
            case "Chimpanzee"     -> "Hoo hoo!";
            default               -> "...";
        };
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public boolean isHasFur()              { return hasFur; }
    public void setHasFur(boolean hasFur)  { this.hasFur = hasFur; }

    public String getFurColor()            { return furColor; }
    public void setFurColor(String color)  { this.furColor = color; }
}
