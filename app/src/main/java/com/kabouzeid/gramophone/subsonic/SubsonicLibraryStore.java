package com.kabouzeid.gramophone.subsonic;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kabouzeid.gramophone.model.Genre;
import com.kabouzeid.gramophone.model.Playlist;
import com.kabouzeid.gramophone.model.Song;

import java.util.ArrayList;
import java.util.List;

public class SubsonicLibraryStore extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "subsonic_library.db";
    private static final int VERSION = 3;

    private static final String TABLE_ID_MAP = "id_map";
    private static final String TABLE_SONGS = "songs";
    private static final String TABLE_GENRES = "genres";
    private static final String TABLE_PLAYLISTS = "playlists";
    private static final String TABLE_PLAYLIST_SONGS = "playlist_songs";

    private static final String COLUMN_SERVER_ID = "server_id";
    private static final String COLUMN_ENTITY_TYPE = "entity_type";
    private static final String COLUMN_REMOTE_ID = "remote_id";
    private static final String COLUMN_LOCAL_ID = "local_id";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_TRACK = "track";
    private static final String COLUMN_YEAR = "year";
    private static final String COLUMN_DURATION = "duration";
    private static final String COLUMN_DATA = "data";
    private static final String COLUMN_DATE_MODIFIED = "date_modified";
    private static final String COLUMN_ALBUM_ID = "album_id";
    private static final String COLUMN_ALBUM = "album";
    private static final String COLUMN_ARTIST_ID = "artist_id";
    private static final String COLUMN_ARTIST = "artist";
    private static final String COLUMN_GENRE = "genre";
    private static final String COLUMN_COVER_ART = "cover_art";
    private static final String COLUMN_PLAYED = "played";
    private static final String COLUMN_PLAY_COUNT = "play_count";
    private static final String COLUMN_USER_RATING = "user_rating";
    private static final String COLUMN_FILE_SIZE = "file_size";
    private static final String COLUMN_CONTENT_TYPE = "content_type";
    private static final String COLUMN_SUFFIX = "suffix";
    private static final String COLUMN_PATH = "path";
    private static final String COLUMN_BIT_RATE = "bit_rate";
    private static final String COLUMN_SAMPLING_RATE = "sampling_rate";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_SONG_COUNT = "song_count";
    private static final String COLUMN_PLAYLIST_ID = "playlist_id";
    private static final String COLUMN_SONG_ID = "song_id";
    private static final String COLUMN_POSITION = "position";

    private static final String TYPE_SONG = "song";
    private static final String TYPE_ALBUM = "album";
    private static final String TYPE_ARTIST = "artist";
    private static final String TYPE_GENRE = "genre";
    private static final String TYPE_PLAYLIST = "playlist";

    private static final long FIRST_REMOTE_ID = -1000;

    @Nullable
    private static SubsonicLibraryStore sInstance;

    private SubsonicLibraryStore(@NonNull Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, VERSION);
    }

    @NonNull
    public static synchronized SubsonicLibraryStore getInstance(@NonNull Context context) {
        if (sInstance == null) {
            sInstance = new SubsonicLibraryStore(context);
        }
        return sInstance;
    }

    @Override
    public void onCreate(@NonNull SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ID_MAP + " ("
                + COLUMN_SERVER_ID + " INTEGER NOT NULL,"
                + COLUMN_ENTITY_TYPE + " TEXT NOT NULL,"
                + COLUMN_REMOTE_ID + " TEXT NOT NULL,"
                + COLUMN_LOCAL_ID + " INTEGER NOT NULL UNIQUE,"
                + "PRIMARY KEY(" + COLUMN_SERVER_ID + "," + COLUMN_ENTITY_TYPE + "," + COLUMN_REMOTE_ID + ")"
                + ");");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_SONGS + " ("
                + COLUMN_SERVER_ID + " INTEGER NOT NULL,"
                + COLUMN_LOCAL_ID + " INTEGER PRIMARY KEY,"
                + COLUMN_REMOTE_ID + " TEXT NOT NULL,"
                + COLUMN_TITLE + " TEXT NOT NULL,"
                + COLUMN_TRACK + " INTEGER NOT NULL,"
                + COLUMN_YEAR + " INTEGER NOT NULL,"
                + COLUMN_DURATION + " INTEGER NOT NULL,"
                + COLUMN_DATA + " TEXT NOT NULL,"
                + COLUMN_DATE_MODIFIED + " INTEGER NOT NULL,"
                + COLUMN_ALBUM_ID + " INTEGER NOT NULL,"
                + COLUMN_ALBUM + " TEXT NOT NULL,"
                + COLUMN_ARTIST_ID + " INTEGER NOT NULL,"
                + COLUMN_ARTIST + " TEXT NOT NULL,"
                + COLUMN_GENRE + " TEXT,"
                + COLUMN_COVER_ART + " TEXT,"
                + COLUMN_PLAYED + " INTEGER NOT NULL DEFAULT 0,"
                + COLUMN_PLAY_COUNT + " INTEGER NOT NULL DEFAULT 0,"
                + COLUMN_USER_RATING + " INTEGER NOT NULL DEFAULT 0,"
                + COLUMN_FILE_SIZE + " INTEGER NOT NULL DEFAULT 0,"
                + COLUMN_CONTENT_TYPE + " TEXT,"
                + COLUMN_SUFFIX + " TEXT,"
                + COLUMN_PATH + " TEXT,"
                + COLUMN_BIT_RATE + " INTEGER NOT NULL DEFAULT 0,"
                + COLUMN_SAMPLING_RATE + " INTEGER NOT NULL DEFAULT 0"
                + ");");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_GENRES + " ("
                + COLUMN_SERVER_ID + " INTEGER NOT NULL,"
                + COLUMN_LOCAL_ID + " INTEGER PRIMARY KEY,"
                + COLUMN_NAME + " TEXT NOT NULL,"
                + COLUMN_SONG_COUNT + " INTEGER NOT NULL"
                + ");");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_PLAYLISTS + " ("
                + COLUMN_SERVER_ID + " INTEGER NOT NULL,"
                + COLUMN_LOCAL_ID + " INTEGER PRIMARY KEY,"
                + COLUMN_REMOTE_ID + " TEXT NOT NULL,"
                + COLUMN_NAME + " TEXT NOT NULL,"
                + COLUMN_SONG_COUNT + " INTEGER NOT NULL"
                + ");");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_PLAYLIST_SONGS + " ("
                + COLUMN_SERVER_ID + " INTEGER NOT NULL,"
                + COLUMN_PLAYLIST_ID + " INTEGER NOT NULL,"
                + COLUMN_SONG_ID + " INTEGER NOT NULL,"
                + COLUMN_POSITION + " INTEGER NOT NULL,"
                + "PRIMARY KEY(" + COLUMN_SERVER_ID + "," + COLUMN_PLAYLIST_ID + "," + COLUMN_POSITION + ")"
                + ");");
    }

    @Override
    public void onUpgrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_SONGS + " ADD COLUMN "
                    + COLUMN_PLAYED + " INTEGER NOT NULL DEFAULT 0;");
            db.execSQL("ALTER TABLE " + TABLE_SONGS + " ADD COLUMN "
                    + COLUMN_PLAY_COUNT + " INTEGER NOT NULL DEFAULT 0;");
            db.execSQL("ALTER TABLE " + TABLE_SONGS + " ADD COLUMN "
                    + COLUMN_USER_RATING + " INTEGER NOT NULL DEFAULT 0;");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_SONGS + " ADD COLUMN "
                    + COLUMN_FILE_SIZE + " INTEGER NOT NULL DEFAULT 0;");
            db.execSQL("ALTER TABLE " + TABLE_SONGS + " ADD COLUMN "
                    + COLUMN_CONTENT_TYPE + " TEXT;");
            db.execSQL("ALTER TABLE " + TABLE_SONGS + " ADD COLUMN "
                    + COLUMN_SUFFIX + " TEXT;");
            db.execSQL("ALTER TABLE " + TABLE_SONGS + " ADD COLUMN "
                    + COLUMN_PATH + " TEXT;");
            db.execSQL("ALTER TABLE " + TABLE_SONGS + " ADD COLUMN "
                    + COLUMN_BIT_RATE + " INTEGER NOT NULL DEFAULT 0;");
            db.execSQL("ALTER TABLE " + TABLE_SONGS + " ADD COLUMN "
                    + COLUMN_SAMPLING_RATE + " INTEGER NOT NULL DEFAULT 0;");
            return;
        }
        dropTables(db);
        onCreate(db);
    }

    @Override
    public void onDowngrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
        dropTables(db);
        onCreate(db);
    }

    private void dropTables(@NonNull SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PLAYLIST_SONGS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PLAYLISTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GENRES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SONGS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ID_MAP);
    }

    public synchronized void replaceLibrary(long serverId, @NonNull List<CachedSong> songs,
                                            @NonNull List<CachedGenre> genres,
                                            @NonNull List<CachedPlaylist> playlists) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        long syncTime = System.currentTimeMillis() / 1000;
        try {
            clearServerData(db, serverId);
            for (CachedSong song : songs) {
                insertSong(db, serverId, song, syncTime);
            }
            for (CachedGenre genre : genres) {
                insertGenre(db, serverId, genre);
            }
            for (CachedPlaylist playlist : playlists) {
                insertPlaylist(db, serverId, playlist);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public synchronized void clearServerData(long serverId) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            clearServerData(db, serverId);
            db.delete(TABLE_ID_MAP, COLUMN_SERVER_ID + "=?", new String[]{String.valueOf(serverId)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void clearServerData(@NonNull SQLiteDatabase db, long serverId) {
        String[] args = new String[]{String.valueOf(serverId)};
        db.delete(TABLE_PLAYLIST_SONGS, COLUMN_SERVER_ID + "=?", args);
        db.delete(TABLE_PLAYLISTS, COLUMN_SERVER_ID + "=?", args);
        db.delete(TABLE_GENRES, COLUMN_SERVER_ID + "=?", args);
        db.delete(TABLE_SONGS, COLUMN_SERVER_ID + "=?", args);
    }

    @NonNull
    public synchronized List<Song> getAllSongs(long serverId) {
        return getSongs(serverId, null, null, COLUMN_TITLE + " COLLATE NOCASE");
    }

    @NonNull
    public synchronized List<Song> searchSongs(long serverId, @NonNull String query) {
        return getSongs(serverId, COLUMN_TITLE + " LIKE ?", new String[]{"%" + query + "%"}, COLUMN_TITLE + " COLLATE NOCASE");
    }

    @NonNull
    public synchronized Song getSong(long serverId, long songId) {
        List<Song> songs = getSongs(serverId, COLUMN_LOCAL_ID + "=?", new String[]{String.valueOf(songId)}, null);
        return songs.isEmpty() ? Song.EMPTY_SONG : songs.get(0);
    }

    @NonNull
    public synchronized List<Song> getSongsForAlbum(long serverId, long albumId) {
        return getSongs(serverId, COLUMN_ALBUM_ID + "=?", new String[]{String.valueOf(albumId)}, COLUMN_TRACK + "," + COLUMN_TITLE + " COLLATE NOCASE");
    }

    @NonNull
    public synchronized List<Song> getSongsForArtist(long serverId, long artistId) {
        return getSongs(serverId, COLUMN_ARTIST_ID + "=?", new String[]{String.valueOf(artistId)}, COLUMN_ALBUM + " COLLATE NOCASE," + COLUMN_TRACK);
    }

    @NonNull
    public synchronized List<Genre> getAllGenres(long serverId) {
        List<Genre> genres = new ArrayList<>();
        Cursor cursor = getReadableDatabase().query(
                TABLE_GENRES,
                null,
                COLUMN_SERVER_ID + "=?",
                new String[]{String.valueOf(serverId)},
                null,
                null,
                COLUMN_NAME + " COLLATE NOCASE"
        );
        try {
            if (cursor.moveToFirst()) {
                do {
                    genres.add(new Genre(
                            cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LOCAL_ID)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                            cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SONG_COUNT))
                    ));
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }
        return genres;
    }

    @NonNull
    public synchronized List<Song> getSongsForGenre(long serverId, long genreId) {
        Cursor cursor = getReadableDatabase().query(
                TABLE_GENRES,
                new String[]{COLUMN_NAME},
                COLUMN_SERVER_ID + "=? AND " + COLUMN_LOCAL_ID + "=?",
                new String[]{String.valueOf(serverId), String.valueOf(genreId)},
                null,
                null,
                null
        );
        try {
            if (cursor.moveToFirst()) {
                String genre = cursor.getString(0);
                return getSongs(serverId, COLUMN_GENRE + "=?", new String[]{genre}, COLUMN_TITLE + " COLLATE NOCASE");
            }
            return new ArrayList<>();
        } finally {
            cursor.close();
        }
    }

    @NonNull
    public synchronized List<Playlist> getAllPlaylists(long serverId) {
        List<Playlist> playlists = new ArrayList<>();
        Cursor cursor = getReadableDatabase().query(
                TABLE_PLAYLISTS,
                null,
                COLUMN_SERVER_ID + "=?",
                new String[]{String.valueOf(serverId)},
                null,
                null,
                COLUMN_NAME + " COLLATE NOCASE"
        );
        try {
            if (cursor.moveToFirst()) {
                do {
                    playlists.add(new Playlist(
                            cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LOCAL_ID)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME))
                    ));
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }
        return playlists;
    }

    @NonNull
    public synchronized List<Song> getSongsForPlaylist(long serverId, long playlistId) {
        List<Song> songs = new ArrayList<>();
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT s.* FROM " + TABLE_PLAYLIST_SONGS + " ps "
                        + "INNER JOIN " + TABLE_SONGS + " s ON ps." + COLUMN_SONG_ID + "=s." + COLUMN_LOCAL_ID + " "
                        + "WHERE ps." + COLUMN_SERVER_ID + "=? AND ps." + COLUMN_PLAYLIST_ID + "=? "
                        + "ORDER BY ps." + COLUMN_POSITION,
                new String[]{String.valueOf(serverId), String.valueOf(playlistId)}
        );
        try {
            if (cursor.moveToFirst()) {
                do {
                    songs.add(readSong(cursor));
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }
        return songs;
    }

    @NonNull
    public synchronized List<Song> getTopRatedSongs(long serverId) {
        return getSongs(
                serverId,
                COLUMN_USER_RATING + ">0",
                null,
                COLUMN_USER_RATING + " DESC," + COLUMN_TITLE + " COLLATE NOCASE"
        );
    }

    @NonNull
    public synchronized List<Song> getLastAddedSongs(long serverId, long cutoff) {
        return getSongs(
                serverId,
                COLUMN_DATE_MODIFIED + ">?",
                new String[]{String.valueOf(cutoff)},
                COLUMN_DATE_MODIFIED + " DESC," + COLUMN_TITLE + " COLLATE NOCASE"
        );
    }

    @NonNull
    public synchronized List<Song> getRecentlyPlayedSongs(long serverId) {
        return getSongs(
                serverId,
                COLUMN_PLAYED + ">0",
                null,
                COLUMN_PLAYED + " DESC," + COLUMN_TITLE + " COLLATE NOCASE"
        );
    }

    @NonNull
    public synchronized List<Song> getMostPlayedSongs(long serverId) {
        return getSongs(
                serverId,
                COLUMN_PLAY_COUNT + ">0",
                null,
                COLUMN_PLAY_COUNT + " DESC," + COLUMN_TITLE + " COLLATE NOCASE"
        );
    }

    @Nullable
    public synchronized CachedSongDetails getSongDetails(long serverId, long songId) {
        Cursor cursor = getReadableDatabase().query(
                TABLE_SONGS,
                new String[]{
                        COLUMN_TITLE,
                        COLUMN_PATH,
                        COLUMN_FILE_SIZE,
                        COLUMN_CONTENT_TYPE,
                        COLUMN_SUFFIX,
                        COLUMN_BIT_RATE,
                        COLUMN_SAMPLING_RATE
                },
                COLUMN_SERVER_ID + "=? AND " + COLUMN_LOCAL_ID + "=?",
                new String[]{String.valueOf(serverId), String.valueOf(songId)},
                null,
                null,
                null
        );
        try {
            if (!cursor.moveToFirst()) {
                return null;
            }
            return new CachedSongDetails(
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PATH)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_FILE_SIZE)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT_TYPE)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SUFFIX)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_BIT_RATE)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SAMPLING_RATE))
            );
        } finally {
            cursor.close();
        }
    }

    @Nullable
    public synchronized String getCoverArt(long serverId, long songId) {
        Cursor cursor = getReadableDatabase().query(
                TABLE_SONGS,
                new String[]{COLUMN_COVER_ART},
                COLUMN_SERVER_ID + "=? AND " + COLUMN_LOCAL_ID + "=?",
                new String[]{String.valueOf(serverId), String.valueOf(songId)},
                null,
                null,
                null
        );
        try {
            return cursor.moveToFirst() ? cursor.getString(0) : null;
        } finally {
            cursor.close();
        }
    }

    @NonNull
    public synchronized List<String> getDistinctCoverArtIds(long serverId) {
        List<String> coverArtIds = new ArrayList<>();
        Cursor cursor = getReadableDatabase().query(
                true,
                TABLE_SONGS,
                new String[]{COLUMN_COVER_ART},
                COLUMN_SERVER_ID + "=? AND " + COLUMN_COVER_ART + " IS NOT NULL AND TRIM(" + COLUMN_COVER_ART + ")<>''",
                new String[]{String.valueOf(serverId)},
                null,
                null,
                COLUMN_COVER_ART + " COLLATE NOCASE",
                null
        );
        try {
            if (cursor.moveToFirst()) {
                do {
                    coverArtIds.add(cursor.getString(0));
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }
        return coverArtIds;
    }

    @NonNull
    private List<Song> getSongs(long serverId, @Nullable String selection,
                                @Nullable String[] selectionArgs, @Nullable String orderBy) {
        List<Song> songs = new ArrayList<>();
        String where = COLUMN_SERVER_ID + "=?";
        List<String> args = new ArrayList<>();
        args.add(String.valueOf(serverId));
        if (selection != null) {
            where += " AND " + selection;
            if (selectionArgs != null) {
                for (String arg : selectionArgs) {
                    args.add(arg);
                }
            }
        }
        Cursor cursor = getReadableDatabase().query(
                TABLE_SONGS,
                null,
                where,
                args.toArray(new String[0]),
                null,
                null,
                orderBy
        );
        try {
            if (cursor.moveToFirst()) {
                do {
                    songs.add(readSong(cursor));
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }
        return songs;
    }

    private void insertSong(@NonNull SQLiteDatabase db, long serverId, @NonNull CachedSong song, long syncTime) {
        long songId = getOrCreateLocalId(db, serverId, TYPE_SONG, song.remoteId);
        long albumId = getOrCreateLocalId(db, serverId, TYPE_ALBUM, emptyFallback(song.remoteAlbumId, song.albumName));
        long artistId = getOrCreateLocalId(db, serverId, TYPE_ARTIST, emptyFallback(song.remoteArtistId, song.artistName));

        ContentValues values = new ContentValues();
        values.put(COLUMN_SERVER_ID, serverId);
        values.put(COLUMN_LOCAL_ID, songId);
        values.put(COLUMN_REMOTE_ID, song.remoteId);
        values.put(COLUMN_TITLE, song.title);
        values.put(COLUMN_TRACK, song.trackNumber);
        values.put(COLUMN_YEAR, song.year);
        values.put(COLUMN_DURATION, song.duration);
        values.put(COLUMN_DATA, SubsonicUri.forSong(serverId, song.remoteId));
        values.put(COLUMN_DATE_MODIFIED, song.created > 0 ? song.created : syncTime);
        values.put(COLUMN_ALBUM_ID, albumId);
        values.put(COLUMN_ALBUM, song.albumName);
        values.put(COLUMN_ARTIST_ID, artistId);
        values.put(COLUMN_ARTIST, song.artistName);
        values.put(COLUMN_GENRE, song.genre);
        values.put(COLUMN_COVER_ART, song.coverArt);
        values.put(COLUMN_PLAYED, song.played);
        values.put(COLUMN_PLAY_COUNT, song.playCount);
        values.put(COLUMN_USER_RATING, song.userRating);
        values.put(COLUMN_FILE_SIZE, song.fileSize);
        values.put(COLUMN_CONTENT_TYPE, song.contentType);
        values.put(COLUMN_SUFFIX, song.suffix);
        values.put(COLUMN_PATH, song.path);
        values.put(COLUMN_BIT_RATE, song.bitRate);
        values.put(COLUMN_SAMPLING_RATE, song.samplingRate);
        db.insert(TABLE_SONGS, null, values);
    }

    private void insertGenre(@NonNull SQLiteDatabase db, long serverId, @NonNull CachedGenre genre) {
        if (genre.name.trim().isEmpty()) {
            return;
        }
        long genreId = getOrCreateLocalId(db, serverId, TYPE_GENRE, genre.name);
        ContentValues values = new ContentValues();
        values.put(COLUMN_SERVER_ID, serverId);
        values.put(COLUMN_LOCAL_ID, genreId);
        values.put(COLUMN_NAME, genre.name);
        values.put(COLUMN_SONG_COUNT, genre.songCount);
        db.insert(TABLE_GENRES, null, values);
    }

    private void insertPlaylist(@NonNull SQLiteDatabase db, long serverId, @NonNull CachedPlaylist playlist) {
        long playlistId = getOrCreateLocalId(db, serverId, TYPE_PLAYLIST, playlist.remoteId);
        ContentValues values = new ContentValues();
        values.put(COLUMN_SERVER_ID, serverId);
        values.put(COLUMN_LOCAL_ID, playlistId);
        values.put(COLUMN_REMOTE_ID, playlist.remoteId);
        values.put(COLUMN_NAME, playlist.name);
        values.put(COLUMN_SONG_COUNT, playlist.songRemoteIds.size());
        db.insert(TABLE_PLAYLISTS, null, values);

        int position = 0;
        for (String remoteSongId : playlist.songRemoteIds) {
            Long songId = getLocalId(db, serverId, TYPE_SONG, remoteSongId);
            if (songId == null) {
                continue;
            }
            ContentValues songValues = new ContentValues();
            songValues.put(COLUMN_SERVER_ID, serverId);
            songValues.put(COLUMN_PLAYLIST_ID, playlistId);
            songValues.put(COLUMN_SONG_ID, songId);
            songValues.put(COLUMN_POSITION, position++);
            db.insert(TABLE_PLAYLIST_SONGS, null, songValues);
        }
    }

    @NonNull
    private Song readSong(@NonNull Cursor cursor) {
        return new Song(
                cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LOCAL_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)),
                cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TRACK)),
                cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_YEAR)),
                cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DURATION)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATA)),
                cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DATE_MODIFIED)),
                cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ALBUM_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ALBUM)),
                cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ARTIST_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ARTIST))
        );
    }

    @NonNull
    private String emptyFallback(@Nullable String value, @NonNull String fallback) {
        if (value != null && !value.trim().isEmpty()) {
            return value;
        }
        return fallback;
    }

    private long getOrCreateLocalId(@NonNull SQLiteDatabase db, long serverId,
                                    @NonNull String entityType, @NonNull String remoteId) {
        Long existingId = getLocalId(db, serverId, entityType, remoteId);
        if (existingId != null) {
            return existingId;
        }
        long localId = nextLocalId(db);
        ContentValues values = new ContentValues();
        values.put(COLUMN_SERVER_ID, serverId);
        values.put(COLUMN_ENTITY_TYPE, entityType);
        values.put(COLUMN_REMOTE_ID, remoteId);
        values.put(COLUMN_LOCAL_ID, localId);
        db.insert(TABLE_ID_MAP, null, values);
        return localId;
    }

    @Nullable
    private Long getLocalId(@NonNull SQLiteDatabase db, long serverId,
                            @NonNull String entityType, @NonNull String remoteId) {
        Cursor cursor = db.query(
                TABLE_ID_MAP,
                new String[]{COLUMN_LOCAL_ID},
                COLUMN_SERVER_ID + "=? AND " + COLUMN_ENTITY_TYPE + "=? AND " + COLUMN_REMOTE_ID + "=?",
                new String[]{String.valueOf(serverId), entityType, remoteId},
                null,
                null,
                null
        );
        try {
            return cursor.moveToFirst() ? cursor.getLong(0) : null;
        } finally {
            cursor.close();
        }
    }

    private long nextLocalId(@NonNull SQLiteDatabase db) {
        Cursor cursor = db.rawQuery("SELECT MIN(" + COLUMN_LOCAL_ID + ") FROM " + TABLE_ID_MAP, null);
        try {
            if (cursor.moveToFirst() && !cursor.isNull(0)) {
                return Math.min(FIRST_REMOTE_ID, cursor.getLong(0) - 1);
            }
            return FIRST_REMOTE_ID;
        } finally {
            cursor.close();
        }
    }

    public static class CachedSong {
        public final String remoteId;
        public final String title;
        public final int trackNumber;
        public final int year;
        public final long duration;
        public final String remoteAlbumId;
        public final String albumName;
        public final String remoteArtistId;
        public final String artistName;
        public final String genre;
        public final String coverArt;
        public final long created;
        public final long played;
        public final int playCount;
        public final int userRating;
        public final long fileSize;
        public final String contentType;
        public final String suffix;
        public final String path;
        public final int bitRate;
        public final int samplingRate;

        public CachedSong(@NonNull String remoteId, @NonNull String title, int trackNumber, int year,
                          long duration, @Nullable String remoteAlbumId, @NonNull String albumName,
                          @Nullable String remoteArtistId, @NonNull String artistName,
                          @Nullable String genre, @Nullable String coverArt, long created,
                          long played, int playCount, int userRating, long fileSize,
                          @Nullable String contentType, @Nullable String suffix,
                          @Nullable String path, int bitRate, int samplingRate) {
            this.remoteId = remoteId;
            this.title = title;
            this.trackNumber = trackNumber;
            this.year = year;
            this.duration = duration;
            this.remoteAlbumId = remoteAlbumId;
            this.albumName = albumName;
            this.remoteArtistId = remoteArtistId;
            this.artistName = artistName;
            this.genre = genre;
            this.coverArt = coverArt;
            this.created = created;
            this.played = played;
            this.playCount = playCount;
            this.userRating = userRating;
            this.fileSize = fileSize;
            this.contentType = contentType;
            this.suffix = suffix;
            this.path = path;
            this.bitRate = bitRate;
            this.samplingRate = samplingRate;
        }
    }

    public static class CachedSongDetails {
        public final String title;
        public final String path;
        public final long fileSize;
        public final String contentType;
        public final String suffix;
        public final int bitRate;
        public final int samplingRate;

        public CachedSongDetails(@NonNull String title, @Nullable String path, long fileSize,
                                 @Nullable String contentType, @Nullable String suffix,
                                 int bitRate, int samplingRate) {
            this.title = title;
            this.path = path;
            this.fileSize = fileSize;
            this.contentType = contentType;
            this.suffix = suffix;
            this.bitRate = bitRate;
            this.samplingRate = samplingRate;
        }
    }

    public static class CachedGenre {
        public final String name;
        public final int songCount;

        public CachedGenre(@NonNull String name, int songCount) {
            this.name = name;
            this.songCount = songCount;
        }
    }

    public static class CachedPlaylist {
        public final String remoteId;
        public final String name;
        public final List<String> songRemoteIds;

        public CachedPlaylist(@NonNull String remoteId, @NonNull String name, @NonNull List<String> songRemoteIds) {
            this.remoteId = remoteId;
            this.name = name;
            this.songRemoteIds = songRemoteIds;
        }
    }
}
