package cz.cuni.mff.java.project.filemanager.controller;

import cz.cuni.mff.java.project.filemanager.model.FileItem;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainController {
    @FXML private TreeView<File> directoryTree;
    @FXML private TableView<FileItem> fileTable;
    @FXML private TableColumn<FileItem, String> fileName;
    @FXML private TableColumn<FileItem, String> fileType;
    @FXML private TableColumn<FileItem, String> fileDate;
    @FXML private TableColumn<FileItem, String> fileSize;

    @FXML private TextField pathTextField;
    private File currentDirectory;


    private final Stack<File> backHistory = new Stack<>();
    private final Stack<File> forwardHistory = new Stack<>();
    private boolean isNavigatingBack = false;
    private boolean isNavigatingForward = false;

    @FXML private Button btnBack;
    @FXML private Button btnForward;

    @FXML
    public void initialize() {
        fileName.setCellValueFactory(new PropertyValueFactory<>("name"));
        fileName.setOnEditCommit(event -> {
            FileItem item = event.getRowValue();
            String newName = event.getNewValue();
            File newFile = new File(item.getFile().getParentFile(), newName);

            try{
                if(item.getFile().renameTo(newFile)){
                    item.getFile().renameTo(newFile);
                    updateDirectoryView();
                }else{
                    showError("Rename failed", "Could not rename file");
                    fileTable.refresh();
                }

            }catch (SecurityException se){
                showError("Permission denied", "No permission to rename file");
                fileTable.refresh();
            }
        });
        fileType.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getFile().isDirectory() ? "Directory" : "File"));
        fileDate.setCellValueFactory(new PropertyValueFactory<>("modified"));
        fileSize.setCellValueFactory(new PropertyValueFactory<>("size"));

        btnBack.setDisable(true);
        btnForward.setDisable(true);

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

        currentDirectory = new File("user.home");
        showFilesInDirectory(currentDirectory);
        fileTable.setRowFactory(tv ->{
            TableRow<FileItem> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if(event.getClickCount() == 2 && !row.isEmpty()) {
                    FileItem item = row.getItem();
                    File file = item.getFile();
                    if(file.isDirectory()) {
                        showFilesInDirectory(file);
                    }
                }
            });
            return row;
        });
        fileTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    private void showFilesInDirectory(File directory) {
        if(!directory.isDirectory()) return;
        if(!isNavigatingBack && !isNavigatingForward && currentDirectory != null)
        {
            backHistory.push(currentDirectory);
            forwardHistory.clear();
        }

        pathTextField.setText(directory.getAbsolutePath());
        currentDirectory = directory;

        updateFileTable(directory);
        updateNavigationButtons();
        selectInTreeView(directory);
    }

    private void updateFileTable(File directory) {
        ObservableList<FileItem> items = FXCollections.observableArrayList();
        File[] files = directory.listFiles();
        if(files != null) {
            for(File file : files) {
                items.add(new FileItem(file));
            }
        }
        fileTable.setItems(items);
    }

    private void updateNavigationButtons() {
        btnBack.setDisable(backHistory.isEmpty());
        btnForward.setDisable(forwardHistory.isEmpty());
    }

    private void selectInTreeView(File directory) {
        TreeItem<File> rootItem = directoryTree.getRoot();
        if(rootItem == null) return;

        for(TreeItem<File> child : rootItem.getChildren()) {
            if(child.getValue().equals(directory)) {
                directoryTree.getSelectionModel().select(child);
                directoryTree.scrollTo(directoryTree.getRow(child));
                return;
            }
        }
        pathTextField.setText(directory.getAbsolutePath());
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

    private void createNewItem(boolean isDirectory) {
        TextInputDialog dialog = new TextInputDialog();
        String fileType = isDirectory ? "Directory" : "File";
        dialog.setTitle("New " + fileType);
        dialog.setHeaderText("Enter Name for new " + fileType.toLowerCase());
        dialog.setContentText("Name: ");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if(name.isEmpty() || name.contains("\\") || name.contains("/")) {
                showError("Invalid name", "Name cannot be empty or contain slashes");
                return;
            }
            File newItem = new File(currentDirectory, name);
            try{
                if(isDirectory) {
                    if(!newItem.mkdir()) {
                        showError("Fail", "Unable to create directory");
                    }
                } else{
                    if(!newItem.createNewFile()){
                        showError("Fail", "Unable to create new file");
                    }
                }
                updateDirectoryView();
            } catch (IOException e) {
                showError("Error occured", e.getMessage());
            }
        });

    }

    private void updateDirectoryView() {
        showFilesInDirectory(currentDirectory);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void deleteRecursively(File file) throws IOException {
        Path path = file.toPath();
        if(Files.exists(path)){
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException{
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }

    }

    // Handlers

    @FXML
    private void handleNewFile() {
        createNewItem(false);
    }

    @FXML
    private void handleNewFolder() {
        createNewItem(true);
    }

    @FXML
    private void handleRename() {
        FileItem selectedItem = fileTable.getSelectionModel().getSelectedItem();
        if(selectedItem == null){
            showError("No item is selected", "Please select an item to rename first");
            return;
        }
        File fileToRename = selectedItem.getFile();
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Rename " + fileToRename.getName());
        dialog.setHeaderText("Enter new name for this item");
        dialog.setContentText("New name: ");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if(name.isEmpty() || name.contains("\\") || name.contains("/")) {
                showError("Invalid name", "Name cannot be empty or contain slashes");
                return;
            }

            File f = new File(fileToRename.getParentFile(), name);
            if(f.exists()){
                showError("Name exists", "A file with this name already exists in the current directory");
                return;
            }
            try{
                boolean renamed = fileToRename.renameTo(f);
                if(!renamed){
                    throw new IOException("Failed to rename file");
                }
                updateDirectoryView();
            }
            catch(SecurityException | IOException e){
                showError("Rename failed", "Error renaming file "+ e.getMessage());
            }
        });
    }

    @FXML
    private void handleDelete() {
        ObservableList<FileItem> selectedItems = fileTable.getSelectionModel().getSelectedItems();
        if(selectedItems == null || selectedItems.isEmpty()) {
            showError("No items selected", "Please select item to delete");
            return;
        }

        StringBuilder builder = new StringBuilder("Are you sure you want to delete: \n");
        for(FileItem item : selectedItems) {
            builder.append("- ").append(item.getName()).append("\n");
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm deletion");
        confirmation.setHeaderText(null);
        confirmation.setContentText(builder.toString());

        Optional<ButtonType> result = confirmation.showAndWait();
        if(result.isPresent() && result.get() == ButtonType.OK) {
            boolean allDeleted = true;
            StringBuilder errors = new StringBuilder();

            for(FileItem item : selectedItems) {
                File f = item.getFile();
                try{
                    deleteRecursively(f);
                }catch (IOException e){
                    allDeleted = false;
                    errors.append("Failed to delete ")
                            .append(f.getName())
                            .append(": ")
                            .append(e.getMessage())
                            .append("\n");
                }

            }
            if(!allDeleted) {
                showError("Delete failed", errors.toString());
            }
        }

        updateDirectoryView();

    }

    @FXML
    private void handleBack(){
        if(!backHistory.isEmpty()) {
            isNavigatingBack = true;
            forwardHistory.push(currentDirectory);
            File previousDirectory = backHistory.pop();
            showFilesInDirectory(previousDirectory);
            isNavigatingBack = false;
        }
    }

    @FXML
    private void handleForward(){
        if(!forwardHistory.isEmpty()) {
            isNavigatingForward = true;
            backHistory.push(currentDirectory);
            File nextDirectory = forwardHistory.pop();
            showFilesInDirectory(nextDirectory);
            isNavigatingForward = false;
        }
    }
}
