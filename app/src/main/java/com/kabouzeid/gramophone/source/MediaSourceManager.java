package com.kabouzeid.gramophone.source;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.kabouzeid.gramophone.R;

public class MediaSourceManager {
    public static final String LOCAL_SOURCE_ID = "local";

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

    public static boolean isCurrentSourceLocal(@NonNull Context context) {
        return isLocalSource(getCurrentSourceId(context));
    }

    @NonNull
    public static MusicRepository getCurrentRepository(@NonNull Context context) {
        return getRepository(context, getCurrentSourceId(context));
    }

    @NonNull
    public static MusicRepository getRepository(@NonNull Context context, @NonNull String sourceId) {
        return new LocalMusicRepository(context);
    }

    @NonNull
    private static SharedPreferences getPreferences(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }
}
