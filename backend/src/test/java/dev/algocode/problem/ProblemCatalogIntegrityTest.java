package dev.algocode.problem;

import static org.assertj.core.api.Assertions.assertThat;

import dev.algocode.execution.ExecutionService;
import dev.algocode.execution.ExecutionTestSupport;
import dev.algocode.execution.RunStatus;
import dev.algocode.execution.SolveReport;
import dev.algocode.execution.TestRun;
import dev.algocode.judge.OutputComparator;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Every bundled reference solution must pass every bundled test within its time limit. */
class ProblemCatalogIntegrityTest {

    private static final ProblemCatalog CATALOG = new ProblemCatalog(JsonMapper.builder().build());

    static Stream<ProblemDefinition> problems() {
        return CATALOG.load().stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("problems")
    void referenceSolutionPassesAllTests(ProblemDefinition problem) {
        ExecutionService execution = ExecutionTestSupport.executionService();
        List<JsonNode> args = problem.tests().stream().map(ProblemDefinition.Test::args).toList();
        SolveReport report = execution.solve(problem.referenceCode(), "Solution", problem.meta().method(), args,
                problem.meta().timeLimitMs(), true);

        assertThat(report.status()).as(report.message()).isEqualTo(RunStatus.COMPLETED);
        assertThat(report.tests()).hasSameSizeAs(problem.tests());
        for (int i = 0; i < problem.tests().size(); i++) {
            TestRun run = report.tests().get(i);
            assertThat(run.error()).as("test %d", i).isNull();
            assertThat(OutputComparator.matches(problem.meta().compare(), problem.tests().get(i).expected(),
                    run.output())).as("test %d of %s", i, problem.slug()).isTrue();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("problems")
    void metadataIsComplete(ProblemDefinition problem) {
        assertThat(problem.meta().starterCode()).contains("class Solution").contains(problem.meta().method() + "(");
        assertThat(problem.description()).isNotBlank();
        assertThat(problem.meta().hints()).isNotEmpty();
        assertThat(problem.tests()).anyMatch(ProblemDefinition.Test::sample);
    }
}
