package com.crittercare.service;

import com.crittercare.model.Animal;
import com.crittercare.model.Enclosure;
import com.crittercare.model.HabitatType;
import com.crittercare.model.Mammal;
import com.crittercare.repository.AnimalRepository;
import com.crittercare.repository.EnclosureRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AnimalService.
 *
 * Repositories are mocked so no database or JavaFX toolkit is required.
 * Each test exercises one slice of the service's business logic.
 */
@ExtendWith(MockitoExtension.class)
class AnimalServiceTest {

    @Mock AnimalRepository    animalRepo;
    @Mock EnclosureRepository enclosureRepo;

    AnimalService service;

    @BeforeEach
    void setUp() {
        service = new AnimalService(animalRepo, enclosureRepo);
    }

    // ── addAnimal ────────────────────────────────────────────────────────────

    @Test
    void addAnimal_valid_no_enclosure_calls_save_and_returns_animal() {
        Mammal animal = new Mammal("Leo", "Lion", 5);
        when(animalRepo.save(animal)).thenReturn(animal);

        Animal result = service.addAnimal(animal, 0);

        verify(animalRepo).save(animal);
        assertSame(animal, result);
    }

    @Test
    void addAnimal_valid_enclosure_id_sets_enclosure_on_animal() {
        Mammal animal = new Mammal("Leo", "Lion", 5);
        Enclosure enc = new Enclosure("Savannah", HabitatType.SAVANNAH, 10);
        enc.setId(1);
        when(enclosureRepo.findById(1)).thenReturn(Optional.of(enc));
        when(animalRepo.save(animal)).thenReturn(animal);

        service.addAnimal(animal, 1);

        assertEquals(1, animal.getEnclosureId());
        verify(animalRepo).save(animal);
    }

    @Test
    void addAnimal_invalid_enclosure_id_throws_illegal_argument() {
        Mammal animal = new Mammal("Leo", "Lion", 5);
        when(enclosureRepo.findById(99)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.addAnimal(animal, 99));
        verify(animalRepo, never()).save(any());
    }

    @Test
    void addAnimal_null_throws_illegal_argument() {
        assertThrows(IllegalArgumentException.class,
                () -> service.addAnimal(null, 0));
    }

    @Test
    void addAnimal_blank_name_throws_illegal_argument() {
        Mammal animal = new Mammal("", "Lion", 5);
        assertThrows(IllegalArgumentException.class,
                () -> service.addAnimal(animal, 0));
    }

    @Test
    void addAnimal_blank_species_throws_illegal_argument() {
        Mammal animal = new Mammal("Leo", "   ", 5);
        assertThrows(IllegalArgumentException.class,
                () -> service.addAnimal(animal, 0));
    }

    @Test
    void addAnimal_negative_age_throws_illegal_argument() {
        Mammal animal = new Mammal("Leo", "Lion", -1);
        assertThrows(IllegalArgumentException.class,
                () -> service.addAnimal(animal, 0));
    }

    // ── updateAnimal ─────────────────────────────────────────────────────────

    @Test
    void updateAnimal_existing_animal_calls_repo_update() {
        Mammal animal = new Mammal("Leo", "Lion", 5);
        animal.setId(1);
        when(animalRepo.findById(1)).thenReturn(Optional.of(animal));

        service.updateAnimal(animal);

        verify(animalRepo).update(animal);
    }

    @Test
    void updateAnimal_not_found_throws_illegal_state() {
        Mammal animal = new Mammal("Leo", "Lion", 5);
        animal.setId(999);
        when(animalRepo.findById(999)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> service.updateAnimal(animal));
        verify(animalRepo, never()).update(any());
    }

    // ── removeAnimal ─────────────────────────────────────────────────────────

    @Test
    void removeAnimal_existing_calls_repo_delete() {
        Mammal animal = new Mammal("Leo", "Lion", 5);
        animal.setId(1);
        when(animalRepo.findById(1)).thenReturn(Optional.of(animal));

        service.removeAnimal(1);

        verify(animalRepo).delete(1);
    }

    @Test
    void removeAnimal_not_found_throws_illegal_state() {
        when(animalRepo.findById(99)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> service.removeAnimal(99));
        verify(animalRepo, never()).delete(anyInt());
    }

    // ── feedAnimal ───────────────────────────────────────────────────────────

    @Test
    void feedAnimal_resets_hunger_to_zero_and_calls_update() {
        Mammal animal = new Mammal("Leo", "Lion", 5);
        animal.setId(1);
        animal.setHunger(80.0);
        animal.setHydration(80.0);
        when(animalRepo.findById(1)).thenReturn(Optional.of(animal));

        service.feedAnimal(1);

        assertEquals(0.0, animal.getHunger(), 1e-9);
        verify(animalRepo).update(animal);
    }

    @Test
    void feedAnimal_boosts_low_hydration_to_50() {
        Mammal animal = new Mammal("Leo", "Lion", 5);
        animal.setId(1);
        animal.setHydration(30.0);  // below 50 → should be boosted
        when(animalRepo.findById(1)).thenReturn(Optional.of(animal));

        service.feedAnimal(1);

        assertEquals(50.0, animal.getHydration(), 1e-9);
    }

    @Test
    void feedAnimal_does_not_reduce_adequate_hydration() {
        Mammal animal = new Mammal("Leo", "Lion", 5);
        animal.setId(1);
        animal.setHydration(70.0);  // above 50 → unchanged
        when(animalRepo.findById(1)).thenReturn(Optional.of(animal));

        service.feedAnimal(1);

        assertEquals(70.0, animal.getHydration(), 1e-9);
    }

    @Test
    void feedAnimal_not_found_throws_illegal_state() {
        when(animalRepo.findById(99)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> service.feedAnimal(99));
    }

    // ── performHealthCheck ───────────────────────────────────────────────────

    @Test
    void performHealthCheck_boosts_health_by_10() {
        Mammal animal = new Mammal("Leo", "Lion", 5);
        animal.setId(1);
        animal.setHealth(60.0);
        when(animalRepo.findById(1)).thenReturn(Optional.of(animal));

        service.performHealthCheck(1);

        assertEquals(70.0, animal.getHealth(), 1e-9);
        verify(animalRepo).update(animal);
    }

    @Test
    void performHealthCheck_caps_health_at_100() {
        Mammal animal = new Mammal("Leo", "Lion", 5);
        animal.setId(1);
        animal.setHealth(95.0);
        when(animalRepo.findById(1)).thenReturn(Optional.of(animal));

        service.performHealthCheck(1);

        assertEquals(100.0, animal.getHealth(), 1e-9);
    }

    @Test
    void performHealthCheck_not_found_throws_illegal_state() {
        when(animalRepo.findById(99)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> service.performHealthCheck(99));
    }

    // ── getAnimalsRequiringAttention ─────────────────────────────────────────

    @Test
    void getAnimalsRequiringAttention_returns_only_animals_outside_normal_range() {
        Mammal healthy = new Mammal("Healthy", "Lion", 5);  // defaults: all good

        Mammal hungry = new Mammal("Hungry", "Tiger", 3);
        hungry.setHunger(65.0);  // ≥ 60 → requires attention

        when(animalRepo.findAll()).thenReturn(List.of(healthy, hungry));

        List<Animal> result = service.getAnimalsRequiringAttention();

        assertEquals(1, result.size());
        assertSame(hungry, result.get(0));
    }

    @Test
    void getAnimalsRequiringAttention_returns_empty_when_all_healthy() {
        Mammal a = new Mammal("A", "Lion", 5);
        Mammal b = new Mammal("B", "Tiger", 3);
        when(animalRepo.findAll()).thenReturn(List.of(a, b));

        assertTrue(service.getAnimalsRequiringAttention().isEmpty());
    }

    // ── validateAnimal ───────────────────────────────────────────────────────

    @Test
    void validateAnimal_null_throws_illegal_argument() {
        assertThrows(IllegalArgumentException.class,
                () -> service.validateAnimal(null));
    }

    @Test
    void validateAnimal_whitespace_name_throws_illegal_argument() {
        Mammal m = new Mammal("   ", "Lion", 5);
        assertThrows(IllegalArgumentException.class,
                () -> service.validateAnimal(m));
    }

    @Test
    void validateAnimal_whitespace_species_throws_illegal_argument() {
        Mammal m = new Mammal("Leo", "  ", 5);
        assertThrows(IllegalArgumentException.class,
                () -> service.validateAnimal(m));
    }

    @Test
    void validateAnimal_negative_age_throws_illegal_argument() {
        Mammal m = new Mammal("Leo", "Lion", -1);
        assertThrows(IllegalArgumentException.class,
                () -> service.validateAnimal(m));
    }

    @Test
    void validateAnimal_valid_animal_does_not_throw() {
        Mammal m = new Mammal("Leo", "Lion", 5);
        assertDoesNotThrow(() -> service.validateAnimal(m));
    }
}
