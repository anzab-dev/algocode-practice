package dev.algocode.problem;

/** How a solution's output is compared with the expected answer. */
public enum CompareMode {
    /** Deep equality; integral and floating numbers with the same value are equal. */
    EXACT,
    /** The top-level array may come back in any order. */
    UNORDERED,
    /** Neither the top-level array nor the arrays inside it are ordered. */
    UNORDERED_DEEP,
    /** Numbers are compared with an absolute/relative tolerance of 1e-5. */
    FLOAT
}
