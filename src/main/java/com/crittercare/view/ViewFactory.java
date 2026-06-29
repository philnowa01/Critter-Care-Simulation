package com.crittercare.view;

import com.crittercare.controller.AlertsController;
import com.crittercare.controller.AnimalsController;
import com.crittercare.controller.CareLogsController;
import com.crittercare.controller.DashboardController;
import com.crittercare.controller.EnclosuresController;
import com.crittercare.controller.MainController;
import com.crittercare.service.AlertService;
import com.crittercare.service.AnimalService;
import com.crittercare.service.EnclosureService;
import com.crittercare.service.MaintenanceLogService;
import com.crittercare.simulation.SimulationEngine;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.net.URL;

/**
 * DI bridge between services and JavaFX controllers.
 *
 * Why this class exists:
 *   JavaFX @FXML injection handles scene-graph nodes, not services.
 *   FXMLLoader.setControllerFactory() lets us intercept controller
 *   creation and supply fully-wired instances via constructor injection.
 *   Controllers therefore remain plain-Java testable without FXML.
 *
 * Caching:
 *   Each sub-view (Dashboard, Animals, …) is loaded once on first access
 *   and the Parent is cached.  Revisiting a screen reuses the same scene
 *   graph; SimulationListeners are registered once, eliminating leak risk.
 *
 * Called from: MainApp.start() (creates this) and MainController (uses
 *              loadXxx() methods to swap the center content pane).
 */
public class ViewFactory {

    // ── Services ──────────────────────────────────────────────────────────────
    private final AnimalService          animalService;
    private final EnclosureService       enclosureService;
    private final MaintenanceLogService  logService;
    private final AlertService           alertService;
    private final SimulationEngine       simulationEngine;

    // ── Cached controllers ────────────────────────────────────────────────────
    private MainController       mainController;
    private DashboardController  dashboardController;
    private AnimalsController    animalsController;
    private EnclosuresController enclosuresController;
    private CareLogsController   careLogsController;
    private AlertsController     alertsController;

    // ── Cached views (lazy-loaded on first navigation) ────────────────────────
    private Parent dashboardView;
    private Parent animalsView;
    private Parent enclosuresView;
    private Parent careLogsView;
    private Parent alertsView;

    // ── Constructor ───────────────────────────────────────────────────────────

    public ViewFactory(AnimalService animalService,
                       EnclosureService enclosureService,
                       MaintenanceLogService logService,
                       AlertService alertService,
                       SimulationEngine simulationEngine) {
        this.animalService    = animalService;
        this.enclosureService = enclosureService;
        this.logService       = logService;
        this.alertService     = alertService;
        this.simulationEngine = simulationEngine;
    }

    // ── Main shell ────────────────────────────────────────────────────────────

    /**
     * Loads the application shell (MainView.fxml) and returns the root Parent.
     * Must be called exactly once from MainApp.start().
     */
    public Parent loadMainView() throws IOException {
        FXMLLoader loader = fxmlLoader("/com/crittercare/view/MainView.fxml");
        loader.setControllerFactory(this::createController);
        Parent root = loader.load();
        mainController = loader.getController();
        return root;
    }

    // ── Sub-view loaders (called by MainController when navigating) ───────────

    /** Returns the Dashboard view, loading it on first call. */
    public Parent loadDashboard() throws IOException {
        if (dashboardView == null) {
            FXMLLoader loader = fxmlLoader("/com/crittercare/view/Dashboard.fxml");
            loader.setControllerFactory(this::createController);
            dashboardView       = loader.load();
            dashboardController = loader.getController();
            simulationEngine.addListener(dashboardController);
            dashboardController.setOnNavigateToLogs(() -> mainController.showCareLogs());
            dashboardController.setOnNavigateToEnclosures(() -> mainController.showEnclosures());
        }
        return dashboardView;
    }

    /** Returns the Animals view, loading it on first call. */
    public Parent loadAnimals() throws IOException {
        if (animalsView == null) {
            FXMLLoader loader = fxmlLoader("/com/crittercare/view/Animals.fxml");
            loader.setControllerFactory(this::createController);
            animalsView       = loader.load();
            animalsController = loader.getController();
            simulationEngine.addListener(animalsController);
        }
        return animalsView;
    }

    /** Returns the Enclosures view, loading it on first call. */
    public Parent loadEnclosures() throws IOException {
        if (enclosuresView == null) {
            FXMLLoader loader = fxmlLoader("/com/crittercare/view/Enclosures.fxml");
            loader.setControllerFactory(this::createController);
            enclosuresView       = loader.load();
            enclosuresController = loader.getController();
            simulationEngine.addListener(enclosuresController);
        }
        return enclosuresView;
    }

    /** Returns the Care Logs view, loading it on first call. */
    public Parent loadCareLogs() throws IOException {
        if (careLogsView == null) {
            FXMLLoader loader = fxmlLoader("/com/crittercare/view/CareLogs.fxml");
            loader.setControllerFactory(this::createController);
            careLogsView       = loader.load();
            careLogsController = loader.getController();
            // CareLogsController does not implement SimulationListener
        }
        return careLogsView;
    }

    /** Returns the Alerts view, loading it on first call. */
    public Parent loadAlerts() throws IOException {
        if (alertsView == null) {
            FXMLLoader loader = fxmlLoader("/com/crittercare/view/Alerts.fxml");
            loader.setControllerFactory(this::createController);
            alertsView       = loader.load();
            alertsController = loader.getController();
            simulationEngine.addListener(alertsController);
        }
        return alertsView;
    }

    // ── Controller getters (for testing / cross-controller wiring) ────────────

    public MainController       getMainController()       { return mainController; }
    public DashboardController  getDashboardController()  { return dashboardController; }
    public AnimalsController    getAnimalsController()    { return animalsController; }
    public EnclosuresController getEnclosuresController() { return enclosuresController; }
    public CareLogsController   getCareLogsController()   { return careLogsController; }
    public AlertsController     getAlertsController()     { return alertsController; }

    // ── Private: controller factory ───────────────────────────────────────────

    /**
     * Called by FXMLLoader for each fx:controller type encountered.
     * Returns a fully-wired controller instance (constructor injection).
     */
    private Object createController(Class<?> type) {
        if (type == MainController.class) {
            return new MainController(this, simulationEngine, alertService);
        }
        if (type == DashboardController.class) {
            return new DashboardController(
                    animalService, enclosureService, logService, alertService, simulationEngine);
        }
        if (type == AnimalsController.class) {
            return new AnimalsController(animalService, enclosureService, logService);
        }
        if (type == EnclosuresController.class) {
            return new EnclosuresController(enclosureService, animalService);
        }
        if (type == CareLogsController.class) {
            return new CareLogsController(logService, animalService, enclosureService);
        }
        if (type == AlertsController.class) {
            return new AlertsController(alertService);
        }
        throw new IllegalStateException(
                "ViewFactory: no wiring defined for controller type " + type.getName());
    }

    // ── Private: FXML loader helper ───────────────────────────────────────────

    private FXMLLoader fxmlLoader(String resourcePath) {
        URL url = getClass().getResource(resourcePath);
        if (url == null) {
            throw new IllegalStateException(
                    "FXML resource not found on classpath: " + resourcePath);
        }
        return new FXMLLoader(url);
    }
}
