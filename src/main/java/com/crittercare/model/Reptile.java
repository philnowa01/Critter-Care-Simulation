package com.crittercare.model;

/**
 * Represents a reptilian entity within the CritterCare simulation.
 * <p>
 * Reptiles are ectothermic (cold-blooded) animals. Their slower metabolism
 * results in the lowest hunger and hydration decay rates among the animal
 * classifications. Additionally, they require specific environmental conditions,
 * such as adequate UVB lighting, to synthesize essential nutrients like vitamin D3.
 * </p>
 */
public class Reptile extends Animal {

    private boolean isVenomous;
    private double  uvbRequirement; // hours of UVB per day

    /**
     * Default constructor required for framework instantiation.
     * Initializes the reptile with a standard baseline UVB daily requirement.
     */
    public Reptile() {
        super();
        this.uvbRequirement = 5.0;
    }

    /**
     * Constructs a new reptile with core identity attributes.
     *
     * @param name    the given name of the reptile
     * @param species the species designation
     * @param age     the age in years
     */
    public Reptile(String name, String species, int age) {
        super(name, species, age);
        this.uvbRequirement = 5.0;
    }

    /**
     * Constructs a fully initialized reptile with specific physiological and environmental traits.
     *
     * @param name           the given name of the reptile
     * @param species        the species designation
     * @param age            the age in years
     * @param isVenomous     indicates whether the reptile produces venom
     * @param uvbRequirement the required hours of UVB exposure per day
     */
    public Reptile(String name, String species, int age,
                   boolean isVenomous, double uvbRequirement) {
        super(name, species, age);
        this.isVenomous      = isVenomous;
        this.uvbRequirement  = uvbRequirement;
    }

    @Override
    protected double getHungerRate() {
        // Cold-blooded → slowest metabolism
        return 1.5;
    }

    @Override
    protected double getHydrationDecayRate() {
        return 1.0;
    }

    @Override
    protected double getHealthDecayRate() {
        return 1.5;
    }

    @Override
    public String getType() {
        return "REPTILE";
    }

    /**
     * Generates a sound specific to the reptile's species.
     * Uses pattern matching to return the appropriate vocalization.
     *
     * @return a short sound description
     */
    @Override
    public String makeSound() {
        return switch (getSpecies()) {
            case "Nile Crocodile" -> "Hiss!";
            case "Komodo Dragon"  -> "Growl!";
            case "Green Iguana"   -> "...";
            default               -> "Hiss!";
        };
    }

    // Getters and Setters

    public boolean isVenomous()                         { return isVenomous; }
    public void setVenomous(boolean venomous)           { this.isVenomous = venomous; }

    public double getUvbRequirement()                   { return uvbRequirement; }
    public void setUvbRequirement(double uvbReq)        { this.uvbRequirement = uvbReq; }
}
