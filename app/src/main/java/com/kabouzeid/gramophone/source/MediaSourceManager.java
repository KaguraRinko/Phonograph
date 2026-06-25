package com.kabouzeid.gramophone.source;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kabouzeid.gramophone.R;
import com.kabouzeid.gramophone.subsonic.SubsonicServer;
import com.kabouzeid.gramophone.subsonic.SubsonicServerStore;

import java.util.ArrayList;
import java.util.List;

public class MediaSourceManager {
    public static final String LOCAL_SOURCE_ID = "local";
    public static final String SUBSONIC_SOURCE_PREFIX = "subsonic:";

    private static final String PREFERENCES_NAME = "media_sources";
    private static final String KEY_CURRENT_SOURCE_ID = "current_source_id";

    private MediaSourceManager() {
    }

    @NonNull
    public static MediaSource getLocalSource(@NonNull Context context) {
        return new MediaSource(LOCAL_SOURCE_ID, MediaSource.TYPE_LOCAL, context.getString(R.string.library));
    }

    @NonNull
    public static String getCurrentSourceId(@NonNull Context context) {
        return getPreferences(context).getString(KEY_CURRENT_SOURCE_ID, LOCAL_SOURCE_ID);
    }

    public static void setCurrentSourceId(@NonNull Context context, @NonNull String sourceId) {
        getPreferences(context).edit().putString(KEY_CURRENT_SOURCE_ID, sourceId).apply();
    }

    public static boolean isLocalSource(@NonNull String sourceId) {
        return LOCAL_SOURCE_ID.equals(sourceId);
    }

    public static boolean isSubsonicSource(@NonNull String sourceId) {
        return sourceId.startsWith(SUBSONIC_SOURCE_PREFIX);
    }

    public static boolean isCurrentSourceLocal(@NonNull Context context) {
        return isLocalSource(getCurrentSourceId(context));
    }

    @NonNull
    public static List<MediaSource> getAvailableSources(@NonNull Context context) {
        List<MediaSource> sources = new ArrayList<>();
        sources.add(getLocalSource(context));
        for (SubsonicServer server : SubsonicServerStore.getInstance(context).getAllServers()) {
            sources.add(toMediaSource(server));
        }
        return sources;
    }

    @Nullable
    public static MediaSource getSource(@NonNull Context context, @NonNull String sourceId) {
        if (isLocalSource(sourceId)) {
            return getLocalSource(context);
        }
        long serverId = getSubsonicServerId(sourceId);
        if (serverId == SubsonicServer.NO_ID) {
            return null;
        }
        SubsonicServer server = SubsonicServerStore.getInstance(context).getServer(serverId);
        return server == null ? null : toMediaSource(server);
    }

    @NonNull
    public static String toSubsonicSourceId(long serverId) {
        return SUBSONIC_SOURCE_PREFIX + serverId;
    }

    public static long getSubsonicServerId(@NonNull String sourceId) {
        if (!isSubsonicSource(sourceId)) {
            return SubsonicServer.NO_ID;
        }
        try {
            return Long.parseLong(sourceId.substring(SUBSONIC_SOURCE_PREFIX.length()));
        } catch (NumberFormatException e) {
            return SubsonicServer.NO_ID;
        }
    }

    @NonNull
    public static MediaSource toMediaSource(@NonNull SubsonicServer server) {
        return new MediaSource(toSubsonicSourceId(server.id), MediaSource.TYPE_SUBSONIC, server.name);
    }

    @NonNull
    public static MusicRepository getCurrentRepository(@NonNull Context context) {
        return getRepository(context, getCurrentSourceId(context));
    }

    @NonNull
    public static MusicRepository getRepository(@NonNull Context context, @NonNull String sourceId) {
        if (isSubsonicSource(sourceId)) {
            long serverId = getSubsonicServerId(sourceId);
            SubsonicServer server = SubsonicServerStore.getInstance(context).getServer(serverId);
            if (server != null) {
                return new SubsonicMusicRepository(server);
            }
        }
        return new LocalMusicRepository(context);
    }

    @NonNull
    private static SharedPreferences getPreferences(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }
}
