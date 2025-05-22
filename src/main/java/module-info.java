module cz.cuni.mff.java.project.filemanager {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.web;
    requires java.desktop;

    opens cz.cuni.mff.java.project.filemanager to javafx.graphics;
    opens cz.cuni.mff.java.project.filemanager.controller to javafx.fxml;
    opens cz.cuni.mff.java.project.filemanager.model to javafx.base;

    exports cz.cuni.mff.java.project.filemanager;
}