package com.kabouzeid.gramophone.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.kabouzeid.gramophone.R;
import com.kabouzeid.gramophone.lyrics.LyricsProviderRegistry;
import com.kabouzeid.gramophone.model.LyricsProviderInfo;
import com.kabouzeid.gramophone.util.SwipeAndDragHelper;

import java.util.List;

public class LyricsProviderInfoAdapter extends RecyclerView.Adapter<LyricsProviderInfoAdapter.ViewHolder>
        implements SwipeAndDragHelper.ActionCompletionContract {
    private List<LyricsProviderInfo> providers;
    private final ItemTouchHelper touchHelper;

    public LyricsProviderInfoAdapter(@NonNull List<LyricsProviderInfo> providers) {
        this.providers = providers;
        touchHelper = new ItemTouchHelper(new SwipeAndDragHelper(this));
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.preference_dialog_library_categories_listitem, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LyricsProviderInfo provider = providers.get(position);
        holder.checkBox.setChecked(provider.enabled);
        holder.title.setText(LyricsProviderRegistry.getDisplayName(holder.itemView.getContext(), provider.id));

        holder.itemView.setOnClickListener(v -> {
            if (provider.enabled && isLastEnabledProvider(provider)) {
                Toast.makeText(holder.itemView.getContext(),
                        R.string.you_have_to_enable_at_least_one_lyrics_provider,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            provider.enabled = !provider.enabled;
            holder.checkBox.setChecked(provider.enabled);
        });

        holder.dragView.setOnTouchListener((view, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                touchHelper.startDrag(holder);
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return providers.size();
    }

    @Override
    public void onViewMoved(int oldPosition, int newPosition) {
        LyricsProviderInfo provider = providers.remove(oldPosition);
        providers.add(newPosition, provider);
        notifyItemMoved(oldPosition, newPosition);
    }

    public void attachToRecyclerView(@NonNull RecyclerView recyclerView) {
        touchHelper.attachToRecyclerView(recyclerView);
    }

    @NonNull
    public List<LyricsProviderInfo> getProviders() {
        return providers;
    }

    public void setProviders(@NonNull List<LyricsProviderInfo> providers) {
        this.providers = providers;
        notifyDataSetChanged();
    }

    private boolean isLastEnabledProvider(@NonNull LyricsProviderInfo provider) {
        if (provider.enabled) {
            for (LyricsProviderInfo info : providers) {
                if (info != provider && info.enabled) {
                    return false;
                }
            }
        }
        return true;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final CheckBox checkBox;
        final TextView title;
        final View dragView;

        ViewHolder(@NonNull View view) {
            super(view);
            checkBox = view.findViewById(R.id.checkbox);
            title = view.findViewById(R.id.title);
            dragView = view.findViewById(R.id.drag_view);
        }
    }
}
