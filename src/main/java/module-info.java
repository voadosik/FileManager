module cz.cuni.mff.java.project.filemanager {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.graphics;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires java.desktop;

    // Model package access
    opens cz.cuni.mff.java.project.filemanager.model to javafx.base;
    exports cz.cuni.mff.java.project.filemanager.model;

    // Controller package access
    opens cz.cuni.mff.java.project.filemanager.controller to javafx.fxml;
    exports cz.cuni.mff.java.project.filemanager.controller;

    // Main package access
    opens cz.cuni.mff.java.project.filemanager to javafx.fxml;
    exports cz.cuni.mff.java.project.filemanager;
}