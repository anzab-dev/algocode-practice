package dev.algocode.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class ExecutionServiceTest {

    private final ExecutionService execution = ExecutionTestSupport.executionService();
    private final JsonMapper json = JsonMapper.builder().build();

    private List<JsonNode> args(String... lists) {
        return java.util.Arrays.stream(lists).map(json::readTree).toList();
    }

    @Test
    void runsEachTestAndReportsOutputTimeAndMemory() {
        String code = """
                import java.util.*;

                class Solution {
                    public int[] twoSum(int[] nums, int target) {
                        System.out.println("looking for " + target);
                        Map<Integer, Integer> seen = new HashMap<>();
                        for (int i = 0; i < nums.length; i++) {
                            if (seen.containsKey(target - nums[i])) return new int[] {seen.get(target - nums[i]), i};
                            seen.put(nums[i], i);
                        }
                        return null;
                    }
                }
                """;
        SolveReport report = execution.solve(code, "Solution", "twoSum",
                args("[[2,7,11,15],9]", "[[3,2,4],6]"), 2000, true);

        assertThat(report.status()).isEqualTo(RunStatus.COMPLETED);
        assertThat(report.tests()).hasSize(2);
        assertThat(report.tests().get(0).output()).isEqualTo(json.readTree("[0,1]"));
        assertThat(report.tests().get(1).output()).isEqualTo(json.readTree("[1,2]"));
        assertThat(report.tests().get(0).stdout()).isEqualTo("looking for 9\n");
        assertThat(report.totalTimeNanos()).isPositive();
        assertThat(report.allocatedBytes()).isPositive();
    }

    @Test
    void reportsCompileErrorsWithPositions() {
        SolveReport report = execution.solve("class Solution { int f() { return \"x\"; } }", "Solution", "f",
                args("[]"), 2000, true);
        assertThat(report.status()).isEqualTo(RunStatus.COMPILE_ERROR);
        assertThat(report.diagnostics()).anySatisfy(d -> {
            assertThat(d.severity()).isEqualTo(SourceDiagnostic.Severity.ERROR);
            assertThat(d.startLine()).isEqualTo(1);
            assertThat(d.startColumn()).isEqualTo(35);
        });
    }

    @Test
    void capturesExceptionsWithUserFramesOnly() {
        String code = """
                class Solution {
                    public int f(int[] a) {
                        return a[5];
                    }
                }
                """;
        SolveReport report = execution.solve(code, "Solution", "f", args("[[1]]", "[[1,2,3,4,5,6]]"), 2000, true);
        assertThat(report.status()).isEqualTo(RunStatus.COMPLETED);
        assertThat(report.tests()).hasSize(1); // stopped at the first error
        TestRun.RuntimeFailure error = report.tests().getFirst().error();
        assertThat(error.type()).isEqualTo("java.lang.ArrayIndexOutOfBoundsException");
        assertThat(error.trace()).contains("at Solution.f(Solution.java:3)").doesNotContain("algocode.harness");
    }

    @Test
    void stopsInfiniteLoopsAtTheTimeLimit() {
        String code = "class Solution { public int f() { while (true) { } } }";
        SolveReport report = execution.solve(code, "Solution", "f", args("[]"), 500, true);
        assertThat(report.status()).isEqualTo(RunStatus.TIME_LIMIT_EXCEEDED);
    }

    @Test
    void reportsMemoryLimit() {
        String code = """
                import java.util.*;
                class Solution {
                    public int f() {
                        List<long[]> hog = new ArrayList<>();
                        while (true) hog.add(new long[1_000_000]);
                    }
                }
                """;
        SolveReport report = execution.solve(code, "Solution", "f", args("[]"), 5000, true);
        assertThat(report.status()).isEqualTo(RunStatus.MEMORY_LIMIT_EXCEEDED);
    }

    @Test
    void inPlaceMethodsReportTheirFirstArgument() {
        String code = "class Solution { public void f(int[] a) { a[0] = 42; } }";
        SolveReport report = execution.solve(code, "Solution", "f", args("[[1,2]]"), 2000, true);
        assertThat(report.tests().getFirst().output()).isEqualTo(json.readTree("[42,2]"));
    }

    @Test
    void reportsMissingMethod() {
        SolveReport report = execution.solve("class Solution { }", "Solution", "f", args("[]"), 2000, true);
        assertThat(report.status()).isEqualTo(RunStatus.SETUP_ERROR);
        assertThat(report.message()).contains("'f'");
    }

    @Test
    void scratchpadReadsStdinAndCapturesOutput() {
        String code = """
                import java.util.Scanner;
                public class Main {
                    public static void main(String[] args) {
                        Scanner in = new Scanner(System.in);
                        int a = in.nextInt(), b = in.nextInt();
                        System.out.println(a + b);
                        System.err.println("done");
                    }
                }
                """;
        ScratchReport report = execution.scratch(code, "2 40");
        assertThat(report.status()).isEqualTo(RunStatus.COMPLETED);
        assertThat(report.stdout()).isEqualTo("42\n");
        assertThat(report.stderr()).isEqualTo("done\n");
        assertThat(report.mainClass()).isEqualTo("Main");
        assertThat(report.timeMs()).isPositive();
    }

    @Test
    void scratchpadSurvivesSystemExit() {
        String code = """
                class Tool {
                    public static void main(String[] args) {
                        System.out.println("bye");
                        System.exit(3);
                    }
                }
                """;
        ScratchReport report = execution.scratch(code, "");
        assertThat(report.status()).isEqualTo(RunStatus.EXITED);
        assertThat(report.stdout()).isEqualTo("bye\n");
    }

    @Test
    void scratchpadWithoutMainExplainsWhatIsMissing() {
        ScratchReport report = execution.scratch("class Idea { int x; }", "");
        assertThat(report.status()).isEqualTo(RunStatus.SETUP_ERROR);
        assertThat(report.message()).contains("main");
    }
}
