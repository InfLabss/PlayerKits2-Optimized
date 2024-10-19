package me.infnox.pk.managers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.infnox.pk.model.internal.UpdateCheckerResult;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class UpdateCheckerManager {

    private String currentVersion;
    private String latestVersion;
    private static final String GITHUB_API_URL = "https://api.github.com/InfLabss/PlayerKits2-Optimized/releases";

    public UpdateCheckerManager(String currentVersion) {
        this.currentVersion = currentVersion;
    }

    public UpdateCheckerResult check() {
        try {
            URL url = new URL(GITHUB_API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = connection.getInputStream();
                String response = new Scanner(inputStream, "UTF-8").useDelimiter("\\A").next();
                inputStream.close();

                JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();
                latestVersion = jsonObject.get("tag_name").getAsString().replace("v", "");

                if (!currentVersion.equals(latestVersion)) {
                    return UpdateCheckerResult.noErrors(latestVersion);
                }
            }
            return UpdateCheckerResult.noErrors(null);
        } catch (Exception ex) {
            return UpdateCheckerResult.error();
        }
    }

    public String getLatestVersion() {
        return latestVersion;
    }
}