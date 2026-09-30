package dev.algocode.problem;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Keeps the database in sync with the problem files on start-up. Problems are matched by slug
 * and only rewritten when their files changed, so submissions keep pointing at the same row.
 */
@Component
public class ProblemSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ProblemSeeder.class);

    private final ProblemCatalog catalog;
    private final ProblemRepository problems;
    private final TestCaseRepository testCases;
    private final ObjectMapper json;

    public ProblemSeeder(ProblemCatalog catalog, ProblemRepository problems, TestCaseRepository testCases,
                         ObjectMapper json) {
        this.catalog = catalog;
        this.problems = problems;
        this.testCases = testCases;
        this.json = json;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int changed = 0;
        List<ProblemDefinition> definitions = catalog.load();
        for (ProblemDefinition def : definitions) {
            Problem problem = problems.findBySlug(def.slug()).orElseGet(Problem::new);
            if (def.contentHash().equals(problem.getContentHash())) {
                continue;
            }
            apply(def, problem);
            problem = problems.save(problem);
            testCases.deleteByProblem(problem);
            int ordinal = 0;
            for (ProblemDefinition.Test t : def.tests()) {
                testCases.save(new TestCase(problem, ordinal++, json.writeValueAsString(t.args()),
                        json.writeValueAsString(t.expected()), t.sample()));
            }
            changed++;
        }
        log.info("Problem catalog: {} problems, {} created or updated", definitions.size(), changed);
    }

    private void apply(ProblemDefinition def, Problem p) {
        ProblemDefinition.Meta m = def.meta();
        p.setSlug(def.slug());
        p.setTitle(m.title());
        p.setDifficulty(m.difficulty());
        p.setTags(m.tags() == null ? "" : String.join(",", m.tags()));
        p.setDescription(def.description());
        p.setStarterCode(m.starterCode());
        p.setReferenceCode(def.referenceCode());
        p.setMethodName(m.method());
        p.setParamsJson(json.writeValueAsString(m.params() == null ? List.of() : m.params()));
        p.setReturnType(m.returnType() == null ? "" : m.returnType());
        p.setCompareMode(m.compare() == null ? CompareMode.EXACT : m.compare());
        p.setTimeLimitMs(m.timeLimitMs() == null ? 2000 : m.timeLimitMs());
        p.setHintsJson(json.writeValueAsString(m.hints() == null ? List.of() : m.hints()));
        p.setSortOrder(m.order() == null ? 1000 : m.order());
        p.setContentHash(def.contentHash());
    }
}
