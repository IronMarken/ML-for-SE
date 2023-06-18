package logic;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class JavaFile {

    private int releaseIndex;
    private String className;
    private LocalDateTime creationDate;

    //size without comments
    private int size;
    //comments lines
    private int comments;

    //comments percentage
    private float commentsPercentage;

    private long age;
    //LOC added + deleted
    private long touchedLOC;
    //commit count
    private int commitCount;
    //authorList for author count
    private List<String> authorList;

    //lists in order to calculate avg, max ad sum easily
    //added count
    private List<Integer> addedList;
    //added-deleted
    private List<Integer> churnList;
    //amount of files committed with
    private List<Integer> chgSetSizeList;


    public JavaFile(String className, int releaseIndex, LocalDateTime creationDate){
        this.className = className;
        this.releaseIndex = releaseIndex;
        this.creationDate = creationDate;
        this.touchedLOC = 0;
        this.commitCount = 0;
        this.size = 0;
        this.comments = 0;


        this.authorList = new ArrayList<>();
        this.addedList = new ArrayList<>();
        this.churnList = new ArrayList<>();
        this.chgSetSizeList = new ArrayList<>();
    }

    //calculate age as WEEKS
    public void execAge(LocalDateTime releaseDate) {
        this.age = ChronoUnit.WEEKS.between(this.creationDate, releaseDate);
    }

    public void setSizes(int code, int comments) {
        this.size = code;
        this.comments = comments;

        //calculate comments percentage
        this.commentsPercentage = (float) this.comments / (this.comments + this.size);
    }

    public String getName() { return this.className; }

    public void increaseCommitCount() { this.commitCount++; }

    public void increaseTouchedLOC(long added, long deleted) {
        this.touchedLOC = this.touchedLOC + added + deleted;
    }

    public void addAuthor(String authorName) {
        //add only new names
        if(!this.authorList.contains(authorName))
            this.authorList.add(authorName);
    }

    public void addAddedCount(Integer count) {
        this.addedList.add(count);
    }

    public void addChurnCount(Integer added, Integer deleted) {
        this.churnList.add(added-deleted);
    }

    public void addChgSetSize(Integer count) {
        this.chgSetSizeList.add(count);
    }
}
