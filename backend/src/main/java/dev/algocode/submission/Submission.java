package dev.algocode.submission;

import dev.algocode.problem.Problem;
import dev.algocode.user.AppUser;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "submission")
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id")
    private Problem problem;

    private String code;
    @Enumerated(EnumType.STRING)
    private Verdict verdict;
    private int passed;
    private int total;
    private Double runtimeMs;
    private Long memoryBytes;
    private Long allocatedBytes;
    /** Share (0-100) of other accepted submissions this one is faster than; null if it was the first. */
    private Double runtimeBeats;
    private Double memoryBeats;
    private String message;
    /** JSON of the first failing case, for the submission details view. */
    private String failedTestJson;
    private Instant createdAt;

    protected Submission() {
    }

    public Submission(AppUser user, Problem problem, String code) {
        this.user = user;
        this.problem = problem;
        this.code = code;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public Problem getProblem() { return problem; }
    public String getCode() { return code; }
    public Verdict getVerdict() { return verdict; }
    public void setVerdict(Verdict verdict) { this.verdict = verdict; }
    public int getPassed() { return passed; }
    public void setPassed(int passed) { this.passed = passed; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public Double getRuntimeMs() { return runtimeMs; }
    public void setRuntimeMs(Double runtimeMs) { this.runtimeMs = runtimeMs; }
    public Long getMemoryBytes() { return memoryBytes; }
    public void setMemoryBytes(Long memoryBytes) { this.memoryBytes = memoryBytes; }
    public Long getAllocatedBytes() { return allocatedBytes; }
    public void setAllocatedBytes(Long allocatedBytes) { this.allocatedBytes = allocatedBytes; }
    public Double getRuntimeBeats() { return runtimeBeats; }
    public void setRuntimeBeats(Double runtimeBeats) { this.runtimeBeats = runtimeBeats; }
    public Double getMemoryBeats() { return memoryBeats; }
    public void setMemoryBeats(Double memoryBeats) { this.memoryBeats = memoryBeats; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getFailedTestJson() { return failedTestJson; }
    public void setFailedTestJson(String failedTestJson) { this.failedTestJson = failedTestJson; }
    public Instant getCreatedAt() { return createdAt; }
}
