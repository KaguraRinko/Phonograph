package com.kabouzeid.gramophone.lyrics.providers;

import androidx.annotation.NonNull;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kabouzeid.gramophone.lyrics.LyricsMatcher;
import com.kabouzeid.gramophone.lyrics.LyricsProvider;
import com.kabouzeid.gramophone.lyrics.LyricsSearchQuery;
import com.kabouzeid.gramophone.lyrics.LyricsSearchResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NeteaseLyricsProvider implements LyricsProvider {
    private static final String NAME = "NetEase";
    private static final int MAX_RESULTS = 8;

    @NonNull
    @Override
    public String getName() {
        return NAME;
    }

    @NonNull
    @Override
    public List<LyricsSearchResult> search(@NonNull LyricsSearchQuery query) throws IOException {
        Map<String, String> headers = commonHeaders();
        Map<String, String> form = new LinkedHashMap<>();
        form.put("s", query.keyword());
        form.put("type", "1");
        form.put("limit", String.valueOf(MAX_RESULTS));
        form.put("offset", "0");

        JsonObject json = LyricsHttpClient
                .postFormJson("https://music.163.com/api/search/get/web?csrf_token=", form, headers)
                .getAsJsonObject();
        JsonObject result = object(json, "result");
        JsonArray songs = array(result, "songs");
        List<LyricsSearchResult> results = new ArrayList<>();
        for (int i = 0; i < songs.size() && results.size() < MAX_RESULTS; i++) {
            JsonObject song = songs.get(i).getAsJsonObject();
            long id = number(song, "id");
            if (id <= 0) {
                continue;
            }
            String title = string(song, "name");
            String artist = artists(array(song, "artists"));
            String album = string(object(song, "album"), "name");
            long duration = number(song, "duration");
            String lyrics = fetchLyrics(id, headers);
            if (lyrics.trim().isEmpty()) {
                continue;
            }
            int score = LyricsMatcher.score(query, title, artist, album, duration);
            results.add(new LyricsSearchResult(NAME, title, artist, album, duration, lyrics, score));
        }
        return results;
    }

    @NonNull
    private String fetchLyrics(long id, @NonNull Map<String, String> headers) throws IOException {
        JsonObject json = LyricsHttpClient.getJson(
                "https://music.163.com/api/song/lyric?id=" + id + "&lv=1&kv=1&tv=-1",
                headers
        ).getAsJsonObject();
        return string(object(json, "lrc"), "lyric");
    }

    @NonNull
    private static Map<String, String> commonHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Referer", "https://music.163.com/");
        headers.put("Origin", "https://music.163.com");
        return headers;
    }

    @NonNull
    private static String artists(@NonNull JsonArray artists) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < artists.size(); i++) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(string(artists.get(i).getAsJsonObject(), "name"));
        }
        return builder.toString();
    }

    @NonNull
    private static JsonObject object(@NonNull JsonObject object, @NonNull String key) {
        JsonElement value = object.get(key);
        return value == null || !value.isJsonObject() ? new JsonObject() : value.getAsJsonObject();
    }

    @NonNull
    private static JsonArray array(@NonNull JsonObject object, @NonNull String key) {
        JsonElement value = object.get(key);
        return value == null || !value.isJsonArray() ? new JsonArray() : value.getAsJsonArray();
    }

    @NonNull
    private static String string(@NonNull JsonObject object, @NonNull String key) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? "" : value.getAsString();
    }

    private static long number(@NonNull JsonObject object, @NonNull String key) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? 0 : value.getAsLong();
    }
}
