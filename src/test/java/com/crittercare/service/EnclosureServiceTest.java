package com.crittercare.service;

import com.crittercare.model.Bird;
import com.crittercare.model.Enclosure;
import com.crittercare.model.HabitatType;
import com.crittercare.model.Mammal;
import com.crittercare.model.Reptile;
import com.crittercare.repository.AnimalRepository;
import com.crittercare.repository.EnclosureRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EnclosureService.
 *
 * The compatibility matrix (validateCompatibility) is tested exhaustively
 * because it drives the animal-assignment business rule.
 */
@ExtendWith(MockitoExtension.class)
class EnclosureServiceTest {

    @Mock EnclosureRepository enclosureRepo;
    @Mock AnimalRepository    animalRepo;

    EnclosureService service;

    @BeforeEach
    void setUp() {
        service = new EnclosureService(enclosureRepo, animalRepo);
    }

    // ── createEnclosure ──────────────────────────────────────────────────────

    @Test
    void createEnclosure_valid_calls_save_and_returns_enclosure() {
        Enclosure enc = new Enclosure("Savannah A", HabitatType.SAVANNAH, 10);
        when(enclosureRepo.save(enc)).thenReturn(enc);

        Enclosure result = service.createEnclosure(enc);

        verify(enclosureRepo).save(enc);
        assertSame(enc, result);
    }

    @Test
    void createEnclosure_null_throws_illegal_argument() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createEnclosure(null));
    }

    @Test
    void createEnclosure_blank_name_throws_illegal_argument() {
        Enclosure enc = new Enclosure("  ", HabitatType.SAVANNAH, 10);
        assertThrows(IllegalArgumentException.class,
                () -> service.createEnclosure(enc));
    }

    @Test
    void createEnclosure_null_habitat_throws_illegal_argument() {
        Enclosure enc = new Enclosure("Test Zone", null, 10);
        assertThrows(IllegalArgumentException.class,
                () -> service.createEnclosure(enc));
    }

    @Test
    void createEnclosure_zero_capacity_throws_illegal_argument() {
        Enclosure enc = new Enclosure("Test Zone", HabitatType.SAVANNAH, 0);
        assertThrows(IllegalArgumentException.class,
                () -> service.createEnclosure(enc));
    }

    // ── updateEnclosure ──────────────────────────────────────────────────────

    @Test
    void updateEnclosure_existing_calls_repo_update() {
        Enclosure enc = new Enclosure("Savannah A", HabitatType.SAVANNAH, 10);
        enc.setId(1);
        when(enclosureRepo.findById(1)).thenReturn(Optional.of(enc));

        service.updateEnclosure(enc);

        verify(enclosureRepo).update(enc);
    }

    @Test
    void updateEnclosure_not_found_throws_illegal_state() {
        Enclosure enc = new Enclosure("Ghost Zone", HabitatType.FOREST, 5);
        enc.setId(999);
        when(enclosureRepo.findById(999)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> service.updateEnclosure(enc));
        verify(enclosureRepo, never()).update(any());
    }

    // ── deleteEnclosure ──────────────────────────────────────────────────────

    @Test
    void deleteEnclosure_empty_enclosure_calls_repo_delete() {
        Enclosure enc = new Enclosure("Savannah A", HabitatType.SAVANNAH, 10);
        enc.setId(1);  // no animals → animalIds is empty
        when(enclosureRepo.findById(1)).thenReturn(Optional.of(enc));

        service.deleteEnclosure(1);

        verify(enclosureRepo).delete(1);
    }

    @Test
    void deleteEnclosure_not_found_throws_illegal_state() {
        when(enclosureRepo.findById(99)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> service.deleteEnclosure(99));
        verify(enclosureRepo, never()).delete(anyInt());
    }

    @Test
    void deleteEnclosure_with_animals_still_inside_throws_illegal_state() {
        Enclosure enc = new Enclosure("Savannah A", HabitatType.SAVANNAH, 10);
        enc.setId(1);
        enc.addAnimal(42);  // occupancy = 1 → not empty
        when(enclosureRepo.findById(1)).thenReturn(Optional.of(enc));

        assertThrows(IllegalStateException.class,
                () -> service.deleteEnclosure(1));
        verify(enclosureRepo, never()).delete(anyInt());
    }

    // ── cleanEnclosure ───────────────────────────────────────────────────────

    @Test
    void cleanEnclosure_resets_cleanliness_to_100_and_calls_update() {
        Enclosure enc = new Enclosure("Dirty Zone", HabitatType.FOREST, 5);
        enc.setId(1);
        enc.setCleanliness(30.0);   // dirty before clean
        when(enclosureRepo.findById(1)).thenReturn(Optional.of(enc));

        service.cleanEnclosure(1);

        assertEquals(100.0, enc.getCleanliness(), 1e-9);
        verify(enclosureRepo).update(enc);
    }

    @Test
    void cleanEnclosure_not_found_throws_illegal_state() {
        when(enclosureRepo.findById(99)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> service.cleanEnclosure(99));
        verify(enclosureRepo, never()).update(any());
    }

    // ── assignAnimalToEnclosure ──────────────────────────────────────────────

    @Test
    void assignAnimalToEnclosure_compatible_sets_enclosure_id_and_calls_update() {
        Mammal mammal = new Mammal("Leo", "Lion", 5);
        mammal.setId(1);
        Enclosure savannah = new Enclosure("Savannah A", HabitatType.SAVANNAH, 10);
        savannah.setId(1);
        when(animalRepo.findById(1)).thenReturn(Optional.of(mammal));
        when(enclosureRepo.findById(1)).thenReturn(Optional.of(savannah));

        service.assignAnimalToEnclosure(1, 1);

        assertEquals(1, mammal.getEnclosureId());
        verify(animalRepo).update(mammal);
    }

    @Test
    void assignAnimalToEnclosure_full_enclosure_throws_illegal_state() {
        Mammal mammal = new Mammal("Leo", "Lion", 5);
        mammal.setId(1);
        Enclosure enc = new Enclosure("Tiny Pen", HabitatType.SAVANNAH, 1);
        enc.setId(1);
        enc.addAnimal(99);  // capacity=1, occupancy=1 → full
        when(animalRepo.findById(1)).thenReturn(Optional.of(mammal));
        when(enclosureRepo.findById(1)).thenReturn(Optional.of(enc));

        assertThrows(IllegalStateException.class,
                () -> service.assignAnimalToEnclosure(1, 1));
    }

    @Test
    void assignAnimalToEnclosure_incompatible_throws_illegal_argument() {
        Reptile reptile = new Reptile("Rex", "Komodo Dragon", 4);
        reptile.setId(2);
        Enclosure savannah = new Enclosure("Savannah", HabitatType.SAVANNAH, 10);
        savannah.setId(2);
        when(animalRepo.findById(2)).thenReturn(Optional.of(reptile));
        when(enclosureRepo.findById(2)).thenReturn(Optional.of(savannah));

        assertThrows(IllegalArgumentException.class,
                () -> service.assignAnimalToEnclosure(2, 2));
    }

    // ── validateCompatibility: Reptile ───────────────────────────────────────

    @Test
    void reptile_is_compatible_with_reptile_house() {
        assertTrue(service.validateCompatibility(
                new Reptile("Rex", "Iguana", 3),
                new Enclosure("Reptile House", HabitatType.REPTILE_HOUSE, 5)));
    }

    @Test
    void reptile_is_incompatible_with_savannah() {
        assertFalse(service.validateCompatibility(
                new Reptile("Rex", "Iguana", 3),
                new Enclosure("Savannah", HabitatType.SAVANNAH, 10)));
    }

    @Test
    void reptile_is_incompatible_with_forest() {
        assertFalse(service.validateCompatibility(
                new Reptile("Rex", "Iguana", 3),
                new Enclosure("Forest", HabitatType.FOREST, 8)));
    }

    // ── validateCompatibility: Bird (flying) ─────────────────────────────────

    @Test
    void flying_bird_is_compatible_with_aviary() {
        Bird b = new Bird("Eagle", "Bald Eagle", 4, 180.0, true);
        assertTrue(service.validateCompatibility(b,
                new Enclosure("Aviary", HabitatType.AVIARY, 10)));
    }

    @Test
    void flying_bird_is_compatible_with_forest() {
        Bird b = new Bird("Macaw", "Scarlet Macaw", 3, 50.0, true);
        assertTrue(service.validateCompatibility(b,
                new Enclosure("Forest", HabitatType.FOREST, 8)));
    }

    @Test
    void flying_bird_is_incompatible_with_arctic() {
        Bird b = new Bird("Eagle", "Bald Eagle", 4, 180.0, true);
        assertFalse(service.validateCompatibility(b,
                new Enclosure("Arctic", HabitatType.ARCTIC, 6)));
    }

    @Test
    void flying_bird_is_incompatible_with_aquatic() {
        Bird b = new Bird("Eagle", "Bald Eagle", 4, 180.0, true);
        assertFalse(service.validateCompatibility(b,
                new Enclosure("Aquatic", HabitatType.AQUATIC, 8)));
    }

    // ── validateCompatibility: Bird (flightless) ─────────────────────────────

    @Test
    void flightless_bird_is_compatible_with_arctic() {
        Bird b = new Bird("Pingu", "African Penguin", 2, 30.0, false);
        assertTrue(service.validateCompatibility(b,
                new Enclosure("Arctic", HabitatType.ARCTIC, 6)));
    }

    @Test
    void flightless_bird_is_compatible_with_aquatic() {
        Bird b = new Bird("Pingu", "African Penguin", 2, 30.0, false);
        assertTrue(service.validateCompatibility(b,
                new Enclosure("Aquatic", HabitatType.AQUATIC, 8)));
    }

    @Test
    void flightless_bird_is_incompatible_with_aviary() {
        Bird b = new Bird("Pingu", "African Penguin", 2, 30.0, false);
        assertFalse(service.validateCompatibility(b,
                new Enclosure("Aviary", HabitatType.AVIARY, 10)));
    }

    @Test
    void flightless_bird_is_incompatible_with_savannah() {
        Bird b = new Bird("Pingu", "African Penguin", 2, 30.0, false);
        assertFalse(service.validateCompatibility(b,
                new Enclosure("Savannah", HabitatType.SAVANNAH, 10)));
    }

    // ── validateCompatibility: Mammal ────────────────────────────────────────

    @Test
    void mammal_is_compatible_with_savannah() {
        assertTrue(service.validateCompatibility(
                new Mammal("Leo", "Lion", 5),
                new Enclosure("Savannah", HabitatType.SAVANNAH, 10)));
    }

    @Test
    void mammal_is_compatible_with_forest() {
        assertTrue(service.validateCompatibility(
                new Mammal("Bear", "Brown Bear", 6),
                new Enclosure("Forest", HabitatType.FOREST, 8)));
    }

    @Test
    void mammal_is_compatible_with_aquatic() {
        assertTrue(service.validateCompatibility(
                new Mammal("Flipper", "Dolphin", 4),
                new Enclosure("Aquatic", HabitatType.AQUATIC, 8)));
    }

    @Test
    void mammal_is_incompatible_with_reptile_house() {
        assertFalse(service.validateCompatibility(
                new Mammal("Leo", "Lion", 5),
                new Enclosure("Reptile House", HabitatType.REPTILE_HOUSE, 5)));
    }

    @Test
    void mammal_is_incompatible_with_arctic() {
        assertFalse(service.validateCompatibility(
                new Mammal("Leo", "Lion", 5),
                new Enclosure("Arctic", HabitatType.ARCTIC, 6)));
    }
}
