package com.kabouzeid.gramophone.helper.menu;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import com.kabouzeid.gramophone.R;
import com.kabouzeid.gramophone.dialogs.AddToPlaylistDialog;
import com.kabouzeid.gramophone.dialogs.DeleteSongsDialog;
import com.kabouzeid.gramophone.dialogs.SongDetailDialog;
import com.kabouzeid.gramophone.helper.MusicPlayerRemote;
import com.kabouzeid.gramophone.interfaces.PaletteColorHolder;
import com.kabouzeid.gramophone.model.Song;
import com.kabouzeid.gramophone.source.MediaSourceManager;
import com.kabouzeid.gramophone.subsonic.SubsonicServer;
import com.kabouzeid.gramophone.subsonic.SubsonicUri;
import com.kabouzeid.gramophone.ui.activities.tageditor.AbsTagEditorActivity;
import com.kabouzeid.gramophone.ui.activities.tageditor.SongTagEditorActivity;
import com.kabouzeid.gramophone.util.MusicUtil;
import com.kabouzeid.gramophone.util.NavigationUtil;
import com.kabouzeid.gramophone.util.RingtoneManager;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class SongMenuHelper {
    public static final int MENU_RES = R.menu.menu_item_song;

    public static boolean handleMenuClick(@NonNull FragmentActivity activity, @NonNull Song song, int menuItemId) {
        if (isRemoteSong(song) && isLocalOnlySongAction(menuItemId)) {
            return false;
        }
                if (menuItemId == R.id.action_set_as_ringtone) {
                if (RingtoneManager.requiresDialog(activity)) {
                    RingtoneManager.showDialog(activity);
                } else {
                    RingtoneManager ringtoneManager = new RingtoneManager();
                    ringtoneManager.setRingtone(activity, song.id);
                }
                return true;
                    } else if (menuItemId == R.id.action_share) {
                activity.startActivity(Intent.createChooser(MusicUtil.createShareSongFileIntent(song, activity), null));
                return true;
                    } else if (menuItemId == R.id.action_delete_from_device) {
                DeleteSongsDialog.create(song).show(activity.getSupportFragmentManager(), "DELETE_SONGS");
                return true;
                    } else if (menuItemId == R.id.action_add_to_playlist) {
                AddToPlaylistDialog.create(song).show(activity.getSupportFragmentManager(), "ADD_PLAYLIST");
                return true;
                    } else if (menuItemId == R.id.action_play_next) {
                MusicPlayerRemote.playNext(song);
                return true;
                    } else if (menuItemId == R.id.action_add_to_current_playing) {
                MusicPlayerRemote.enqueue(song);
                return true;
                    } else if (menuItemId == R.id.action_tag_editor) {
                Intent tagEditorIntent = new Intent(activity, SongTagEditorActivity.class);
                tagEditorIntent.putExtra(AbsTagEditorActivity.EXTRA_ID, song.id);
                if (activity instanceof PaletteColorHolder)
                    tagEditorIntent.putExtra(AbsTagEditorActivity.EXTRA_PALETTE, ((PaletteColorHolder) activity).getPaletteColor());
                activity.startActivity(tagEditorIntent);
                return true;
                    } else if (menuItemId == R.id.action_details) {
                SongDetailDialog.create(song).show(activity.getSupportFragmentManager(), "SONG_DETAILS");
                return true;
                    } else if (menuItemId == R.id.action_go_to_album) {
                NavigationUtil.goToAlbum(activity, getSourceId(activity, song), song.albumId);
                return true;
                    } else if (menuItemId == R.id.action_go_to_artist) {
                NavigationUtil.goToArtist(activity, getSourceId(activity, song), song.artistId);
                return true;
                }
        return false;
    }

    public static void prepareSongMenu(@NonNull Menu menu, @NonNull Song song) {
        if (!isRemoteSong(song)) {
            return;
        }
        hide(menu, R.id.action_set_as_ringtone);
        hide(menu, R.id.action_share);
        hide(menu, R.id.action_delete_from_device);
        hide(menu, R.id.action_add_to_playlist);
        hide(menu, R.id.action_tag_editor);
        hide(menu, R.id.action_remove_from_playlist);
    }

    public static boolean isRemoteSong(@NonNull Song song) {
        return SubsonicUri.isSubsonicUri(song.data);
    }

    public static boolean isLocalOnlySongAction(int menuItemId) {
        return menuItemId == R.id.action_set_as_ringtone
                || menuItemId == R.id.action_share
                || menuItemId == R.id.action_delete_from_device
                || menuItemId == R.id.action_add_to_playlist
                || menuItemId == R.id.action_tag_editor
                || menuItemId == R.id.action_remove_from_playlist;
    }

    @NonNull
    public static String getSourceId(@NonNull Context context, @NonNull Song song) {
        if (isRemoteSong(song)) {
            long serverId = SubsonicUri.getServerId(song.data);
            String sourceId = MediaSourceManager.toSubsonicSourceId(serverId);
            if (serverId != SubsonicServer.NO_ID && MediaSourceManager.getSource(context, sourceId) != null) {
                return sourceId;
            }
        }
        return MediaSourceManager.getCurrentSourceId(context);
    }

    private static void hide(@NonNull Menu menu, int itemId) {
        MenuItem item = menu.findItem(itemId);
        if (item != null) {
            item.setVisible(false);
        }
    }

    public static abstract class OnClickSongMenu implements View.OnClickListener, PopupMenu.OnMenuItemClickListener {
        private AppCompatActivity activity;

        public OnClickSongMenu(@NonNull AppCompatActivity activity) {
            this.activity = activity;
        }

        public int getMenuRes() {
            return MENU_RES;
        }

        @Override
        public void onClick(View v) {
            Song song = getSong();
            PopupMenu popupMenu = new PopupMenu(activity, v);
            popupMenu.inflate(getMenuRes());
            prepareSongMenu(popupMenu.getMenu(), song);
            popupMenu.setOnMenuItemClickListener(this);
            popupMenu.show();
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            return handleMenuClick(activity, getSong(), item.getItemId());
        }

        public abstract Song getSong();
    }
}
