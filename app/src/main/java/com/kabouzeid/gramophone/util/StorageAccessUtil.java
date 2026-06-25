package com.kabouzeid.gramophone.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;

public final class StorageAccessUtil {
    private static final String PRIMARY_VOLUME = "primary";
    private static final String HOME_VOLUME = "home";

    private StorageAccessUtil() {
    }

    public static void takePersistableReadPermission(@NonNull Context context, @NonNull Uri uri) {
        try {
            context.getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignored) {
        }
    }

    @Nullable
    public static File getFileFromTreeUri(@NonNull Context context, @NonNull Uri treeUri) {
        if (!DocumentsContract.isTreeUri(treeUri)) {
            return null;
        }

        String documentId = DocumentsContract.getTreeDocumentId(treeUri);
        if (documentId == null) {
            return null;
        }

        int splitIndex = documentId.indexOf(':');
        String volume = splitIndex >= 0 ? documentId.substring(0, splitIndex) : documentId;
        String relativePath = splitIndex >= 0 ? documentId.substring(splitIndex + 1) : "";

        if (PRIMARY_VOLUME.equalsIgnoreCase(volume)) {
            return buildFile(Environment.getExternalStorageDirectory(), relativePath);
        }
        if (HOME_VOLUME.equalsIgnoreCase(volume)) {
            return buildFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), relativePath);
        }

        File volumeRoot = findStorageVolume(context, volume);
        return volumeRoot != null ? buildFile(volumeRoot, relativePath) : null;
    }

    private static File buildFile(@NonNull File root, @Nullable String relativePath) {
        if (relativePath == null || relativePath.length() == 0) {
            return root;
        }
        return new File(root, relativePath);
    }

    @Nullable
    private static File findStorageVolume(@NonNull Context context, @NonNull String volume) {
        File storageVolume = new File("/storage/" + volume);
        if (storageVolume.exists()) {
            return storageVolume;
        }

        File[] externalFilesDirs = context.getExternalFilesDirs(null);
        if (externalFilesDirs == null) {
            return null;
        }

        for (File externalFilesDir : externalFilesDirs) {
            File root = findVolumeRoot(externalFilesDir);
            if (root != null && volume.equalsIgnoreCase(root.getName())) {
                return root;
            }
        }
        return null;
    }

    @Nullable
    private static File findVolumeRoot(@Nullable File file) {
        File current = file;
        while (current != null) {
            if ("Android".equals(current.getName())) {
                return current.getParentFile();
            }
            current = current.getParentFile();
        }
        return null;
    }
}
