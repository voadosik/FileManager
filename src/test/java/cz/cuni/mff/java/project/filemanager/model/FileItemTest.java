package cz.cuni.mff.java.project.filemanager.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;


class FileItemTest {
    @TempDir
    Path tempDir;

    @Test
    void filePropertiesTest() throws IOException {
        File testFile = tempDir.resolve("test.txt").toFile();
        Files.writeString(testFile.toPath(), "something");
        FileItem item = new FileItem(testFile);
        assertEquals("test.txt", item.getName());
        assertTrue(item.getSize().contains("9 bytes"));
        assertNotNull(item.getModified());
    }

    @Test
    void dirItemTest() {
        File dir = tempDir.toFile();
        FileItem item = new FileItem(dir);
        assertEquals("<DIR>", item.getSize());
    }

    @Test
    void matchFileSystemTest() {
        File file = tempDir.toFile();
        FileItem item = new FileItem(file);
        LocalDateTime expected = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(file.lastModified()),
                ZoneId.systemDefault()
        );
        assertEquals(expected.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")),
                item.getModified());
    }
}