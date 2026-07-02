package com.crittercare.repository;

import com.crittercare.model.Animal;

import java.util.List;
import java.util.Optional;

/**
 * Contract for all Animal persistence operations.
 *
 * Services depend on this interface, not on the JDBC implementation,
 * which satisfies the Dependency Inversion Principle (SOLID – D).
 */
public interface AnimalRepository {

    /** Returns every animal in the database. */
    List<Animal> findAll();

    /** Returns the animal with the given id, or empty if not found. */
    Optional<Animal> findById(int id);

    /** Returns all animals currently assigned to a specific enclosure. */
    List<Animal> findByEnclosureId(int enclosureId);

    /**
     * Inserts a new animal row and sets its generated id on the object.
     * @return the same animal object, now with a valid id
     */
    Animal save(Animal animal);

    /** Updates every field of an existing animal row. */
    void update(Animal animal);

    /** Deletes the animal with the given id. */
    void delete(int id);
}
