package dev.algopractice.problem;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ProblemSeederTest {

    @Autowired
    ProblemSeeder seeder;
    @Autowired
    ProblemRepository problems;

    @Test
    void renamedProblemKeepsItsRow() {
        Problem renamed = problems.findBySlug("pair-with-target-sum").orElseThrow();
        long id = renamed.getId();
        renamed.setSlug("two-sum");
        renamed.setContentHash("stale");
        problems.saveAndFlush(renamed);
        long count = problems.count();

        seeder.run(null);

        assertThat(problems.findBySlug("two-sum")).isEmpty();
        assertThat(problems.findBySlug("pair-with-target-sum")).get().extracting(Problem::getId).isEqualTo(id);
        assertThat(problems.count()).isEqualTo(count);
    }
}
