package com.crittercare.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Records a single care activity performed on an animal or enclosure.
 *
 * Examples: Feeding, Health Check, Sanitation, Enrichment, Intervention.
 * Either animalId or enclosureId may be 0 when the log targets only one
 * of them (e.g., a sanitation log may only reference an enclosure).
 */
public class MaintenanceLog {

    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    // ── Fields ───────────────────────────────────────────────────────────────

    private int           id;
    private int           animalId;        // 0 means not linked to an animal
    private int           enclosureId;     // 0 means not linked to an enclosure
    private LocalDateTime timestamp;
    private String        activityType;
    private String        staffMember;
    private String        status;
    private String        notes;
    private boolean       followUpNeeded;

    // ── Constructors ─────────────────────────────────────────────────────────

    public MaintenanceLog() {
        this.timestamp = LocalDateTime.now();
        this.status    = "Completed";
    }

    public MaintenanceLog(int animalId, int enclosureId,
                          String activityType, String staffMember) {
        this();
        this.animalId     = animalId;
        this.enclosureId  = enclosureId;
        this.activityType = activityType;
        this.staffMember  = staffMember;
    }

    // ── Business logic ───────────────────────────────────────────────────────

    /** Returns true if this log entry is marked as Completed. */
    public boolean isCompleted() {
        return "Completed".equalsIgnoreCase(status);
    }

    /** Returns true if this log entry needs a follow-up action. */
    public boolean needsFollowUp() {
        return followUpNeeded;
    }

    /** Returns a formatted timestamp string for display in the UI. */
    public String getFormattedTimestamp() {
        return timestamp != null ? timestamp.format(DISPLAY_FORMAT) : "";
    }

    @Override
    public String toString() {
        return "[" + activityType + "] " + staffMember
               + " @ " + getFormattedTimestamp()
               + " – " + status;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }

    public int getAnimalId()                    { return animalId; }
    public void setAnimalId(int animalId)       { this.animalId = animalId; }

    public int getEnclosureId()                     { return enclosureId; }
    public void setEnclosureId(int enclosureId)     { this.enclosureId = enclosureId; }

    public LocalDateTime getTimestamp()                     { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp)       { this.timestamp = timestamp; }

    public String getActivityType()                     { return activityType; }
    public void setActivityType(String activityType)    { this.activityType = activityType; }

    public String getStaffMember()                      { return staffMember; }
    public void setStaffMember(String staffMember)      { this.staffMember = staffMember; }

    public String getStatus()                   { return status; }
    public void setStatus(String status)        { this.status = status; }

    public String getNotes()                    { return notes; }
    public void setNotes(String notes)          { this.notes = notes; }

    public boolean isFollowUpNeeded()                       { return followUpNeeded; }
    public void setFollowUpNeeded(boolean followUpNeeded)   { this.followUpNeeded = followUpNeeded; }
}
