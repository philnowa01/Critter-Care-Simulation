package com.crittercare.repository;

import com.crittercare.model.Alert;

import java.util.List;
import java.util.Optional;

/**
 * Defines the contract for persistence operations related to {@link Alert} entities.
 * <p>
 * This interface abstracts the underlying data storage mechanism, providing standard
 * CRUD (Create, Read, Update, Delete) operations and specific query methods to manage
 * the lifecycle of system alerts.
 * </p>
 */
public interface AlertRepository {

    /**
     * Retrieves all alerts currently stored in the system.
     * Implementations should ensure the results are ordered by their creation time,
     * with the most recent alerts appearing first.
     *
     * @return a list containing all alerts
     */
    List<Alert> findAll();

    /**
     * Retrieves all alerts that are currently in an active, unresolved state.
     *
     * @return a list containing all unresolved alerts
     */
    List<Alert> findActive();

    /**
     * Retrieves a specific alert based on its unique identifier.
     *
     * @param id the unique system identifier of the alert
     * @return an {@link Optional} containing the alert if found, or empty if it does not exist
     */
    Optional<Alert> findById(int id);

    /**
     * Persists a newly created alert to the datastore.
     * <p>
     * The implementation must ensure that a unique identifier is generated and
     * assigned to the provided alert instance during the persistence process.
     * </p>
     *
     * @param alert the alert entity to be stored
     * @return the persisted alert instance containing the generated unique identifier
     */
    Alert save(Alert alert);

    /**
     * Synchronizes modifications made to an existing alert with the datastore.
     * This primarily updates the operational status and resolution flags.
     *
     * @param alert the modified alert entity to be updated
     */
    void update(Alert alert);

    /**
     * Permanently removes a specific alert from the datastore.
     *
     * @param id the unique system identifier of the alert to be removed
     */
    void delete(int id);
}
