package com.kabouzeid.gramophone.subsonic.rest.model;

import com.google.gson.annotations.SerializedName;

public class SubsonicGenre {
    public Integer songCount;
    public Integer albumCount;
    public String name;
    public String value;
    @SerializedName("#text")
    public String text;

    public String getDisplayName() {
        if (name != null && !name.trim().isEmpty()) return name;
        if (value != null && !value.trim().isEmpty()) return value;
        if (text != null && !text.trim().isEmpty()) return text;
        return "";
    }
}
