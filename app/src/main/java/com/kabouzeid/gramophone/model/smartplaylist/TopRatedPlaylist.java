package com.kabouzeid.gramophone.model.smartplaylist;

import android.content.Context;
import android.os.Parcel;

import androidx.annotation.NonNull;

import com.kabouzeid.gramophone.R;
import com.kabouzeid.gramophone.model.Song;
import com.kabouzeid.gramophone.source.MediaSourceManager;

import java.util.ArrayList;
import java.util.List;

public class TopRatedPlaylist extends AbsSmartPlaylist {

    public TopRatedPlaylist(@NonNull Context context) {
        super(context.getString(R.string.top_rated), R.drawable.ic_star_white_24dp);
    }

    @NonNull
    @Override
    public List<Song> getSongs(@NonNull Context context) {
        if (MediaSourceManager.isCurrentSourceLocal(context)) {
            return new ArrayList<>();
        }
        return MediaSourceManager.getCurrentRepository(context).getSongsForPlaylist(context, this);
    }

    @Override
    public void clear(@NonNull Context context) {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    protected TopRatedPlaylist(Parcel in) {
        super(in);
    }

    public static final Creator<TopRatedPlaylist> CREATOR = new Creator<TopRatedPlaylist>() {
        public TopRatedPlaylist createFromParcel(Parcel source) {
            return new TopRatedPlaylist(source);
        }

        public TopRatedPlaylist[] newArray(int size) {
            return new TopRatedPlaylist[size];
        }
    };
}
