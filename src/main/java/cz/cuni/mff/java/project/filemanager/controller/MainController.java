package cz.cuni.mff.java.project.filemanager.controller;

import cz.cuni.mff.java.project.filemanager.model.FileItem;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.Optional;
import java.util.Stack;



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

        TreeItem<File> rootItem = new TreeItem<>(new File("This PC")){
            @Override
            public String toString() {
                return "This PC";
            }
        };
        rootItem.setExpanded(true);

        Image driveIcon = new Image(getClass().getResourceAsStream("/images/drive-icon.png"));

        for(File root : File.listRoots()) {
            TreeItem<File> driveItem = createNode(root);
            ImageView icon = new ImageView(driveIcon);
            icon.setFitHeight(16);
            icon.setFitWidth(16);
            driveItem.setGraphic(icon);
            rootItem.getChildren().add(driveItem);
        }
        directoryTree.setRoot(rootItem);
        directoryTree.setShowRoot(true);
        directoryTree.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue != null) {
                showFilesInDirectory(newValue.getValue(), true);
            }
        });

        directoryTree.setCellFactory(tv -> new TreeCell<File>() {
            private final Image driveImage;
            private final Image folderImage;

            {
                try {
                    driveImage = new Image(Objects
                            .requireNonNull(getClass()
                                    .getResourceAsStream("/images/drive-icon.png")));

                    folderImage = new Image(Objects
                            .requireNonNull(getClass()
                                    .getResourceAsStream("/images/folder-icon.png")));

                } catch (Exception e) {
                    throw new RuntimeException("Failed to load icons", e);
                }
            }

            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(getTreeItem().toString());

                    ImageView iconView = new ImageView();
                    iconView.setFitWidth(16);
                    iconView.setFitHeight(16);

                    if (item.getPath().endsWith(File.separator)) {
                        iconView.setImage(driveImage);
                    } else {
                        iconView.setImage(folderImage);
                    }

                    setGraphic(iconView);
                }
            }
        });

        currentDirectory = new File("user.home");
        fileTable.setRowFactory(tv ->{
            TableRow<FileItem> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if(event.getClickCount() == 2 && !row.isEmpty()) {
                    FileItem item = row.getItem();
                    File file = item.getFile();
                    if(file.isDirectory()) {
                        showFilesInDirectory(file, true);
                    }
                }
            });
            return row;
        });
        fileTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    private void showFilesInDirectory(File directory, boolean isNavigating) {
        if(!directory.isDirectory()) return;

        if(isNavigating)
        {
            if(currentDirectory != null && !isNavigatingBack && !isNavigatingForward) {
                backHistory.push(currentDirectory);
                forwardHistory.clear();
            }
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
        String displayName = file.getPath().endsWith(File.separator)
                ? file.getPath()
                : file.getName();

        TreeItem<File> item = new TreeItem<>(file){
            @Override
            public String toString() {
                return displayName;
            }
        };
        item.setExpanded(false);

        if(file.isDirectory()) {
            TreeItem<File> dummy = new TreeItem<>();
            item.getChildren().add(dummy);

            item.addEventHandler(TreeItem.<File>branchExpandedEvent(), event -> {
                TreeItem<File> expandedItem = event.getSource();

                if (expandedItem.getChildren().size() == 1 &&
                        expandedItem.getChildren().get(0).getValue() == null) {

                    expandedItem.getChildren().clear();
                    File[] subDirs = expandedItem.getValue().listFiles(File::isDirectory);

                    if (subDirs != null) {
                        for (File dir : subDirs) {
                            expandedItem.getChildren().add(createNode(dir));
                        }
                    }
                }
            });
        }

        item.expandedProperty().addListener((observable, wasExpanded, isExpanded) -> {
           if(isExpanded && item.getChildren().isEmpty()) {
               item.getChildren().clear();
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

    private TreeItem<File> findTreeItem(TreeItem<File> root, File target) {
        if (root == null || target == null) {
            return null;
        }
        if (root.getValue() != null && root.getValue().getAbsolutePath().equals(target.getAbsolutePath())) {
            return root;
        }
        for (TreeItem<File> child : root.getChildren()) {
            TreeItem<File> found = findTreeItem(child, target);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private void updateTreeWithNewDirectory(File newDir) {
        File parentDir = newDir.getParentFile();
        if (parentDir == null) {
            return;
        }

        TreeItem<File> parentTreeItem = findTreeItem(directoryTree.getRoot(), parentDir);
        if (parentTreeItem == null) {
            return;
        }

        boolean hasDummy = parentTreeItem.getChildren().size() == 1 &&
                parentTreeItem.getChildren().get(0).getValue() == null;

        if (!hasDummy) {
            TreeItem<File> newItem = createNode(newDir);
            parentTreeItem.getChildren().add(newItem);
            parentTreeItem.getChildren().sort((ti1, ti2) ->
                    ti1.getValue().getName().compareToIgnoreCase(ti2.getValue().getName()));
        }
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
                if (isDirectory) {
                    if (newItem.mkdir()) {
                        updateDirectoryView();
                        updateTreeWithNewDirectory(newItem);
                    } else {
                        showError("Fail", "Unable to create directory");
                    }
                } else {
                    if (newItem.createNewFile()) {
                        updateDirectoryView();
                    } else {
                        showError("Fail", "Unable to create new file");
                    }
                }
            } catch (IOException e) {
                showError("Error occured", e.getMessage());
            }
        });

    }

    private void updateDirectoryView() {
        showFilesInDirectory(currentDirectory, false);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void removeDirectoryFromTree(File deletedDir) {
        TreeItem<File> deletedTreeItem = findTreeItem(directoryTree.getRoot(), deletedDir);
        if (deletedTreeItem == null) {
            return;
        }

        TreeItem<File> parentTreeItem = deletedTreeItem.getParent();
        if (parentTreeItem != null) {
            parentTreeItem.getChildren().remove(deletedTreeItem);
            parentTreeItem.getChildren().sort((ti1, ti2) ->
                    ti1.getValue().getName().compareToIgnoreCase(ti2.getValue().getName())
            );
            directoryTree.refresh();
        }
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
                    if (f.isDirectory() && findTreeItem(directoryTree.getRoot(), f) != null) {
                        deleteRecursively(f);
                        removeDirectoryFromTree(f);
                    }
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
            showFilesInDirectory(previousDirectory, true);
            isNavigatingBack = false;
        }
    }

    @FXML
    private void handleForward(){
        if(!forwardHistory.isEmpty()) {
            isNavigatingForward = true;
            backHistory.push(currentDirectory);
            File nextDirectory = forwardHistory.pop();
            showFilesInDirectory(nextDirectory, true);
            isNavigatingForward = false;
        }
    }
}
