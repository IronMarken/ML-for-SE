package logic;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class JiraBoundary {

    private JiraBoundary() {}

    public static JSONArray getReleases(String projectName) throws IOException, JSONException {
        String url = "https://issues.apache.org/jira/rest/api/2/project/" + projectName.toUpperCase() + "/versions";
        return JSONManager.readJsonArrayFromUrl(url);
    }

    public static JSONObject getIssue(String projectName, Integer startIndex) throws IOException, JSONException {

        String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"
                + projectName.toUpperCase() +"%22AND%22type%22=%22bug%22AND(%22status%22"+
                "=%22closed%22OR%22status%22=%22resolved%22)AND%22resolution%22=%22fixed%22&"+
                "fields=key,fixVersions,versions,created&startAt=" + startIndex.toString() +"&maxResults=1000";
        return JSONManager.readJsonObjectFromUrl(url);

    }
}
