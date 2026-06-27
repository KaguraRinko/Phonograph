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
import java.util.List;

import okhttp3.HttpUrl;

public class LrclibLyricsProvider implements LyricsProvider {
    private static final String NAME = "LRCLIB";
    private static final int MAX_RESULTS = 8;

    @NonNull
    @Override
    public String getName() {
        return NAME;
    }

    @NonNull
    @Override
    public List<LyricsSearchResult> search(@NonNull LyricsSearchQuery query) throws IOException {
        HttpUrl.Builder url = HttpUrl.parse("https://lrclib.net/api/search").newBuilder()
                .addQueryParameter("track_name", query.title)
                .addQueryParameter("artist_name", query.artist);
        if (!query.album.isEmpty()) {
            url.addQueryParameter("album_name", query.album);
        }
        if (query.durationMillis > 0) {
            url.addQueryParameter("duration", String.valueOf(Math.round(query.durationMillis / 1000f)));
        }

        JsonElement json = LyricsHttpClient.getJson(url.build().toString(), null);
        JsonArray array = json.isJsonArray() ? json.getAsJsonArray() : new JsonArray();
        List<LyricsSearchResult> results = new ArrayList<>();
        for (int i = 0; i < array.size() && results.size() < MAX_RESULTS; i++) {
            JsonObject item = array.get(i).getAsJsonObject();
            String lyrics = firstNonEmpty(string(item, "syncedLyrics"), string(item, "plainLyrics"));
            if (lyrics.isEmpty()) {
                continue;
            }
            String title = firstNonEmpty(string(item, "trackName"), string(item, "name"));
            String artist = string(item, "artistName");
            String album = string(item, "albumName");
            long duration = Math.round(number(item, "duration") * 1000);
            int score = LyricsMatcher.score(query, title, artist, album, duration);
            results.add(new LyricsSearchResult(NAME, title, artist, album, duration, lyrics, score));
        }
        return results;
    }

    @NonNull
    private static String string(@NonNull JsonObject object, @NonNull String key) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? "" : value.getAsString();
    }

    private static double number(@NonNull JsonObject object, @NonNull String key) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? 0 : value.getAsDouble();
    }

    @NonNull
    private static String firstNonEmpty(@NonNull String first, @NonNull String second) {
        return first.trim().isEmpty() ? second.trim() : first.trim();
    }
}
