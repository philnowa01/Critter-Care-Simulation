package com.crittercare.simulation;

import com.crittercare.model.Alert;
import com.crittercare.model.AlertSeverity;
import com.crittercare.model.AlertType;
import com.crittercare.model.Animal;
import com.crittercare.model.Enclosure;
import com.crittercare.model.HabitatType;
import com.crittercare.model.Mammal;
import com.crittercare.service.AlertService;
import com.crittercare.service.AnimalService;
import com.crittercare.service.EnclosureService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Tests for SimulationTick (pure immutable snapshot) and SimulationEngine
 * (lifecycle, listener management, and speed control).
 *
 * Static mocking of Platform.runLater() is intentionally omitted: it
 * requires Mockito's inline mock maker, which is incompatible with the
 * subclass mock maker used in this project for Java 25 compatibility.
 * The tick-notification path is exercised through integration tests instead.
 */
@ExtendWith(MockitoExtension.class)
class SimulationEngineTest {

    // ════════════════════════════════════════════════════════════════════════
    // SimulationTick — pure immutable snapshot (no mocks needed)
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    class SimulationTickTests {

        @Test
        void attentionCount_equals_animals_requiring_attention() {
            Mammal healthy = new Mammal("Healthy", "Lion", 5);

            Mammal hungry = new Mammal("Hungry", "Tiger", 3);
            hungry.setHunger(65.0);  // ≥ 60 → requiresAttention = true

            SimulationTick tick = new SimulationTick(
                    List.of(healthy, hungry), List.of(), List.of(),
                    LocalDateTime.now(), 1);

            assertEquals(1, tick.getAttentionCount());
        }

        @Test
        void cleaningDueCount_equals_enclosures_below_warning_threshold() {
            Enclosure clean = new Enclosure("Clean Zone", HabitatType.SAVANNAH, 10);
            // cleanliness defaults to 100 → isCleaningDue = false

            Enclosure dirty = new Enclosure("Dirty Zone", HabitatType.FOREST, 5);
            dirty.setCleanliness(50.0);  // < 70 → isCleaningDue = true

            SimulationTick tick = new SimulationTick(
                    List.of(), List.of(clean, dirty), List.of(),
                    LocalDateTime.now(), 1);

            assertEquals(1, tick.getCleaningDueCount());
        }

        @Test
        void hasNewAlerts_true_when_alert_list_is_non_empty() {
            Alert alert = new Alert(AlertType.CRITICAL_HUNGER, AlertSeverity.CRITICAL,
                    "ANM-1", "Leo (Lion)", "Hungry");
            SimulationTick tick = new SimulationTick(
                    List.of(), List.of(), List.of(alert),
                    LocalDateTime.now(), 1);

            assertTrue(tick.hasNewAlerts());
        }

        @Test
        void hasNewAlerts_false_when_alert_list_is_empty() {
            SimulationTick tick = new SimulationTick(
                    List.of(), List.of(), List.of(),
                    LocalDateTime.now(), 1);

            assertFalse(tick.hasNewAlerts());
        }

        @Test
        void snapshot_animals_list_is_defensively_copied() {
            Mammal m = new Mammal("Leo", "Lion", 5);
            List<Animal> mutableAnimals = new java.util.ArrayList<>(List.of(m));
            SimulationTick tick = new SimulationTick(
                    mutableAnimals, List.of(), List.of(),
                    LocalDateTime.now(), 1);

            mutableAnimals.clear();           // mutate the original

            assertEquals(1, tick.getAnimals().size(),
                    "Snapshot should be unaffected by mutation of the source list");
        }

        @Test
        void snapshot_animals_list_is_unmodifiable() {
            SimulationTick tick = new SimulationTick(
                    new java.util.ArrayList<>(List.of(new Mammal("Leo", "Lion", 5))),
                    List.of(), List.of(), LocalDateTime.now(), 1);

            List<Animal> snapshot = tick.getAnimals();
            assertThrows(UnsupportedOperationException.class, snapshot::clear);
        }

        @Test
        void getTickNumber_returns_value_passed_to_constructor() {
            SimulationTick tick = new SimulationTick(
                    List.of(), List.of(), List.of(), LocalDateTime.now(), 42);

            assertEquals(42, tick.getTickNumber());
        }

        @Test
        void zero_animals_and_enclosures_gives_zero_counts() {
            SimulationTick tick = new SimulationTick(
                    List.of(), List.of(), List.of(), LocalDateTime.now(), 1);

            assertEquals(0, tick.getAttentionCount());
            assertEquals(0, tick.getCleaningDueCount());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // SimulationEngine — lifecycle, listeners, and speed control
    // ════════════════════════════════════════════════════════════════════════

    @Mock AnimalService    animalService;
    @Mock EnclosureService enclosureService;
    @Mock AlertService     alertService;

    SimulationEngine engine;

    @BeforeEach
    void setUpEngine() {
        engine = new SimulationEngine(animalService, enclosureService, alertService);
    }

    @AfterEach
    void stopEngine() {
        engine.stop();  // safe to call even when not started
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────

    @Test
    void engine_is_not_running_before_start() {
        assertFalse(engine.isRunning());
    }

    @Test
    void engine_is_running_after_start() {
        engine.start();
        assertTrue(engine.isRunning());
    }

    @Test
    void engine_is_not_running_after_stop() {
        engine.start();
        engine.stop();
        assertFalse(engine.isRunning());
    }

    @Test
    void calling_start_twice_does_not_throw_or_restart() {
        engine.start();
        assertDoesNotThrow(() -> engine.start());
        assertTrue(engine.isRunning());
    }

    @Test
    void calling_stop_when_not_started_does_not_throw() {
        assertDoesNotThrow(() -> engine.stop());
    }

    // ── Listener management ───────────────────────────────────────────────

    @Test
    void addListener_null_is_silently_ignored() throws Exception {
        engine.addListener(null);
        assertEquals(0, listenerCount());
    }

    @Test
    void addListener_registers_listener() throws Exception {
        engine.addListener(mock(SimulationListener.class));
        assertEquals(1, listenerCount());
    }

    @Test
    void addListener_duplicate_is_not_registered_twice() throws Exception {
        SimulationListener listener = mock(SimulationListener.class);
        engine.addListener(listener);
        engine.addListener(listener);
        assertEquals(1, listenerCount());
    }

    @Test
    void removeListener_unregisters_listener() throws Exception {
        SimulationListener listener = mock(SimulationListener.class);
        engine.addListener(listener);
        engine.removeListener(listener);
        assertEquals(0, listenerCount());
    }

    @Test
    void multiple_distinct_listeners_all_registered() throws Exception {
        engine.addListener(mock(SimulationListener.class));
        engine.addListener(mock(SimulationListener.class));
        assertEquals(2, listenerCount());
    }

    // ── Speed multiplier ─────────────────────────────────────────────────

    @Test
    void setSpeedMultiplier_zero_throws_illegal_argument() {
        SimulationEngine e = engine;
        assertThrows(IllegalArgumentException.class, () -> e.setSpeedMultiplier(0.0));
    }

    @Test
    void setSpeedMultiplier_negative_throws_illegal_argument() {
        SimulationEngine e = engine;
        assertThrows(IllegalArgumentException.class, () -> e.setSpeedMultiplier(-100.0));
    }

    @Test
    void setSpeedMultiplier_valid_value_is_stored() {
        engine.setSpeedMultiplier(360.0);
        assertEquals(360.0, engine.getSpeedMultiplier(), 1e-9);
    }

    @Test
    void setSpeedMultiplier_small_positive_value_is_accepted() {
        engine.setSpeedMultiplier(0.001);
        assertEquals(0.001, engine.getSpeedMultiplier(), 1e-9);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private int listenerCount() throws Exception {
        Field field = SimulationEngine.class.getDeclaredField("listeners");
        field.setAccessible(true);
        return ((List<?>) field.get(engine)).size();
    }
}
