package logic;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
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
    //number of fix
    private int nFix;
    private boolean buggy;


    public JavaFile(String className, int releaseIndex, LocalDateTime creationDate) {
        this.className = className;
        this.releaseIndex = releaseIndex;
        this.creationDate = creationDate;
        this.touchedLOC = 0;
        this.commitCount = 0;
        this.size = 0;
        this.comments = 0;
        this.nFix = 0;
        this.buggy = false;


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

    public String getName() {
        return this.className;
    }

    public void increaseCommitCount() {
        this.commitCount++;
    }

    public void increaseTouchedLOC(long added, long deleted) {
        this.touchedLOC = this.touchedLOC + added + deleted;
    }

    public void addAuthor(String authorName) {
        //add only new names
        if (!this.authorList.contains(authorName))
            this.authorList.add(authorName);
    }

    public void addAddedCount(Integer count) {
        this.addedList.add(count);
    }

    public void addChurnCount(Integer added, Integer deleted) {
        this.churnList.add(added - deleted);
    }

    public void addChgSetSize(Integer count) {
        this.chgSetSizeList.add(count);
    }

    public void setBuggy() {
        this.buggy = true;
        this.nFix++;
    }

    public int getReleaseIndex() {
        return releaseIndex;
    }

    public int getSize() {
        return size;
    }

    public float getCommentsPercentage() {
        return commentsPercentage;
    }

    public long getTouchedLOC() {
        return touchedLOC;
    }

    public int getCommitCount() {
        return commitCount;
    }

    public int getAuthorCount() {
        return this.authorList.size();
    }

    public int getTotalAddedLOC() {
        if(this.addedList.isEmpty())
            return 0;
        else
            return this.addedList.stream().mapToInt(Integer::intValue).sum();
    }

    public Integer getMaxAddedLOC() {
        if(this.addedList.isEmpty())
            return 0;
        else
            return Collections.max(this.addedList);
    }

    public double getAvgAddedLOC() {
        if(this.addedList.isEmpty())
            return 0;
        else
            return this.addedList.stream().mapToDouble(a->a).average().getAsDouble();
    }

    public Integer getTotalChurn() {
        if(this.churnList.isEmpty())
            return 0;
        else
            return this.churnList.stream().mapToInt(Integer::intValue).sum();
    }

    public Integer getMaxChurn() {
        if(this.churnList.isEmpty())
            return 0;
        else
            return Collections.max(this.churnList);
    }

    public double getAvgChurn() {
        if(this.churnList.isEmpty())
            return 0;
        else
            return this.churnList.stream().mapToDouble(a->a).average().getAsDouble();
    }

    public Integer getTotalChgSetSize() {
        if(this.chgSetSizeList.isEmpty())
            return 0;
        else
            return this.chgSetSizeList.stream().mapToInt(Integer::intValue).sum();
    }

    public Integer getMaxChgSetSize() {
        if(this.chgSetSizeList.isEmpty())
            return 0;
        else
            return Collections.max(this.chgSetSizeList);
    }

    public double getAvgChgSetSize() {
        if(this.chgSetSizeList.isEmpty())
            return 0;
        else
            return this.chgSetSizeList.stream().mapToDouble(a->a).average().getAsDouble();
    }

    public long getAge() {
        return this.age;
    }

    public long getWeightedAge() {
        return this.age * this.touchedLOC;
    }

    public int getNFix() {
        return nFix;
    }

    public Boolean isBuggy() {
        return this.buggy;
    }


}
