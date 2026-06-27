package com.kabouzeid.gramophone.glide.subsonic;

import android.content.Context;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.signature.ObjectKey;

import java.io.InputStream;

public class SubsonicCoverArtLoader implements ModelLoader<SubsonicCoverArt, InputStream> {
    private final Context context;

    private SubsonicCoverArtLoader(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public LoadData<InputStream> buildLoadData(@NonNull SubsonicCoverArt model, int width, int height,
                                               @NonNull Options options) {
        return new LoadData<>(new ObjectKey(model.stableKey()), new SubsonicCoverArtFetcher(context, model));
    }

    @Override
    public boolean handles(@NonNull SubsonicCoverArt model) {
        return true;
    }

    public static class Factory implements ModelLoaderFactory<SubsonicCoverArt, InputStream> {
        private final Context context;

        public Factory(@NonNull Context context) {
            this.context = context.getApplicationContext();
        }

        @NonNull
        @Override
        public ModelLoader<SubsonicCoverArt, InputStream> build(@NonNull MultiModelLoaderFactory multiFactory) {
            return new SubsonicCoverArtLoader(context);
        }

        @Override
        public void teardown() {
        }
    }
}
