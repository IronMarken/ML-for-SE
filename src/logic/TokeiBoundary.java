package logic;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class TokeiBoundary {

    private TokeiBoundary(){}

    public static List<Integer> getSizes(String filePath, File workingCopy) throws IOException, InterruptedException, JSONException {
        //[0] code lines
        //[1] comments lines
        List<Integer> sizes = new ArrayList<>();

        Process process = Runtime.getRuntime().exec(new String[] {"tokei", filePath , "-o", "json"}, null, workingCopy);
        BufferedReader reader = new BufferedReader (new InputStreamReader(process.getInputStream()));
        process.waitFor();


        StringBuilder builder = new StringBuilder();
        String line;

        // convert output as string
        while ( (line = reader.readLine()) != null) {
            builder.append(line);
            builder.append(System.getProperty("line.separator"));
        }

        String jsonString = builder.toString();

        //convert string into json object
        JSONObject jsonObject = new JSONObject(jsonString);

        //get fields 'code' and 'comments'
        jsonObject = jsonObject.getJSONObject("Java");
        sizes.add(jsonObject.getInt("code"));
        sizes.add(jsonObject.getInt("comments"));

        return sizes;
    }

}
