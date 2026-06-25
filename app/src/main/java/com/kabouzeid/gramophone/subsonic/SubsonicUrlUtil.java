package com.kabouzeid.gramophone.subsonic;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

import okhttp3.HttpUrl;

public class SubsonicUrlUtil {
    private static final String REST_PATH = "rest";

    private SubsonicUrlUtil() {
    }

    @NonNull
    public static String normalizeServerUrl(@NonNull String rawUrl) {
        String url = rawUrl.trim();
        if (!url.contains("://")) {
            url = "https://" + url;
        }
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        if (url.endsWith("/" + REST_PATH)) {
            url = url.substring(0, url.length() - REST_PATH.length() - 1);
        }
        return url;
    }

    @NonNull
    public static String getRestBaseUrl(@NonNull String serverBaseUrl) {
        return normalizeServerUrl(serverBaseUrl) + "/" + REST_PATH + "/";
    }

    @NonNull
    public static String buildRestEndpointUrl(@NonNull SubsonicServer server,
                                              @NonNull String endpoint,
                                              @NonNull Map<String, String> authParams,
                                              @NonNull String id) {
        return buildRestEndpointUrl(server, endpoint, authParams, id, null);
    }

    @NonNull
    public static String buildRestEndpointUrl(@NonNull SubsonicServer server,
                                              @NonNull String endpoint,
                                              @NonNull Map<String, String> authParams,
                                              @NonNull String id,
                                              @Nullable Map<String, String> extraParams) {
        HttpUrl baseUrl = HttpUrl.parse(getRestBaseUrl(server.baseUrl) + endpoint + ".view");
        if (baseUrl == null) {
            throw new IllegalArgumentException("Invalid Subsonic server URL: " + server.baseUrl);
        }
        HttpUrl.Builder builder = baseUrl.newBuilder();
        for (Map.Entry<String, String> entry : authParams.entrySet()) {
            builder.addQueryParameter(entry.getKey(), entry.getValue());
        }
        builder.addQueryParameter("id", id);
        if (extraParams != null) {
            for (Map.Entry<String, String> entry : extraParams.entrySet()) {
                builder.addQueryParameter(entry.getKey(), entry.getValue());
            }
        }
        return builder.build().toString();
    }
}
