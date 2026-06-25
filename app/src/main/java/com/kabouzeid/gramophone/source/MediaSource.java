package com.kabouzeid.gramophone.source;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class MediaSource implements Parcelable {
    public static final int TYPE_LOCAL = 0;
    public static final int TYPE_SUBSONIC = 1;

    public final String id;
    public final int type;
    public final String name;

    public MediaSource(@NonNull String id, int type, @NonNull String name) {
        this.id = id;
        this.type = type;
        this.name = name;
    }

    public boolean isLocal() {
        return type == TYPE_LOCAL;
    }

    public boolean isSubsonic() {
        return type == TYPE_SUBSONIC;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeInt(type);
        dest.writeString(name);
    }

    protected MediaSource(Parcel in) {
        id = in.readString();
        type = in.readInt();
        name = in.readString();
    }

    public static final Creator<MediaSource> CREATOR = new Creator<MediaSource>() {
        @Override
        public MediaSource createFromParcel(Parcel source) {
            return new MediaSource(source);
        }

        @Override
        public MediaSource[] newArray(int size) {
            return new MediaSource[size];
        }
    };
}
