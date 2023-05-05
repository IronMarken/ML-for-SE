package logic;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReleaseManager {

    private static final Logger LOGGER = Logger.getLogger(ReleaseManager.class.getName());
    private final String projectName;
    private final GitBoundary gitBoundary;
    private final ReleaseNameAdapter nameAdapter;
    private List<Release> releases;
    private List<Release> unreleased;
    //analysis only on a subset of releases
    private List<Release> releaseSubset;


    public ReleaseManager(String projectName, GitBoundary gitBoundary, ReleaseNameAdapter nameAdapter ) {
        this.projectName = projectName;
        this.gitBoundary = gitBoundary;
        this.nameAdapter = nameAdapter;
    }

    public void setupReleaseManager() throws IOException, JSONException, InterruptedException {
        // setup releases
        this.retrieveReleases();
        // setup java classes on each release
        this.retrieveClasses();
        // retrieve commits for each release
        this.retrieveReleaseCommit();
    }


    private void addRelease(String jiraName, String id, LocalDateTime releaseDate) {
        String gitName = nameAdapter.deriveGitName(jiraName);
        Release r = new Release(releaseDate, jiraName, gitName, id);
        this.releases.add(r);
    }

    private void addUnreleased(String jiraName, String id) {
        String gitName = nameAdapter.deriveGitName(jiraName);
        Release r = new Release(jiraName, gitName, id);
        this.unreleased.add(r);
    }


    private void parseRelease(JSONObject obj) throws JSONException, IOException, InterruptedException {
        String name = "";
        String id = "";
        LocalDateTime releaseDate;

        //get parameters from the JSON
        if(obj.has("name"))
            name = obj.getString("name");
        if(obj.has("id"))
            id = obj.getString("id");

        boolean isReleased = obj.getBoolean("released");
        boolean isDated = obj.has("releaseDate");

        //released with JiraDate
        if(isReleased && isDated) {
            releaseDate = LocalDate.parse(obj.getString("releaseDate")).atStartOfDay();
            this.addRelease(name, id, releaseDate);
        }

        //released without JiraDate
        if(isReleased && !isDated) {
            releaseDate = this.gitBoundary.getDate(this.nameAdapter.deriveGitName(name), true);
            if(releaseDate == null)
                //date not found in Git
                this.addUnreleased(name, id);
            else
                //date found in Git
                this.addRelease(name, id, releaseDate);

        }

        //unreleased version
        if(!isReleased)
            this.addUnreleased(name, id);
    }


    private void retrieveReleases() throws IOException, JSONException, InterruptedException {
        LOGGER.log(Level.INFO, "Retrieving releases");
        int i;
        //init release array
        this.releases = new ArrayList<>();
        this.unreleased = new ArrayList<>();
        this.releaseSubset = new ArrayList<>();

        JSONArray versions = JiraBoundary.getReleases(this.projectName);
        for (i = 0; i < versions.length(); i++) {
            parseRelease(versions.getJSONObject(i));
        }

        //order releases by date
        this.releases.sort((Release r1, Release r2) -> r1.getReleaseDate().compareTo(r2.getReleaseDate()));

        LOGGER.log(Level.INFO, "Indexing releases");
        //set index
        for (i = 0; i < this.releases.size(); i++ ) {
            this.releases.get(i).setReleaseIndex(i+1);
        }
        //for unreleased maxIndex + 1
        for (Release release : this.unreleased) {
            release.setReleaseIndex(i + 1);
        }

        //consider only first half
        this.releaseSubset = this.releases.subList(0, this.releases.size()/2);

        String outStr = "Retrieved " + this.releases.size() + " releases released";
        LOGGER.log(Level.INFO, outStr);

        outStr = "Retrieved " + this.unreleased.size() + " releases unreleased";
        LOGGER.log(Level.INFO, outStr);

        outStr = "Analysis will be carried out considering the first " + this.releaseSubset.size() + " releases";
        LOGGER.log(Level.INFO, outStr);

    }

    private void retrieveClasses() throws IOException, InterruptedException {
        LOGGER.log(Level.INFO, "Retrieving java files for each release");
        JavaFile javaFile;
        Release release;
        List<String> classes;
        List<JavaFile> fileList;

        String out_string;

        for (int i = 0; i < this.releaseSubset.size(); i++) {
            release = this.releaseSubset.get(i);
            fileList = new ArrayList<>();
            classes = this.gitBoundary.getReleaseClasses(release.getGitName());

            out_string = "Step: " + (i + 1) + "/" + this.releaseSubset.size();
            LOGGER.log(Level.INFO, out_string);

            for (String className : classes) {
                LocalDateTime creationDate = this.gitBoundary.getDate(className, false);
                javaFile = new JavaFile(className, release.getReleaseIndex(), creationDate);
                fileList.add(javaFile);
            }
            release.setJavaFiles(fileList);

            out_string = "Release name: " + release.getGitName() + " Java files retrieved: " + fileList.size();
            LOGGER.log(Level.INFO, out_string);
        }
    }

    private void retrieveReleaseCommit() throws IOException, InterruptedException {
        Release release;
        List<Commit> commitList;
        LocalDateTime minDate;
        LocalDateTime maxDate;
        int i;

        String outString = "Retrieving release commits";
        LOGGER.log(Level.INFO, outString);

        for(i=0; i < this.releaseSubset.size(); i++) {

            outString = "Release: " + (i + 1) + "/" + this.releaseSubset.size();
            LOGGER.log(Level.INFO, outString);

            if(i == 0) {
                // first release
                minDate = null;
            }else
                // other releases
                minDate = this.releaseSubset.get(i-1).getReleaseDate();
            release = this.releaseSubset.get(i);
            maxDate = release.getReleaseDate();
            commitList = this.gitBoundary.getReleaseCommits(minDate, maxDate);
            release.setCommitList(commitList);
            outString = "Release name: " + release.getGitName() + " Commits retrieved: " + commitList.size();
            LOGGER.log(Level.INFO, outString);
        }
        outString = "Commits retrieved";
        LOGGER.log(Level.INFO, outString);
    }



    //TODO Debug function
    public void printDebugReleaseLists() {
        System.out.println("\nReleased " + this.releases.size());
        for(Release release : this.releases){
            release.printDebugRelease();
        }

        System.out.println("\nUnreleased " + this.unreleased.size());
        for(Release release : this.unreleased){
            release.printDebugRelease();
        }

        System.out.println("\nSubset " + this.releaseSubset.size());
        for(Release release : this.releaseSubset){
            release.printDebugRelease();
        }
    }


}
