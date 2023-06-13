package logic;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitBoundary {

    private static final Logger LOGGER = Logger.getLogger(GitBoundary.class.getName());
    private static final String DATE_FORMAT = "--date=iso";
    private static final String DATE = "--pretty=format:%cd";
    private static final String FILE_EXT = ".java";
    private static final String ALL_OPT = "--all";
    private static final String NO_MERGE_OPT = "--no-merges";
    private static final String COMMIT_FORMAT = "--pretty=format:%H---%s---%an---%cd---";
    private final String projectName;
    private final File workingCopy;



    public GitBoundary(String gitUrl ) throws IOException, InterruptedException {

        //parse project name
        String[] splitted = gitUrl.split("/");
        this.projectName = splitted[splitted.length-1];

        Process process;
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
            process = Runtime.getRuntime().exec(new String[] {"git", "clone", gitUrl}, null, new File("repo"));
            process.waitFor();
            LOGGER.log(Level.INFO, "Project cloned");
        } else {
            //pull
            LOGGER.log(Level.INFO, "Project exists pulling it please wait...");
            process = Runtime.getRuntime().exec(new String[] {"git", "pull"}, null, this.workingCopy);
            process.waitFor();
            LOGGER.log(Level.INFO, "Pull terminated");
        }
    }


    public LocalDateTime getDate(String name, boolean isRelease) throws IOException, InterruptedException {
        Process process;

        if(isRelease)
            process = Runtime.getRuntime().exec(new String[] {"git", "log", name, "-1", DATE ,DATE_FORMAT }, null, this.workingCopy);
        else
            process = Runtime.getRuntime().exec(new String[] {"git", "log", "--diff-filter=A", DATE ,DATE_FORMAT, "--",name }, null, this.workingCopy);
        BufferedReader reader = new BufferedReader (new InputStreamReader (process.getInputStream()));
        process.waitFor();

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

    public List<String> getReleaseClasses(String gitName) throws IOException, InterruptedException {
        List<String> classes = new ArrayList<>();

        Process process = Runtime.getRuntime().exec(new String[] {"git", "ls-tree", "-r", gitName, "--name-only"}, null, this.workingCopy);
        BufferedReader reader = new BufferedReader (new InputStreamReader (process.getInputStream()));

        process.waitFor();

        String line;
        String className = null;

        while((line = reader.readLine()) != null) {
            className = line;

            //remove last \n
            className = className.split("\n")[0];
            if(className.endsWith(FILE_EXT))
                classes.add(className);
        }
        Collections.sort(classes);
        return classes;
    }

    public List<Commit> getReleaseCommits(LocalDateTime afterDate, LocalDateTime beforeDate) throws IOException, InterruptedException {

        List<Commit> commits = new ArrayList<>();
        //managing commits with same date of the release

        LocalDateTime before = beforeDate.plusDays(1);

        String beforeString = "--before="+before.getYear()+"-"+before.getMonthValue()+"-"+before.getDayOfMonth();
        Process process;
        //after = null for first release
        if(afterDate != null) {
            LocalDateTime after = afterDate.plusDays(1);

            String afterString = "--after="+after.getYear()+"-"+after.getMonthValue()+"-"+after.getDayOfMonth();
            process = Runtime.getRuntime().exec(new String[] {"git", "log", ALL_OPT, NO_MERGE_OPT,beforeString, afterString, COMMIT_FORMAT, DATE_FORMAT}, null, this.workingCopy);
        }else {
            process = Runtime.getRuntime().exec(new String[] {"git", "log", ALL_OPT, NO_MERGE_OPT, beforeString, COMMIT_FORMAT, DATE_FORMAT}, null, this.workingCopy);
        }

        process.waitFor();

        BufferedReader reader = new BufferedReader (new InputStreamReader (process.getInputStream()));
        String line;
        String[] splitted;

        String sha;
        String message;
        String author;
        String date;
        Commit commit;

        while((line = reader.readLine()) != null) {
            if(!line.isEmpty()) {
                splitted = line.split("---");
                sha = splitted[0];
                message = splitted[1];
                author = splitted[2];
                //get only date
                date = splitted[3].split(" ")[0];

                commit = new Commit(sha, message, author, date);
                commits.add(commit);

            }
        }
        //order by date
        commits.sort((Commit c1, Commit c2) -> c1.getDate().compareTo(c2.getDate()));
        return commits;
    }

    public List<CommitFileData> getCommitData(String sha) throws IOException, InterruptedException {
        List<CommitFileData> dataList = new ArrayList<>();

        Process process = Runtime.getRuntime().exec(new String[] {"git", "show", "--numstat" ,"--format=",sha,"-1"}, null, this.workingCopy);
        BufferedReader reader = new BufferedReader (new InputStreamReader (process.getInputStream()));

        process.waitFor();

        CommitFileData dataFile;
        int added;
        int deleted;
        String name;
        int chgSetSize;

        String line;
        String [] splitted;

        chgSetSize = 0;

        while((line = reader.readLine()) != null) {
            if(line.endsWith(FILE_EXT)) {
                chgSetSize ++;
                splitted = line.split("\t");
                added = Integer.parseInt(splitted[0]);
                deleted = Integer.parseInt(splitted[1]);
                name = splitted[2];

                dataFile = new CommitFileData(name, added, deleted);
                dataList.add(dataFile);
            }
        }

        for(CommitFileData touchedFile: dataList) {
            //don't count file itself
            touchedFile.setChgSetSize(chgSetSize-1);
        }

        dataList.sort((CommitFileData df1, CommitFileData df2) -> df1.getName().compareTo(df2.getName()));
        return dataList;
    }

    public void changeRelease(String releaseName) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec(new String[] {"git","checkout",releaseName}, null, this.workingCopy);
        process.waitFor();
    }

    public void restoreLastRelease() throws IOException, InterruptedException {
        this.changeRelease("master");
    }

    public List<Integer> getSizes(String releaseName, String filePath) throws IOException, InterruptedException, JSONException {
        this.changeRelease(releaseName);
        List<Integer> returnSizes = TokeiBoundary.getSizes(filePath, this.workingCopy);
        this.restoreLastRelease();
        return returnSizes;
    }


    public File getWorkingCopy() { return this.workingCopy;}

    public List<Commit> getIssueCommit(Issue issue) throws IOException {

        String line;
        String[] splitted;

        String sha;
        String message;
        String author;
        String date;
        Commit commit;

        Pattern pattern;
        Matcher matcher;

        List<Commit> commits = new ArrayList<>();

        Process process = Runtime.getRuntime().exec(new String[] {"git", "log",COMMIT_FORMAT,NO_MERGE_OPT, ALL_OPT,DATE_FORMAT}, null, this.workingCopy);
        BufferedReader reader = new BufferedReader (new InputStreamReader (process.getInputStream()));

        while((line = reader.readLine()) != null) {
            if(!line.isEmpty()) {
                splitted = line.split("---");
                sha = splitted[0];
                message = splitted[1];
                author = splitted[2];
                //get only date
                date = splitted[3].split(" ")[0];

                //regular expression for matching
                pattern = Pattern.compile("(ISSUE|"+this.projectName.toUpperCase()+")(-| #)"+issue.getIndex()+"(:|\\.)",Pattern.CASE_INSENSITIVE);
                matcher = pattern.matcher(message);

                if(matcher.find()) {
                    commit = new Commit(sha, message, author, date);
                    commits.add(commit);
                }
            }
        }
        //order by date
        commits.sort((Commit c1, Commit c2) -> c1.getDate().compareTo(c2.getDate()));
        return commits;

    }
}
