package dev.algocode.problem;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface TestCaseRepository extends JpaRepository<TestCase, Long> {

    List<TestCase> findByProblemOrderByOrdinalAsc(Problem problem);

    List<TestCase> findByProblemAndSampleTrueOrderByOrdinalAsc(Problem problem);

    @Modifying
    @Query("delete from TestCase t where t.problem = :problem")
    void deleteByProblem(Problem problem);
}
