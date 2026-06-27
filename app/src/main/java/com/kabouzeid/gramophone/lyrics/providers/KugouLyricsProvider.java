package com.kabouzeid.gramophone.lyrics.providers;

import android.util.Base64;

import androidx.annotation.NonNull;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kabouzeid.gramophone.lyrics.LyricsMatcher;
import com.kabouzeid.gramophone.lyrics.LyricsProvider;
import com.kabouzeid.gramophone.lyrics.LyricsSearchQuery;
import com.kabouzeid.gramophone.lyrics.LyricsSearchResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import okhttp3.HttpUrl;

public class KugouLyricsProvider implements LyricsProvider {
    private static final String NAME = "KuGou";
    private static final int MAX_RESULTS = 8;

    @NonNull
    @Override
    public String getName() {
        return NAME;
    }

    @NonNull
    @Override
    public List<LyricsSearchResult> search(@NonNull LyricsSearchQuery query) throws IOException {
        HttpUrl url = HttpUrl.parse("https://lyrics.kugou.com/search").newBuilder()
                .addQueryParameter("ver", "1")
                .addQueryParameter("man", "yes")
                .addQueryParameter("client", "pc")
                .addQueryParameter("keyword", query.keyword())
                .addQueryParameter("duration", String.valueOf(query.durationMillis))
                .addQueryParameter("hash", "")
                .build();
        JsonObject json = LyricsHttpClient.getJson(url.toString(), null).getAsJsonObject();
        JsonArray candidates = array(json, "candidates");
        List<LyricsSearchResult> results = new ArrayList<>();
        for (int i = 0; i < candidates.size() && results.size() < MAX_RESULTS; i++) {
            JsonObject item = candidates.get(i).getAsJsonObject();
            String id = string(item, "id");
            String accessKey = string(item, "accesskey");
            if (id.isEmpty() || accessKey.isEmpty()) {
                continue;
            }
            String lyrics = download(id, accessKey);
            if (lyrics.trim().isEmpty()) {
                continue;
            }
            String title = string(item, "song");
            String artist = string(item, "singer");
            long duration = number(item, "duration");
            int score = LyricsMatcher.score(query, title, artist, "", duration);
            results.add(new LyricsSearchResult(NAME, title, artist, "", duration, lyrics, score));
        }
        return results;
    }

    @NonNull
    private String download(@NonNull String id, @NonNull String accessKey) throws IOException {
        HttpUrl url = HttpUrl.parse("https://lyrics.kugou.com/download").newBuilder()
                .addQueryParameter("ver", "1")
                .addQueryParameter("client", "pc")
                .addQueryParameter("id", id)
                .addQueryParameter("accesskey", accessKey)
                .addQueryParameter("fmt", "lrc")
                .addQueryParameter("charset", "utf8")
                .build();
        JsonObject json = LyricsHttpClient.getJson(url.toString(), null).getAsJsonObject();
        String content = string(json, "content");
        if (content.trim().isEmpty()) {
            return "";
        }
        try {
            return new String(Base64.decode(content, Base64.DEFAULT), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return content;
        }
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
