package logic;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Commit {

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
}
