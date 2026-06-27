package com.kabouzeid.gramophone.lyrics;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kabouzeid.gramophone.lyrics.providers.KugouLyricsProvider;
import com.kabouzeid.gramophone.lyrics.providers.LrclibLyricsProvider;
import com.kabouzeid.gramophone.lyrics.providers.NeteaseLyricsProvider;
import com.kabouzeid.gramophone.lyrics.providers.QqMusicLyricsProvider;
import com.kabouzeid.gramophone.model.Song;
import com.kabouzeid.gramophone.util.PreferenceUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LyricsRepository {
    private final List<LyricsProvider> providers;
    private final Map<String, Integer> providerPriority;

    public LyricsRepository(@NonNull List<LyricsProvider> providers) {
        this.providers = new ArrayList<>(providers);
        providerPriority = new HashMap<>();
        for (int i = 0; i < this.providers.size(); i++) {
            providerPriority.put(this.providers.get(i).getName(), i);
        }
    }

    @NonNull
    public static LyricsRepository empty() {
        return new LyricsRepository(Collections.emptyList());
    }

    @NonNull
    public static LyricsRepository createDefault() {
        List<LyricsProvider> providers = new ArrayList<>();
        providers.add(new LrclibLyricsProvider());
        providers.add(new NeteaseLyricsProvider());
        providers.add(new KugouLyricsProvider());
        providers.add(new QqMusicLyricsProvider());
        return new LyricsRepository(providers);
    }

    @NonNull
    public static LyricsRepository createConfigured(@NonNull Context context) {
        return new LyricsRepository(LyricsProviderRegistry.createEnabledProviders(
                PreferenceUtil.getInstance(context).getLyricsProviderInfos()));
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

        LyricsSearchResult bestResult = findBestAutoMatch(LyricsSearchQuery.fromSong(song));
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
                results.addAll(search(provider, query));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Collections.sort(results, this::compareResults);
        return results;
    }

    @Nullable
    public LyricsSearchResult findBestResult(@NonNull LyricsSearchQuery query) {
        List<LyricsSearchResult> results = search(query);
        return results.isEmpty() ? null : results.get(0);
    }

    @Nullable
    public LyricsSearchResult findBestAutoMatch(@NonNull LyricsSearchQuery query) {
        for (LyricsProvider provider : providers) {
            try {
                List<LyricsSearchResult> results = search(provider, query);
                Collections.sort(results, this::compareResults);
                if (!results.isEmpty() && results.get(0).score >= LyricsMatcher.AUTO_MATCH_THRESHOLD) {
                    return results.get(0);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @NonNull
    private List<LyricsSearchResult> search(@NonNull LyricsProvider provider,
                                            @NonNull LyricsSearchQuery query) throws IOException {
        List<LyricsSearchResult> results = new ArrayList<>();
        for (LyricsSearchResult result : provider.search(query)) {
            if (result.hasUsableLyrics()) {
                results.add(result);
            }
        }
        return results;
    }

    private static boolean hasText(@Nullable String value) {
        return value != null && !value.trim().isEmpty();
    }

    private int compareResults(@NonNull LyricsSearchResult left, @NonNull LyricsSearchResult right) {
        int score = Integer.compare(right.score, left.score);
        if (score != 0) {
            return score;
        }
        if (left.synchronizedLyrics != right.synchronizedLyrics) {
            return left.synchronizedLyrics ? -1 : 1;
        }
        int priority = Integer.compare(priority(left.provider), priority(right.provider));
        if (priority != 0) {
            return priority;
        }
        return left.provider.compareToIgnoreCase(right.provider);
    }

    private int priority(@NonNull String providerName) {
        Integer priority = providerPriority.get(providerName);
        return priority == null ? Integer.MAX_VALUE : priority;
    }
}
