package dev.algopractice.language;

import dev.algopractice.execution.SourceDiagnostic;
import dev.algopractice.language.LanguageModels.CompletionResponse;
import dev.algopractice.language.LanguageModels.FileKind;
import dev.algopractice.language.LanguageModels.HoverResponse;
import dev.algopractice.language.LanguageModels.SignatureHelpResponse;
import dev.algopractice.telemetry.AlgoTelemetry;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

/** Facade over the javac-backed editor features, with timing reported to telemetry. */
@Service
public class LanguageService {

    private final JavaAnalyzer analyzer;
    private final DiagnosticsProvider diagnostics;
    private final CompletionProvider completion;
    private final HoverProvider hover;
    private final SignatureHelpProvider signatures;
    private final AlgoTelemetry telemetry;

    public LanguageService(JavaAnalyzer analyzer, DiagnosticsProvider diagnostics, CompletionProvider completion,
                           HoverProvider hover, SignatureHelpProvider signatures, AlgoTelemetry telemetry) {
        this.analyzer = analyzer;
        this.diagnostics = diagnostics;
        this.completion = completion;
        this.hover = hover;
        this.signatures = signatures;
        this.telemetry = telemetry;
    }

    public List<SourceDiagnostic> diagnostics(String code, FileKind kind) {
        return timed("diagnostics", () -> diagnostics.diagnostics(analyzer.analyze(code, fallback(kind))));
    }

    public CompletionResponse complete(String code, FileKind kind, int line, int column) {
        return timed("completion", () -> completion.complete(code, line, column, fallback(kind)));
    }

    public HoverResponse hover(String code, FileKind kind, int line, int column) {
        return timed("hover", () -> hover.hover(code, line, column, fallback(kind)));
    }

    public SignatureHelpResponse signatureHelp(String code, FileKind kind, int line, int column) {
        return timed("signature", () -> signatures.signatureHelp(code, line, column, fallback(kind)));
    }

    private static String fallback(FileKind kind) {
        return kind == FileKind.SCRATCH ? "Main" : "Solution";
    }

    private <T> T timed(String kind, Supplier<T> work) {
        long start = System.nanoTime();
        try {
            return work.get();
        } finally {
            telemetry.languageRequest(kind, System.nanoTime() - start);
        }
    }
}
