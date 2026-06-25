package com.kabouzeid.gramophone.util;

import android.app.Activity;
import android.content.Context;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.media.audiofx.AudioEffect;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.util.Pair;
import android.widget.Toast;

import com.kabouzeid.gramophone.R;
import com.kabouzeid.gramophone.helper.MusicPlayerRemote;
import com.kabouzeid.gramophone.model.Genre;
import com.kabouzeid.gramophone.model.Playlist;
import com.kabouzeid.gramophone.source.MediaSourceManager;
import com.kabouzeid.gramophone.ui.activities.AlbumDetailActivity;
import com.kabouzeid.gramophone.ui.activities.ArtistDetailActivity;
import com.kabouzeid.gramophone.ui.activities.GenreDetailActivity;
import com.kabouzeid.gramophone.ui.activities.PlaylistDetailActivity;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class NavigationUtil {
    public static final String EXTRA_SOURCE_ID = "extra_source_id";

    public static void goToArtist(@NonNull final Activity activity, final long artistId, @Nullable Pair... sharedElements) {
        goToArtist(activity, MediaSourceManager.getCurrentSourceId(activity), artistId, sharedElements);
    }

    public static void goToArtist(@NonNull final Activity activity, @NonNull final String sourceId,
                                  final long artistId, @Nullable Pair... sharedElements) {
        final Intent intent = new Intent(activity, ArtistDetailActivity.class);
        putSourceId(intent, sourceId);
        intent.putExtra(ArtistDetailActivity.EXTRA_ARTIST_ID, artistId);

        //noinspection unchecked
        if (sharedElements != null && sharedElements.length > 0) {
            activity.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity, sharedElements).toBundle());
        } else {
            activity.startActivity(intent);
        }
    }

    public static void goToAlbum(@NonNull final Activity activity, final long albumId, @Nullable Pair... sharedElements) {
        goToAlbum(activity, MediaSourceManager.getCurrentSourceId(activity), albumId, sharedElements);
    }

    public static void goToAlbum(@NonNull final Activity activity, @NonNull final String sourceId,
                                 final long albumId, @Nullable Pair... sharedElements) {
        final Intent intent = new Intent(activity, AlbumDetailActivity.class);
        putSourceId(intent, sourceId);
        intent.putExtra(AlbumDetailActivity.EXTRA_ALBUM_ID, albumId);

        //noinspection unchecked
        if (sharedElements != null && sharedElements.length > 0) {
            activity.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity, sharedElements).toBundle());
        } else {
            activity.startActivity(intent);
        }
    }

    public static void goToGenre(@NonNull final Activity activity, final Genre genre, @Nullable Pair... sharedElements) {
        final Intent intent = new Intent(activity, GenreDetailActivity.class);
        putSourceId(intent, MediaSourceManager.getCurrentSourceId(activity));
        intent.putExtra(GenreDetailActivity.EXTRA_GENRE, genre);

        activity.startActivity(intent);
    }

    public static void goToPlaylist(@NonNull final Activity activity, final Playlist playlist, @Nullable Pair... sharedElements) {
        final Intent intent = new Intent(activity, PlaylistDetailActivity.class);
        putSourceId(intent, MediaSourceManager.getCurrentSourceId(activity));
        intent.putExtra(PlaylistDetailActivity.EXTRA_PLAYLIST, playlist);

        activity.startActivity(intent);
    }

    @NonNull
    public static String getSourceId(@NonNull Activity activity) {
        Intent intent = activity.getIntent();
        return getSourceId(activity, intent == null ? null : intent.getExtras());
    }

    @NonNull
    public static String getSourceId(@NonNull Context context, @Nullable Bundle args) {
        if (args != null) {
            String sourceId = args.getString(EXTRA_SOURCE_ID);
            if (sourceId != null && MediaSourceManager.getSource(context, sourceId) != null) {
                return sourceId;
            }
        }
        return MediaSourceManager.getCurrentSourceId(context);
    }

    private static void putSourceId(@NonNull Intent intent, @NonNull String sourceId) {
        intent.putExtra(EXTRA_SOURCE_ID, sourceId);
    }

    public static void openEqualizer(@NonNull final Activity activity) {
        final int sessionId = MusicPlayerRemote.getAudioSessionId();
        if (sessionId == AudioEffect.ERROR_BAD_VALUE) {
            Toast.makeText(activity, activity.getResources().getString(R.string.no_audio_ID), Toast.LENGTH_LONG).show();
        } else {
            try {
                final Intent effects = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
                effects.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId);
                effects.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC);
                activity.startActivityForResult(effects, 0);
            } catch (@NonNull final ActivityNotFoundException notFound) {
                Toast.makeText(activity, activity.getResources().getString(R.string.no_equalizer), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
