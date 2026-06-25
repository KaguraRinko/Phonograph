package com.kabouzeid.gramophone.subsonic.rest.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SubsonicGenres {
    @SerializedName("genre")
    public List<SubsonicGenre> genres;
}
