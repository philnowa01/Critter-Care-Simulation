package com.crittercare;

import com.crittercare.persistence.DatabaseInitializer;
import com.crittercare.persistence.DatabaseManager;
import com.crittercare.repository.AlertRepository;
import com.crittercare.repository.AlertRepositoryImpl;
import com.crittercare.repository.AnimalRepository;
import com.crittercare.repository.AnimalRepositoryImpl;
import com.crittercare.repository.EnclosureRepository;
import com.crittercare.repository.EnclosureRepositoryImpl;
import com.crittercare.repository.MaintenanceLogRepository;
import com.crittercare.repository.MaintenanceLogRepositoryImpl;
import com.crittercare.service.AlertService;
import com.crittercare.service.AnimalService;
import com.crittercare.service.EnclosureService;
import com.crittercare.service.MaintenanceLogService;
import com.crittercare.simulation.SimulationEngine;
import com.crittercare.view.ViewFactory;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX Application subclass.
 *
 * Wires the full object graph (repositories → services → simulation engine →
 * ViewFactory) and hands a fully-configured scene to the primary stage.
 * Dependency order: repos first, then services, then engine, then ViewFactory.
 *
 * Called by Main.main() via Application.launch(MainApp.class, args).
 */
public class MainApp extends Application {

    private SimulationEngine simulationEngine;

    @Override
    public void start(Stage primaryStage) throws Exception {
        // 1. Database — singleton connection, then idempotent schema + seed
        DatabaseManager dbManager = DatabaseManager.getInstance();
        new DatabaseInitializer(dbManager).initialize();

        // 2. Repositories (all take the shared DatabaseManager)
        AnimalRepository         animalRepo    = new AnimalRepositoryImpl(dbManager);
        EnclosureRepository      enclosureRepo = new EnclosureRepositoryImpl(dbManager);
        MaintenanceLogRepository logRepo       = new MaintenanceLogRepositoryImpl(dbManager);
        AlertRepository          alertRepo     = new AlertRepositoryImpl(dbManager);

        // 3. Services (constructor injection — no DI framework)
        AlertService          alertService     = new AlertService(alertRepo, animalRepo, enclosureRepo);
        AnimalService         animalService    = new AnimalService(animalRepo, enclosureRepo, alertService);
        EnclosureService      enclosureService = new EnclosureService(enclosureRepo, animalRepo, alertService);
        MaintenanceLogService logService       = new MaintenanceLogService(logRepo);

        // 4. Simulation engine
        simulationEngine = new SimulationEngine(animalService, enclosureService, alertService);

        // 5. ViewFactory — the DI bridge between services and JavaFX controllers
        ViewFactory viewFactory = new ViewFactory(
                animalService, enclosureService, logService, alertService, simulationEngine);

        // 6. Build scene
        Scene scene = new Scene(viewFactory.loadMainView(), 1280, 800);
        scene.getStylesheets().add(
                getClass().getResource("/com/crittercare/view/styles.css").toExternalForm());

        primaryStage.setTitle("CritterCare — Zoological Management System");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(700);
        primaryStage.show();

        // 7. Start simulation after UI is visible
        simulationEngine.start();
    }

    @Override
    public void stop() {
        if (simulationEngine != null) {
            simulationEngine.stop();
        }
        DatabaseManager.getInstance().closeConnection();
    }
}
