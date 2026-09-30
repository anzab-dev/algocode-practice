package dev.algocode.language;

import dev.algocode.execution.JavaSourceCompiler;
import dev.algocode.language.LanguageModels.CompletionResponse;
import dev.algocode.language.LanguageModels.DiagnosticsResponse;
import dev.algocode.language.LanguageModels.FileKind;
import dev.algocode.language.LanguageModels.HoverResponse;
import dev.algocode.language.LanguageModels.PositionRequest;
import dev.algocode.language.LanguageModels.SignatureHelpResponse;
import dev.algocode.language.LanguageModels.SourceRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Editor intelligence for Monaco: diagnostics, completion, hover and parameter info. */
@RestController
@RequestMapping("/api/lang")
public class LanguageController {

    private final LanguageService language;

    public LanguageController(LanguageService language) {
        this.language = language;
    }

    @PostMapping("/diagnostics")
    public DiagnosticsResponse diagnostics(@RequestBody SourceRequest request) {
        long start = System.nanoTime();
        var result = language.diagnostics(checked(request.code()), kind(request.kind()));
        return new DiagnosticsResponse(result, (System.nanoTime() - start) / 1_000_000);
    }

    @PostMapping("/completion")
    public CompletionResponse completion(@RequestBody PositionRequest request) {
        return language.complete(checked(request.code()), kind(request.kind()), request.line(), request.column());
    }

    @PostMapping("/hover")
    public ResponseEntity<HoverResponse> hover(@RequestBody PositionRequest request) {
        HoverResponse hover = language.hover(checked(request.code()), kind(request.kind()), request.line(),
                request.column());
        return hover == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(hover);
    }

    @PostMapping("/signature")
    public ResponseEntity<SignatureHelpResponse> signature(@RequestBody PositionRequest request) {
        SignatureHelpResponse help = language.signatureHelp(checked(request.code()), kind(request.kind()),
                request.line(), request.column());
        return help == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(help);
    }

    private static String checked(String code) {
        if (code == null) {
            throw new IllegalArgumentException("code is required");
        }
        if (code.length() > JavaSourceCompiler.MAX_SOURCE_CHARS) {
            throw new IllegalArgumentException("Source is too long");
        }
        return code;
    }

    private static FileKind kind(FileKind kind) {
        return kind == null ? FileKind.SOLUTION : kind;
    }
}
