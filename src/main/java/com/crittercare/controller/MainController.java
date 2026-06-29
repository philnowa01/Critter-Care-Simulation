package com.crittercare.controller;

import com.crittercare.minigame.HabitatGame;
import com.crittercare.model.Alert;
import com.crittercare.service.AlertService;
import com.crittercare.simulation.SimulationEngine;
import com.crittercare.simulation.SimulationListener;
import com.crittercare.simulation.SimulationTick;
import com.crittercare.util.DateTimeUtils;
import com.crittercare.view.ViewFactory;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Controller for MainView.fxml — the application shell.
 *
 * Responsibilities:
 *  - Swaps the center StackPane when the user clicks a nav button.
 *  - Maintains the "active" CSS class on the selected nav button.
 *  - Updates the alert badge, tick counter, and clock via SimulationListener.
 *
 * Navigation pattern: each sub-view is loaded lazily through ViewFactory and
 * cached there — clicking the same nav button twice reuses the same scene graph.
 */
public class MainController implements SimulationListener {

    // ── Injected collaborators ────────────────────────────────────────────────
    private final ViewFactory      viewFactory;
    private final SimulationEngine simulationEngine;
    private final AlertService     alertService;

    // ── FXML nodes ────────────────────────────────────────────────────────────
    @FXML private StackPane contentPane;
    @FXML private Button    btnDashboard;
    @FXML private Button    btnAnimals;
    @FXML private Button    btnEnclosures;
    @FXML private Button    btnCareLogs;
    @FXML private Button    btnAlerts;
    @FXML private Button    btnMiniGame;
    @FXML private Label     alertBadge;
    @FXML private Label     alertBadge2;
    @FXML private Label     pageTitle;
    @FXML private Label     tickLabel;
    @FXML private Label     simDot;
    @FXML private Label     simStatusLabel;
    @FXML private Label     clockLabel;

    private Button currentActive;

    // ── Constructor ───────────────────────────────────────────────────────────

    public MainController(ViewFactory viewFactory,
                          SimulationEngine simulationEngine,
                          AlertService alertService) {
        this.viewFactory      = viewFactory;
        this.simulationEngine = simulationEngine;
        this.alertService     = alertService;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        simulationEngine.addListener(this);
        updateAlertBadge();
        alertService.addChangeListener(this::updateAlertBadge);
        showDashboard();
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @FXML
    public void showDashboard() {
        navigate("Dashboard", viewFactory::loadDashboard, btnDashboard);
    }

    @FXML
    public void showAnimals() {
        navigate("Animals", viewFactory::loadAnimals, btnAnimals);
    }

    @FXML
    public void showEnclosures() {
        navigate("Enclosures", viewFactory::loadEnclosures, btnEnclosures);
    }

    @FXML
    public void showCareLogs() {
        navigate("Care Logs", viewFactory::loadCareLogs, btnCareLogs);
    }

    @FXML
    public void showAlerts() {
        navigate("Alerts", viewFactory::loadAlerts, btnAlerts);
    }

    @FXML
    public void showMiniGame() {
        try {
            new HabitatGame().start(new Stage());
        } catch (Exception e) {
            System.err.println("[MainController] Could not launch Mini Game: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── SimulationListener ────────────────────────────────────────────────────

    @Override
    public void onTick(SimulationTick tick) {
        tickLabel.setText("Tick #" + tick.getTickNumber());
        clockLabel.setText(DateTimeUtils.formatClock(tick.getTimestamp()));
        updateAlertBadge();
    }

    @Override
    public void onAlertGenerated(Alert alert) {
        updateAlertBadge();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void navigate(String title, ViewLoader loader, Button navButton) {
        try {
            Parent view = loader.load();
            contentPane.getChildren().setAll(view);
            pageTitle.setText(title);
            setActive(navButton);
        } catch (IOException e) {
            System.err.println("[MainController] Could not load view '" + title + "': " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setActive(Button button) {
        if (currentActive != null) {
            currentActive.getStyleClass().remove("active");
        }
        button.getStyleClass().add("active");
        currentActive = button;
    }

    private void updateAlertBadge() {
        long count = alertService.getActiveAlertCount();
        if (count > 0) {
            String text = String.valueOf(count);
            alertBadge.setText(text);
            alertBadge.setVisible(true);
            if (alertBadge2 != null) { alertBadge2.setText(text); alertBadge2.setVisible(true); }
        } else {
            alertBadge.setVisible(false);
            if (alertBadge2 != null) alertBadge2.setVisible(false);
        }
    }

    // ── Private functional interface ──────────────────────────────────────────

    @FunctionalInterface
    private interface ViewLoader {
        Parent load() throws IOException;
    }
}
