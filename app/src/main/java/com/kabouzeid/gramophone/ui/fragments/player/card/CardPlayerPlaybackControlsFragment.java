package com.kabouzeid.gramophone.ui.fragments.player.card;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.kabouzeid.appthemehelper.util.ColorUtil;
import com.kabouzeid.appthemehelper.util.MaterialValueHelper;
import com.kabouzeid.appthemehelper.util.TintHelper;
import com.kabouzeid.gramophone.R;
import com.kabouzeid.gramophone.helper.MusicPlayerRemote;
import com.kabouzeid.gramophone.helper.MusicProgressViewUpdateHelper;
import com.kabouzeid.gramophone.helper.PlayPauseButtonOnClickHandler;
import com.kabouzeid.gramophone.misc.SimpleOnSeekbarChangeListener;
import com.kabouzeid.gramophone.service.MusicService;
import com.kabouzeid.gramophone.ui.fragments.AbsMusicServiceFragment;
import com.kabouzeid.gramophone.util.MusicUtil;
import com.kabouzeid.gramophone.views.PlayPauseDrawable;


/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class CardPlayerPlaybackControlsFragment extends AbsMusicServiceFragment implements MusicProgressViewUpdateHelper.Callback {


    FloatingActionButton playPauseFab;
    ImageButton prevButton;
    ImageButton nextButton;
    ImageButton repeatButton;
    ImageButton shuffleButton;
    ProgressBar bufferingProgress;

    SeekBar progressSlider;
    TextView songTotalTime;
    TextView songCurrentProgress;

    private PlayPauseDrawable playerFabPlayPauseDrawable;
    private boolean showingBufferingIcon;

    private int lastPlaybackControlsColor;
    private int lastDisabledPlaybackControlsColor;

    private MusicProgressViewUpdateHelper progressViewUpdateHelper;
    private boolean progressSliderTracking;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        progressViewUpdateHelper = new MusicProgressViewUpdateHelper(this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_card_player_playback_controls, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        playPauseFab = view.findViewById(R.id.player_play_pause_fab);
        prevButton = view.findViewById(R.id.player_prev_button);
        nextButton = view.findViewById(R.id.player_next_button);
        repeatButton = view.findViewById(R.id.player_repeat_button);
        shuffleButton = view.findViewById(R.id.player_shuffle_button);
        bufferingProgress = view.findViewById(R.id.player_buffering_progress);
        progressSlider = view.findViewById(R.id.player_progress_slider);
        songTotalTime = view.findViewById(R.id.player_song_total_time);
        songCurrentProgress = view.findViewById(R.id.player_song_current_progress);
        setUpMusicControllers();
        updateProgressTextColor();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        progressViewUpdateHelper.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        progressViewUpdateHelper.stop();
    }

    @Override
    public void onServiceConnected() {
        updatePlayPauseDrawableState(false);
        updateRepeatState();
        updateShuffleState();
        onUpdateProgressViews(MusicPlayerRemote.getSongProgressMillis(), MusicPlayerRemote.getSongDurationMillis());
    }

    @Override
    public void onPlayStateChanged() {
        updatePlayPauseDrawableState(true);
    }

    @Override
    public void onBufferingStateChanged() {
        updatePlayPauseDrawableState(true);
        onUpdateProgressViews(MusicPlayerRemote.getSongProgressMillis(), MusicPlayerRemote.getSongDurationMillis());
    }

    @Override
    public void onRepeatModeChanged() {
        updateRepeatState();
    }

    @Override
    public void onShuffleModeChanged() {
        updateShuffleState();
    }

    public void setDark(boolean dark) {
        if (dark) {
            lastPlaybackControlsColor = MaterialValueHelper.getSecondaryTextColor(getActivity(), true);
            lastDisabledPlaybackControlsColor = MaterialValueHelper.getSecondaryDisabledTextColor(getActivity(), true);
        } else {
            lastPlaybackControlsColor = MaterialValueHelper.getPrimaryTextColor(getActivity(), false);
            lastDisabledPlaybackControlsColor = MaterialValueHelper.getPrimaryDisabledTextColor(getActivity(), false);
        }

        updateRepeatState();
        updateShuffleState();
        updatePrevNextColor();
        updateProgressTextColor();
    }

    private void setUpPlayPauseFab() {
        final int fabColor = Color.WHITE;
        TintHelper.setTintAuto(playPauseFab, fabColor, true);

        playerFabPlayPauseDrawable = new PlayPauseDrawable(getActivity());

        playPauseFab.setImageDrawable(playerFabPlayPauseDrawable); // Note: set the drawable AFTER TintHelper.setTintAuto() was called
        playPauseFab.setColorFilter(MaterialValueHelper.getPrimaryTextColor(getContext(), ColorUtil.isColorLight(fabColor)), PorterDuff.Mode.SRC_IN);
        PlayPauseButtonOnClickHandler clickHandler = new PlayPauseButtonOnClickHandler();
        playPauseFab.setOnClickListener(clickHandler);
        bufferingProgress.setOnClickListener(clickHandler);
        playPauseFab.post(() -> {
            if (playPauseFab != null) {
                playPauseFab.setPivotX(playPauseFab.getWidth() / 2);
                playPauseFab.setPivotY(playPauseFab.getHeight() / 2);
            }
        });
    }

    protected void updatePlayPauseDrawableState(boolean animate) {
        if (MusicPlayerRemote.isBuffering()) {
            if (!showingBufferingIcon) {
                playPauseFab.setImageDrawable(null);
                playPauseFab.setVisibility(View.VISIBLE);
                bufferingProgress.setVisibility(View.VISIBLE);
                showingBufferingIcon = true;
            }
        } else if (showingBufferingIcon) {
            bufferingProgress.setVisibility(View.GONE);
            playPauseFab.setVisibility(View.VISIBLE);
            playPauseFab.setImageDrawable(playerFabPlayPauseDrawable);
            showingBufferingIcon = false;
        }
        if (MusicPlayerRemote.isPlaying()) {
            playerFabPlayPauseDrawable.setPause(animate);
        } else {
            playerFabPlayPauseDrawable.setPlay(animate);
        }
    }

    private void setUpMusicControllers() {
        setUpPlayPauseFab();
        setUpPrevNext();
        setUpRepeatButton();
        setUpShuffleButton();
        setUpProgressSlider();
    }

    private void setUpPrevNext() {
        updatePrevNextColor();
        nextButton.setOnClickListener(v -> MusicPlayerRemote.playNextSong());
        prevButton.setOnClickListener(v -> MusicPlayerRemote.back());
    }

    private void updateProgressTextColor() {
        int color = MaterialValueHelper.getPrimaryTextColor(getContext(), false);
        songTotalTime.setTextColor(color);
        songCurrentProgress.setTextColor(color);
    }

    private void updatePrevNextColor() {
        nextButton.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN);
        prevButton.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN);
    }

    private void setUpShuffleButton() {
        shuffleButton.setOnClickListener(v -> MusicPlayerRemote.toggleShuffleMode());
    }

    private void updateShuffleState() {
        switch (MusicPlayerRemote.getShuffleMode()) {
            case MusicService.SHUFFLE_MODE_SHUFFLE:
                shuffleButton.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN);
                break;
            default:
                shuffleButton.setColorFilter(lastDisabledPlaybackControlsColor, PorterDuff.Mode.SRC_IN);
                break;
        }
    }

    private void setUpRepeatButton() {
        repeatButton.setOnClickListener(v -> MusicPlayerRemote.cycleRepeatMode());
    }

    private void updateRepeatState() {
        switch (MusicPlayerRemote.getRepeatMode()) {
            case MusicService.REPEAT_MODE_NONE:
                repeatButton.setImageResource(R.drawable.ic_repeat_white_24dp);
                repeatButton.setColorFilter(lastDisabledPlaybackControlsColor, PorterDuff.Mode.SRC_IN);
                break;
            case MusicService.REPEAT_MODE_ALL:
                repeatButton.setImageResource(R.drawable.ic_repeat_white_24dp);
                repeatButton.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN);
                break;
            case MusicService.REPEAT_MODE_THIS:
                repeatButton.setImageResource(R.drawable.ic_repeat_one_white_24dp);
                repeatButton.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN);
                break;
        }
    }

    public void show() {
        playPauseFab.animate()
                .scaleX(1f)
                .scaleY(1f)
                .rotation(360f)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    public void hide() {
        if (playPauseFab != null) {
            playPauseFab.setScaleX(0f);
            playPauseFab.setScaleY(0f);
            playPauseFab.setRotation(0f);
        }
    }

    private void setUpProgressSlider() {
        int color = MaterialValueHelper.getPrimaryTextColor(getContext(), false);
        progressSlider.getThumb().mutate().setColorFilter(color, PorterDuff.Mode.SRC_IN);

        progressSlider.setOnSeekBarChangeListener(new SimpleOnSeekbarChangeListener() {
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                progressSliderTracking = true;
                progressViewUpdateHelper.stop();
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    updateProgressText(progress, MusicPlayerRemote.getSongDurationMillis());
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                progressSliderTracking = false;
                MusicPlayerRemote.seekTo(progress);
                onUpdateProgressViews(progress, MusicPlayerRemote.getSongDurationMillis());
                progressViewUpdateHelper.start();
            }
        });
    }

    @Override
    public void onUpdateProgressViews(int progress, int total) {
        int safeTotal = Math.max(total, 0);
        int safeProgress = Math.max(progress, 0);
        progressSlider.setMax(safeTotal);
        progressSlider.setSecondaryProgress(Math.max(MusicPlayerRemote.getBufferedPositionMillis(), 0));
        if (progressSliderTracking) {
            updateProgressText(progressSlider.getProgress(), safeTotal);
            return;
        }
        progressSlider.setProgress(safeProgress);
        updateProgressText(safeProgress, safeTotal);
    }

    private void updateProgressText(int progress, int total) {
        int safeTotal = Math.max(total, 0);
        int safeProgress = Math.max(progress, 0);
        songTotalTime.setText(MusicUtil.getReadableDurationString(safeTotal));
        songCurrentProgress.setText(MusicUtil.getReadableDurationString(safeProgress));
    }
}
