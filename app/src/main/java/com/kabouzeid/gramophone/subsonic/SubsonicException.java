package com.kabouzeid.gramophone.subsonic;

import androidx.annotation.Nullable;

public class SubsonicException extends Exception {
    public final int code;

    public SubsonicException(int code, @Nullable String message) {
        super(message == null || message.trim().isEmpty() ? "Subsonic error " + code : message);
        this.code = code;
    }
}
