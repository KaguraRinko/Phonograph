package com.kabouzeid.gramophone.subsonic.rest.model;

import com.google.gson.annotations.SerializedName;

public class SubsonicResponseEnvelope {
    @SerializedName("subsonic-response")
    public SubsonicResponse response;
}
