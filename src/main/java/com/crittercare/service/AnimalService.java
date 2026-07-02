package com.crittercare.service;

import com.crittercare.model.Animal;
import com.crittercare.repository.AnimalRepository;
import com.crittercare.repository.EnclosureRepository;

import java.util.List;
import java.util.Optional;

/**
 * Business logic for animal management.
 *
 * Rules enforced here:
 *  - An animal must have a non-blank name and species, and age ≥ 0.
 *  - An animal can only be assigned to an enclosure that exists.
 *  - Removing an animal also clears its enclosure reference in the DB.
 *
 * This class does NOT talk to JavaFX — it is pure Java and can be tested
 * with JUnit without launching the application.
 */
public class AnimalService {

    private final AnimalRepository    animalRepo;
    private final EnclosureRepository enclosureRepo;
    private final AlertService        alertService;

    public AnimalService(AnimalRepository animalRepo,
                         EnclosureRepository enclosureRepo) {
        this(animalRepo, enclosureRepo, null);
    }

    public AnimalService(AnimalRepository animalRepo,
                         EnclosureRepository enclosureRepo,
                         AlertService alertService) {
        this.animalRepo    = animalRepo;
        this.enclosureRepo = enclosureRepo;
        this.alertService  = alertService;
    }

    // ── Queries ──────────────────────────────────────────────────────────────

    /** Returns every animal in the zoo, ordered by DB id. */
    public List<Animal> getAllAnimals() {
        return animalRepo.findAll();
    }

    /** Returns the animal with the given id, or empty if not found. */
    public Optional<Animal> getAnimalById(int id) {
        return animalRepo.findById(id);
    }

    /** Returns all animals in the given enclosure. */
    public List<Animal> getAnimalsByEnclosure(int enclosureId) {
        return animalRepo.findByEnclosureId(enclosureId);
    }

    /**
     * Returns animals whose health, hunger, or hydration is in a warning
     * state.  These appear in the "Animals Requiring Attention" panel on
     * the Dashboard.
     */
    public List<Animal> getAnimalsRequiringAttention() {
        return animalRepo.findAll().stream()
                .filter(Animal::requiresAttention)
                .toList();
    }

    // ── Commands ─────────────────────────────────────────────────────────────

    /**
     * Validates and persists a new animal, assigning it to the specified
     * enclosure if enclosureId > 0.
     *
     * @return the persisted animal with its generated id set
     * @throws IllegalArgumentException if validation fails or the enclosure
     *                                  does not exist
     */
    public Animal addAnimal(Animal animal, int enclosureId) {
        validateAnimal(animal);

        if (enclosureId > 0) {
            enclosureRepo.findById(enclosureId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Enclosure with id " + enclosureId + " does not exist."));
            animal.setEnclosureId(enclosureId);
        }

        return animalRepo.save(animal);
    }

    /**
     * Validates and persists updates to an existing animal.
     *
     * @throws IllegalArgumentException if validation fails
     * @throws IllegalStateException    if the animal does not exist in the DB
     */
    public void updateAnimal(Animal animal) {
        validateAnimal(animal);
        animalRepo.findById(animal.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Cannot update — animal id " + animal.getId() + " not found."));
        animalRepo.update(animal);
    }

    /**
     * Removes an animal from the zoo.
     *
     * @throws IllegalStateException if the animal does not exist
     */
    public void removeAnimal(int id) {
        animalRepo.findById(id)
                .orElseThrow(() -> new IllegalStateException(
                        "Cannot remove — animal id " + id + " not found."));
        animalRepo.delete(id);
    }

    /**
     * Feeds an animal: resets its hunger to 0 and ensures hydration is at
     * least 50%.  Persists the change immediately.
     */
    public void feedAnimal(int animalId) {
        Animal animal = animalRepo.findById(animalId)
                .orElseThrow(() -> new IllegalStateException(
                        "Animal id " + animalId + " not found."));
        animal.setHunger(0.0);
        if (animal.getHydration() < 50.0) {
            animal.setHydration(50.0);
        }
        animalRepo.update(animal);
        if (alertService != null) {
            alertService.resolveAlertsForAnimal(animalId);
        }
    }

    /**
     * Performs a health check: boosts health by 10 points (capped at 100)
     * and persists the change.
     */
    public void performHealthCheck(int animalId) {
        Animal animal = animalRepo.findById(animalId)
                .orElseThrow(() -> new IllegalStateException(
                        "Animal id " + animalId + " not found."));
        animal.setHealth(Math.min(100.0, animal.getHealth() + 10.0));
        animalRepo.update(animal);
        if (alertService != null) {
            alertService.resolveAlertsForAnimal(animalId);
        }
    }

    // ── Validation ───────────────────────────────────────────────────────────

    /**
     * Validates the core fields every animal must have.
     * Called before every save() or update() call.
     *
     * @throws IllegalArgumentException describing what is wrong
     */
    public void validateAnimal(Animal animal) {
        if (animal == null) {
            throw new IllegalArgumentException("Animal must not be null.");
        }
        if (animal.getName() == null || animal.getName().isBlank()) {
            throw new IllegalArgumentException("Animal name must not be blank.");
        }
        if (animal.getSpecies() == null || animal.getSpecies().isBlank()) {
            throw new IllegalArgumentException("Animal species must not be blank.");
        }
        if (animal.getAge() < 0) {
            throw new IllegalArgumentException("Animal age must be 0 or greater.");
        }
        if (animal.getHealth() < 0 || animal.getHealth() > 100) {
            throw new IllegalArgumentException("Health must be between 0 and 100.");
        }
    }
}
