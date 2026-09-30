package dev.algocode.language;

/** Lexical facts about a cursor position that do not need a parse tree. */
final class SourceContext {

    private SourceContext() {
    }

    /** True when {@code offset} is inside a comment, string, char or text-block literal. */
    static boolean inCommentOrLiteral(String s, int offset) {
        int i = 0;
        int end = Math.min(offset, s.length());
        while (i < end) {
            char c = s.charAt(i);
            char next = i + 1 < s.length() ? s.charAt(i + 1) : 0;
            if (c == '/' && next == '/') {
                int eol = s.indexOf('\n', i);
                if (eol < 0 || eol >= offset) {
                    return true;
                }
                i = eol + 1;
            } else if (c == '/' && next == '*') {
                int close = s.indexOf("*/", i + 2);
                if (close < 0 || close + 2 > offset) {
                    return true;
                }
                i = close + 2;
            } else if (c == '"' && s.startsWith("\"\"\"", i)) {
                int close = s.indexOf("\"\"\"", i + 3);
                if (close < 0 || close + 3 > offset) {
                    return true;
                }
                i = close + 3;
            } else if (c == '"' || c == '\'') {
                int j = i + 1;
                while (j < s.length() && s.charAt(j) != c && s.charAt(j) != '\n') {
                    j += s.charAt(j) == '\\' ? 2 : 1;
                }
                if (j >= offset) {
                    return true;
                }
                i = j + 1;
            } else {
                i++;
            }
        }
        return false;
    }

    static int identifierStart(String s, int offset) {
        int i = offset;
        while (i > 0 && Character.isJavaIdentifierPart(s.charAt(i - 1))) {
            i--;
        }
        return i;
    }

    /** Index of the '.' right before {@code identifierStart} (whitespace allowed), or -1. */
    static int dotBefore(String s, int identifierStart) {
        int i = identifierStart - 1;
        while (i >= 0 && Character.isWhitespace(s.charAt(i))) {
            i--;
        }
        return i >= 0 && s.charAt(i) == '.' ? i : -1;
    }
}
