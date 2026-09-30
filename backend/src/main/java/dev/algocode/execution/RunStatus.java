package dev.algocode.execution;

/** How a sandbox run ended, before any comparison against expected answers. */
public enum RunStatus {
    /** All requested tests (or the program) ran; individual tests may still have thrown. */
    COMPLETED,
    COMPILE_ERROR,
    TIME_LIMIT_EXCEEDED,
    MEMORY_LIMIT_EXCEEDED,
    /** The program called System.exit before finishing. */
    EXITED,
    /** Class or method missing, or arguments could not be converted. */
    SETUP_ERROR,
    INTERNAL_ERROR
}
