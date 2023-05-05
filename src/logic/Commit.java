package logic;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Commit {

    private String sha;
    private String message;
    private String author;
    private LocalDateTime commitDate;


    public Commit(String sha, String message, String author, String commitDate){
        this.sha = sha;
        this.message = message;
        this.author = author;
        this.commitDate = LocalDate.parse(commitDate).atStartOfDay();
    }

    public LocalDateTime getDate() { return this.commitDate; }
}
