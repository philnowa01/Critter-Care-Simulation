package com.crittercare.repository;

import com.crittercare.model.Enclosure;
import com.crittercare.model.HabitatType;

import java.util.List;
import java.util.Optional;

/**
 * Defines the contract for persistence operations related to {@link Enclosure} entities.
 * <p>
 * This interface abstracts the underlying data storage mechanism, providing standard
 * CRUD (Create, Read, Update, Delete) operations and specific query methods to manage
 * the lifecycle of habitat enclosures within the facility.
 * </p>
 */
public interface EnclosureRepository {

    /**
     * Retrieves all enclosures currently stored in the system.
     * <p>
     * Implementations must ensure that the returned enclosure entities are fully
     * populated with their associated animal identifiers.
     * </p>
     *
     * @return a list containing all enclosures
     */
    List<Enclosure> findAll();

    /**
     * Retrieves a specific enclosure based on its unique identifier.
     *
     * @param id the unique system identifier of the enclosure
     * @return an {@link Optional} containing the enclosure if found, or empty if it does not exist
     */
    Optional<Enclosure> findById(int id);

    /**
     * Retrieves all enclosures that match a specific environmental classification.
     *
     * @param type the {@link HabitatType} to filter by
     * @return a list of enclosures matching the specified habitat type
     */
    List<Enclosure> findByHabitatType(HabitatType type);

    /**
     * Persists a newly created enclosure to the datastore.
     * <p>
     * The implementation must ensure that a unique identifier is generated and
     * assigned to the provided enclosure instance during the persistence process.
     * </p>
     *
     * @param enclosure the enclosure entity to be stored
     * @return the persisted enclosure instance containing the generated unique identifier
     */
    Enclosure save(Enclosure enclosure);

    /**
     * Synchronizes modifications made to an existing enclosure with the datastore.
     * This updates all relevant state fields of the enclosure record.
     *
     * @param enclosure the modified enclosure entity to be updated
     */
    void update(Enclosure enclosure);

    /**
     * Permanently removes a specific enclosure from the datastore.
     *
     * @param id the unique system identifier of the enclosure to be removed
     */
    void delete(int id);
}
