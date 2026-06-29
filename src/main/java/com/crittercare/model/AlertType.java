package com.crittercare.model;

/**
 * Categories of alerts the simulation engine can generate.
 */
public enum AlertType {

    CRITICAL_HUNGER("Critical Hunger"),
    LOW_HEALTH("Low Health"),
    DIRTY_ENCLOSURE("Dirty Enclosure"),
    OVERCROWDED("Overcrowded"),
    ANIMAL_SICK("Animal Sick");

    private final String displayName;

    AlertType(String displayName) {
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
