package com.kabouzeid.gramophone.subsonic.rest.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SubsonicAlbum {
    public String id;
    public String name;
    public String artist;
    public String artistId;
    public String coverArt;
    public Integer songCount;
    public Integer duration;
    public Integer playCount;
    public String created;
    public Integer year;
    public String genre;
    @SerializedName("song")
    public List<SubsonicChild> songs;
}
