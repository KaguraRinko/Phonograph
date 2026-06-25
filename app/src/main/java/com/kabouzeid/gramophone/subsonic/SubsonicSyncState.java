package com.kabouzeid.gramophone.subsonic;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class SubsonicSyncState {
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final Object LOCK = new Object();
    private static final List<Listener> LISTENERS = new ArrayList<>();

    @Nullable
    private static Snapshot snapshot;

    private SubsonicSyncState() {
    }

    public static void update(long serverId, @NonNull String message, int progress) {
        publish(new Snapshot(serverId, message, clampProgress(progress), true));
    }

    public static void finish(long serverId, @NonNull String message) {
        publish(new Snapshot(serverId, message, 100, false));
    }

    @Nullable
    public static Snapshot getSnapshot() {
        synchronized (LOCK) {
            return snapshot;
        }
    }

    public static boolean isSyncing() {
        synchronized (LOCK) {
            return snapshot != null && snapshot.running;
        }
    }

    public static void addListener(@NonNull Listener listener) {
        synchronized (LOCK) {
            if (!LISTENERS.contains(listener)) {
                LISTENERS.add(listener);
            }
        }
    }

    public static void removeListener(@NonNull Listener listener) {
        synchronized (LOCK) {
            LISTENERS.remove(listener);
        }
    }

    private static void publish(@NonNull Snapshot newSnapshot) {
        List<Listener> listeners;
        synchronized (LOCK) {
            snapshot = newSnapshot;
            listeners = new ArrayList<>(LISTENERS);
        }
        MAIN_HANDLER.post(() -> {
            for (Listener listener : listeners) {
                listener.onSubsonicSyncStateChanged(newSnapshot);
            }
        });
    }

    private static int clampProgress(int progress) {
        return Math.max(0, Math.min(100, progress));
    }

    public interface Listener {
        void onSubsonicSyncStateChanged(@NonNull Snapshot snapshot);
    }

    public static final class Snapshot {
        public final long serverId;
        @NonNull
        public final String message;
        public final int progress;
        public final boolean running;

        private Snapshot(long serverId, @NonNull String message, int progress, boolean running) {
            this.serverId = serverId;
            this.message = message;
            this.progress = progress;
            this.running = running;
        }
    }
}
