package dev.algopractice.progress;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ProgressServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 30);

    @Test
    void levelsGrowQuadratically() {
        assertThat(ProgressService.levelFor(0)).isEqualTo(1);
        assertThat(ProgressService.levelFor(29)).isEqualTo(1);
        assertThat(ProgressService.levelFor(30)).isEqualTo(2);
        assertThat(ProgressService.levelFor(90)).isEqualTo(3);
        assertThat(ProgressService.levelStart(4)).isEqualTo(180);
    }

    @Test
    void currentStreakCountsBackFromTodayOrYesterday() {
        Set<LocalDate> days = Set.of(TODAY.minusDays(1), TODAY.minusDays(2), TODAY.minusDays(3),
                TODAY.minusDays(10), TODAY.minusDays(11));
        assertThat(ProgressService.streaks(days, TODAY)).containsExactly(3, 3);
        assertThat(ProgressService.streaks(days, TODAY.plusDays(2))).containsExactly(0, 3);
        assertThat(ProgressService.streaks(Set.of(TODAY), TODAY)).containsExactly(1, 1);
        assertThat(ProgressService.streaks(Set.of(), TODAY)).containsExactly(0, 0);
    }
}
