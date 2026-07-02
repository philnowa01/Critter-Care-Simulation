package com.crittercare.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Records a single care or maintenance activity performed within the facility.
 * <p>
 * A maintenance log provides an auditable history of staff actions. It can be
 * associated with a specific animal, a specific enclosure, or both, depending
 * on the nature of the activity (e.g., enclosure sanitation versus animal medical intervention).
 * </p>
 */
public class MaintenanceLog {

    /**
     * Represents a null or unassigned reference for animal or enclosure IDs,
     * used when a log entry only applies to one specific entity type.
     */
    public static final int UNASSIGNED_ID = 0;

    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    private int           id;
    private int           animalId;        // 0 means not linked to an animal
    private int           enclosureId;     // 0 means not linked to an enclosure
    private LocalDateTime timestamp;
    private String        activityType;
    private String        staffMember;
    private String        status;
    private String        notes;
    private boolean       followUpNeeded;

    /**
     * Default constructor required for framework instantiation.
     * Initializes the log with the current timestamp and a completed status.
     */
    public MaintenanceLog() {
        this.timestamp = LocalDateTime.now();
        this.status    = "Completed";
    }

    /**
     * Constructs a new maintenance log entry with core activity details.
     *
     * @param animalId     the target animal's ID, or {@value #UNASSIGNED_ID} if not applicable
     * @param enclosureId  the target enclosure's ID, or {@value #UNASSIGNED_ID} if not applicable
     * @param activityType the categorization of the performed work (e.g., "Feeding", "Health Check")
     * @param staffMember  the identifier or name of the staff member performing the task
     */
    public MaintenanceLog(int animalId, int enclosureId,
                          String activityType, String staffMember) {
        this();
        this.animalId     = animalId;
        this.enclosureId  = enclosureId;
        this.activityType = activityType;
        this.staffMember  = staffMember;
    }

    /**
     * Evaluates whether this maintenance task has been fully executed.
     *
     * @return {@code true} if the status matches the completed state, otherwise {@code false}
     */
    public boolean isCompleted() {
        return "Completed".equalsIgnoreCase(status);
    }

    /**
     * Evaluates whether this activity requires subsequent action or monitoring.
     *
     * @return {@code true} if a follow-up is necessary, otherwise {@code false}
     */
    public boolean needsFollowUp() {
        return followUpNeeded;
    }

    /**
     * Retrieves the formatted timestamp of the logged activity for presentation layers.
     *
     * @return the formatted time string, or an empty string if no timestamp is set
     */
    public String getFormattedTimestamp() {
        return timestamp != null ? timestamp.format(DISPLAY_FORMAT) : "";
    }

    @Override
    public String toString() {
        return "[" + activityType + "] " + staffMember
               + " @ " + getFormattedTimestamp()
               + " – " + status;
    }

    // Getters and Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAnimalId() {
        return animalId;
    }
    public void setAnimalId(int animalId) {
        this.animalId = animalId;
    }

    public int getEnclosureId() {
        return enclosureId;
    }
    public void setEnclosureId(int enclosureId) {
        this.enclosureId = enclosureId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getActivityType() {
        return activityType;
    }
    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public String getStaffMember() {
        return staffMember;
    }
    public void setStaffMember(String staffMember) {
        this.staffMember = staffMember;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }
    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isFollowUpNeeded() {
        return followUpNeeded;
    }
    public void setFollowUpNeeded(boolean followUpNeeded) {
        this.followUpNeeded = followUpNeeded;
    }
}
