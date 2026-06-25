package com.kabouzeid.gramophone.ui.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.navigation.NavigationView;
import androidx.fragment.app.Fragment;
import androidx.drawerlayout.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import com.kabouzeid.appthemehelper.ThemeStore;
import com.kabouzeid.appthemehelper.util.ATHUtil;
import com.kabouzeid.appthemehelper.util.NavigationViewUtil;
import com.kabouzeid.gramophone.R;
import com.kabouzeid.gramophone.dialogs.ChangelogDialog;
import com.kabouzeid.gramophone.glide.SongGlideRequest;
import com.kabouzeid.gramophone.helper.MusicPlayerRemote;
import com.kabouzeid.gramophone.helper.SearchQueryHelper;
import com.kabouzeid.gramophone.loader.AlbumLoader;
import com.kabouzeid.gramophone.loader.ArtistLoader;
import com.kabouzeid.gramophone.loader.PlaylistSongLoader;
import com.kabouzeid.gramophone.misc.UpdateToastMediaScannerCompletionListener;
import com.kabouzeid.gramophone.model.Song;
import com.kabouzeid.gramophone.service.MusicService;
import com.kabouzeid.gramophone.source.MediaSource;
import com.kabouzeid.gramophone.source.MediaSourceManager;
import com.kabouzeid.gramophone.ui.activities.base.AbsSlidingMusicPanelActivity;
import com.kabouzeid.gramophone.ui.activities.intro.AppIntroActivity;
import com.kabouzeid.gramophone.ui.fragments.mainactivity.folders.FoldersFragment;
import com.kabouzeid.gramophone.ui.fragments.mainactivity.library.LibraryFragment;
import com.kabouzeid.gramophone.util.MusicUtil;
import com.kabouzeid.gramophone.util.PreferenceUtil;
import com.kabouzeid.gramophone.util.StorageAccessUtil;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AbsSlidingMusicPanelActivity {

    public static final String TAG = MainActivity.class.getSimpleName();
    public static final int APP_INTRO_REQUEST = 100;
    private static final int NOTIFICATION_PERMISSION_REQUEST = 101;

    private static final int LIBRARY = 0;
    private static final int FOLDERS = 1;
    private static final int DYNAMIC_SOURCE_ITEM_ID = 100000;
    private static final int DYNAMIC_SOURCE_ITEM_ID_LIMIT = 200000;
    private static final String EXTRA_SOURCE_ID = "extra_source_id";

    NavigationView navigationView;
    DrawerLayout drawerLayout;

    @Nullable
    MainActivityFragmentCallbacks currentFragment;

    @Nullable
    private View navigationDrawerHeader;

    private ActivityResultLauncher<Uri> scanMediaFolderLauncher;

    private boolean blockRequestPermissions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        scanMediaFolderLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), this::scanSelectedMediaFolder);
        setDrawUnderStatusbar();
        navigationView = findViewById(R.id.navigation_view);
        drawerLayout = findViewById(R.id.drawer_layout);

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            navigationView.setFitsSystemWindows(false); // for header to go below statusbar
        }

        setUpDrawerLayout();

        if (savedInstanceState == null) {
            int lastMusicChooser = PreferenceUtil.getInstance(this).getLastMusicChooser();
            String currentSourceId = MediaSourceManager.getCurrentSourceId(this);
            if (lastMusicChooser == LIBRARY && !MediaSourceManager.isLocalSource(currentSourceId)) {
                setLibrarySource(currentSourceId);
            } else {
                setMusicChooser(lastMusicChooser);
            }
        } else {
            restoreCurrentFragment();
        }

        if (!checkShowIntro()) {
            showChangelog();
        }

        requestNotificationPermissionIfNeeded();
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && hasPermissions()
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST);
        }
    }

    private void setMusicChooser(int key) {
        PreferenceUtil.getInstance(this).setLastMusicChooser(key);
        switch (key) {
            case LIBRARY:
                setLibrarySource(MediaSourceManager.LOCAL_SOURCE_ID);
                break;
            case FOLDERS:
                MediaSourceManager.setCurrentSourceId(this, MediaSourceManager.LOCAL_SOURCE_ID);
                refreshMediaSourceMenu();
                navigationView.setCheckedItem(R.id.nav_folders);
                setCurrentFragment(FoldersFragment.newInstance(this));
                break;
        }
    }

    private void setLibrarySource(@NonNull String sourceId) {
        if (MediaSourceManager.getSource(this, sourceId) == null) {
            sourceId = MediaSourceManager.LOCAL_SOURCE_ID;
        }
        PreferenceUtil.getInstance(this).setLastMusicChooser(LIBRARY);
        MediaSourceManager.setCurrentSourceId(this, sourceId);
        refreshMediaSourceMenu();
        setCurrentFragment(LibraryFragment.newInstance());
    }

    private void setCurrentFragment(@SuppressWarnings("NullableProblems") Fragment fragment) {
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment, null).commit();
        currentFragment = (MainActivityFragmentCallbacks) fragment;
    }

    private void restoreCurrentFragment() {
        currentFragment = (MainActivityFragmentCallbacks) getSupportFragmentManager().findFragmentById(R.id.fragment_container);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == APP_INTRO_REQUEST) {
            blockRequestPermissions = false;
            if (!hasPermissions()) {
                requestPermissions();
            }
        }
    }

    @Override
    protected void requestPermissions() {
        if (!blockRequestPermissions) super.requestPermissions();
    }

    @Override
    protected void onHasPermissionsChanged(boolean hasPermissions) {
        super.onHasPermissionsChanged(hasPermissions);
        if (hasPermissions) {
            requestNotificationPermissionIfNeeded();
        }
    }

    @Override
    protected View createContentView() {
        @SuppressLint("InflateParams")
        View contentView = getLayoutInflater().inflate(R.layout.activity_main_drawer_layout, null);
        ViewGroup drawerContent = contentView.findViewById(R.id.drawer_content_container);
        drawerContent.addView(wrapSlidingMusicPanel(R.layout.activity_main_content));
        return contentView;
    }

    private void setUpNavigationView() {
        int accentColor = ThemeStore.accentColor(this);
        NavigationViewUtil.setItemIconColors(navigationView, ATHUtil.resolveColor(this, R.attr.iconColor, ThemeStore.textColorSecondary(this)), accentColor);
        NavigationViewUtil.setItemTextColors(navigationView, ThemeStore.textColorPrimary(this), accentColor);
        refreshMediaSourceMenu();

        navigationView.setNavigationItemSelectedListener(menuItem -> {
            drawerLayout.closeDrawers();
            String sourceId = getMenuSourceId(menuItem);
            if (sourceId != null) {
                new Handler().postDelayed(() -> setLibrarySource(sourceId), 200);
            } else if (menuItem.getItemId() == R.id.nav_folders) {
                new Handler().postDelayed(() -> setMusicChooser(FOLDERS), 200);
            } else if (menuItem.getItemId() == R.id.action_scan) {
                new Handler().postDelayed(() -> {
                    if (scanMediaFolderLauncher != null) {
                        scanMediaFolderLauncher.launch(null);
                    }
                }, 200);
            } else if (menuItem.getItemId() == R.id.action_add_subsonic_server) {
                new Handler().postDelayed(() -> startActivity(new Intent(MainActivity.this, SubsonicServersActivity.class)), 200);
            } else if (menuItem.getItemId() == R.id.nav_settings) {
                new Handler().postDelayed(() -> startActivity(new Intent(MainActivity.this, SettingsActivity.class)), 200);
            } else if (menuItem.getItemId() == R.id.nav_about) {
                new Handler().postDelayed(() -> startActivity(new Intent(MainActivity.this, AboutActivity.class)), 200);
            }
            return true;
        });
    }

    @Nullable
    private String getMenuSourceId(@NonNull MenuItem menuItem) {
        Intent intent = menuItem.getIntent();
        return intent == null ? null : intent.getStringExtra(EXTRA_SOURCE_ID);
    }

    private void refreshMediaSourceMenu() {
        if (navigationView == null) {
            return;
        }

        Menu menu = navigationView.getMenu();
        for (int i = menu.size() - 1; i >= 0; i--) {
            int itemId = menu.getItem(i).getItemId();
            if (itemId >= DYNAMIC_SOURCE_ITEM_ID && itemId < DYNAMIC_SOURCE_ITEM_ID_LIMIT) {
                menu.removeItem(itemId);
            }
        }

        MenuItem localLibrary = menu.findItem(R.id.nav_library);
        if (localLibrary != null) {
            localLibrary.setIntent(new Intent().putExtra(EXTRA_SOURCE_ID, MediaSourceManager.LOCAL_SOURCE_ID));
        }

        int sourceIndex = 0;
        for (MediaSource source : MediaSourceManager.getAvailableSources(this)) {
            if (source.isLocal()) {
                continue;
            }
            menu.add(R.id.navigation_drawer_menu_category_sections,
                    DYNAMIC_SOURCE_ITEM_ID + sourceIndex,
                    2 + sourceIndex,
                    source.name)
                    .setIcon(R.drawable.ic_library_music_white_24dp)
                    .setCheckable(true)
                    .setIntent(new Intent().putExtra(EXTRA_SOURCE_ID, source.id));
            sourceIndex++;
        }

        int lastMusicChooser = PreferenceUtil.getInstance(this).getLastMusicChooser();
        if (lastMusicChooser == FOLDERS) {
            navigationView.setCheckedItem(R.id.nav_folders);
            return;
        }

        String currentSourceId = MediaSourceManager.getCurrentSourceId(this);
        if (MediaSourceManager.isLocalSource(currentSourceId)) {
            navigationView.setCheckedItem(R.id.nav_library);
            return;
        }

        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (currentSourceId.equals(getMenuSourceId(item))) {
                item.setChecked(true);
                return;
            }
        }

        MediaSourceManager.setCurrentSourceId(this, MediaSourceManager.LOCAL_SOURCE_ID);
        navigationView.setCheckedItem(R.id.nav_library);
    }

    private void scanSelectedMediaFolder(@Nullable Uri uri) {
        if (uri == null) {
            return;
        }

        StorageAccessUtil.takePersistableReadPermission(this, uri);
        File folder = StorageAccessUtil.getFileFromTreeUri(this, uri);
        if (folder == null || !folder.exists() || !folder.isDirectory()) {
            Toast.makeText(this, R.string.selected_folder_not_accessible, Toast.LENGTH_SHORT).show();
            return;
        }

        new FoldersFragment.ArrayListPathsAsyncTask(this, this::scanPaths)
                .execute(new FoldersFragment.ArrayListPathsAsyncTask.LoadingInfo(folder, FoldersFragment.AUDIO_FILE_FILTER));
    }

    private void scanPaths(@Nullable String[] toBeScanned) {
        if (toBeScanned == null || toBeScanned.length < 1) {
            Toast.makeText(this, R.string.nothing_to_scan, Toast.LENGTH_SHORT).show();
        } else {
            MediaScannerConnection.scanFile(
                    getApplicationContext(),
                    toBeScanned,
                    null,
                    new UpdateToastMediaScannerCompletionListener(this, toBeScanned)
            );
        }
    }

    private void setUpDrawerLayout() {
        setUpNavigationView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshMediaSourceMenu();
    }

    private void updateNavigationDrawerHeader() {
        if (!MusicPlayerRemote.getPlayingQueue().isEmpty()) {
            Song song = MusicPlayerRemote.getCurrentSong();
            if (navigationDrawerHeader == null) {
                navigationDrawerHeader = navigationView.inflateHeaderView(R.layout.navigation_drawer_header);
                //noinspection ConstantConditions
                navigationDrawerHeader.setOnClickListener(v -> {
                    drawerLayout.closeDrawers();
                    if (getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                        expandPanel();
                    }
                });
            }
            ((TextView) navigationDrawerHeader.findViewById(R.id.title)).setText(song.title);
            ((TextView) navigationDrawerHeader.findViewById(R.id.text)).setText(MusicUtil.getSongInfoString(song));
            SongGlideRequest.Builder.from(Glide.with(this), song)
                    .checkIgnoreMediaStore(this).build()
                    .into(((ImageView) navigationDrawerHeader.findViewById(R.id.image)));
        } else {
            if (navigationDrawerHeader != null) {
                navigationView.removeHeaderView(navigationDrawerHeader);
                navigationDrawerHeader = null;
            }
        }
    }

    @Override
    public void onPlayingMetaChanged() {
        super.onPlayingMetaChanged();
        updateNavigationDrawerHeader();
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        updateNavigationDrawerHeader();
        handlePlaybackIntent(getIntent());
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (drawerLayout.isDrawerOpen(navigationView)) {
                drawerLayout.closeDrawer(navigationView);
            } else {
                drawerLayout.openDrawer(navigationView);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean handleBackPress() {
        if (drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawers();
            return true;
        }
        return super.handleBackPress() || (currentFragment != null && currentFragment.handleBackPress());
    }

    private void handlePlaybackIntent(@Nullable Intent intent) {
        if (intent == null) {
            return;
        }

        Uri uri = intent.getData();
        String mimeType = intent.getType();
        boolean handled = false;

        if (intent.getAction() != null && intent.getAction().equals(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)) {
            final List<Song> songs = SearchQueryHelper.getSongs(this, intent.getExtras());
            if (MusicPlayerRemote.getShuffleMode() == MusicService.SHUFFLE_MODE_SHUFFLE) {
                MusicPlayerRemote.openAndShuffleQueue(songs, true);
            } else {
                MusicPlayerRemote.openQueue(songs, 0, true);
            }
            handled = true;
        }

        if (uri != null && uri.toString().length() > 0) {
            MusicPlayerRemote.playFromUri(uri);
            handled = true;
        } else if (MediaStore.Audio.Playlists.CONTENT_TYPE.equals(mimeType)) {
            final long id = parseIdFromIntent(intent, "playlistId", "playlist");
            if (id >= 0) {
                int position = intent.getIntExtra("position", 0);
                List<Song> songs = new ArrayList<>(PlaylistSongLoader.getPlaylistSongList(this, id));
                MusicPlayerRemote.openQueue(songs, position, true);
                handled = true;
            }
        } else if (MediaStore.Audio.Albums.CONTENT_TYPE.equals(mimeType)) {
            final long id = parseIdFromIntent(intent, "albumId", "album");
            if (id >= 0) {
                int position = intent.getIntExtra("position", 0);
                MusicPlayerRemote.openQueue(AlbumLoader.getAlbum(this, id).songs, position, true);
                handled = true;
            }
        } else if (MediaStore.Audio.Artists.CONTENT_TYPE.equals(mimeType)) {
            final long id = parseIdFromIntent(intent, "artistId", "artist");
            if (id >= 0) {
                int position = intent.getIntExtra("position", 0);
                MusicPlayerRemote.openQueue(ArtistLoader.getArtist(this, id).getSongs(), position, true);
                handled = true;
            }
        }
        if (handled) {
            setIntent(new Intent());
        }
    }

    private long parseIdFromIntent(@NonNull Intent intent, String longKey,
                                   String stringKey) {
        long id = intent.getLongExtra(longKey, -1);
        if (id < 0) {
            String idString = intent.getStringExtra(stringKey);
            if (idString != null) {
                try {
                    id = Long.parseLong(idString);
                } catch (NumberFormatException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        }
        return id;
    }

    @Override
    public void onPanelExpanded(View view) {
        super.onPanelExpanded(view);
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }

    @Override
    public void onPanelCollapsed(View view) {
        super.onPanelCollapsed(view);
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
    }

    private boolean checkShowIntro() {
        if (!PreferenceUtil.getInstance(this).introShown()) {
            PreferenceUtil.getInstance(this).setIntroShown();
            ChangelogDialog.setChangelogRead(this);
            blockRequestPermissions = true;
            new Handler().postDelayed(() -> startActivityForResult(new Intent(MainActivity.this, AppIntroActivity.class), APP_INTRO_REQUEST), 50);
            return true;
        }
        return false;
    }

    private void showChangelog() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            int currentVersion = pInfo.versionCode;
            if (currentVersion != PreferenceUtil.getInstance(this).getLastChangelogVersion()) {
                ChangelogDialog.create().show(getSupportFragmentManager(), "CHANGE_LOG_DIALOG");
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    public interface MainActivityFragmentCallbacks {
        boolean handleBackPress();
    }
}
