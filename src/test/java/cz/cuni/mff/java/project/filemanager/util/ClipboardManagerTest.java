package cz.cuni.mff.java.project.filemanager.util;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClipboardManagerTest {

    @Test
    void singleInstanceTest() {
        ClipboardManager instance1 = ClipboardManager.getInstance();
        ClipboardManager instance2 = ClipboardManager.getInstance();
        assertSame(instance1, instance2);
    }

    @Test
    void copiesTest() {
        ClipboardManager clipboard = ClipboardManager.getInstance();
        List<File> original = List.of(new File("test1.txt"), new File("test2.txt"));
        clipboard.setFiles(original, false);
        List<File> retrieved = clipboard.getFiles();
        assertNotSame(original, retrieved);
        assertEquals(original.size(), retrieved.size());
    }

    @Test
    void resetStateTest() {
        ClipboardManager clipboard = ClipboardManager.getInstance();
        clipboard.setFiles(List.of(new File("test.txt")), true);
        clipboard.clear();
        assertTrue(clipboard.getFiles().isEmpty());
    }
}