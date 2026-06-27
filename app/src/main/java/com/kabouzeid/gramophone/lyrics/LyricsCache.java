package com.kabouzeid.gramophone.lyrics;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kabouzeid.gramophone.model.Song;
import com.kabouzeid.gramophone.subsonic.SubsonicServer;
import com.kabouzeid.gramophone.subsonic.SubsonicUri;
import com.kabouzeid.gramophone.util.FileUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public final class LyricsCache {
    private static final String CACHE_DIR = "lyrics";
    private static final String LRC_EXTENSION = ".lrc";
    private static final String TEXT_EXTENSION = ".txt";

    private LyricsCache() {
    }

    @Nullable
    public static String read(@NonNull Context context, @NonNull Song song) {
        File file = findCacheFile(context, song);
        if (file == null) {
            return null;
        }
        try {
            return FileUtil.read(file);
        } catch (Exception e) {
            return null;
        }
    }

    public static void write(@NonNull Context context, @NonNull Song song,
                             @NonNull LyricsSearchResult result) throws IOException {
        write(context, song, result.lyrics, result.synchronizedLyrics);
    }

    public static void write(@NonNull Context context, @NonNull Song song,
                             @NonNull String lyrics, boolean synchronizedLyrics) throws IOException {
        File dir = getCacheDir(context);
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("Could not create lyrics cache");
        }
        delete(context, song);
        File file = new File(dir, cacheName(song) + (synchronizedLyrics ? LRC_EXTENSION : TEXT_EXTENSION));
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(lyrics.getBytes(StandardCharsets.UTF_8));
        }
    }

    public static void delete(@NonNull Context context, @NonNull Song song) {
        String name = cacheName(song);
        File dir = getCacheDir(context);
        //noinspection ResultOfMethodCallIgnored
        new File(dir, name + LRC_EXTENSION).delete();
        //noinspection ResultOfMethodCallIgnored
        new File(dir, name + TEXT_EXTENSION).delete();
    }

    @Nullable
    private static File findCacheFile(@NonNull Context context, @NonNull Song song) {
        String name = cacheName(song);
        File dir = getCacheDir(context);
        File lrc = new File(dir, name + LRC_EXTENSION);
        if (lrc.isFile() && lrc.length() > 0) {
            return lrc;
        }
        File text = new File(dir, name + TEXT_EXTENSION);
        return text.isFile() && text.length() > 0 ? text : null;
    }

    @NonNull
    private static File getCacheDir(@NonNull Context context) {
        return new File(context.getApplicationContext().getCacheDir(), CACHE_DIR);
    }

    @NonNull
    public static String cacheName(@NonNull Song song) {
        return sha256(cacheKey(song));
    }

    @NonNull
    static String cacheKey(@NonNull Song song) {
        String source = "local";
        String id = String.valueOf(song.id);
        if (SubsonicUri.isSubsonicUri(song.data)) {
            long serverId = SubsonicUri.getServerId(song.data);
            String remoteSongId = SubsonicUri.getRemoteSongId(song.data);
            source = serverId == SubsonicServer.NO_ID ? "subsonic" : "subsonic:" + serverId;
            id = remoteSongId == null ? song.data : remoteSongId;
        }
        return source + "|"
                + id + "|"
                + clean(song.title) + "|"
                + clean(song.artistName) + "|"
                + clean(song.albumName) + "|"
                + Math.max(song.duration, 0);
    }

    @NonNull
    private static String clean(@Nullable String value) {
        return value == null ? "" : value.trim();
    }

    @NonNull
    private static String sha256(@NonNull String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                builder.append(String.format(Locale.US, "%02x", b & 0xff));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
