import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class Main extends Application {

    // Globale Variablen, damit wir sie bei der Auflösungsänderung ansteuern können
    private double zoomFactor = 1.0;
    private Label zoomLabel;
    private Pane worldPane;
    private BorderPane root;
    private ProgressBar healthBar;
    private ProgressBar secondaryBar;

    private double lastMouseX;
    private double lastMouseY;

    @Override
    public void start(Stage primaryStage) {
        root = new BorderPane();

        // ==========================================
        // 1. OBERE ZEILE (Header-Bar)
        // ==========================================
        HBox topHeader = new HBox();
        topHeader.setPadding(new Insets(10, 15, 10, 15));
        topHeader.setStyle("-fx-background-color: #333333;");
        topHeader.setAlignment(Pos.CENTER_LEFT);

        VBox leftContainer = new VBox(8);
        HBox flatMenu = new HBox(10);
        flatMenu.setAlignment(Pos.CENTER_LEFT);

        Button btnNew = new Button("+ new");
        Button btnSave = new Button("save");
        CheckBox cbAutoSave = new CheckBox("turn auto save on");
        cbAutoSave.setStyle("-fx-text-fill: white;");
        Button btnLoad = new Button("load");

        // --- NEU: Settings MenuButton ---
        MenuButton btnSettings = new MenuButton("settings");
        Menu menuResolution = new Menu("resolution");

        MenuItem resDefault = new MenuItem("1000 x 700 (Default)");
        resDefault.setOnAction(e -> applyResolution(primaryStage, 1000, 700, 1.0));

        MenuItem res720p = new MenuItem("1280 x 720 (720p)");
        res720p.setOnAction(e -> applyResolution(primaryStage, 1280, 720, 1.25));

        MenuItem res1080p = new MenuItem("1920 x 1080 (1080p)");
        res1080p.setOnAction(e -> applyResolution(primaryStage, 1920, 1080, 1.8));

        MenuItem res1440p = new MenuItem("2560 x 1440 (1440p)");
        res1440p.setOnAction(e -> applyResolution(primaryStage, 2560, 1440, 2.5));

        menuResolution.getItems().addAll(resDefault, res720p, res1080p, res1440p);
        btnSettings.getItems().add(menuResolution);
        // ---------------------------------

        flatMenu.getChildren().addAll(btnNew, btnSave, cbAutoSave, btnLoad, btnSettings);

        zoomLabel = new Label("Zoom: 100%");
        zoomLabel.setStyle("-fx-text-fill: #00ff00; -fx-font-weight: bold; -fx-font-family: 'Courier New';");

        leftContainer.getChildren().addAll(flatMenu, zoomLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox rightContainer = new VBox(6);
        rightContainer.setAlignment(Pos.CENTER_LEFT);

        Label lblHealth = new Label("Overall Health");
        lblHealth.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        healthBar = new ProgressBar(0.80);
        healthBar.setPrefWidth(220);
        healthBar.setStyle("-fx-accent: #ff3333;");

        secondaryBar = new ProgressBar(0.55);
        secondaryBar.setPrefWidth(220);
        secondaryBar.setStyle("-fx-accent: #33ccff;");

        rightContainer.getChildren().addAll(lblHealth, healthBar, secondaryBar);
        topHeader.getChildren().addAll(leftContainer, spacer, rightContainer);

        root.setTop(topHeader);

        // ==========================================
        // 2. MITTE (Viewport & Welt)
        // ==========================================
        Pane viewportPane = new Pane();
        viewportPane.setStyle("-fx-background-color: #1e1e1e;");

        Rectangle clipRect = new Rectangle();
        clipRect.widthProperty().bind(viewportPane.widthProperty());
        clipRect.heightProperty().bind(viewportPane.heightProperty());
        viewportPane.setClip(clipRect);

        worldPane = new Pane();
        worldPane.setStyle("-fx-background-color: #2b2b2b;");
        worldPane.setPrefSize(5000, 5000);

        Rectangle testObject = new Rectangle(400, 300, 150, 150);
        testObject.setFill(Color.CADETBLUE);
        testObject.setStroke(Color.WHITE);
        testObject.setStrokeWidth(2);
        worldPane.getChildren().add(testObject);

        viewportPane.getChildren().add(worldPane);
        root.setCenter(viewportPane);

        // ==========================================
        // 3. NAVIGATION (Zoom & Panning)
        // ==========================================
        viewportPane.setOnScroll((ScrollEvent event) -> {
            if (event.getDeltaY() == 0) return;

            double multiplier = (event.getDeltaY() > 0) ? 1.05 : 1.0 / 1.05;
            double newZoom = zoomFactor * multiplier;

            if (newZoom >= 0.2 && newZoom <= 5.0) {
                Point2D mouseInWorld = worldPane.sceneToLocal(event.getSceneX(), event.getSceneY());

                zoomFactor = newZoom;
                worldPane.setScaleX(zoomFactor);
                worldPane.setScaleY(zoomFactor);

                Point2D newMouseInScene = worldPane.localToScene(mouseInWorld);
                double deltaX = event.getSceneX() - newMouseInScene.getX();
                double deltaY = event.getSceneY() - newMouseInScene.getY();

                worldPane.setTranslateX(worldPane.getTranslateX() + deltaX);
                worldPane.setTranslateY(worldPane.getTranslateY() + deltaY);

                zoomLabel.setText(String.format("Zoom: %.0f%%", zoomFactor * 100));
            }
            event.consume();
        });

        viewportPane.setOnMousePressed(event -> {
            if (event.isPrimaryButtonDown()) {
                lastMouseX = event.getSceneX();
                lastMouseY = event.getSceneY();
            }
        });

        viewportPane.setOnMouseDragged(event -> {
            if (event.isPrimaryButtonDown()) {
                double deltaX = event.getSceneX() - lastMouseX;
                double deltaY = event.getSceneY() - lastMouseY;
                worldPane.setTranslateX(worldPane.getTranslateX() + deltaX);
                worldPane.setTranslateY(worldPane.getTranslateY() + deltaY);
                lastMouseX = event.getSceneX();
                lastMouseY = event.getSceneY();
            }
        });

        // ==========================================
        // 4. FENSTER-EINSTELLUNGEN
        // ==========================================
        Scene scene = new Scene(root, 1000, 700);
        // Wir setzen die Standard-Schriftgröße initial
        root.setStyle("-fx-font-size: 12px;");

        primaryStage.setTitle("CritterCare System");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // --- NEU: Die Methode zur Anpassung von Auflösung und UI-Skalierung ---
    private void applyResolution(Stage stage, double width, double height, double uiScale) {
        // 1. Fenstergröße anpassen und zentrieren
        stage.setWidth(width);
        stage.setHeight(height);
        stage.centerOnScreen();

        // 2. DPI/UI skalieren: Wir vergrößern die Basis-Schriftart,
        // wodurch alle JavaFX-Buttons und Texte automatisch größer gerendert werden.
        double newFontSize = 12 * uiScale;
        root.setStyle("-fx-font-size: " + newFontSize + "px;");

        // 3. Fest definierte Breiten (wie die ProgressBars) manuell mitskalieren
        healthBar.setPrefWidth(220 * uiScale);
        secondaryBar.setPrefWidth(220 * uiScale);

        // 4. Den Zoom der Map an die neue Auflösung anpassen
        zoomFactor = uiScale;
        worldPane.setScaleX(zoomFactor);
        worldPane.setScaleY(zoomFactor);
        zoomLabel.setText(String.format("Zoom: %.0f%%", zoomFactor * 100));
    }

    public static void main(String[] args) {
        launch(args);
    }
}