package cz.cuni.mff.java.project.filemanager.controller;

import cz.cuni.mff.java.project.filemanager.model.FileItem;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.File;

public class MainController {
    @FXML private TreeView<File> directoryTree;
    @FXML private TableView<FileItem> fileTable;
    @FXML private TableColumn<FileItem, String> fileName;
    @FXML private TableColumn<FileItem, String> fileType;
    @FXML private TableColumn<FileItem, String> fileDate;
    @FXML private TableColumn<FileItem, String> fileSize;

    @FXML
    public void initialize() {
        fileName.setCellValueFactory(new PropertyValueFactory<>("name"));
        fileType.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getFile().isDirectory() ? "Directory" : "File"));
        fileDate.setCellValueFactory(new PropertyValueFactory<>("modified"));
        fileSize.setCellValueFactory(new PropertyValueFactory<>("size"));


        TreeItem<File> rootItem = new TreeItem<>(new File("My Computer")); //Dummy
        rootItem.setExpanded(true);
        for(File root : File.listRoots()) {
            rootItem.getChildren().add(createNode(root));
        }
        directoryTree.setRoot(rootItem);
        directoryTree.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue != null) {
                showFilesInDirectory(newValue.getValue());
            }
        });
    }

    private void showFilesInDirectory(File directory) {
        ObservableList<FileItem> items = FXCollections.observableArrayList();
        File[] files = directory.listFiles();
        if(files != null) {
            for(File file : files) {
                items.add(new FileItem(file));
            }
        }
        fileTable.setItems(items);
    }

    private TreeItem<File> createNode(File file) {
        TreeItem<File> item = new TreeItem<>(file);
        item.setExpanded(false);

        item.expandedProperty().addListener((observable, wasExpanded, isExpanded) -> {
           if(isExpanded && item.getChildren().isEmpty()) {
               File[] directories = file.listFiles(File::isDirectory);
               if(directories != null) {
                   for(File directory : directories) {
                       item.getChildren().add(createNode(directory));
                   }
               }
           }
        });

        return item;
    }



    @FXML
    private void handleRename() {}

    @FXML
    private void handleDelete() {}
}
