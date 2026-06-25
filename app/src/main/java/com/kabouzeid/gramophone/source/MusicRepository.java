package com.kabouzeid.gramophone.source;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kabouzeid.gramophone.model.Album;
import com.kabouzeid.gramophone.model.Artist;
import com.kabouzeid.gramophone.model.Genre;
import com.kabouzeid.gramophone.model.Playlist;
import com.kabouzeid.gramophone.model.Song;

import java.util.List;

public interface MusicRepository {
    @NonNull
    MediaSource getSource();

    @NonNull
    List<Song> getAllSongs(@NonNull Context context);

    @NonNull
    List<Song> getSongs(@NonNull Context context, @NonNull String query);

    @NonNull
    Song getSong(@NonNull Context context, long songId);

    @NonNull
    List<Album> getAllAlbums(@NonNull Context context);

    @NonNull
    List<Album> getAlbums(@NonNull Context context, @NonNull String query);

    @NonNull
    Album getAlbum(@NonNull Context context, long albumId);

    @NonNull
    List<Artist> getAllArtists(@NonNull Context context);

    @NonNull
    List<Artist> getArtists(@NonNull Context context, @NonNull String query);

    @NonNull
    Artist getArtist(@NonNull Context context, long artistId);

    @NonNull
    List<Genre> getAllGenres(@NonNull Context context);

    @NonNull
    List<Song> getSongsForGenre(@NonNull Context context, long genreId);

    @NonNull
    List<Playlist> getAllPlaylists(@NonNull Context context);

    @NonNull
    List<Song> getSongsForPlaylist(@NonNull Context context, @NonNull Playlist playlist);

    @NonNull
    String resolveSongUri(@NonNull Context context, @NonNull Song song);

    @Nullable
    String resolveCoverArtUri(@NonNull Context context, @NonNull Song song);

    boolean supportsLocalFileActions();
}
