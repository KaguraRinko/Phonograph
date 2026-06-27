package com.kabouzeid.gramophone.lyrics;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kabouzeid.gramophone.model.Song;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class LyricsRepository {
    private final List<LyricsProvider> providers;

    public LyricsRepository(@NonNull List<LyricsProvider> providers) {
        this.providers = new ArrayList<>(providers);
    }

    @NonNull
    public static LyricsRepository empty() {
        return new LyricsRepository(Collections.emptyList());
    }

    @Nullable
    public String getLyrics(@NonNull Context context, @NonNull Song song,
                            boolean allowOnline) {
        String localLyrics = LocalLyricsLoader.load(song);
        if (hasText(localLyrics)) {
            return localLyrics;
        }

        String cachedLyrics = LyricsCache.read(context, song);
        if (hasText(cachedLyrics)) {
            return cachedLyrics;
        }

        if (!allowOnline) {
            return null;
        }

        LyricsSearchResult bestResult = findBestResult(LyricsSearchQuery.fromSong(song));
        if (bestResult != null && bestResult.score >= LyricsMatcher.AUTO_MATCH_THRESHOLD) {
            try {
                LyricsCache.write(context, song, bestResult);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return bestResult.lyrics;
        }
        return null;
    }

    @NonNull
    public List<LyricsSearchResult> search(@NonNull LyricsSearchQuery query) {
        List<LyricsSearchResult> results = new ArrayList<>();
        for (LyricsProvider provider : providers) {
            try {
                for (LyricsSearchResult result : provider.search(query)) {
                    if (result.hasUsableLyrics()) {
                        results.add(result);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Collections.sort(results, RESULT_COMPARATOR);
        return results;
    }

    @Nullable
    public LyricsSearchResult findBestResult(@NonNull LyricsSearchQuery query) {
        List<LyricsSearchResult> results = search(query);
        return results.isEmpty() ? null : results.get(0);
    }

    private static boolean hasText(@Nullable String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static final Comparator<LyricsSearchResult> RESULT_COMPARATOR = (left, right) -> {
        int score = Integer.compare(right.score, left.score);
        if (score != 0) {
            return score;
        }
        if (left.synchronizedLyrics != right.synchronizedLyrics) {
            return left.synchronizedLyrics ? -1 : 1;
        }
        return left.provider.compareToIgnoreCase(right.provider);
    };
}
