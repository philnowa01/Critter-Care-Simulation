package com.crittercare.controller;

import com.crittercare.model.Alert;
import com.crittercare.model.Animal;
import com.crittercare.model.Bird;
import com.crittercare.model.Enclosure;
import com.crittercare.model.Mammal;
import com.crittercare.model.Reptile;
import com.crittercare.service.AnimalService;
import com.crittercare.service.EnclosureService;
import com.crittercare.service.MaintenanceLogService;
import com.crittercare.model.MaintenanceLog;
import javafx.scene.control.TextInputDialog;
import com.crittercare.simulation.SimulationListener;
import com.crittercare.simulation.SimulationTick;
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

import java.util.function.Function;

/**
 * Controller for Animals.fxml.
 *
 * Maintains an ObservableList<Animal> as the master data source.  A
 * FilteredList wraps it so search and type-filter work without DB round-trips.
 * onTick() replaces the master list contents from the tick snapshot, which
 * causes the FilteredList → SortedList → TableView chain to refresh
 * automatically.
 */
public class AnimalsController implements SimulationListener {

    // ── Injected services ─────────────────────────────────────────────────────
    final AnimalService    animalService;
    final EnclosureService enclosureService;
    final MaintenanceLogService logService;

    // ── FXML nodes ────────────────────────────────────────────────────────────
    @FXML TextField              searchField;
    @FXML ComboBox<String>       typeFilter;
    @FXML Button                 btnFeed;
    @FXML Button                 btnHealthCheck;

    @FXML TableView<Animal>           animalsTable;
    @FXML TableColumn<Animal, String> colName;
    @FXML TableColumn<Animal, String> colSpecies;
    @FXML TableColumn<Animal, String> colType;
    @FXML TableColumn<Animal, Number> colAge;
    @FXML TableColumn<Animal, Animal> colHealth;
    @FXML TableColumn<Animal, Animal> colHunger;
    @FXML TableColumn<Animal, Animal> colHydration;
    @FXML TableColumn<Animal, String> colEnclosure;
    @FXML TableColumn<Animal, String> colStatus;
    @FXML TableColumn<Animal, Animal> colActions;

    @FXML Label animalCountLabel;
    @FXML Label attentionCountLabel;

    // ── Reactive data layer ───────────────────────────────────────────────────
    private ObservableList<Animal> masterList;
    private FilteredList<Animal>   filteredList;

    // ── Constructor ───────────────────────────────────────────────────────────

    public AnimalsController(AnimalService animalService,
                             EnclosureService enclosureService,
                             MaintenanceLogService logService) {
        this.animalService    = animalService;
        this.enclosureService = enclosureService;
        this.logService       = logService;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        typeFilter.getItems().setAll("All Types", "MAMMAL", "BIRD", "REPTILE");
        typeFilter.getSelectionModel().selectFirst();

        // ── Cell value factories ──────────────────────────────────────────────
        colName.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getName()));
        colSpecies.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getSpecies()));
        colType.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getType()));
        colAge.setCellValueFactory(cd ->
                new SimpleObjectProperty<Number>(cd.getValue().getAge()));
        // Progress-bar columns carry the whole Animal so the cell can read any stat
        colHealth.setCellValueFactory(cd ->
                new SimpleObjectProperty<>(cd.getValue()));
        colHunger.setCellValueFactory(cd ->
                new SimpleObjectProperty<>(cd.getValue()));
        colHydration.setCellValueFactory(cd ->
                new SimpleObjectProperty<>(cd.getValue()));
        colEnclosure.setCellValueFactory(cd -> {
            int encId = cd.getValue().getEnclosureId();
            if (encId <= 0) return new SimpleStringProperty("—");
            return new SimpleStringProperty(
                    enclosureService.getEnclosureById(encId)
                            .map(Enclosure::getName)
                            .orElse("—"));
        });
        colStatus.setCellValueFactory(cd -> {
            Animal a   = cd.getValue();
            String txt = a.isHealthCritical()   ? "Critical"
                       : a.requiresAttention()  ? "Attention"
                       : "Healthy";
            return new SimpleStringProperty(txt);
        });
        colActions.setCellValueFactory(cd ->
                new SimpleObjectProperty<>(cd.getValue()));

        // ── Cell factories (rendering) ────────────────────────────────────────

        // Type chip
        colType.setCellFactory(col -> new TableCell<>() {
            private final Label chip = new Label();
            @Override
            protected void updateItem(String type, boolean empty) {
                super.updateItem(type, empty);
                if (empty || type == null) { setGraphic(null); setText(null); return; }
                chip.setText(capitalize(type));
                chip.getStyleClass().removeAll("chip-mammal", "chip-bird", "chip-reptile");
                switch (type.toUpperCase()) {
                    case "MAMMAL"  -> chip.getStyleClass().add("chip-mammal");
                    case "BIRD"    -> chip.getStyleClass().add("chip-bird");
                    case "REPTILE" -> chip.getStyleClass().add("chip-reptile");
                    default        -> {}
                }
                setGraphic(chip);
                setText(null);
            }
        });

        // Progress bars: health, satiation (inverted hunger), hydration
        colHealth.setCellFactory(col -> progressCell(a -> a.getHealth()));
        colHunger.setCellFactory(col -> progressCell(a -> 100.0 - a.getHunger()));
        colHydration.setCellFactory(col -> progressCell(a -> a.getHydration()));

        // Status chip
        colStatus.setCellFactory(col -> new TableCell<>() {
            private final Label chip = new Label();
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setGraphic(null); setText(null); return; }
                chip.setText(status);
                chip.getStyleClass().removeAll(
                        "chip-critical", "chip-warning", "chip-normal", "chip-high");
                switch (status) {
                    case "Critical"  -> chip.getStyleClass().add("chip-critical");
                    case "Attention" -> chip.getStyleClass().add("chip-warning");
                    default          -> chip.getStyleClass().add("chip-normal");
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
            protected void updateItem(Animal animal, boolean empty) {
                super.updateItem(animal, empty);
                if (empty || animal == null) { setGraphic(null); return; }
                btnEdit.setOnAction(e   -> openAnimalDialog(animal));
                btnDelete.setOnAction(e -> confirmDelete(animal));
                setGraphic(box);
            }
        });

        // ── Observable data chain ─────────────────────────────────────────────
        masterList   = FXCollections.observableArrayList(animalService.getAllAnimals());
        filteredList = new FilteredList<>(masterList, a -> true);
        SortedList<Animal> sortedList = new SortedList<>(filteredList);
        sortedList.comparatorProperty().bind(animalsTable.comparatorProperty());
        animalsTable.setItems(sortedList);

        // ── Listeners ─────────────────────────────────────────────────────────
        searchField.textProperty().addListener((obs, old, val) -> applyFilter());
        typeFilter.valueProperty().addListener((obs, old, val) -> applyFilter());

        animalsTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, sel) -> {
                    btnFeed.setDisable(sel == null);
                    btnHealthCheck.setDisable(sel == null);
                });
        btnFeed.setDisable(true);
        btnHealthCheck.setDisable(true);

        updateStatusBar();
    }

    // ── SimulationListener ────────────────────────────────────────────────────

    @Override
    public void onTick(SimulationTick tick) {
        Platform.runLater(() -> {
            masterList.setAll(tick.getAnimals());
            updateStatusBar();
        });
    }

    @Override
    public void onAlertGenerated(Alert alert) { /* no badge on this view */ }

    // ── FXML actions ──────────────────────────────────────────────────────────

    @FXML
    void showAddAnimalDialog() {
        openAnimalDialog(null);
    }

    @FXML
    void feedSelectedAnimal() {
        Animal sel = animalsTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Record Feeding");
        dlg.setHeaderText("Enter staff member name for this feeding:");
        dlg.initOwner(animalsTable.getScene().getWindow());
        dlg.showAndWait().ifPresent(name -> {
            if (name == null || name.trim().isEmpty()) {
                showError("Validation Error", "Staff member name must not be blank.");
                return;
            }
            try {
                animalService.feedAnimal(sel.getId());
                if (logService != null) {
                    MaintenanceLog log = new MaintenanceLog(sel.getId(), 0, "Feeding", name.trim());
                    logService.addLog(log);
                }
                reload();
            } catch (Exception ex) {
                showError("Feed Error", ex.getMessage());
            }
        });
    }

    @FXML
    void performHealthCheck() {
        Animal sel = animalsTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Health Check");
        dlg.setHeaderText("Enter staff member name for this health check:");
        dlg.initOwner(animalsTable.getScene().getWindow());
        dlg.showAndWait().ifPresent(name -> {
            if (name == null || name.trim().isEmpty()) {
                showError("Validation Error", "Staff member name must not be blank.");
                return;
            }
            try {
                animalService.performHealthCheck(sel.getId());
                if (logService != null) {
                    MaintenanceLog log = new MaintenanceLog(sel.getId(), 0, "Health Check", name.trim());
                    logService.addLog(log);
                }
                reload();
            } catch (Exception ex) {
                showError("Health Check Error", ex.getMessage());
            }
        });
    }

    // ── Private: dialog ───────────────────────────────────────────────────────

    private void openAnimalDialog(Animal existing) {
        boolean isEdit = (existing != null);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Edit Animal" : "Add Animal");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.initOwner(animalsTable.getScene().getWindow());
        dialog.getDialogPane().getStylesheets().addAll(
                animalsTable.getScene().getStylesheets());
        dialog.getDialogPane().getStyleClass().add("dialog-pane");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        TextField       tfName      = new TextField(isEdit ? existing.getName()    : "");
        TextField       tfSpecies   = new TextField(isEdit ? existing.getSpecies() : "");
        ComboBox<String> cbType     = new ComboBox<>();
        cbType.getItems().setAll("MAMMAL", "BIRD", "REPTILE");
        cbType.getSelectionModel().select(isEdit ? existing.getType() : "MAMMAL");
        if (isEdit) cbType.setDisable(true);  // type cannot change after creation
        TextField        tfAge      = new TextField(isEdit ? String.valueOf(existing.getAge()) : "0");
        TextField        tfKeeper   = new TextField(
                isEdit && existing.getAssignedZookeeper() != null
                ? existing.getAssignedZookeeper() : "");

        ComboBox<Enclosure> cbEnclosure = new ComboBox<>();
        cbEnclosure.getItems().setAll(enclosureService.getAllEnclosures());
        cbEnclosure.setPromptText("No Enclosure");
        if (isEdit && existing.getEnclosureId() > 0) {
            enclosureService.getEnclosureById(existing.getEnclosureId())
                    .ifPresent(cbEnclosure.getSelectionModel()::select);
        }

        grid.add(new Label("Name:"),       0, 0); grid.add(tfName,       1, 0);
        grid.add(new Label("Species:"),    0, 1); grid.add(tfSpecies,    1, 1);
        grid.add(new Label("Type:"),       0, 2); grid.add(cbType,       1, 2);
        grid.add(new Label("Age:"),        0, 3); grid.add(tfAge,        1, 3);
        grid.add(new Label("Zookeeper:"),  0, 4); grid.add(tfKeeper,     1, 4);
        grid.add(new Label("Enclosure:"),  0, 5); grid.add(cbEnclosure,  1, 5);
        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(result -> {
            if (result != ButtonType.OK) return;
            try {
                int    age   = Integer.parseInt(tfAge.getText().trim());
                String type  = isEdit ? existing.getType() : cbType.getValue();
                String name  = tfName.getText();
                String spec  = tfSpecies.getText();

                Animal animal = switch (type.toUpperCase()) {
                    case "BIRD"    -> new Bird(name, spec, age);
                    case "REPTILE" -> new Reptile(name, spec, age);
                    default        -> new Mammal(name, spec, age);
                };
                animal.setAssignedZookeeper(tfKeeper.getText().trim());
                int encId = cbEnclosure.getValue() != null
                            ? cbEnclosure.getValue().getId() : 0;

                if (isEdit) {
                    animal.setId(existing.getId());
                    animal.setHealth(existing.getHealth());
                    animal.setHunger(existing.getHunger());
                    animal.setHydration(existing.getHydration());
                    animal.setActivityLevel(existing.getActivityLevel());
                    animal.setAdmissionDate(existing.getAdmissionDate());
                    animal.setEnclosureId(encId);
                    animalService.updateAnimal(animal);
                } else {
                    animalService.addAnimal(animal, encId);
                }
                reload();
            } catch (NumberFormatException nfe) {
                showError("Validation Error", "Age must be a whole number.");
            } catch (Exception ex) {
                showError("Save Error", ex.getMessage());
            }
        });
    }

    private void confirmDelete(Animal animal) {
        javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION,
                "Delete '" + animal.getName() + "'? This cannot be undone.",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Delete");
        confirm.initOwner(animalsTable.getScene().getWindow());
        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.YES) {
                try {
                    animalService.removeAnimal(animal.getId());
                    reload();
                } catch (Exception ex) {
                    showError("Delete Error", ex.getMessage());
                }
            }
        });
    }

    // ── Private: filter & status ──────────────────────────────────────────────

    private void applyFilter() {
        String query = searchField.getText() == null
                ? "" : searchField.getText().trim().toLowerCase();
        String type  = typeFilter.getValue();
        filteredList.setPredicate(animal -> {
            boolean matchSearch = query.isEmpty()
                    || animal.getName().toLowerCase().contains(query)
                    || animal.getSpecies().toLowerCase().contains(query);
            boolean matchType   = type == null || "All Types".equals(type)
                    || type.equalsIgnoreCase(animal.getType());
            return matchSearch && matchType;
        });
        updateStatusBar();
    }

    private void updateStatusBar() {
        int  total     = masterList.size();
        int  shown     = filteredList.size();
        long attention = masterList.stream().filter(Animal::requiresAttention).count();
        animalCountLabel.setText(shown == total
                ? total + " animals"
                : shown + " of " + total + " animals");
        attentionCountLabel.setText(attention > 0
                ? attention + " need attention" : "");
    }

    private void reload() {
        masterList.setAll(animalService.getAllAnimals());
        updateStatusBar();
    }

    // ── Private: cell factory helpers ────────────────────────────────────────

    private TableCell<Animal, Animal> progressCell(Function<Animal, Double> valueOf) {
        return new TableCell<>() {
            private final ProgressBar bar = new ProgressBar(0);
            { bar.setMaxWidth(Double.MAX_VALUE); }

            @Override
            protected void updateItem(Animal animal, boolean empty) {
                super.updateItem(animal, empty);
                if (empty || animal == null) { setGraphic(null); return; }
                double pct = Math.max(0.0, Math.min(1.0, valueOf.apply(animal) / 100.0));
                bar.setProgress(pct);
                bar.getStyleClass().removeAll("danger", "warning", "good");
                if      (pct >= 0.60) bar.getStyleClass().add("good");
                else if (pct >= 0.30) bar.getStyleClass().add("warning");
                else                  bar.getStyleClass().add("danger");
                setGraphic(bar);
                setText(null);
            }
        };
    }

    private void showError(String title, String msg) {
        javafx.scene.control.Alert err = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR, msg, ButtonType.OK);
        err.setTitle(title);
        err.initOwner(animalsTable.getScene().getWindow());
        err.showAndWait();
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
