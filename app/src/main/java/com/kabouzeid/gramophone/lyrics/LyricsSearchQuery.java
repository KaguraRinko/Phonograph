package com.kabouzeid.gramophone.lyrics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kabouzeid.gramophone.model.Song;

public class LyricsSearchQuery {
    @NonNull
    public final String title;
    @NonNull
    public final String artist;
    @NonNull
    public final String album;
    public final long durationMillis;

    public LyricsSearchQuery(@NonNull String title, @NonNull String artist,
                             @NonNull String album, long durationMillis) {
        this.title = clean(title);
        this.artist = clean(artist);
        this.album = clean(album);
        this.durationMillis = Math.max(durationMillis, 0);
    }

    @NonNull
    public static LyricsSearchQuery fromSong(@NonNull Song song) {
        return new LyricsSearchQuery(song.title, song.artistName, song.albumName, song.duration);
    }

    @NonNull
    public LyricsSearchQuery withKeyword(@Nullable String keyword) {
        String value = clean(keyword);
        if (value.isEmpty()) {
            return this;
        }
        return new LyricsSearchQuery(value, artist, album, durationMillis);
    }

    @NonNull
    public String keyword() {
        if (artist.isEmpty()) {
            return title;
        }
        return title + " " + artist;
    }

    @NonNull
    private static String clean(@Nullable String value) {
        return value == null ? "" : value.trim();
    }
}
