package com.kabouzeid.gramophone.subsonic.rest.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SubsonicPlaylists {
    @SerializedName("playlist")
    public List<SubsonicPlaylist> playlists;
}
