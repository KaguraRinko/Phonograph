package com.kabouzeid.gramophone.source;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kabouzeid.gramophone.loader.AlbumLoader;
import com.kabouzeid.gramophone.loader.ArtistLoader;
import com.kabouzeid.gramophone.loader.GenreLoader;
import com.kabouzeid.gramophone.loader.PlaylistLoader;
import com.kabouzeid.gramophone.loader.PlaylistSongLoader;
import com.kabouzeid.gramophone.loader.SongLoader;
import com.kabouzeid.gramophone.model.AbsCustomPlaylist;
import com.kabouzeid.gramophone.model.Album;
import com.kabouzeid.gramophone.model.Artist;
import com.kabouzeid.gramophone.model.Genre;
import com.kabouzeid.gramophone.model.Playlist;
import com.kabouzeid.gramophone.model.Song;
import com.kabouzeid.gramophone.model.smartplaylist.HistoryPlaylist;
import com.kabouzeid.gramophone.model.smartplaylist.LastAddedPlaylist;
import com.kabouzeid.gramophone.model.smartplaylist.MyTopTracksPlaylist;
import com.kabouzeid.gramophone.util.MusicUtil;

import java.util.ArrayList;
import java.util.List;

public class LocalMusicRepository implements MusicRepository {
    private final MediaSource source;

    public LocalMusicRepository(@NonNull Context context) {
        source = MediaSourceManager.getLocalSource(context);
    }

    @NonNull
    @Override
    public MediaSource getSource() {
        return source;
    }

    @NonNull
    @Override
    public List<Song> getAllSongs(@NonNull Context context) {
        return SongLoader.getAllSongs(context);
    }

    @NonNull
    @Override
    public List<Song> getSongs(@NonNull Context context, @NonNull String query) {
        return SongLoader.getSongs(context, query);
    }

    @NonNull
    @Override
    public Song getSong(@NonNull Context context, long songId) {
        return SongLoader.getSong(context, songId);
    }

    @NonNull
    @Override
    public List<Album> getAllAlbums(@NonNull Context context) {
        return AlbumLoader.getAllAlbums(context);
    }

    @NonNull
    @Override
    public List<Album> getAlbums(@NonNull Context context, @NonNull String query) {
        return AlbumLoader.getAlbums(context, query);
    }

    @NonNull
    @Override
    public Album getAlbum(@NonNull Context context, long albumId) {
        return AlbumLoader.getAlbum(context, albumId);
    }

    @NonNull
    @Override
    public List<Artist> getAllArtists(@NonNull Context context) {
        return ArtistLoader.getAllArtists(context);
    }

    @NonNull
    @Override
    public List<Artist> getArtists(@NonNull Context context, @NonNull String query) {
        return ArtistLoader.getArtists(context, query);
    }

    @NonNull
    @Override
    public Artist getArtist(@NonNull Context context, long artistId) {
        return ArtistLoader.getArtist(context, artistId);
    }

    @NonNull
    @Override
    public List<Genre> getAllGenres(@NonNull Context context) {
        return GenreLoader.getAllGenres(context);
    }

    @NonNull
    @Override
    public List<Song> getSongsForGenre(@NonNull Context context, long genreId) {
        return GenreLoader.getSongs(context, genreId);
    }

    @NonNull
    @Override
    public List<Playlist> getAllPlaylists(@NonNull Context context) {
        List<Playlist> playlists = new ArrayList<>();
        playlists.add(new LastAddedPlaylist(context));
        playlists.add(new HistoryPlaylist(context));
        playlists.add(new MyTopTracksPlaylist(context));
        playlists.addAll(PlaylistLoader.getAllPlaylists(context));
        return playlists;
    }

    @NonNull
    @Override
    public List<Song> getSongsForPlaylist(@NonNull Context context, @NonNull Playlist playlist) {
        if (playlist instanceof AbsCustomPlaylist) {
            return ((AbsCustomPlaylist) playlist).getSongs(context);
        }
        return new ArrayList<Song>(PlaylistSongLoader.getPlaylistSongList(context, playlist.id));
    }

    @NonNull
    @Override
    public String resolveSongUri(@NonNull Context context, @NonNull Song song) {
        return MusicUtil.getSongFileUri(song.id).toString();
    }

    @Nullable
    @Override
    public Object resolveCoverArt(@NonNull Context context, @NonNull Song song) {
        return MusicUtil.getMediaStoreAlbumCoverUri(song.albumId);
    }

    @Override
    public boolean supportsLocalFileActions() {
        return true;
    }
}
