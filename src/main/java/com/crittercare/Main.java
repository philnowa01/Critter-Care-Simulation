package com.crittercare;

/**
 * True application entry-point.
 *
 * This class intentionally does NOT extend javafx.application.Application.
 * Some fat-jar launchers and module-system bootstrappers initialise the JavaFX
 * toolkit before main() runs when the main class is an Application subclass,
 * which can cause startup failures.  Delegating to Application.launch() from
 * a plain class sidesteps that issue entirely.
 */
public class Main {
    public static void main(String[] args) {
        MainApp.launch(MainApp.class, args);
    }
}

// Run java tt