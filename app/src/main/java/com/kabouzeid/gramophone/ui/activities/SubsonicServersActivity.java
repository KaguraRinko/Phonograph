package com.kabouzeid.gramophone.ui.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kabouzeid.appthemehelper.ThemeStore;
import com.kabouzeid.gramophone.R;
import com.kabouzeid.gramophone.source.MediaSourceManager;
import com.kabouzeid.gramophone.subsonic.SubsonicException;
import com.kabouzeid.gramophone.subsonic.SubsonicLibraryStore;
import com.kabouzeid.gramophone.subsonic.SubsonicServer;
import com.kabouzeid.gramophone.subsonic.SubsonicServerStore;
import com.kabouzeid.gramophone.subsonic.SubsonicSyncer;
import com.kabouzeid.gramophone.subsonic.SubsonicUrlUtil;
import com.kabouzeid.gramophone.subsonic.rest.SubsonicRestClient;
import com.kabouzeid.gramophone.ui.activities.base.AbsBaseActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SubsonicServersActivity extends AbsBaseActivity {
    private static final int MENU_ADD = 1;
    private static final int MENU_EDIT = 2;
    private static final int MENU_TEST = 3;
    private static final int MENU_SYNC = 4;
    private static final int MENU_DELETE = 5;

    private Toolbar toolbar;
    private RecyclerView recyclerView;
    private TextView emptyView;
    private ServerAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subsonic_servers);
        setDrawUnderStatusbar();
        toolbar = findViewById(R.id.toolbar);
        recyclerView = findViewById(R.id.recycler_view);
        emptyView = findViewById(android.R.id.empty);

        setStatusbarColorAuto();
        setNavigationbarColorAuto();
        setTaskDescriptionColorAuto();

        toolbar.setBackgroundColor(ThemeStore.primaryColor(this));
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.subsonic_servers);

        adapter = new ServerAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        reloadServers();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_ADD, 0, R.string.add_subsonic_server)
                .setIcon(R.drawable.ic_library_add_white_24dp)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        if (item.getItemId() == MENU_ADD) {
            showServerDialog(null);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void reloadServers() {
        List<SubsonicServer> servers = SubsonicServerStore.getInstance(this).getAllServers();
        adapter.setServers(servers);
        emptyView.setVisibility(servers.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showServerDialog(@Nullable SubsonicServer server) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_subsonic_server, null);
        EditText name = view.findViewById(R.id.name);
        EditText url = view.findViewById(R.id.url);
        EditText username = view.findViewById(R.id.username);
        EditText password = view.findViewById(R.id.password);

        if (server != null) {
            name.setText(server.name);
            url.setText(server.baseUrl);
            username.setText(server.username);
            password.setText(server.password);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(server == null ? R.string.add_subsonic_server : R.string.edit_subsonic_server)
                .setView(view)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, null)
                .create();
        dialog.setOnShowListener(shownDialog -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button -> {
            SubsonicServer editedServer = createServerFromFields(server, name, url, username, password);
            if (editedServer == null) {
                Toast.makeText(this, R.string.server_fields_required, Toast.LENGTH_SHORT).show();
                return;
            }
            if (editedServer.baseUrl.startsWith("http://")) {
                Toast.makeText(this, R.string.http_server_warning, Toast.LENGTH_LONG).show();
            }
            if (editedServer.id == SubsonicServer.NO_ID) {
                SubsonicServerStore.getInstance(this).insertServer(editedServer);
            } else {
                SubsonicServerStore.getInstance(this).updateServer(editedServer);
            }
            reloadServers();
            dialog.dismiss();
        }));
        dialog.show();
    }

    @Nullable
    private SubsonicServer createServerFromFields(@Nullable SubsonicServer original,
                                                  @NonNull EditText name,
                                                  @NonNull EditText url,
                                                  @NonNull EditText username,
                                                  @NonNull EditText password) {
        String baseUrl = url.getText().toString().trim();
        String user = username.getText().toString().trim();
        String pass = password.getText().toString();
        if (baseUrl.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            return null;
        }
        baseUrl = SubsonicUrlUtil.normalizeServerUrl(baseUrl);
        String displayName = name.getText().toString().trim();
        if (displayName.isEmpty()) {
            displayName = baseUrl;
        }
        long id = original == null ? SubsonicServer.NO_ID : original.id;
        long lastSynced = original == null ? 0 : original.lastSynced;
        return new SubsonicServer(id, displayName, baseUrl, user, pass, lastSynced);
    }

    private void testServer(@NonNull SubsonicServer server) {
        Toast.makeText(this, R.string.updating, Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                new SubsonicRestClient(SubsonicServersActivity.this, server).ping();
                runOnUiThread(() -> Toast.makeText(SubsonicServersActivity.this, R.string.connection_successful, Toast.LENGTH_SHORT).show());
            } catch (IOException | SubsonicException e) {
                runOnUiThread(() -> Toast.makeText(SubsonicServersActivity.this, getString(R.string.connection_failed_x, e.getMessage()), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void syncServer(@NonNull SubsonicServer server) {
        Toast.makeText(this, R.string.sync_started, Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                new SubsonicSyncer(SubsonicServersActivity.this, server).sync();
                runOnUiThread(() -> {
                    reloadServers();
                    Toast.makeText(SubsonicServersActivity.this, R.string.sync_finished, Toast.LENGTH_SHORT).show();
                });
            } catch (IOException | SubsonicException e) {
                runOnUiThread(() -> Toast.makeText(SubsonicServersActivity.this, getString(R.string.sync_failed_x, e.getMessage()), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void deleteServer(@NonNull SubsonicServer server) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.action_delete)
                .setMessage(getString(R.string.delete_server_x, server.name))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.delete_action, (dialog, which) -> {
                    SubsonicLibraryStore.getInstance(this).clearServerData(server.id);
                    SubsonicServerStore.getInstance(this).deleteServer(server.id);
                    String currentSourceId = MediaSourceManager.getCurrentSourceId(this);
                    if (MediaSourceManager.toSubsonicSourceId(server.id).equals(currentSourceId)) {
                        MediaSourceManager.setCurrentSourceId(this, MediaSourceManager.LOCAL_SOURCE_ID);
                    }
                    reloadServers();
                })
                .show();
    }

    private class ServerAdapter extends RecyclerView.Adapter<ServerViewHolder> {
        private final List<SubsonicServer> servers = new ArrayList<>();

        void setServers(@NonNull List<SubsonicServer> servers) {
            this.servers.clear();
            this.servers.addAll(servers);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ServerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ServerViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list_no_image, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ServerViewHolder holder, int position) {
            holder.bind(servers.get(position));
        }

        @Override
        public int getItemCount() {
            return servers.size();
        }
    }

    private class ServerViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView text;
        private final View menu;

        ServerViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            text = itemView.findViewById(R.id.text);
            menu = itemView.findViewById(R.id.menu);
        }

        void bind(@NonNull SubsonicServer server) {
            title.setText(server.name);
            text.setText(server.baseUrl + " - " + server.username);
            itemView.setOnClickListener(v -> syncServer(server));
            menu.setOnClickListener(v -> {
                PopupMenu popupMenu = new PopupMenu(SubsonicServersActivity.this, v);
                popupMenu.getMenu().add(0, MENU_TEST, 0, R.string.test_connection);
                popupMenu.getMenu().add(0, MENU_SYNC, 1, R.string.sync_library);
                popupMenu.getMenu().add(0, MENU_EDIT, 2, R.string.action_rename);
                popupMenu.getMenu().add(0, MENU_DELETE, 3, R.string.action_delete);
                popupMenu.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == MENU_TEST) {
                        testServer(server);
                        return true;
                    } else if (item.getItemId() == MENU_SYNC) {
                        syncServer(server);
                        return true;
                    } else if (item.getItemId() == MENU_EDIT) {
                        showServerDialog(server);
                        return true;
                    } else if (item.getItemId() == MENU_DELETE) {
                        deleteServer(server);
                        return true;
                    }
                    return false;
                });
                popupMenu.show();
            });
        }
    }
}
