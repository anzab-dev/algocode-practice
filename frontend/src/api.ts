import { getHandle } from "./user";

export type Difficulty = "EASY" | "MEDIUM" | "HARD";
export type Verdict =
  | "ACCEPTED"
  | "WRONG_ANSWER"
  | "TIME_LIMIT_EXCEEDED"
  | "MEMORY_LIMIT_EXCEEDED"
  | "RUNTIME_ERROR"
  | "COMPILE_ERROR"
  | "INTERNAL_ERROR";
export type FileKind = "SOLUTION" | "SCRATCH";

export interface Diagnostic {
  severity: "ERROR" | "WARNING" | "INFO";
  message: string;
  code: string | null;
  startLine: number;
  startColumn: number;
  endLine: number;
  endColumn: number;
}

export interface ProblemSummary {
  id: number;
  slug: string;
  title: string;
  difficulty: Difficulty;
  tags: string[];
  acceptanceRate: number;
  submissions: number;
  status: "SOLVED" | "ATTEMPTED" | null;
}

export interface ProblemDetail {
  id: number;
  slug: string;
  title: string;
  difficulty: Difficulty;
  tags: string[];
  description: string;
  starterCode: string;
  method: string;
  params: { name: string; type: string }[];
  returnType: string;
  compareMode: string;
  timeLimitMs: number;
  hints: string[];
  samples: { input: string; expected: string }[];
  previousSlug: string | null;
  nextSlug: string | null;
}

export interface CaseResult {
  index: number;
  custom: boolean;
  input: string;
  expected: string | null;
  output: string | null;
  passed: boolean;
  stdout: string | null;
  timeMs: number | null;
  error: string | null;
}

export interface RunResponse {
  verdict: Verdict;
  message: string | null;
  diagnostics: Diagnostic[];
  cases: CaseResult[];
  runtimeMs: number;
  memoryBytes: number;
}

export interface Achievement {
  id: string;
  title: string;
  description: string;
  icon: string;
}

export interface SubmitResponse {
  submissionId: number;
  verdict: Verdict;
  message: string | null;
  diagnostics: Diagnostic[];
  passed: number;
  total: number;
  runtimeMs: number | null;
  memoryBytes: number | null;
  allocatedBytes: number | null;
  runtimeBeats: number | null;
  memoryBeats: number | null;
  failedCase: CaseResult | null;
  xpGained: number;
  level: number;
  streak: number;
  newAchievements: Achievement[];
}

export interface SubmissionSummary {
  id: number;
  problemSlug: string;
  problemTitle: string;
  verdict: Verdict;
  passed: number;
  total: number;
  runtimeMs: number | null;
  memoryBytes: number | null;
  runtimeBeats: number | null;
  memoryBeats: number | null;
  createdAt: string;
}

export interface SubmissionDetail {
  summary: SubmissionSummary;
  code: string;
  message: string | null;
  failedTestJson: string | null;
}

export interface Distribution {
  buckets: { from: number; to: number; count: number }[];
  mine: number | null;
  count: number;
}

export interface Stats {
  runtimeMs: Distribution;
  memoryBytes: Distribution;
}

export interface Profile {
  handle: string;
  xp: number;
  level: number;
  levelStartXp: number;
  nextLevelXp: number;
  currentStreak: number;
  longestStreak: number;
  solved: number;
  totalProblems: number;
  byDifficulty: Record<Difficulty, { solved: number; total: number }>;
  submissions: number;
  acceptanceRate: number;
  achievements: { achievement: Achievement; unlocked: boolean }[];
  activity: Record<string, number>;
}

export interface Me {
  profile: Profile;
  recent: SubmissionSummary[];
}

export interface LeaderboardEntry {
  handle: string;
  xp: number;
  level: number;
  solved: number;
  streak: number;
}

export interface ScratchReport {
  status: "COMPLETED" | "COMPILE_ERROR" | "TIME_LIMIT_EXCEEDED" | "MEMORY_LIMIT_EXCEEDED" | "EXITED" | "SETUP_ERROR" | "INTERNAL_ERROR";
  message: string | null;
  diagnostics: Diagnostic[];
  stdout: string;
  stderr: string;
  stdoutTruncated: boolean;
  exception: { type: string; message: string | null; trace: string } | null;
  timeMs: number;
  peakHeapBytes: number;
  allocatedBytes: number;
  mainClass: string | null;
}

export interface Scratchpad {
  id: number;
  title: string;
  code: string;
  stdin: string;
  updatedAt: string;
}

export interface Range {
  startLine: number;
  startColumn: number;
  endLine: number;
  endColumn: number;
}

export interface CompletionItem {
  label: string;
  kind: string;
  signature: string | null;
  type: string | null;
  owner: string | null;
  insertText: string;
  snippet: boolean;
  sortText: string;
  additionalTextEdits: { range: Range; text: string }[];
}

export interface CompletionResponse {
  items: CompletionItem[];
  replace: Range;
  incomplete: boolean;
}

export interface HoverResponse {
  markdown: string;
  range: Range;
}

export interface SignatureHelpResponse {
  signatures: { label: string; parameters: string[]; documentation: string | null }[];
  activeSignature: number;
  activeParameter: number;
}

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
  ) {
    super(message);
  }
}

async function request<T>(method: string, path: string, body?: unknown, signal?: AbortSignal): Promise<T> {
  const response = await fetch(`/api${path}`, {
    method,
    signal,
    headers: {
      "Content-Type": "application/json",
      "X-AlgoPractice-User": getHandle(),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  if (response.status === 204) {
    return null as T;
  }
  if (!response.ok) {
    let detail = response.statusText;
    try {
      const problem = await response.json();
      detail = problem.detail ?? problem.message ?? detail;
    } catch {
      // not JSON
    }
    throw new ApiError(detail, response.status);
  }
  return (await response.json()) as T;
}

export const api = {
  problems: () => request<ProblemSummary[]>("GET", "/problems"),
  daily: () => request<ProblemSummary>("GET", "/problems/daily"),
  problem: (slug: string) => request<ProblemDetail>("GET", `/problems/${slug}`),
  run: (slug: string, code: string, customInputs: unknown[]) =>
    request<RunResponse>("POST", `/problems/${slug}/run`, { code, customInputs }),
  submit: (slug: string, code: string) => request<SubmitResponse>("POST", `/problems/${slug}/submit`, { code }),
  submissions: (slug: string) => request<SubmissionSummary[]>("GET", `/problems/${slug}/submissions`),
  submission: (id: number) => request<SubmissionDetail>("GET", `/submissions/${id}`),
  stats: (slug: string) => request<Stats>("GET", `/problems/${slug}/stats`),
  me: () => request<Me>("GET", "/me"),
  leaderboard: () => request<LeaderboardEntry[]>("GET", "/leaderboard"),
  scratchpads: () => request<Scratchpad[]>("GET", "/scratchpads"),
  createScratchpad: (body: Omit<Scratchpad, "id" | "updatedAt">) => request<Scratchpad>("POST", "/scratchpads", body),
  updateScratchpad: (id: number, body: Omit<Scratchpad, "id" | "updatedAt">) =>
    request<Scratchpad>("PUT", `/scratchpads/${id}`, body),
  deleteScratchpad: (id: number) => request<void>("DELETE", `/scratchpads/${id}`),
  runScratch: (code: string, stdin: string) => request<ScratchReport>("POST", "/scratchpads/run", { code, stdin }),
  lang: {
    diagnostics: (code: string, kind: FileKind, signal?: AbortSignal) =>
      request<{ diagnostics: Diagnostic[]; durationMs: number }>("POST", "/lang/diagnostics", { code, kind }, signal),
    completion: (code: string, kind: FileKind, line: number, column: number, signal?: AbortSignal) =>
      request<CompletionResponse>("POST", "/lang/completion", { code, kind, line, column }, signal),
    hover: (code: string, kind: FileKind, line: number, column: number, signal?: AbortSignal) =>
      request<HoverResponse | null>("POST", "/lang/hover", { code, kind, line, column }, signal),
    signature: (code: string, kind: FileKind, line: number, column: number, signal?: AbortSignal) =>
      request<SignatureHelpResponse | null>("POST", "/lang/signature", { code, kind, line, column }, signal),
  },
};
