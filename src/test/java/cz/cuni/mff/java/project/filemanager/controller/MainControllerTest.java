package cz.cuni.mff.java.project.filemanager.controller;

import cz.cuni.mff.java.project.filemanager.model.FileItem;
import cz.cuni.mff.java.project.filemanager.util.ClipboardManager;
import javafx.collections.ObservableList;
import javafx.scene.control.TextInputDialog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

class MainControllerTest {
    private MainController controller;
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        controller = new MainController();
        controller.currentDirectory = tempDir.toFile();
    }

    @Test
    void uniqueNamesTest() throws IOException {
        Files.createFile(tempDir.resolve("test.txt"));

        File result = controller.generateName(tempDir.toFile(), "test.txt");
        assertEquals("test - Copy.txt", result.getName());

        Files.createFile(tempDir.resolve("test - Copy.txt"));
        File result2 = controller.generateName(tempDir.toFile(), "test.txt");
        assertEquals("test - Copy(2).txt", result2.getName());
    }

    @Test
    void deleteRecursivelyTest() throws IOException {
        Path subDir = tempDir.resolve("subdir");
        Files.createDirectories(subDir);
        Files.createFile(subDir.resolve("file.txt"));

        controller.deleteRecursively(subDir.toFile());

        assertFalse(Files.exists(subDir));
    }

    @Test
    void renameFailIfExistsTest() throws IOException {
        Files.createFile(tempDir.resolve("original.txt"));
        Files.createFile(tempDir.resolve("existing.txt"));

        FileItem item = new FileItem(tempDir.resolve("original.txt").toFile());
        boolean result = item.getFile().renameTo(new File(tempDir.toFile(), "existing.txt"));

        assertFalse(result);
    }

}