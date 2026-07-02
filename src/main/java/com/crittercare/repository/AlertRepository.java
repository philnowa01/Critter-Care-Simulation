package com.crittercare.repository;

import com.crittercare.model.Alert;

import java.util.List;
import java.util.Optional;

/**
 * Contract for Alert persistence operations.
 *
 * Alerts are generated in memory by AlertService but also persisted so
 * they survive an application restart and appear in the DB console.
 */
public interface AlertRepository {

    /** Returns all alerts, most recent first. */
    List<Alert> findAll();

    /** Returns all unresolved alerts. */
    List<Alert> findActive();

    /** Returns the alert with the given id, or empty if not found. */
    Optional<Alert> findById(int id);

    /**
     * Inserts a new alert and sets its generated id on the object.
     * @return the same alert object, now with a valid id
     */
    Alert save(Alert alert);

    /** Updates the status and resolved flag of an existing alert. */
    void update(Alert alert);

    /** Deletes the alert with the given id. */
    void delete(int id);
}
