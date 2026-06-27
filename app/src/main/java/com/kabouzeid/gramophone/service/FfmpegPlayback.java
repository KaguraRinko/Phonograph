package com.kabouzeid.gramophone.service;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;

import com.kabouzeid.gramophone.R;
import com.kabouzeid.gramophone.service.playback.Playback;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@UnstableApi
public class FfmpegPlayback implements Playback {
    private static final long PLAYER_CALL_TIMEOUT_SECONDS = 5;

    private final Context context;
    private final HandlerThread playerThread;
    private final Handler playerHandler;
    private final ExoPlayer player;
    @Nullable
    private PlaybackCallbacks callbacks;
    private volatile boolean isInitialized;
    private volatile boolean cachedIsPlaying;
    private volatile int bufferedPercent = 100;
    private volatile long cachedDuration = C.TIME_UNSET;
    private volatile long cachedPosition = C.TIME_UNSET;
    private volatile long cachedBufferedPosition = C.TIME_UNSET;
    private volatile long cachedPositionUpdateTime;
    private volatile int cachedAudioSessionId = C.AUDIO_SESSION_ID_UNSET;
    private final AtomicInteger seekVersion = new AtomicInteger();

    public FfmpegPlayback(@NonNull Context context) {
        this.context = context.getApplicationContext();
        playerThread = new HandlerThread("FfmpegPlayback", Process.THREAD_PRIORITY_AUDIO);
        playerThread.start();
        playerHandler = new Handler(playerThread.getLooper());
        player = callOnPlayerThread(() -> {
            DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(this.context)
                    .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
                    .setEnableDecoderFallback(true);
            ExoPlayer exoPlayer = new ExoPlayer.Builder(this.context, renderersFactory)
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                            .setUsage(C.USAGE_MEDIA)
                            .build(), false)
                    .setHandleAudioBecomingNoisy(false)
                    .build();
            exoPlayer.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int playbackState) {
                    handlePlaybackStateChanged(playbackState);
                }

                @Override
                public void onIsPlayingChanged(boolean isPlaying) {
                    cachePlayerState(exoPlayer);
                    notifyPlaybackStateChanged();
                }

                @Override
                public void onPlayerError(@NonNull PlaybackException error) {
                    handlePlayerError();
                }

                @Override
                public void onEvents(@NonNull Player player, @NonNull Player.Events events) {
                    cachePlayerState(player);
                    bufferedPercent = Math.max(0, Math.min(100, player.getBufferedPercentage()));
                    if (callbacks != null) {
                        callbacks.onBufferingProgressChanged(bufferedPercent);
                    }
                }
            });
            return exoPlayer;
        }, null);
    }

    @Override
    public boolean setDataSource(@NonNull String path) {
        isInitialized = false;
        bufferedPercent = isRemotePath(path) ? 0 : 100;
        resetCachedState();
        Boolean prepared = callOnPlayerThread(() -> {
            if (player == null) {
                return false;
            }
            player.stop();
            player.clearMediaItems();
            player.setMediaItem(MediaItem.fromUri(Uri.parse(path)));
            player.prepare();
            isInitialized = true;
            cachePlayerState(player);
            return true;
        }, false);
        return prepared != null && prepared;
    }

    @Override
    public void setNextDataSource(@Nullable String path) {
        // ExoPlayer fallback is used only for the active failed item; keep gapless preparation on
        // the primary MediaPlayer path.
    }

    @Override
    public void setCallbacks(PlaybackCallbacks callbacks) {
        this.callbacks = callbacks;
    }

    @Override
    public boolean isInitialized() {
        return isInitialized;
    }

    @Override
    public boolean start() {
        Boolean started = callOnPlayerThread(() -> {
            if (player == null) {
                return false;
            }
            player.play();
            cachePlayerState(player);
            return true;
        }, false);
        return started != null && started;
    }

    @Override
    public void stop() {
        isInitialized = false;
        resetCachedState();
        callOnPlayerThread(() -> {
            if (player != null) {
                player.stop();
                player.clearMediaItems();
                cachePlayerState(player);
            }
            return null;
        }, null);
    }

    @Override
    public void release() {
        isInitialized = false;
        resetCachedState();
        callOnPlayerThread(() -> {
            if (player != null) {
                player.release();
            }
            return null;
        }, null);
        playerThread.quitSafely();
    }

    @Override
    public boolean pause() {
        Boolean paused = callOnPlayerThread(() -> {
            if (player == null) {
                return false;
            }
            player.pause();
            cachePlayerState(player);
            return true;
        }, false);
        return paused != null && paused;
    }

    @Override
    public boolean isPlaying() {
        return cachedIsPlaying;
    }

    @Override
    public int duration() {
        return safeTimeToInt(cachedDuration);
    }

    @Override
    public int position() {
        return safeTimeToInt(getCachedPosition());
    }

    @Override
    public int bufferedPosition() {
        return safeTimeToInt(cachedBufferedPosition);
    }

    @Override
    public int seek(int whereto) {
        if (player == null) {
            return -1;
        }
        int safePosition = Math.max(0, whereto);
        cachedPosition = safePosition;
        cachedPositionUpdateTime = SystemClock.elapsedRealtime();
        int version = seekVersion.incrementAndGet();

        if (Looper.myLooper() == playerThread.getLooper()) {
            try {
                player.seekTo(safePosition);
                cachedPosition = safePosition;
                cachedPositionUpdateTime = SystemClock.elapsedRealtime();
                notifyPlaybackStateChanged();
                return safePosition;
            } catch (RuntimeException e) {
                return -1;
            }
        }

        boolean posted = playerHandler.post(() -> {
            if (player == null || version != seekVersion.get()) {
                return;
            }
            try {
                player.seekTo(safePosition);
                cachedPosition = safePosition;
                cachedPositionUpdateTime = SystemClock.elapsedRealtime();
                notifyPlaybackStateChanged();
            } catch (RuntimeException ignored) {
            }
        });
        return posted ? safePosition : -1;
    }

    @Override
    public boolean setVolume(float vol) {
        Boolean volumeSet = callOnPlayerThread(() -> {
            if (player == null) {
                return false;
            }
            player.setVolume(vol);
            return true;
        }, false);
        return volumeSet != null && volumeSet;
    }

    @Override
    public boolean setAudioSessionId(int sessionId) {
        Boolean sessionSet = callOnPlayerThread(() -> {
            if (player == null) {
                return false;
            }
            player.setAudioSessionId(sessionId);
            cachedAudioSessionId = sessionId;
            cachePlayerState(player);
            return true;
        }, false);
        return sessionSet != null && sessionSet;
    }

    @Override
    public int getAudioSessionId() {
        return cachedAudioSessionId;
    }

    public int getBufferedProgressPercent() {
        return bufferedPercent;
    }

    private void handlePlaybackStateChanged(int playbackState) {
        if (callbacks == null) {
            return;
        }
        cachePlayerState(player);
        if (playbackState == Player.STATE_BUFFERING) {
            callbacks.onBufferingStarted();
        } else if (playbackState == Player.STATE_READY) {
            callbacks.onBufferingEnded();
        } else if (playbackState == Player.STATE_ENDED) {
            callbacks.onTrackEnded();
        }
        notifyPlaybackStateChanged();
    }

    private void notifyPlaybackStateChanged() {
        if (callbacks != null) {
            callbacks.onPlaybackStateChanged();
        }
    }

    private void handlePlayerError() {
        isInitialized = false;
        bufferedPercent = 0;
        resetCachedState();
        if (callbacks != null) {
            callbacks.onBufferingEnded();
        }
        boolean handled = callbacks != null && callbacks.onPlaybackError();
        if (!handled) {
            Toast.makeText(context, context.getResources().getString(R.string.unplayable_file), Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isRemotePath(@NonNull String path) {
        return path.startsWith("http://") || path.startsWith("https://");
    }

    private int safeTimeToInt(long time) {
        if (time == C.TIME_UNSET || time < 0) {
            return -1;
        }
        return time > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) time;
    }

    private void cachePlayerState(@Nullable Player player) {
        if (player == null) {
            resetCachedState();
            return;
        }
        cachedIsPlaying = player.isPlaying();
        cachedDuration = player.getDuration();
        cachedPosition = player.getCurrentPosition();
        cachedBufferedPosition = player.getBufferedPosition();
        if (player instanceof ExoPlayer) {
            cachedAudioSessionId = ((ExoPlayer) player).getAudioSessionId();
        }
        cachedPositionUpdateTime = SystemClock.elapsedRealtime();
    }

    private long getCachedPosition() {
        long position = cachedPosition;
        if (position == C.TIME_UNSET || position < 0) {
            return position;
        }
        if (cachedIsPlaying) {
            position += SystemClock.elapsedRealtime() - cachedPositionUpdateTime;
        }
        return cachedDuration > 0 ? Math.min(position, cachedDuration) : position;
    }

    private void resetCachedState() {
        cachedIsPlaying = false;
        cachedDuration = C.TIME_UNSET;
        cachedPosition = C.TIME_UNSET;
        cachedBufferedPosition = C.TIME_UNSET;
        cachedPositionUpdateTime = SystemClock.elapsedRealtime();
        cachedAudioSessionId = C.AUDIO_SESSION_ID_UNSET;
    }

    @Nullable
    private <T> T callOnPlayerThread(@NonNull PlayerCall<T> call, @Nullable T fallback) {
        if (Looper.myLooper() == playerThread.getLooper()) {
            try {
                return call.run();
            } catch (RuntimeException e) {
                return fallback;
            }
        }
        AtomicReference<T> result = new AtomicReference<>(fallback);
        CountDownLatch latch = new CountDownLatch(1);
        boolean posted = playerHandler.post(() -> {
            try {
                result.set(call.run());
            } catch (RuntimeException ignored) {
                result.set(fallback);
            } finally {
                latch.countDown();
            }
        });
        if (!posted) {
            return fallback;
        }
        try {
            if (!latch.await(PLAYER_CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                return fallback;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return fallback;
        }
        return result.get();
    }

    private interface PlayerCall<T> {
        @Nullable
        T run();
    }
}
