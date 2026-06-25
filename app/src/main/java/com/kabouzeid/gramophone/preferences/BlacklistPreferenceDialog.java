package com.kabouzeid.gramophone.preferences;

import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import android.text.Html;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.kabouzeid.gramophone.R;
import com.kabouzeid.gramophone.provider.BlacklistStore;
import com.kabouzeid.gramophone.util.StorageAccessUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class BlacklistPreferenceDialog extends DialogFragment {

    private List<String> paths;
    private ActivityResultLauncher<Uri> addFolderLauncher;

    public static BlacklistPreferenceDialog newInstance() {
        return new BlacklistPreferenceDialog();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addFolderLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), this::addFolderToBlacklist);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        refreshBlacklistData();
        return new MaterialDialog.Builder(getContext())
                .title(R.string.blacklist)
                .positiveText(android.R.string.ok)
                .neutralText(R.string.clear_action)
                .negativeText(R.string.add_action)
                .items(paths)
                .autoDismiss(false)
                .itemsCallback((materialDialog, view, i, charSequence) -> new MaterialDialog.Builder(getContext())
                        .title(R.string.remove_from_blacklist)
                        .content(Html.fromHtml(getString(R.string.do_you_want_to_remove_from_the_blacklist, charSequence)))
                        .positiveText(R.string.remove_action)
                        .negativeText(android.R.string.cancel)
                        .onPositive((materialDialog12, dialogAction) -> {
                            BlacklistStore.getInstance(getContext()).removePath(new File(charSequence.toString()));
                            refreshBlacklistData();
                        }).show())
                // clear
                .onNeutral((materialDialog, dialogAction) -> new MaterialDialog.Builder(getContext())
                        .title(R.string.clear_blacklist)
                        .content(R.string.do_you_want_to_clear_the_blacklist)
                        .positiveText(R.string.clear_action)
                        .negativeText(android.R.string.cancel)
                        .onPositive((materialDialog1, dialogAction1) -> {
                            BlacklistStore.getInstance(getContext()).clear();
                            refreshBlacklistData();
                        }).show())
                // add
                .onNegative((materialDialog, dialogAction) -> {
                    if (addFolderLauncher != null) {
                        addFolderLauncher.launch(null);
                    }
                })
                .onPositive((materialDialog, dialogAction) -> dismiss())
                .build();
    }

    private void refreshBlacklistData() {
        paths = BlacklistStore.getInstance(getContext()).getPaths();

        MaterialDialog dialog = (MaterialDialog) getDialog();
        if (dialog != null) {
            String[] pathArray = new String[paths.size()];
            pathArray = paths.toArray(pathArray);
            dialog.setItems((CharSequence[]) pathArray);
        }
    }

    private void addFolderToBlacklist(@Nullable Uri uri) {
        if (uri == null || getContext() == null) {
            return;
        }

        StorageAccessUtil.takePersistableReadPermission(getContext(), uri);
        File folder = StorageAccessUtil.getFileFromTreeUri(getContext(), uri);
        if (folder == null || !folder.exists() || !folder.isDirectory()) {
            Toast.makeText(getContext(), R.string.selected_folder_not_accessible, Toast.LENGTH_SHORT).show();
            return;
        }

        BlacklistStore.getInstance(getContext()).addPath(folder);
        refreshBlacklistData();
    }
}
