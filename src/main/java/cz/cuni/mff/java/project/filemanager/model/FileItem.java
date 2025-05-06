package cz.cuni.mff.java.project.filemanager.model;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class FileItem {
    private File file;

    public FileItem(File file) {
        this.file = file;
    }

    public String getName(){
        return file.getName().isEmpty()? file.getAbsolutePath() : file.getName();
    }

    public String getSize(){
        if(file.isDirectory()) return "<DIR>";
        long size = file.length();
        return String.format("%,d bytes", size);
    }

    public String getModified(){
        LocalDateTime dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(file.lastModified()), ZoneId.systemDefault()
        );
        return dateTime.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
    }

    public File getFile() {
        return file;
    }

}
