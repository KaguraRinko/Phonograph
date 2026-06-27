package com.kabouzeid.gramophone.service;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
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
import java.util.concurrent.atomic.AtomicReference;

@UnstableApi
public class FfmpegPlayback implements Playback {
    private static final long PLAYER_CALL_TIMEOUT_SECONDS = 5;

    private final Context context;
    private final Handler playerHandler = new Handler(Looper.getMainLooper());
    private final ExoPlayer player;
    @Nullable
    private PlaybackCallbacks callbacks;
    private boolean isInitialized;
    private int bufferedPercent = 100;

    public FfmpegPlayback(@NonNull Context context) {
        this.context = context.getApplicationContext();
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
                    notifyPlaybackStateChanged();
                }

                @Override
                public void onPlayerError(@NonNull PlaybackException error) {
                    handlePlayerError();
                }

                @Override
                public void onEvents(@NonNull Player player, @NonNull Player.Events events) {
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
        Boolean prepared = callOnPlayerThread(() -> {
            if (player == null) {
                return false;
            }
            player.stop();
            player.clearMediaItems();
            player.setMediaItem(MediaItem.fromUri(Uri.parse(path)));
            player.prepare();
            isInitialized = true;
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
            return true;
        }, false);
        return started != null && started;
    }

    @Override
    public void stop() {
        isInitialized = false;
        callOnPlayerThread(() -> {
            if (player != null) {
                player.stop();
                player.clearMediaItems();
            }
            return null;
        }, null);
    }

    @Override
    public void release() {
        isInitialized = false;
        callOnPlayerThread(() -> {
            if (player != null) {
                player.release();
            }
            return null;
        }, null);
    }

    @Override
    public boolean pause() {
        Boolean paused = callOnPlayerThread(() -> {
            if (player == null) {
                return false;
            }
            player.pause();
            return true;
        }, false);
        return paused != null && paused;
    }

    @Override
    public boolean isPlaying() {
        Boolean playing = callOnPlayerThread(() -> player != null && player.isPlaying(), false);
        return playing != null && playing;
    }

    @Override
    public int duration() {
        Long duration = callOnPlayerThread(() -> player == null ? C.TIME_UNSET : player.getDuration(), C.TIME_UNSET);
        return duration == null ? -1 : safeTimeToInt(duration);
    }

    @Override
    public int position() {
        Long position = callOnPlayerThread(() -> player == null ? C.TIME_UNSET : player.getCurrentPosition(), C.TIME_UNSET);
        return position == null ? -1 : safeTimeToInt(position);
    }

    @Override
    public int bufferedPosition() {
        Long position = callOnPlayerThread(() -> player == null ? C.TIME_UNSET : player.getBufferedPosition(), C.TIME_UNSET);
        return position == null ? -1 : safeTimeToInt(position);
    }

    @Override
    public int seek(int whereto) {
        Boolean seeked = callOnPlayerThread(() -> {
            if (player == null) {
                return false;
            }
            player.seekTo(whereto);
            return true;
        }, false);
        return seeked != null && seeked ? whereto : -1;
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
            return true;
        }, false);
        return sessionSet != null && sessionSet;
    }

    @Override
    public int getAudioSessionId() {
        Integer audioSessionId = callOnPlayerThread(() -> player == null ? C.AUDIO_SESSION_ID_UNSET : player.getAudioSessionId(), C.AUDIO_SESSION_ID_UNSET);
        return audioSessionId == null ? C.AUDIO_SESSION_ID_UNSET : audioSessionId;
    }

    public int getBufferedProgressPercent() {
        return bufferedPercent;
    }

    private void handlePlaybackStateChanged(int playbackState) {
        if (callbacks == null) {
            return;
        }
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

    @Nullable
    private <T> T callOnPlayerThread(@NonNull PlayerCall<T> call, @Nullable T fallback) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            try {
                return call.run();
            } catch (RuntimeException e) {
                return fallback;
            }
        }
        AtomicReference<T> result = new AtomicReference<>(fallback);
        CountDownLatch latch = new CountDownLatch(1);
        playerHandler.post(() -> {
            try {
                result.set(call.run());
            } catch (RuntimeException ignored) {
                result.set(fallback);
            } finally {
                latch.countDown();
            }
        });
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
