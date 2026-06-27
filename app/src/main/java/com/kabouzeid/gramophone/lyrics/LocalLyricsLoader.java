package com.kabouzeid.gramophone.lyrics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kabouzeid.gramophone.model.Song;
import com.kabouzeid.gramophone.model.lyrics.AbsSynchronizedLyrics;
import com.kabouzeid.gramophone.subsonic.SubsonicUri;
import com.kabouzeid.gramophone.util.FileUtil;

import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class LocalLyricsLoader {
    private LocalLyricsLoader() {
    }

    @Nullable
    public static String load(@NonNull Song song) {
        if (SubsonicUri.isSubsonicUri(song.data)) {
            return null;
        }

        String lyrics = null;
        File file = new File(song.data);

        try {
            lyrics = AudioFileIO.read(file).getTagOrCreateDefault().getFirst(FieldKey.LYRICS);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (lyrics == null || lyrics.trim().isEmpty() || !AbsSynchronizedLyrics.isSynchronized(lyrics)) {
            File dir = file.getAbsoluteFile().getParentFile();

            if (dir != null && dir.exists() && dir.isDirectory()) {
                String format = ".*%s.*\\.(lrc|txt)";
                String filename = Pattern.quote(FileUtil.stripExtension(file.getName()));
                String songTitle = Pattern.quote(song.title);

                final List<Pattern> patterns = new ArrayList<>();
                patterns.add(Pattern.compile(String.format(format, filename), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
                patterns.add(Pattern.compile(String.format(format, songTitle), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));

                File[] files = dir.listFiles(f -> {
                    for (Pattern pattern : patterns) {
                        if (pattern.matcher(f.getName()).matches()) {
                            return true;
                        }
                    }
                    return false;
                });

                if (files != null && files.length > 0) {
                    for (File lyricsFile : files) {
                        try {
                            String newLyrics = FileUtil.read(lyricsFile);
                            if (newLyrics != null && !newLyrics.trim().isEmpty()) {
                                if (AbsSynchronizedLyrics.isSynchronized(newLyrics)) {
                                    return newLyrics;
                                }
                                lyrics = newLyrics;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        return lyrics;
    }
}
