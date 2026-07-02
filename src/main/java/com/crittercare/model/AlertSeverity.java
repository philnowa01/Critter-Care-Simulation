package com.crittercare.model;

/**
 * Represents the urgency level of an alert within the CritterCare system.
 * The severity levels are ordered from the highest urgency to the lowest.
 */
public enum AlertSeverity {
    /**
     * Indicates a critical issue that requires immediate attention and action.
     */
    CRITICAL("Critical"),

    /**
     * Indicates a high-priority issue that should be addressed promptly.
     */
    HIGH("High"),

    /**
     * Represents a warning about a potential issue or non-standard condition.
     */
    WARNING("Warning"),

    /**
     * Represents a standard informational alert requiring no immediate action.
     */
    NORMAL("Normal");

    private final String displayName;

    /**
     * Constructs a new alert severity with the specified display name.
     *
     * @param displayName the human-readable representation of the severity level
     */
    AlertSeverity(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Retrieves the human-readable display name of the severity level.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the string representation of the severity level.
     *
     * @return the human-readable display name
     */
    @Override
    public String toString() {
        return displayName;
    }
}
