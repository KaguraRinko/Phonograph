package com.kabouzeid.gramophone.lyrics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kabouzeid.gramophone.model.lyrics.AbsSynchronizedLyrics;

public class LyricsSearchResult {
    @NonNull
    public final String provider;
    @NonNull
    public final String title;
    @NonNull
    public final String artist;
    @NonNull
    public final String album;
    public final long durationMillis;
    @NonNull
    public final String lyrics;
    public final boolean synchronizedLyrics;
    public final int score;

    public LyricsSearchResult(@NonNull String provider, @Nullable String title,
                              @Nullable String artist, @Nullable String album,
                              long durationMillis, @NonNull String lyrics,
                              int score) {
        this.provider = provider;
        this.title = clean(title);
        this.artist = clean(artist);
        this.album = clean(album);
        this.durationMillis = Math.max(durationMillis, 0);
        this.lyrics = lyrics.trim();
        synchronizedLyrics = AbsSynchronizedLyrics.isSynchronized(this.lyrics);
        this.score = Math.max(score, 0);
    }

    public boolean hasUsableLyrics() {
        return !lyrics.trim().isEmpty();
    }

    @NonNull
    private static String clean(@Nullable String value) {
        return value == null ? "" : value.trim();
    }
}
