package com.crittercare.controller;

import com.crittercare.model.Alert;
import com.crittercare.model.Zookeeper;
import com.crittercare.persistence.DatabaseInitializer;
import com.crittercare.service.AlertService;
import com.crittercare.service.ZookeeperService;
import com.crittercare.simulation.SimulationEngine;
import com.crittercare.simulation.SimulationListener;
import com.crittercare.simulation.SimulationTick;
import com.crittercare.util.DateTimeUtils;
import com.crittercare.view.ViewFactory;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

import java.io.IOException;
import java.util.List;

/**
 * Controller for MainView.fxml — the application shell.
 *
 * Responsibilities:
 *  - Swaps the center StackPane when the user clicks a nav button.
 *  - Maintains the "active" CSS class on the selected nav button.
 *  - Updates the alert badge, tick counter, and clock via SimulationListener.
 *  - Manages the zookeeper selector popup (add, rename, delete, switch).
 *
 * Navigation pattern: each sub-view is loaded lazily through ViewFactory and
 * cached there — clicking the same nav button twice reuses the same scene graph.
 */
public class MainController implements SimulationListener {

    // ── Injected collaborators ────────────────────────────────────────────────
    private final ViewFactory        viewFactory;
    private final SimulationEngine   simulationEngine;
    private final AlertService       alertService;
    private final ZookeeperService   zookeeperService;
    private final DatabaseInitializer dbInitializer;

    // ── FXML nodes ────────────────────────────────────────────────────────────
    @FXML private StackPane contentPane;
    @FXML private Button    btnDashboard;
    @FXML private Button    btnAnimals;
    @FXML private Button    btnEnclosures;
    @FXML private Button    btnCareLogs;
    @FXML private Button    btnAlerts;
    @FXML private Button    btnMinigame;
    @FXML private Label     alertBadge;
    @FXML private Label     alertBadge2;
    @FXML private Label     pageTitle;
    @FXML private Label     tickLabel;
    @FXML private Label     simDot;
    @FXML private Label     simStatusLabel;
    @FXML private Label     clockLabel;

    // Zookeeper profile area
    @FXML private HBox  userProfileArea;
    @FXML private Label userAvatar;
    @FXML private Label userNameLabel;

    private Button currentActive;
    private Popup  zookeeperPopup;

    // ── Constructor ───────────────────────────────────────────────────────────

    public MainController(ViewFactory viewFactory,
                          SimulationEngine simulationEngine,
                          AlertService alertService,
                          ZookeeperService zookeeperService,
                          DatabaseInitializer dbInitializer) {
        this.viewFactory       = viewFactory;
        this.simulationEngine  = simulationEngine;
        this.alertService      = alertService;
        this.zookeeperService  = zookeeperService;
        this.dbInitializer     = dbInitializer;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        simulationEngine.addListener(this);
        updateAlertBadge();
        alertService.addChangeListener(this::updateAlertBadge);
        updateZookeeperDisplay();
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
    public void showMinigame() {
        navigate("Minigame", viewFactory::loadMinigame, btnMinigame);
    }

    // ── Zookeeper popup ───────────────────────────────────────────────────────

    @FXML
    public void toggleZookeeperMenu() {
        if (zookeeperPopup != null && zookeeperPopup.isShowing()) {
            zookeeperPopup.hide();
            return;
        }
        showZookeeperMenu();
    }

    private void showZookeeperMenu() {
        List<Zookeeper> zookeepers = zookeeperService.getAll();
        VBox content = buildPopupContent(zookeepers);

        zookeeperPopup = new Popup();
        zookeeperPopup.setAutoHide(true);
        zookeeperPopup.getContent().add(content);

        // Show off-screen first so JavaFX can compute preferred height
        zookeeperPopup.show(userProfileArea.getScene().getWindow(), -9999, -9999);

        Platform.runLater(() -> {
            if (!zookeeperPopup.isShowing()) return;
            Bounds b = userProfileArea.localToScreen(userProfileArea.getBoundsInLocal());
            double popupH = content.prefHeight(content.getPrefWidth());
            zookeeperPopup.setX(b.getMinX() + 4);
            zookeeperPopup.setY(b.getMinY() - popupH - 6);
        });
    }

    private VBox buildPopupContent(List<Zookeeper> zookeepers) {
        VBox content = new VBox(0);
        content.setPrefWidth(220);
        content.setStyle(
            "-fx-background-color: #1B3A28;" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: rgba(255,255,255,0.12);" +
            "-fx-border-radius: 10;" +
            "-fx-border-width: 1;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.40), 18, 0, 0, -3);" +
            "-fx-padding: 6 0 4 0;"
        );

        // Section header
        Label header = new Label("ZOOKEEPERS");
        header.setStyle(
            "-fx-text-fill: #7DA990;" +
            "-fx-font-size: 10px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 4 14 6 14;"
        );
        content.getChildren().add(header);

        // Zookeeper rows
        for (Zookeeper z : zookeepers) {
            content.getChildren().add(buildZookeeperRow(z));
        }

        // Separator before "Add" action
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: rgba(255,255,255,0.08);");
        VBox.setMargin(sep, new Insets(4, 8, 2, 8));
        content.getChildren().add(sep);

        // "Add Zookeeper" row
        HBox addRow = new HBox(8);
        addRow.setAlignment(Pos.CENTER_LEFT);
        addRow.setPadding(new Insets(8, 14, 8, 14));
        addRow.setStyle("-fx-cursor: hand; -fx-background-radius: 7;");

        Label plusIcon = new Label("+");
        plusIcon.setStyle("-fx-text-fill: #34D399; -fx-font-size: 15px; -fx-font-weight: bold;");
        Label addLabel = new Label("Add Zookeeper");
        addLabel.setStyle("-fx-text-fill: #34D399; -fx-font-size: 13px;");

        addRow.getChildren().addAll(plusIcon, addLabel);
        addRow.setOnMouseClicked(e -> { zookeeperPopup.hide(); showAddZookeeperDialog(); });
        addRow.setOnMouseEntered(e -> addRow.setStyle(
            "-fx-cursor: hand; -fx-background-color: rgba(255,255,255,0.06); -fx-background-radius: 7;"));
        addRow.setOnMouseExited(e -> addRow.setStyle(
            "-fx-cursor: hand; -fx-background-radius: 7;"));
        content.getChildren().add(addRow);

        return content;
    }

    private HBox buildZookeeperRow(Zookeeper z) {
        boolean active = zookeeperService.getCurrent() != null
                      && zookeeperService.getCurrent().getId() == z.getId();

        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(6, 10, 6, 10));
        String rowBase = active
            ? "-fx-background-color: rgba(255,255,255,0.12); -fx-background-radius: 7;"
            : "-fx-background-color: transparent; -fx-background-radius: 7;";
        row.setStyle(rowBase);

        // Mini avatar circle
        Label av = new Label(z.getInitials());
        av.setStyle(
            "-fx-background-color: rgba(255,255,255,0.18);" +
            "-fx-background-radius: 13;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 11px;" +
            "-fx-font-weight: bold;" +
            "-fx-min-width: 26; -fx-min-height: 26;" +
            "-fx-max-width: 26; -fx-max-height: 26;" +
            "-fx-alignment: CENTER;"
        );

        // Name label
        Label nameLbl = new Label(z.getName());
        nameLbl.setStyle(
            "-fx-text-fill: " + (active ? "white" : "rgba(216,234,224,0.85)") + ";" +
            "-fx-font-size: 13px;" +
            (active ? "-fx-font-weight: bold;" : "")
        );
        HBox.setHgrow(nameLbl, Priority.ALWAYS);

        // Clicking the left section (avatar + name) switches to this zookeeper
        HBox leftPart = new HBox(8, av, nameLbl);
        leftPart.setAlignment(Pos.CENTER_LEFT);
        leftPart.setStyle("-fx-cursor: hand;");
        HBox.setHgrow(leftPart, Priority.ALWAYS);
        leftPart.setOnMouseClicked(e -> { zookeeperPopup.hide(); switchToZookeeper(z); });

        // Active checkmark
        Label check = new Label("✓");
        check.setStyle("-fx-text-fill: #34D399; -fx-font-size: 11px; -fx-font-weight: bold;");
        check.setVisible(active);
        check.setManaged(active);

        // Edit (pencil) button
        String btnNormal = "-fx-background-color: transparent;" +
                           "-fx-text-fill: rgba(216,234,224,0.5);" +
                           "-fx-font-size: 12px; -fx-padding: 2 5;" +
                           "-fx-cursor: hand; -fx-min-width: 24; -fx-border-width: 0;";
        String btnHover  = "-fx-background-color: rgba(255,255,255,0.1);" +
                           "-fx-background-radius: 5;" +
                           "-fx-text-fill: rgba(216,234,224,0.9);" +
                           "-fx-font-size: 12px; -fx-padding: 2 5;" +
                           "-fx-cursor: hand; -fx-min-width: 24; -fx-border-width: 0;";

        Button editBtn = new Button("✏");
        editBtn.setStyle(btnNormal);
        editBtn.setOnMouseEntered(e -> editBtn.setStyle(btnHover));
        editBtn.setOnMouseExited(e -> editBtn.setStyle(btnNormal));
        editBtn.setOnAction(e -> { zookeeperPopup.hide(); showRenameDialog(z); });

        // Delete (bin) button
        String delNormal = "-fx-background-color: transparent;" +
                           "-fx-text-fill: rgba(220,38,38,0.55);" +
                           "-fx-font-size: 12px; -fx-padding: 2 5;" +
                           "-fx-cursor: hand; -fx-min-width: 24; -fx-border-width: 0;";
        String delHover  = "-fx-background-color: rgba(220,38,38,0.15);" +
                           "-fx-background-radius: 5;" +
                           "-fx-text-fill: rgba(220,38,38,0.9);" +
                           "-fx-font-size: 12px; -fx-padding: 2 5;" +
                           "-fx-cursor: hand; -fx-min-width: 24; -fx-border-width: 0;";

        Button delBtn = new Button("🗑");
        delBtn.setStyle(delNormal);
        delBtn.setOnMouseEntered(e -> delBtn.setStyle(delHover));
        delBtn.setOnMouseExited(e -> delBtn.setStyle(delNormal));
        delBtn.setOnAction(e -> { zookeeperPopup.hide(); showDeleteConfirmation(z); });

        row.getChildren().addAll(leftPart, check, editBtn, delBtn);

        if (!active) {
            row.setOnMouseEntered(ev -> row.setStyle(
                "-fx-background-color: rgba(255,255,255,0.06); -fx-background-radius: 7;"));
            row.setOnMouseExited(ev -> row.setStyle(rowBase));
        }

        return row;
    }

    private void switchToZookeeper(Zookeeper z) {
        if (zookeeperService.getCurrent() != null
                && zookeeperService.getCurrent().getId() == z.getId()) return;

        zookeeperService.setCurrent(z);
        updateZookeeperDisplay();

        // Reset the simulation to a clean default state for the new zookeeper
        dbInitializer.resetSimulationStats();
        simulationEngine.restart();

        showDashboard();
    }

    private void showAddZookeeperDialog() {
        TextInputDialog dlg = new TextInputDialog();
        dlg.initOwner(userProfileArea.getScene().getWindow());
        dlg.setTitle("Add Zookeeper");
        dlg.setHeaderText("Enter the new zookeeper's full name:");
        dlg.setContentText("Name:");
        dlg.showAndWait().ifPresent(name -> {
            if (!name.isBlank()) {
                zookeeperService.add(name);
                updateZookeeperDisplay();
            }
        });
    }

    private void showRenameDialog(Zookeeper z) {
        TextInputDialog dlg = new TextInputDialog(z.getName());
        dlg.initOwner(userProfileArea.getScene().getWindow());
        dlg.setTitle("Rename Zookeeper");
        dlg.setHeaderText("Rename \"" + z.getName() + "\": ");
        dlg.setContentText("New name:");
        dlg.showAndWait().ifPresent(name -> {
            if (!name.isBlank()) {
                zookeeperService.rename(z.getId(), name);
                updateZookeeperDisplay();
            }
        });
    }

    private void showDeleteConfirmation(Zookeeper z) {
        javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION);
        confirm.initOwner(userProfileArea.getScene().getWindow());
        confirm.setTitle("Delete Zookeeper");
        confirm.setHeaderText("Delete \"" + z.getName() + "\"?");
        confirm.setContentText("This cannot be undone.");
        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                boolean deleted = zookeeperService.delete(z.getId());
                if (deleted && zookeeperService.getCurrent() != null
                        && zookeeperService.getCurrent().getId() == z.getId()) {
                    List<Zookeeper> remaining = zookeeperService.getAll();
                    zookeeperService.setCurrent(remaining.isEmpty() ? null : remaining.get(0));
                    updateZookeeperDisplay();
                }
            }
        });
    }

    private void updateZookeeperDisplay() {
        Zookeeper z = zookeeperService.getCurrent();
        if (z != null && userAvatar != null) {
            userAvatar.setText(z.getInitials());
            userNameLabel.setText(z.getName());
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
