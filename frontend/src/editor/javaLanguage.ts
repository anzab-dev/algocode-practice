import type * as Monaco from "monaco-editor";
import { api, type Diagnostic, type FileKind } from "../api";
import { LIVE_TEMPLATES } from "./templates";

type MonacoApi = typeof Monaco;

/** Models whose path starts with /scratch/ are scratchpad programs; everything else is a Solution file. */
export function fileKindOf(model: Monaco.editor.ITextModel): FileKind {
  return model.uri.path.startsWith("/scratch/") ? "SCRATCH" : "SOLUTION";
}

const KIND_MAP: Record<string, keyof typeof Monaco.languages.CompletionItemKind> = {
  Method: "Method",
  Field: "Field",
  Variable: "Variable",
  Class: "Class",
  Interface: "Interface",
  Enum: "Enum",
  EnumMember: "EnumMember",
  Struct: "Struct",
  Constant: "Constant",
  Keyword: "Keyword",
  Module: "Module",
};

function toRange(monaco: MonacoApi, r: { startLine: number; startColumn: number; endLine: number; endColumn: number }) {
  return new monaco.Range(r.startLine, r.startColumn, r.endLine, r.endColumn);
}

function abortOn(token: Monaco.CancellationToken): AbortSignal {
  const controller = new AbortController();
  token.onCancellationRequested(() => controller.abort());
  return controller.signal;
}

/**
 * Wires Monaco's Java language to the backend's javac-powered language service:
 * completion (with auto-import), hover, parameter info and live templates.
 * Diagnostics are pushed per editor by {@link attachDiagnostics}.
 */
export function registerJavaIntelligence(monaco: MonacoApi) {
  monaco.languages.registerCompletionItemProvider("java", {
    triggerCharacters: ["."],
    async provideCompletionItems(model, position, _context, token) {
      const kind = fileKindOf(model);
      const word = model.getWordUntilPosition(position);
      const wordRange = new monaco.Range(position.lineNumber, word.startColumn, position.lineNumber, word.endColumn);
      const suggestions: Monaco.languages.CompletionItem[] = [];

      const lineBefore = model.getLineContent(position.lineNumber).slice(0, word.startColumn - 1);
      if (!lineBefore.trimEnd().endsWith(".")) {
        const indent = model.getLineContent(position.lineNumber).match(/^\s*/)?.[0] ?? "";
        for (const t of LIVE_TEMPLATES) {
          if (!t.abbreviation.startsWith(word.word) || word.word.length === 0) continue;
          suggestions.push({
            label: { label: t.abbreviation, description: t.description },
            kind: monaco.languages.CompletionItemKind.Snippet,
            insertText: t.body.replace(/\n/g, `\n${indent}`),
            insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
            range: wordRange,
            sortText: "00" + t.abbreviation,
            detail: "Live template",
          });
        }
      }

      try {
        const response = await api.lang.completion(
          model.getValue(),
          kind,
          position.lineNumber,
          position.column,
          abortOn(token),
        );
        const range = toRange(monaco, response.replace);
        for (const item of response.items) {
          suggestions.push({
            label: {
              label: item.label,
              detail: item.signature ?? undefined,
              description: item.type ?? undefined,
            },
            kind: monaco.languages.CompletionItemKind[KIND_MAP[item.kind] ?? "Text"],
            insertText: item.insertText,
            insertTextRules: item.snippet ? monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet : undefined,
            range,
            sortText: item.sortText,
            filterText: item.label,
            detail: item.owner ? `${item.owner}` : undefined,
            additionalTextEdits: item.additionalTextEdits.map((e) => ({ range: toRange(monaco, e.range), text: e.text })),
            command: item.snippet ? { id: "editor.action.triggerParameterHints", title: "Parameter info" } : undefined,
          });
        }
        return { suggestions, incomplete: response.incomplete };
      } catch {
        return { suggestions };
      }
    },
  });

  monaco.languages.registerHoverProvider("java", {
    async provideHover(model, position, token) {
      try {
        const hover = await api.lang.hover(model.getValue(), fileKindOf(model), position.lineNumber, position.column, abortOn(token));
        if (!hover) return null;
        return { range: toRange(monaco, hover.range), contents: [{ value: hover.markdown }] };
      } catch {
        return null;
      }
    },
  });

  monaco.languages.registerSignatureHelpProvider("java", {
    signatureHelpTriggerCharacters: ["(", ","],
    signatureHelpRetriggerCharacters: [")"],
    async provideSignatureHelp(model, position, token) {
      try {
        const help = await api.lang.signature(model.getValue(), fileKindOf(model), position.lineNumber, position.column, abortOn(token));
        if (!help) return null;
        return {
          value: {
            signatures: help.signatures.map((s) => ({
              label: s.label,
              documentation: s.documentation ?? undefined,
              parameters: s.parameters.map((p) => ({ label: p })),
            })),
            activeSignature: help.activeSignature,
            activeParameter: help.activeParameter,
          },
          dispose() {},
        };
      } catch {
        return null;
      }
    },
  });
}

export function toMarkers(monaco: MonacoApi, diagnostics: Diagnostic[]): Monaco.editor.IMarkerData[] {
  return diagnostics.map((d) => ({
    severity:
      d.severity === "ERROR"
        ? monaco.MarkerSeverity.Error
        : d.severity === "WARNING"
          ? monaco.MarkerSeverity.Warning
          : monaco.MarkerSeverity.Info,
    message: d.message,
    code: d.code ?? undefined,
    source: d.code === "algopractice.unused" ? "inspection" : "javac",
    tags: d.code === "algopractice.unused" ? [monaco.MarkerTag.Unnecessary] : undefined,
    startLineNumber: d.startLine,
    startColumn: d.startColumn,
    endLineNumber: d.endLine,
    endColumn: d.endColumn,
  }));
}

/**
 * Re-checks the model on every edit (debounced) and shows javac findings as squiggles,
 * like IntelliJ's on-the-fly inspections. Returns a disposer.
 */
export function attachDiagnostics(
  monaco: MonacoApi,
  editor: Monaco.editor.IStandaloneCodeEditor,
  onUpdate?: (diagnostics: Diagnostic[]) => void,
  delayMs = 450,
) {
  let timer: ReturnType<typeof setTimeout> | undefined;
  let controller: AbortController | undefined;

  const check = async () => {
    const model = editor.getModel();
    if (!model) return;
    controller?.abort();
    controller = new AbortController();
    const version = model.getVersionId();
    try {
      const { diagnostics } = await api.lang.diagnostics(model.getValue(), fileKindOf(model), controller.signal);
      if (model.isDisposed() || model.getVersionId() !== version) return;
      monaco.editor.setModelMarkers(model, "javac", toMarkers(monaco, diagnostics));
      onUpdate?.(diagnostics);
    } catch {
      // backend unavailable or request superseded; keep the old markers
    }
  };

  const schedule = () => {
    clearTimeout(timer);
    timer = setTimeout(check, delayMs);
  };

  const subscriptions = [editor.onDidChangeModelContent(schedule), editor.onDidChangeModel(schedule)];
  schedule();
  return () => {
    clearTimeout(timer);
    controller?.abort();
    subscriptions.forEach((s) => s.dispose());
  };
}

/** IntelliJ keymap favourites on top of Monaco's defaults. */
export function installIntellijKeys(monaco: MonacoApi, editor: Monaco.editor.IStandaloneCodeEditor) {
  const { KeyMod, KeyCode } = monaco;
  const run = (id: string) => () => editor.trigger("keyboard", id, null);
  editor.addCommand(KeyMod.CtrlCmd | KeyCode.KeyD, run("editor.action.copyLinesDownAction"));
  editor.addCommand(KeyMod.CtrlCmd | KeyCode.KeyY, run("editor.action.deleteLines"));
  editor.addCommand(KeyMod.CtrlCmd | KeyCode.KeyP, run("editor.action.triggerParameterHints"));
  editor.addCommand(KeyMod.CtrlCmd | KeyCode.KeyW, run("editor.action.smartSelect.expand"));
  editor.addCommand(KeyMod.CtrlCmd | KeyMod.Shift | KeyCode.KeyW, run("editor.action.smartSelect.shrink"));
  editor.addCommand(KeyMod.Alt | KeyCode.Enter, run("editor.action.quickFix"));
  editor.addCommand(KeyMod.Shift | KeyCode.F6, run("editor.action.rename"));
  editor.addCommand(KeyMod.CtrlCmd | KeyMod.Shift | KeyCode.UpArrow, run("editor.action.moveLinesUpAction"));
  editor.addCommand(KeyMod.CtrlCmd | KeyMod.Shift | KeyCode.DownArrow, run("editor.action.moveLinesDownAction"));
  editor.addCommand(KeyMod.CtrlCmd | KeyCode.F1, run("editor.action.showHover"));
  editor.addCommand(KeyMod.Alt | KeyCode.F1, run("editor.action.marker.next"));
  editor.addCommand(KeyCode.F2, run("editor.action.marker.next"));
  editor.addCommand(KeyMod.Shift | KeyCode.F2, run("editor.action.marker.prev"));
}
