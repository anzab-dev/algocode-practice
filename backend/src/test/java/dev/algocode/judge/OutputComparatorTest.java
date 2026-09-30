package dev.algocode.judge;

import static org.assertj.core.api.Assertions.assertThat;

import dev.algocode.problem.CompareMode;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class OutputComparatorTest {

    private final JsonMapper json = JsonMapper.builder().build();

    private boolean matches(CompareMode mode, String expected, String actual) {
        return OutputComparator.matches(mode, json.readTree(expected), json.readTree(actual));
    }

    @Test
    void exactTreatsEqualNumbersAsEqual() {
        assertThat(matches(CompareMode.EXACT, "[1,2]", "[1,2]")).isTrue();
        assertThat(matches(CompareMode.EXACT, "[1,2]", "[2,1]")).isFalse();
        assertThat(matches(CompareMode.EXACT, "2", "2.0")).isTrue();
        assertThat(matches(CompareMode.EXACT, "\"1\"", "1")).isFalse();
        assertThat(matches(CompareMode.EXACT, "true", "true")).isTrue();
    }

    @Test
    void unorderedIgnoresTopLevelOrderOnly() {
        assertThat(matches(CompareMode.UNORDERED, "[[1,6],[8,10]]", "[[8,10],[1,6]]")).isTrue();
        assertThat(matches(CompareMode.UNORDERED, "[[1,6]]", "[[6,1]]")).isFalse();
        assertThat(matches(CompareMode.UNORDERED, "[1,1,2]", "[1,2,2]")).isFalse();
    }

    @Test
    void unorderedDeepIgnoresNestedOrder() {
        assertThat(matches(CompareMode.UNORDERED_DEEP, "[[\"bat\"],[\"nat\",\"tan\"]]",
                "[[\"tan\",\"nat\"],[\"bat\"]]")).isTrue();
        assertThat(matches(CompareMode.UNORDERED_DEEP, "[[\"a\"],[\"b\"]]", "[[\"a\",\"b\"]]")).isFalse();
    }

    @Test
    void floatAllowsSmallError() {
        assertThat(matches(CompareMode.FLOAT, "2.5", "2.500001")).isTrue();
        assertThat(matches(CompareMode.FLOAT, "2.5", "2.51")).isFalse();
        assertThat(matches(CompareMode.FLOAT, "[1.0, 2.0]", "[1, 2.000000001]")).isTrue();
    }

    @Test
    void nullOutputOnlyMatchesNull() {
        assertThat(OutputComparator.matches(CompareMode.EXACT, json.readTree("null"), null)).isTrue();
        assertThat(OutputComparator.matches(CompareMode.EXACT, json.readTree("[1]"), null)).isFalse();
    }
}
