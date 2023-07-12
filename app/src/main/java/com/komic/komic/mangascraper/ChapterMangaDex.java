package com.komic.komic.mangascraper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ChapterMangaDex extends Chapter {
    public static final String SEP = "###";
    public static final String URL_TEMPLATE = "https://api.mangadex.org/at-home/server/%s";
    public static final String IMAGE_URL_TEMPLATE = "https://uploads.mangadex.org/data/%s/%s";

    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new GsonBuilder().create();

    public ChapterMangaDex(String id, String name) {
        super(id, name);
    }

    private static String getPageImageUrl(String hash, String imageId) {
        if (imageId.startsWith("\"") && imageId.endsWith("\"")) {
            imageId = imageId.substring(1, imageId.length() - 1);
        }
        return String.format(IMAGE_URL_TEMPLATE, hash, imageId);
    }

    @Override
    protected String[] inflateChapterPages() {
        String url = String.format(URL_TEMPLATE, id);
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response);
            }
            // Parse the response body using Gson
            String responseBody = response.body().string();
            JsonObject json = gson.fromJson(responseBody, JsonElement.class).getAsJsonObject();
            JsonObject chapterJson = json.get("chapter").getAsJsonObject();

            // Extract pages from raw jsonElement
            JsonArray data = chapterJson.getAsJsonArray("data");
            String hash = chapterJson.get("hash").getAsString();
            List<String> images = new ArrayList<String>();
            data.forEach(s -> {
                images.add(getPageImageUrl(hash, s.toString()));
            });
            return images.toArray(new String[0]);
        } catch (IOException e) {
            // Handle any exceptions that occur during the API call
            e.printStackTrace();
            return null;
        }
    }


    public static ChapterMangaDex fromString(String data) {
        String[] args = data.split(SEP);
        return new ChapterMangaDex(args[0], args[1]);
    }

    @Override
    public String toString() {
        String[] data = {id, name};
        String joined = String.join(SEP, data);
        return joined;
    }
}

