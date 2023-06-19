package logic;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Commit {

    private static final Logger LOGGER = Logger.getLogger(Commit.class.getName());
    private String sha;
    private String message;
    private String author;
    private LocalDateTime commitDate;

    private List<CommitFileData> touchedFiles;



    public Commit(String sha, String message, String author, String commitDate){
        this.sha = sha;
        this.message = message;
        this.author = author;
        this.commitDate = LocalDate.parse(commitDate).atStartOfDay();
        this.touchedFiles = new ArrayList<>();
    }

    public LocalDateTime getDate() { return this.commitDate; }

    public String getSha() { return this.sha; }

    public void setTouchedFiles(List<CommitFileData> touchedFiles) {
        this.touchedFiles = touchedFiles;
    }

    public String getAuthor() { return this.author; }

    public List<CommitFileData> getTouchedFiles() { return this.touchedFiles; }

    public List<String> getTouchedFilesNames() {
        // return only the names of touched files
        List<String> nameList = new ArrayList<>();
        for(CommitFileData fileData: this.touchedFiles){
            nameList.add(fileData.getName());
        }
        return nameList;
    }

    public void logMessage(){
        LOGGER.log(Level.INFO, message);
    }
}
