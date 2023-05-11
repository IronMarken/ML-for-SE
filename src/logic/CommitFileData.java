package logic;

public class CommitFileData {

    private String name;
    private Integer added;
    private Integer deleted;

    public CommitFileData(String name, int added, int deleted) {
        this.name = name;
        this.added = added;
        this.deleted = deleted;
    }

    public String getName() { return this.name; }

}
