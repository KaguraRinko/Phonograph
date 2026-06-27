package com.kabouzeid.gramophone.subsonic;

import android.content.Context;

import androidx.annotation.NonNull;

import com.kabouzeid.gramophone.glide.subsonic.SubsonicCoverArt;
import com.kabouzeid.gramophone.subsonic.rest.SubsonicRestClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public final class SubsonicCoverArtCache {
    private static final String CACHE_DIR = "subsonic-cover-art";
    private static final String TEMP_SUFFIX = ".tmp";
    private static final long MAX_CACHE_BYTES = 256L * 1024L * 1024L;
    private static final int BUFFER_SIZE = 16 * 1024;
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();

    private SubsonicCoverArtCache() {
    }

    @NonNull
    public static InputStream open(@NonNull Context context, @NonNull SubsonicCoverArt coverArt) throws IOException {
        File cachedFile = getCacheFile(context, coverArt.server.id, coverArt.coverArtId);
        if (isUsableFile(cachedFile)) {
            touch(cachedFile);
            return new FileInputStream(cachedFile);
        }
        download(context, coverArt, cachedFile);
        return new FileInputStream(cachedFile);
    }

    public static void cache(@NonNull Context context, @NonNull SubsonicServer server,
                             @NonNull String coverArtId) throws IOException {
        InputStream stream = open(context, new SubsonicCoverArt(server, coverArtId));
        stream.close();
    }

    public static boolean isCached(@NonNull Context context, long serverId, @NonNull String coverArtId) {
        return isUsableFile(getCacheFile(context, serverId, coverArtId));
    }

    public static void clearServer(@NonNull Context context, long serverId) {
        deleteRecursively(new File(getRootDir(context), String.valueOf(serverId)));
    }

    private static void download(@NonNull Context context, @NonNull SubsonicCoverArt coverArt,
                                 @NonNull File cachedFile) throws IOException {
        File parent = cachedFile.getParentFile();
        if (parent == null || (!parent.isDirectory() && !parent.mkdirs())) {
            throw new IOException("Could not create Subsonic cover art cache");
        }

        File tempFile = new File(parent, cachedFile.getName() + TEMP_SUFFIX);
        if (tempFile.exists() && !tempFile.delete()) {
            throw new IOException("Could not clear stale Subsonic cover art cache file");
        }

        Request request = new Request.Builder()
                .url(new SubsonicRestClient(context, coverArt.server).buildCoverArtUrl(coverArt.coverArtId))
                .build();
        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            ResponseBody body = response.body();
            if (!response.isSuccessful() || body == null) {
                throw new IOException("Subsonic cover art request failed: HTTP " + response.code());
            }
            try (InputStream in = body.byteStream();
                 FileOutputStream out = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
        } catch (IOException e) {
            //noinspection ResultOfMethodCallIgnored
            tempFile.delete();
            throw e;
        }

        if (cachedFile.exists() && !cachedFile.delete()) {
            //noinspection ResultOfMethodCallIgnored
            tempFile.delete();
            throw new IOException("Could not replace Subsonic cover art cache file");
        }
        if (!tempFile.renameTo(cachedFile)) {
            //noinspection ResultOfMethodCallIgnored
            tempFile.delete();
            throw new IOException("Could not store Subsonic cover art cache file");
        }
        touch(cachedFile);
        trimToSize(context);
    }

    @NonNull
    private static File getCacheFile(@NonNull Context context, long serverId, @NonNull String coverArtId) {
        return new File(new File(getRootDir(context), String.valueOf(serverId)), sha256(coverArtId));
    }

    @NonNull
    private static File getRootDir(@NonNull Context context) {
        return new File(context.getApplicationContext().getCacheDir(), CACHE_DIR);
    }

    private static boolean isUsableFile(@NonNull File file) {
        return file.isFile() && file.length() > 0;
    }

    private static void trimToSize(@NonNull Context context) {
        File rootDir = getRootDir(context);
        if (!rootDir.isDirectory()) {
            return;
        }

        List<CachedFile> cachedFiles = new ArrayList<>();
        collectCachedFiles(rootDir, cachedFiles);
        long totalSize = 0;
        for (CachedFile cachedFile : cachedFiles) {
            totalSize += cachedFile.length;
        }
        if (totalSize <= MAX_CACHE_BYTES) {
            return;
        }

        Collections.sort(cachedFiles, Comparator.comparingLong(file -> file.lastModified));
        for (CachedFile cachedFile : cachedFiles) {
            if (totalSize <= MAX_CACHE_BYTES) {
                break;
            }
            if (cachedFile.file.delete()) {
                totalSize -= cachedFile.length;
            }
        }
    }

    private static void collectCachedFiles(@NonNull File directory, @NonNull List<CachedFile> cachedFiles) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                collectCachedFiles(file, cachedFiles);
            } else if (file.isFile() && !file.getName().endsWith(TEMP_SUFFIX)) {
                cachedFiles.add(new CachedFile(file));
            }
        }
    }

    private static void deleteRecursively(@NonNull File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }

    private static void touch(@NonNull File file) {
        //noinspection ResultOfMethodCallIgnored
        file.setLastModified(System.currentTimeMillis());
    }

    @NonNull
    private static String sha256(@NonNull String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                builder.append(String.format(Locale.US, "%02x", b & 0xff));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private static final class CachedFile {
        final File file;
        final long length;
        final long lastModified;

        CachedFile(@NonNull File file) {
            this.file = file;
            length = file.length();
            lastModified = file.lastModified();
        }
    }
}
