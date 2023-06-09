package logic;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class Release {

    private LocalDateTime releaseDate;
    private String jiraName;
    private String gitName;
    private int releaseIndex;
    private String releaseID;

    private List<JavaFile> javaFiles;

    private List<Commit> commitList;

    public Release(LocalDateTime releaseDate, String jiraName, String gitName, String releaseID) {
        this.releaseDate = releaseDate;
        this.jiraName = jiraName;
        this.gitName = gitName;
        this.releaseID = releaseID;
    }

    public Release(String jiraName, String gitName, String releaseID) {
        this.releaseDate = null;
        this.jiraName = jiraName;
        this.gitName = gitName;
        this.releaseID = releaseID;
    }

    public List<JavaFile> getJavaFiles(){ return this.javaFiles; }


    public LocalDateTime getReleaseDate() {
        return releaseDate;
    }

    public String getGitName() { return gitName; }

    public int getReleaseIndex(){ return this.releaseIndex; }
    public void setReleaseIndex(int releaseIndex) { this.releaseIndex = releaseIndex; }

    public void setJavaFiles(List<JavaFile> javaFiles) { this.javaFiles = javaFiles;   }

    public void setCommitList(List<Commit> commitList){
        this.commitList = commitList;
    }

    public List<Commit> getCommitList(){ return this.commitList; }

    public JavaFile getClassByName(String name) {
        JavaFile file;
        file = this.javaFiles.stream().filter(f -> name.contentEquals(f.getName())).findAny().orElse(null);
        return file;
    }
}
