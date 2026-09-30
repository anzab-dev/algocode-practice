package dev.algocode.language;

import static dev.algocode.language.LanguageTestSupport.ANALYZER;
import static org.assertj.core.api.Assertions.assertThat;

import dev.algocode.execution.SourceDiagnostic;
import dev.algocode.execution.SourceDiagnostic.Severity;
import java.util.List;
import org.junit.jupiter.api.Test;

class DiagnosticsProviderTest {

    private final DiagnosticsProvider provider = new DiagnosticsProvider();

    private List<SourceDiagnostic> diagnose(String source) {
        return provider.diagnostics(ANALYZER.analyze(source, "Solution"));
    }

    @Test
    void reportsTypeErrorsAtTheOffendingExpression() {
        List<SourceDiagnostic> result = diagnose("""
                class Solution {
                    int f() {
                        String s = 1;
                        return s.size();
                    }
                }
                """);
        assertThat(result).filteredOn(d -> d.severity() == Severity.ERROR).hasSize(2);
        assertThat(result).anySatisfy(d -> {
            assertThat(d.message()).contains("incompatible types");
            assertThat(d.startLine()).isEqualTo(3);
            assertThat(d.startColumn()).isEqualTo(20);
        });
        assertThat(result).anySatisfy(d -> {
            assertThat(d.message()).contains("cannot find symbol");
            assertThat(d.startLine()).isEqualTo(4);
        });
    }

    @Test
    void reportsFlowErrorsLikeMissingReturn() {
        assertThat(diagnose("class Solution { int f(int x) { if (x > 0) return 1; } }"))
                .anySatisfy(d -> assertThat(d.message()).contains("missing return statement"));
    }

    @Test
    void flagsUnusedLocalsAndImportsLikeIntellij() {
        List<SourceDiagnostic> result = diagnose("""
                import java.util.List;
                import java.util.Map;

                class Solution {
                    int f(List<Integer> xs) {
                        int unused = 3;
                        int used = xs.size();
                        return used;
                    }
                }
                """);
        assertThat(result).filteredOn(d -> DiagnosticsProvider.UNUSED.equals(d.code()))
                .extracting(SourceDiagnostic::message, SourceDiagnostic::startLine, SourceDiagnostic::startColumn)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("Variable 'unused' is never used", 6, 13),
                        org.assertj.core.groups.Tuple.tuple("Unused import statement", 2, 1));
    }

    @Test
    void cleanCodeHasNoFindings() {
        assertThat(diagnose("""
                import java.util.HashMap;

                class Solution {
                    public int[] twoSum(int[] nums, int target) {
                        var seen = new HashMap<Integer, Integer>();
                        for (int i = 0; i < nums.length; i++) {
                            Integer j = seen.get(target - nums[i]);
                            if (j != null) return new int[] {j, i};
                            seen.put(nums[i], i);
                        }
                        return new int[0];
                    }
                }
                """)).isEmpty();
    }
}
