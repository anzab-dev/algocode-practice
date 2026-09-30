import * as monaco from "monaco-editor";
import EditorWorker from "monaco-editor/editor/editor.worker?worker";
import { loader } from "@monaco-editor/react";
import { registerJavaIntelligence } from "./javaLanguage";

declare global {
  interface Window {
    MonacoEnvironment?: monaco.Environment;
  }
}

let initialized = false;

/** Bundles Monaco locally (no CDN), installs the IntelliJ-like theme and the Java providers. */
export function setupMonaco() {
  if (initialized) return;
  initialized = true;
  window.MonacoEnvironment = {
    getWorker: () => new EditorWorker(),
  };
  loader.config({ monaco });

  monaco.editor.defineTheme("darcula", {
    base: "vs-dark",
    inherit: true,
    rules: [
      { token: "keyword", foreground: "CC7832" },
      { token: "string", foreground: "6A8759" },
      { token: "number", foreground: "6897BB" },
      { token: "comment", foreground: "808080", fontStyle: "italic" },
      { token: "comment.doc", foreground: "629755", fontStyle: "italic" },
      { token: "annotation", foreground: "BBB529" },
      { token: "type", foreground: "A9B7C6" },
      { token: "identifier", foreground: "A9B7C6" },
      { token: "delimiter", foreground: "CC7832" },
      { token: "operator", foreground: "A9B7C6" },
    ],
    colors: {
      "editor.background": "#1E1F22",
      "editor.foreground": "#A9B7C6",
      "editorLineNumber.foreground": "#4B5059",
      "editorLineNumber.activeForeground": "#A1A3AB",
      "editor.lineHighlightBackground": "#26282E",
      "editor.selectionBackground": "#214283",
      "editorCursor.foreground": "#CED0D6",
      "editorError.foreground": "#F75464",
      "editorWarning.foreground": "#E0B64C",
      "editorUnnecessaryCode.opacity": "#00000088",
      "editorSuggestWidget.background": "#2B2D30",
      "editorSuggestWidget.border": "#43454A",
      "editorSuggestWidget.selectedBackground": "#2E436E",
      "editorHoverWidget.background": "#2B2D30",
      "editorHoverWidget.border": "#43454A",
      "editorIndentGuide.background1": "#393B40",
      "editorGutter.background": "#1E1F22",
    },
  });

  registerJavaIntelligence(monaco);
}
