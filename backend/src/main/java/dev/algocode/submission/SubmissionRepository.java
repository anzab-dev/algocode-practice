package dev.algocode.submission;

import dev.algocode.problem.Problem;
import dev.algocode.user.AppUser;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    List<Submission> findTop50ByUserAndProblemOrderByCreatedAtDesc(AppUser user, Problem problem);

    List<Submission> findByUserOrderByCreatedAtAsc(AppUser user);

    @Query("select s.runtimeMs from Submission s where s.problem = :problem and s.verdict = dev.algocode.submission.Verdict.ACCEPTED")
    List<Double> acceptedRuntimes(Problem problem);

    @Query("select s.memoryBytes from Submission s where s.problem = :problem and s.verdict = dev.algocode.submission.Verdict.ACCEPTED")
    List<Long> acceptedMemory(Problem problem);

    /** Rows of [problemId, submissions, accepted]. */
    @Query("""
            select s.problem.id, count(s), sum(case when s.verdict = dev.algocode.submission.Verdict.ACCEPTED then 1 else 0 end)
            from Submission s group by s.problem.id""")
    List<Object[]> acceptanceByProblem();

    long countByUser(AppUser user);
}
