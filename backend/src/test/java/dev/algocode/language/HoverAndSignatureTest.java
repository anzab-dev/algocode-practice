package dev.algocode.language;

import static dev.algocode.language.LanguageTestSupport.ANALYZER;
import static dev.algocode.language.LanguageTestSupport.COMPLETION;
import static dev.algocode.language.LanguageTestSupport.caret;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HoverAndSignatureTest {

    private final HoverProvider hover = new HoverProvider(ANALYZER);
    private final SignatureHelpProvider signatures = new SignatureHelpProvider(COMPLETION);

    @Test
    void hoverShowsTheDeclarationOfALocal() {
        var c = caret("""
                class Solution {
                    int f() {
                        long total = 0;
                        return (int) to|tal;
                    }
                }
                """);
        var result = hover.hover(c.source(), c.line(), c.column(), "Solution");
        assertThat(result.markdown()).contains("long total").contains("Local variable");
        assertThat(result.range().startLine()).isEqualTo(4);
        assertThat(result.range().startColumn()).isEqualTo(22);
    }

    @Test
    void hoverOnAJdkMethod() {
        var c = caret("class Solution { int f(String s) { return s.ind|exOf('a'); } }");
        var result = hover.hover(c.source(), c.line(), c.column(), "Solution");
        assertThat(result.markdown()).contains("int indexOf(").contains("java.lang.String");
    }

    @Test
    void signatureHelpTracksTheActiveArgument() {
        var c = caret("""
                import java.util.*;
                class Solution {
                    void f(Map<String, Integer> m) {
                        m.getOrDefault("a", |);
                    }
                }
                """);
        var result = signatures.signatureHelp(c.source(), c.line(), c.column(), "Solution");
        assertThat(result.activeParameter()).isEqualTo(1);
        assertThat(result.signatures()).singleElement()
                .satisfies(s -> assertThat(s.label()).isEqualTo("getOrDefault(Object o, Integer v) : Integer"));
    }

    @Test
    void enclosingCallIgnoresNestedParentheses() {
        String s = "foo(a, bar(b, c), ";
        assertThat(SignatureHelpProvider.enclosingCall(s, s.length())).containsExactly(3, 2);
        assertThat(SignatureHelpProvider.enclosingCall("x = 1;", 6)).isNull();
    }
}
