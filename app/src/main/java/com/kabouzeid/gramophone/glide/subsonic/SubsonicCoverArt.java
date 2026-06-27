package com.kabouzeid.gramophone.glide.subsonic;

import androidx.annotation.NonNull;

import com.kabouzeid.gramophone.subsonic.SubsonicServer;

public class SubsonicCoverArt {
    public final SubsonicServer server;
    public final String coverArtId;

    public SubsonicCoverArt(@NonNull SubsonicServer server, @NonNull String coverArtId) {
        this.server = server;
        this.coverArtId = coverArtId;
    }

    @NonNull
    public String stableKey() {
        return server.id + ":" + coverArtId;
    }
}
