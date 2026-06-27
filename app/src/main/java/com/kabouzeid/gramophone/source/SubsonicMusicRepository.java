package com.kabouzeid.gramophone.source;

import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kabouzeid.gramophone.loader.TopAndRecentlyPlayedTracksLoader;
import com.kabouzeid.gramophone.loader.AlbumLoader;
import com.kabouzeid.gramophone.loader.ArtistLoader;
import com.kabouzeid.gramophone.model.Album;
import com.kabouzeid.gramophone.model.Artist;
import com.kabouzeid.gramophone.model.Genre;
import com.kabouzeid.gramophone.model.Playlist;
import com.kabouzeid.gramophone.model.Song;
import com.kabouzeid.gramophone.model.smartplaylist.HistoryPlaylist;
import com.kabouzeid.gramophone.model.smartplaylist.LastAddedPlaylist;
import com.kabouzeid.gramophone.model.smartplaylist.MyTopTracksPlaylist;
import com.kabouzeid.gramophone.model.smartplaylist.TopRatedPlaylist;
import com.kabouzeid.gramophone.glide.subsonic.SubsonicCoverArt;
import com.kabouzeid.gramophone.provider.HistoryStore;
import com.kabouzeid.gramophone.provider.SongPlayCountStore;
import com.kabouzeid.gramophone.subsonic.SubsonicLibraryStore;
import com.kabouzeid.gramophone.subsonic.SubsonicServer;
import com.kabouzeid.gramophone.subsonic.SubsonicUri;
import com.kabouzeid.gramophone.subsonic.rest.SubsonicRestClient;
import com.kabouzeid.gramophone.util.PreferenceUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SubsonicMusicRepository implements MusicRepository {
    private final SubsonicServer server;
    private final MediaSource source;

    public SubsonicMusicRepository(@NonNull SubsonicServer server) {
        this.server = server;
        source = MediaSourceManager.toMediaSource(server);
    }

    @NonNull
    @Override
    public MediaSource getSource() {
        return source;
    }

    @NonNull
    @Override
    public List<Song> getAllSongs(@NonNull Context context) {
        return SubsonicLibraryStore.getInstance(context).getAllSongs(server.id);
    }

    @NonNull
    @Override
    public List<Song> getSongs(@NonNull Context context, @NonNull String query) {
        return SubsonicLibraryStore.getInstance(context).searchSongs(server.id, query);
    }

    @NonNull
    @Override
    public Song getSong(@NonNull Context context, long songId) {
        return SubsonicLibraryStore.getInstance(context).getSong(server.id, songId);
    }

    @NonNull
    @Override
    public List<Album> getAllAlbums(@NonNull Context context) {
        return AlbumLoader.splitIntoAlbums(getAllSongs(context));
    }

    @NonNull
    @Override
    public List<Album> getAlbums(@NonNull Context context, @NonNull String query) {
        String lowerCaseQuery = query.toLowerCase(Locale.ROOT);
        List<Album> results = new ArrayList<>();
        for (Album album : getAllAlbums(context)) {
            if (album.getTitle().toLowerCase(Locale.ROOT).contains(lowerCaseQuery)
                    || album.getArtistName().toLowerCase(Locale.ROOT).contains(lowerCaseQuery)) {
                results.add(album);
            }
        }
        return results;
    }

    @NonNull
    @Override
    public Album getAlbum(@NonNull Context context, long albumId) {
        return new Album(SubsonicLibraryStore.getInstance(context).getSongsForAlbum(server.id, albumId));
    }

    @NonNull
    @Override
    public List<Artist> getAllArtists(@NonNull Context context) {
        return ArtistLoader.splitIntoArtists(getAllAlbums(context));
    }

    @NonNull
    @Override
    public List<Artist> getArtists(@NonNull Context context, @NonNull String query) {
        String lowerCaseQuery = query.toLowerCase(Locale.ROOT);
        List<Artist> results = new ArrayList<>();
        for (Artist artist : getAllArtists(context)) {
            if (artist.getName().toLowerCase(Locale.ROOT).contains(lowerCaseQuery)) {
                results.add(artist);
            }
        }
        return results;
    }

    @NonNull
    @Override
    public Artist getArtist(@NonNull Context context, long artistId) {
        return new Artist(AlbumLoader.splitIntoAlbums(SubsonicLibraryStore.getInstance(context).getSongsForArtist(server.id, artistId)));
    }

    @NonNull
    @Override
    public List<Genre> getAllGenres(@NonNull Context context) {
        return SubsonicLibraryStore.getInstance(context).getAllGenres(server.id);
    }

    @NonNull
    @Override
    public List<Song> getSongsForGenre(@NonNull Context context, long genreId) {
        return SubsonicLibraryStore.getInstance(context).getSongsForGenre(server.id, genreId);
    }

    @NonNull
    @Override
    public List<Playlist> getAllPlaylists(@NonNull Context context) {
        List<Playlist> playlists = new ArrayList<>();
        playlists.add(new TopRatedPlaylist(context));
        playlists.add(new LastAddedPlaylist(context));
        playlists.add(new HistoryPlaylist(context));
        playlists.add(new MyTopTracksPlaylist(context));
        playlists.addAll(SubsonicLibraryStore.getInstance(context).getAllPlaylists(server.id));
        return playlists;
    }

    @NonNull
    @Override
    public List<Song> getSongsForPlaylist(@NonNull Context context, @NonNull Playlist playlist) {
        SubsonicLibraryStore store = SubsonicLibraryStore.getInstance(context);
        if (playlist instanceof TopRatedPlaylist) {
            return store.getTopRatedSongs(server.id);
        } else if (playlist instanceof LastAddedPlaylist) {
            return store.getLastAddedSongs(server.id, PreferenceUtil.getInstance(context).getLastAddedCutoff());
        } else if (playlist instanceof HistoryPlaylist) {
            List<Song> songs = store.getRecentlyPlayedSongs(server.id);
            return songs.isEmpty() ? getRecentlyPlayedSongsFromLocalHistory(context) : songs;
        } else if (playlist instanceof MyTopTracksPlaylist) {
            List<Song> songs = store.getMostPlayedSongs(server.id);
            return songs.isEmpty() ? getMostPlayedSongsFromLocalHistory(context) : songs;
        }
        return store.getSongsForPlaylist(server.id, playlist.id);
    }

    @NonNull
    private List<Song> getRecentlyPlayedSongsFromLocalHistory(@NonNull Context context) {
        Cursor cursor = HistoryStore.getInstance(context).queryRecentIds();
        return getSongsFromLocalIdCursor(context, cursor, HistoryStore.RecentStoreColumns.ID);
    }

    @NonNull
    private List<Song> getMostPlayedSongsFromLocalHistory(@NonNull Context context) {
        Cursor cursor = SongPlayCountStore.getInstance(context)
                .getTopPlayedResults(TopAndRecentlyPlayedTracksLoader.NUMBER_OF_TOP_TRACKS);
        return getSongsFromLocalIdCursor(context, cursor, SongPlayCountStore.SongPlayCountColumns.ID);
    }

    @NonNull
    private List<Song> getSongsFromLocalIdCursor(@NonNull Context context, @Nullable Cursor cursor,
                                                 @NonNull String idColumnName) {
        List<Song> songs = new ArrayList<>();
        if (cursor == null) {
            return songs;
        }
        try {
            int idColumn = cursor.getColumnIndex(idColumnName);
            if (idColumn < 0 || !cursor.moveToFirst()) {
                return songs;
            }
            SubsonicLibraryStore store = SubsonicLibraryStore.getInstance(context);
            do {
                Song song = store.getSong(server.id, cursor.getLong(idColumn));
                if (song.id != Song.EMPTY_SONG.id) {
                    songs.add(song);
                }
            } while (cursor.moveToNext());
            return songs;
        } finally {
            cursor.close();
        }
    }

    @NonNull
    @Override
    public String resolveSongUri(@NonNull Context context, @NonNull Song song) {
        String remoteSongId = SubsonicUri.getRemoteSongId(song.data);
        return new SubsonicRestClient(context, server).buildStreamUrl(remoteSongId == null ? song.data : remoteSongId);
    }

    @Nullable
    @Override
    public Object resolveCoverArt(@NonNull Context context, @NonNull Song song) {
        String coverArt = SubsonicLibraryStore.getInstance(context).getCoverArt(server.id, song.id);
        return coverArt == null || coverArt.trim().isEmpty()
                ? null
                : new SubsonicCoverArt(server, coverArt);
    }

    @Override
    public boolean supportsLocalFileActions() {
        return false;
    }
}
