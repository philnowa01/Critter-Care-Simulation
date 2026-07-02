package com.crittercare.model;

/**
 * Represents a mammalian entity within the CritterCare simulation.
 * <p>
 * Mammals are endothermic (warm-blooded) animals. Due to the continuous energy
 * expenditure required to maintain internal body temperature, they exhibit higher
 * hunger and hydration decay rates compared to ectothermic species (e.g., reptiles).
 * </p>
 */
public class Mammal extends Animal {

    private boolean hasFur;
    private String  furColor;

    /**
     * Default constructor required for framework instantiation.
     * Initializes the mammal with standard traits (assumes the presence of fur).
     */
    public Mammal() {
        super();
        this.hasFur = true;
    }

    /**
     * Constructs a new mammal with core identity attributes.
     *
     * @param name    the given name of the mammal
     * @param species the species designation
     * @param age     the age in years
     */
    public Mammal(String name, String species, int age) {
        super(name, species, age);
        this.hasFur = true;
    }

    /**
     * Constructs a fully initialized mammal with specific physical traits.
     *
     * @param name     the given name of the mammal
     * @param species  the species designation
     * @param age      the age in years
     * @param hasFur   indicates whether the mammal possesses fur
     * @param furColor the descriptive color of the fur
     */
    public Mammal(String name, String species, int age, boolean hasFur, String furColor) {
        super(name, species, age);
        this.hasFur   = hasFur;
        this.furColor = furColor;
    }

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

    @Override
    public String getType() {
        return "MAMMAL";
    }

    /**
     * Generates a sound specific to the mammal's species.
     * Uses pattern matching to return the appropriate vocalization.
     *
     * @return a short sound description
     */
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

    // Getters and Setters

    public boolean isHasFur()              { return hasFur; }
    public void setHasFur(boolean hasFur)  { this.hasFur = hasFur; }

    public String getFurColor()            { return furColor; }
    public void setFurColor(String color)  { this.furColor = color; }
}
