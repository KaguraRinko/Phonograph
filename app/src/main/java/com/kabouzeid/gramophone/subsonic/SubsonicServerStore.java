package com.kabouzeid.gramophone.subsonic;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SubsonicServerStore extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "subsonic_sources.db";
    private static final int VERSION = 1;

    private static final String TABLE_SERVERS = "servers";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_BASE_URL = "base_url";
    private static final String COLUMN_USERNAME = "username";
    private static final String COLUMN_PASSWORD = "password";
    private static final String COLUMN_LAST_SYNCED = "last_synced";

    @Nullable
    private static SubsonicServerStore sInstance;

    private SubsonicServerStore(@NonNull Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, VERSION);
    }

    @NonNull
    public static synchronized SubsonicServerStore getInstance(@NonNull Context context) {
        if (sInstance == null) {
            sInstance = new SubsonicServerStore(context);
        }
        return sInstance;
    }

    @Override
    public void onCreate(@NonNull SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_SERVERS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_NAME + " TEXT NOT NULL,"
                + COLUMN_BASE_URL + " TEXT NOT NULL,"
                + COLUMN_USERNAME + " TEXT NOT NULL,"
                + COLUMN_PASSWORD + " TEXT NOT NULL,"
                + COLUMN_LAST_SYNCED + " INTEGER NOT NULL DEFAULT 0"
                + ");");
    }

    @Override
    public void onUpgrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SERVERS);
        onCreate(db);
    }

    @Override
    public void onDowngrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SERVERS);
        onCreate(db);
    }

    @NonNull
    public synchronized List<SubsonicServer> getAllServers() {
        List<SubsonicServer> servers = new ArrayList<>();
        Cursor cursor = getReadableDatabase().query(TABLE_SERVERS, null, null, null, null, null, COLUMN_NAME);
        try {
            if (cursor.moveToFirst()) {
                do {
                    servers.add(readServer(cursor));
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }
        return servers;
    }

    @Nullable
    public synchronized SubsonicServer getServer(long serverId) {
        Cursor cursor = getReadableDatabase().query(
                TABLE_SERVERS,
                null,
                BaseColumns._ID + "=?",
                new String[]{String.valueOf(serverId)},
                null,
                null,
                null
        );
        try {
            return cursor.moveToFirst() ? readServer(cursor) : null;
        } finally {
            cursor.close();
        }
    }

    public synchronized long insertServer(@NonNull SubsonicServer server) {
        return getWritableDatabase().insert(TABLE_SERVERS, null, toContentValues(server));
    }

    public synchronized boolean updateServer(@NonNull SubsonicServer server) {
        if (server.id == SubsonicServer.NO_ID) {
            return false;
        }
        return getWritableDatabase().update(
                TABLE_SERVERS,
                toContentValues(server),
                BaseColumns._ID + "=?",
                new String[]{String.valueOf(server.id)}
        ) > 0;
    }

    public synchronized boolean updateLastSynced(long serverId, long lastSynced) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_LAST_SYNCED, lastSynced);
        return getWritableDatabase().update(
                TABLE_SERVERS,
                values,
                BaseColumns._ID + "=?",
                new String[]{String.valueOf(serverId)}
        ) > 0;
    }

    public synchronized boolean deleteServer(long serverId) {
        return getWritableDatabase().delete(
                TABLE_SERVERS,
                BaseColumns._ID + "=?",
                new String[]{String.valueOf(serverId)}
        ) > 0;
    }

    @NonNull
    private ContentValues toContentValues(@NonNull SubsonicServer server) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, server.name);
        values.put(COLUMN_BASE_URL, server.baseUrl);
        values.put(COLUMN_USERNAME, server.username);
        values.put(COLUMN_PASSWORD, server.password);
        values.put(COLUMN_LAST_SYNCED, server.lastSynced);
        return values;
    }

    @NonNull
    private SubsonicServer readServer(@NonNull Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID));
        String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME));
        String baseUrl = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BASE_URL));
        String username = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME));
        String password = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD));
        long lastSynced = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LAST_SYNCED));
        return new SubsonicServer(id, name, baseUrl, username, password, lastSynced);
    }
}
