package com.crittercare.service;

import com.crittercare.model.MaintenanceLog;
import com.crittercare.repository.MaintenanceLogRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Business logic for care log management.
 *
 * Rules enforced here:
 *  - A log must have a non-blank activity type and staff member name.
 *  - A log must reference at least one of: an animal or an enclosure.
 *  - getTodaysLogCount() counts logs whose timestamp falls on today's date
 *    (used by the Dashboard "Today's Logs" summary card).
 */
public class MaintenanceLogService {

    private final MaintenanceLogRepository logRepo;

    public MaintenanceLogService(MaintenanceLogRepository logRepo) {
        this.logRepo = logRepo;
    }

    // ── Queries ──────────────────────────────────────────────────────────────

    /** Returns all log entries, most recent first. */
    public List<MaintenanceLog> getAllLogs() {
        return logRepo.findAll();
    }

    /** Returns the most recent {@code limit} log entries across all sources. */
    public List<MaintenanceLog> getRecentLogs(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("Limit must be at least 1.");
        }
        return logRepo.findRecent(limit);
    }

    /** Returns the log entry with the given id, or empty if not found. */
    public Optional<MaintenanceLog> getLogById(int id) {
        return logRepo.findById(id);
    }

    /** Returns all logs linked to a specific animal. */
    public List<MaintenanceLog> getLogsByAnimal(int animalId) {
        return logRepo.findByAnimalId(animalId);
    }

    /** Returns all logs linked to a specific enclosure. */
    public List<MaintenanceLog> getLogsByEnclosure(int enclosureId) {
        return logRepo.findByEnclosureId(enclosureId);
    }

    /**
     * Counts log entries whose timestamp falls on today's date.
     * Used by the Dashboard summary card.
     */
    public long getTodaysLogCount() {
        LocalDate today = LocalDate.now();
        return logRepo.findAll().stream()
                .filter(log -> log.getTimestamp() != null
                        && log.getTimestamp().toLocalDate().equals(today))
                .count();
    }

    /**
     * Counts feeding logs completed today.
     * Used by the Care Logs "Feedings Completed" summary card.
     */
    public long getTodaysFeedingCount() {
        LocalDate today = LocalDate.now();
        return logRepo.findAll().stream()
                .filter(log -> log.getTimestamp() != null
                        && log.getTimestamp().toLocalDate().equals(today)
                        && "Feeding".equalsIgnoreCase(log.getActivityType())
                        && log.isCompleted())
                .count();
    }

    /**
     * Counts sanitation logs completed or in-progress today.
     * Used by the Care Logs "Sanitation Tasks" summary card.
     */
    public long getTodaysSanitationCount() {
        LocalDate today = LocalDate.now();
        return logRepo.findAll().stream()
                .filter(log -> log.getTimestamp() != null
                        && log.getTimestamp().toLocalDate().equals(today)
                        && "Sanitation".equalsIgnoreCase(log.getActivityType()))
                .count();
    }

    /**
     * Counts logs that require follow-up.
     * Used by the Care Logs "Pending Interventions" summary card.
     */
    public long getPendingInterventionCount() {
        return logRepo.findAll().stream()
                .filter(MaintenanceLog::isFollowUpNeeded)
                .count();
    }

    // ── Commands ─────────────────────────────────────────────────────────────

    /**
     * Validates and persists a new log entry.
     *
     * @return the persisted log with its generated id set
     * @throws IllegalArgumentException if validation fails
     */
    public MaintenanceLog addLog(MaintenanceLog log) {
        validateLog(log);
        return logRepo.save(log);
    }

    /**
     * Validates and persists updates to an existing log entry.
     *
     * @throws IllegalArgumentException if validation fails
     * @throws IllegalStateException    if the log does not exist
     */
    public void updateLog(MaintenanceLog log) {
        validateLog(log);
        logRepo.findById(log.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Cannot update — log id " + log.getId() + " not found."));
        logRepo.update(log);
    }

    /**
     * Deletes a log entry.
     *
     * @throws IllegalStateException if the log does not exist
     */
    public void deleteLog(int id) {
        logRepo.findById(id)
                .orElseThrow(() -> new IllegalStateException(
                        "Cannot delete — log id " + id + " not found."));
        logRepo.delete(id);
    }

    /**
     * Marks an existing log as completed and persists the change.
     */
    public void markAsCompleted(int id) {
        MaintenanceLog log = logRepo.findById(id)
                .orElseThrow(() -> new IllegalStateException(
                        "Log id " + id + " not found."));
        log.setStatus("Completed");
        logRepo.update(log);
    }

    // ── Validation ───────────────────────────────────────────────────────────

    /**
     * Validates the fields every log must have.
     *
     * @throws IllegalArgumentException describing what is wrong
     */
    public void validateLog(MaintenanceLog log) {
        if (log == null) {
            throw new IllegalArgumentException("Log must not be null.");
        }
        if (log.getActivityType() == null || log.getActivityType().isBlank()) {
            throw new IllegalArgumentException("Activity type must not be blank.");
        }
        if (log.getStaffMember() == null || log.getStaffMember().isBlank()) {
            throw new IllegalArgumentException("Staff member name must not be blank.");
        }
        if (log.getAnimalId() <= 0 && log.getEnclosureId() <= 0) {
            throw new IllegalArgumentException(
                    "Log must reference at least one animal or enclosure.");
        }
    }
}
