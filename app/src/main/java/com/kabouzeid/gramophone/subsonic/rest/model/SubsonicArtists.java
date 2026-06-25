package com.kabouzeid.gramophone.subsonic.rest.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SubsonicArtists {
    public String ignoredArticles;
    @SerializedName("index")
    public List<SubsonicIndex> indexes;
}
