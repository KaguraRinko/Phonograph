package com.kabouzeid.gramophone.subsonic;

import androidx.annotation.NonNull;

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
        HttpUrl baseUrl = HttpUrl.parse(getRestBaseUrl(server.baseUrl) + endpoint + ".view");
        if (baseUrl == null) {
            throw new IllegalArgumentException("Invalid Subsonic server URL: " + server.baseUrl);
        }
        HttpUrl.Builder builder = baseUrl.newBuilder();
        for (Map.Entry<String, String> entry : authParams.entrySet()) {
            builder.addQueryParameter(entry.getKey(), entry.getValue());
        }
        builder.addQueryParameter("id", id);
        return builder.build().toString();
    }
}
