package com.crittercare.repository;

import com.crittercare.model.Enclosure;
import com.crittercare.model.HabitatType;

import java.util.List;
import java.util.Optional;

/**
 * Contract for all Enclosure persistence operations.
 */
public interface EnclosureRepository {

    /** Returns every enclosure in the database, with animalIds populated. */
    List<Enclosure> findAll();

    /** Returns the enclosure with the given id, or empty if not found. */
    Optional<Enclosure> findById(int id);

    /** Returns all enclosures of a specific habitat type. */
    List<Enclosure> findByHabitatType(HabitatType type);

    /**
     * Inserts a new enclosure row and sets its generated id on the object.
     * @return the same enclosure object, now with a valid id
     */
    Enclosure save(Enclosure enclosure);

    /** Updates every field of an existing enclosure row. */
    void update(Enclosure enclosure);

    /** Deletes the enclosure with the given id. */
    void delete(int id);
}
