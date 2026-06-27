package com.kabouzeid.gramophone.subsonic;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.List;

public class SubsonicCoverArtPreloader {
    private final Context context;
    private final SubsonicServer server;
    private final ProgressCallback callback;

    public SubsonicCoverArtPreloader(@NonNull Context context, @NonNull SubsonicServer server,
                                     @NonNull ProgressCallback callback) {
        this.context = context.getApplicationContext();
        this.server = server;
        this.callback = callback;
    }

    @NonNull
    public Result preload() {
        List<String> coverArtIds = SubsonicLibraryStore.getInstance(context).getDistinctCoverArtIds(server.id);
        Result result = new Result(coverArtIds.size());
        if (coverArtIds.isEmpty()) {
            callback.onProgress(result.snapshot());
            return result;
        }

        for (String coverArtId : coverArtIds) {
            if (SubsonicCoverArtCache.isCached(context, server.id, coverArtId)) {
                result.skipped++;
            } else {
                try {
                    SubsonicCoverArtCache.cache(context, server, coverArtId);
                    result.downloaded++;
                } catch (IOException e) {
                    result.failed++;
                }
            }
            result.completed++;
            callback.onProgress(result.snapshot());
        }
        return result;
    }

    public interface ProgressCallback {
        void onProgress(@NonNull Snapshot snapshot);
    }

    public static final class Result {
        public final int total;
        public int completed;
        public int downloaded;
        public int skipped;
        public int failed;

        Result(int total) {
            this.total = total;
        }

        @NonNull
        Snapshot snapshot() {
            return new Snapshot(total, completed, downloaded, skipped, failed);
        }
    }

    public static final class Snapshot {
        public final int total;
        public final int completed;
        public final int downloaded;
        public final int skipped;
        public final int failed;
        public final int progress;

        Snapshot(int total, int completed, int downloaded, int skipped, int failed) {
            this.total = total;
            this.completed = completed;
            this.downloaded = downloaded;
            this.skipped = skipped;
            this.failed = failed;
            progress = total <= 0 ? 100 : Math.round((completed * 100f) / total);
        }
    }
}
