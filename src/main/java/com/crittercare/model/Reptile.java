package com.crittercare.model;

/**
 * Cold-blooded, scaled animal.
 *
 * Reptiles have the lowest hunger and hydration decay rate — their
 * ectothermic metabolism means they can go days without food.
 * They also require UVB lighting to synthesise vitamin D3; the
 * uvbRequirement field tracks how many hours of UVB exposure per day
 * the animal needs.
 *
 * Subtype-specific DB columns: is_venomous, uvb_requirement
 */
public class Reptile extends Animal {

    private boolean isVenomous;
    private double  uvbRequirement; // hours of UVB per day

    // ── Constructors ─────────────────────────────────────────────────────────

    public Reptile() {
        super();
        this.uvbRequirement = 5.0;
    }

    public Reptile(String name, String species, int age) {
        super(name, species, age);
        this.uvbRequirement = 5.0;
    }

    public Reptile(String name, String species, int age,
                   boolean isVenomous, double uvbRequirement) {
        super(name, species, age);
        this.isVenomous      = isVenomous;
        this.uvbRequirement  = uvbRequirement;
    }

    // ── Template Method hooks ────────────────────────────────────────────────

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

    // ── Abstract method implementations ──────────────────────────────────────

    @Override
    public String getType() {
        return "REPTILE";
    }

    @Override
    public String makeSound() {
        return switch (getSpecies()) {
            case "Nile Crocodile" -> "Hiss!";
            case "Komodo Dragon"  -> "Growl!";
            case "Green Iguana"   -> "...";
            default               -> "Hiss!";
        };
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public boolean isVenomous()                         { return isVenomous; }
    public void setVenomous(boolean venomous)           { this.isVenomous = venomous; }

    public double getUvbRequirement()                   { return uvbRequirement; }
    public void setUvbRequirement(double uvbReq)        { this.uvbRequirement = uvbReq; }
}
