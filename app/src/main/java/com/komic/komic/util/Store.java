package com.komic.komic.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.komic.komic.mangascraper.Manga;
import com.komic.komic.mangascraper.MangaMangaDex;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Store {
    private final Context context;
    private SharedPreferences prefs;

    public java.util.HashMap<String, MangaPreference> mangaPref; // <String, MangaPreference>
    public LinkedList<String> mangaHistory;
    public boolean initiated = false;

    private static Store instance;


    private Store(Context context) {
        this.context = context;
        init();
    }

    public static synchronized Store getInstance(Context context) {
        if (instance == null) {
            instance = new Store(context);
        }
        return instance;
    }

    private void init() {
        if (initiated) return;
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadMangaPrefs();
        loadMangaHistory();
        initiated = true;
    }

    public java.util.HashMap<String, MangaPreference> loadMangaPrefs() {
        String json = prefs.getString(JSON_PREFIX + MANGA_PREF_KEY, null);
        if (json == null) {
            mangaPref = new java.util.HashMap<String, MangaPreference>();
            return mangaPref;
        }
        Gson gson = new Gson();
        java.lang.reflect.Type type = com.google.gson.reflect.TypeToken.getParameterized(HashMap.class, String.class, MangaPreference.class).getType();
        mangaPref = gson.fromJson(json, type);
        return mangaPref;
    }

    public LinkedList<String> loadMangaHistory() {
        String json = prefs.getString(JSON_PREFIX + MANGA_HISTORY_KEY, null);
        if (json == null) {
            mangaHistory = new LinkedList<String>();
            return mangaHistory;
        }
        Gson gson = new Gson();
        mangaHistory = gson.fromJson(json, LinkedList.class);
        return mangaHistory;
    }

    public void addMangaHistory(String mangaId, String chapterStr, String mangaInStr) {
        updateMangaPref(mangaId, "lastReadChapter", chapterStr);
        if (mangaHistory.contains(mangaInStr)) return;
        if (mangaHistory.size() >= 15) {
            mangaHistory.removeLast();
        }
        mangaHistory.addFirst(mangaInStr);
        saveJson(context, MANGA_HISTORY_KEY, mangaHistory);
    }

    public MangaPreference getMangaPref(String id) {
        return mangaPref.getOrDefault(id, null);
    }


    public List<Manga> getMangaHistory() {
        LinkedList<Manga> mangaList = new LinkedList<>();
        for (String mangaStr : mangaHistory) {
            try {
                mangaList.addLast(MangaMangaDex.fromString(mangaStr));
            } catch (Exception e) {
                System.out.println(e);
            }
        }
        return mangaList;
    }


    public void updateMangaPref(String id, String key, boolean value, String mangaStr) {
        MangaPreference p;
        if (mangaPref.containsKey(id)) {
            p = mangaPref.get(id);
        } else {
            p = new MangaPreference();
            mangaPref.put(id, p);
        }
        switch (key) {
            case "bookmarked":
                p.bookmarked = value;
                break;
            case "downloaded":
                p.downloaded = value;
                break;
        }
        if (p.mangaStr == null)
            p.mangaStr = mangaStr;
        saveJson(context, MANGA_PREF_KEY, mangaPref);
    }

    public void updateMangaPref(String id, String key, String chapterId) {
        MangaPreference p;
        if (mangaPref.containsKey(id)) {
            p = mangaPref.get(id);
        } else {
            p = new MangaPreference();
            mangaPref.put(id, p);
        }
        p.lastReadChapter = chapterId;
        saveJson(context, MANGA_PREF_KEY, mangaPref);
    }

    public List<Manga> getBookmarkedManga() {
        LinkedList<Manga> mangaList = new LinkedList<>();
        for (Map.Entry<String, MangaPreference> entry : mangaPref.entrySet()) {
            String key = entry.getKey();
            MangaPreference p = entry.getValue();
            if (p.bookmarked) {
                try {
                    mangaList.addLast(MangaMangaDex.fromString(p.mangaStr));
                } catch (Exception e) {
                    System.out.println(e);
                }
            }
        }
        return mangaList;
    }

    private static final String PREFS_NAME = "KomicPrefs";
    private static final String JSON_PREFIX = "json_";
    private static final String MANGA_PREF_KEY = "manga_pref";
    private static final String MANGA_HISTORY_KEY = "manga_history";

    public static class MangaPreference {
        public boolean bookmarked = false;
        public boolean downloaded = false;
        public String lastReadChapter = null;
        public String mangaStr = null;

        @NonNull
        @Override
        public String toString() {
            return "MangaPreference{" +
                    "bookmarked='" + bookmarked + '\'' +
                    ", downloaded='" + downloaded + '\'' +
                    ", lastReadChapter='" + lastReadChapter + '\'' +
                    '}';
        }
    }

    public static void saveJson(Context context, String key, Object value) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        Gson gson = new Gson();
        String json = gson.toJson(value);

        editor.putString(JSON_PREFIX + key, json);
        editor.apply();
    }
}
