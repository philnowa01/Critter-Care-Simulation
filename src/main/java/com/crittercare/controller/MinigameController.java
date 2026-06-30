package com.crittercare.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.awt.Toolkit;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MinigameController {

    private LocalDate ingameDate = LocalDate.of(2026, 1, 1);
    private int ingameMinutes = 8 * 60;

    private int speedMultiplier = 1;
    private int score = 0;
    private int remainingSeconds = 60;
    private int elapsedSeconds = 0;

    private boolean gameOver = false;

    private Timeline clock;

    @FXML
    private VBox minigameRoot;

    private TextField usernameField;

    private Label clockLabel;
    private Label alarmLabel;
    private Label scoreLabel;
    private Label timerLabel;
    private Label highscoreLabel;
    private Label playerLabel;
    private Label speedLabel;
    private Label statusLabel;

    private VBox sidePanel;
    private GridPane map;

    private String currentUsername = "Player";

    private final List<HighscoreEntry> highscores = new ArrayList<>();

    @FXML
    public void initialize() {
        showStartScreen();
    }

    private void showStartScreen() {
        if (clock != null) {
            clock.stop();
        }

        Label title = new Label("Habitat Rescue");
        title.setStyle("-fx-font-size: 36px; -fx-font-weight: bold;");

        Label description = new Label("Enter your username and keep all habitats alive for 60 seconds.");
        description.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.setMaxWidth(260);
        usernameField.setStyle("-fx-font-size: 16px; -fx-padding: 8;");

        Button startButton = new Button("Start Game");
        startButton.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        startButton.setOnAction(event -> startGame());

        highscoreLabel = new Label(buildHighscoreText());
        highscoreLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        VBox startBox = new VBox(16, title, description, usernameField, startButton, highscoreLabel);
        startBox.setAlignment(Pos.CENTER);
        startBox.setPadding(new Insets(40));
        startBox.setStyle(
                "-fx-background-color: #A5D66F;" +
                "-fx-background-radius: 16;"
        );

        minigameRoot.getChildren().setAll(startBox);
    }

    private void startGame() {
        String enteredName = usernameField.getText();

        if (enteredName != null && !enteredName.trim().isEmpty()) {
            currentUsername = enteredName.trim();
        } else {
            currentUsername = "Player";
        }

        buildMinigame();
    }

    private void buildMinigame() {
        gameOver = false;

        score = 0;
        remainingSeconds = 60;
        elapsedSeconds = 0;
        speedMultiplier = 1;

        ingameDate = LocalDate.of(2026, 1, 1);
        ingameMinutes = 8 * 60;

        Label title = new Label("Habitat Rescue");
        title.setStyle("-fx-font-size: 32px; -fx-font-weight: bold;");

        alarmLabel = new Label("No active alarms");
        setNormalAlarmLabel();

        map = new GridPane();
        map.setHgap(18);
        map.setVgap(18);
        map.setAlignment(Pos.CENTER);
        map.setPadding(new Insets(10));

        HabitatBox jungle = createHabitat("Jungle", "🐒 🦜", "#4CAF50", 0.80, 0.80);
        HabitatBox mountains = createHabitat("Mountains", "🐐 🦅", "#B0BEC5", 0.80, 0.80);
        HabitatBox savannah = createHabitat("Savannah", "🦁 🦒", "#DDBB77", 0.80, 0.80);
        HabitatBox forest = createHabitat("Forest", "🦌 🐻", "#81C784", 0.80, 0.80);

        map.add(jungle.box, 0, 0);
        map.add(mountains.box, 1, 0);
        map.add(savannah.box, 0, 1);
        map.add(forest.box, 1, 1);

        HabitatBox[] habitats = {jungle, mountains, savannah, forest};

        VBox gameArea = new VBox(14, title, alarmLabel, map);
        gameArea.setAlignment(Pos.TOP_CENTER);
        gameArea.setPadding(new Insets(10));
        gameArea.setPrefWidth(700);

        buildSidePanel();

        HBox mainLayout = new HBox(20, gameArea, sidePanel);
        mainLayout.setAlignment(Pos.TOP_CENTER);
        mainLayout.setPadding(new Insets(10));

        minigameRoot.getChildren().setAll(mainLayout);

        startClock(habitats);
    }

    private void buildSidePanel() {
        playerLabel = new Label("Player: " + currentUsername);
        clockLabel = new Label();
        timerLabel = new Label("Time Left: 60s");
        speedLabel = new Label("Speed: x1");
        scoreLabel = new Label("Score: 0");
        statusLabel = new Label("Status: Running");
        highscoreLabel = new Label(buildHighscoreText());

        playerLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        clockLabel.setStyle("-fx-font-size: 14px;");
        timerLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        speedLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        scoreLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        statusLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        highscoreLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        Button resetButton = new Button("Reset");
        resetButton.setMaxWidth(Double.MAX_VALUE);
        resetButton.setOnAction(event -> showStartScreen());

        sidePanel = new VBox(
                14,
                playerLabel,
                timerLabel,
                scoreLabel,
                speedLabel,
                clockLabel,
                statusLabel,
                highscoreLabel,
                resetButton
        );

        sidePanel.setAlignment(Pos.TOP_LEFT);
        sidePanel.setPadding(new Insets(18));
        sidePanel.setPrefWidth(260);
        sidePanel.setStyle(
                "-fx-background-color: rgba(255,255,255,0.65);" +
                "-fx-background-radius: 16;" +
                "-fx-border-color: #5D4037;" +
                "-fx-border-width: 3;" +
                "-fx-border-radius: 16;"
        );

        updateClock();
    }

    private void startClock(HabitatBox[] habitats) {
        if (clock != null) {
            clock.stop();
        }

        clock = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> {
                    if (gameOver) {
                        return;
                    }

                    elapsedSeconds++;
                    remainingSeconds--;

                    if (elapsedSeconds % 10 == 0) {
                        speedMultiplier = (elapsedSeconds / 10) * 10;
                    }

                    score += speedMultiplier;
                    ingameMinutes += speedMultiplier;

                    while (ingameMinutes >= 24 * 60) {
                        ingameMinutes -= 24 * 60;
                        ingameDate = ingameDate.plusDays(1);
                    }

                    for (HabitatBox habitat : habitats) {
                        habitat.decreaseResources(speedMultiplier);
                        habitat.checkDeath(speedMultiplier);
                    }

                    updateClock();
                    updateTimer();
                    updateScore();
                    updateSpeed();
                    checkAlarms(habitats);
                    checkGameOver(habitats);

                    if (remainingSeconds <= 0 && !gameOver) {
                        finishGame("Time over. Final Score: " + score);
                    }
                })
        );

        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
    }

    private HabitatBox createHabitat(String name, String animals, String color,
                                     double waterValue, double foodValue) {

        Label title = new Label(name);
        title.setStyle("-fx-font-size: 17px; -fx-font-weight: bold;");

        Label animalLabel = new Label(animals);
        animalLabel.setStyle("-fx-font-size: 34px;");

        ProgressBar waterBar = new ProgressBar(waterValue);
        waterBar.setPrefWidth(145);

        ProgressBar foodBar = new ProgressBar(foodValue);
        foodBar.setPrefWidth(145);

        Button refillWaterButton = new Button("Water");
        Button refillFoodButton = new Button("Food");

        VBox box = new VBox(
                7,
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
        box.setPadding(new Insets(12));
        box.setPrefSize(230, 245);

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

        refillWaterButton.setOnAction(event -> {
            if (!habitatBox.dead && !gameOver) {
                double oldValue = waterBar.getProgress();
                double newValue = Math.min(oldValue + 0.15, 1.0);

                waterBar.setProgress(newValue);

                if (newValue > oldValue) {
                    score += 10;
                    updateScore();
            }

            habitatBox.resetCriticalIfRecovered();
        }
});

        refillFoodButton.setOnAction(event -> {
            if (!habitatBox.dead && !gameOver) {
                double oldValue = foodBar.getProgress();
                double newValue = Math.min(oldValue + 0.15, 1.0);

                foodBar.setProgress(newValue);

                if (newValue > oldValue) {
                    score += 10;
                    updateScore();
                }

                habitatBox.resetCriticalIfRecovered();
            }
});

        habitatBox.setNormalStyle();
        return habitatBox;
    }

    private void checkAlarms(HabitatBox[] habitats) {
        boolean alarmFound = false;

        for (HabitatBox habitat : habitats) {
            if (habitat.dead) {
                continue;
            }

            boolean waterCritical = habitat.waterBar.getProgress() <= 0.15;
            boolean foodCritical = habitat.foodBar.getProgress() <= 0.15;

            if (waterCritical || foodCritical) {
                alarmFound = true;

                alarmLabel.setText(
                        "ALARM: " + habitat.name + " needs help! "
                                + "Water: " + formatPercent(habitat.waterBar.getProgress())
                                + " | Food: " + formatPercent(habitat.foodBar.getProgress())
                );

                setAlarmLabel();
                habitat.setAlarmStyle();
                Toolkit.getDefaultToolkit().beep();
            } else {
                habitat.setNormalStyle();
            }
        }

        if (!alarmFound && !gameOver) {
            alarmLabel.setText("No active alarms");
            setNormalAlarmLabel();
        }
    }

    private void checkGameOver(HabitatBox[] habitats) {
        int deadCount = 0;

        for (HabitatBox habitat : habitats) {
            if (habitat.dead) {
                deadCount++;
            }
        }

        if (deadCount == habitats.length) {
            finishGame("Game over. All habitats failed. Final Score: " + score);
        }
    }

    private void finishGame(String message) {
        gameOver = true;

        if (clock != null) {
            clock.stop();
        }

        highscores.add(new HighscoreEntry(currentUsername, score));
        highscores.sort(Comparator.comparingInt(HighscoreEntry::score).reversed());

        if (highscores.size() > 5) {
            highscores.remove(highscores.size() - 1);
        }

        alarmLabel.setText(message);
        setAlarmLabel();

        statusLabel.setText("Status: Finished");
        highscoreLabel.setText(buildHighscoreText());
    }

    private String buildHighscoreText() {
        if (highscores.isEmpty()) {
            return "Highscores:\nNo scores yet";
        }

        StringBuilder sb = new StringBuilder("Highscores:\n");

        for (int i = 0; i < highscores.size(); i++) {
            HighscoreEntry entry = highscores.get(i);
            sb.append(i + 1)
                    .append(". ")
                    .append(entry.username())
                    .append(" - ")
                    .append(entry.score())
                    .append("\n");
        }

        return sb.toString();
    }

    private void updateClock() {
        int hours = ingameMinutes / 60;
        int minutes = ingameMinutes % 60;

        clockLabel.setText(String.format(
                "%02d.%02d.%04d | %02d:%02d",
                ingameDate.getDayOfMonth(),
                ingameDate.getMonthValue(),
                ingameDate.getYear(),
                hours,
                minutes
        ));
    }

    private void updateTimer() {
        timerLabel.setText("Time Left: " + remainingSeconds + "s");
    }

    private void updateScore() {
        scoreLabel.setText("Score: " + score);
    }

    private void updateSpeed() {
        speedLabel.setText("Speed: x" + speedMultiplier);
    }

    private String formatPercent(double value) {
        return Math.round(value * 100) + "%";
    }

    private void setNormalAlarmLabel() {
        alarmLabel.setStyle(
                "-fx-font-size: 18px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: white;" +
                "-fx-background-color: #2E7D32;" +
                "-fx-padding: 10;" +
                "-fx-background-radius: 10;"
        );
    }

    private void setAlarmLabel() {
        alarmLabel.setStyle(
                "-fx-font-size: 18px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: white;" +
                "-fx-background-color: #B71C1C;" +
                "-fx-padding: 10;" +
                "-fx-background-radius: 10;"
        );
    }

    private record HighscoreEntry(String username, int score) {}

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

            double waterDecrease = minutesPassed * 0.002;
            double foodDecrease = minutesPassed * 0.003;

            waterBar.setProgress(Math.max(waterBar.getProgress() - waterDecrease, 0));
            foodBar.setProgress(Math.max(foodBar.getProgress() - foodDecrease, 0));
        }

        void checkDeath(int minutesPassed) {
            if (dead) {
                return;
            }

            boolean waterEmpty = waterBar.getProgress() <= 0.0;
            boolean foodEmpty = foodBar.getProgress() <= 0.0;

            if (waterEmpty || foodEmpty) {
                criticalMinutes += minutesPassed;
            } else {
                criticalMinutes = 0;
            }

            if (criticalMinutes >= 20) {
                markDead();
            }
        }

        void resetCriticalIfRecovered() {
            if (waterBar.getProgress() > 0.0 && foodBar.getProgress() > 0.0) {
                criticalMinutes = 0;
                setNormalStyle();
            }
        }

        void markDead() {
            dead = true;

            animalLabel.setText("☠ ☠");
            title.setText(name + " - Lost");

            waterBar.setDisable(true);
            foodBar.setDisable(true);
            refillWaterButton.setDisable(true);
            refillFoodButton.setDisable(true);

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
                    "-fx-border-width:4;" +
                    "-fx-background-radius:12;" +
                    "-fx-border-radius:12;"
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
                    "-fx-border-width:6;" +
                    "-fx-background-radius:12;" +
                    "-fx-border-radius:12;"
            );
        }

        void setDeadStyle() {
            box.setStyle(
                    "-fx-background-color:#9E9E9E;" +
                    "-fx-border-color:#424242;" +
                    "-fx-border-width:6;" +
                    "-fx-opacity:0.75;" +
                    "-fx-background-radius:12;" +
                    "-fx-border-radius:12;"
            );
        }
    }
}