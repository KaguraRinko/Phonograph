package com.kabouzeid.gramophone.ui.fragments.mainactivity.library.pager;

import android.os.Bundle;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.kabouzeid.appthemehelper.ThemeStore;
import com.kabouzeid.gramophone.R;
import com.kabouzeid.gramophone.source.MediaSourceManager;
import com.kabouzeid.gramophone.subsonic.SubsonicSyncState;
import com.kabouzeid.gramophone.util.ViewUtil;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;


/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public abstract class AbsLibraryPagerRecyclerViewFragment<A extends RecyclerView.Adapter, LM extends RecyclerView.LayoutManager> extends AbsLibraryPagerFragment implements OnOffsetChangedListener {


    View container;
    RecyclerView recyclerView;
    @Nullable
    TextView empty;
    @Nullable
    private View syncProgressContainer;
    @Nullable
    private ProgressBar syncProgressBar;
    @Nullable
    private TextView syncProgressMessage;

    private A adapter;
    private LM layoutManager;
    private boolean showingSyncProgress;
    private final SubsonicSyncState.Listener syncStateListener = this::updateSyncProgress;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(getLayoutRes(), container, false);
        this.container = view.findViewById(R.id.container);
        recyclerView = view.findViewById(R.id.recycler_view);
        empty = view.findViewById(android.R.id.empty);
        syncProgressContainer = view.findViewById(R.id.sync_progress_container);
        syncProgressBar = view.findViewById(R.id.sync_progress_bar);
        syncProgressMessage = view.findViewById(R.id.sync_progress_message);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getLibraryFragment().addOnAppBarOffsetChangedListener(this);

        initLayoutManager();
        initAdapter();
        setUpRecyclerView();
        SubsonicSyncState.addListener(syncStateListener);
        updateSyncProgress(SubsonicSyncState.getSnapshot());
    }

    private void setUpRecyclerView() {
        if (recyclerView instanceof FastScrollRecyclerView) {
            ViewUtil.setUpFastScrollRecyclerViewColor(getActivity(), ((FastScrollRecyclerView) recyclerView), ThemeStore.accentColor(getActivity()));
        }
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
    }

    protected void invalidateLayoutManager() {
        initLayoutManager();
        recyclerView.setLayoutManager(layoutManager);
    }

    protected void invalidateAdapter() {
        initAdapter();
        checkIsEmpty();
        recyclerView.setAdapter(adapter);
    }

    private void initAdapter() {
        adapter = createAdapter();
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                checkIsEmpty();
            }
        });
    }

    private void initLayoutManager() {
        layoutManager = createLayoutManager();
    }

    protected A getAdapter() {
        return adapter;
    }

    protected LM getLayoutManager() {
        return layoutManager;
    }

    protected RecyclerView getRecyclerView() {
        return recyclerView;
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int i) {
        if (container == null) return;
        container.setPadding(container.getPaddingLeft(), container.getPaddingTop(), container.getPaddingRight(), getLibraryFragment().getTotalAppBarScrollingRange() + i);
    }

    private void checkIsEmpty() {
        if (empty != null) {
            empty.setText(getEmptyMessage());
            empty.setVisibility(!showingSyncProgress && (adapter == null || adapter.getItemCount() == 0) ? View.VISIBLE : View.GONE);
        }
    }

    private void updateSyncProgress(@Nullable SubsonicSyncState.Snapshot snapshot) {
        if (syncProgressContainer == null || getContext() == null) {
            return;
        }

        long currentServerId = MediaSourceManager.getSubsonicServerId(MediaSourceManager.getCurrentSourceId(getContext()));
        boolean matchesCurrentSource = snapshot != null && snapshot.serverId == currentServerId;
        boolean shouldShowProgress = matchesCurrentSource && snapshot.running;

        if (shouldShowProgress) {
            syncProgressContainer.setVisibility(View.VISIBLE);
            if (syncProgressBar != null) {
                syncProgressBar.setProgress(snapshot.progress);
            }
            if (syncProgressMessage != null) {
                syncProgressMessage.setText(getString(R.string.subsonic_syncing_progress_x, snapshot.progress, snapshot.message));
            }
        } else {
            syncProgressContainer.setVisibility(View.GONE);
        }

        if (showingSyncProgress && matchesCurrentSource && snapshot != null && !snapshot.running) {
            onMediaStoreChanged();
        }
        showingSyncProgress = shouldShowProgress;
        checkIsEmpty();
    }

    @StringRes
    protected int getEmptyMessage() {
        return R.string.empty;
    }

    @LayoutRes
    protected int getLayoutRes() {
        return R.layout.fragment_main_activity_recycler_view;
    }

    protected abstract LM createLayoutManager();

    @NonNull
    protected abstract A createAdapter();

    @Override
    public void onDestroyView() {
        SubsonicSyncState.removeListener(syncStateListener);
        super.onDestroyView();
        getLibraryFragment().removeOnAppBarOffsetChangedListener(this);
        syncProgressContainer = null;
        syncProgressBar = null;
        syncProgressMessage = null;
    }
}
