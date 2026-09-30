package dev.algocode.progress;

import dev.algocode.problem.Difficulty;
import dev.algocode.problem.Problem;
import dev.algocode.problem.ProblemRepository;
import dev.algocode.scratchpad.ScratchpadRepository;
import dev.algocode.submission.Submission;
import dev.algocode.submission.SubmissionRepository;
import dev.algocode.submission.Verdict;
import dev.algocode.user.AppUser;
import dev.algocode.user.AppUserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Derives XP, levels, streaks and achievements from a user's submission history. */
@Service
@Transactional(readOnly = true)
public class ProgressService {

    static final int XP_PER_ACHIEVEMENT = 5;
    static final int ACTIVITY_DAYS = 182;

    private final SubmissionRepository submissions;
    private final ProblemRepository problems;
    private final ScratchpadRepository scratchpads;
    private final AppUserRepository users;
    private final Clock clock;

    @Autowired
    public ProgressService(SubmissionRepository submissions, ProblemRepository problems,
                           ScratchpadRepository scratchpads, AppUserRepository users) {
        this(submissions, problems, scratchpads, users, Clock.systemUTC());
    }

    ProgressService(SubmissionRepository submissions, ProblemRepository problems,
                    ScratchpadRepository scratchpads, AppUserRepository users, Clock clock) {
        this.submissions = submissions;
        this.problems = problems;
        this.scratchpads = scratchpads;
        this.users = users;
        this.clock = clock;
    }

    public Profile profile(AppUser user) {
        List<Problem> allProblems = problems.findAll();
        List<Submission> history = submissions.findByUserOrderByCreatedAtAsc(user);
        long scratchCount = scratchpads.countByUser(user);

        Map<Long, Problem> byId = new LinkedHashMap<>();
        allProblems.forEach(p -> byId.put(p.getId(), p));
        Set<Long> solved = new HashSet<>();
        Set<Long> attempted = new HashSet<>();
        Set<Long> firstTryAccepted = new HashSet<>();
        Set<LocalDate> activeDays = new TreeSet<>();
        Map<LocalDate, Integer> activity = new TreeMap<>();
        LocalDate today = LocalDate.now(clock);
        boolean fast = false;
        boolean lean = false;
        int accepted = 0;

        for (Submission s : history) {
            Long pid = s.getProblem().getId();
            LocalDate day = s.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate();
            activeDays.add(day);
            if (!day.isBefore(today.minusDays(ACTIVITY_DAYS))) {
                activity.merge(day, 1, Integer::sum);
            }
            boolean firstAttempt = attempted.add(pid);
            if (s.getVerdict() == Verdict.ACCEPTED) {
                accepted++;
                solved.add(pid);
                if (firstAttempt) {
                    firstTryAccepted.add(pid);
                }
                fast |= s.getRuntimeBeats() != null && s.getRuntimeBeats() >= 90;
                lean |= s.getMemoryBeats() != null && s.getMemoryBeats() >= 90;
            }
        }
        attempted.removeAll(solved);

        Map<String, Profile.DifficultyProgress> byDifficulty = new LinkedHashMap<>();
        int baseXp = 0;
        for (Difficulty d : Difficulty.values()) {
            int total = 0;
            int done = 0;
            for (Problem p : allProblems) {
                if (p.getDifficulty() == d) {
                    total++;
                    if (solved.contains(p.getId())) {
                        done++;
                        baseXp += d.xp();
                    }
                }
            }
            byDifficulty.put(d.name(), new Profile.DifficultyProgress(done, total));
        }

        int[] streaks = streaks(activeDays, today);
        Set<Achievement> unlocked = EnumSet.noneOf(Achievement.class);
        if (!solved.isEmpty()) unlocked.add(Achievement.FIRST_SOLVE);
        if (solved.size() >= 5) unlocked.add(Achievement.FIVE_SOLVES);
        Profile.DifficultyProgress easy = byDifficulty.get(Difficulty.EASY.name());
        if (easy.total() > 0 && easy.solved() == easy.total()) unlocked.add(Achievement.ALL_EASY);
        if (byDifficulty.get(Difficulty.HARD.name()).solved() > 0) unlocked.add(Achievement.FIRST_HARD);
        if (!firstTryAccepted.isEmpty()) unlocked.add(Achievement.CLEAN_SHEET);
        if (fast) unlocked.add(Achievement.SPEED_DEMON);
        if (lean) unlocked.add(Achievement.MEMORY_MISER);
        if (streaks[1] >= 3) unlocked.add(Achievement.ON_A_ROLL);
        if (streaks[1] >= 7) unlocked.add(Achievement.UNSTOPPABLE);
        if (scratchCount >= 3) unlocked.add(Achievement.TINKERER);

        List<Profile.AchievementStatus> achievements = new ArrayList<>();
        for (Achievement a : Achievement.values()) {
            achievements.add(new Profile.AchievementStatus(a, unlocked.contains(a)));
        }
        int xp = baseXp + unlocked.size() * XP_PER_ACHIEVEMENT;
        int level = levelFor(xp);
        return new Profile(user.getHandle(), xp, level, levelStart(level), levelStart(level + 1),
                streaks[0], streaks[1], solved.size(), allProblems.size(), byDifficulty,
                history.size(), history.isEmpty() ? 0 : 100.0 * accepted / history.size(),
                achievements, activity, solved, attempted);
    }

    public List<LeaderboardEntry> leaderboard(int limit) {
        List<LeaderboardEntry> rows = new ArrayList<>();
        for (AppUser user : users.findAll()) {
            Profile p = profile(user);
            if (p.submissions() > 0) {
                rows.add(new LeaderboardEntry(p.handle(), p.xp(), p.level(), p.solved(), p.currentStreak()));
            }
        }
        rows.sort(Comparator.comparingInt(LeaderboardEntry::xp).reversed()
                .thenComparing(Comparator.comparingInt(LeaderboardEntry::solved).reversed())
                .thenComparing(LeaderboardEntry::handle));
        return rows.size() > limit ? rows.subList(0, limit) : rows;
    }

    public record LeaderboardEntry(String handle, int xp, int level, int solved, int streak) {
    }

    /** Level L starts at 15 * L * (L - 1) XP: 0, 30, 90, 180, 300, ... */
    static int levelStart(int level) {
        return 15 * level * (level - 1);
    }

    static int levelFor(int xp) {
        int level = 1;
        while (levelStart(level + 1) <= xp) {
            level++;
        }
        return level;
    }

    /** Returns {current, longest}; the current streak survives until the end of the day after the last activity. */
    static int[] streaks(Set<LocalDate> days, LocalDate today) {
        int longest = 0;
        int run = 0;
        LocalDate previous = null;
        for (LocalDate day : new TreeSet<>(days)) {
            run = previous != null && previous.plusDays(1).equals(day) ? run + 1 : 1;
            longest = Math.max(longest, run);
            previous = day;
        }
        int current = 0;
        LocalDate cursor = days.contains(today) ? today : today.minusDays(1);
        while (days.contains(cursor)) {
            current++;
            cursor = cursor.minusDays(1);
        }
        return new int[] {current, longest};
    }
}
