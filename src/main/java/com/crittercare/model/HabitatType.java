package com.crittercare.model;

/**
 * Classifies the specific biome or natural environment of an enclosure.
 * <p>
 * This classification is primarily utilized by the simulation engine and
 * transfer services to validate the environmental compatibility before
 * assigning an animal to a specific enclosure.
 * </p>
 */
public enum HabitatType {

    /**
     * Represents a grassy plain biome with sparse tree cover, typically
     * suited for large grazing mammals and herd animals.
     */
    SAVANNAH("Savannah"),

    /**
     * Represents a water-based environment, equipped with pools or tanks,
     * designed for marine and semi-aquatic species.
     */
    AQUATIC("Aquatic"),

    /**
     * Represents a densely wooded environment with ample vegetation and
     * shade, suitable for woodland creatures.
     */
    FOREST("Forest"),

    /**
     * Represents a large, netted or glass-enclosed vertical space,
     * designed specifically to accommodate the flight patterns of birds.
     */
    AVIARY("Aviary"),

    /**
     * Represents a strictly climate-controlled, typically indoor facility,
     * providing necessary heat and humidity for cold-blooded species.
     */
    REPTILE_HOUSE("Reptile House"),

    /**
     * Represents a permanently cold, icy environment with specialized
     * cooling systems, required for polar and sub-polar species.
     */
    ARCTIC("Arctic");

    private final String displayName;

    /**
     * Constructs a new habitat type with the designated display name.
     *
     * @param displayName the human-readable representation of the habitat
     */
    HabitatType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Retrieves the human-readable display name of the habitat.
     *
     * @return the formatted display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the string representation of the habitat type.
     *
     * @return the human-readable display name
     */
    @Override
    public String toString() {
        return displayName;
    }
}
