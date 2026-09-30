package dev.algopractice.judge;

import dev.algopractice.problem.CompareMode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import tools.jackson.databind.JsonNode;

/** Decides whether a solution's output matches the expected answer. */
public final class OutputComparator {

    static final double EPSILON = 1e-5;

    private OutputComparator() {
    }

    public static boolean matches(CompareMode mode, JsonNode expected, JsonNode actual) {
        if (expected == null || actual == null) {
            return isNull(expected) && isNull(actual);
        }
        return switch (mode) {
            case EXACT -> normalize(expected, 0).equals(normalize(actual, 0));
            case UNORDERED -> normalize(expected, 1).equals(normalize(actual, 1));
            case UNORDERED_DEEP -> normalize(expected, Integer.MAX_VALUE).equals(normalize(actual, Integer.MAX_VALUE));
            case FLOAT -> closeEnough(expected, actual);
        };
    }

    private static boolean isNull(JsonNode node) {
        return node == null || node.isNull();
    }

    /**
     * Converts JSON into plain values with canonical numbers. Arrays nested fewer than
     * {@code unorderedDepth} levels deep are sorted so their order does not matter.
     */
    private static Object normalize(JsonNode node, int unorderedDepth) {
        if (node.isNumber()) {
            return canonical(node.decimalValue());
        }
        if (node.isArray()) {
            List<Object> items = new ArrayList<>();
            for (JsonNode child : node) {
                items.add(normalize(child, unorderedDepth - 1));
            }
            if (unorderedDepth > 0) {
                items.sort(Comparator.comparing(String::valueOf));
            }
            return items;
        }
        if (node.isObject()) {
            Map<String, Object> map = new TreeMap<>();
            for (Map.Entry<String, JsonNode> e : node.properties()) {
                map.put(e.getKey(), normalize(e.getValue(), unorderedDepth - 1));
            }
            return map;
        }
        if (node.isNull()) {
            return "null";
        }
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        return "\"" + node.asString() + "\"";
    }

    private static BigDecimal canonical(BigDecimal value) {
        return value.signum() == 0 ? BigDecimal.ZERO : value.stripTrailingZeros();
    }

    private static boolean closeEnough(JsonNode expected, JsonNode actual) {
        if (expected.isNumber() && actual.isNumber()) {
            double e = expected.doubleValue();
            double a = actual.doubleValue();
            return Math.abs(e - a) <= EPSILON * Math.max(1.0, Math.abs(e));
        }
        if (expected.isArray() && actual.isArray()) {
            if (expected.size() != actual.size()) {
                return false;
            }
            for (int i = 0; i < expected.size(); i++) {
                if (!closeEnough(expected.get(i), actual.get(i))) {
                    return false;
                }
            }
            return true;
        }
        return normalize(expected, 0).equals(normalize(actual, 0));
    }
}
