package com.crittercare.minigame;

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

public class HabitatGame extends Application {

    private LocalDate ingameDate = LocalDate.of(2026, 1, 1);
    private int ingameMinutes = 8 * 60;
    private int speedMultiplier = 1;
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

        Button speed1Button = new Button("Speed x1");
        Button speed5Button = new Button("Speed x5");
        Button speed50Button = new Button("Speed x50");

        speed1Button.setOnAction(event -> speedMultiplier = 1);
        speed5Button.setOnAction(event -> speedMultiplier = 5);
        speed50Button.setOnAction(event -> speedMultiplier = 50);

        HBox speedButtons = new HBox(10, speed1Button, speed5Button, speed50Button);
        speedButtons.setAlignment(Pos.CENTER);

        GridPane map = new GridPane();
        map.setHgap(25);
        map.setVgap(25);
        map.setAlignment(Pos.CENTER);
        map.setPadding(new Insets(20));

        HabitatBox jungle    = createHabitat("Jungle",    "🐒 🦜", "#4CAF50", 0.45, 0.60);
        HabitatBox mountains = createHabitat("Mountains", "🐐 🦅", "#B0BEC5", 0.30, 0.50);
        HabitatBox savannah  = createHabitat("Savannah",  "🦁 🦒", "#DDBB77", 0.55, 0.35);
        HabitatBox forest    = createHabitat("Forest",    "🦌 🐻", "#81C784", 0.70, 0.40);

        map.add(jungle.box,    0, 0);
        map.add(mountains.box, 1, 0);
        map.add(savannah.box,  0, 1);
        map.add(forest.box,    1, 1);

        HabitatBox[] habitats = {jungle, mountains, savannah, forest};

        Timeline clock = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> {
                    ingameMinutes += speedMultiplier;
                    while (ingameMinutes >= 24 * 60) {
                        ingameMinutes -= 24 * 60;
                        ingameDate = ingameDate.plusDays(1);
                    }
                    for (HabitatBox habitat : habitats) {
                        habitat.decreaseResources(speedMultiplier);
                        habitat.checkDeath(speedMultiplier);
                    }
                    updateClock(clockLabel);
                    checkAlarms(habitats);
                })
        );
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();

        // Stop the timeline when the window is closed
        stage.setOnCloseRequest(e -> clock.stop());

        VBox root = new VBox(10, title, clockLabel, speedButtons, alarmLabel, map);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color: #A5D66F;");

        Scene scene = new Scene(root, 950, 900);

        stage.setTitle("CritterCare – Mini Game");
        stage.setScene(scene);
        stage.show();
    }

    private void checkAlarms(HabitatBox[] habitats) {
        boolean alarmFound = false;

        for (HabitatBox habitat : habitats) {
            if (habitat.dead) continue;

            boolean waterCritical = habitat.waterBar.getProgress() <= 0.05;
            boolean foodCritical  = habitat.foodBar.getProgress()  <= 0.05;

            if (waterCritical || foodCritical) {
                alarmFound = true;
                String message = "ALARM: " + habitat.name + " needs attention! "
                        + "Water: " + formatPercent(habitat.waterBar.getProgress())
                        + " | Food: " + formatPercent(habitat.foodBar.getProgress())
                        + " | Time left: " + (60 - habitat.criticalMinutes) + " min";
                alarmLabel.setText(message);
                setAlarmLabel();
                habitat.setAlarmStyle();
                Toolkit.getDefaultToolkit().beep();
            } else {
                habitat.criticalMinutes = 0;
                habitat.setNormalStyle();
            }
        }

        if (!alarmFound) {
            alarmLabel.setText("No active alarms");
            setNormalAlarmLabel();
        }
    }

    private HabitatBox createHabitat(String name, String animals, String color,
                                     double waterValue, double foodValue) {
        Label titleLabel = new Label(name);
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label animalLabel = new Label(animals);
        animalLabel.setStyle("-fx-font-size: 42px;");

        ProgressBar waterBar = new ProgressBar(waterValue);
        waterBar.setPrefWidth(170);

        ProgressBar foodBar = new ProgressBar(foodValue);
        foodBar.setPrefWidth(170);

        Button refillWaterButton = new Button("Refill Water");
        Button refillFoodButton  = new Button("Refill Food");

        VBox box = new VBox(
                8,
                titleLabel,
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
                name, titleLabel, animalLabel, box,
                waterBar, foodBar,
                refillWaterButton, refillFoodButton,
                color
        );

        refillWaterButton.setOnAction(event -> {
            if (!habitatBox.dead) {
                waterBar.setProgress(Math.min(waterBar.getProgress() + 0.1, 1.0));
                habitatBox.resetCriticalIfRecovered();
            }
        });

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
        int hours   = ingameMinutes / 60;
        int minutes = ingameMinutes % 60;
        clockLabel.setText(String.format(
                "Date: %02d.%02d.%04d | Time: %02d:%02d | Speed: x%d",
                ingameDate.getDayOfMonth(),
                ingameDate.getMonthValue(),
                ingameDate.getYear(),
                hours, minutes, speedMultiplier
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
        int criticalMinutes = 0;

        HabitatBox(String name, Label title, Label animalLabel, VBox box,
                   ProgressBar waterBar, ProgressBar foodBar,
                   Button refillWaterButton, Button refillFoodButton,
                   String normalColor) {
            this.name = name;
            this.title = title;
            this.animalLabel = animalLabel;
            this.box = box;
            this.waterBar = waterBar;
            this.foodBar = foodBar;
            this.refillWaterButton = refillWaterButton;
            this.refillFoodButton  = refillFoodButton;
            this.normalColor = normalColor;
        }

        void decreaseResources(int minutesPassed) {
            if (dead) return;
            waterBar.setProgress(Math.max(waterBar.getProgress() - minutesPassed * 0.001,  0));
            foodBar.setProgress( Math.max(foodBar.getProgress()  - minutesPassed * 0.0015, 0));
        }

        void checkDeath(int minutesPassed) {
            if (dead) return;
            boolean waterEmpty = waterBar.getProgress() <= 0.05;
            boolean foodEmpty  = foodBar.getProgress()  <= 0.05;
            if (waterEmpty || foodEmpty) {
                criticalMinutes += minutesPassed;
            } else {
                criticalMinutes = 0;
            }
            if (criticalMinutes >= 60) markDead();
        }

        void resetCriticalIfRecovered() {
            if (waterBar.getProgress() > 0.05 && foodBar.getProgress() > 0.05) {
                criticalMinutes = 0;
                setNormalStyle();
            }
        }

        void markDead() {
            dead = true;
            animalLabel.setText("☠ ☠");
            title.setText(name + " - Animals died");
            waterBar.setDisable(true);
            foodBar.setDisable(true);
            refillWaterButton.setDisable(true);
            refillFoodButton.setDisable(true);
            box.setDisable(true);
            setDeadStyle();
        }

        void setNormalStyle() {
            if (dead) { setDeadStyle(); return; }
            box.setStyle(
                    "-fx-background-color:" + normalColor + ";" +
                    "-fx-border-color:#5D4037;" +
                    "-fx-border-width:5;"
            );
        }

        void setAlarmStyle() {
            if (dead) { setDeadStyle(); return; }
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
}
