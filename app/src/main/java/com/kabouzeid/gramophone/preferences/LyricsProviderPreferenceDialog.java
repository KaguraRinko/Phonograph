package com.kabouzeid.gramophone.preferences;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.kabouzeid.gramophone.R;
import com.kabouzeid.gramophone.adapter.LyricsProviderInfoAdapter;
import com.kabouzeid.gramophone.model.LyricsProviderInfo;
import com.kabouzeid.gramophone.util.PreferenceUtil;

import java.util.ArrayList;
import java.util.List;

public class LyricsProviderPreferenceDialog extends DialogFragment {
    public static LyricsProviderPreferenceDialog newInstance() {
        return new LyricsProviderPreferenceDialog();
    }

    private LyricsProviderInfoAdapter adapter;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = getActivity().getLayoutInflater()
                .inflate(R.layout.preference_dialog_library_categories, null);

        List<LyricsProviderInfo> providers;
        if (savedInstanceState != null) {
            providers = savedInstanceState.getParcelableArrayList(PreferenceUtil.LYRICS_PROVIDERS);
        } else {
            providers = PreferenceUtil.getInstance(getContext()).getLyricsProviderInfos();
        }
        if (providers == null) {
            providers = PreferenceUtil.getInstance(getContext()).getDefaultLyricsProviderInfos();
        }
        adapter = new LyricsProviderInfoAdapter(providers);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);
        adapter.attachToRecyclerView(recyclerView);

        return new MaterialDialog.Builder(getContext())
                .title(R.string.lyrics_providers)
                .customView(view, false)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .neutralText(R.string.reset_action)
                .autoDismiss(false)
                .onNeutral((dialog, action) -> adapter.setProviders(
                        PreferenceUtil.getInstance(getContext()).getDefaultLyricsProviderInfos()))
                .onNegative((dialog, action) -> dismiss())
                .onPositive((dialog, action) -> {
                    updateProviders(adapter.getProviders());
                    dismiss();
                })
                .build();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(PreferenceUtil.LYRICS_PROVIDERS,
                new ArrayList<>(adapter.getProviders()));
    }

    private void updateProviders(@NonNull List<LyricsProviderInfo> providers) {
        if (getEnabledCount(providers) == 0) {
            return;
        }
        PreferenceUtil.getInstance(getContext()).setLyricsProviderInfos(providers);
    }

    private int getEnabledCount(@NonNull List<LyricsProviderInfo> providers) {
        int enabled = 0;
        for (LyricsProviderInfo provider : providers) {
            if (provider.enabled) {
                enabled++;
            }
        }
        return enabled;
    }
}
