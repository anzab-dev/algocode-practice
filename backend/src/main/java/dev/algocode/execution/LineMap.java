package dev.algocode.execution;

import java.util.Arrays;

/** Converts between character offsets and 1-based line/column positions of a source text. */
public final class LineMap {

    private final int[] lineStarts;
    private final int length;

    public LineMap(String text) {
        int[] starts = new int[16];
        int count = 1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                if (count == starts.length) {
                    starts = Arrays.copyOf(starts, count * 2);
                }
                starts[count++] = i + 1;
            }
        }
        this.lineStarts = Arrays.copyOf(starts, count);
        this.length = text.length();
    }

    public int line(long offset) {
        int idx = Arrays.binarySearch(lineStarts, (int) clamp(offset));
        return idx >= 0 ? idx + 1 : -idx - 1;
    }

    public int column(long offset) {
        long clamped = clamp(offset);
        return (int) (clamped - lineStarts[line(clamped) - 1]) + 1;
    }

    /** Offset of a 1-based line/column, clamped to the text. */
    public int offset(int line, int column) {
        int l = Math.max(1, Math.min(line, lineStarts.length));
        return (int) clamp((long) lineStarts[l - 1] + Math.max(0, column - 1));
    }

    private long clamp(long offset) {
        return Math.max(0, Math.min(offset, length));
    }
}
