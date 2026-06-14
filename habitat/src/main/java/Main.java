import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.Toolkit;
import java.time.LocalDate;

public class Main extends Application {

    // Das Spiel startet am 01.01.2026
    private LocalDate ingameDate = LocalDate.of(2026, 1, 1);

    // Startzeit ist 08:00 Uhr, deshalb 8 * 60 Minuten
    private int ingameMinutes = 8 * 60;

    // Damit wird gesteuert, wie schnell die Zeit im Spiel läuft
    private int speedMultiplier = 1;

    // Label für den Alarmtext oben im Fenster
    private Label alarmLabel;

    @Override
    public void start(Stage stage) {

        Label title = new Label("CritterCare Zoo Map");
        title.setStyle("-fx-font-size: 36px; -fx-font-weight: bold;");

        Label clockLabel = new Label();
        clockLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        updateClock(clockLabel);

        alarmLabel = new Label("No active alarms");
        setNormalAlarmLabel();

        // Buttons, mit denen man die Simulationsgeschwindigkeit ändern kann
        Button speed1Button = new Button("Speed x1");
        Button speed5Button = new Button("Speed x5");
        Button speed50Button = new Button("Speed x50");

        speed1Button.setOnAction(event -> speedMultiplier = 1);
        speed5Button.setOnAction(event -> speedMultiplier = 5);
        speed50Button.setOnAction(event -> speedMultiplier = 50);

        HBox speedButtons = new HBox(10, speed1Button, speed5Button, speed50Button);
        speedButtons.setAlignment(Pos.CENTER);

        // GridPane wird benutzt, damit die Habitate wie eine Karte angeordnet werden
        GridPane map = new GridPane();
        map.setHgap(25);
        map.setVgap(25);
        map.setAlignment(Pos.CENTER);
        map.setPadding(new Insets(20));

        // Hier werden die vier Habitate erstellt
        HabitatBox jungle = createHabitat("Jungle", "🐒 🦜", "#4CAF50", 0.45, 0.60);
        HabitatBox mountains = createHabitat("Mountains", "🐐 🦅", "#B0BEC5", 0.30, 0.50);
        HabitatBox savannah = createHabitat("Savannah", "🦁 🦒", "#DDBB77", 0.55, 0.35);
        HabitatBox forest = createHabitat("Forest", "🦌 🐻", "#81C784", 0.70, 0.40);

        // Habitate werden in ein 2x2 Raster gesetzt
        map.add(jungle.box, 0, 0);
        map.add(mountains.box, 1, 0);
        map.add(savannah.box, 0, 1);
        map.add(forest.box, 1, 1);

        // Array, damit man später alle Habitate in einer Schleife prüfen kann
        HabitatBox[] habitats = {jungle, mountains, savannah, forest};

        // Diese Timeline ist die simulierte Uhr
        // Jede echte Sekunde wird hier einmal ausgeführt
        Timeline clock = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> {

                    // Je nach Geschwindigkeit werden mehr Ingame-Minuten addiert
                    ingameMinutes += speedMultiplier;

                    // Wenn 24 Stunden erreicht sind, wird ein neuer Tag gestartet
                    while (ingameMinutes >= 24 * 60) {
                        ingameMinutes -= 24 * 60;
                        ingameDate = ingameDate.plusDays(1);
                    }

                    // Bei jedem Tick verlieren alle Habitate etwas Wasser und Futter
                    for (HabitatBox habitat : habitats) {
                        habitat.decreaseResources(speedMultiplier);
                        habitat.checkDeath(speedMultiplier);
                    }

                    updateClock(clockLabel);
                    checkAlarms(habitats);
                })
        );

        // Die Timeline soll dauerhaft laufen
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();

        // Hauptlayout der Anwendung
        VBox root = new VBox(10, title, clockLabel, speedButtons, alarmLabel, map);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color: #A5D66F;");

        Scene scene = new Scene(root, 950, 900);

        stage.setTitle("CritterCare");
        stage.setScene(scene);
        stage.show();
    }

    private void checkAlarms(HabitatBox[] habitats) {
        boolean alarmFound = false;

        for (HabitatBox habitat : habitats) {

            // Tote Habitate müssen nicht mehr geprüft werden
            if (habitat.dead) {
                continue;
            }

            // Kritisch wird es, wenn Wasser oder Futter nur noch 5 % oder weniger hat
            boolean waterCritical = habitat.waterBar.getProgress() <= 0.05;
            boolean foodCritical = habitat.foodBar.getProgress() <= 0.05;

            if (waterCritical || foodCritical) {
                alarmFound = true;

                String message = "ALARM: " + habitat.name + " needs attention! "
                        + "Water: " + formatPercent(habitat.waterBar.getProgress())
                        + " | Food: " + formatPercent(habitat.foodBar.getProgress())
                        + " | Time left: " + (60 - habitat.criticalMinutes) + " min";

                alarmLabel.setText(message);
                setAlarmLabel();

                // Das betroffene Habitat wird rot markiert
                habitat.setAlarmStyle();

                // Systemton als einfacher Alarm
                Toolkit.getDefaultToolkit().beep();

            } else {
                // Wenn alles wieder okay ist, wird der kritische Timer zurückgesetzt
                habitat.criticalMinutes = 0;
                habitat.setNormalStyle();
            }
        }

        // Wenn kein Habitat kritisch ist, bleibt die Anzeige grün
        if (!alarmFound) {
            alarmLabel.setText("No active alarms");
            setNormalAlarmLabel();
        }
    }

    private HabitatBox createHabitat(String name, String animals, String color,
                                     double waterValue, double foodValue) {

        Label title = new Label(name);
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label animalLabel = new Label(animals);
        animalLabel.setStyle("-fx-font-size: 42px;");

        ProgressBar waterBar = new ProgressBar(waterValue);
        waterBar.setPrefWidth(170);

        ProgressBar foodBar = new ProgressBar(foodValue);
        foodBar.setPrefWidth(170);

        Button refillWaterButton = new Button("Refill Water");
        Button refillFoodButton = new Button("Refill Food");

        // Hier wird das Aussehen eines einzelnen Habitats zusammengesetzt
        VBox box = new VBox(
                8,
                title,
                animalLabel,
                new Label("Water"),
                waterBar,
                refillWaterButton,
                new Label("Food"),
                foodBar,
                refillFoodButton
        );

        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(15));
        box.setPrefSize(300, 280);

        HabitatBox habitatBox = new HabitatBox(
                name,
                title,
                animalLabel,
                box,
                waterBar,
                foodBar,
                refillWaterButton,
                refillFoodButton,
                color
        );

        // Wasser wird pro Klick um 10 % aufgefüllt, aber nie über 100 %
        refillWaterButton.setOnAction(event -> {
            if (!habitatBox.dead) {
                waterBar.setProgress(Math.min(waterBar.getProgress() + 0.1, 1.0));
                habitatBox.resetCriticalIfRecovered();
            }
        });

        // Futter wird pro Klick um 10 % aufgefüllt, aber nie über 100 %
        refillFoodButton.setOnAction(event -> {
            if (!habitatBox.dead) {
                foodBar.setProgress(Math.min(foodBar.getProgress() + 0.1, 1.0));
                habitatBox.resetCriticalIfRecovered();
            }
        });

        habitatBox.setNormalStyle();

        return habitatBox;
    }

    private void updateClock(Label clockLabel) {
        int hours = ingameMinutes / 60;
        int minutes = ingameMinutes % 60;

        // Anzeige für Datum, Uhrzeit und aktuelle Geschwindigkeit
        clockLabel.setText(String.format(
                "Date: %02d.%02d.%04d | Time: %02d:%02d | Speed: x%d",
                ingameDate.getDayOfMonth(),
                ingameDate.getMonthValue(),
                ingameDate.getYear(),
                hours,
                minutes,
                speedMultiplier
        ));
    }

    private String formatPercent(double value) {
        return Math.round(value * 100) + "%";
    }

    private void setNormalAlarmLabel() {
        alarmLabel.setStyle(
                "-fx-font-size: 22px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: white;" +
                "-fx-background-color: #2E7D32;" +
                "-fx-padding: 10;"
        );
    }

    private void setAlarmLabel() {
        alarmLabel.setStyle(
                "-fx-font-size: 22px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: white;" +
                "-fx-background-color: #B71C1C;" +
                "-fx-padding: 10;"
        );
    }

    // Diese innere Klasse speichert alles, was zu einem Habitat gehört
    private static class HabitatBox {
        String name;
        Label title;
        Label animalLabel;
        VBox box;
        ProgressBar waterBar;
        ProgressBar foodBar;
        Button refillWaterButton;
        Button refillFoodButton;
        String normalColor;

        boolean dead = false;

        // Zählt, wie lange ein Habitat schon im kritischen Zustand ist
        int criticalMinutes = 0;

        HabitatBox(String name,
                   Label title,
                   Label animalLabel,
                   VBox box,
                   ProgressBar waterBar,
                   ProgressBar foodBar,
                   Button refillWaterButton,
                   Button refillFoodButton,
                   String normalColor) {

            this.name = name;
            this.title = title;
            this.animalLabel = animalLabel;
            this.box = box;
            this.waterBar = waterBar;
            this.foodBar = foodBar;
            this.refillWaterButton = refillWaterButton;
            this.refillFoodButton = refillFoodButton;
            this.normalColor = normalColor;
        }

        void decreaseResources(int minutesPassed) {
            if (dead) {
                return;
            }

            // Wasser sinkt langsamer als Futter
            double waterDecrease = minutesPassed * 0.001;
            double foodDecrease = minutesPassed * 0.0015;

            // Math.max verhindert, dass der Wert unter 0 fällt
            waterBar.setProgress(Math.max(waterBar.getProgress() - waterDecrease, 0));
            foodBar.setProgress(Math.max(foodBar.getProgress() - foodDecrease, 0));
        }

        void checkDeath(int minutesPassed) {
            if (dead) {
                return;
            }

            boolean waterEmpty = waterBar.getProgress() <= 0.05;
            boolean foodEmpty = foodBar.getProgress() <= 0.05;

            if (waterEmpty || foodEmpty) {
                criticalMinutes += minutesPassed;
            } else {
                criticalMinutes = 0;
            }

            // Wenn ein Habitat 60 Ingame-Minuten kritisch bleibt, sterben die Tiere
            if (criticalMinutes >= 60) {
                markDead();
            }
        }

        void resetCriticalIfRecovered() {
            // Nur wenn beide Werte wieder über 5 % sind, gilt das Habitat als gerettet
            if (waterBar.getProgress() > 0.05 && foodBar.getProgress() > 0.05) {
                criticalMinutes = 0;
                setNormalStyle();
            }
        }

        void markDead() {
            dead = true;

            animalLabel.setText("☠ ☠");
            title.setText(name + " - Animals died");

            // Nach dem Tod kann man das Habitat nicht mehr benutzen
            waterBar.setDisable(true);
            foodBar.setDisable(true);
            refillWaterButton.setDisable(true);
            refillFoodButton.setDisable(true);
            box.setDisable(true);

            setDeadStyle();
        }

        void setNormalStyle() {
            if (dead) {
                setDeadStyle();
                return;
            }

            box.setStyle(
                    "-fx-background-color:" + normalColor + ";" +
                    "-fx-border-color:#5D4037;" +
                    "-fx-border-width:5;"
            );
        }

        void setAlarmStyle() {
            if (dead) {
                setDeadStyle();
                return;
            }

            box.setStyle(
                    "-fx-background-color:#FF6B6B;" +
                    "-fx-border-color:#B71C1C;" +
                    "-fx-border-width:8;"
            );
        }

        void setDeadStyle() {
            box.setStyle(
                    "-fx-background-color:#9E9E9E;" +
                    "-fx-border-color:#424242;" +
                    "-fx-border-width:8;" +
                    "-fx-opacity:0.75;"
            );
        }
    }

    public static void main(String[] args) {
        launch();
    }
}