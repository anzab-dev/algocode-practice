export function formatMs(ms: number | null | undefined): string {
  if (ms == null) return "–";
  if (ms < 1) return `${ms.toFixed(3)} ms`;
  if (ms < 100) return `${ms.toFixed(2)} ms`;
  return `${Math.round(ms)} ms`;
}

export function formatBytes(bytes: number | null | undefined): string {
  if (bytes == null) return "–";
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
}

const VERDICT_LABELS: Record<string, string> = {
  ACCEPTED: "Accepted",
  WRONG_ANSWER: "Wrong Answer",
  TIME_LIMIT_EXCEEDED: "Time Limit Exceeded",
  MEMORY_LIMIT_EXCEEDED: "Memory Limit Exceeded",
  RUNTIME_ERROR: "Runtime Error",
  COMPILE_ERROR: "Compile Error",
  INTERNAL_ERROR: "Internal Error",
  COMPLETED: "Finished",
  EXITED: "Exited",
  SETUP_ERROR: "Cannot Run",
};

export function verdictLabel(verdict: string): string {
  return VERDICT_LABELS[verdict] ?? verdict;
}

export function relativeTime(iso: string, now = Date.now()): string {
  const seconds = Math.round((now - new Date(iso).getTime()) / 1000);
  if (seconds < 60) return "just now";
  const minutes = Math.round(seconds / 60);
  if (minutes < 60) return `${minutes} min ago`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours} h ago`;
  const days = Math.round(hours / 24);
  return `${days} d ago`;
}

/** Turns "[[2,7,11,15],9]" into one "name = value" line per parameter. */
export function describeArgs(input: string, names: string[]): string {
  try {
    const values = JSON.parse(input);
    if (Array.isArray(values) && values.length === names.length) {
      return values.map((v, i) => `${names[i]} = ${JSON.stringify(v)}`).join("\n");
    }
  } catch {
    // truncated preview: show as is
  }
  return input;
}

/** Parses the "name = value" lines back into an argument array; throws with a readable message. */
export function parseArgs(text: string, names: string[]): unknown[] {
  const lines = text
    .split("\n")
    .map((l) => l.trim())
    .filter(Boolean);
  if (lines.length !== names.length) {
    throw new Error(`Expected ${names.length} line(s): ${names.map((n) => `${n} = ...`).join(", ")}`);
  }
  return lines.map((line, i) => {
    const eq = line.indexOf("=");
    const raw = eq >= 0 && line.slice(0, eq).trim() === names[i] ? line.slice(eq + 1) : line;
    try {
      return JSON.parse(raw);
    } catch {
      throw new Error(`Line ${i + 1} (${names[i]}) is not valid JSON`);
    }
  });
}
