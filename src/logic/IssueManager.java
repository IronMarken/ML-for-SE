package logic;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;


public class IssueManager {

    private static final Logger LOGGER = Logger.getLogger(IssueManager.class.getName());
    private static final String ISSUE_FIELDS = "fields";
    private static final double PERC = 0.03;
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

    public void setupIssues() throws JSONException, IOException {
        // retrieve issues
        retrieveIssues();
        // add injected version where needed with proportion
        proportion();
        //filter after proportion
        filterAfterProportion();

    }

    private void filterAfterProportion(){
        String report = "Filtering after proportion";
        LOGGER.log(Level.INFO, report);
        int nullVersionCnt = 0;
        int inconsistentCnt= 0;
        int emptyCnt = 0;
        int sameVersionCnt = 0;
        int totalCnt = 0;
        List<Issue> filteredList = new ArrayList<>();

        for (Issue issue: this.issueList){
            switch(issue.validateIssue()){
                case NULL_VERSION:
                    nullVersionCnt++;
                    break;
                case INCONSISTENT:
                    inconsistentCnt++;
                    break;
                case NULL_EMPTY:
                    emptyCnt++;
                    break;
                case IV_IS_FV:
                    sameVersionCnt++;
                    break;
                case VALID:
                default:
                    filteredList.add(issue);
                    break;
            }
            totalCnt ++;
        }
        //reorder
        filteredList.sort((Issue i1, Issue i2) -> i1.getIndex().compareTo(i2.getIndex()));

        this.issueList = filteredList;
        //log final report
        report = "\n-Total issues parsed: " + totalCnt +  "\n-Valid issues: " + this.issueList.size() + "\n-Issues filtered for null Version: " + nullVersionCnt + "\n-Issues filtered for data inconsistency: " + inconsistentCnt + "\n-Issues filtered for empty commit list and null injected version: " + emptyCnt + "\n-Issues filtered for injected version equals to fix version: " + sameVersionCnt + "\n";
        LOGGER.log(Level.INFO, report);


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


    private void retrieveIssues() throws JSONException, IOException {
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
        int sameVersionCount = 0;

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
                    //skip because fv=iv
                    case IV_IS_FV:
                        sameVersionCount++;
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
        report = "\n-Total issues retrieved: " + totalCount +  "\n-Valid issues retrieved: " + this.issueList.size() + "\n-Issues filtered for null Version: " + nullVersionCount + "\n-Issues filtered for data inconsistency: " + inconsistentCount + "\n-Issues filtered for empty commit list and null injected version: " + emptyCount + "\n-Issues filtered for injected version equals to fix version: " + sameVersionCount + "\n";
        LOGGER.log(Level.INFO, report);
    }

    private void proportion() {

        String report;
        double p;
        Release injectedVersion;
        Release openingVersion;
        Release fixVersion;
        int ivIndex;

        List<Release> releaseList;
        List<Release> unreleasedList;
        List<Issue> subList = new ArrayList<>();

        releaseList = this.releaseManager.getReleases();
        unreleasedList = this.releaseManager.getUnreleased();

        for (Issue issue : this.issueList) {
            injectedVersion = issue.getInjectedVersion();
            openingVersion = issue.getOpeningVersion();
            fixVersion = issue.getFixVersion();

            if (injectedVersion == null) {
                report = "Applying proportion";
                LOGGER.log(Level.INFO, report);

                p = movingWindow(subList);

                ivIndex = (int) Math.round(fixVersion.getReleaseIndex() - (fixVersion.getReleaseIndex() - openingVersion.getReleaseIndex()) * p);

                //proportion formula can return negative value if
                //ov is low and distance between ov and fv is big
                //p always >=1
                if (ivIndex <= 0)
                    ivIndex = 1;

                //check if in unreleased
                if (ivIndex > releaseList.size())
                    injectedVersion = unreleasedList.get(0);
                else
                    //indexes starts from 1
                    injectedVersion = releaseList.get(ivIndex - 1);
                issue.setInjectedVersion(injectedVersion);

                // report
                report = "Issue: " + issue.getIndex() +
                        "\n-p value: " + p +
                        "\n-Fix version: " + fixVersion.getGitName() + " " + fixVersion.getReleaseIndex() +
                        "\n-Opening version: " + openingVersion.getGitName() + " " + openingVersion.getReleaseIndex() +
                        "\n-Injected version: " + injectedVersion.getGitName() + " " + injectedVersion.getReleaseIndex();
                LOGGER.log(Level.INFO, report);
            }
            subList.add(issue);
        }
    }


    private double movingWindow(List<Issue> list) {
        double p = 1;
        Release fixVersion;
        Release openingVersion;
        Release injectedVersion;
        double sum = 0;

        int count = 0;

        List<Issue> subList;


        if(list.isEmpty()) {
            //p for first issue if IV is null
            //average of all issues with IV not null
            subList = this.issueList.stream().filter(i -> i.getInjectedVersion() != null).collect(Collectors.toList());
        }else {
            //calculated as moving window
            int size;
            //calculate p on last PERC (1%) issues
            size = (int)Math.ceil(PERC * list.size());
            int fromIndex = list.size()-size;
            subList = list.subList(fromIndex, list.size());
            String report = "p calculated over " + size + " previous issues";
            LOGGER.log(Level.INFO, report);
        }

        //calculate p and its average
        for (Issue issue : subList) {
            fixVersion = issue.getFixVersion();
            openingVersion = issue.getOpeningVersion();
            injectedVersion = issue.getInjectedVersion();

            if (fixVersion.getReleaseIndex() != openingVersion.getReleaseIndex()) {
                sum = sum + ((double) (fixVersion.getReleaseIndex() - injectedVersion.getReleaseIndex()) / (double) (fixVersion.getReleaseIndex() - openingVersion.getReleaseIndex()));
            } else {
                sum = sum + 1;
            }
            count++;
        }
        //control division by 0
        if( count != 0)
            p = sum/count;

        return p;
    }


}
