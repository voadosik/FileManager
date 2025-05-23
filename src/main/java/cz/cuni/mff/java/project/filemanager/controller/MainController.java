package cz.cuni.mff.java.project.filemanager.controller;

import cz.cuni.mff.java.project.filemanager.model.FileItem;

import cz.cuni.mff.java.project.filemanager.util.ClipboardManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Stack;

/**
 * Main controller class for the file manager app.
 * Handles all user interactions.
 */
public class MainController {
    @FXML protected TreeView<File> directoryTree;
    @FXML protected TableView<FileItem> fileTable;
    @FXML protected TableColumn<FileItem, String> fileName;
    @FXML protected TableColumn<FileItem, String> fileType;
    @FXML protected TableColumn<FileItem, String> fileDate;
    @FXML protected TableColumn<FileItem, String> fileSize;

    @FXML protected TextField pathTextField;
    protected File currentDirectory;


    protected Stack<File> backHistory = new Stack<>();
    protected Stack<File> forwardHistory = new Stack<>();
    protected boolean isNavigatingBack = false;
    protected boolean isNavigatingForward = false;

    protected ClipboardManager clipboard = ClipboardManager.getInstance();

    @FXML private Button btnBack;
    @FXML private Button btnForward;
    @FXML private TextField searchTextField;
    private File searchDirectory;
    private Boolean isSearching = false;



    /**
     * Initialize the controller after FXML loading.
     * Sets up table columns and cell factories, directory tree structure.
     * Cell factories for tree and table views, event handlers for file operations.
     */
    @FXML
    public void initialize() {
        // Configure file name column with cells
        fileName.setCellValueFactory(new PropertyValueFactory<>("name"));
        fileName.setOnEditCommit(event -> {
            FileItem item = event.getRowValue();
            String newName = event.getNewValue();
            File newFile = new File(item.getFile().getParentFile(), newName);

            try{
                // Attempt file rename operation
                if(item.getFile().renameTo(newFile)){
                    updateDirectoryView();
                }else{
                    showError("Rename failed", "Could not rename file");
                    fileTable.refresh(); // Revert table
                }

            }catch (SecurityException se){
                showError("Permission denied", "No permission to rename file");
                fileTable.refresh(); // Revert table
            }
        });

        // Configure remaining table columns
        fileType.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getFile().isDirectory() ? "Directory" : "File"));
        fileDate.setCellValueFactory(new PropertyValueFactory<>("modified"));
        fileSize.setCellValueFactory(new PropertyValueFactory<>("size"));

        // Initialize navigation buttons state
        btnBack.setDisable(true);
        btnForward.setDisable(true);

        // Create root node for directory tree
        TreeItem<File> rootItem = new TreeItem<>(new File("This PC")){
            @Override
            public String toString() {
                return "This PC";
            }
        };
        rootItem.setExpanded(true);

        // Load drive icon for tree items
        Image driveIcon = new Image(Objects
                .requireNonNull(getClass()
                        .getResourceAsStream("/cz/cuni/mff/java/project/filemanager/images/drive-icon.png")));

        // Add available drives to the root
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

        // Handle directory tree selection changes
        directoryTree.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (isSearching) handleClearSearch();
            if(newValue != null) {
                showFilesInDirectory(newValue.getValue(), true); //update file view
            }
        });

        // Cells for directory tree items
        directoryTree.setCellFactory(tv -> new TreeCell<>() {
            private final Image driveImage;
            private final Image folderImage;
            {
                // Load icons
                try {
                    driveImage = new Image(Objects
                            .requireNonNull(getClass()
                                    .getResourceAsStream("/cz/cuni/mff/java/project/filemanager/images/drive-icon.png")));

                    folderImage = new Image(Objects
                            .requireNonNull(getClass()
                                    .getResourceAsStream("/cz/cuni/mff/java/project/filemanager/images/folder-icon.png")));

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

                    //Set icon
                    if (item.getPath().endsWith(File.separator)) {
                        iconView.setImage(driveImage);
                    } else {
                        iconView.setImage(folderImage);
                    }

                    setGraphic(iconView);
                }
            }
        });

        // User home is current dir
        currentDirectory = new File("user.home");

        // Configure row double-click
        fileTable.setRowFactory(tv ->{
            TableRow<FileItem> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if(event.getClickCount() == 2 && !row.isEmpty()) {
                    FileItem item = row.getItem();
                    File file = item.getFile();
                    if (isSearching) {
                        // Navigate to search result's parent directory
                        File parentDir = file.getParentFile();
                        navigateToDirectoryFromSearch(parentDir, file);
                    } else {
                        if (file.isDirectory()) {
                            showFilesInDirectory(file, true); // Go to directory
                        }
                        else {
                            handleOpenFile(file); // Open file with default app
                        }
                    }
                }
            });
            return row;
        });

        // Enable multi-selection in file table
        fileTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }


    /**
     * Navigates to a directory from the search string
     * @param directory Destination directory to display
     * @param target File to highlight in the directory view
     */
    private void navigateToDirectoryFromSearch(File directory, File target) {
        isSearching = false;
        searchTextField.clear();

        if(currentDirectory != null){
            backHistory.push(currentDirectory);
            forwardHistory.clear();
        }

        showFilesInDirectory(directory, false);

        fileTable.getItems().stream()
                .filter(item -> item.getFile().equals(target))
                .findFirst()
                .ifPresent(item -> {
                    fileTable.getSelectionModel().select(item);
                    fileTable.scrollTo(item);
                });
    }

    /**
     * Displays files in the specified directory
     * @param directory Directory to display
     * @param isNavigating True if navigation controls are used
     */
    protected void showFilesInDirectory(File directory, boolean isNavigating) {
        if(!directory.isDirectory()) return;
        if(isNavigating && !isSearching)
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

    /**
     * Populates the file table with items from the directory
     * @param directory Directory, contents of which to display
     */
    protected void updateFileTable(File directory) {
        ObservableList<FileItem> items = FXCollections.observableArrayList();
        File[] files = directory.listFiles();
        if(files != null) {
            for(File file : files) {
                items.add(new FileItem(file));
            }
        }
        fileTable.setItems(items);
    }

    /**
     * Updates the state of navigation buttons based on the history stacks
     */
    private void updateNavigationButtons() {
        btnBack.setDisable(backHistory.isEmpty());
        btnForward.setDisable(forwardHistory.isEmpty());
    }


    /**
     * Reveals the specified directory in the tree view
     * @param directory Directory to select in the tree structure
     */
    private void selectInTreeView(File directory) {
        TreeItem<File> rootItem = directoryTree.getRoot();
        TreeItem<File> targetItem = findTreeItem(rootItem, directory);

        if (targetItem != null) {
            TreeItem<File> parent = targetItem.getParent();
            while (parent != null) {
                parent.setExpanded(true);
                parent = parent.getParent();
            }

            directoryTree.getSelectionModel().select(targetItem);
            directoryTree.scrollTo(directoryTree.getRow(targetItem));
        }
    }

    /**
     * Creates a tree node for the file system structure
     * @param file Item to represent in the tree
     * @return Configured TreeItem with children received via lazy loading
     */
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
            // Add dummy node to enable expansion arrow before loading
            TreeItem<File> dummy = new TreeItem<>();
            item.getChildren().add(dummy);

            // Handle expansion events for lazy loading
            item.addEventHandler(TreeItem.<File>branchExpandedEvent(), event -> {
                TreeItem<File> expandedItem = event.getSource();

                // Check if we need to load real children
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


    /**
     * Searches the directory tree for a specific file node
     * @param root Starting node for the search
     * @param target File to locate
     * @return TreeItem containing the target file
     */
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


    /**
     * Updates the directory tree when a new directory is added
     * @param newDir New directory to add to the tree
     */
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

    /**
     * Handles creation of new files/directories through a dialog
     * @param isDirectory True - directory, False - file
     */
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
                showError("Error occurred", e.getMessage());
            }
        });

    }

    /**
     * Refreshes the current directory view
     */
    private void updateDirectoryView() {
        showFilesInDirectory(currentDirectory, false);
    }

    /**
     * Displays an error dialog to the user
     * @param title Dialog window title
     * @param message Dialog error message
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Removes a directory node from the tree view
     * @param deletedDir Directory to remove
     */
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

    /**
     * Deletes a file or directory
     * @param file Target directory/file to delete
     * @throws IOException If any deletion operation fails
     */
    protected void deleteRecursively(File file) throws IOException {
        Path path;
        path = file.toPath();
        if(Files.exists(path)){
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
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

    /**
     * Recursively searches a directory for files matching the search
     * @param dir Directory to search
     * @param text Search query, case-insensitive
     * @param items List to add matching files
     */
    private void searchDirectory(File dir, String text, ObservableList<FileItem> items) {
        File[] files = dir.listFiles();

        if(files == null) return;

        for(File file : files) {
            if(Thread.currentThread().isInterrupted()) return;

            if(file.getName().toLowerCase().contains(text.toLowerCase())) {
                items.add(new FileItem(file));
            }
            if(file.isDirectory()){
                searchDirectory(file, text, items);
            }
        }

    }

    /**
     * Copies a directory structure
     * @param source Source directory to copy
     * @param target Destination directory
     * @throws IOException if any copy operation fails
     */
    private void copyDirectory(File source, File target) throws IOException {
        // Use FileVisitor to recursively walk through the directory tree
        Files.walkFileTree(source.toPath(), new SimpleFileVisitor<>() {
            /**
             * Handles directories before their contents are processed
             * @param dir Current directory being visited
             * @param attrs Directory attributes
             * @return CONTINUE to keep processing the file tree
             */
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path relative = source.toPath().relativize(dir);
                Path destination = target.toPath().resolve(relative);

                if (!Files.exists(destination)) {
                    Files.createDirectory(destination);
                }
                return FileVisitResult.CONTINUE;
            }

            /**
             * Handles individual files during traversal
             * @param file Current file
             * @param attrs File attributes
             * @return CONTINUE to keep processing the file tree
             */
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path relative = source.toPath().relativize(file);
                Path destination = target.toPath().resolve(relative);

                Files.copy(file, destination);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Generates unique name for files
     * @param targetDirectory Destination directory
     * @param name Original file name
     * @return File with unique name for the target directory
     */
    protected File generateName(File targetDirectory, String name){
        String baseName;
        String extension;
        int dot = name.lastIndexOf('.');

        if(dot > 0){
            baseName = name.substring(0, dot);
            extension = name.substring(dot);
        }else{
            baseName = name;
            extension = "";
        }

        int copyNumber = 1;
        File candidate = new File(targetDirectory, name);

        if(!candidate.exists()){
            return candidate;
        }

        while(true){
            String newName = String.format("%s - Copy%s", baseName, extension);
            if(copyNumber > 1){
                newName = String.format("%s - Copy(%d)%s", baseName, copyNumber, extension);
            }
            candidate = new File(targetDirectory, newName);
            if(!candidate.exists()){
                return candidate;
            }
            copyNumber++;
        }
    }


    // Handlers


    /**
     * Handles new file creation
     */
    @FXML
    private void handleNewFile() {
        createNewItem(false);
    }

    /**
     * Handles new directory creation
     */
    @FXML
    private void handleNewFolder() {
        createNewItem(true);
    }

    /**
     * Handles file/directory rename
     * Shows input dialog and validates new name
     */
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

    /**
     * Handles file/directory deletion
     * Shows confirmation dialog before proceeding
     */
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
                    if (f.isDirectory()) {
                        removeDirectoryFromTree(f);
                    }
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

    /**
     * Navigates back through directory history
     */
    @FXML
    protected void handleBack(){
        if(!backHistory.isEmpty()) {
            isNavigatingBack = true;
            forwardHistory.push(currentDirectory);
            File previousDirectory = backHistory.pop();
            showFilesInDirectory(previousDirectory, true);
            isNavigatingBack = false;
        }
    }

    /**
     * Navigates forward through directory history
     */
    @FXML
    protected void handleForward(){
        if(!forwardHistory.isEmpty()) {
            isNavigatingForward = true;
            backHistory.push(currentDirectory);
            File nextDirectory = forwardHistory.pop();
            showFilesInDirectory(nextDirectory, true);
            isNavigatingForward = false;
        }
    }

    /**
     * Executes file search operation
     */
    @FXML
    private void handleSearch(){
        String searchText = searchTextField.getText().trim();
        if(searchText.isEmpty()){
            showError("Empty Search", "Please enter something to search");
            return;
        }

        searchDirectory = currentDirectory;
        isSearching = true;

        Task<ObservableList<FileItem>> searchTask = new Task<>() {
            @Override
            protected ObservableList<FileItem> call() throws Exception {
                try {
                    ObservableList<FileItem> items = FXCollections.observableArrayList();
                    searchDirectory(searchDirectory, searchText, items);
                    return items;
                } catch (Exception e) {
                    throw new Exception(e);
                }

            }
        };

        searchTask.setOnSucceeded(event -> {
            fileTable.setItems(searchTask.getValue());
            pathTextField.setText("Results in: " + searchDirectory.getAbsolutePath());
        });

        searchTask.setOnFailed(event -> {
            showError("Search failed", "Error during search");
            isSearching = false;
        });

        new Thread(searchTask).start();


    }

    /**
     * Clears current search results
     */
    @FXML
    private void handleClearSearch(){
        isSearching = false;
        searchTextField.clear();
        showFilesInDirectory(searchDirectory != null ?
                searchDirectory : currentDirectory, false);
    }

    /**
     * Handles copy operation to clipboard
     */
    @FXML
    private void handleCopy(){
        ObservableList<FileItem> selectedItems = fileTable.getSelectionModel().getSelectedItems();
        if(selectedItems == null || selectedItems.isEmpty()) {
            showError("No items selected", "Please select items to copy");
            return;
        }

        List<File> files = selectedItems
                .stream()
                .map(FileItem::getFile)
                .toList();

        clipboard.setFiles(files);
    }

    /**
     * Handles paste operation from clipboard
     */
    @FXML
    private void handlePaste(){
        List<File> filesToPaste = clipboard.getFiles();

        if(filesToPaste.isEmpty()){
            showError("Clipboard is empty", "No files to paste");
            return;
        }

        Task<Void> pasteTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                for(File file : filesToPaste){
                    File destination = generateName(currentDirectory, file.getName());

                    if(file.isDirectory()){
                        copyDirectory(file, destination);
                    }
                    else{
                        Files.copy(file.toPath(), destination.toPath());
                    }
                }
                return null;
            }
        };

        pasteTask.setOnSucceeded(event -> {
            updateDirectoryView();
        });

        pasteTask.setOnFailed(event -> {
            Throwable ex = event.getSource().getException();
            showError("Paste failed", "Error during paste, exception: " + ex.getMessage());
        });

        new Thread(pasteTask).start();
    }

    /**
     * Opens file with default system application
     * @param file File to open
     */
    @FXML
    private void handleOpenFile(File file){
        try{
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            }
        } catch (IOException e) {
            showError("Open Failed", "Could not open file: " + e.getMessage());
        }
    }

    /**
     * Refreshes current directory view
     */
    @FXML
    private void handleRefresh() {
        updateDirectoryView();
    }

}
