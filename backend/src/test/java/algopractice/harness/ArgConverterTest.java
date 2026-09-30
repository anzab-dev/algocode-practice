package algopractice.harness;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ArgConverterTest {

    @SuppressWarnings("unused")
    static void sample(int a, long[] b, char[][] c, List<List<Integer>> d, String[] e, Map<Integer, String> f,
                       Character g, double h) {
    }

    @Test
    void convertsToDeclaredParameterTypes() throws Exception {
        Method m = ArgConverterTest.class.getDeclaredMethod("sample", int.class, long[].class, char[][].class,
                List.class, String[].class, Map.class, Character.class, double.class);
        var types = m.getGenericParameterTypes();

        assertThat(ArgConverter.convert(5L, types[0])).isEqualTo(5);
        assertThat((long[]) ArgConverter.convert(List.of(1L, 2L), types[1])).containsExactly(1L, 2L);
        char[][] grid = (char[][]) ArgConverter.convert(List.of(List.of("a", "b")), types[2]);
        assertThat(grid[0]).containsExactly('a', 'b');
        assertThat(ArgConverter.convert(List.of(List.of(1L), List.of()), types[3]))
                .isEqualTo(List.of(List.of(1), List.of()));
        assertThat((String[]) ArgConverter.convert(List.of("x"), types[4])).containsExactly("x");
        assertThat(ArgConverter.convert(Map.of("7", "seven"), types[5])).isEqualTo(Map.of(7, "seven"));
        assertThat(ArgConverter.convert("z", types[6])).isEqualTo('z');
        assertThat(ArgConverter.convert(3L, types[7])).isEqualTo(3.0);
    }
}
