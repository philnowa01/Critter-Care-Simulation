package com.crittercare.controller;

import com.crittercare.model.Animal;
import com.crittercare.model.Enclosure;
import com.crittercare.model.MaintenanceLog;
import com.crittercare.service.AnimalService;
import com.crittercare.service.EnclosureService;
import com.crittercare.service.MaintenanceLogService;
import com.crittercare.util.DateTimeUtils;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.util.List;

/**
 * Controller for CareLogs.fxml.
 *
 * Does NOT implement SimulationListener — care logs are created by
 * zookeepers, not by the simulation clock.  The table is refreshed
 * after every user action (add / delete / complete).
 */
public class CareLogsController {

    // ── Injected services ─────────────────────────────────────────────────────
    final MaintenanceLogService logService;
    final AnimalService         animalService;
    final EnclosureService      enclosureService;

    // ── FXML nodes ────────────────────────────────────────────────────────────
    @FXML Label todaysLogsLabel;
    @FXML Label feedingsLabel;
    @FXML Label sanitationLabel;
    @FXML Label pendingLabel;

    @FXML TextField        searchField;
    @FXML ComboBox<String> activityFilter;
    @FXML ComboBox<String> statusFilter;

    @FXML TableView<MaintenanceLog>              logsTable;
    @FXML TableColumn<MaintenanceLog, String>    colTimestamp;
    @FXML TableColumn<MaintenanceLog, String>    colActivity;
    @FXML TableColumn<MaintenanceLog, String>    colStaff;
    @FXML TableColumn<MaintenanceLog, String>    colSubject;
    @FXML TableColumn<MaintenanceLog, String>    colStatus;
    @FXML TableColumn<MaintenanceLog, String>    colNotes;
    @FXML TableColumn<MaintenanceLog, Boolean>   colFollowUp;
    @FXML TableColumn<MaintenanceLog, MaintenanceLog> colActions;

    // ── Reactive data layer ───────────────────────────────────────────────────
    private ObservableList<MaintenanceLog> masterList;
    private FilteredList<MaintenanceLog>   filteredList;

    // ── Constructor ───────────────────────────────────────────────────────────

    public CareLogsController(MaintenanceLogService logService,
                              AnimalService animalService,
                              EnclosureService enclosureService) {
        this.logService       = logService;
        this.animalService    = animalService;
        this.enclosureService = enclosureService;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    private static final List<String> ACTIVITY_TYPES = List.of(
            "Feeding", "Sanitation", "Health Check", "Enrichment", "Intervention", "Other");
    private static final List<String> STATUS_OPTIONS  = List.of(
            "Completed", "In Progress", "Pending");

    @FXML
    public void initialize() {
        // Populate filter combos
        activityFilter.getItems().add(null);
        activityFilter.getItems().addAll(ACTIVITY_TYPES);
        activityFilter.setPromptText("All Activities");

        statusFilter.getItems().add(null);
        statusFilter.getItems().addAll(STATUS_OPTIONS);
        statusFilter.setPromptText("All Statuses");

        // ── Cell value factories ──────────────────────────────────────────────
        colTimestamp.setCellValueFactory(cd ->
                new SimpleStringProperty(
                        DateTimeUtils.formatTableDateTime(cd.getValue().getTimestamp())));
        colActivity.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getActivityType()));
        colStaff.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getStaffMember()));
        colSubject.setCellValueFactory(cd -> {
            MaintenanceLog log = cd.getValue();
            StringBuilder  sb  = new StringBuilder();
            if (log.getAnimalId() > 0) {
                animalService.getAnimalById(log.getAnimalId())
                        .ifPresent(a -> sb.append(a.getName()));
            }
            if (log.getEnclosureId() > 0) {
                if (!sb.isEmpty()) sb.append(" / ");
                enclosureService.getEnclosureById(log.getEnclosureId())
                        .ifPresent(e -> sb.append(e.getName()));
            }
            return new SimpleStringProperty(sb.toString());
        });
        colStatus.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getStatus()));
        colNotes.setCellValueFactory(cd -> {
            String notes = cd.getValue().getNotes();
            return new SimpleStringProperty(notes != null ? notes : "");
        });
        colFollowUp.setCellValueFactory(cd ->
                new SimpleObjectProperty<>(cd.getValue().isFollowUpNeeded()));
        colActions.setCellValueFactory(cd ->
                new SimpleObjectProperty<>(cd.getValue()));

        // ── Cell factories ────────────────────────────────────────────────────

        // Status chip
        colStatus.setCellFactory(col -> new TableCell<>() {
            private final Label chip = new Label();
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setGraphic(null); setText(null); return; }
                chip.setText(status);
                chip.getStyleClass().removeAll(
                        "chip-normal", "chip-warning", "chip-critical");
                switch (status) {
                    case "Completed"   -> chip.getStyleClass().add("chip-completed");
                    case "In Progress" -> chip.getStyleClass().add("chip-in-progress");
                    default            -> chip.getStyleClass().add("chip-pending");
                }
                setGraphic(chip);
                setText(null);
            }
        });

        // Follow-up boolean chip
        colFollowUp.setCellFactory(col -> new TableCell<>() {
            private final Label chip = new Label();
            @Override
            protected void updateItem(Boolean needed, boolean empty) {
                super.updateItem(needed, empty);
                if (empty || needed == null) { setGraphic(null); setText(null); return; }
                if (needed) {
                    chip.setText("Yes");
                    chip.getStyleClass().removeAll("chip-normal");
                    chip.getStyleClass().add("chip-warning");
                } else {
                    chip.setText("No");
                    chip.getStyleClass().removeAll("chip-warning");
                    chip.getStyleClass().add("chip-normal");
                }
                setGraphic(chip);
                setText(null);
            }
        });

        // Action buttons
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnComplete = new Button("Done");
            private final Button btnDelete   = new Button("Del");
            private final HBox   box         = new HBox(6, btnComplete, btnDelete);
            {
                btnComplete.getStyleClass().add("btn-xs");
                btnDelete.getStyleClass().add("btn-xs-danger");
            }
            @Override
            protected void updateItem(MaintenanceLog log, boolean empty) {
                super.updateItem(log, empty);
                if (empty || log == null) { setGraphic(null); return; }
                btnComplete.setDisable(log.isCompleted());
                btnComplete.setOnAction(e -> markCompleted(log));
                btnDelete.setOnAction(e   -> confirmDelete(log));
                setGraphic(box);
            }
        });

        // ── Observable data chain ─────────────────────────────────────────────
        masterList   = FXCollections.observableArrayList(logService.getAllLogs());
        filteredList = new FilteredList<>(masterList, l -> true);
        SortedList<MaintenanceLog> sortedList = new SortedList<>(filteredList);
        sortedList.comparatorProperty().bind(logsTable.comparatorProperty());
        logsTable.setItems(sortedList);

        // ── Listeners ─────────────────────────────────────────────────────────
        searchField.textProperty().addListener((obs, old, val) -> applyFilter());
        activityFilter.valueProperty().addListener((obs, old, val) -> applyFilter());
        statusFilter.valueProperty().addListener((obs, old, val) -> applyFilter());

        refreshSummaryCards();
    }

    // ── FXML actions ──────────────────────────────────────────────────────────

    @FXML
    void showAddLogDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add Care Log");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.initOwner(logsTable.getScene().getWindow());
        dialog.getDialogPane().getStylesheets().addAll(
                logsTable.getScene().getStylesheets());
        dialog.getDialogPane().getStyleClass().add("dialog-pane");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        ComboBox<String>  cbActivity  = new ComboBox<>();
        cbActivity.getItems().addAll(ACTIVITY_TYPES);
        cbActivity.getSelectionModel().selectFirst();

        ComboBox<String>  cbStatus    = new ComboBox<>();
        cbStatus.getItems().addAll(STATUS_OPTIONS);
        cbStatus.getSelectionModel().selectFirst();

        TextField        tfStaff     = new TextField();

        ComboBox<Animal> cbAnimal    = new ComboBox<>();
        cbAnimal.getItems().setAll(animalService.getAllAnimals());
        cbAnimal.setPromptText("No Animal");

        ComboBox<Enclosure> cbEnclosure = new ComboBox<>();
        cbEnclosure.getItems().setAll(enclosureService.getAllEnclosures());
        cbEnclosure.setPromptText("No Enclosure");

        TextArea  taNotes   = new TextArea();
        taNotes.setPrefRowCount(3);
        taNotes.setWrapText(true);

        CheckBox  chkFollowUp = new CheckBox();

        grid.add(new Label("Activity:"),   0, 0); grid.add(cbActivity,   1, 0);
        grid.add(new Label("Staff:"),      0, 1); grid.add(tfStaff,      1, 1);
        grid.add(new Label("Status:"),     0, 2); grid.add(cbStatus,     1, 2);
        grid.add(new Label("Animal:"),     0, 3); grid.add(cbAnimal,     1, 3);
        grid.add(new Label("Enclosure:"),  0, 4); grid.add(cbEnclosure,  1, 4);
        grid.add(new Label("Notes:"),      0, 5); grid.add(taNotes,      1, 5);
        grid.add(new Label("Follow-Up:"),  0, 6); grid.add(chkFollowUp,  1, 6);
        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(result -> {
            if (result != ButtonType.OK) return;
            int animalId    = cbAnimal.getValue()    != null ? cbAnimal.getValue().getId()    : 0;
            int enclosureId = cbEnclosure.getValue() != null ? cbEnclosure.getValue().getId() : 0;
            if (animalId <= 0 && enclosureId <= 0) {
                showError("Validation Error",
                        "Please select at least one Animal or Enclosure.");
                return;
            }
            try {
                MaintenanceLog log = new MaintenanceLog(
                        animalId, enclosureId,
                        cbActivity.getValue(),
                        tfStaff.getText().trim());
                log.setStatus(cbStatus.getValue());
                log.setNotes(taNotes.getText().trim());
                log.setFollowUpNeeded(chkFollowUp.isSelected());
                logService.addLog(log);
                applyLogActivity(log);
                reload();
            } catch (Exception ex) {
                showError("Save Error", ex.getMessage());
            }
        });
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void markCompleted(MaintenanceLog log) {
        try {
            logService.markAsCompleted(log.getId());
            applyCompletedLogActivity(log.getActivityType(), log.getAnimalId(), log.getEnclosureId());
            reload();
        } catch (Exception ex) {
            showError("Update Error", ex.getMessage());
        }
    }

    private void applyLogActivity(MaintenanceLog log) {
        if (log == null || log.getActivityType() == null || !"Completed".equalsIgnoreCase(log.getStatus())) {
            return;
        }
        applyCompletedLogActivity(log.getActivityType(), log.getAnimalId(), log.getEnclosureId());
    }

    private void applyCompletedLogActivity(String activityType, int animalId, int enclosureId) {
        if (activityType == null) {
            return;
        }

        try {
            switch (activityType) {
                case "Feeding" -> {
                    if (animalId > 0) {
                        animalService.feedAnimal(animalId);
                    }
                }
                case "Health Check" -> {
                    if (animalId > 0) {
                        animalService.performHealthCheck(animalId);
                    }
                }
                case "Sanitation" -> {
                    if (enclosureId > 0) {
                        enclosureService.cleanEnclosure(enclosureId);
                    }
                }
                default -> {
                    // no simulation side effect for other log types
                }
            }
        } catch (Exception ex) {
            showError("Action Error", "Could not apply care log action: " + ex.getMessage());
        }
    }

    private void confirmDelete(MaintenanceLog log) {
        javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION,
                "Delete this log entry?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Delete");
        confirm.initOwner(logsTable.getScene().getWindow());
        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.YES) {
                try {
                    logService.deleteLog(log.getId());
                    reload();
                } catch (Exception ex) {
                    showError("Delete Error", ex.getMessage());
                }
            }
        });
    }

    private void applyFilter() {
        String query    = searchField.getText() == null
                ? "" : searchField.getText().trim().toLowerCase();
        String activity = activityFilter.getValue();
        String status   = statusFilter.getValue();

        filteredList.setPredicate(log -> {
            boolean matchSearch   = query.isEmpty()
                    || (log.getActivityType() != null
                        && log.getActivityType().toLowerCase().contains(query))
                    || (log.getStaffMember() != null
                        && log.getStaffMember().toLowerCase().contains(query));
            boolean matchActivity = activity == null
                    || activity.equalsIgnoreCase(log.getActivityType());
            boolean matchStatus   = status == null
                    || status.equalsIgnoreCase(log.getStatus());
            return matchSearch && matchActivity && matchStatus;
        });
    }

    private void refreshSummaryCards() {
        todaysLogsLabel.setText(String.valueOf(logService.getTodaysLogCount()));
        feedingsLabel.setText(String.valueOf(logService.getTodaysFeedingCount()));
        sanitationLabel.setText(String.valueOf(logService.getTodaysSanitationCount()));
        pendingLabel.setText(String.valueOf(logService.getPendingInterventionCount()));
    }

    private void reload() {
        masterList.setAll(logService.getAllLogs());
        refreshSummaryCards();
    }

    private void showError(String title, String msg) {
        javafx.scene.control.Alert err = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR, msg, ButtonType.OK);
        err.setTitle(title);
        err.initOwner(logsTable.getScene().getWindow());
        err.showAndWait();
    }
}
