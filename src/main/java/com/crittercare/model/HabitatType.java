package com.crittercare.model;

/**
 * Classifies the natural environment of an Enclosure.
 * Used to validate animal-enclosure compatibility.
 */
public enum HabitatType {

    SAVANNAH("Savannah"),
    AQUATIC("Aquatic"),
    FOREST("Forest"),
    AVIARY("Aviary"),
    REPTILE_HOUSE("Reptile House"),
    ARCTIC("Arctic");

    private final String displayName;

    HabitatType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
