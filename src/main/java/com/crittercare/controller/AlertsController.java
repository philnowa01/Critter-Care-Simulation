package com.crittercare.controller;

import com.crittercare.model.Alert;
import com.crittercare.model.AlertSeverity;
import com.crittercare.service.AlertService;
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
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class AlertsController implements SimulationListener {

    private static final String CSS_CRITICAL    = "chip-critical";
    private static final String CSS_HIGH        = "chip-high";
    private static final String CSS_WARNING     = "chip-warning";
    private static final String CSS_NORMAL      = "chip-normal";
    private static final String CSS_NEW         = "chip-new";
    private static final String CSS_ACKNOWLEDGED = "chip-acknowledged";
    private static final String CSS_RESOLVED    = "chip-resolved";

    // ── Services ──────────────────────────────────────────────────────────────
    final AlertService alertService;

    // ── FXML: summary cards ───────────────────────────────────────────────────
    @FXML Label activeAlertsLabel;
    @FXML Label criticalLabel;
    @FXML Label highLabel;
    @FXML Label totalAlertsLabel;
    @FXML Label activeCountHeading;

    // ── FXML: filter toolbar ──────────────────────────────────────────────────
    @FXML ToggleGroup  filterGroup;
    @FXML ToggleButton filterAll;
    @FXML ToggleButton filterActive;
    @FXML ToggleButton filterCritical;
    @FXML ToggleButton filterResolved;
    @FXML Button       btnResolveAll;

    // ── FXML: table ───────────────────────────────────────────────────────────
    @FXML TableView<Alert>           alertsTable;
    @FXML TableColumn<Alert, String> colTime;
    @FXML TableColumn<Alert, String> colType;
    @FXML TableColumn<Alert, Alert>  colSeverity;
    @FXML TableColumn<Alert, String> colSource;
    @FXML TableColumn<Alert, String> colMessage;
    @FXML TableColumn<Alert, Alert>  colStatus;
    @FXML TableColumn<Alert, Alert>  colActions;

    // ── FXML: detail panel ────────────────────────────────────────────────────
    @FXML VBox   detailPanel;
    @FXML Label  detailSeverityChip;
    @FXML Label  detailSourceIcon;
    @FXML Label  detailSourceName;
    @FXML Label  detailSourceId;
    @FXML Label  detailAlertId;
    @FXML Label  detailTimeRaised;
    @FXML Label  detailCategory;
    @FXML Label  detailStatus;
    @FXML Label  detailMessage;
    @FXML Button detailAckBtn;
    @FXML Button detailResolveBtn;

    private Alert selectedAlert;

    // ── Reactive data ─────────────────────────────────────────────────────────
    private ObservableList<Alert> masterList;
    private FilteredList<Alert>   filteredList;

    // ── Constructor ───────────────────────────────────────────────────────────

    public AlertsController(AlertService alertService) {
        this.alertService = alertService;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        setupValueFactories();
        setupCellFactories();

        masterList   = FXCollections.observableArrayList(alertService.getAllAlerts());
        filteredList = new FilteredList<>(masterList, a -> true);
        SortedList<Alert> sortedList = new SortedList<>(filteredList);
        sortedList.comparatorProperty().bind(alertsTable.comparatorProperty());
        alertsTable.setItems(sortedList);

        filterGroup.selectedToggleProperty().addListener(
                (obs, old, sel) -> updatePredicate(sel));
        alertsTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, sel) -> showDetail(sel));

        updateSummaryCards();
        alertService.addChangeListener(() -> {
            if (Platform.isFxApplicationThread()) reloadAndRefresh();
            else Platform.runLater(this::reloadAndRefresh);
        });
    }

    private void setupValueFactories() {
        colTime.setCellValueFactory(cd ->
                new SimpleStringProperty(DateTimeUtils.timeAgo(cd.getValue().getTimeRaised())));
        colType.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getType() != null
                        ? cd.getValue().getType().getDisplayName() : ""));
        colSeverity.setCellValueFactory(cd -> new SimpleObjectProperty<>(cd.getValue()));
        colSource.setCellValueFactory(cd  -> new SimpleStringProperty(cd.getValue().getSourceName()));
        colMessage.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getMessage()));
        colStatus.setCellValueFactory(cd  -> new SimpleObjectProperty<>(cd.getValue()));
        colActions.setCellValueFactory(cd -> new SimpleObjectProperty<>(cd.getValue()));
    }

    private void setupCellFactories() {
        colSeverity.setCellFactory(col -> severityChipCell());
        colStatus.setCellFactory(col   -> statusChipCell());
        colActions.setCellFactory(col  -> actionButtonsCell());
    }

    // ── SimulationListener ────────────────────────────────────────────────────

    @Override
    public void onTick(SimulationTick tick) {
        Platform.runLater(this::reloadAndRefresh);
    }

    @Override
    public void onAlertGenerated(Alert alert) {
        Platform.runLater(this::reloadAndRefresh);
    }

    // ── FXML actions ──────────────────────────────────────────────────────────

    @FXML void applyFilter()  { updatePredicate(filterGroup.getSelectedToggle()); }
    @FXML void refreshAlerts() { reloadAndRefresh(); }

    @FXML
    void resolveAllAlerts() {
        alertService.getActiveAlerts().forEach(a -> alertService.resolveAlert(a.getId()));
        reloadAndRefresh();
    }

    @FXML
    void closeDetailPanel() {
        selectedAlert = null;
        setDetailVisible(false);
        alertsTable.getSelectionModel().clearSelection();
    }

    @FXML
    void acknowledgeFromDetail() {
        if (selectedAlert != null) acknowledgeOne(selectedAlert);
    }

    @FXML
    void resolveFromDetail() {
        if (selectedAlert != null) resolveOne(selectedAlert);
    }

    // ── Cell factory helpers ──────────────────────────────────────────────────

    private TableCell<Alert, Alert> severityChipCell() {
        return new TableCell<>() {
            private final Label chip = new Label();
            @Override
            protected void updateItem(Alert alert, boolean empty) {
                super.updateItem(alert, empty);
                if (empty || alert == null || alert.getSeverity() == null) {
                    setGraphic(null); setText(null); return;
                }
                chip.setText(alert.getSeverity().getDisplayName());
                applySeverityStyle(chip, alert.getSeverity());
                setGraphic(chip); setText(null);
            }
        };
    }

    private TableCell<Alert, Alert> statusChipCell() {
        return new TableCell<>() {
            private final Label chip = new Label();
            @Override
            protected void updateItem(Alert alert, boolean empty) {
                super.updateItem(alert, empty);
                if (empty || alert == null) { setGraphic(null); setText(null); return; }
                chip.setText(alert.getStatus());
                applyStatusStyle(chip, alert);
                setGraphic(chip); setText(null);
            }
        };
    }

    private TableCell<Alert, Alert> actionButtonsCell() {
        return new TableCell<>() {
            private final Button btnResolve = new Button("Resolve");
            private final Button btnAck     = new Button("Ack");
            private final HBox   box        = new HBox(6, btnAck, btnResolve);
            {
                btnResolve.getStyleClass().add("btn-xs");
                btnAck.getStyleClass().add("btn-xs");
            }
            @Override
            protected void updateItem(Alert alert, boolean empty) {
                super.updateItem(alert, empty);
                if (empty || alert == null) { setGraphic(null); return; }
                btnResolve.setDisable(alert.isResolved());
                btnAck.setDisable(!alert.isNew());
                btnResolve.setOnAction(e -> resolveOne(alert));
                btnAck.setOnAction(e     -> acknowledgeOne(alert));
                setGraphic(box);
            }
        };
    }

    // ── Style helpers ─────────────────────────────────────────────────────────

    private static void applySeverityStyle(Label chip, AlertSeverity severity) {
        chip.getStyleClass().removeAll(CSS_CRITICAL, CSS_HIGH, CSS_WARNING, CSS_NORMAL);
        chip.getStyleClass().add("chip");
        String css = switch (severity) {
            case CRITICAL -> CSS_CRITICAL;
            case HIGH     -> CSS_HIGH;
            case WARNING  -> CSS_WARNING;
            default       -> CSS_NORMAL;
        };
        chip.getStyleClass().add(css);
    }

    private static void applyStatusStyle(Label chip, Alert alert) {
        chip.getStyleClass().removeAll(CSS_NEW, CSS_ACKNOWLEDGED, CSS_RESOLVED, CSS_WARNING, CSS_NORMAL);
        chip.getStyleClass().add("chip");
        String css;
        if (alert.isResolved()) {
            css = CSS_RESOLVED;
        } else if ("New".equals(alert.getStatus())) {
            css = CSS_NEW;
        } else if ("Acknowledged".equals(alert.getStatus())) {
            css = CSS_ACKNOWLEDGED;
        } else {
            css = CSS_WARNING;
        }
        chip.getStyleClass().add(css);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void showDetail(Alert alert) {
        if (alert == null) { setDetailVisible(false); return; }
        selectedAlert = alert;

        if (detailSeverityChip != null && alert.getSeverity() != null) {
            detailSeverityChip.setText(alert.getSeverity().getDisplayName());
            applySeverityStyle(detailSeverityChip, alert.getSeverity());
        }

        setText(detailSourceName, alert.getSourceName());
        setText(detailSourceId,   alert.getSourceId() != null ? alert.getSourceId() : "");
        setText(detailAlertId,    "ALRT-" + alert.getId());
        setText(detailTimeRaised, alert.getFormattedTimeRaised());
        setText(detailCategory,   alert.getType() != null ? alert.getType().getDisplayName() : "—");
        setText(detailStatus,     alert.getStatus());
        setText(detailMessage,    alert.getMessage());

        if (detailAckBtn     != null) detailAckBtn.setDisable(!alert.isNew());
        if (detailResolveBtn != null) detailResolveBtn.setDisable(alert.isResolved());

        setDetailVisible(true);
    }

    private void setDetailVisible(boolean visible) {
        if (detailPanel != null) {
            detailPanel.setVisible(visible);
            detailPanel.setManaged(visible);
        }
    }

    private void resolveOne(Alert alert) {
        try {
            alertService.resolveAlert(alert.getId());
            reloadAndRefresh();
        } catch (Exception ex) {
            showError("Resolve Error", ex.getMessage());
        }
    }

    private void acknowledgeOne(Alert alert) {
        try {
            alertService.acknowledgeAlert(alert.getId());
            reloadAndRefresh();
        } catch (Exception ex) {
            showError("Acknowledge Error", ex.getMessage());
        }
    }

    private void updatePredicate(Toggle selected) {
        if (selected == filterActive) {
            filteredList.setPredicate(a -> !a.isResolved());
        } else if (selected == filterCritical) {
            filteredList.setPredicate(a -> !a.isResolved() && a.getSeverity() == AlertSeverity.CRITICAL);
        } else if (selected == filterResolved) {
            filteredList.setPredicate(Alert::isResolved);
        } else {
            filteredList.setPredicate(a -> true);
        }
        updateHeading();
    }

    private void reloadAndRefresh() {
        masterList.setAll(alertService.getAllAlerts());
        updateSummaryCards();
        updateHeading();
        if (selectedAlert != null) {
            masterList.stream()
                    .filter(a -> a.getId() == selectedAlert.getId())
                    .findFirst()
                    .ifPresentOrElse(this::showDetail, () -> setDetailVisible(false));
        }
    }

    private void updateSummaryCards() {
        long active   = alertService.getActiveAlertCount();
        long critical = alertService.getCriticalAlertCount();
        long high     = alertService.getActiveAlerts().stream()
                .filter(a -> a.getSeverity() == AlertSeverity.HIGH).count();
        long total    = alertService.getAllAlerts().size();

        setText(activeAlertsLabel, String.valueOf(active));
        setText(criticalLabel,     String.valueOf(critical));
        setText(highLabel,         String.valueOf(high));
        setText(totalAlertsLabel,  String.valueOf(total));
    }

    private void updateHeading() {
        if (activeCountHeading != null) {
            activeCountHeading.setText("Active Alerts (" + filteredList.size() + ")");
        }
    }

    private static void setText(Label label, String text) {
        if (label != null) label.setText(text);
    }

    private void showError(String title, String msg) {
        javafx.scene.control.Alert err = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR, msg, ButtonType.OK);
        err.setTitle(title);
        err.initOwner(alertsTable.getScene().getWindow());
        err.showAndWait();
    }
}
