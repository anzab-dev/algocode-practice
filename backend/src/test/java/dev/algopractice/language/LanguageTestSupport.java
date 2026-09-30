package dev.algopractice.language;

import dev.algopractice.execution.JavaSourceCompiler;

final class LanguageTestSupport {

    static final JavaAnalyzer ANALYZER = new JavaAnalyzer(new JavaSourceCompiler());
    static final CompletionProvider COMPLETION = new CompletionProvider(ANALYZER, new JdkTypeIndex(ANALYZER));

    private LanguageTestSupport() {
    }

    /** Splits "text with | caret" into the source without the bar and the bar's 1-based line/column. */
    static Caret caret(String withBar) {
        int idx = withBar.indexOf('|');
        String source = withBar.substring(0, idx) + withBar.substring(idx + 1);
        String before = withBar.substring(0, idx);
        int line = (int) before.chars().filter(c -> c == '\n').count() + 1;
        int column = idx - before.lastIndexOf('\n');
        return new Caret(source, line, column);
    }

    record Caret(String source, int line, int column) {
    }
}
