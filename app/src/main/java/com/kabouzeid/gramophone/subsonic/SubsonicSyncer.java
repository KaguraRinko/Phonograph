package com.kabouzeid.gramophone.subsonic;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kabouzeid.gramophone.R;
import com.kabouzeid.gramophone.subsonic.rest.SubsonicRestClient;
import com.kabouzeid.gramophone.subsonic.rest.model.SubsonicAlbum;
import com.kabouzeid.gramophone.subsonic.rest.model.SubsonicArtist;
import com.kabouzeid.gramophone.subsonic.rest.model.SubsonicChild;
import com.kabouzeid.gramophone.subsonic.rest.model.SubsonicGenre;
import com.kabouzeid.gramophone.subsonic.rest.model.SubsonicIndex;
import com.kabouzeid.gramophone.subsonic.rest.model.SubsonicPlaylist;
import com.kabouzeid.gramophone.subsonic.rest.model.SubsonicResponse;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

public class SubsonicSyncer {
    private final Context context;
    private final SubsonicServer server;
    private final SubsonicRestClient client;
    @Nullable
    private final ProgressListener progressListener;

    public SubsonicSyncer(@NonNull Context context, @NonNull SubsonicServer server) {
        this(context, server, null);
    }

    public SubsonicSyncer(@NonNull Context context, @NonNull SubsonicServer server,
                          @Nullable ProgressListener progressListener) {
        this.context = context.getApplicationContext();
        this.server = server;
        this.progressListener = progressListener;
        client = new SubsonicRestClient(context, server);
    }

    public void sync() throws IOException, SubsonicException {
        try {
            doSync();
            SubsonicSyncState.finish(server.id, context.getString(R.string.sync_finished));
        } catch (IOException | SubsonicException e) {
            SubsonicSyncState.finish(server.id, context.getString(R.string.sync_failed_x, e.getMessage()));
            throw e;
        }
    }

    private void doSync() throws IOException, SubsonicException {
        publishProgress(context.getString(R.string.sync_progress_ping), 5);
        client.ping();

        List<SubsonicLibraryStore.CachedSong> songs = new ArrayList<>();
        publishProgress(context.getString(R.string.sync_progress_genres), 10);
        List<SubsonicLibraryStore.CachedGenre> genres = loadGenres();
        List<SubsonicLibraryStore.CachedPlaylist> playlists = new ArrayList<>();
        Set<String> visitedAlbums = new HashSet<>();

        publishProgress(context.getString(R.string.sync_progress_artists), 15);
        List<SubsonicArtist> artists = new ArrayList<>();
        SubsonicResponse artistsResponse = SubsonicRestClient.execute(client.getApiService().getArtists(client.createAuthParams()));
        if (artistsResponse.artists != null && artistsResponse.artists.indexes != null) {
            for (SubsonicIndex index : artistsResponse.artists.indexes) {
                if (index.artists == null) continue;
                artists.addAll(index.artists);
            }
            for (int i = 0; i < artists.size(); i++) {
                publishProgress(context.getString(R.string.sync_progress_artist_x_of_y, i + 1, artists.size()),
                        interpolateProgress(15, 75, i + 1, artists.size()));
                loadArtistSongs(artists.get(i), visitedAlbums, songs);
            }
        }

        publishProgress(context.getString(R.string.sync_progress_playlists), 78);
        SubsonicResponse playlistsResponse = SubsonicRestClient.execute(client.getApiService().getPlaylists(client.createAuthParams()));
        if (playlistsResponse.playlists != null && playlistsResponse.playlists.playlists != null) {
            List<SubsonicPlaylist> remotePlaylists = playlistsResponse.playlists.playlists;
            for (int i = 0; i < remotePlaylists.size(); i++) {
                publishProgress(context.getString(R.string.sync_progress_playlist_x_of_y, i + 1, remotePlaylists.size()),
                        interpolateProgress(78, 95, i + 1, remotePlaylists.size()));
                SubsonicPlaylist playlist = remotePlaylists.get(i);
                SubsonicLibraryStore.CachedPlaylist cachedPlaylist = loadPlaylist(playlist);
                if (cachedPlaylist != null) {
                    playlists.add(cachedPlaylist);
                }
            }
        }

        publishProgress(context.getString(R.string.sync_progress_saving), 98);
        SubsonicLibraryStore.getInstance(context).replaceLibrary(server.id, songs, genres, playlists);
        SubsonicServerStore.getInstance(context).updateLastSynced(server.id, System.currentTimeMillis());
    }

    private void publishProgress(@NonNull String message, int progress) {
        SubsonicSyncState.update(server.id, message, progress);
        if (progressListener != null) {
            progressListener.onProgress(message);
        }
    }

    private int interpolateProgress(int start, int end, int current, int total) {
        if (total <= 0) {
            return end;
        }
        return start + Math.round((end - start) * (current / (float) total));
    }

    public interface ProgressListener {
        void onProgress(@NonNull String message);
    }

    private void loadArtistSongs(@NonNull SubsonicArtist artist, @NonNull Set<String> visitedAlbums,
                                 @NonNull List<SubsonicLibraryStore.CachedSong> songs) throws IOException, SubsonicException {
        if (artist.id == null) {
            return;
        }
        SubsonicResponse artistResponse = SubsonicRestClient.execute(client.getApiService().getArtist(client.createAuthParams(), artist.id));
        if (artistResponse.artist == null || artistResponse.artist.albums == null) {
            return;
        }
        for (SubsonicAlbum album : artistResponse.artist.albums) {
            if (album.id == null || visitedAlbums.contains(album.id)) {
                continue;
            }
            visitedAlbums.add(album.id);
            loadAlbumSongs(album, songs);
        }
    }

    private void loadAlbumSongs(@NonNull SubsonicAlbum album, @NonNull List<SubsonicLibraryStore.CachedSong> songs) throws IOException, SubsonicException {
        SubsonicResponse albumResponse = SubsonicRestClient.execute(client.getApiService().getAlbum(client.createAuthParams(), album.id));
        if (albumResponse.album == null || albumResponse.album.songs == null) {
            return;
        }
        for (SubsonicChild child : albumResponse.album.songs) {
            SubsonicLibraryStore.CachedSong song = toCachedSong(child, albumResponse.album);
            if (song != null) {
                songs.add(song);
            }
        }
    }

    @NonNull
    private List<SubsonicLibraryStore.CachedGenre> loadGenres() throws IOException, SubsonicException {
        List<SubsonicLibraryStore.CachedGenre> genres = new ArrayList<>();
        SubsonicResponse genresResponse = SubsonicRestClient.execute(client.getApiService().getGenres(client.createAuthParams()));
        if (genresResponse.genres != null && genresResponse.genres.genres != null) {
            for (SubsonicGenre genre : genresResponse.genres.genres) {
                String name = genre.getDisplayName();
                if (!name.trim().isEmpty()) {
                    genres.add(new SubsonicLibraryStore.CachedGenre(name, safeInt(genre.songCount)));
                }
            }
        }
        return genres;
    }

    @Nullable
    private SubsonicLibraryStore.CachedPlaylist loadPlaylist(@NonNull SubsonicPlaylist playlist) throws IOException, SubsonicException {
        if (playlist.id == null) {
            return null;
        }
        SubsonicResponse playlistResponse = SubsonicRestClient.execute(client.getApiService().getPlaylist(client.createAuthParams(), playlist.id));
        SubsonicPlaylist detail = playlistResponse.playlist == null ? playlist : playlistResponse.playlist;
        List<String> songIds = new ArrayList<>();
        if (detail.songs != null) {
            for (SubsonicChild child : detail.songs) {
                if (child.id != null) {
                    songIds.add(child.id);
                }
            }
        }
        return new SubsonicLibraryStore.CachedPlaylist(playlist.id, safeString(detail.name, playlist.name), songIds);
    }

    @Nullable
    private SubsonicLibraryStore.CachedSong toCachedSong(@NonNull SubsonicChild child, @NonNull SubsonicAlbum album) {
        if (child.id == null) {
            return null;
        }
        String title = safeString(child.title, child.path);
        String albumName = safeString(child.album, album.name);
        String artistName = safeString(child.artist, album.artist);
        return new SubsonicLibraryStore.CachedSong(
                child.id,
                title,
                parseTrack(child.track),
                safeInt(child.year),
                safeInt(child.duration) * 1000L,
                safeString(child.albumId, album.id),
                albumName,
                safeString(child.artistId, album.artistId),
                artistName,
                child.genre,
                safeString(child.coverArt, album.coverArt),
                parseSubsonicTime(child.created),
                parseSubsonicTime(child.played),
                safeInt(child.playCount),
                safeInt(child.userRating),
                safeLong(child.size),
                safeString(child.contentType, null),
                safeString(child.suffix, null),
                safeString(child.path, null),
                safeInt(child.bitRate),
                safeInt(child.samplingRate)
        );
    }

    private long parseSubsonicTime(@Nullable String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }
        String[] patterns = new String[]{
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSSX",
                "yyyy-MM-dd'T'HH:mm:ssX"
        };
        for (String pattern : patterns) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
                format.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date date = format.parse(value);
                if (date != null) {
                    return date.getTime() / 1000;
                }
            } catch (Exception ignored) {
            }
        }
        return 0;
    }

    private int parseTrack(@Nullable String track) {
        if (track == null) {
            return 0;
        }
        try {
            return Integer.parseInt(track);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int safeInt(@Nullable Integer value) {
        return value == null ? 0 : value;
    }

    private long safeLong(@Nullable Long value) {
        return value == null ? 0 : value;
    }

    @NonNull
    private String safeString(@Nullable String primary, @Nullable String fallback) {
        if (primary != null && !primary.trim().isEmpty()) {
            return primary;
        }
        if (fallback != null && !fallback.trim().isEmpty()) {
            return fallback;
        }
        return "";
    }
}
