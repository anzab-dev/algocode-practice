import Editor, { type OnMount } from "@monaco-editor/react";
import type * as Monaco from "monaco-editor";
import { useEffect, useRef } from "react";
import type { Diagnostic } from "../api";
import { monacoThemeName, useTheme } from "../theme";
import { attachDiagnostics, installIntellijKeys } from "./javaLanguage";
import { setupMonaco } from "./monacoSetup";

setupMonaco();

export interface EditorShortcut {
  keys: number;
  run: () => void;
}

interface Props {
  /** Model path; start it with /scratch/ for scratchpad programs, /solution/ for problem solutions. */
  path: string;
  value: string;
  onChange: (value: string) => void;
  onDiagnostics?: (diagnostics: Diagnostic[]) => void;
  shortcuts?: (monaco: typeof Monaco) => EditorShortcut[];
  onReady?: (editor: Monaco.editor.IStandaloneCodeEditor, monaco: typeof Monaco) => void;
}

/** Monaco configured as a Java editor with live diagnostics, completion and IntelliJ key bindings. */
export function CodeEditor({ path, value, onChange, onDiagnostics, shortcuts, onReady }: Props) {
  const theme = useTheme();
  const cleanup = useRef<(() => void) | null>(null);
  const diagnosticsCallback = useRef(onDiagnostics);
  const shortcutsRef = useRef(shortcuts);
  diagnosticsCallback.current = onDiagnostics;
  shortcutsRef.current = shortcuts;

  useEffect(() => () => cleanup.current?.(), []);

  const handleMount: OnMount = (editor, monaco) => {
    installIntellijKeys(monaco, editor);
    for (const shortcut of shortcutsRef.current?.(monaco) ?? []) {
      editor.addCommand(shortcut.keys, () => {
        // Look the handler up at press time so it sees the latest state.
        const current = shortcutsRef.current?.(monaco).find((s) => s.keys === shortcut.keys);
        (current ?? shortcut).run();
      });
    }
    cleanup.current = attachDiagnostics(monaco, editor, (d) => diagnosticsCallback.current?.(d));
    onReady?.(editor, monaco);
    editor.focus();
  };

  return (
    <Editor
      path={path}
      language="java"
      theme={monacoThemeName(theme)}
      value={value}
      onChange={(v) => onChange(v ?? "")}
      onMount={handleMount}
      options={{
        fontFamily: "'JetBrains Mono', 'Fira Code', Menlo, Consolas, monospace",
        fontLigatures: true,
        fontSize: 14,
        lineHeight: 21,
        minimap: { enabled: false },
        tabSize: 4,
        insertSpaces: true,
        scrollBeyondLastLine: false,
        automaticLayout: true,
        renderLineHighlight: "all",
        bracketPairColorization: { enabled: true },
        guides: { bracketPairs: "active", indentation: true },
        suggest: { showStatusBar: true, preview: true, localityBonus: true, snippetsPreventQuickSuggestions: false },
        quickSuggestions: { other: true, comments: false, strings: false },
        quickSuggestionsDelay: 120,
        parameterHints: { enabled: true, cycle: true },
        hover: { delay: 400 },
        smoothScrolling: true,
        cursorBlinking: "smooth",
        stickyScroll: { enabled: true },
        padding: { top: 10 },
      }}
    />
  );
}
