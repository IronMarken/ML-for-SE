package logic;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Release {

    private LocalDateTime releaseDate;
    private String jiraName;
    private String gitName;
    private String releaseID;

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


    //TODO Debug function
    public void printDebugRelease(){
        String outStr = "\tJiraName: " + this.jiraName + " GitName: " + this.gitName + " ReleaseID: " + this.releaseID;
        if(this.releaseDate != null){
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            outStr = "\tReleaseDate: " + this.releaseDate.format(dateTimeFormatter) + outStr;
        }
        System.out.println(outStr);
    }
}
