package main.Util; /**
 * Created by shuruiz on 4/7/17.
 */


import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class JsonUtility {
    static List<String> token;


    public static String json = "{ \"id\": 01, \"language\": \"Java\", \"edition\": \"third\", \"author\": \"Herbert Schildt\", \"chapters\": [\"chapter 1\",\"chapter 2\",\"chapter 3\"] }";

    public void readJson(String json) {
        JSONObject jsonObj = new JSONObject(json);

        String id = jsonObj.getString("id");
        System.out.println(id);

        String language = jsonObj.getString("language");
        System.out.println(language);

        String edition = jsonObj.getString("edition");
        System.out.println(edition);

        String author = jsonObj.getString("author");
        System.out.println(author);

        JSONArray chapters = (JSONArray) jsonObj.get("chapters");
        Iterator<Object> iterator = chapters.iterator();
        while (iterator.hasNext()) {
            System.out.println(iterator.next());
        }
    }

    public static ArrayList<String> readUrl(String urlString) {

        try {
            TimeUnit.SECONDS.sleep(4);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(urlString);
        ArrayList<String> json_block_array = new ArrayList<>();
        BufferedReader reader = null;
        try {
            URL url = new URL(urlString);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.connect();
            if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream urlContent = url.openStream();
                reader = new BufferedReader(new InputStreamReader(url.openStream()));
                StringBuffer buffer = new StringBuffer();
                int read;
                char[] chars = new char[1024];
                while ((read = reader.read(chars)) != -1)
                    buffer.append(chars, 0, read);

                String json_string = buffer.toString();
                if (!json_string.equals("[]")) {
                    if (json_string.startsWith("[")) {
                        if (json_string.contains(",{")) {
                            JSONArray jsonArray = new JSONArray(json_string);

                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObj = jsonArray.getJSONObject(i);
                                json_block_array.add(String.valueOf(jsonObj));
                            }
                        } else {
                            json_block_array.add(json_string.substring(1, json_string.lastIndexOf("]")));
                        }
                    } else {
                        json_block_array.add(json_string);
                    }
                }
                return json_block_array;
            } else {


                return new ArrayList<>();
            }


        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null)
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        return new ArrayList<>();
    }


    public static void main(String args[]) {
        JsonUtility utility = new JsonUtility();
        ArrayList<String> json_block_array = new ArrayList<>();
        IO_Process io = new IO_Process();
        try {
            String json_string = io.readResult("/Users/shuruiz/Box Sync/queryGithub/timscaffidi/ofxVideoRecorder/pr.txt");
            if (!json_string.equals("[]")) {
                if (json_string.startsWith("[")) {
                    if (json_string.contains(",{")) {
                        JSONArray jsonArray = new JSONArray(json_string);

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObj = jsonArray.getJSONObject(i);
                            json_block_array.add(String.valueOf(jsonObj));
                        }
                    } else {
                        json_block_array.add(json_string.substring(1, json_string.lastIndexOf("]")));
                    }
                } else {
                    json_block_array.add(json_string);
                }
            }

//          JSONObject iss_jsonObj = new JSONObject(pr);
//          JSONArray items_json = iss_jsonObj.getJSONArray("url");
            System.out.println();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}