package com.kabouzeid.gramophone.model.lyrics;

import android.util.SparseArray;

public abstract class AbsSynchronizedLyrics extends Lyrics {
    private static final int TIME_OFFSET_MS = 500; // time adjustment to display line before it actually starts

    protected final SparseArray<String> lines = new SparseArray<>();
    protected int offset = 0;

    public String getLine(int time) {
        parse(false);
        if (lines.size() == 0) {
            return "";
        }

        time += offset + AbsSynchronizedLyrics.TIME_OFFSET_MS;

        int lastLineTime = lines.keyAt(0);

        for (int i = 0; i < lines.size(); i++) {
            int lineTime = lines.keyAt(i);

            if (time >= lineTime) {
                lastLineTime = lineTime;
            } else {
                break;
            }
        }

        return lines.get(lastLineTime);
    }

    public int getNextLineStartTime(int time) {
        parse(false);
        if (lines.size() == 0) {
            return -1;
        }

        int adjustedTime = time + offset + AbsSynchronizedLyrics.TIME_OFFSET_MS;
        for (int i = 0; i < lines.size(); i++) {
            int lineTime = lines.keyAt(i);
            if (lineTime > adjustedTime) {
                return Math.max(0, lineTime - offset - AbsSynchronizedLyrics.TIME_OFFSET_MS);
            }
        }
        return -1;
    }

    public boolean isSynchronized() {
        return true;
    }

    public boolean isValid() {
        parse(true);
        return valid;
    }

    @Override
    public String getText() {
        parse(false);

        if (valid) {
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.valueAt(i);
                sb.append(line).append("\r\n");
            }

            return sb.toString().trim().replaceAll("(\r?\n){3,}", "\r\n\r\n");
        }

        return super.getText();
    }
}
