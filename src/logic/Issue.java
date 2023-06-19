package logic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Issue {

    private String id;
    private Integer index;
    private String key;
    private Release fixVersion;
    private Release injectedVersion;
    private Release openingVersion;
    private List<Commit> commitList;
    private List<String> touchedFiles;

    public  enum Status {
        NULL_VERSION,
        INCONSISTENT,
        NULL_EMPTY,
        IV_IS_FV,
        VALID,
        AFTER_LAST_RELEASE,
        EMPTY_TOUCHED_FILES
    }


    public Issue(String id, String key, Release injectedVersion, Release fixVersion, Release openingVersion) {
        this.id = id;
        this.key = key;
        this.injectedVersion = injectedVersion;
        this.fixVersion = fixVersion;
        this.openingVersion = openingVersion;
        this.commitList = new ArrayList<>();
        this.index = Integer.parseInt(this.key.split("-")[1]);
        this.touchedFiles = new ArrayList<>();
    }

    private void retrieveTouchedFiles() {
        for(Commit commit: this.commitList){
            // only file name needed
            for(String fileName: commit.getTouchedFilesNames()){
                // avoid repetitions
                if(!this.touchedFiles.contains(fileName))
                    this.touchedFiles.add(fileName);
            }
        }
    }

    public Integer getIndex(){ return this.index; }

    private boolean isConsistent(){
        boolean valid = true;
        int openingVersionIndex = this.openingVersion.getReleaseIndex();
        int fixVersionIndex = this.fixVersion.getReleaseIndex();

        //filter inconsistent data
        if(injectedVersion != null){
            int injectedVersionIndex = this.injectedVersion.getReleaseIndex();
            if( injectedVersionIndex > openingVersionIndex || injectedVersionIndex > fixVersionIndex || openingVersionIndex > fixVersionIndex)
                valid = false;
        }else{
            if(openingVersionIndex > fixVersionIndex)
                valid = false;
        }
        return valid;
    }

    public void setCommitList(List<Commit> commitList, GitBoundary gitBoundary) throws IOException, InterruptedException {
        this.commitList = commitList;
        List<CommitFileData> dataList;
        //retrieve commit data
        for(Commit commit: this.commitList){
            dataList = gitBoundary.getCommitData(commit.getSha());
            if (!dataList.isEmpty()) {
                commit.setTouchedFiles(dataList);
            }
        }
        this.retrieveTouchedFiles();
    }

    private boolean isVersionNull(){
        return fixVersion == null || openingVersion == null;
    }


    public Status validateIssue() {
        // check if opening version or fix version are null
        if(this.isVersionNull()) return Status.NULL_VERSION;
        // check versions consistency
        if(!this.isConsistent()) return Status.INCONSISTENT;
        // check injected version is null and no commits
        if(this.injectedVersion == null && this.commitList.isEmpty()) return Status.NULL_EMPTY;
        if(this.injectedVersion != null && this.injectedVersion.getReleaseIndex() == this.fixVersion.getReleaseIndex()) return Status.IV_IS_FV;
        // all check passed
        return Status.VALID;
    }

    public Status validateIssueAfterProportion(Release lastRelease) {
        // check versions consistency
        if(!this.isConsistent()) return Status.INCONSISTENT;
        // check iv= fv
        if(this.injectedVersion.getReleaseIndex() == this.fixVersion.getReleaseIndex()) return Status.IV_IS_FV;
        // check empty touched files
        if(this.touchedFiles.isEmpty()) return Status.EMPTY_TOUCHED_FILES;
        // check if injected is after last release considered
        if(this.injectedVersion.getReleaseIndex() > lastRelease.getReleaseIndex()) return Status.AFTER_LAST_RELEASE;
        // all check passed
        return Status.VALID;
    }

    public Release getFixVersion() {
        return this.fixVersion;
    }

    public Release getInjectedVersion() {
        return this.injectedVersion;
    }

    public Release getOpeningVersion() {
        return this.openingVersion;
    }

    public void setInjectedVersion(Release injectedVersion) {
        this.injectedVersion = injectedVersion;
    }

    public List<String> getTouchedFiles() { return this.touchedFiles; }

    public String getId() {
        return id;
    }
}
