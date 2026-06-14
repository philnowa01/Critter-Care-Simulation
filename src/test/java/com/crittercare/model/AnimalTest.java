package com.crittercare.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Animal.tick() (Template Method pattern), vital-stat thresholds,
 * setter clamping, and subtype-specific behaviour.
 *
 * No mocks are needed — these are pure domain-model tests.
 */
class AnimalTest {

    // ── Constructor defaults ─────────────────────────────────────────────────

    @Test
    void constructor_sets_healthy_defaults() {
        Mammal m = new Mammal("Leo", "Lion", 5);
        assertEquals(100.0, m.getHealth(),       1e-9);
        assertEquals(0.0,   m.getHunger(),       1e-9);
        assertEquals(100.0, m.getHydration(),    1e-9);
        assertEquals(80.0,  m.getActivityLevel(), 1e-9);
    }

    // ── getType ──────────────────────────────────────────────────────────────

    @Test
    void mammal_returns_correct_type() {
        assertEquals("MAMMAL", new Mammal().getType());
    }

    @Test
    void bird_returns_correct_type() {
        assertEquals("BIRD", new Bird().getType());
    }

    @Test
    void reptile_returns_correct_type() {
        assertEquals("REPTILE", new Reptile().getType());
    }

    // ── makeSound ────────────────────────────────────────────────────────────

    @Test
    void makeSound_returns_non_null_for_every_subtype() {
        assertNotNull(new Mammal("A", "Giraffe",       3).makeSound());
        assertNotNull(new Bird("B",   "Bald Eagle",    2).makeSound());
        assertNotNull(new Reptile("C","Komodo Dragon", 6).makeSound());
    }

    // ── tick: hunger rates differ per subtype ────────────────────────────────

    @Test
    void tick_mammal_hunger_increases_at_4_per_hour() {
        Mammal m = new Mammal("Leo", "Lion", 5);
        m.tick(1.0);
        assertEquals(4.0, m.getHunger(), 1e-9);
    }

    @Test
    void tick_bird_hunger_increases_at_5_per_hour() {
        Bird b = new Bird("Polly", "Scarlet Macaw", 2);
        b.tick(1.0);
        assertEquals(5.0, b.getHunger(), 1e-9);
    }

    @Test
    void tick_reptile_hunger_increases_at_1_5_per_hour() {
        Reptile r = new Reptile("Rex", "Nile Crocodile", 10);
        r.tick(1.0);
        assertEquals(1.5, r.getHunger(), 1e-9);
    }

    // ── tick: hydration decay ────────────────────────────────────────────────

    @Test
    void tick_mammal_hydration_decreases_at_2_5_per_hour() {
        Mammal m = new Mammal("Leo", "Lion", 5);
        m.tick(1.0);
        assertEquals(97.5, m.getHydration(), 1e-9);
    }

    @Test
    void tick_reptile_hydration_decreases_at_1_per_hour() {
        Reptile r = new Reptile("Iggy", "Green Iguana", 4);
        r.tick(1.0);
        assertEquals(99.0, r.getHydration(), 1e-9);
    }

    @Test
    void tick_bird_hydration_decreases_at_3_per_hour() {
        Bird b = new Bird("Polly", "Parrot", 1);
        b.tick(1.0);
        assertEquals(97.0, b.getHydration(), 1e-9);
    }

    // ── tick: health only degrades under critical conditions ─────────────────

    @Test
    void tick_health_unchanged_when_animal_is_well_fed_and_hydrated() {
        // hunger after tick(1.0) = 4 (<75), hydration = 97.5 (>20) — no decay
        Mammal m = new Mammal("Leo", "Lion", 5);
        m.tick(1.0);
        assertEquals(100.0, m.getHealth(), 1e-9);
    }

    @Test
    void tick_health_decreases_when_hunger_is_critical() {
        Mammal m = new Mammal("Leo", "Lion", 5);
        m.setHunger(80.0);          // ≥ 75 → isHungerCritical = true
        m.tick(1.0);
        // health = 100 − 2.0 × 1.0 = 98
        assertEquals(98.0, m.getHealth(), 1e-9);
    }

    @Test
    void tick_health_decreases_when_dehydrated() {
        Mammal m = new Mammal("Leo", "Lion", 5);
        m.setHydration(15.0);       // ≤ 20 → isDehydrationCritical = true
        m.tick(1.0);
        assertEquals(98.0, m.getHealth(), 1e-9);
    }

    @Test
    void tick_reptile_health_uses_lower_decay_rate() {
        Reptile r = new Reptile("Rex", "Nile Crocodile", 10);
        r.setHunger(80.0);          // critical → health decays
        r.tick(1.0);
        // health = 100 − 1.5 × 1.0 = 98.5
        assertEquals(98.5, r.getHealth(), 1e-9);
    }

    // ── tick: activity drops when hungry ────────────────────────────────────

    @Test
    void tick_activity_drops_when_hunger_exceeds_70() {
        Mammal m = new Mammal("Leo", "Lion", 5);
        m.setHunger(75.0);          // > 70 already; after tick = 79 (still > 70)
        m.tick(1.0);
        // activityLevel = 80 − 1.5 = 78.5
        assertEquals(78.5, m.getActivityLevel(), 1e-9);
    }

    @Test
    void tick_activity_unchanged_when_hunger_under_70() {
        Mammal m = new Mammal("Leo", "Lion", 5);
        // hunger starts at 0; after tick(1.0) = 4 (<70)
        m.tick(1.0);
        assertEquals(80.0, m.getActivityLevel(), 1e-9);
    }

    // ── tick: stat clamping at boundaries ───────────────────────────────────

    @Test
    void tick_hunger_clamps_at_100() {
        Mammal m = new Mammal("Leo", "Lion", 5);
        m.setHunger(98.0);
        m.tick(1.0);                // 98 + 4 = 102 → clamped to 100
        assertEquals(100.0, m.getHunger(), 1e-9);
    }

    @Test
    void tick_hydration_clamps_at_zero() {
        Mammal m = new Mammal("Leo", "Lion", 5);
        m.setHydration(1.0);
        m.tick(1.0);                // 1 − 2.5 = −1.5 → clamped to 0
        assertEquals(0.0, m.getHydration(), 1e-9);
    }

    // ── Setter clamping ──────────────────────────────────────────────────────

    @Test
    void setHealth_clamps_below_zero() {
        Mammal m = new Mammal("Leo", "Lion", 5);
        m.setHealth(-10.0);
        assertEquals(0.0, m.getHealth(), 1e-9);
    }

    @Test
    void setHealth_clamps_above_100() {
        Mammal m = new Mammal("Leo", "Lion", 5);
        m.setHealth(110.0);
        assertEquals(100.0, m.getHealth(), 1e-9);
    }

    @Test
    void setHunger_clamps_above_100() {
        Mammal m = new Mammal("Leo", "Lion", 5);
        m.setHunger(110.0);
        assertEquals(100.0, m.getHunger(), 1e-9);
    }

    @Test
    void setHydration_clamps_below_zero() {
        Mammal m = new Mammal("Leo", "Lion", 5);
        m.setHydration(-5.0);
        assertEquals(0.0, m.getHydration(), 1e-9);
    }

    // ── isHungerCritical (threshold ≥ 75) ───────────────────────────────────

    @Test
    void isHungerCritical_false_just_below_threshold() {
        Mammal m = new Mammal("Leo", "Lion", 5);
        m.setHunger(74.9);
        assertFalse(m.isHungerCritical());
    }

    @Test
    void isHungerCritical_true_at_threshold() {
        Mammal m = new Mammal("Leo", "Lion", 5);
        m.setHunger(75.0);
        assertTrue(m.isHungerCritical());
    }

    // ── isHealthCritical (threshold ≤ 30) ───────────────────────────────────

    @Test
    void isHealthCritical_false_just_above_threshold() {
        Mammal m = new Mammal("Leo", "Lion", 5);
        m.setHealth(30.1);
        assertFalse(m.isHealthCritical());
    }

    @Test
    void isHealthCritical_true_at_threshold() {
        Mammal m = new Mammal("Leo", "Lion", 5);
        m.setHealth(30.0);
        assertTrue(m.isHealthCritical());
    }

    // ── isDehydrationCritical (threshold ≤ 20) ──────────────────────────────

    @Test
    void isDehydrationCritical_false_just_above_threshold() {
        Mammal m = new Mammal("Leo", "Lion", 5);
        m.setHydration(20.1);
        assertFalse(m.isDehydrationCritical());
    }

    @Test
    void isDehydrationCritical_true_at_threshold() {
        Mammal m = new Mammal("Leo", "Lion", 5);
        m.setHydration(20.0);
        assertTrue(m.isDehydrationCritical());
    }

    // ── requiresAttention (hunger ≥ 60 || health ≤ 50 || hydration ≤ 30) ───

    @Test
    void requiresAttention_false_when_all_stats_healthy() {
        // defaults: hunger=0, health=100, hydration=100
        assertFalse(new Mammal("Leo", "Lion", 5).requiresAttention());
    }

    @Test
    void requiresAttention_true_at_hunger_warning_threshold() {
        Mammal m = new Mammal("Leo", "Lion", 5);
        m.setHunger(60.0);
        assertTrue(m.requiresAttention());
    }

    @Test
    void requiresAttention_true_at_health_warning_threshold() {
        Mammal m = new Mammal("Leo", "Lion", 5);
        m.setHealth(50.0);
        assertTrue(m.requiresAttention());
    }

    @Test
    void requiresAttention_true_at_hydration_warning_threshold() {
        Mammal m = new Mammal("Leo", "Lion", 5);
        m.setHydration(30.0);
        assertTrue(m.requiresAttention());
    }

    @Test
    void requiresAttention_false_just_outside_all_warning_thresholds() {
        Mammal m = new Mammal("Leo", "Lion", 5);
        m.setHunger(59.9);
        m.setHealth(50.1);
        m.setHydration(30.1);
        assertFalse(m.requiresAttention());
    }

    // ── Bird-specific ────────────────────────────────────────────────────────

    @Test
    void bird_can_fly_is_true_by_default() {
        assertTrue(new Bird("Polly", "Parrot", 1).isCanFly());
    }

    @Test
    void bird_full_constructor_sets_flightless() {
        Bird b = new Bird("Pingu", "African Penguin", 2, 30.0, false);
        assertFalse(b.isCanFly());
    }

    // ── Reptile-specific ─────────────────────────────────────────────────────

    @Test
    void reptile_is_not_venomous_by_default() {
        assertFalse(new Reptile("Iggy", "Green Iguana", 3).isVenomous());
    }

    @Test
    void reptile_full_constructor_sets_venomous() {
        Reptile r = new Reptile("King", "King Cobra", 5, true, 8.0);
        assertTrue(r.isVenomous());
        assertEquals(8.0, r.getUvbRequirement(), 1e-9);
    }
}
