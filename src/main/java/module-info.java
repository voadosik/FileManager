
/**
 * Module declaration for the File Manager application.
 * Defines the module's dependencies, exported packages, and opened packages.
 */
module cz.cuni.mff.java.project.filemanager {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.web;
    requires java.desktop;

    // Open main package to JavaFX graphics
    opens cz.cuni.mff.java.project.filemanager to javafx.graphics;

    // Open controller package to FXML loader
    opens cz.cuni.mff.java.project.filemanager.controller to javafx.fxml;

    // Open model package to JavaFX property bindings
    opens cz.cuni.mff.java.project.filemanager.model to javafx.base;
    exports cz.cuni.mff.java.project.filemanager;
}