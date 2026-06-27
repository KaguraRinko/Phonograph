package com.kabouzeid.gramophone.lyrics.providers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

final class LyricsHttpClient {
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.SECONDS)
            .build();

    private LyricsHttpClient() {
    }

    @NonNull
    static JsonElement getJson(@NonNull String url, @Nullable Map<String, String> headers) throws IOException {
        Request.Builder builder = new Request.Builder().url(url);
        applyHeaders(builder, headers);
        return executeJson(builder.build());
    }

    @NonNull
    static JsonElement postJson(@NonNull String url, @NonNull String body,
                                @Nullable Map<String, String> headers) throws IOException {
        Request.Builder builder = new Request.Builder()
                .url(url)
                .post(RequestBody.create(JSON_MEDIA_TYPE, body));
        applyHeaders(builder, headers);
        return executeJson(builder.build());
    }

    @NonNull
    static JsonElement postFormJson(@NonNull String url, @NonNull Map<String, String> form,
                                    @Nullable Map<String, String> headers) throws IOException {
        FormBody.Builder bodyBuilder = new FormBody.Builder();
        for (Map.Entry<String, String> entry : form.entrySet()) {
            bodyBuilder.add(entry.getKey(), entry.getValue());
        }
        Request.Builder builder = new Request.Builder().url(url).post(bodyBuilder.build());
        applyHeaders(builder, headers);
        return executeJson(builder.build());
    }

    @NonNull
    private static JsonElement executeJson(@NonNull Request request) throws IOException {
        try (Response response = CLIENT.newCall(request).execute()) {
            ResponseBody body = response.body();
            if (!response.isSuccessful() || body == null) {
                throw new IOException("Lyrics request failed: HTTP " + response.code());
            }
            return JsonParser.parseString(body.string());
        }
    }

    private static void applyHeaders(@NonNull Request.Builder builder, @Nullable Map<String, String> headers) {
        builder.header("User-Agent", "Mozilla/5.0 Phonograph");
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.header(entry.getKey(), entry.getValue());
            }
        }
    }
}
