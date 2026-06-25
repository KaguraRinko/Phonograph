package com.kabouzeid.gramophone.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import android.text.Html;

import com.afollestad.materialdialogs.MaterialDialog;
import com.kabouzeid.gramophone.R;
import com.kabouzeid.gramophone.helper.MusicPlayerRemote;
import com.kabouzeid.gramophone.model.Song;
import com.kabouzeid.gramophone.service.MusicService;
import com.kabouzeid.gramophone.util.MusicUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Karim Abou Zeid (kabouzeid), Aidan Follestad (afollestad)
 */
public class DeleteSongsDialog extends DialogFragment {

    private final ArrayList<Song> pendingDeleteSongs = new ArrayList<>();
    private ActivityResultLauncher<IntentSenderRequest> deleteRequestLauncher;

    @NonNull
    public static DeleteSongsDialog create(Song song) {
        List<Song> list = new ArrayList<>();
        list.add(song);
        return create(list);
    }

    @NonNull
    public static DeleteSongsDialog create(List<Song> songs) {
        DeleteSongsDialog dialog = new DeleteSongsDialog();
        Bundle args = new Bundle();
        args.putParcelableArrayList("songs", new ArrayList<>(songs));
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        deleteRequestLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        onSongsDeleted(pendingDeleteSongs);
                    }
                    pendingDeleteSongs.clear();
                    dismissAllowingStateLoss();
                });
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        //noinspection unchecked
        final List<Song> songs = getArguments().getParcelableArrayList("songs");
        int title;
        CharSequence content;
        if (songs.size() > 1) {
            title = R.string.delete_songs_title;
            content = Html.fromHtml(getString(R.string.delete_x_songs, songs.size()));
        } else {
            title = R.string.delete_song_title;
            content = Html.fromHtml(getString(R.string.delete_song_x, songs.get(0).title));
        }
        return new MaterialDialog.Builder(getActivity())
                .title(title)
                .content(content)
                .autoDismiss(false)
                .positiveText(R.string.delete_action)
                .negativeText(android.R.string.cancel)
                .onNegative((dialog, which) -> dismiss())
                .onPositive((dialog, which) -> {
                    if (getActivity() == null)
                        return;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        requestScopedStorageDelete(songs);
                    } else {
                        MusicUtil.deleteTracks(getActivity(), songs);
                        dismiss();
                    }
                })
                .build();
    }

    private void requestScopedStorageDelete(@NonNull List<Song> songs) {
        if (getActivity() == null || deleteRequestLauncher == null) {
            return;
        }

        ArrayList<Uri> uris = new ArrayList<>();
        for (Song song : songs) {
            if (song.id >= 0) {
                uris.add(ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id));
            }
        }

        if (uris.isEmpty()) {
            dismiss();
            return;
        }

        pendingDeleteSongs.clear();
        pendingDeleteSongs.addAll(songs);

        try {
            PendingIntent request = MediaStore.createDeleteRequest(getActivity().getContentResolver(), uris);
            deleteRequestLauncher.launch(new IntentSenderRequest.Builder(request.getIntentSender()).build());
        } catch (RuntimeException e) {
            pendingDeleteSongs.clear();
            Toast.makeText(getActivity(), R.string.permissions_denied, Toast.LENGTH_SHORT).show();
            dismiss();
        }
    }

    private void onSongsDeleted(@NonNull List<Song> songs) {
        if (getContext() == null) {
            return;
        }

        for (Song song : songs) {
            MusicPlayerRemote.removeFromQueue(song);
        }
        Toast.makeText(getContext(), getString(R.string.deleted_x_songs, songs.size()), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(MusicService.MEDIA_STORE_CHANGED).setPackage(getContext().getPackageName());
        getContext().sendBroadcast(intent);
    }
}
