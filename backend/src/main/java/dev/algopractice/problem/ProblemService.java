package dev.algopractice.problem;

import dev.algopractice.progress.Profile;
import dev.algopractice.progress.ProgressService;
import dev.algopractice.submission.Submission;
import dev.algopractice.submission.SubmissionRepository;
import dev.algopractice.submission.Verdict;
import dev.algopractice.user.AppUser;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
@Transactional(readOnly = true)
public class ProblemService {

    static final int BUCKETS = 12;

    private final ProblemRepository problems;
    private final TestCaseRepository testCases;
    private final SubmissionRepository submissions;
    private final ProgressService progress;
    private final ObjectMapper json;
    private final Clock clock = Clock.systemUTC();

    public ProblemService(ProblemRepository problems, TestCaseRepository testCases, SubmissionRepository submissions,
                          ProgressService progress, ObjectMapper json) {
        this.problems = problems;
        this.testCases = testCases;
        this.submissions = submissions;
        this.progress = progress;
        this.json = json;
    }

    public Problem get(String slug) {
        return problems.findBySlug(slug).orElseThrow(() -> new NoSuchElementException("Problem '" + slug + "' not found"));
    }

    public List<ProblemViews.Summary> list(AppUser user) {
        Map<Long, long[]> acceptance = new HashMap<>();
        for (Object[] row : submissions.acceptanceByProblem()) {
            acceptance.put((Long) row[0], new long[] {((Number) row[1]).longValue(), ((Number) row[2]).longValue()});
        }
        Profile profile = progress.profile(user);
        List<ProblemViews.Summary> out = new ArrayList<>();
        for (Problem p : problems.findAllByOrderBySortOrderAscIdAsc()) {
            long[] a = acceptance.getOrDefault(p.getId(), new long[] {0, 0});
            ProblemViews.Status status = profile.solvedProblemIds().contains(p.getId()) ? ProblemViews.Status.SOLVED
                    : profile.attemptedProblemIds().contains(p.getId()) ? ProblemViews.Status.ATTEMPTED : null;
            out.add(new ProblemViews.Summary(p.getId(), p.getSlug(), p.getTitle(), p.getDifficulty(), p.tagList(),
                    a[0] == 0 ? 0 : 100.0 * a[1] / a[0], (int) a[0], status));
        }
        return out;
    }

    /** The same problem for everyone on a given UTC day. */
    public ProblemViews.Summary daily(AppUser user) {
        List<ProblemViews.Summary> all = list(user);
        if (all.isEmpty()) {
            throw new NoSuchElementException("No problems available");
        }
        long day = LocalDate.now(clock).toEpochDay();
        return all.get((int) Math.floorMod(day * 7919L, all.size()));
    }

    public ProblemViews.Detail detail(String slug) {
        Problem p = get(slug);
        List<ProblemViews.Sample> samples = testCases.findByProblemAndSampleTrueOrderByOrdinalAsc(p).stream()
                .map(t -> new ProblemViews.Sample(t.getArgsJson(), t.getExpectedJson()))
                .toList();
        List<Problem> ordered = problems.findAllByOrderBySortOrderAscIdAsc();
        int idx = ordered.indexOf(p);
        return new ProblemViews.Detail(p.getId(), p.getSlug(), p.getTitle(), p.getDifficulty(), p.tagList(),
                p.getDescription(), p.getStarterCode(), p.getMethodName(),
                json.readValue(p.getParamsJson(), new TypeReference<List<ProblemDefinition.Param>>() { }),
                p.getReturnType(), p.getCompareMode(), p.getTimeLimitMs(),
                json.readValue(p.getHintsJson(), new TypeReference<List<String>>() { }), samples,
                idx > 0 ? ordered.get(idx - 1).getSlug() : null,
                idx >= 0 && idx < ordered.size() - 1 ? ordered.get(idx + 1).getSlug() : null);
    }

    public List<ProblemViews.SubmissionSummary> submissions(AppUser user, String slug) {
        return submissions.findTop50ByUserAndProblemOrderByCreatedAtDesc(user, get(slug)).stream()
                .map(ProblemService::summary)
                .toList();
    }

    public ProblemViews.SubmissionDetail submission(AppUser user, long id) {
        Submission s = submissions.findById(id)
                .filter(sub -> sub.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new NoSuchElementException("Submission " + id + " not found"));
        return new ProblemViews.SubmissionDetail(summary(s), s.getCode(), s.getMessage(), s.getFailedTestJson());
    }

    public List<ProblemViews.SubmissionSummary> recent(AppUser user, int limit) {
        List<Submission> all = submissions.findByUserOrderByCreatedAtAsc(user);
        List<ProblemViews.SubmissionSummary> out = new ArrayList<>();
        for (int i = all.size() - 1; i >= 0 && out.size() < limit; i--) {
            out.add(summary(all.get(i)));
        }
        return out;
    }

    public ProblemViews.Stats stats(AppUser user, String slug) {
        Problem p = get(slug);
        List<Submission> mine = submissions.findTop50ByUserAndProblemOrderByCreatedAtDesc(user, p).stream()
                .filter(s -> s.getVerdict() == Verdict.ACCEPTED)
                .toList();
        Double bestRuntime = mine.stream().map(Submission::getRuntimeMs).filter(Objects::nonNull)
                .min(Double::compare).orElse(null);
        Double bestMemory = mine.stream().map(Submission::getMemoryBytes).filter(Objects::nonNull)
                .map(Long::doubleValue).min(Double::compare).orElse(null);
        return new ProblemViews.Stats(
                histogram(submissions.acceptedRuntimes(p), Function.identity(), bestRuntime),
                histogram(submissions.acceptedMemory(p), Long::doubleValue, bestMemory));
    }

    static <T> ProblemViews.Distribution histogram(List<T> raw, Function<T, Double> toDouble, Double mine) {
        List<Double> values = raw.stream().filter(Objects::nonNull).map(toDouble).sorted().toList();
        if (values.isEmpty()) {
            return new ProblemViews.Distribution(List.of(), mine, 0);
        }
        // Trim the slowest 5% so one pathological run does not squash the chart.
        double min = values.getFirst();
        double max = values.get((int) Math.floor((values.size() - 1) * 0.95));
        if (mine != null) {
            max = Math.max(max, mine);
        }
        double width = max > min ? (max - min) / BUCKETS : 1;
        int[] counts = new int[BUCKETS];
        for (double v : values) {
            counts[(int) Math.min(BUCKETS - 1, Math.max(0, Math.floor((v - min) / width)))]++;
        }
        List<ProblemViews.Bucket> buckets = new ArrayList<>();
        for (int i = 0; i < BUCKETS; i++) {
            buckets.add(new ProblemViews.Bucket(min + i * width, min + (i + 1) * width, counts[i]));
        }
        return new ProblemViews.Distribution(buckets, mine, values.size());
    }

    static ProblemViews.SubmissionSummary summary(Submission s) {
        return new ProblemViews.SubmissionSummary(s.getId(), s.getProblem().getSlug(), s.getProblem().getTitle(),
                s.getVerdict().name(), s.getPassed(), s.getTotal(), s.getRuntimeMs(), s.getMemoryBytes(),
                s.getRuntimeBeats(), s.getMemoryBeats(), s.getCreatedAt());
    }
}
