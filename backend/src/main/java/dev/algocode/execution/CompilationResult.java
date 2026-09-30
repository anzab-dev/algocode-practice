package dev.algocode.execution;

import java.util.List;
import java.util.Map;

/**
 * Outcome of compiling a user's source file.
 *
 * @param classFiles  binary class name to bytecode, empty when compilation failed
 * @param mainClasses classes declaring {@code public static void main(String[])}
 */
public record CompilationResult(
        boolean success,
        List<SourceDiagnostic> diagnostics,
        Map<String, byte[]> classFiles,
        List<String> mainClasses) {

    public List<SourceDiagnostic> errors() {
        return diagnostics.stream()
                .filter(d -> d.severity() == SourceDiagnostic.Severity.ERROR)
                .toList();
    }
}
