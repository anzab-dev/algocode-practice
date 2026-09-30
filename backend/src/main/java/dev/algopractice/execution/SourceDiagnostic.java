package dev.algopractice.execution;

/**
 * A compiler or analyzer finding, positioned with 1-based lines and columns so it maps
 * directly onto Monaco markers. The end position is exclusive.
 */
public record SourceDiagnostic(
        Severity severity,
        String message,
        String code,
        int startLine,
        int startColumn,
        int endLine,
        int endColumn) {

    public enum Severity { ERROR, WARNING, INFO }
}
