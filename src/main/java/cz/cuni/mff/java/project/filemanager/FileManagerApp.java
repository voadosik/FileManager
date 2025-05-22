package cz.cuni.mff.java.project.filemanager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class FileManagerApp extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass()
                .getResource("/layout/main.fxml"));

        Scene scene = new Scene(loader.load());

        scene.getStylesheets()
                .add(Objects
                .requireNonNull(getClass().getResource("/style/style.css"))
                .toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.setTitle("File Manager");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

}
