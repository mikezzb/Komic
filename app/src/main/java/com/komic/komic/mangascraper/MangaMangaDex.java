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

public class MangaMangaDex extends Manga {
    public static final String SEP = "###";
    public static final String URL_TEMPLATE = "https://api.mangadex.org/manga/%s/feed?limit=96&offset=0&order[chapter]=asc&translatedLanguage[]=en";
    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new GsonBuilder().create();

    public MangaMangaDex(String id, String title, String thumbnail, String description, String author) {
        super(id, title, thumbnail, description, author);
    }

    public static MangaMangaDex fromString(String data) {
        String[] args = data.split(SEP);
        return new MangaMangaDex(args[0], args[1], args[2], args[3], args[4]);
    }

    @Override
    protected List<Chapter> inflateChapters() {
        String url = String.format(URL_TEMPLATE, id);
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response);
            }
            List<Chapter> chapterList = new ArrayList<Chapter>();

            // Parse the response body using Gson
            String responseBody = response.body().string();
            JsonObject json = gson.fromJson(responseBody, JsonElement.class).getAsJsonObject();

            // Extract Chapters from raw jsonElement
            JsonArray data = json.getAsJsonArray("data");
            for (JsonElement obj : data) {
                String chapterId = obj.getAsJsonObject().get("id").getAsString();
                JsonObject attrs = obj.getAsJsonObject().getAsJsonObject("attributes");
                String chapterName = attrs.get("chapter").getAsString();
                if (chapterName == null) {
                    chapterName = attrs.get("title").getAsString();
                }
                Chapter chapter = new ChapterMangaDex(
                        chapterId,
                        chapterName);
                chapterList.add(chapter);
            }
            return chapterList;
        } catch (IOException e) {
            // Handle any exceptions that occur during the API call
            e.printStackTrace();
            return null;
        }
    }

    // for passing in bundle

    @Override
    public String toString() {
        String[] data = {id, title, thumbnail, description, author};
        String joined = String.join(SEP, data);
        return joined;
    }
}

