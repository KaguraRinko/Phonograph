package com.kabouzeid.gramophone.source;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kabouzeid.gramophone.loader.AlbumLoader;
import com.kabouzeid.gramophone.loader.ArtistLoader;
import com.kabouzeid.gramophone.model.Album;
import com.kabouzeid.gramophone.model.Artist;
import com.kabouzeid.gramophone.model.Genre;
import com.kabouzeid.gramophone.model.Playlist;
import com.kabouzeid.gramophone.model.Song;
import com.kabouzeid.gramophone.subsonic.SubsonicLibraryStore;
import com.kabouzeid.gramophone.subsonic.SubsonicServer;
import com.kabouzeid.gramophone.subsonic.SubsonicUri;
import com.kabouzeid.gramophone.subsonic.rest.SubsonicRestClient;

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
        return SubsonicLibraryStore.getInstance(context).getAllPlaylists(server.id);
    }

    @NonNull
    @Override
    public List<Song> getSongsForPlaylist(@NonNull Context context, @NonNull Playlist playlist) {
        return SubsonicLibraryStore.getInstance(context).getSongsForPlaylist(server.id, playlist.id);
    }

    @NonNull
    @Override
    public String resolveSongUri(@NonNull Context context, @NonNull Song song) {
        String remoteSongId = SubsonicUri.getRemoteSongId(song.data);
        return new SubsonicRestClient(context, server).buildStreamUrl(remoteSongId == null ? song.data : remoteSongId);
    }

    @Nullable
    @Override
    public String resolveCoverArtUri(@NonNull Context context, @NonNull Song song) {
        String coverArt = SubsonicLibraryStore.getInstance(context).getCoverArt(server.id, song.id);
        return coverArt == null || coverArt.trim().isEmpty()
                ? null
                : new SubsonicRestClient(context, server).buildCoverArtUrl(coverArt);
    }

    @Override
    public boolean supportsLocalFileActions() {
        return false;
    }
}
