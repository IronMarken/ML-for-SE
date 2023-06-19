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


    public ReleaseManager(String projectName, GitBoundary gitBoundary, ReleaseNameAdapter nameAdapter) {
        this.projectName = projectName;
        this.gitBoundary = gitBoundary;
        this.nameAdapter = nameAdapter;
    }

    public void setupReleaseManager() throws JSONException, IOException, InterruptedException {
        // setup releases
        this.retrieveReleases();
        // setup java classes on each release
        this.retrieveClasses();
        // calculate sizes
        this.retrieveJavaFileSize();
        // retrieve commits for each release
        this.retrieveReleaseCommit();
        //calculate classes data
        this.retrieveData();
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
        if (obj.has("name"))
            name = obj.getString("name");
        if (obj.has("id"))
            id = obj.getString("id");

        boolean isReleased = obj.getBoolean("released");
        boolean isDated = obj.has("releaseDate");

        //released with JiraDate
        if (isReleased && isDated) {
            releaseDate = LocalDate.parse(obj.getString("releaseDate")).atStartOfDay();
            this.addRelease(name, id, releaseDate);
        }

        //released without JiraDate
        if (isReleased && !isDated) {
            releaseDate = this.gitBoundary.getDate(this.nameAdapter.deriveGitName(name), true);
            if (releaseDate == null)
                //date not found in Git
                this.addUnreleased(name, id);
            else
                //date found in Git
                this.addRelease(name, id, releaseDate);

        }

        //unreleased version
        if (!isReleased)
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
        //set index starting from 1
        for (i = 0; i < this.releases.size(); i++) {
            this.releases.get(i).setReleaseIndex(i + 1);
        }
        //for unreleased maxIndex + 1
        for (Release release : this.unreleased) {
            release.setReleaseIndex(i + 1);
        }

        //consider only first half
        this.releaseSubset = this.releases.subList(0, this.releases.size() / 2);

        String outStr = "Retrieved " + this.releases.size() + " releases released";
        LOGGER.log(Level.INFO, outStr);

        outStr = "Retrieved " + this.unreleased.size() + " releases unreleased";
        LOGGER.log(Level.INFO, outStr);

        outStr = "Analysis will be carried out considering the first " + this.releaseSubset.size() + " releases";
        LOGGER.log(Level.INFO, outStr);

    }

    //retrieve the classes of each release their size and age in weeks
    private void retrieveClasses() throws IOException, InterruptedException {
        LOGGER.log(Level.INFO, "Retrieving java files for each release");
        JavaFile javaFile;
        Release release;
        List<String> classes;
        List<JavaFile> fileList;

        String outString;

        for (int i = 0; i < this.releaseSubset.size(); i++) {
            release = this.releaseSubset.get(i);
            fileList = new ArrayList<>();
            classes = this.gitBoundary.getReleaseClasses(release.getGitName());

            outString = "Step: " + (i + 1) + "/" + this.releaseSubset.size();
            LOGGER.log(Level.INFO, outString);

            for (String className : classes) {
                LocalDateTime creationDate = this.gitBoundary.getDate(className, false);
                //filter limit case
                if (creationDate.isBefore(release.getReleaseDate())) {
                    javaFile = new JavaFile(className, release.getReleaseIndex(), creationDate/*, sizes.get(0), sizes.get(1)*/);
                    //exec age
                    javaFile.execAge(release.getReleaseDate());
                    fileList.add(javaFile);
                }
            }
            release.setJavaFiles(fileList);

            outString = "Release name: " + release.getGitName() + " Java files retrieved: " + fileList.size();
            LOGGER.log(Level.INFO, outString);
        }
    }

    private void retrieveJavaFileSize() throws IOException, InterruptedException, JSONException {
        Release release;
        int relSize = this.releaseSubset.size();
        int index = 0;
        List<Integer> sizes;
        LOGGER.log(Level.INFO, "Calculating file size for each file in each release");
        for (index = 0; index < relSize; index++) {
            String outStr = "Release " + (index + 1) + "/" + relSize;
            LOGGER.log(Level.INFO, outStr);

            release = this.releaseSubset.get(index);
            //change version on repo
            this.gitBoundary.changeRelease(release.getGitName());

            //calculate size for each file
            for (JavaFile file : release.getJavaFiles()) {
                //[0] codes
                //[1] comments
                sizes = TokeiBoundary.getSizes(file.getName(), this.gitBoundary.getWorkingCopy());
                file.setSizes(sizes.get(0), sizes.get(1));
            }
        }
        //reset last version
        this.gitBoundary.restoreLastRelease();

    }

    private void retrieveReleaseCommit() throws IOException, InterruptedException {
        Release release;
        List<Commit> commitList;
        LocalDateTime minDate;
        LocalDateTime maxDate;
        int i;

        String outString = "Retrieving release commits";
        LOGGER.log(Level.INFO, outString);

        for (i = 0; i < this.releaseSubset.size(); i++) {

            outString = "Release: " + (i + 1) + "/" + this.releaseSubset.size();
            LOGGER.log(Level.INFO, outString);

            if (i == 0) {
                // first release
                minDate = null;
            } else
                // other releases
                minDate = this.releaseSubset.get(i - 1).getReleaseDate();
            release = this.releaseSubset.get(i);
            maxDate = release.getReleaseDate();
            commitList = this.gitBoundary.getReleaseCommits(minDate, maxDate);
            outString = "Release name: " + release.getGitName() + " " + release.getReleaseID() + " Commits retrieved: " + commitList.size();
            LOGGER.log(Level.INFO, outString);
            commitList = this.retrieveCommitsData(commitList);
            release.setCommitList(commitList);
        }
        outString = "Commits retrieved";
        LOGGER.log(Level.INFO, outString);
    }


    private List<Commit> retrieveCommitsData(List<Commit> commitList) throws IOException, InterruptedException {
        List<Commit> finalList = new ArrayList<>();
        List<CommitFileData> dataList;
        String outStr;

        outStr = "Retrieving java classes touched by the commits";
        LOGGER.log(Level.INFO, outStr);

        int withFileCount = 0;

        for (Commit commit : commitList) {
            // get data of file touched by the commit
            dataList = this.gitBoundary.getCommitData(commit.getSha());

            // touched at least a java class
            if (!dataList.isEmpty()) {
                commit.setTouchedFiles(dataList);
                withFileCount++;
            }
            finalList.add(commit);
        }
        outStr = "Phase completed\n Parsed: " + commitList.size() + "\tCommit with java classes: " + withFileCount + "\tCommit without java classes: " + (commitList.size() - withFileCount);
        LOGGER.log(Level.INFO, outStr);
        return finalList;
    }

    private void retrieveData() {
        LOGGER.log(Level.INFO, "Calculating file data");
        JavaFile javaFile;
        String fileName;
        String author;
        Integer added;
        Integer deleted;
        Integer chgSetSize;
        Release release;

        String outStr;
        int counter;

        for (counter = 0; counter < this.releaseSubset.size(); counter++) {
            outStr = "Release " + (counter + 1) + "/" + this.releaseSubset.size();
            LOGGER.log(Level.INFO, outStr);
            release = this.releaseSubset.get(counter);
            for (Commit commit : release.getCommitList()) {
                author = commit.getAuthor();
                for (CommitFileData touchedFile : commit.getTouchedFiles()) {
                    fileName = touchedFile.getName();
                    added = touchedFile.getAdded();
                    deleted = touchedFile.getDeleted();
                    chgSetSize = touchedFile.getChgSetSize();

                    javaFile = release.getClassByName(fileName);
                    if (javaFile != null) {
                        //set needed parameters
                        javaFile.increaseCommitCount();
                        javaFile.increaseTouchedLOC(added, deleted);
                        javaFile.addAuthor(author);
                        javaFile.addAddedCount(added);
                        javaFile.addChurnCount(added, deleted);
                        javaFile.addChgSetSize(chgSetSize);
                    }
                }
            }
        }
        LOGGER.log(Level.INFO, "Classes data calculated");
    }

    public Release getReleaseFromDate(String date) {
        Release rel;
        Release actual;
        rel = null;

        LocalDateTime ldt = LocalDate.parse(date).atStartOfDay();

        //return the first release with first date after given date
        for (Release release : this.releases) {
            actual = release;
            if (ldt.isBefore(actual.getReleaseDate())) {
                return actual;
            }
        }

        //if no release match get the first unreleased or null
        if(!this.unreleased.isEmpty())
            rel = this.unreleased.get(0);

        return rel;
    }

    public Release getReleaseByJiraName(String jiraName) {
        Release rel;
        rel = this.releases.stream().filter(release -> jiraName.equals(release.getJiraName())).findAny().orElse(null);
        if(rel == null)
            rel = this.unreleased.stream().filter(release -> jiraName.equals(release.getJiraName())).findAny().orElse(null);
        return rel;
    }

    public List<Release> getReleases() {
        return this.releases;
    }

    public List<Release> getUnreleased() {
        return this.unreleased;
    }

    public Release getLastReleaseConsidered() {
        return this.releaseSubset.get(this.releaseSubset.size()-1);
    }

    public List<Release> getReleaseSubset() { return this.releaseSubset; }

    public List<JavaFile> getDataList(){

        List<JavaFile> finalList = new ArrayList<>();
        List<JavaFile> relList;


        for(Release rel:this.releaseSubset) {
            relList = rel.getJavaFiles();
            relList.sort((JavaFile jf1, JavaFile jf2) -> jf1.getName().compareTo(jf2.getName()));
            finalList.addAll(relList);
        }

        return finalList;
    }
}
