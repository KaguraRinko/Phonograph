package com.kabouzeid.gramophone.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.kabouzeid.gramophone.R;
import com.kabouzeid.gramophone.model.Song;
import com.kabouzeid.gramophone.subsonic.SubsonicLibraryStore;
import com.kabouzeid.gramophone.subsonic.SubsonicServer;
import com.kabouzeid.gramophone.subsonic.SubsonicUri;
import com.kabouzeid.gramophone.util.MusicUtil;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * @author Karim Abou Zeid (kabouzeid), Aidan Follestad (afollestad)
 */
public class SongDetailDialog extends DialogFragment {

    public static final String TAG = SongDetailDialog.class.getSimpleName();

    @NonNull
    public static SongDetailDialog create(Song song) {
        SongDetailDialog dialog = new SongDetailDialog();
        Bundle args = new Bundle();
        args.putParcelable("song", song);
        dialog.setArguments(args);
        return dialog;
    }

    private static Spanned makeTextWithTitle(@NonNull Context context, int titleResId, String text) {
        return Html.fromHtml("<b>" + context.getResources().getString(titleResId) + ": " + "</b>" + text);
    }

    private static String getFileSizeString(long sizeInBytes) {
        if (sizeInBytes <= 0) {
            return "-";
        }
        double size = sizeInBytes;
        String[] units = new String[]{"B", "KB", "MB", "GB"};
        int unit = 0;
        while (size >= 1024 && unit < units.length - 1) {
            size /= 1024;
            unit++;
        }
        if (unit == 0) {
            return (long) size + " " + units[unit];
        }
        return String.format(Locale.US, "%.1f %s", size, units[unit]);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity context = getActivity();
        final Song song = getArguments().getParcelable("song");

        MaterialDialog dialog = new MaterialDialog.Builder(context)
                .customView(R.layout.dialog_file_details, true)
                .title(context.getResources().getString(R.string.label_details))
                .positiveText(android.R.string.ok)
                .build();

        View dialogView = dialog.getCustomView();
        final TextView fileName = dialogView.findViewById(R.id.file_name);
        final TextView filePath = dialogView.findViewById(R.id.file_path);
        final TextView fileSize = dialogView.findViewById(R.id.file_size);
        final TextView fileFormat = dialogView.findViewById(R.id.file_format);
        final TextView trackLength = dialogView.findViewById(R.id.track_length);
        final TextView bitRate = dialogView.findViewById(R.id.bitrate);
        final TextView samplingRate = dialogView.findViewById(R.id.sampling_rate);

        fileName.setText(makeTextWithTitle(context, R.string.label_file_name, "-"));
        filePath.setText(makeTextWithTitle(context, R.string.label_file_path, "-"));
        fileSize.setText(makeTextWithTitle(context, R.string.label_file_size, "-"));
        fileFormat.setText(makeTextWithTitle(context, R.string.label_file_format, "-"));
        trackLength.setText(makeTextWithTitle(context, R.string.label_track_length, "-"));
        bitRate.setText(makeTextWithTitle(context, R.string.label_bit_rate, "-"));
        samplingRate.setText(makeTextWithTitle(context, R.string.label_sampling_rate, "-"));

        if (song != null) {
            if (SubsonicUri.isSubsonicUri(song.data)) {
                bindSubsonicDetails(context, song, fileName, filePath, fileSize, fileFormat,
                        trackLength, bitRate, samplingRate);
                return dialog;
            }

            final File songFile = new File(song.data);
            if (songFile.exists()) {
                fileName.setText(makeTextWithTitle(context, R.string.label_file_name, songFile.getName()));
                filePath.setText(makeTextWithTitle(context, R.string.label_file_path, songFile.getAbsolutePath()));
                fileSize.setText(makeTextWithTitle(context, R.string.label_file_size, getFileSizeString(songFile.length())));
                try {
                    AudioFile audioFile = AudioFileIO.read(songFile);
                    AudioHeader audioHeader = audioFile.getAudioHeader();

                    fileFormat.setText(makeTextWithTitle(context, R.string.label_file_format, audioHeader.getFormat()));
                    trackLength.setText(makeTextWithTitle(context, R.string.label_track_length, MusicUtil.getReadableDurationString(audioHeader.getTrackLength() * 1000)));
                    bitRate.setText(makeTextWithTitle(context, R.string.label_bit_rate, audioHeader.getBitRate() + " kb/s"));
                    samplingRate.setText(makeTextWithTitle(context, R.string.label_sampling_rate, audioHeader.getSampleRate() + " Hz"));
                } catch (@NonNull CannotReadException | IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException e) {
                    Log.e(TAG, "error while reading the song file", e);
                    // fallback
                    trackLength.setText(makeTextWithTitle(context, R.string.label_track_length, MusicUtil.getReadableDurationString(song.duration)));
                }
            } else {
                // fallback
                fileName.setText(makeTextWithTitle(context, R.string.label_file_name, song.title));
                trackLength.setText(makeTextWithTitle(context, R.string.label_track_length, MusicUtil.getReadableDurationString(song.duration)));
            }
        }

        return dialog;
    }

    private void bindSubsonicDetails(@NonNull Context context, @NonNull Song song,
                                     @NonNull TextView fileName, @NonNull TextView filePath,
                                     @NonNull TextView fileSize, @NonNull TextView fileFormat,
                                     @NonNull TextView trackLength, @NonNull TextView bitRate,
                                     @NonNull TextView samplingRate) {
        long serverId = SubsonicUri.getServerId(song.data);
        SubsonicLibraryStore.CachedSongDetails details = serverId == SubsonicServer.NO_ID
                ? null
                : SubsonicLibraryStore.getInstance(context).getSongDetails(serverId, song.id);

        String path = details == null ? null : details.path;
        String name = details == null ? "" : getFileName(path, details.title, details.suffix);
        fileName.setText(makeTextWithTitle(context, R.string.label_file_name,
                fallback(name, details == null ? song.title : details.title)));
        filePath.setText(makeTextWithTitle(context, R.string.label_file_path, fallback(path, "-")));
        fileSize.setText(makeTextWithTitle(context, R.string.label_file_size,
                details == null ? "-" : getFileSizeString(details.fileSize)));
        fileFormat.setText(makeTextWithTitle(context, R.string.label_file_format,
                details == null ? "-" : getFormat(details)));
        trackLength.setText(makeTextWithTitle(context, R.string.label_track_length,
                MusicUtil.getReadableDurationString(song.duration)));
        bitRate.setText(makeTextWithTitle(context, R.string.label_bit_rate,
                details != null && details.bitRate > 0 ? details.bitRate + " kb/s" : "-"));
        samplingRate.setText(makeTextWithTitle(context, R.string.label_sampling_rate,
                details != null && details.samplingRate > 0 ? details.samplingRate + " Hz" : "-"));
    }

    @NonNull
    private String getFormat(@NonNull SubsonicLibraryStore.CachedSongDetails details) {
        String suffix = normalize(details.suffix);
        String contentType = normalize(details.contentType);
        String path = normalize(details.path);
        if (suffix.isEmpty()) {
            suffix = extension(path);
        }
        if (contentType.contains("alac") || "alac".equals(suffix)) {
            return "ALAC";
        } else if (contentType.contains("flac") || "flac".equals(suffix)) {
            return "FLAC";
        } else if (contentType.contains("mpeg") || "mp3".equals(suffix)) {
            return "MP3";
        } else if (contentType.contains("aac") || "aac".equals(suffix)) {
            return "AAC";
        } else if (contentType.contains("opus") || "opus".equals(suffix)) {
            return "OPUS";
        } else if (contentType.contains("ogg") || "ogg".equals(suffix)) {
            return "OGG";
        } else if (contentType.contains("wav") || "wav".equals(suffix)) {
            return "WAV";
        } else if (contentType.contains("mp4") || "m4a".equals(suffix)) {
            return "M4A";
        } else if (!suffix.isEmpty()) {
            return suffix.toUpperCase(Locale.US);
        } else if (!contentType.isEmpty()) {
            return contentType;
        }
        return "-";
    }

    @NonNull
    private String getFileName(String path, @NonNull String title, String suffix) {
        if (path == null || path.trim().isEmpty()) {
            String normalizedSuffix = normalize(suffix);
            return normalizedSuffix.isEmpty() ? title : title + "." + normalizedSuffix;
        }
        int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return slash >= 0 && slash < path.length() - 1 ? path.substring(slash + 1) : path;
    }

    @NonNull
    private String extension(@NonNull String path) {
        int dot = path.lastIndexOf('.');
        return dot >= 0 && dot < path.length() - 1 ? path.substring(dot + 1) : "";
    }

    @NonNull
    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.US);
    }

    @NonNull
    private String fallback(String primary, @NonNull String fallback) {
        return primary == null || primary.trim().isEmpty() ? fallback : primary;
    }
}
