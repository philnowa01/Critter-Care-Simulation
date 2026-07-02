package com.crittercare.model;

/**
 * Represents a staff member responsible for the care, monitoring, and
 * maintenance of animals and enclosures within the facility.
 */
public class Zookeeper {

    public static final String UNKNOWN_INITIALS = "?";

    private final int id;
    private String name;

    /**
     * Default constructor required for framework instantiation.
     */
    public Zookeeper() {
    }

    /**
     * Constructs a new zookeeper with the specified identity attributes.
     *
     * @param id   the unique system identifier for the zookeeper
     * @param name the full name of the staff member
     */
    public Zookeeper(int id, String name) {
        this.id   = id;
        this.name = name;
    }

// Getters and Setters

    public int    getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void   setName(String name) {
        this.name = name;
    }

    /**
     * Extracts and formats the zookeeper's initials for UI display purposes.
     * <p>
     * If the name consists of multiple words, the first letters of the first and
     * last words are utilized. For single-word names, up to the first two letters
     * are extracted.
     * </p>
     *
     * @return the capitalized initials, or {@value #UNKNOWN_INITIALS} if the name is unassigned or empty
     */
    public String getInitials() {
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
        }
        return name.isBlank() ? "?" : name.substring(0, Math.min(2, name.length())).toUpperCase();
    }
}
