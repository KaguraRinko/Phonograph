package com.kabouzeid.gramophone.model;

import android.os.Parcel;
import android.os.Parcelable;

public class LyricsProviderInfo implements Parcelable {
    public String id;
    public boolean enabled;

    public LyricsProviderInfo() {
    }

    public LyricsProviderInfo(String id, boolean enabled) {
        this.id = id;
        this.enabled = enabled;
    }

    protected LyricsProviderInfo(Parcel in) {
        id = in.readString();
        enabled = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeByte((byte) (enabled ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<LyricsProviderInfo> CREATOR = new Creator<LyricsProviderInfo>() {
        @Override
        public LyricsProviderInfo createFromParcel(Parcel in) {
            return new LyricsProviderInfo(in);
        }

        @Override
        public LyricsProviderInfo[] newArray(int size) {
            return new LyricsProviderInfo[size];
        }
    };
}
