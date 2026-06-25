package com.kabouzeid.gramophone.subsonic.rest.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SubsonicArtist {
    public String id;
    public String name;
    public String coverArt;
    public Integer albumCount;
    @SerializedName("album")
    public List<SubsonicAlbum> albums;
}
