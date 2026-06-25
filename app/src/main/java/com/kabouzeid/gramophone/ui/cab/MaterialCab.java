package com.kabouzeid.gramophone.ui.cab;

import android.app.Activity;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.MenuRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

/**
 * Small compatibility wrapper for the removed external material-cab dependency.
 */
public class MaterialCab {
    private final Activity activity;
    @SuppressWarnings("unused")
    private final int stubId;

    @MenuRes
    private int menuRes;
    @Nullable
    private ActionMode actionMode;
    @Nullable
    private CharSequence title;

    public MaterialCab(Activity activity, int stubId) {
        this.activity = activity;
        this.stubId = stubId;
    }

    public MaterialCab setMenu(@MenuRes int menuRes) {
        this.menuRes = menuRes;
        return this;
    }

    public MaterialCab setCloseDrawableRes(@DrawableRes int closeDrawableRes) {
        return this;
    }

    public MaterialCab setBackgroundColor(@ColorInt int color) {
        return this;
    }

    public MaterialCab setTitle(@Nullable CharSequence title) {
        this.title = title;
        if (actionMode != null) {
            actionMode.setTitle(title);
        }
        return this;
    }

    public boolean isActive() {
        return actionMode != null;
    }

    public void finish() {
        if (actionMode != null) {
            actionMode.finish();
        }
    }

    public MaterialCab start(final Callback callback) {
        if (!(activity instanceof AppCompatActivity)) {
            throw new IllegalStateException("MaterialCab requires an AppCompatActivity host");
        }

        actionMode = ((AppCompatActivity) activity).startSupportActionMode(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                if (menuRes != 0) {
                    mode.getMenuInflater().inflate(menuRes, menu);
                }
                if (title != null) {
                    mode.setTitle(title);
                }
                return callback.onCabCreated(MaterialCab.this, menu);
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                return callback.onCabItemClicked(item);
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                actionMode = null;
                callback.onCabFinished(MaterialCab.this);
            }
        });
        return this;
    }

    public interface Callback {
        boolean onCabCreated(MaterialCab materialCab, Menu menu);

        boolean onCabItemClicked(MenuItem menuItem);

        boolean onCabFinished(MaterialCab materialCab);
    }
}
