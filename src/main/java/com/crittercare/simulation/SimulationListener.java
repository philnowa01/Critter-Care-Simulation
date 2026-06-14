package com.crittercare.simulation;

import com.crittercare.model.Alert;

/**
 * Observer interface implemented by any UI controller that wants to
 * react to simulation ticks.
 *
 * Design pattern: Observer (also called Event Listener in Java).
 *
 * The SimulationEngine is the Subject; DashboardController,
 * AnimalsController, etc. are the Observers.
 *
 * IMPORTANT: Both methods are always called on the JavaFX Application
 * Thread (via Platform.runLater), so implementations can update UI
 * controls directly without an additional runLater call.
 */
public interface SimulationListener {

    /**
     * Called once per simulation tick after all animal and enclosure
     * stats have been updated and persisted.
     *
     * @param tick immutable snapshot of the zoo state for this tick
     */
    void onTick(SimulationTick tick);

    /**
     * Called for each new alert generated during the tick.
     * Fired before onTick so controllers can highlight new alerts
     * in the notification badge before the table refreshes.
     *
     * @param alert the newly created (and already persisted) alert
     */
    void onAlertGenerated(Alert alert);
}
