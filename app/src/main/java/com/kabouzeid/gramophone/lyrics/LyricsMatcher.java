package com.kabouzeid.gramophone.lyrics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.Normalizer;
import java.util.Locale;

public final class LyricsMatcher {
    public static final int AUTO_MATCH_THRESHOLD = 72;

    private LyricsMatcher() {
    }

    public static int score(@NonNull LyricsSearchQuery query, @Nullable String title,
                            @Nullable String artist, @Nullable String album,
                            long durationMillis) {
        int score = 0;
        score += textScore(query.title, title) * 45 / 100;
        score += textScore(query.artist, artist) * 35 / 100;
        score += textScore(query.album, album) * 10 / 100;
        score += durationScore(query.durationMillis, durationMillis) * 10 / 100;
        return Math.max(0, Math.min(100, score));
    }

    private static int textScore(@Nullable String expected, @Nullable String actual) {
        String left = normalize(expected);
        String right = normalize(actual);
        if (left.isEmpty() || right.isEmpty()) {
            return 0;
        }
        if (left.equals(right)) {
            return 100;
        }
        if (left.contains(right) || right.contains(left)) {
            return 82;
        }

        String[] leftWords = left.split(" ");
        int matched = 0;
        for (String word : leftWords) {
            if (!word.isEmpty() && right.contains(word)) {
                matched++;
            }
        }
        return leftWords.length == 0 ? 0 : Math.min(70, (matched * 70) / leftWords.length);
    }

    private static int durationScore(long expectedMillis, long actualMillis) {
        if (expectedMillis <= 0 || actualMillis <= 0) {
            return 0;
        }
        long diff = Math.abs(expectedMillis - actualMillis);
        if (diff <= 5000) {
            return 100;
        }
        if (diff <= 15000) {
            return 70;
        }
        if (diff <= 30000) {
            return 35;
        }
        return 0;
    }

    @NonNull
    public static String normalize(@Nullable String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\([^)]*\\)", " ")
                .replaceAll("\\[[^]]*\\]", " ")
                .replaceAll("[`'\"._,，。:：;；!！?？/\\\\|《》<>]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (normalized.startsWith("the ")) {
            normalized = normalized.substring(4);
        }
        return normalized;
    }
}
