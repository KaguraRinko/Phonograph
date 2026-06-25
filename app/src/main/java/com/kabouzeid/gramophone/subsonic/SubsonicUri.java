package com.kabouzeid.gramophone.subsonic;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class SubsonicUri {
    public static final String SCHEME = "subsonic";
    private static final String AUTHORITY = "server";
    private static final String SONG_PATH = "song";

    private SubsonicUri() {
    }

    @NonNull
    public static String forSong(long serverId, @NonNull String remoteSongId) {
        return new Uri.Builder()
                .scheme(SCHEME)
                .authority(AUTHORITY)
                .appendPath(String.valueOf(serverId))
                .appendPath(SONG_PATH)
                .appendPath(remoteSongId)
                .build()
                .toString();
    }

    public static boolean isSubsonicUri(@Nullable String value) {
        return value != null && SCHEME.equals(Uri.parse(value).getScheme());
    }

    public static long getServerId(@Nullable String value) {
        if (!isSubsonicUri(value)) {
            return SubsonicServer.NO_ID;
        }
        List<String> segments = Uri.parse(value).getPathSegments();
        if (segments.size() < 1) {
            return SubsonicServer.NO_ID;
        }
        try {
            return Long.parseLong(segments.get(0));
        } catch (NumberFormatException e) {
            return SubsonicServer.NO_ID;
        }
    }

    @Nullable
    public static String getRemoteSongId(@Nullable String value) {
        if (!isSubsonicUri(value)) {
            return null;
        }
        List<String> segments = Uri.parse(value).getPathSegments();
        if (segments.size() < 3 || !SONG_PATH.equals(segments.get(1))) {
            return null;
        }
        return segments.get(2);
    }
}
