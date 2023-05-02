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


    public LocalDateTime getReleaseDate() {
        return releaseDate;
    }

    public String getGitName() { return gitName; }

    public int getReleaseIndex(){ return this.releaseIndex; }
    public void setReleaseIndex(int releaseIndex) { this.releaseIndex = releaseIndex; }

    public void setJavaFiles(List<JavaFile> javaFiles) { this.javaFiles = javaFiles;   }

    //TODO Debug function
    public void printDebugRelease(){
        String outStr = "\tIndex: "+ this.releaseIndex + " JiraName: " + this.jiraName + " GitName: " + this.gitName + " ReleaseID: " + this.releaseID;
        if(this.releaseDate != null){
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            outStr = "\tReleaseDate: " + this.releaseDate.format(dateTimeFormatter) + outStr;
        }
        System.out.println(outStr);
    }
}
