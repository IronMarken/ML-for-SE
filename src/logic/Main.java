package logic;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {

    public static void main(String[] args) throws IOException, JSONException {
        List<String> urlList = new ArrayList<>(Arrays.asList("https://github.com/apache/avro", "https://github.com/apache/bookkeeper"));
        GitBoundary gb;
        ReleaseManager rm;
        ReleaseNameAdapter rna;
        String[] splitted;
        String projName;

        for (String gitUrl:urlList) {

            splitted = gitUrl.split("/");
            projName = splitted[splitted.length -1];

            gb = new GitBoundary(gitUrl);
            rna = new ReleaseNameAdapter(0, "release-");
            rm = new ReleaseManager(projName, gb, rna);

            rm.setupReleaseManager();
            //rm.printDebugReleaseLists();




        }
    }
}