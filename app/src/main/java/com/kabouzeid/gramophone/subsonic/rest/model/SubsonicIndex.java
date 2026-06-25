package com.kabouzeid.gramophone.subsonic.rest.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SubsonicIndex {
    public String name;
    @SerializedName("artist")
    public List<SubsonicArtist> artists;
}
