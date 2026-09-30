package algocode.harness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JsonTest {

    @Test
    void parsesNestedStructures() {
        Object value = Json.parse("{\"a\": [1, -2.5, true, null, \"x\\n\\u0041\"], \"b\": {}}");
        assertThat(value).isEqualTo(Map.of(
                "a", java.util.Arrays.asList(1L, -2.5, true, null, "x\nA"),
                "b", Map.of()));
    }

    @Test
    void stringifiesArraysCollectionsAndChars() {
        assertThat(Json.stringify(new int[][] {{1, 2}, {3}})).isEqualTo("[[1,2],[3]]");
        assertThat(Json.stringify(List.of('a', "b\"c"))).isEqualTo("[\"a\",\"b\\\"c\"]");
        assertThat(Json.stringify(2.0)).isEqualTo("2.0");
        assertThat(Json.stringify(new boolean[] {true})).isEqualTo("[true]");
    }

    @Test
    void rejectsTrailingGarbage() {
        assertThatThrownBy(() -> Json.parse("[1] x")).isInstanceOf(IllegalArgumentException.class);
    }
}
