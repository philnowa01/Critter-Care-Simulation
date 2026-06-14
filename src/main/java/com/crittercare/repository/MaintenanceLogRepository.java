package com.crittercare.repository;

import com.crittercare.model.MaintenanceLog;

import java.util.List;
import java.util.Optional;

/**
 * Contract for all MaintenanceLog persistence operations.
 */
public interface MaintenanceLogRepository {

    /** Returns every log entry, most recent first. */
    List<MaintenanceLog> findAll();

    /** Returns the log entry with the given id, or empty if not found. */
    Optional<MaintenanceLog> findById(int id);

    /** Returns all log entries linked to a specific animal. */
    List<MaintenanceLog> findByAnimalId(int animalId);

    /** Returns all log entries linked to a specific enclosure. */
    List<MaintenanceLog> findByEnclosureId(int enclosureId);

    /**
     * Returns the most recent {@code limit} log entries across all animals
     * and enclosures (used by the Dashboard "Recent Care Logs" panel).
     */
    List<MaintenanceLog> findRecent(int limit);

    /**
     * Inserts a new log entry and sets its generated id on the object.
     * @return the same log object, now with a valid id
     */
    MaintenanceLog save(MaintenanceLog log);

    /** Updates every field of an existing log entry. */
    void update(MaintenanceLog log);

    /** Deletes the log entry with the given id. */
    void delete(int id);
}
