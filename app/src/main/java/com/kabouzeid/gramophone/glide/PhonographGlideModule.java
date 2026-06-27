package com.kabouzeid.gramophone.glide;

import android.content.Context;
import android.graphics.Bitmap;

import java.io.InputStream;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;
import com.kabouzeid.gramophone.glide.artistimage.ArtistImage;
import com.kabouzeid.gramophone.glide.artistimage.ArtistImageLoader;
import com.kabouzeid.gramophone.glide.audiocover.AudioFileCover;
import com.kabouzeid.gramophone.glide.audiocover.AudioFileCoverLoader;
import com.kabouzeid.gramophone.glide.palette.BitmapPaletteTranscoder;
import com.kabouzeid.gramophone.glide.palette.BitmapPaletteWrapper;
import com.kabouzeid.gramophone.glide.subsonic.SubsonicCoverArt;
import com.kabouzeid.gramophone.glide.subsonic.SubsonicCoverArtLoader;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
@GlideModule
public class PhonographGlideModule extends AppGlideModule {
    @Override
    public void applyOptions(Context context, GlideBuilder builder) {

    }

    @Override
    public void registerComponents(Context context, Glide glide, Registry registry) {
        registry.append(AudioFileCover.class, InputStream.class, new AudioFileCoverLoader.Factory());
        registry.append(ArtistImage.class, InputStream.class, new ArtistImageLoader.Factory(context));
        registry.append(SubsonicCoverArt.class, InputStream.class, new SubsonicCoverArtLoader.Factory(context));
        registry.register(Bitmap.class, BitmapPaletteWrapper.class, new BitmapPaletteTranscoder(glide.getBitmapPool()));
    }

    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }
}
