package logic;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GitBoundary {

    private static final Logger LOGGER = Logger.getLogger(GitBoundary.class.getName());
    private static final String DATE_FORMAT = "--date=iso";
    private static final String DATE = "--pretty=format:%cd";

    private final String projectName;
    private final File workingCopy;



    public GitBoundary(String gitUrl ) throws IOException {

        //parse project name
        String[] splitted = gitUrl.split("/");
        this.projectName = splitted[splitted.length-1];

        String outputString = "Creating git boundary for " + this.projectName;
        LOGGER.log(Level.INFO, outputString);

        //check if repo directory exists
        File localDir = new File ("repo");
        if( !localDir.isDirectory()) {
            //check errors during dir creation
            if( !localDir.mkdir() )
                LOGGER.log(Level.WARNING, "Repo dir not created");
            else
                LOGGER.log(Level.INFO, "Repo dir created");
        } else
            LOGGER.log(Level.INFO, "Repo dir already exists");

        //clone if working copy doesn't exist or pull it
        this.workingCopy = new File("repo/"+ projectName);
        if(!this.workingCopy.exists()) {
            //clone
            LOGGER.log(Level.INFO,"Cloning project please wait...");
            Runtime.getRuntime().exec(new String[] {"git", "clone", gitUrl}, null, new File("repo"));
            LOGGER.log(Level.INFO, "Project cloned");
        } else {
            //pull
            LOGGER.log(Level.INFO, "Project exists pulling it please wait...");
            Runtime.getRuntime().exec(new String[] {"git", "pull"}, null, this.workingCopy);
            LOGGER.log(Level.INFO, "Pull terminated");
        }
    }


    public LocalDateTime getDate(String name) throws IOException {
        Process process = Runtime.getRuntime().exec(new String[] {"git", "log", name, "-1", DATE ,DATE_FORMAT }, null, this.workingCopy);
        BufferedReader reader = new BufferedReader (new InputStreamReader(process.getInputStream()));
        String line;
        String date = null;
        LocalDateTime dateTime = null;
        while((line = reader.readLine()) != null) {
            date = line;

            //get Date from full line
            date = date.split(" ")[0];

            LocalDate ld = LocalDate.parse(date);
            dateTime = ld.atStartOfDay();
        }

        return dateTime;
    }

}
