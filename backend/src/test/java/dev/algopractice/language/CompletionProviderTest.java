package dev.algopractice.language;

import static dev.algopractice.language.LanguageTestSupport.COMPLETION;
import static dev.algopractice.language.LanguageTestSupport.caret;
import static org.assertj.core.api.Assertions.assertThat;

import dev.algopractice.language.LanguageModels.CompletionItem;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompletionProviderTest {

    private List<CompletionItem> complete(String withBar) {
        var c = caret(withBar);
        return COMPLETION.complete(c.source(), c.line(), c.column(), "Solution").items();
    }

    private static List<String> labels(List<CompletionItem> items) {
        return items.stream().map(CompletionItem::label).toList();
    }

    @Test
    void membersOfAGenericReceiverWithSubstitutedTypes() {
        List<CompletionItem> items = complete("""
                import java.util.*;
                class Solution {
                    int f() {
                        Map<String, List<Integer>> groups = new HashMap<>();
                        groups.|
                        return 0;
                    }
                }
                """);
        CompletionItem put = items.stream().filter(i -> i.label().equals("put")).findFirst().orElseThrow();
        assertThat(put.kind()).isEqualTo("Method");
        assertThat(put.signature()).isEqualTo("(String k, List<Integer> v)");
        assertThat(put.type()).isEqualTo("List<Integer>");
        assertThat(put.insertText()).isEqualTo("put(${1:k}, ${2:v})$0");
        assertThat(labels(items)).contains("computeIfAbsent", "getOrDefault", "entrySet", "hashCode")
                .doesNotContain("of", "copyOf"); // static factories are not offered on instances
    }

    @Test
    void membersFilteredByTypedPrefix() {
        List<CompletionItem> items = complete("""
                class Solution {
                    void f(String s) {
                        s.toC|
                    }
                }
                """);
        assertThat(labels(items)).contains("toCharArray").doesNotContain("length", "substring");
    }

    @Test
    void arrayLengthAndStaticMembers() {
        assertThat(labels(complete("class Solution { int f(int[] nums) { return nums.| } }")))
                .contains("length", "clone");
        List<String> math = labels(complete("class Solution { int f(int a) { return Math.m| } }"));
        assertThat(math).contains("max", "min").doesNotContain("abs", "getClass");
    }

    @Test
    void localsParametersAndFieldsInScope() {
        List<CompletionItem> items = complete("""
                class Solution {
                    private int counter;
                    int f(int target) {
                        int total = 0;
                        for (int i = 0; i < 3; i++) {
                            |
                        }
                        return total;
                    }
                }
                """);
        assertThat(labels(items)).contains("total", "target", "i", "counter", "f", "return", "for");
        CompletionItem total = items.stream().filter(i -> i.label().equals("total")).findFirst().orElseThrow();
        assertThat(total.kind()).isEqualTo("Variable");
        assertThat(total.type()).isEqualTo("int");
        assertThat(total.sortText()).startsWith("0");
    }

    @Test
    void jdkTypesComeWithAnAutoImport() {
        List<CompletionItem> items = complete("""
                class Solution {
                    void f() {
                        HashM|
                    }
                }
                """);
        CompletionItem hashMap = items.stream().filter(i -> i.label().equals("HashMap")).findFirst().orElseThrow();
        assertThat(hashMap.type()).isEqualTo("java.util");
        assertThat(hashMap.additionalTextEdits()).singleElement().satisfies(edit -> {
            assertThat(edit.text()).isEqualTo("import java.util.HashMap;\n\n");
            assertThat(edit.range().startLine()).isEqualTo(1);
        });
    }

    @Test
    void alreadyImportedTypesNeedNoEdit() {
        List<CompletionItem> items = complete("""
                import java.util.*;

                class Solution {
                    void f() {
                        ArrayDe|
                    }
                }
                """);
        CompletionItem deque = items.stream().filter(i -> i.label().equals("ArrayDeque")).findFirst().orElseThrow();
        assertThat(deque.additionalTextEdits()).isEmpty();
    }

    @Test
    void importGoesAfterExistingImports() {
        var edit = CompletionProvider.importEdit("import java.util.List;\n\nclass A {}\n", "java.util.Map");
        assertThat(edit.range().startLine()).isEqualTo(2);
        assertThat(edit.text()).isEqualTo("import java.util.Map;\n");
    }

    @Test
    void camelHumpMatching() {
        assertThat(CompletionProvider.fuzzyMatch("gOD", "getOrDefault")).isTrue();
        assertThat(CompletionProvider.fuzzyMatch("hm", "HashMap")).isTrue();
        assertThat(CompletionProvider.fuzzyMatch("xm", "HashMap")).isFalse();
    }

    @Test
    void nothingInsideStringsOrComments() {
        assertThat(complete("class Solution { String s = \"abc.|\"; }")).isEmpty();
        assertThat(complete("class Solution { // note: x.|\n}")).isEmpty();
    }
}
