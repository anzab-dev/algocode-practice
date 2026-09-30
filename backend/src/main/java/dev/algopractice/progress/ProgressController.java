package dev.algopractice.progress;

import dev.algopractice.problem.ProblemService;
import dev.algopractice.problem.ProblemViews;
import dev.algopractice.user.AppUser;
import dev.algopractice.user.CurrentUser;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ProgressController {

    private final ProgressService progress;
    private final ProblemService problems;

    public ProgressController(ProgressService progress, ProblemService problems) {
        this.progress = progress;
        this.problems = problems;
    }

    public record Me(Profile profile, List<ProblemViews.SubmissionSummary> recent) {
    }

    @GetMapping("/me")
    public Me me(@CurrentUser AppUser user) {
        return new Me(progress.profile(user), problems.recent(user, 10));
    }

    @GetMapping("/leaderboard")
    public List<ProgressService.LeaderboardEntry> leaderboard(@RequestParam(defaultValue = "20") int limit) {
        return progress.leaderboard(Math.min(Math.max(limit, 1), 100));
    }
}
