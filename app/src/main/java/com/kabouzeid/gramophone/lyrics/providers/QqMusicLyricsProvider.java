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

import okhttp3.HttpUrl;

public class QqMusicLyricsProvider implements LyricsProvider {
    private static final String NAME = "QQ Music";
    private static final int MAX_RESULTS = 8;

    @NonNull
    @Override
    public String getName() {
        return NAME;
    }

    @NonNull
    @Override
    public List<LyricsSearchResult> search(@NonNull LyricsSearchQuery query) throws IOException {
        JsonObject request = new JsonObject();
        JsonObject comm = new JsonObject();
        comm.addProperty("ct", 24);
        comm.addProperty("cv", 0);
        request.add("comm", comm);

        JsonObject param = new JsonObject();
        param.addProperty("remoteplace", "txt.yqq.top");
        param.addProperty("search_type", 0);
        param.addProperty("query", query.keyword());
        param.addProperty("page_num", 1);
        param.addProperty("num_per_page", MAX_RESULTS);

        JsonObject req = new JsonObject();
        req.addProperty("module", "music.search.SearchCgiService");
        req.addProperty("method", "DoSearchForQQMusicDesktop");
        req.add("param", param);
        request.add("req_1", req);

        JsonObject json = LyricsHttpClient.postJson(
                "https://u.y.qq.com/cgi-bin/musicu.fcg",
                request.toString(),
                commonHeaders()
        ).getAsJsonObject();
        JsonArray songs = array(object(object(object(object(json, "req_1"), "data"), "body"), "song"), "list");
        List<LyricsSearchResult> results = new ArrayList<>();
        for (int i = 0; i < songs.size() && results.size() < MAX_RESULTS; i++) {
            JsonObject song = songs.get(i).getAsJsonObject();
            String mid = firstNonEmpty(string(song, "mid"), string(song, "songmid"));
            if (mid.isEmpty()) {
                continue;
            }
            String lyrics = fetchLyrics(mid);
            if (lyrics.trim().isEmpty()) {
                continue;
            }
            String title = firstNonEmpty(string(song, "title"), string(song, "name"));
            String artist = singers(array(song, "singer"));
            String album = string(object(song, "album"), "name");
            long duration = number(song, "interval") * 1000L;
            int score = LyricsMatcher.score(query, title, artist, album, duration);
            results.add(new LyricsSearchResult(NAME, title, artist, album, duration, lyrics, score));
        }
        return results;
    }

    @NonNull
    private String fetchLyrics(@NonNull String songMid) throws IOException {
        HttpUrl url = HttpUrl.parse("https://c.y.qq.com/lyric/fcgi-bin/fcg_query_lyric_new.fcg")
                .newBuilder()
                .addQueryParameter("songmid", songMid)
                .addQueryParameter("format", "json")
                .addQueryParameter("nobase64", "1")
                .addQueryParameter("g_tk", "5381")
                .build();
        JsonObject json = LyricsHttpClient.getJson(url.toString(), commonHeaders()).getAsJsonObject();
        return string(json, "lyric");
    }

    @NonNull
    private static Map<String, String> commonHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Referer", "https://y.qq.com/portal/player.html");
        headers.put("Origin", "https://y.qq.com");
        return headers;
    }

    @NonNull
    private static String singers(@NonNull JsonArray singers) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < singers.size(); i++) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(string(singers.get(i).getAsJsonObject(), "name"));
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

    @NonNull
    private static String firstNonEmpty(@NonNull String first, @NonNull String second) {
        return first.trim().isEmpty() ? second.trim() : first.trim();
    }
}
