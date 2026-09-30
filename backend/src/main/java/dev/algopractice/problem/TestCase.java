package dev.algopractice.problem;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "test_case")
public class TestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id")
    private Problem problem;
    private int ordinal;
    /** JSON array with one element per method parameter. */
    private String argsJson;
    private String expectedJson;
    /** Sample tests are shown to the user and used by "Run". */
    private boolean sample;

    protected TestCase() {
    }

    public TestCase(Problem problem, int ordinal, String argsJson, String expectedJson, boolean sample) {
        this.problem = problem;
        this.ordinal = ordinal;
        this.argsJson = argsJson;
        this.expectedJson = expectedJson;
        this.sample = sample;
    }

    public Long getId() { return id; }
    public Problem getProblem() { return problem; }
    public int getOrdinal() { return ordinal; }
    public String getArgsJson() { return argsJson; }
    public String getExpectedJson() { return expectedJson; }
    public boolean isSample() { return sample; }
}
