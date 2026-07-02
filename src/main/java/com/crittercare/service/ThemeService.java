package com.crittercare.service;

import javafx.scene.Parent;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the active UI theme (light/dark mode + color palette).
 *
 * Applies theme by adding/removing CSS class names on the scene root node.
 * JavaFX re-evaluates all looked-up color variables when style classes change,
 * so every node using -cc-* colors updates instantly without reloading any FXML.
 *
 * Call setSceneRoot() once from MainApp.start() after loadMainView() returns.
 */
public class ThemeService {

    public enum ColorTheme {
        FOREST  ("Forest",   "#1B3A28"),
        OCEAN   ("Ocean",    "#1E3A5F"),
        SLATE   ("Slate",    "#1E293B"),
        ROSE    ("Rose",     "#881337"),
        MIDNIGHT("Midnight", "#2D1B69");

        private final String label;
        private final String swatchColor;

        ColorTheme(String label, String swatchColor) {
            this.label       = label;
            this.swatchColor = swatchColor;
        }

        public String getLabel()       { return label; }
        public String getSwatchColor() { return swatchColor; }

        public String cssClass() {
            return this == FOREST ? null : "theme-" + name().toLowerCase();
        }
    }

    private boolean    darkMode   = false;
    private ColorTheme colorTheme = ColorTheme.FOREST;
    private Parent     sceneRoot;

    public void setSceneRoot(Parent root) { this.sceneRoot = root; }

    public boolean    isDarkMode()    { return darkMode; }
    public ColorTheme getColorTheme() { return colorTheme; }

    public void setDarkMode(boolean dark) {
        darkMode = dark;
        apply();
    }

    public void setColorTheme(ColorTheme theme) {
        colorTheme = theme;
        apply();
    }

    private void apply() {
        if (sceneRoot == null) return;
        List<String> toRemove = new ArrayList<>();
        for (String c : sceneRoot.getStyleClass()) {
            if (c.equals("dark") || c.startsWith("theme-")) toRemove.add(c);
        }
        sceneRoot.getStyleClass().removeAll(toRemove);
        if (darkMode) sceneRoot.getStyleClass().add("dark");
        String cls = colorTheme.cssClass();
        if (cls != null) sceneRoot.getStyleClass().add(cls);
    }
}
