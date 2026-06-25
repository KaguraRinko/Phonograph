package com.kabouzeid.gramophone.ui.activities;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.kabouzeid.gramophone.ui.cab.MaterialCab;
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils;
import com.kabouzeid.appthemehelper.ThemeStore;
import com.kabouzeid.gramophone.R;
import com.kabouzeid.gramophone.adapter.song.SongAdapter;
import com.kabouzeid.gramophone.helper.MusicPlayerRemote;
import com.kabouzeid.gramophone.interfaces.CabHolder;
import com.kabouzeid.gramophone.interfaces.LoaderIds;
import com.kabouzeid.gramophone.loader.GenreLoader;
import com.kabouzeid.gramophone.misc.WrappedAsyncTaskLoader;
import com.kabouzeid.gramophone.model.Genre;
import com.kabouzeid.gramophone.model.Song;
import com.kabouzeid.gramophone.source.MediaSourceManager;
import com.kabouzeid.gramophone.ui.activities.base.AbsSlidingMusicPanelActivity;
import com.kabouzeid.gramophone.util.NavigationUtil;
import com.kabouzeid.gramophone.util.PhonographColorUtil;
import com.kabouzeid.gramophone.util.ViewUtil;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.util.ArrayList;
import java.util.List;


public class GenreDetailActivity extends AbsSlidingMusicPanelActivity implements CabHolder, LoaderManager.LoaderCallbacks<List<Song>> {

    private static final int LOADER_ID = LoaderIds.GENRE_DETAIL_ACTIVITY;

    public static final String EXTRA_GENRE = "extra_genre";

    RecyclerView recyclerView;
    Toolbar toolbar;
    TextView empty;

    private Genre genre;

    private MaterialCab cab;
    private SongAdapter adapter;
    private String sourceId;

    private RecyclerView.Adapter wrappedAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sourceId = NavigationUtil.getSourceId(this);
        setDrawUnderStatusbar();
        recyclerView = findViewById(R.id.recycler_view);
        toolbar = findViewById(R.id.toolbar);
        empty = findViewById(android.R.id.empty);

        setStatusbarColorAuto();
        setNavigationbarColorAuto();
        setTaskDescriptionColorAuto();

        genre = getIntent().getExtras().getParcelable(EXTRA_GENRE);

        setUpRecyclerView();

        setUpToolBar();

        getSupportLoaderManager().initLoader(LOADER_ID, getIntent().getExtras(), this);
    }

    @Override
    protected View createContentView() {
        return wrapSlidingMusicPanel(R.layout.activity_genre_detail);
    }

    private void setUpRecyclerView() {
        ViewUtil.setUpFastScrollRecyclerViewColor(this, ((FastScrollRecyclerView) recyclerView), ThemeStore.accentColor(this));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new SongAdapter(this, new ArrayList<>(), R.layout.item_list, false, this);
        recyclerView.setAdapter(adapter);

        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                checkIsEmpty();
            }
        });
    }

    private void setUpToolBar() {
        toolbar.setBackgroundColor(ThemeStore.primaryColor(this));
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setTitle(genre.name);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_genre_detail, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
                if (id == R.id.action_shuffle_genre) {
                MusicPlayerRemote.openAndShuffleQueue(adapter.getDataSet(), true);
                return true;
                    } else if (id == android.R.id.home) {
                getOnBackPressedDispatcher().onBackPressed();
                return true;
                }
        return super.onOptionsItemSelected(item);
    }

    @NonNull
    @Override
    public MaterialCab openCab(final int menu, final MaterialCab.Callback callback) {
        if (cab != null && cab.isActive()) cab.finish();
        cab = new MaterialCab(this, R.id.cab_stub)
                .setMenu(menu)
                .setCloseDrawableRes(R.drawable.ic_close_white_24dp)
                .setBackgroundColor(PhonographColorUtil.shiftBackgroundColorForLightText(ThemeStore.primaryColor(this)))
                .start(callback);
        return cab;
    }

    @Override
    public boolean handleBackPress() {
        if (cab != null && cab.isActive()) {
            cab.finish();
            return true;
        }
        recyclerView.stopScroll();
        return super.handleBackPress();
    }

    @Override
    public void onMediaStoreChanged() {
        super.onMediaStoreChanged();
        if (MediaSourceManager.isLocalSource(sourceId)) {
            getSupportLoaderManager().restartLoader(LOADER_ID, getIntent().getExtras(), this);
        }
    }

    private void checkIsEmpty() {
        empty.setVisibility(
                adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE
        );
    }

    @Override
    protected void onDestroy() {
        if (recyclerView != null) {
            recyclerView.setAdapter(null);
            recyclerView = null;
        }

        if (wrappedAdapter != null) {
            WrapperAdapterUtils.releaseAll(wrappedAdapter);
            wrappedAdapter = null;
        }
        adapter = null;

        super.onDestroy();
    }

    @Override
    @NonNull
    public Loader<List<Song>> onCreateLoader(int id, Bundle args) {
        return new GenreDetailActivity.AsyncGenreSongLoader(this, genre, NavigationUtil.getSourceId(this, args));
    }

    @Override
    public void onLoadFinished(@NonNull Loader<List<Song>> loader, List<Song> data) {
        if (adapter != null)
            adapter.swapDataSet(data);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<List<Song>> loader) {
        if (adapter != null)
            adapter.swapDataSet(new ArrayList<>());
    }

    private static class AsyncGenreSongLoader extends WrappedAsyncTaskLoader<List<Song>> {
        private final Genre genre;
        private final String sourceId;

        public AsyncGenreSongLoader(Context context, Genre genre, @NonNull String sourceId) {
            super(context);
            this.genre = genre;
            this.sourceId = sourceId;
        }

        @Override
        public List<Song> loadInBackground() {
            return MediaSourceManager.getRepository(getContext(), sourceId).getSongsForGenre(getContext(), genre.id);
        }
    }
}
