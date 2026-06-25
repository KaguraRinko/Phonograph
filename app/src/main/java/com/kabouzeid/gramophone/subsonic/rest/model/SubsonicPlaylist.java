package com.kabouzeid.gramophone.subsonic.rest.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SubsonicPlaylist {
    public String id;
    public String name;
    public Integer songCount;
    public Integer duration;
    public String owner;
    @SerializedName("entry")
    public List<SubsonicChild> songs;
}
