package com.crittercare.controller;

import com.crittercare.model.Alert;
import com.crittercare.model.Enclosure;
import com.crittercare.model.HabitatType;
import com.crittercare.service.AnimalService;
import com.crittercare.service.EnclosureService;
import com.crittercare.simulation.SimulationListener;
import com.crittercare.simulation.SimulationTick;
import com.crittercare.util.DateTimeUtils;
import javafx.application.Platform;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

/**
 * Controller for Enclosures.fxml.
 *
 * The cleanliness column renders a ProgressBar whose colour reacts to the
 * Enclosure thresholds defined in the model (40 % = critical, 70 % = warning).
 * onTick() pushes the post-tick enclosure snapshot into the master list so
 * the bars update automatically without a DB round-trip.
 */
public class EnclosuresController implements SimulationListener {

    // ── Injected services ─────────────────────────────────────────────────────
    final EnclosureService enclosureService;
    final AnimalService    animalService;

    // ── FXML nodes ────────────────────────────────────────────────────────────
    @FXML TextField                searchField;
    @FXML ComboBox<HabitatType>    habitatFilter;
    @FXML Button                   btnClean;

    @FXML TableView<Enclosure>              enclosuresTable;
    @FXML TableColumn<Enclosure, String>    colName;
    @FXML TableColumn<Enclosure, String>    colHabitat;
    @FXML TableColumn<Enclosure, Number>    colCapacity;
    @FXML TableColumn<Enclosure, Number>    colOccupancy;
    @FXML TableColumn<Enclosure, Enclosure> colCleanliness;
    @FXML TableColumn<Enclosure, String>    colLastCleaned;
    @FXML TableColumn<Enclosure, String>    colStatus;
    @FXML TableColumn<Enclosure, Enclosure> colActions;

    @FXML Label enclosureCountLabel;
    @FXML Label cleaningDueLabel;

    // ── New stat card labels ───────────────────────────────────────────────────
    @FXML Label       totalEnclosuresLabel;
    @FXML Label       occupiedEnclosuresLabel;
    @FXML Label       occupancyRateLabel;
    @FXML Label       cleaningDueCountLabel;
    @FXML Label       criticalIssuesLabel;
    @FXML Label       overallCleanlinessLabel;
    @FXML ProgressBar cleanlinessScoreBar;

    // ── Reactive data layer ───────────────────────────────────────────────────
    private ObservableList<Enclosure> masterList;
    private FilteredList<Enclosure>   filteredList;

    // ── Constructor ───────────────────────────────────────────────────────────

    public EnclosuresController(EnclosureService enclosureService,
                                AnimalService animalService) {
        this.enclosureService = enclosureService;
        this.animalService    = animalService;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        // Habitat filter: "All" entry + every enum value
        habitatFilter.getItems().add(null);          // null = "All Habitats"
        habitatFilter.getItems().addAll(HabitatType.values());
        habitatFilter.setPromptText("All Habitats");

        // ── Cell value factories ──────────────────────────────────────────────
        colName.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getName()));
        colHabitat.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getHabitatType().getDisplayName()));
        colCapacity.setCellValueFactory(cd ->
                new SimpleObjectProperty<Number>(cd.getValue().getCapacity()));
        colOccupancy.setCellValueFactory(cd ->
                new SimpleObjectProperty<Number>(cd.getValue().getOccupancy()));
        // Pass full Enclosure to cleanliness cell so bar can read getCleanliness()
        colCleanliness.setCellValueFactory(cd ->
                new SimpleObjectProperty<>(cd.getValue()));
        colLastCleaned.setCellValueFactory(cd ->
                new SimpleStringProperty(
                        DateTimeUtils.formatTableDateTime(cd.getValue().getLastCleaned())));
        colStatus.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getStatus()));
        colActions.setCellValueFactory(cd ->
                new SimpleObjectProperty<>(cd.getValue()));

        // ── Cell factories ────────────────────────────────────────────────────

        // Cleanliness progress bar
        colCleanliness.setCellFactory(col -> new TableCell<>() {
            private final ProgressBar bar = new ProgressBar(0);
            { bar.setMaxWidth(Double.MAX_VALUE); }

            @Override
            protected void updateItem(Enclosure enc, boolean empty) {
                super.updateItem(enc, empty);
                if (empty || enc == null) { setGraphic(null); return; }
                double pct = Math.max(0.0, Math.min(1.0, enc.getCleanliness() / 100.0));
                bar.setProgress(pct);
                bar.getStyleClass().removeAll("danger", "warning", "good");
                if      (pct >= 0.70) bar.getStyleClass().add("good");
                else if (pct >= 0.40) bar.getStyleClass().add("warning");
                else                  bar.getStyleClass().add("danger");
                setGraphic(bar);
                setText(null);
            }
        });

        // Status chip
        colStatus.setCellFactory(col -> new TableCell<>() {
            private final Label chip = new Label();
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setGraphic(null); setText(null); return; }
                chip.setText(status);
                chip.getStyleClass().removeAll(
                        "chip-critical", "chip-warning", "chip-normal");
                switch (status) {
                    case "Critical"     -> chip.getStyleClass().add("chip-critical");
                    case "Cleaning Due" -> chip.getStyleClass().add("chip-warning");
                    default             -> chip.getStyleClass().add("chip-normal");
                }
                setGraphic(chip);
                setText(null);
            }
        });

        // Action buttons
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit   = new Button("Edit");
            private final Button btnDelete = new Button("Del");
            private final HBox   box       = new HBox(6, btnEdit, btnDelete);
            {
                btnEdit.getStyleClass().add("btn-xs");
                btnDelete.getStyleClass().add("btn-xs-danger");
            }
            @Override
            protected void updateItem(Enclosure enc, boolean empty) {
                super.updateItem(enc, empty);
                if (empty || enc == null) { setGraphic(null); return; }
                btnEdit.setOnAction(e   -> openEnclosureDialog(enc));
                btnDelete.setOnAction(e -> confirmDelete(enc));
                setGraphic(box);
            }
        });

        // ── Observable data chain ─────────────────────────────────────────────
        masterList   = FXCollections.observableArrayList(enclosureService.getAllEnclosures());
        filteredList = new FilteredList<>(masterList, e -> true);
        SortedList<Enclosure> sortedList = new SortedList<>(filteredList);
        sortedList.comparatorProperty().bind(enclosuresTable.comparatorProperty());
        enclosuresTable.setItems(sortedList);

        // ── Listeners ─────────────────────────────────────────────────────────
        searchField.textProperty().addListener((obs, old, val) -> applyFilter());
        habitatFilter.valueProperty().addListener((obs, old, val) -> applyFilter());

        enclosuresTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, sel) -> btnClean.setDisable(sel == null));
        btnClean.setDisable(true);

        updateStatusBar();
    }

    // ── SimulationListener ────────────────────────────────────────────────────

    @Override
    public void onTick(SimulationTick tick) {
        Platform.runLater(() -> {
            masterList.setAll(tick.getEnclosures());
            updateStatusBar();
        });
    }

    @Override
    public void onAlertGenerated(Alert alert) { /* no badge on this view */ }

    // ── FXML actions ──────────────────────────────────────────────────────────

    @FXML
    void showAddEnclosureDialog() {
        openEnclosureDialog(null);
    }

    @FXML
    void cleanSelectedEnclosure() {
        Enclosure sel = enclosuresTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        try {
            enclosureService.cleanEnclosure(sel.getId());
            reload();
        } catch (Exception ex) {
            showError("Clean Error", ex.getMessage());
        }
    }

    // ── Private: dialog ───────────────────────────────────────────────────────

    private void openEnclosureDialog(Enclosure existing) {
        boolean isEdit = (existing != null);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Edit Enclosure" : "Add Enclosure");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.initOwner(enclosuresTable.getScene().getWindow());
        dialog.getDialogPane().getStylesheets().addAll(
                enclosuresTable.getScene().getStylesheets());
        dialog.getDialogPane().getStyleClass().add("dialog-pane");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        TextField tfName     = new TextField(isEdit ? existing.getName() : "");
        ComboBox<HabitatType> cbHabitat = new ComboBox<>();
        cbHabitat.getItems().addAll(HabitatType.values());
        cbHabitat.getSelectionModel().select(
                isEdit ? existing.getHabitatType() : HabitatType.SAVANNAH);
        TextField tfCapacity = new TextField(
                isEdit ? String.valueOf(existing.getCapacity()) : "10");
        TextField tfSchedule = new TextField(
                isEdit && existing.getMaintenanceSchedule() != null
                ? existing.getMaintenanceSchedule() : "Daily");

        grid.add(new Label("Name:"),         0, 0); grid.add(tfName,     1, 0);
        grid.add(new Label("Habitat:"),      0, 1); grid.add(cbHabitat,  1, 1);
        grid.add(new Label("Capacity:"),     0, 2); grid.add(tfCapacity, 1, 2);
        grid.add(new Label("Maintenance:"),  0, 3); grid.add(tfSchedule, 1, 3);
        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(result -> {
            if (result != ButtonType.OK) return;
            try {
                int capacity = Integer.parseInt(tfCapacity.getText().trim());
                Enclosure enc = isEdit ? existing : new Enclosure();
                enc.setName(tfName.getText().trim());
                enc.setHabitatType(cbHabitat.getValue());
                enc.setCapacity(capacity);
                enc.setMaintenanceSchedule(tfSchedule.getText().trim());

                if (isEdit) {
                    enclosureService.updateEnclosure(enc);
                } else {
                    enclosureService.createEnclosure(enc);
                }
                reload();
            } catch (NumberFormatException nfe) {
                showError("Validation Error", "Capacity must be a whole number.");
            } catch (Exception ex) {
                showError("Save Error", ex.getMessage());
            }
        });
    }

    private void confirmDelete(Enclosure enc) {
        javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION,
                "Delete '" + enc.getName() + "'? It must be empty first.",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Delete");
        confirm.initOwner(enclosuresTable.getScene().getWindow());
        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.YES) {
                try {
                    enclosureService.deleteEnclosure(enc.getId());
                    reload();
                } catch (Exception ex) {
                    showError("Delete Error", ex.getMessage());
                }
            }
        });
    }

    // ── Private: filter & status ──────────────────────────────────────────────

    private void applyFilter() {
        String      query   = searchField.getText() == null
                ? "" : searchField.getText().trim().toLowerCase();
        HabitatType habitat = habitatFilter.getValue();
        filteredList.setPredicate(enc -> {
            boolean matchSearch  = query.isEmpty()
                    || enc.getName().toLowerCase().contains(query);
            boolean matchHabitat = habitat == null
                    || enc.getHabitatType() == habitat;
            return matchSearch && matchHabitat;
        });
        updateStatusBar();
    }

    private void updateStatusBar() {
        int  total       = masterList.size();
        int  shown       = filteredList.size();
        long cleaningDue = masterList.stream().filter(Enclosure::isCleaningDue).count();
        enclosureCountLabel.setText(shown == total
                ? total + " enclosures"
                : shown + " of " + total + " enclosures");
        cleaningDueLabel.setText(cleaningDue > 0
                ? cleaningDue + " need cleaning" : "");
        updateStatCards();
    }

    private void updateStatCards() {
        int total    = masterList.size();
        long occupied = masterList.stream().filter(e -> e.getOccupancy() > 0).count();
        long cleaning = masterList.stream().filter(Enclosure::isCleaningDue).count();
        long critical = masterList.stream().filter(Enclosure::isCritical).count();
        double avgClean = masterList.isEmpty() ? 100.0
                : masterList.stream().mapToDouble(Enclosure::getCleanliness).average().orElse(100.0);

        if (totalEnclosuresLabel   != null) totalEnclosuresLabel.setText(String.valueOf(total));
        if (occupiedEnclosuresLabel != null) occupiedEnclosuresLabel.setText(String.valueOf(occupied));
        if (occupancyRateLabel     != null && total > 0)
            occupancyRateLabel.setText(String.format("%.0f%% occupancy", 100.0 * occupied / total));
        if (cleaningDueCountLabel  != null) cleaningDueCountLabel.setText(String.valueOf(cleaning));
        if (criticalIssuesLabel    != null) criticalIssuesLabel.setText(String.valueOf(critical));
        if (overallCleanlinessLabel != null)
            overallCleanlinessLabel.setText(String.format("%.0f%%", avgClean));
        if (cleanlinessScoreBar    != null)
            cleanlinessScoreBar.setProgress(avgClean / 100.0);
    }

    private void reload() {
        masterList.setAll(enclosureService.getAllEnclosures());
        updateStatusBar();
    }

    private void showError(String title, String msg) {
        javafx.scene.control.Alert err = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR, msg, ButtonType.OK);
        err.setTitle(title);
        err.initOwner(enclosuresTable.getScene().getWindow());
        err.showAndWait();
    }
}
