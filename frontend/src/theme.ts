import { useSyncExternalStore } from "react";
import type * as Monaco from "monaco-editor";

export type ThemeId = "aurora" | "nord" | "paper" | "contrast";

interface EditorPalette {
  base: "vs" | "vs-dark" | "hc-black";
  bg: string;
  fg: string;
  keyword: string;
  string: string;
  number: string;
  comment: string;
  annotation: string;
  type: string;
  lineNumber: string;
  lineNumberActive: string;
  lineHighlight: string;
  selection: string;
  widget: string;
  widgetBorder: string;
  widgetSelected: string;
}

export interface ThemeInfo {
  id: ThemeId;
  label: string;
  description: string;
  /** Background, surface and accent, shown as the picker swatch. */
  swatch: [string, string, string];
  editor: EditorPalette;
}

export const THEMES: ThemeInfo[] = [
  {
    id: "aurora",
    label: "Aurora",
    description: "Deep ink with teal and violet",
    swatch: ["#0d1117", "#161c27", "#2dd4bf"],
    editor: {
      base: "vs-dark",
      bg: "#111722",
      fg: "#d4dbe8",
      keyword: "c792ea",
      string: "a5e3a0",
      number: "f7a072",
      comment: "5f6f89",
      annotation: "2dd4bf",
      type: "82c7ff",
      lineNumber: "#3a4558",
      lineNumberActive: "#9aa7bd",
      lineHighlight: "#18202d",
      selection: "#2a3b5a",
      widget: "#18202d",
      widgetBorder: "#2a3446",
      widgetSelected: "#21405a",
    },
  },
  {
    id: "nord",
    label: "Fjord",
    description: "Cool slate with frost blue",
    swatch: ["#2e3440", "#3b4252", "#88c0d0"],
    editor: {
      base: "vs-dark",
      bg: "#2e3440",
      fg: "#d8dee9",
      keyword: "81a1c1",
      string: "a3be8c",
      number: "b48ead",
      comment: "616e88",
      annotation: "d08770",
      type: "8fbcbb",
      lineNumber: "#4c566a",
      lineNumberActive: "#d8dee9",
      lineHighlight: "#353c4a",
      selection: "#434c5e",
      widget: "#3b4252",
      widgetBorder: "#4c566a",
      widgetSelected: "#4c566a",
    },
  },
  {
    id: "paper",
    label: "Paper",
    description: "Light, warm and calm",
    swatch: ["#f6f3ec", "#ffffff", "#3b5bdb"],
    editor: {
      base: "vs",
      bg: "#fffdf8",
      fg: "#2b2a33",
      keyword: "7c3aed",
      string: "2f7d4f",
      number: "b45309",
      comment: "8a8578",
      annotation: "0e7490",
      type: "1d4ed8",
      lineNumber: "#c4bfb2",
      lineNumberActive: "#5b574d",
      lineHighlight: "#f5f1e6",
      selection: "#d9e2ff",
      widget: "#ffffff",
      widgetBorder: "#e2ddd0",
      widgetSelected: "#e6ecff",
    },
  },
  {
    id: "contrast",
    label: "High contrast",
    description: "Black with bright yellow, easiest to read",
    swatch: ["#000000", "#0f0f0f", "#ffd60a"],
    editor: {
      base: "hc-black",
      bg: "#000000",
      fg: "#ffffff",
      keyword: "ffd60a",
      string: "7cff9b",
      number: "7cd4ff",
      comment: "b0b0b0",
      annotation: "ff9ef5",
      type: "ffffff",
      lineNumber: "#8a8a8a",
      lineNumberActive: "#ffffff",
      lineHighlight: "#1a1a1a",
      selection: "#264f78",
      widget: "#0f0f0f",
      widgetBorder: "#ffffff",
      widgetSelected: "#264f78",
    },
  },
];

const KEY = "algopractice.theme";
const listeners = new Set<() => void>();

function isTheme(value: string | null): value is ThemeId {
  return THEMES.some((t) => t.id === value);
}

function initial(): ThemeId {
  try {
    const stored = localStorage.getItem(KEY);
    if (isTheme(stored)) return stored;
  } catch {
    // storage unavailable
  }
  return window.matchMedia?.("(prefers-color-scheme: light)").matches ? "paper" : "aurora";
}

let current: ThemeId = initial();

/** Puts the theme on <html> so the CSS variables in styles.css switch over. Call once before rendering. */
export function applyTheme() {
  document.documentElement.dataset.theme = current;
  const meta = THEMES.find((t) => t.id === current)!;
  document.documentElement.style.colorScheme = meta.editor.base === "vs" ? "light" : "dark";
}

export function setTheme(id: ThemeId) {
  current = id;
  try {
    localStorage.setItem(KEY, id);
  } catch {
    // keep it for this tab only
  }
  applyTheme();
  listeners.forEach((l) => l());
}

export function useTheme(): ThemeId {
  return useSyncExternalStore(
    (l) => {
      listeners.add(l);
      return () => listeners.delete(l);
    },
    () => current,
  );
}

/** Reads a CSS custom property of the active theme, e.g. cssVar("--accent"). */
export function cssVar(name: string) {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim();
}

export function monacoThemeName(id: ThemeId) {
  return `algopractice-${id}`;
}

/** Registers one Monaco theme per app theme. */
export function defineEditorThemes(monaco: typeof Monaco) {
  for (const { id, editor: e } of THEMES) {
    monaco.editor.defineTheme(monacoThemeName(id), {
      base: e.base,
      inherit: true,
      rules: [
        { token: "keyword", foreground: e.keyword },
        { token: "string", foreground: e.string },
        { token: "number", foreground: e.number },
        { token: "comment", foreground: e.comment, fontStyle: "italic" },
        { token: "comment.doc", foreground: e.comment, fontStyle: "italic" },
        { token: "annotation", foreground: e.annotation },
        { token: "type", foreground: e.type },
        { token: "identifier", foreground: e.fg.slice(1) },
        { token: "delimiter", foreground: e.keyword },
        { token: "operator", foreground: e.fg.slice(1) },
      ],
      colors: {
        "editor.background": e.bg,
        "editor.foreground": e.fg,
        "editorLineNumber.foreground": e.lineNumber,
        "editorLineNumber.activeForeground": e.lineNumberActive,
        "editor.lineHighlightBackground": e.lineHighlight,
        "editor.selectionBackground": e.selection,
        "editorCursor.foreground": e.fg,
        "editorSuggestWidget.background": e.widget,
        "editorSuggestWidget.border": e.widgetBorder,
        "editorSuggestWidget.selectedBackground": e.widgetSelected,
        "editorHoverWidget.background": e.widget,
        "editorHoverWidget.border": e.widgetBorder,
        "editorIndentGuide.background1": e.lineNumber,
        "editorGutter.background": e.bg,
        "editorStickyScroll.background": e.bg,
      },
    });
  }
}
