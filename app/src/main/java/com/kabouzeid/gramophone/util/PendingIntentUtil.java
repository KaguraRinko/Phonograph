package com.kabouzeid.gramophone.util;

import android.app.PendingIntent;
import android.os.Build;

public final class PendingIntentUtil {

    private PendingIntentUtil() {
    }

    public static int immutableFlag(int flags) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return flags | PendingIntent.FLAG_IMMUTABLE;
        }
        return flags;
    }
}
