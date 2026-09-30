import * as monaco from "monaco-editor";
import EditorWorker from "monaco-editor/editor/editor.worker?worker";
import { loader } from "@monaco-editor/react";
import { defineEditorThemes } from "../theme";
import { registerJavaIntelligence } from "./javaLanguage";

declare global {
  interface Window {
    MonacoEnvironment?: monaco.Environment;
  }
}

let initialized = false;

/** Bundles Monaco locally (no CDN), installs the editor themes and the Java providers. */
export function setupMonaco() {
  if (initialized) return;
  initialized = true;
  window.MonacoEnvironment = {
    getWorker: () => new EditorWorker(),
  };
  loader.config({ monaco });

  defineEditorThemes(monaco);
  registerJavaIntelligence(monaco);
}
