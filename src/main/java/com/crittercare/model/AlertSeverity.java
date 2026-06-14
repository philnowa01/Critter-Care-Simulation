package com.crittercare.model;

/**
 * Urgency level of an Alert.
 * Ordered from highest to lowest urgency.
 */
public enum AlertSeverity {

    CRITICAL("Critical"),
    HIGH("High"),
    WARNING("Warning"),
    NORMAL("Normal");

    private final String displayName;

    AlertSeverity(String displayName) {
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
