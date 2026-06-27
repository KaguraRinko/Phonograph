package com.kabouzeid.gramophone.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.RequestManager;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.signature.MediaStoreSignature;
import com.kabouzeid.gramophone.R;
import com.kabouzeid.gramophone.glide.audiocover.AudioFileCover;
import com.kabouzeid.gramophone.glide.palette.BitmapPaletteWrapper;
import com.kabouzeid.gramophone.model.Song;
import com.kabouzeid.gramophone.source.MediaSourceManager;
import com.kabouzeid.gramophone.subsonic.SubsonicServer;
import com.kabouzeid.gramophone.subsonic.SubsonicUri;
import com.kabouzeid.gramophone.util.MusicUtil;
import com.kabouzeid.gramophone.util.PreferenceUtil;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class SongGlideRequest {

    public static final DiskCacheStrategy DEFAULT_DISK_CACHE_STRATEGY = DiskCacheStrategy.NONE;
    public static final int DEFAULT_ERROR_IMAGE = R.drawable.default_album_art;
    public static final int DEFAULT_ANIMATION = android.R.anim.fade_in;

    public static class Builder {
        final RequestManager requestManager;
        final Song song;
        boolean ignoreMediaStore;
        @Nullable
        Context context;

        public static Builder from(@NonNull RequestManager requestManager, Song song) {
            return new Builder(requestManager, song);
        }

        private Builder(@NonNull RequestManager requestManager, Song song) {
            this.requestManager = requestManager;
            this.song = song;
        }

        public PaletteBuilder generatePalette(Context context) {
            this.context = context.getApplicationContext();
            return new PaletteBuilder(this, context);
        }

        public BitmapBuilder asBitmap() {
            return new BitmapBuilder(this);
        }

        public Builder checkIgnoreMediaStore(Context context) {
            this.context = context.getApplicationContext();
            return ignoreMediaStore(PreferenceUtil.getInstance(context).ignoreMediaStoreArtwork());
        }

        public Builder ignoreMediaStore(boolean ignoreMediaStore) {
            this.ignoreMediaStore = ignoreMediaStore;
            return this;
        }

        public RequestBuilder<Drawable> build() {
            return createBaseRequest(requestManager.load(getLoadModel(context, song, ignoreMediaStore)), song)
                    .transition(DrawableTransitionOptions.withCrossFade());
        }
    }

    public static class BitmapBuilder {
        private final Builder builder;

        public BitmapBuilder(Builder builder) {
            this.builder = builder;
        }

        public RequestBuilder<Bitmap> build() {
            return createBaseRequest(builder.requestManager.asBitmap().load(getLoadModel(builder.context, builder.song, builder.ignoreMediaStore)), builder.song);
        }
    }

    public static class PaletteBuilder {
        final Context context;
        private final Builder builder;

        public PaletteBuilder(Builder builder, Context context) {
            this.builder = builder;
            this.context = context;
        }

        public RequestBuilder<BitmapPaletteWrapper> build() {
            return createBaseRequest(builder.requestManager.as(BitmapPaletteWrapper.class)
                    .load(getLoadModel(builder.context, builder.song, builder.ignoreMediaStore)), builder.song);
        }
    }

    private static Object getLoadModel(@Nullable Context context, Song song, boolean ignoreMediaStore) {
        Object remoteCoverArt = getRemoteCoverArt(context, song);
        if (remoteCoverArt != null) {
            return remoteCoverArt;
        }
        if (ignoreMediaStore) {
            return new AudioFileCover(song.data);
        } else {
            return MusicUtil.getMediaStoreAlbumCoverUri(song.albumId);
        }
    }

    @Nullable
    private static Object getRemoteCoverArt(@Nullable Context context, @Nullable Song song) {
        if (context == null || song == null || !SubsonicUri.isSubsonicUri(song.data)) {
            return null;
        }
        long serverId = SubsonicUri.getServerId(song.data);
        String sourceId = MediaSourceManager.toSubsonicSourceId(serverId);
        if (serverId == SubsonicServer.NO_ID || MediaSourceManager.getSource(context, sourceId) == null) {
            return null;
        }
        return MediaSourceManager.getRepository(context, sourceId).resolveCoverArt(context, song);
    }

    private static <T> RequestBuilder<T> createBaseRequest(RequestBuilder<T> requestBuilder, Song song) {
        return requestBuilder.apply(new RequestOptions()
                .diskCacheStrategy(DEFAULT_DISK_CACHE_STRATEGY)
                .error(DEFAULT_ERROR_IMAGE)
                .signature(createSignature(song)));
    }

    public static Key createSignature(Song song) {
        return new MediaStoreSignature("", song.dateModified, 0);
    }
}
