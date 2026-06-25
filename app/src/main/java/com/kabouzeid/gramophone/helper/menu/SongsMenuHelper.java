package com.kabouzeid.gramophone.helper.menu;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.kabouzeid.gramophone.R;
import com.kabouzeid.gramophone.dialogs.AddToPlaylistDialog;
import com.kabouzeid.gramophone.dialogs.DeleteSongsDialog;
import com.kabouzeid.gramophone.helper.MusicPlayerRemote;
import com.kabouzeid.gramophone.model.Song;

import java.util.List;
import android.view.Menu;
import android.view.MenuItem;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class SongsMenuHelper {
    public static boolean handleMenuClick(@NonNull FragmentActivity activity, @NonNull List<Song> songs, int menuItemId) {
        if (containsRemoteSongs(songs) && isLocalOnlySongsAction(menuItemId)) {
            return false;
        }
                if (menuItemId == R.id.action_play_next) {
                MusicPlayerRemote.playNext(songs);
                return true;
                    } else if (menuItemId == R.id.action_add_to_current_playing) {
                MusicPlayerRemote.enqueue(songs);
                return true;
                    } else if (menuItemId == R.id.action_add_to_playlist) {
                AddToPlaylistDialog.create(songs).show(activity.getSupportFragmentManager(), "ADD_PLAYLIST");
                return true;
                    } else if (menuItemId == R.id.action_delete_from_device) {
                DeleteSongsDialog.create(songs).show(activity.getSupportFragmentManager(), "DELETE_SONGS");
                return true;
                }
        return false;
    }

    public static void prepareSongsMenu(@NonNull Menu menu, boolean supportsLocalActions) {
        if (supportsLocalActions) {
            return;
        }
        hide(menu, R.id.action_add_to_playlist);
        hide(menu, R.id.action_delete_from_device);
        hide(menu, R.id.action_remove_from_playlist);
    }

    public static boolean containsRemoteSongs(@NonNull List<Song> songs) {
        for (Song song : songs) {
            if (SongMenuHelper.isRemoteSong(song)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isLocalOnlySongsAction(int menuItemId) {
        return menuItemId == R.id.action_add_to_playlist
                || menuItemId == R.id.action_delete_from_device
                || menuItemId == R.id.action_remove_from_playlist;
    }

    private static void hide(@NonNull Menu menu, int itemId) {
        MenuItem item = menu.findItem(itemId);
        if (item != null) {
            item.setVisible(false);
        }
    }
}
