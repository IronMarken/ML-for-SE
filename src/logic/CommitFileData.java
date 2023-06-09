package logic;

public class CommitFileData {

    private String name;
    private Integer added;
    private Integer deleted;
    private Integer chgSetSize;

    public CommitFileData(String name, int added, int deleted) {
        this.name = name;
        this.added = added;
        this.deleted = deleted;
        this.chgSetSize = null;
    }

    public String getName() { return this.name; }

    public Integer getAdded() { return this.added; }

    public Integer getDeleted() { return this.deleted; }

    public void setChgSetSize(Integer chgSetSize) { this.chgSetSize = chgSetSize; }
    public Integer getChgSetSize() {
        if(this.chgSetSize == null)
            return 0;
        else
            return this.chgSetSize;
    }

}
