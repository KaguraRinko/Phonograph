package com.kabouzeid.gramophone.subsonic;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class SubsonicServer implements Parcelable {
    public static final long NO_ID = -1;

    public final long id;
    public final String name;
    public final String baseUrl;
    public final String username;
    public final String password;
    public final long lastSynced;

    public SubsonicServer(long id, @NonNull String name, @NonNull String baseUrl,
                          @NonNull String username, @NonNull String password, long lastSynced) {
        this.id = id;
        this.name = name;
        this.baseUrl = baseUrl;
        this.username = username;
        this.password = password;
        this.lastSynced = lastSynced;
    }

    @NonNull
    public SubsonicServer withId(long id) {
        return new SubsonicServer(id, name, baseUrl, username, password, lastSynced);
    }

    @NonNull
    public SubsonicServer withLastSynced(long lastSynced) {
        return new SubsonicServer(id, name, baseUrl, username, password, lastSynced);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(name);
        dest.writeString(baseUrl);
        dest.writeString(username);
        dest.writeString(password);
        dest.writeLong(lastSynced);
    }

    protected SubsonicServer(Parcel in) {
        id = in.readLong();
        name = in.readString();
        baseUrl = in.readString();
        username = in.readString();
        password = in.readString();
        lastSynced = in.readLong();
    }

    public static final Creator<SubsonicServer> CREATOR = new Creator<SubsonicServer>() {
        @Override
        public SubsonicServer createFromParcel(Parcel source) {
            return new SubsonicServer(source);
        }

        @Override
        public SubsonicServer[] newArray(int size) {
            return new SubsonicServer[size];
        }
    };
}
