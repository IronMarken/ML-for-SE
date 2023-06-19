package logic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {

    public static void main(String[] args) throws Exception {
        List<String> urlList = new ArrayList<>(Arrays.asList("https://github.com/apache/avro", "https://github.com/apache/bookkeeper"));
        GitBoundary gb;
        ReleaseManager rm;
        ReleaseNameAdapter rna;
        IssueManager im;
        String[] splitted;
        String projName;
        List<JavaFile> dataList;

        for (String gitUrl:urlList) {

            splitted = gitUrl.split("/");
            projName = splitted[splitted.length -1];

            gb = new GitBoundary(gitUrl);
            rna = new ReleaseNameAdapter(0, "release-");
            rm = new ReleaseManager(projName, gb, rna);
            im = new IssueManager(projName, rm, gb);

            rm.setupReleaseManager();

            im.setupIssues();

            dataList = rm.getDataList();

            FileManager.generateOutputDir();
            FileManager.generateDatasetCsv(projName, dataList, false);
            FileManager.generateDatasetCsv(projName, dataList, true);
        }
    }
}