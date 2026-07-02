package com.crittercare.repository;

import com.crittercare.model.Animal;

import java.util.List;
import java.util.Optional;

/**
 * Defines the contract for persistence operations related to {@link Animal} entities.
 * <p>
 * This interface abstracts the underlying data storage mechanism, providing standard
 * CRUD (Create, Read, Update, Delete) operations and specific query methods to manage
 * the lifecycle of animals within the facility.
 * </p>
 */
public interface AnimalRepository {

    /**
     * Retrieves all animals currently stored in the system.
     *
     * @return a list containing all animals
     */
    List<Animal> findAll();

    /**
     * Retrieves a specific animal based on its unique identifier.
     *
     * @param id the unique system identifier of the animal
     * @return an {@link Optional} containing the animal if found, or empty if it does not exist
     */
    Optional<Animal> findById(int id);

    /**
     * Retrieves a list of all animals currently assigned to a specific enclosure.
     *
     * @param enclosureId the unique identifier of the target enclosure
     * @return a list of animals residing in the specified enclosure
     */
    List<Animal> findByEnclosureId(int enclosureId);

    /**
     * Persists a newly created animal to the datastore.
     * <p>
     * The implementation must ensure that a unique identifier is generated and
     * assigned to the provided animal instance during the persistence process.
     * </p>
     *
     * @param animal the animal entity to be stored
     * @return the persisted animal instance containing the generated unique identifier
     */
    Animal save(Animal animal);

    /**
     * Synchronizes modifications made to an existing animal with the datastore.
     * This updates all relevant fields of the animal record.
     *
     * @param animal the modified animal entity to be updated
     */
    void update(Animal animal);

    /**
     * Permanently removes a specific animal from the datastore.
     *
     * @param id the unique system identifier of the animal to be removed
     */
    void delete(int id);
}
