package dev.algocode.problem;

import dev.algocode.judge.JudgeService;
import dev.algocode.judge.RunRequest;
import dev.algocode.judge.RunResponse;
import dev.algocode.judge.SubmitRequest;
import dev.algocode.judge.SubmitResponse;
import dev.algocode.user.AppUser;
import dev.algocode.user.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ProblemController {

    private final ProblemService problems;
    private final JudgeService judge;

    public ProblemController(ProblemService problems, JudgeService judge) {
        this.problems = problems;
        this.judge = judge;
    }

    @GetMapping("/problems")
    public List<ProblemViews.Summary> list(@CurrentUser AppUser user) {
        return problems.list(user);
    }

    @GetMapping("/problems/daily")
    public ProblemViews.Summary daily(@CurrentUser AppUser user) {
        return problems.daily(user);
    }

    @GetMapping("/problems/{slug}")
    public ProblemViews.Detail detail(@PathVariable String slug) {
        return problems.detail(slug);
    }

    @PostMapping("/problems/{slug}/run")
    public RunResponse run(@PathVariable String slug, @Valid @RequestBody RunRequest request) {
        return judge.run(problems.get(slug), request.code(), request.customInputs());
    }

    @PostMapping("/problems/{slug}/submit")
    public SubmitResponse submit(@CurrentUser AppUser user, @PathVariable String slug,
                                 @Valid @RequestBody SubmitRequest request) {
        return judge.submit(user, problems.get(slug), request.code());
    }

    @GetMapping("/problems/{slug}/submissions")
    public List<ProblemViews.SubmissionSummary> submissions(@CurrentUser AppUser user, @PathVariable String slug) {
        return problems.submissions(user, slug);
    }

    @GetMapping("/problems/{slug}/stats")
    public ProblemViews.Stats stats(@CurrentUser AppUser user, @PathVariable String slug) {
        return problems.stats(user, slug);
    }

    @GetMapping("/submissions/{id}")
    public ProblemViews.SubmissionDetail submission(@CurrentUser AppUser user, @PathVariable long id) {
        return problems.submission(user, id);
    }
}
