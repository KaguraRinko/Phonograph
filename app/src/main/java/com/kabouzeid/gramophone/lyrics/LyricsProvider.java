package com.kabouzeid.gramophone.lyrics;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.List;

public interface LyricsProvider {
    @NonNull
    String getName();

    @NonNull
    List<LyricsSearchResult> search(@NonNull LyricsSearchQuery query) throws IOException;
}
