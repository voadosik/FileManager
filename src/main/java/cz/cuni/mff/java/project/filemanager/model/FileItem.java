package cz.cuni.mff.java.project.filemanager.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class FileItem {
    private final File file;
    private final SimpleStringProperty name;

    public FileItem(File file) {
        this.file = file;
        this.name = new SimpleStringProperty(file.getName());
    }

    public String getName(){
        return name.get();
    }

    public StringProperty name(){
        return name;
    }

    public void setName(String name){
        this.name.set(name);
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
