package com.kabouzeid.gramophone.glide.subsonic;

import android.content.Context;

import androidx.annotation.NonNull;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;
import com.kabouzeid.gramophone.subsonic.SubsonicCoverArtCache;

import java.io.IOException;
import java.io.InputStream;

public class SubsonicCoverArtFetcher implements DataFetcher<InputStream> {
    private final Context context;
    private final SubsonicCoverArt model;

    private boolean cachedBeforeLoad;
    private InputStream stream;

    public SubsonicCoverArtFetcher(@NonNull Context context, @NonNull SubsonicCoverArt model) {
        this.context = context.getApplicationContext();
        this.model = model;
    }

    @Override
    public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super InputStream> callback) {
        try {
            cachedBeforeLoad = SubsonicCoverArtCache.isCached(context, model.server.id, model.coverArtId);
            stream = SubsonicCoverArtCache.open(context, model);
            callback.onDataReady(stream);
        } catch (IOException e) {
            callback.onLoadFailed(e);
        }
    }

    @Override
    public void cleanup() {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException ignore) {
                // Nothing useful to do once Glide is cleaning up the request.
            }
        }
    }

    @Override
    public void cancel() {
    }

    @NonNull
    @Override
    public Class<InputStream> getDataClass() {
        return InputStream.class;
    }

    @NonNull
    @Override
    public DataSource getDataSource() {
        return cachedBeforeLoad ? DataSource.LOCAL : DataSource.REMOTE;
    }
}
