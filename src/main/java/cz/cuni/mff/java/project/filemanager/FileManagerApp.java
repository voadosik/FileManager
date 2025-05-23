package cz.cuni.mff.java.project.filemanager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

/**
 * Main application class for the File Manager
 * Initializes and configures the JavaFX application window.
 */
public class FileManagerApp extends Application {
    /**
     * Entry point of the application
     * @param primaryStage The main window container
     * @throws Exception If FXML loading or resource access fails
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass()
                .getResource("/cz/cuni/mff/java/project/filemanager/layout/layout/main.fxml"));

        Scene scene = new Scene(loader.load());

        // Add CSS stylesheet to the scene for visual styling
        scene.getStylesheets()
                .add(Objects
                .requireNonNull(getClass().getResource("/cz/cuni/mff/java/project/filemanager/style/style.css"))
                .toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.setTitle("File Manager");
        primaryStage.show();
    }

    /**
     * Application launch
     * @param args Program arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}
