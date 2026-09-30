package dev.algocode.judge;

import dev.algocode.execution.ExecutionService;
import dev.algocode.execution.RunStatus;
import dev.algocode.execution.SolveReport;
import dev.algocode.execution.TestRun;
import dev.algocode.problem.Problem;
import dev.algocode.problem.TestCase;
import dev.algocode.problem.TestCaseRepository;
import dev.algocode.progress.Achievement;
import dev.algocode.progress.Profile;
import dev.algocode.progress.ProgressService;
import dev.algocode.submission.Submission;
import dev.algocode.submission.SubmissionRepository;
import dev.algocode.submission.Verdict;
import dev.algocode.telemetry.AlgoTelemetry;
import dev.algocode.user.AppUser;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Runs solutions against a problem's tests and turns the results into verdicts. */
@Service
public class JudgeService {

    static final String SOLUTION_CLASS = "Solution";
    static final int PREVIEW_CHARS = 2000;

    private final ExecutionService execution;
    private final TestCaseRepository testCases;
    private final SubmissionRepository submissions;
    private final ProgressService progress;
    private final AlgoTelemetry telemetry;
    private final ObjectMapper json;

    public JudgeService(ExecutionService execution, TestCaseRepository testCases, SubmissionRepository submissions,
                        ProgressService progress, AlgoTelemetry telemetry, ObjectMapper json) {
        this.execution = execution;
        this.testCases = testCases;
        this.submissions = submissions;
        this.progress = progress;
        this.telemetry = telemetry;
        this.json = json;
    }

    /** Runs the sample tests plus any custom inputs. Nothing is recorded. */
    public RunResponse run(Problem problem, String code, List<JsonNode> customInputs) {
        List<JsonNode> customs = customInputs == null ? List.of() : customInputs;
        int arity = json.readTree(problem.getParamsJson()).size();
        for (JsonNode input : customs) {
            if (input == null || !input.isArray() || input.size() != arity) {
                throw new IllegalArgumentException("Each custom input must be a JSON array of " + arity + " argument(s)");
            }
        }
        List<TestCase> samples = testCases.findByProblemAndSampleTrueOrderByOrdinalAsc(problem);
        List<JsonNode> args = new ArrayList<>();
        List<JsonNode> expected = new ArrayList<>();
        for (TestCase t : samples) {
            args.add(json.readTree(t.getArgsJson()));
            expected.add(json.readTree(t.getExpectedJson()));
        }
        args.addAll(customs);

        SolveReport report = execution.solve(code, SOLUTION_CLASS, problem.getMethodName(), args,
                problem.getTimeLimitMs(), false);
        if (!customs.isEmpty() && report.status() == RunStatus.COMPLETED) {
            SolveReport reference = execution.solve(problem.getReferenceCode(), SOLUTION_CLASS,
                    problem.getMethodName(), customs, problem.getTimeLimitMs(), false);
            for (int i = 0; i < customs.size(); i++) {
                TestRun ref = i < reference.tests().size() ? reference.tests().get(i) : null;
                expected.add(ref == null || ref.error() != null ? null : ref.output());
            }
        }

        List<CaseResult> cases = new ArrayList<>();
        Verdict verdict = verdictFor(report.status());
        for (int i = 0; i < args.size(); i++) {
            boolean custom = i >= samples.size();
            TestRun run = i < report.tests().size() ? report.tests().get(i) : null;
            JsonNode want = i < expected.size() ? expected.get(i) : null;
            CaseResult result = caseResult(problem, i, custom, args.get(i), want, run);
            cases.add(result);
            if (verdict == Verdict.ACCEPTED && run != null && !result.passed()) {
                verdict = run.error() != null ? Verdict.RUNTIME_ERROR : Verdict.WRONG_ANSWER;
            }
        }
        return new RunResponse(verdict, report.message(), report.diagnostics(), cases,
                report.totalTimeNanos() / 1e6, report.peakHeapBytes());
    }

    /** Judges the code against every test, records the submission and reports progress changes. */
    public SubmitResponse submit(AppUser user, Problem problem, String code) {
        Profile before = progress.profile(user);
        List<TestCase> tests = testCases.findByProblemOrderByOrdinalAsc(problem);
        List<JsonNode> args = tests.stream().map(t -> json.readTree(t.getArgsJson())).toList();
        SolveReport report = execution.solve(code, SOLUTION_CLASS, problem.getMethodName(), args,
                problem.getTimeLimitMs(), true);

        Verdict verdict = verdictFor(report.status());
        int passed = 0;
        CaseResult failed = null;
        for (int i = 0; i < report.tests().size() && failed == null; i++) {
            TestCase test = tests.get(i);
            CaseResult result = caseResult(problem, i, false, args.get(i),
                    json.readTree(test.getExpectedJson()), report.tests().get(i));
            if (result.passed()) {
                passed++;
            } else {
                failed = result;
                if (verdict == Verdict.ACCEPTED) {
                    verdict = result.error() != null ? Verdict.RUNTIME_ERROR : Verdict.WRONG_ANSWER;
                }
            }
        }
        if (verdict == Verdict.ACCEPTED && passed < tests.size()) {
            verdict = Verdict.INTERNAL_ERROR; // the harness stopped early without saying why
        }

        Submission submission = new Submission(user, problem, code);
        submission.setVerdict(verdict);
        submission.setPassed(passed);
        submission.setTotal(tests.size());
        submission.setMessage(messageFor(verdict, report, failed));
        if (verdict == Verdict.ACCEPTED) {
            double runtimeMs = report.totalTimeNanos() / 1e6;
            submission.setRuntimeMs(runtimeMs);
            submission.setMemoryBytes(report.peakHeapBytes());
            submission.setAllocatedBytes(report.allocatedBytes());
            submission.setRuntimeBeats(beats(submissions.acceptedRuntimes(problem), runtimeMs));
            submission.setMemoryBeats(beats(submissions.acceptedMemory(problem), (double) report.peakHeapBytes()));
        }
        if (failed != null) {
            submission.setFailedTestJson(json.writeValueAsString(failed));
        }
        submission = submissions.save(submission);
        telemetry.submissionJudged(new AlgoTelemetry.SubmissionEvent(problem.getSlug(),
                problem.getDifficulty().name(), verdict.name(), submission.getRuntimeMs(), submission.getMemoryBytes()));

        Profile after = progress.profile(user);
        Set<Achievement> had = unlocked(before);
        List<Achievement> fresh = unlocked(after).stream().filter(a -> !had.contains(a)).toList();
        return new SubmitResponse(submission.getId(), verdict, submission.getMessage(), report.diagnostics(),
                passed, tests.size(), submission.getRuntimeMs(), submission.getMemoryBytes(),
                submission.getAllocatedBytes(), submission.getRuntimeBeats(), submission.getMemoryBeats(),
                failed, after.xp() - before.xp(), after.level(), after.currentStreak(), fresh);
    }

    /** Percentage of earlier accepted results strictly worse than {@code mine}; null when there are none. */
    static Double beats(List<? extends Number> others, double mine) {
        List<Double> values = others.stream().filter(Objects::nonNull).map(Number::doubleValue).toList();
        if (values.isEmpty()) {
            return null;
        }
        long worse = values.stream().filter(v -> v > mine).count();
        return Math.round(1000.0 * worse / values.size()) / 10.0;
    }

    private CaseResult caseResult(Problem problem, int index, boolean custom, JsonNode args, JsonNode expected,
                                  TestRun run) {
        String input = preview(args);
        String want = expected == null ? null : preview(expected);
        if (run == null) {
            return new CaseResult(index, custom, input, want, null, false, null, null, null);
        }
        Double timeMs = run.timeNanos() / 1e6;
        if (run.error() != null) {
            return new CaseResult(index, custom, input, want, null, false, run.stdout(), timeMs, run.error().trace());
        }
        boolean passed = expected != null && OutputComparator.matches(problem.getCompareMode(), expected, run.output());
        return new CaseResult(index, custom, input, want, preview(run.output()), passed, run.stdout(), timeMs, null);
    }

    private String preview(JsonNode node) {
        String text = json.writeValueAsString(node);
        return text.length() <= PREVIEW_CHARS ? text : text.substring(0, PREVIEW_CHARS) + " … (" + text.length() + " chars)";
    }

    private static Verdict verdictFor(RunStatus status) {
        return switch (status) {
            case COMPLETED -> Verdict.ACCEPTED;
            case COMPILE_ERROR -> Verdict.COMPILE_ERROR;
            case TIME_LIMIT_EXCEEDED -> Verdict.TIME_LIMIT_EXCEEDED;
            case MEMORY_LIMIT_EXCEEDED -> Verdict.MEMORY_LIMIT_EXCEEDED;
            case EXITED, SETUP_ERROR -> Verdict.RUNTIME_ERROR;
            case INTERNAL_ERROR -> Verdict.INTERNAL_ERROR;
        };
    }

    private static String messageFor(Verdict verdict, SolveReport report, CaseResult failed) {
        if (report.message() != null) {
            return report.message();
        }
        return switch (verdict) {
            case WRONG_ANSWER -> "Wrong answer on test " + (failed.index() + 1);
            case RUNTIME_ERROR -> failed == null ? "Runtime error"
                    : failed.error().lines().findFirst().orElse("Runtime error");
            default -> null;
        };
    }

    private static Set<Achievement> unlocked(Profile profile) {
        return profile.achievements().stream()
                .filter(Profile.AchievementStatus::unlocked)
                .map(Profile.AchievementStatus::achievement)
                .collect(Collectors.toSet());
    }
}
