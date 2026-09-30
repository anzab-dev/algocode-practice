package dev.algopractice.language;

import dev.algopractice.execution.SourceDiagnostic;
import java.util.List;

/** Request and response shapes of the language API; positions are 1-based like Monaco's. */
public final class LanguageModels {

    private LanguageModels() {
    }

    /** {@code SOLUTION} files hold a {@code Solution} class, {@code SCRATCH} files a program with {@code main}. */
    public enum FileKind { SOLUTION, SCRATCH }

    public record SourceRequest(String code, FileKind kind) {
    }

    public record PositionRequest(String code, FileKind kind, int line, int column) {
    }

    public record Range(int startLine, int startColumn, int endLine, int endColumn) {
    }

    public record TextEdit(Range range, String text) {
    }

    public record DiagnosticsResponse(List<SourceDiagnostic> diagnostics, long durationMs) {
    }

    /**
     * @param kind       Monaco CompletionItemKind name (Method, Field, Variable, Class, ...)
     * @param signature  parameter list for methods, e.g. {@code (int index, E element)}
     * @param type       return or field type, shown right-aligned like IntelliJ
     * @param snippet    whether {@code insertText} uses snippet placeholders
     */
    public record CompletionItem(
            String label,
            String kind,
            String signature,
            String type,
            String owner,
            String insertText,
            boolean snippet,
            String sortText,
            List<TextEdit> additionalTextEdits) {
    }

    public record CompletionResponse(List<CompletionItem> items, Range replace, boolean incomplete) {
    }

    public record HoverResponse(String markdown, Range range) {
    }

    public record SignatureHelpResponse(List<Signature> signatures, int activeSignature, int activeParameter) {
    }

    public record Signature(String label, List<String> parameters, String documentation) {
    }
}
