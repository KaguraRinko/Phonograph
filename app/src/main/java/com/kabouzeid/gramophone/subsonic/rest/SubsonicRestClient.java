package com.kabouzeid.gramophone.subsonic.rest;

import android.content.Context;

import androidx.annotation.NonNull;

import com.kabouzeid.gramophone.subsonic.SubsonicAuth;
import com.kabouzeid.gramophone.subsonic.SubsonicException;
import com.kabouzeid.gramophone.subsonic.SubsonicServer;
import com.kabouzeid.gramophone.subsonic.SubsonicUrlUtil;
import com.kabouzeid.gramophone.subsonic.rest.model.SubsonicResponse;
import com.kabouzeid.gramophone.subsonic.rest.model.SubsonicResponseEnvelope;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SubsonicRestClient {
    private static final long CACHE_SIZE = 1024 * 1024 * 10;

    private final SubsonicServer server;
    private final SubsonicApiService apiService;

    public SubsonicRestClient(@NonNull Context context, @NonNull SubsonicServer server) {
        this(server, createDefaultOkHttpClient(context));
    }

    public SubsonicRestClient(@NonNull SubsonicServer server, @NonNull OkHttpClient client) {
        this.server = server;
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(SubsonicUrlUtil.getRestBaseUrl(server.baseUrl))
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(SubsonicApiService.class);
    }

    @NonNull
    public SubsonicServer getServer() {
        return server;
    }

    @NonNull
    public SubsonicApiService getApiService() {
        return apiService;
    }

    @NonNull
    public SubsonicResponse ping() throws IOException, SubsonicException {
        return execute(apiService.ping(createAuthParams()));
    }

    @NonNull
    public Map<String, String> createAuthParams() {
        return SubsonicAuth.createAuthParams(server.username, server.password);
    }

    @NonNull
    public String buildStreamUrl(@NonNull String remoteSongId) {
        return SubsonicUrlUtil.buildRestEndpointUrl(server, "stream", createAuthParams(), remoteSongId);
    }

    @NonNull
    public String buildCoverArtUrl(@NonNull String coverArtId) {
        return SubsonicUrlUtil.buildRestEndpointUrl(server, "getCoverArt", createAuthParams(), coverArtId);
    }

    @NonNull
    public static SubsonicResponse execute(@NonNull Call<SubsonicResponseEnvelope> call) throws IOException, SubsonicException {
        Response<SubsonicResponseEnvelope> response = call.execute();
        SubsonicResponseEnvelope envelope = response.body();
        if (!response.isSuccessful() || envelope == null || envelope.response == null) {
            throw new IOException("Subsonic request failed: HTTP " + response.code());
        }
        SubsonicResponse subsonicResponse = envelope.response;
        if (!subsonicResponse.isOk()) {
            if (subsonicResponse.error != null) {
                throw new SubsonicException(subsonicResponse.error.code, subsonicResponse.error.message);
            }
            throw new SubsonicException(0, "Subsonic request failed");
        }
        return subsonicResponse;
    }

    @NonNull
    private static OkHttpClient createDefaultOkHttpClient(@NonNull Context context) {
        File cacheDir = new File(context.getCacheDir(), "okhttp-subsonic");
        Cache cache = null;
        if (cacheDir.mkdirs() || cacheDir.isDirectory()) {
            cache = new Cache(cacheDir, CACHE_SIZE);
        }
        return new OkHttpClient.Builder()
                .cache(cache)
                .build();
    }
}
