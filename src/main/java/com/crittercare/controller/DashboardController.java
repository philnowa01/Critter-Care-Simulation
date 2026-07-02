package com.crittercare.controller;

import com.crittercare.model.Alert;
import com.crittercare.model.Animal;
import com.crittercare.model.Enclosure;
import com.crittercare.model.HabitatType;
import com.crittercare.model.MaintenanceLog;
import com.crittercare.service.AlertService;
import com.crittercare.service.AnimalService;
import com.crittercare.service.EnclosureService;
import com.crittercare.service.MaintenanceLogService;
import com.crittercare.simulation.SimulationEngine;
import com.crittercare.simulation.SimulationListener;
import com.crittercare.simulation.SimulationTick;
import com.crittercare.util.DateTimeUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;


import java.util.List;

public class DashboardController implements SimulationListener {

    // ── Services ──────────────────────────────────────────────────────────────
    final AnimalService         animalService;
    final EnclosureService      enclosureService;
    final MaintenanceLogService logService;
    final AlertService          alertService;
    final SimulationEngine      simulationEngine;

    // ── FXML: stat cards ─────────────────────────────────────────────────────
    @FXML Label totalAnimalsLabel;
    @FXML Label animalBreakdownLabel;
    @FXML Label activeEnclosuresLabel;
    @FXML Label activeAlertsLabel;
    @FXML Label criticalAlertsLabel;
    @FXML Label todaysLogsLabel;
    @FXML Label attentionLabel;

    // ── FXML: trend section ───────────────────────────────────────────────────
    @FXML Label       healthScoreLabel;
    @FXML Label       cleanlinessScoreLabel;
    @FXML ProgressBar healthBar;
    @FXML ProgressBar cleanlinessBar;

    // ── FXML: habitat map ─────────────────────────────────────────────────────
    @FXML StackPane habitatMap;
    @FXML HBox      habitatRow;      // kept for FXML binding; not manipulated
    @FXML Button    expandHabitatBtn;
    private Canvas     mapCanvas;
    private StackPane  habitatOverlay;

    // ── FXML: attention animals ───────────────────────────────────────────────
    @FXML VBox attentionAnimalsBox;

    // ── FXML: enclosure status ────────────────────────────────────────────────
    @FXML Label enclosureNormalLabel;
    @FXML Label enclosureAttentionLabel;
    @FXML Label enclosureCriticalLabel;
    @FXML Label enclosureOfflineLabel;

    // ── FXML: recent logs table ───────────────────────────────────────────────
    @FXML TableView<MaintenanceLog>           recentLogsTable;
    @FXML TableColumn<MaintenanceLog, String> logTimeCol;
    @FXML TableColumn<MaintenanceLog, String> logActivityCol;
    @FXML TableColumn<MaintenanceLog, String> logStaffCol;
    @FXML TableColumn<MaintenanceLog, String> logSubjectCol;
    @FXML TableColumn<MaintenanceLog, String> logStatusCol;

    // ── Legacy FXML refs (unused in current FXML, kept to avoid null injection errors)
    @FXML Label simAttentionLabel;
    @FXML Label simCleaningLabel;
    @FXML Label lastTickLabel;
    @FXML Label simTickNumberLabel;

    private Runnable onNavigateToLogs;
    private Runnable onNavigateToEnclosures;

    // ── Constructor ───────────────────────────────────────────────────────────

    public DashboardController(AnimalService animalService,
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

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        logTimeCol.setCellValueFactory(cd ->
                new SimpleStringProperty(DateTimeUtils.timeAgo(cd.getValue().getTimestamp())));
        logActivityCol.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getActivityType()));
        logStaffCol.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getStaffMember()));
        logSubjectCol.setCellValueFactory(cd -> {
            MaintenanceLog log = cd.getValue();
            StringBuilder  sb  = new StringBuilder();
            if (log.getAnimalId() > 0) {
                animalService.getAnimalById(log.getAnimalId())
                        .ifPresent(a -> sb.append(a.getName()));
            }
            if (log.getEnclosureId() > 0) {
                if (!sb.isEmpty()) sb.append(", ");
                enclosureService.getEnclosureById(log.getEnclosureId())
                        .ifPresent(e -> sb.append(e.getName()));
            }
            return new SimpleStringProperty(sb.toString());
        });
        logStatusCol.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getStatus()));

        // Create the map Canvas once; bind its size to the StackPane
        if (habitatMap != null) {
            mapCanvas = new Canvas();
            mapCanvas.widthProperty().bind(habitatMap.widthProperty());
            mapCanvas.heightProperty().bind(habitatMap.heightProperty());
            mapCanvas.widthProperty().addListener((obs, o, n) -> drawMapBg(mapCanvas));
            mapCanvas.heightProperty().addListener((obs, o, n) -> drawMapBg(mapCanvas));
            habitatMap.getChildren().add(mapCanvas);
        }

        refreshAll(animalService.getAllAnimals(), enclosureService.getAllEnclosures());
        recentLogsTable.getItems().setAll(safeRecentLogs());
    }

    // ── SimulationListener ────────────────────────────────────────────────────

    @Override
    public void onTick(SimulationTick tick) {
        Platform.runLater(() -> {
            refreshAll(tick.getAnimals(), tick.getEnclosures());
            recentLogsTable.getItems().setAll(safeRecentLogs());
        });
    }

    @Override
    public void onAlertGenerated(Alert alert) {
        Platform.runLater(this::refreshAlertCard);
    }

    // ── FXML actions ──────────────────────────────────────────────────────────

    @FXML void goToCareLogs()    { if (onNavigateToLogs       != null) onNavigateToLogs.run(); }
    @FXML void goToEnclosures()  { if (onNavigateToEnclosures != null) onNavigateToEnclosures.run(); }

    @FXML
    void toggleHabitatExpand() {
        if (isOverlayVisible()) { closeHabitatPopup(); return; }
        habitatOverlay = null;   // discard stale reference if user navigated away
        showHabitatPopup();
    }

    private boolean isOverlayVisible() {
        if (habitatOverlay == null || habitatMap.getScene() == null) return false;
        Node cp = habitatMap.getScene().lookup("#contentPane");
        return cp instanceof StackPane sp && sp.getChildren().contains(habitatOverlay);
    }

    private void showHabitatPopup() {
        // ── canvas + full grid for popup ──────────────────────────────────
        List<Enclosure> all = enclosureService.getAllEnclosures();
        Canvas popupCanvas  = new Canvas();
        GridPane fullGrid   = buildHabitatGrid(all);
        fullGrid.setMaxWidth(Double.MAX_VALUE);
        fullGrid.setMaxHeight(Double.MAX_VALUE);

        StackPane mapPane = new StackPane(popupCanvas, fullGrid);
        mapPane.setStyle("-fx-background-color: #D4C9A8; -fx-background-radius: 0 0 12 12;");
        VBox.setVgrow(mapPane, Priority.ALWAYS);
        popupCanvas.widthProperty().bind(mapPane.widthProperty());
        popupCanvas.heightProperty().bind(mapPane.heightProperty());
        popupCanvas.widthProperty().addListener((obs, o, n) -> drawMapBg(popupCanvas));
        popupCanvas.heightProperty().addListener((obs, o, n) -> drawMapBg(popupCanvas));

        // ── panel header ──────────────────────────────────────────────────
        Label titleLbl = new Label("Habitat Overview — All Enclosures");
        titleLbl.getStyleClass().add("panel-title");
        Button closeBtn = new Button("✕  Close");
        closeBtn.getStyleClass().add("habitat-header-btn");
        closeBtn.setOnAction(e -> closeHabitatPopup());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(12, titleLbl, spacer, closeBtn);
        header.getStyleClass().add("panel-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 16, 14, 16));

        // ── popup panel ───────────────────────────────────────────────────
        VBox panel = new VBox(0, header, mapPane);
        panel.setMaxWidth(960);
        panel.setMaxHeight(600);
        panel.setStyle(
            "-fx-background-color: -cc-surface;" +
            "-fx-background-radius: 14;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 32, 0, 0, 8);"
        );
        panel.setOnMouseClicked(e -> e.consume());

        // ── semi-transparent backdrop, closes on click ────────────────────
        StackPane backdrop = new StackPane(panel);
        backdrop.setStyle("-fx-background-color: rgba(0,0,0,0.45);");
        backdrop.setOnMouseClicked(e -> closeHabitatPopup());
        StackPane.setMargin(panel, new Insets(50, 50, 50, 50));

        habitatOverlay = backdrop;

        Node cp = habitatMap.getScene().lookup("#contentPane");
        if (cp instanceof StackPane contentPane) contentPane.getChildren().add(habitatOverlay);
        if (expandHabitatBtn != null) expandHabitatBtn.setText("⊟  Collapse");
    }

    private void closeHabitatPopup() {
        if (habitatOverlay == null) return;
        Node cp = habitatMap.getScene().lookup("#contentPane");
        if (cp instanceof StackPane sp) sp.getChildren().remove(habitatOverlay);
        habitatOverlay = null;
        if (expandHabitatBtn != null) expandHabitatBtn.setText("⊞  Expand");
    }

    // ── Callbacks ─────────────────────────────────────────────────────────────

    public void setOnNavigateToLogs(Runnable cb)       { this.onNavigateToLogs = cb; }
    public void setOnNavigateToEnclosures(Runnable cb) { this.onNavigateToEnclosures = cb; }

    // ── Private ───────────────────────────────────────────────────────────────

    private void refreshAll(List<Animal> animals, List<Enclosure> enclosures) {
        refreshAnimalCard(animals);
        refreshAlertCard();
        refreshEnclosureCards(enclosures);
        refreshTrends(animals, enclosures);
        refreshHabitatMap(enclosures);
        refreshAttentionAnimals(animals);
        set(todaysLogsLabel, String.valueOf(logService.getTodaysLogCount()));
    }

    private void refreshAnimalCard(List<Animal> animals) {
        long mammals  = animals.stream().filter(a -> "MAMMAL".equalsIgnoreCase(a.getType())).count();
        long birds    = animals.stream().filter(a -> "BIRD".equalsIgnoreCase(a.getType())).count();
        long reptiles = animals.stream().filter(a -> "REPTILE".equalsIgnoreCase(a.getType())).count();
        set(totalAnimalsLabel, String.valueOf(animals.size()));
        set(animalBreakdownLabel, mammals + " mammals · " + birds + " birds · " + reptiles + " reptiles");
    }

    private void refreshAlertCard() {
        long critical = alertService.getCriticalAlertCount();
        long active   = alertService.getActiveAlertCount();
        set(activeAlertsLabel, String.valueOf(critical));
        set(criticalAlertsLabel, active > 0 ? active + " alerts active" : "No active alerts");
    }

    private void refreshEnclosureCards(List<Enclosure> enclosures) {
        long occupied = enclosures.stream().filter(e -> e.getOccupancy() > 0).count();
        long normal   = enclosures.stream().filter(e -> "Good".equals(e.getStatus())).count();
        long attn     = enclosures.stream().filter(e -> "Cleaning Due".equals(e.getStatus())).count();
        long critical = enclosures.stream().filter(e -> "Critical".equals(e.getStatus())).count();

        set(activeEnclosuresLabel, String.valueOf(occupied));
        set(enclosureNormalLabel,   String.valueOf(normal));
        set(enclosureAttentionLabel, String.valueOf(attn));
        set(enclosureCriticalLabel, String.valueOf(critical));
        set(enclosureOfflineLabel,  "0");
    }

    private void refreshTrends(List<Animal> animals, List<Enclosure> enclosures) {
        if (!animals.isEmpty()) {
            double avg = animals.stream().mapToDouble(Animal::getHealth).average().orElse(0);
            set(healthScoreLabel, String.format("%.0f%%", avg));
            if (healthBar != null) healthBar.setProgress(avg / 100.0);
        }
        if (!enclosures.isEmpty()) {
            double avg = enclosures.stream().mapToDouble(Enclosure::getCleanliness).average().orElse(0);
            set(cleanlinessScoreLabel, String.format("%.0f%%", avg));
            if (cleanlinessBar != null) cleanlinessBar.setProgress(avg / 100.0);
        }
    }

    // ── Habitat map ───────────────────────────────────────────────────────────

    private void refreshHabitatMap(List<Enclosure> enclosures) {
        if (habitatMap == null) return;

        // Compact inline view: first 4 only — see popup for all 8
        List<Enclosure> compact = enclosures.stream().limit(4).toList();
        GridPane grid = buildHabitatGrid(compact);
        grid.setMaxWidth(Double.MAX_VALUE);
        grid.setMaxHeight(Double.MAX_VALUE);

        // Canvas stays at index 0; replace the zone grid (index 1+)
        if (habitatMap.getChildren().size() > 1) {
            habitatMap.getChildren().subList(1, habitatMap.getChildren().size()).clear();
        }
        habitatMap.getChildren().add(grid);

        if (mapCanvas != null) drawMapBg(mapCanvas);
    }

    private void drawMapBg(Canvas canvas) {
        double w = canvas.getWidth(), h = canvas.getHeight();
        if (w <= 0 || h <= 0) return;
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        // Sandy cream path base
        gc.setFill(Color.web("#D4C9A8"));
        gc.fillRect(0, 0, w, h);

        // Grass patches in corners
        gc.setFill(Color.web("#8FBF78"));
        gc.fillOval(-30, -30,      w * 0.30, h * 0.45);
        gc.fillOval(w * 0.70, -30, w * 0.35, h * 0.45);
        gc.fillOval(-30, h * 0.55, w * 0.30, h * 0.50);
        gc.fillOval(w * 0.70, h * 0.55, w * 0.35, h * 0.50);

        // Deeper-green secondary blobs
        gc.setFill(Color.web("#6DA856", 0.5));
        gc.fillOval(-10, h * 0.20, w * 0.18, h * 0.30);
        gc.fillOval(w * 0.82, h * 0.20, w * 0.22, h * 0.30);

        // Tree dots (dark green circles with lighter highlight)
        double[][] trees = {
            {0.03, 0.06}, {0.09, 0.13}, {0.06, 0.24},
            {0.91, 0.06}, {0.95, 0.14}, {0.93, 0.25},
            {0.03, 0.65}, {0.08, 0.76}, {0.05, 0.88},
            {0.92, 0.66}, {0.96, 0.77}, {0.94, 0.89}
        };
        for (double[] t : trees) {
            double tx = t[0] * w, ty = t[1] * h;
            gc.setFill(Color.web("#3D7A3D"));
            gc.fillOval(tx - 7, ty - 7, 14, 14);
            gc.setFill(Color.web("#5EA05E", 0.6));
            gc.fillOval(tx - 4, ty - 5, 7, 7);
        }

        // Central lake
        gc.setFill(Color.web("#7EB3CF", 0.85));
        gc.fillOval(w * 0.36, h * 0.20, w * 0.26, h * 0.55);
        gc.setFill(Color.web("#A8D0E6", 0.45));
        gc.fillOval(w * 0.40, h * 0.27, w * 0.16, h * 0.32);
        gc.setFill(Color.web("#5A9ABF", 0.25));
        gc.fillOval(w * 0.38, h * 0.50, w * 0.20, h * 0.16);

        // Main entrance building (bottom-center)
        double bw = 38, bh = 26;
        double bx = w / 2.0 - bw / 2.0, by = h - bh - 4;
        gc.setFill(Color.web("#C8BEA8"));
        gc.fillRoundRect(bx, by, bw, bh, 6, 6);
        gc.setFill(Color.web("#A09080"));
        gc.fillRoundRect(bx + bw * 0.28, by + bh * 0.45, bw * 0.44, bh * 0.55, 3, 3);
        gc.setFill(Color.web("#C8BE9A", 0.5));
        gc.fillOval(w / 2.0 - 16, by + bh - 4, 32, 18);
    }

    private GridPane buildHabitatGrid(List<Enclosure> enclosures) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(14, 14, 14, 14));

        int cols = enclosures.isEmpty() ? 1 : Math.min(4, enclosures.size());
        int rows = enclosures.isEmpty() ? 1 : (int) Math.ceil(enclosures.size() / (double) cols);

        for (int i = 0; i < cols; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(Priority.ALWAYS);
            cc.setPercentWidth(25.0);
            grid.getColumnConstraints().add(cc);
        }
        for (int i = 0; i < rows; i++) {
            RowConstraints rc = new RowConstraints();
            rc.setVgrow(Priority.ALWAYS);
            rc.setPercentHeight(100.0 / rows);
            grid.getRowConstraints().add(rc);
        }

        if (enclosures.isEmpty()) {
            Label empty = new Label("No enclosures added yet");
            empty.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 13px;");
            GridPane.setColumnSpan(empty, cols);
            GridPane.setHalignment(empty, HPos.CENTER);
            GridPane.setValignment(empty, VPos.CENTER);
            grid.add(empty, 0, 0);
        } else {
            for (int i = 0; i < enclosures.size(); i++) {
                VBox card = buildEnclosureCard(enclosures.get(i));
                GridPane.setHgrow(card, Priority.ALWAYS);
                GridPane.setVgrow(card, Priority.ALWAYS);
                grid.add(card, i % cols, i / cols);
            }
        }
        return grid;
    }

    private Node buildZoneIconNode(HabitatType type) {
    if (type == HabitatType.REPTILE_HOUSE) {
        try {
            Image frogImage = new Image(
                getClass().getResourceAsStream("/com/crittercare/images/CroakingToad.gif")
            );
            ImageView iv = new ImageView(frogImage);
            iv.setFitWidth(32);
            iv.setFitHeight(32);
            iv.setPreserveRatio(true);
            iv.setSmooth(false); // keep pixel-art crisp, no blurring
            return iv;
        } catch (Exception e) {
            // fall through to emoji if the gif can't be loaded
        }
    }

    if (type == HabitatType.ARCTIC) {
        try {
            Image frogImage = new Image(
                getClass().getResourceAsStream("/com/crittercare/images/MeowingCat.gif")
            );
            ImageView iv = new ImageView(frogImage);
            iv.setFitWidth(32);
            iv.setFitHeight(32);
            iv.setPreserveRatio(true);
            iv.setSmooth(false); // keep pixel-art crisp, no blurring
            return iv;
        } catch (Exception e) {
            // fall through to emoji if the gif can't be loaded
        }
    }    

    if (type == HabitatType.AVIARY) {
        try {
            Image frogImage = new Image(
                getClass().getResourceAsStream("/com/crittercare/images/TinyChick.gif")
            );
            ImageView iv = new ImageView(frogImage);
            iv.setFitWidth(32);
            iv.setFitHeight(32);
            iv.setPreserveRatio(true);
            iv.setSmooth(false); // keep pixel-art crisp, no blurring
            return iv;
        } catch (Exception e) {
            // fall through to emoji if the gif can't be loaded
        }
    }        

    if (type == HabitatType.FOREST) {
        try {
            Image frogImage = new Image(
                getClass().getResourceAsStream("/com/crittercare/images/MonkeyShake.gif")
            );
            ImageView iv = new ImageView(frogImage);
            iv.setFitWidth(32);
            iv.setFitHeight(32);
            iv.setPreserveRatio(true);
            iv.setSmooth(false); // keep pixel-art crisp, no blurring
            return iv;
        } catch (Exception e) {
            // fall through to emoji if the gif can't be loaded
        }
    }      



    Label fallback = new Label(zoneIcon(type));
    fallback.setStyle("-fx-font-size: 26px;");
    return fallback;
}


    
    private VBox buildEnclosureCard(Enclosure enc) {
        boolean critical = enc.isCritical();
        boolean warning  = !critical && enc.isCleaningDue();

        String status, statusColor;
        if (critical)     { status = "● Alert";        statusColor = "#DC2626"; }
        else if (warning) { status = "● Cleaning Due"; statusColor = "#D97706"; }
        else              { status = "● Good";         statusColor = "#16A34A"; }

        Node iconNode = buildZoneIconNode(enc.getHabitatType());

        Label nameLbl = new Label(enc.getName());
        nameLbl.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #1A1A1A; -fx-text-alignment: CENTER;");
        nameLbl.setWrapText(true);
        nameLbl.setMaxWidth(Double.MAX_VALUE);
        nameLbl.setAlignment(Pos.CENTER);

        int animalCount = enc.getAnimalIds().size();
        Label countLbl = new Label(animalCount + " animal" + (animalCount == 1 ? "" : "s"));
        countLbl.setStyle("-fx-font-size: 9px; -fx-text-fill: #6B7280;");

        Label statusLbl = new Label(status);
        statusLbl.setStyle("-fx-font-size: 9px; -fx-text-fill: " + statusColor + "; -fx-font-weight: bold;");

        VBox card = new VBox(4, iconNode, nameLbl, countLbl, statusLbl);
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setMaxHeight(Double.MAX_VALUE);
        card.setStyle(
            "-fx-background-color: " + zoneBgColor(enc.getHabitatType()) + ";" +
            "-fx-border-color: "     + zoneBorderColor(enc.getHabitatType()) + ";" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 16;" +
            "-fx-background-radius: 16;" +
            "-fx-padding: 10 8;"
        );
        VBox.setVgrow(card, Priority.ALWAYS);
        return card;
    }

    private static String zoneIcon(HabitatType type) {
        return switch (type) {
            case SAVANNAH      -> "🦒";
            case FOREST        -> "🐒";
            case AQUATIC       -> "🐧";
            case AVIARY        -> "🦜";
            case ARCTIC        -> "🐅";
            case REPTILE_HOUSE -> "🦎";
        };
    }

    private static String zoneBgColor(HabitatType type) {
        return switch (type) {
            case SAVANNAH      -> "rgba(185,228,170,0.88)";
            case FOREST        -> "rgba(215,198,158,0.88)";
            case AQUATIC       -> "rgba(175,215,240,0.88)";
            case AVIARY        -> "rgba(235,190,178,0.88)";
            case ARCTIC        -> "rgba(235,218,160,0.88)";
            case REPTILE_HOUSE -> "rgba(178,225,165,0.88)";
        };
    }

    private static String zoneBorderColor(HabitatType type) {
        return switch (type) {
            case SAVANNAH      -> "#4A8A40";
            case FOREST        -> "#7A5A2A";
            case AQUATIC       -> "#4070A0";
            case AVIARY        -> "#A04848";
            case ARCTIC        -> "#B07828";
            case REPTILE_HOUSE -> "#4A7838";
        };
    }

    // ── Attention animals panel ───────────────────────────────────────────────

    private void refreshAttentionAnimals(List<Animal> animals) {
        if (attentionAnimalsBox == null) return;
        attentionAnimalsBox.getChildren().clear();

        List<Animal> needsAttention = animals.stream()
                .filter(Animal::requiresAttention)
                .limit(6)
                .toList();

        if (needsAttention.isEmpty()) {
            Label ok = new Label("✓ All animals healthy");
            ok.setStyle("-fx-text-fill: #16A34A; -fx-font-size: 13px; -fx-padding: 8 0;");
            attentionAnimalsBox.getChildren().add(ok);
        } else {
            for (Animal animal : needsAttention) {
                attentionAnimalsBox.getChildren().add(buildAttentionCard(animal));
            }
        }
        if (attentionLabel != null) {
            long count = animals.stream().filter(Animal::requiresAttention).count();
            attentionLabel.setText(count > 0 ? String.valueOf(count) : "");
            attentionLabel.setVisible(count > 0);
        }
    }

    private HBox buildAttentionCard(Animal animal) {
        String emoji = habitatIcon(animal);

        String chipText, chipClass;
        if (animal.isHealthCritical())       { chipText = "Medical";   chipClass = "chip-medical"; }
        else if (animal.isHungerCritical())  { chipText = "Hunger";    chipClass = "chip-hunger"; }
        else if (animal.isDehydrationCritical()) { chipText = "Health"; chipClass = "chip-health"; }
        else                                 { chipText = "Attention"; chipClass = "chip-warning"; }

        Label avatarLbl = new Label(emoji);
        avatarLbl.getStyleClass().add("animal-avatar");

        Label nameLbl = new Label(animal.getName());
        nameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        String enclosureName = animal.getEnclosureId() > 0
                ? enclosureService.getEnclosureById(animal.getEnclosureId())
                        .map(Enclosure::getName).orElse("—")
                : "—";
        Label encLbl = new Label(enclosureName);
        encLbl.getStyleClass().add("card-label");

        VBox infoBox = new VBox(2, nameLbl, encLbl);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label chip = new Label(chipText);
        chip.getStyleClass().addAll("chip", chipClass);

        HBox card = new HBox(10, avatarLbl, infoBox, chip);
        card.getStyleClass().add("attention-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    // Returns the zone icon for the animal's habitat, falling back to type-based icon
    private String habitatIcon(Animal animal) {
        if (animal.getEnclosureId() <= 0) return animalTypeIcon(animal);
        return enclosureService.getEnclosureById(animal.getEnclosureId())
                .map(enc -> zoneIcon(enc.getHabitatType()))
                .orElseGet(() -> animalTypeIcon(animal));
    }

    private static String animalTypeIcon(Animal animal) {
        if (animal.getType() == null) return "🐾";
        return switch (animal.getType().toUpperCase()) {
            case "BIRD"    -> "🦜";
            case "REPTILE" -> "🦎";
            default        -> "🦁";
        };
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private List<MaintenanceLog> safeRecentLogs() {
        try { return logService.getRecentLogs(10); }
        catch (Exception e) { return List.of(); }
    }

    private static void set(Label label, String text) {
        if (label != null) label.setText(text);
    }
}
