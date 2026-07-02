package com.crittercare.controller;

import com.crittercare.service.ThemeService;
import com.crittercare.service.ThemeService.ColorTheme;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Controller for the Settings view.
 *
 * Builds the theme swatches programmatically so the active indicator (green
 * ring + checkmark) always reflects the current ThemeService state.
 * MainController calls refresh() each time the user navigates to Settings so
 * the toggle and swatches stay in sync regardless of when they last changed.
 */
public class SettingsController {

    private final ThemeService themeService;

    @FXML private ToggleButton darkModeToggle;
    @FXML private HBox         themeSwatchRow;

    public SettingsController(ThemeService themeService) {
        this.themeService = themeService;
    }

    @FXML
    public void initialize() {
        refresh();
    }

    @FXML
    public void toggleDarkMode() {
        themeService.setDarkMode(darkModeToggle.isSelected());
        syncToggle();
    }

    /** Syncs toggle label and rebuilds swatches to reflect current theme state. */
    public void refresh() {
        syncToggle();
        buildThemeSwatches();
    }

    private void syncToggle() {
        boolean dark = themeService.isDarkMode();
        darkModeToggle.setSelected(dark);
        darkModeToggle.setText(dark ? "🌙  Dark Mode" : "☀  Light Mode");
    }

    private void buildThemeSwatches() {
        themeSwatchRow.getChildren().clear();
        for (ColorTheme theme : ColorTheme.values()) {
            themeSwatchRow.getChildren().add(buildSwatch(theme));
        }
    }

    private VBox buildSwatch(ColorTheme theme) {
        boolean active = themeService.getColorTheme() == theme;

        StackPane circle = new StackPane();
        circle.setMinSize(48, 48);
        circle.setMaxSize(48, 48);
        circle.setStyle(
            "-fx-background-color: " + theme.getSwatchColor() + ";" +
            "-fx-background-radius: 24;" +
            "-fx-cursor: hand;" +
            (active
                ? "-fx-border-color: #34D399; -fx-border-radius: 24; -fx-border-width: 3;"
                : "-fx-border-color: rgba(0,0,0,0.12); -fx-border-radius: 24; -fx-border-width: 1.5;")
        );

        if (active) {
            Label check = new Label("✓");
            check.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px;");
            circle.getChildren().add(check);
        }

        Label nameLbl = new Label(theme.getLabel());
        nameLbl.setMaxWidth(Double.MAX_VALUE);
        nameLbl.setAlignment(Pos.CENTER);
        nameLbl.setStyle("-fx-text-fill: -cc-text-dim; -fx-font-size: 11px;");

        VBox box = new VBox(8, circle, nameLbl);
        box.setAlignment(Pos.TOP_CENTER);
        box.setPadding(new Insets(2, 6, 2, 6));
        box.setOnMouseClicked(e -> {
            themeService.setColorTheme(theme);
            buildThemeSwatches();
        });

        return box;
    }
}
