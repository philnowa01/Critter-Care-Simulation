package com.crittercare.service;

import com.crittercare.model.Animal;
import com.crittercare.model.Bird;
import com.crittercare.model.Enclosure;
import com.crittercare.model.HabitatType;
import com.crittercare.model.Mammal;
import com.crittercare.model.Reptile;
import com.crittercare.repository.AnimalRepository;
import com.crittercare.repository.EnclosureRepository;

import java.util.List;
import java.util.Optional;

/**
 * Business logic for enclosure management.
 *
 * Rules enforced here:
 *  - An enclosure must have a non-blank name, a valid habitat type, and
 *    capacity ≥ 1.
 *  - An animal can only be assigned to an enclosure it is biologically
 *    compatible with (validateCompatibility).
 *  - An enclosure cannot be deleted while it still holds animals.
 *  - Assigning an animal updates both the animal row and clears the
 *    previous enclosure's occupancy count.
 */
public class EnclosureService {

    private final EnclosureRepository enclosureRepo;
    private final AnimalRepository    animalRepo;
    private final AlertService        alertService;

    public EnclosureService(EnclosureRepository enclosureRepo,
                            AnimalRepository animalRepo) {
        this(enclosureRepo, animalRepo, null);
    }

    public EnclosureService(EnclosureRepository enclosureRepo,
                            AnimalRepository animalRepo,
                            AlertService alertService) {
        this.enclosureRepo = enclosureRepo;
        this.animalRepo    = animalRepo;
        this.alertService  = alertService;
    }

    // ── Queries ──────────────────────────────────────────────────────────────

    /** Returns every enclosure ordered by DB id. */
    public List<Enclosure> getAllEnclosures() {
        return enclosureRepo.findAll();
    }

    /** Returns the enclosure with the given id, or empty if not found. */
    public Optional<Enclosure> getEnclosureById(int id) {
        return enclosureRepo.findById(id);
    }

    /** Returns all enclosures of a specific habitat type. */
    public List<Enclosure> getEnclosuresByHabitat(HabitatType type) {
        return enclosureRepo.findByHabitatType(type);
    }

    /**
     * Returns a human-readable status string for an enclosure.
     * Used by the Enclosure table "Status" column.
     */
    public String getEnclosureStatus(int id) {
        return enclosureRepo.findById(id)
                .map(Enclosure::getStatus)
                .orElse("Unknown");
    }

    // ── Commands ─────────────────────────────────────────────────────────────

    /**
     * Validates and persists a new enclosure.
     *
     * @return the persisted enclosure with its generated id set
     * @throws IllegalArgumentException if validation fails
     */
    public Enclosure createEnclosure(Enclosure enclosure) {
        validateEnclosure(enclosure);
        return enclosureRepo.save(enclosure);
    }

    /**
     * Validates and persists updates to an existing enclosure.
     *
     * @throws IllegalArgumentException if validation fails
     * @throws IllegalStateException    if the enclosure does not exist
     */
    public void updateEnclosure(Enclosure enclosure) {
        validateEnclosure(enclosure);
        enclosureRepo.findById(enclosure.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Cannot update — enclosure id "
                        + enclosure.getId() + " not found."));
        enclosureRepo.update(enclosure);
    }

    /**
     * Deletes an enclosure only if it contains no animals.
     *
     * @throws IllegalStateException if the enclosure still has animals
     *                               or does not exist
     */
    public void deleteEnclosure(int id) {
        Enclosure enc = enclosureRepo.findById(id)
                .orElseThrow(() -> new IllegalStateException(
                        "Cannot delete — enclosure id " + id + " not found."));

        if (!enc.getAnimalIds().isEmpty()) {
            throw new IllegalStateException(
                    "Cannot delete '" + enc.getName()
                    + "' — it still contains "
                    + enc.getOccupancy() + " animal(s). "
                    + "Reassign or remove them first.");
        }
        enclosureRepo.delete(id);
    }

    /**
     * Assigns an animal to an enclosure after validating:
     *  1. Both animal and enclosure exist.
     *  2. The enclosure is not full.
     *  3. The animal is biologically compatible with the habitat.
     *
     * @throws IllegalStateException    if animal or enclosure not found,
     *                                  or if the enclosure is full
     * @throws IllegalArgumentException if the animal is incompatible with
     *                                  the habitat type
     */
    public void assignAnimalToEnclosure(int animalId, int enclosureId) {
        Animal animal = animalRepo.findById(animalId)
                .orElseThrow(() -> new IllegalStateException(
                        "Animal id " + animalId + " not found."));

        Enclosure enclosure = enclosureRepo.findById(enclosureId)
                .orElseThrow(() -> new IllegalStateException(
                        "Enclosure id " + enclosureId + " not found."));

        if (enclosure.isFull()) {
            throw new IllegalStateException(
                    "Enclosure '" + enclosure.getName() + "' is at full capacity ("
                    + enclosure.getCapacity() + " animals).");
        }

        if (!validateCompatibility(animal, enclosure)) {
            throw new IllegalArgumentException(
                    animal.getName() + " (" + animal.getType() + ") is not compatible "
                    + "with " + enclosure.getHabitatType().getDisplayName()
                    + " habitat.");
        }

        animal.setEnclosureId(enclosureId);
        animalRepo.update(animal);
    }

    /**
     * Marks an enclosure as cleaned: resets cleanliness to 100 and
     * updates lastCleaned to now.
     */
    public void cleanEnclosure(int enclosureId) {
        Enclosure enc = enclosureRepo.findById(enclosureId)
                .orElseThrow(() -> new IllegalStateException(
                        "Enclosure id " + enclosureId + " not found."));
        enc.clean();
        enclosureRepo.update(enc);
        if (alertService != null) {
            alertService.resolveAlertsForEnclosure(enclosureId);
        }
    }

    // ── Compatibility rules ──────────────────────────────────────────────────

    /**
     * Returns true when the animal is biologically compatible with the
     * enclosure's habitat type.
     *
     * Compatibility matrix:
     *  Reptile   → REPTILE_HOUSE only
     *  Bird      → flightless (canFly=false) → ARCTIC or AQUATIC
     *              flying (canFly=true)       → AVIARY or FOREST
     *  Mammal    → SAVANNAH, FOREST, or AQUATIC
     */
    public boolean validateCompatibility(Animal animal, Enclosure enclosure) {
        HabitatType habitat = enclosure.getHabitatType();

        if (animal instanceof Reptile) {
            return habitat == HabitatType.REPTILE_HOUSE;
        }

        if (animal instanceof Bird bird) {
            if (!bird.isCanFly()) {
                return habitat == HabitatType.ARCTIC
                        || habitat == HabitatType.AQUATIC;
            }
            return habitat == HabitatType.AVIARY
                    || habitat == HabitatType.FOREST;
        }

        if (animal instanceof Mammal) {
            return habitat == HabitatType.SAVANNAH
                    || habitat == HabitatType.FOREST
                    || habitat == HabitatType.AQUATIC;
        }

        // Unknown type — allow to be safe
        return true;
    }

    // ── Validation ───────────────────────────────────────────────────────────

    /**
     * Validates the core fields every enclosure must have.
     *
     * @throws IllegalArgumentException describing what is wrong
     */
    public void validateEnclosure(Enclosure enclosure) {
        if (enclosure == null) {
            throw new IllegalArgumentException("Enclosure must not be null.");
        }
        if (enclosure.getName() == null || enclosure.getName().isBlank()) {
            throw new IllegalArgumentException("Enclosure name must not be blank.");
        }
        if (enclosure.getHabitatType() == null) {
            throw new IllegalArgumentException("Enclosure must have a habitat type.");
        }
        if (enclosure.getCapacity() < 1) {
            throw new IllegalArgumentException(
                    "Enclosure capacity must be at least 1.");
        }
        if (enclosure.getCleanliness() < 0 || enclosure.getCleanliness() > 100) {
            throw new IllegalArgumentException(
                    "Cleanliness must be between 0 and 100.");
        }
    }
}
