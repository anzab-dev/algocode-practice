package dev.algocode.language;

import dev.algocode.execution.LineMap;
import dev.algocode.language.LanguageModels.CompletionItem;
import dev.algocode.language.LanguageModels.Signature;
import dev.algocode.language.LanguageModels.SignatureHelpResponse;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Parameter info (IntelliJ's Ctrl+P): finds the call the caret is inside, then asks the
 * completion engine which overloads of that method the receiver offers.
 */
@Component
public class SignatureHelpProvider {

    private final CompletionProvider completion;

    public SignatureHelpProvider(CompletionProvider completion) {
        this.completion = completion;
    }

    public SignatureHelpResponse signatureHelp(String source, int line, int column, String fallbackClassName) {
        LineMap lines = new LineMap(source);
        int offset = lines.offset(line, column);
        if (SourceContext.inCommentOrLiteral(source, offset)) {
            return null;
        }
        int[] call = enclosingCall(source, offset);
        if (call == null) {
            return null;
        }
        int nameEnd = call[0];
        int nameStart = SourceContext.identifierStart(source, nameEnd);
        if (nameStart == nameEnd) {
            return null;
        }
        String name = source.substring(nameStart, nameEnd);
        var response = completion.complete(source, lines.line(nameEnd), lines.column(nameEnd), fallbackClassName);
        List<Signature> signatures = new ArrayList<>();
        for (CompletionItem item : response.items()) {
            if (item.kind().equals("Method") && item.label().equals(name) && item.signature() != null) {
                String inner = item.signature().substring(1, item.signature().length() - 1);
                List<String> params = inner.isEmpty() ? List.of() : List.of(inner.split(", "));
                signatures.add(new Signature(name + item.signature() + (item.type() == null ? "" : " : " + item.type()),
                        params, item.owner() == null ? null : "Declared in " + item.owner()));
            }
        }
        if (signatures.isEmpty()) {
            return null;
        }
        int activeParameter = call[1];
        int active = 0;
        for (int i = 0; i < signatures.size(); i++) {
            if (signatures.get(i).parameters().size() > activeParameter) {
                active = i;
                break;
            }
        }
        return new SignatureHelpResponse(signatures, active, activeParameter);
    }

    /**
     * Walks back from the caret to the unmatched '(' of the current call.
     * Returns {end of the method name, index of the argument under the caret}, or null.
     */
    static int[] enclosingCall(String s, int offset) {
        int depth = 0;
        int commas = 0;
        for (int i = offset - 1; i >= 0; i--) {
            char c = s.charAt(i);
            if (c == ')' || c == ']' || c == '}') {
                depth++;
            } else if (c == '[' || c == '{') {
                if (depth == 0) {
                    return null;
                }
                depth--;
            } else if (c == '(') {
                if (depth == 0) {
                    int end = i;
                    while (end > 0 && Character.isWhitespace(s.charAt(end - 1))) {
                        end--;
                    }
                    return new int[] {end, commas};
                }
                depth--;
            } else if (c == ',' && depth == 0) {
                commas++;
            } else if (c == ';' && depth == 0) {
                return null;
            }
        }
        return null;
    }
}
