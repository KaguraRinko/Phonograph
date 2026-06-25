package com.kabouzeid.gramophone.glide.artistimage;

import android.content.Context;
import androidx.annotation.NonNull;

import java.io.InputStream;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.signature.ObjectKey;
import com.kabouzeid.gramophone.util.PreferenceUtil;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */

public class ArtistImageLoader implements ModelLoader<ArtistImage, InputStream> {
    private final Context context;

    public ArtistImageLoader(Context context) {
        this.context = context;
    }

    @Override
    public LoadData<InputStream> buildLoadData(@NonNull final ArtistImage model, int width, int height, @NonNull Options options) {
        boolean ignoreMediaStore = PreferenceUtil.getInstance(context).ignoreMediaStoreArtwork();
        return new LoadData<>(new ObjectKey(model.toIdString() + "ignoremediastore:" + ignoreMediaStore),
                new ArtistImageFetcher(model, ignoreMediaStore));
    }

    @Override
    public boolean handles(@NonNull ArtistImage model) {
        return true;
    }

    public static class Factory implements ModelLoaderFactory<ArtistImage, InputStream> {
        private final Context context;

        public Factory(Context context) {
            this.context = context.getApplicationContext();
        }

        @NonNull
        @Override
        public ModelLoader<ArtistImage, InputStream> build(@NonNull MultiModelLoaderFactory multiFactory) {
            return new ArtistImageLoader(context);
        }

        @Override
        public void teardown() {

        }
    }
}
