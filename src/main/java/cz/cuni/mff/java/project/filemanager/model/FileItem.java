package cz.cuni.mff.java.project.filemanager.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Represents a file system item in the TableView.
 * Wraps a File object and provides attributes for displaying.
 */
public class FileItem {
    private final File file;
    private final SimpleStringProperty name;

    /**
     * Constructs a FileItem
     * @param file The File object to represent
     */
    public FileItem(File file) {
        this.file = file;
        this.name = new SimpleStringProperty(file.getName());
    }

    /**
     * @return The current name of the file
     */
    public String getName(){
        return name.get();
    }

    /**
     * @return The StringProperty name
     */
    public StringProperty name(){
        return name;
    }

    /**
     * Updates the file name property
     * @param name New name to set (does not rename the actual file)
     */
    public void setName(String name){
        this.name.set(name);
    }

    /**
     * Gets formatted size information
     * @return "<DIR>" for directories, formatted byte count for files
     */
    public String getSize(){
        if(file.isDirectory()) return "<DIR>";
        long size = file.length();
        return String.format("%,d bytes", size);
    }

    /**
     * Gets formatted modification timestamp
     * @return Date string in "dd-MM-yyyy HH:mm:ss" format
     */
    public String getModified(){
        LocalDateTime dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(file.lastModified()), ZoneId.systemDefault()
        );
        return dateTime.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
    }

    /**
     * Access to the File object
     * @return The represented File
     */
    public File getFile() {
        return file;
    }

}
