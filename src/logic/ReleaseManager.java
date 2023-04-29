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


    private void parseRelease(JSONObject obj) throws JSONException, IOException {
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
            releaseDate = this.gitBoundary.getDate(this.nameAdapter.deriveGitName(name));
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


    public void retrieveReleases() throws IOException, JSONException {
        LOGGER.log(Level.INFO, "Retrieving releases");
        //init release array
        this.releases = new ArrayList<>();
        this.unreleased = new ArrayList<>();
        this.releaseSubset = new ArrayList<>();

        JSONArray versions = JiraBoundary.getReleases(this.projectName);
        for (int i = 0; i < versions.length(); i++) {
            parseRelease(versions.getJSONObject(i));
        }

        //order releases by date
        this.releases.sort((Release r1, Release r2) -> r1.getReleaseDate().compareTo(r2.getReleaseDate()));

        //consider only first half
        this.releaseSubset = this.releases.subList(0, this.releases.size()/2);

        String outStr = "Retrieved " + this.releases.size() + " releases released";
        LOGGER.log(Level.INFO, outStr);

        outStr = "Retrieved " + this.unreleased.size() + " releases unreleased";
        LOGGER.log(Level.INFO, outStr);

        outStr = "Analysis will be carried out considering the first " + this.releaseSubset.size() + " releases";
        LOGGER.log(Level.INFO, outStr);

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
