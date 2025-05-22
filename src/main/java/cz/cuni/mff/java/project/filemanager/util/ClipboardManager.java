package cz.cuni.mff.java.project.filemanager.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ClipboardManager {
    private static ClipboardManager instance;
    private List<File> clipboardFiles = new ArrayList<>();


    private ClipboardManager() {}

    public static ClipboardManager getInstance() {
        if (instance == null) {
            instance = new ClipboardManager();
        }
        return instance;
    }

    public void setFiles(List<File> files, boolean isCut) {
        clipboardFiles.clear();
        clipboardFiles.addAll(files);
    }

    public List<File> getFiles() {
        return new ArrayList<>(clipboardFiles);
    }


    public void clear(){
        clipboardFiles.clear();
    }

}
