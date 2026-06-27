package com.kabouzeid.gramophone.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.kabouzeid.gramophone.R;
import com.kabouzeid.gramophone.lyrics.LyricsCache;
import com.kabouzeid.gramophone.lyrics.LyricsRepository;
import com.kabouzeid.gramophone.lyrics.LyricsSearchQuery;
import com.kabouzeid.gramophone.lyrics.LyricsSearchResult;
import com.kabouzeid.gramophone.model.Song;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LyricsSearchDialog extends DialogFragment {
    private static final String EXTRA_SONG = "song";

    private Song song;
    private EditText keywordView;
    private ProgressBar progressBar;
    private TextView messageView;
    private ListView resultsView;
    private ArrayAdapter<String> adapter;
    private final List<LyricsSearchResult> results = new ArrayList<>();
    private AsyncTask<String, Void, List<LyricsSearchResult>> searchTask;

    public interface Callback {
        void onLyricsSearchResultSaved();
    }

    @NonNull
    public static LyricsSearchDialog create(@NonNull Song song) {
        LyricsSearchDialog dialog = new LyricsSearchDialog();
        Bundle args = new Bundle();
        args.putParcelable(EXTRA_SONG, song);
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle arguments = getArguments();
        song = arguments == null ? Song.EMPTY_SONG : arguments.getParcelable(EXTRA_SONG);
        if (song == null) {
            song = Song.EMPTY_SONG;
        }

        Context context = requireContext();
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(context, 20);
        root.setPadding(padding, dp(context, 12), padding, 0);

        keywordView = new EditText(context);
        keywordView.setSingleLine(true);
        keywordView.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        keywordView.setHint(R.string.lyrics_search_keyword_hint);
        keywordView.setText(defaultKeyword(song));
        keywordView.setSelectAllOnFocus(true);
        root.addView(keywordView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        progressBar = new ProgressBar(context);
        progressBar.setIndeterminate(true);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        progressParams.gravity = Gravity.CENTER_HORIZONTAL;
        progressParams.topMargin = dp(context, 16);
        root.addView(progressBar, progressParams);

        messageView = new TextView(context);
        messageView.setGravity(Gravity.CENTER);
        messageView.setPadding(0, dp(context, 24), 0, dp(context, 24));
        root.addView(messageView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        resultsView = new ListView(context);
        adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, new ArrayList<>());
        resultsView.setAdapter(adapter);
        resultsView.setOnItemClickListener((parent, view, position, id) -> saveResult(position));
        root.addView(resultsView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(context, 300)));

        showIdleState();

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.action_search_lyrics)
                .setView(root)
                .setPositiveButton(R.string.action_search, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(view -> startSearch(keywordView.getText().toString()));
            keywordView.setOnEditorActionListener((view, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    startSearch(keywordView.getText().toString());
                    return true;
                }
                return false;
            });
            startSearch(keywordView.getText().toString());
        });
        return dialog;
    }

    @Override
    public void onDestroyView() {
        if (searchTask != null) {
            searchTask.cancel(true);
        }
        super.onDestroyView();
    }

    private void startSearch(@Nullable String keyword) {
        if (searchTask != null) {
            searchTask.cancel(true);
        }
        searchTask = new AsyncTask<String, Void, List<LyricsSearchResult>>() {
            @Override
            protected void onPreExecute() {
                showSearchingState();
            }

            @Override
            protected List<LyricsSearchResult> doInBackground(String... params) {
                LyricsSearchQuery query = LyricsSearchQuery.fromSong(song)
                        .withKeyword(params.length > 0 ? params[0] : null);
                Context context = getContext();
                if (context == null) {
                    return new ArrayList<>();
                }
                return LyricsRepository.createConfigured(context.getApplicationContext()).search(query);
            }

            @Override
            protected void onPostExecute(List<LyricsSearchResult> lyricsSearchResults) {
                results.clear();
                if (lyricsSearchResults != null) {
                    results.addAll(lyricsSearchResults);
                }
                showResultsState();
            }

            @Override
            protected void onCancelled(List<LyricsSearchResult> lyricsSearchResults) {
                showIdleState();
            }
        }.execute(keyword);
    }

    private void saveResult(int position) {
        if (position < 0 || position >= results.size()) {
            return;
        }
        Context context = getContext();
        if (context == null) {
            return;
        }
        try {
            LyricsCache.write(context.getApplicationContext(), song, results.get(position));
            Toast.makeText(context, R.string.lyrics_search_saved, Toast.LENGTH_SHORT).show();
            notifyLyricsSaved();
            dismiss();
        } catch (IOException e) {
            Toast.makeText(context, R.string.lyrics_search_save_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void notifyLyricsSaved() {
        Fragment target = getTargetFragment();
        if (target instanceof Callback) {
            ((Callback) target).onLyricsSearchResultSaved();
            return;
        }
        if (getActivity() instanceof Callback) {
            ((Callback) getActivity()).onLyricsSearchResultSaved();
        }
    }

    private void showIdleState() {
        if (progressBar != null) {
            progressBar.setVisibility(android.view.View.GONE);
        }
        if (messageView != null) {
            messageView.setVisibility(android.view.View.GONE);
        }
        if (resultsView != null) {
            resultsView.setVisibility(android.view.View.GONE);
        }
    }

    private void showSearchingState() {
        adapter.clear();
        results.clear();
        progressBar.setVisibility(android.view.View.VISIBLE);
        messageView.setVisibility(android.view.View.VISIBLE);
        messageView.setText(R.string.lyrics_searching);
        resultsView.setVisibility(android.view.View.GONE);
    }

    private void showResultsState() {
        progressBar.setVisibility(android.view.View.GONE);
        adapter.clear();
        for (LyricsSearchResult result : results) {
            adapter.add(describeResult(result));
        }
        adapter.notifyDataSetChanged();

        if (results.isEmpty()) {
            messageView.setText(R.string.lyrics_search_no_results);
            messageView.setVisibility(android.view.View.VISIBLE);
            resultsView.setVisibility(android.view.View.GONE);
        } else {
            messageView.setVisibility(android.view.View.GONE);
            resultsView.setVisibility(android.view.View.VISIBLE);
        }
    }

    @NonNull
    private String describeResult(@NonNull LyricsSearchResult result) {
        String title = TextUtils.isEmpty(result.title) ? song.title : result.title;
        String artist = TextUtils.isEmpty(result.artist) ? song.artistName : result.artist;
        String type = getString(result.synchronizedLyrics
                ? R.string.lyrics_search_result_synced
                : R.string.lyrics_search_result_plain);
        return getString(R.string.lyrics_search_result_format, result.provider, type, title, artist);
    }

    @NonNull
    private static String defaultKeyword(@NonNull Song song) {
        String title = song.title == null ? "" : song.title;
        String artist = song.artistName == null ? "" : song.artistName;
        return (title + " " + artist).trim();
    }

    private static int dp(@NonNull Context context, int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }
}
