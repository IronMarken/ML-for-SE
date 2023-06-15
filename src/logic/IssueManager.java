package logic;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class IssueManager {

    private static final Logger LOGGER = Logger.getLogger(IssueManager.class.getName());
    private static final String ISSUE_FIELDS = "fields";
    private String projectName;
    private ReleaseManager releaseManager;
    private GitBoundary gitBoundary;

    private List<Issue> issueList;

    public IssueManager(String projectName, ReleaseManager releaseManager, GitBoundary gitBoundary) {
        this.projectName = projectName;
        this.releaseManager = releaseManager;
        this.gitBoundary = gitBoundary;
        this.issueList = new ArrayList<>();
    }


    private List<Release> extractList(int size, JSONArray ja) throws JSONException {
        List<Release> releaseList = new ArrayList<>();
        String relName;
        Release rel;
        for(int k=0; k < size; k++) {
            relName = ja.getJSONObject(k).getString("name");
            rel = releaseManager.getReleaseByJiraName(relName);
            if(rel != null)
                releaseList.add(rel);
        }
        return releaseList;
    }

    private Release retrieveInjectedVersion(JSONArray ja) throws JSONException {
        int size;
        Release injectedVersion;
        List<Release> releaseList;

        size = ja.length();

        if(size == 0) {
            injectedVersion = null;
        }else {
            releaseList = this.extractList(size, ja);
            injectedVersion = Release.getMinRelease(releaseList);
        }
        return injectedVersion;
    }

    private Release retrieveFixedVersion(JSONArray ja) throws JSONException {
        int size;
        Release fixVersion;
        List<Release> releaseList;

        size = ja.length();


        if(size == 0) {
            fixVersion = null;
        }else {
            releaseList = this.extractList(size, ja);
            fixVersion = Release.getMaxRelease(releaseList);
        }
        return fixVersion;
    }


    public void retrieveIssues() throws JSONException, IOException {
        LOGGER.log(Level.INFO, "Retrieving issues and related data");

        int i = 0;
        int j = 0;
        int total = 1;

        int totalCount = 0;

        String key;
        String id;

        String openingDate;

        Release injectedVersion;
        Release fixVersion;
        Release openingVersion;

        Issue issue;
        List<Commit> commitList;

        int nullVersionCount = 0;
        int inconsistentCount = 0;
        int emptyCount = 0;

        String report;

        do {
            j = i + 1000;
            JSONObject json = JiraBoundary.getIssue(this.projectName, i);
            JSONArray issuesJson = json.getJSONArray("issues");
            total = json.getInt("total");

            for(; i < total && i < j; i++) {
                //i%1000 max per page
                id = issuesJson.getJSONObject(i%1000).getString("id");
                key = issuesJson.getJSONObject(i%1000).getString("key");

                openingDate = issuesJson.getJSONObject(i%1000).getJSONObject(ISSUE_FIELDS).getString("created").split("T")[0];


                injectedVersion = this.retrieveInjectedVersion(issuesJson.getJSONObject(i).getJSONObject(ISSUE_FIELDS).getJSONArray("versions"));
                fixVersion = this.retrieveFixedVersion(issuesJson.getJSONObject(i).getJSONObject(ISSUE_FIELDS).getJSONArray("fixVersions"));
                openingVersion = this.releaseManager.getReleaseFromDate(openingDate);

                // generate issue and related commmits
                issue = new Issue(id, key, injectedVersion, fixVersion, openingVersion);
                commitList = this.gitBoundary.getIssueCommit(issue);
                issue.setCommitList(commitList);

                switch(issue.validateIssue()){
                    //skip issue because opening or fix versions are null
                    case NULL_VERSION:
                        nullVersionCount++;
                        break;
                    //skip issue because data is inconsistent
                    case INCONSISTENT:
                        inconsistentCount++;
                        break;
                    //skip issue because commit list is empty and injected is null
                    case NULL_EMPTY:
                        emptyCount++;
                        break;
                    //valid issue
                    case VALID:
                    default:
                        this.issueList.add(issue);
                }
                totalCount ++;
            }
        }while(i < total);

        report= "Data retrieved";
        LOGGER.log(Level.INFO, report);

        //reorder issues
        this.issueList.sort((Issue i1, Issue i2) -> i1.getIndex().compareTo(i2.getIndex()));

        //log final report
        report = "\n-Total issues retrieved: " + totalCount +  "\n-Valid issues retrieved: " + this.issueList.size() + "\n-Issues filtered for null Version: " + nullVersionCount + "\n-Issues filtered for data inconsistency: " + inconsistentCount + "\n-Issues filtered for empty commit list and null injected version: " + emptyCount + "\n";
        LOGGER.log(Level.INFO, report);

    }


}
