package com.kabouzeid.gramophone.lyrics;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kabouzeid.gramophone.R;
import com.kabouzeid.gramophone.lyrics.providers.KugouLyricsProvider;
import com.kabouzeid.gramophone.lyrics.providers.LrclibLyricsProvider;
import com.kabouzeid.gramophone.lyrics.providers.NeteaseLyricsProvider;
import com.kabouzeid.gramophone.lyrics.providers.QqMusicLyricsProvider;
import com.kabouzeid.gramophone.model.LyricsProviderInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class LyricsProviderRegistry {
    public static final String ID_LRCLIB = "lrclib";
    public static final String ID_NETEASE = "netease";
    public static final String ID_KUGOU = "kugou";
    public static final String ID_QQ_MUSIC = "qq_music";

    private LyricsProviderRegistry() {
    }

    @NonNull
    public static List<LyricsProviderInfo> getDefaultProviderInfos() {
        List<LyricsProviderInfo> providers = new ArrayList<>();
        providers.add(new LyricsProviderInfo(ID_LRCLIB, true));
        providers.add(new LyricsProviderInfo(ID_NETEASE, true));
        providers.add(new LyricsProviderInfo(ID_KUGOU, true));
        providers.add(new LyricsProviderInfo(ID_QQ_MUSIC, true));
        return providers;
    }

    @NonNull
    public static List<LyricsProviderInfo> normalize(@Nullable List<LyricsProviderInfo> savedProviders) {
        List<LyricsProviderInfo> normalized = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        if (savedProviders != null) {
            for (LyricsProviderInfo provider : savedProviders) {
                if (provider == null || provider.id == null || !isKnownProvider(provider.id)
                        || seen.contains(provider.id)) {
                    continue;
                }
                normalized.add(new LyricsProviderInfo(provider.id, provider.enabled));
                seen.add(provider.id);
            }
        }
        for (LyricsProviderInfo provider : getDefaultProviderInfos()) {
            if (!seen.contains(provider.id)) {
                normalized.add(provider);
            }
        }
        return normalized;
    }

    @NonNull
    public static List<LyricsProvider> createEnabledProviders(@NonNull List<LyricsProviderInfo> infos) {
        List<LyricsProvider> providers = new ArrayList<>();
        for (LyricsProviderInfo info : normalize(infos)) {
            if (!info.enabled) {
                continue;
            }
            LyricsProvider provider = createProvider(info.id);
            if (provider != null) {
                providers.add(provider);
            }
        }
        return providers;
    }

    @Nullable
    private static LyricsProvider createProvider(@NonNull String id) {
        switch (id) {
            case ID_LRCLIB:
                return new LrclibLyricsProvider();
            case ID_NETEASE:
                return new NeteaseLyricsProvider();
            case ID_KUGOU:
                return new KugouLyricsProvider();
            case ID_QQ_MUSIC:
                return new QqMusicLyricsProvider();
            default:
                return null;
        }
    }

    public static boolean isKnownProvider(@NonNull String id) {
        return ID_LRCLIB.equals(id)
                || ID_NETEASE.equals(id)
                || ID_KUGOU.equals(id)
                || ID_QQ_MUSIC.equals(id);
    }

    public static int getTitleRes(@NonNull String id) {
        switch (id) {
            case ID_LRCLIB:
                return R.string.lyrics_provider_lrclib;
            case ID_NETEASE:
                return R.string.lyrics_provider_netease;
            case ID_KUGOU:
                return R.string.lyrics_provider_kugou;
            case ID_QQ_MUSIC:
                return R.string.lyrics_provider_qq_music;
            default:
                return 0;
        }
    }

    @NonNull
    public static String getDisplayName(@NonNull Context context, @NonNull String id) {
        int titleRes = getTitleRes(id);
        return titleRes == 0 ? id : context.getString(titleRes);
    }
}
