package cz.cuni.mff.java.project.filemanager.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages clipboard operations for file copy/paste functionality.
 * Implements storing files for copy/move operations,
 * retrieving stored files,
 * clearing content.
 */
public class ClipboardManager {
    private static ClipboardManager instance;
    private List<File> clipboardFiles = new ArrayList<>();


    private ClipboardManager() {}

    /**
     * Provides access to the instance
     * @return The single ClipboardManager instance
     */
    public static ClipboardManager getInstance() {
        if (instance == null) {
            instance = new ClipboardManager();
        }
        return instance;
    }

    /**
     * Sets files to the clipboard, replacing previous contents
     * @param files List of files to store
     */
    public void setFiles(List<File> files) {
        clipboardFiles.clear();
        clipboardFiles.addAll(files);
    }

    /**
     * Retrieves a copy of clipboard contents
     * @return New list containing clipboard files
     */
    public List<File> getFiles() {
        return new ArrayList<>(clipboardFiles);
    }

    /**
     * Clears clipboard contents and resets state
     */
    public void clear(){
        clipboardFiles.clear();
    }

}
