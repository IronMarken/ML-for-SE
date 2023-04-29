package logic;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;

public class JiraBoundary {

    private JiraBoundary() {}

    public static JSONArray getReleases(String projectName) throws IOException, JSONException {
        String url = "https://issues.apache.org/jira/rest/api/2/project/" + projectName.toUpperCase() + "/versions";
        return JSONManager.readJsonArrayFromUrl(url);
    }
}
