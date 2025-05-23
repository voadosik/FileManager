# File Manager

File manager app built with JavaFX, supporting basic file operations and directory navigation.

## Features

- Browse directories using tree view or table view
- Create new files/folders
- Navigation history
- Open files with default applications
- Internal clipboard
- Search for the files by name 
- Rename, copy/paste, deletion operations

## Requirements

- Java 17
- JavaFX SDK 17
- Maven

## Instalation

1. Clone the repository
2. Import project into IntelliJ as Maven project

## Running the Application
### IDE
1. Open `FileManagerApp.java`
2. Ensure JavaFX SDK is configured in project settings
3. Start the program

### Command Line

```bash
mvn clean javafx:run
```


### UI Overview
- Toolbar - navigation buttons and file operations
- Directory Tree
- Central panel with file details
- Current directory path
- Search - enter query in search field
- Refresh button(in case of the recent file modification)
