import { useCallback, useEffect, useRef, useState } from "react";
import Markdown from "react-markdown";
import { Link, useParams } from "react-router-dom";
import {
  api,
  type CaseResult,
  type Diagnostic,
  type ProblemDetail,
  type RunResponse,
  type Stats,
  type SubmissionDetail,
  type SubmissionSummary,
  type SubmitResponse,
} from "../api";
import { Confetti } from "../components/Confetti";
import { DiagnosticsStatus } from "../components/DiagnosticsStatus";
import { Difficulty } from "../components/Difficulty";
import { Histogram } from "../components/Histogram";
import { Icon } from "../components/Icon";
import { announceAchievements, notifyProgressChanged } from "../components/progress";
import { SCRATCH_TEMPLATE, ScratchWorkbench } from "../components/ScratchWorkbench";
import { Split } from "../components/Split";
import { CodeEditor } from "../editor/CodeEditor";
import { describeArgs, formatBytes, formatMs, parseArgs, relativeTime, verdictLabel } from "../format";
import { drafts } from "../user";

/** Stored with the drafts: whether the problem pane sits left of the editor. */
const SWAP_KEY = "layout.swapped";

type LeftTab = "description" | "submissions" | "result";
type ConsoleTab = "testcase" | "result";

function verdictTone(verdict: string) {
  if (verdict === "ACCEPTED") return "good";
  if (verdict === "TIME_LIMIT_EXCEEDED" || verdict === "MEMORY_LIMIT_EXCEEDED") return "warn";
  return "bad";
}

export function ProblemPage() {
  const { slug = "" } = useParams();
  const [problem, setProblem] = useState<ProblemDetail | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [code, setCode] = useState("");
  const [mode, setMode] = useState<"solution" | "scratch">("solution");
  const [scratchCode, setScratchCode] = useState("");
  const [scratchStdin, setScratchStdin] = useState("");
  const [leftTab, setLeftTab] = useState<LeftTab>("description");
  const [consoleTab, setConsoleTab] = useState<ConsoleTab>("testcase");
  const [customCases, setCustomCases] = useState<string[]>([]);
  const [activeCase, setActiveCase] = useState(0);
  const [running, setRunning] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [runResult, setRunResult] = useState<RunResponse | null>(null);
  const [submitResult, setSubmitResult] = useState<SubmitResponse | null>(null);
  const [stats, setStats] = useState<Stats | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [diagnostics, setDiagnostics] = useState<Diagnostic[]>([]);
  const [burst, setBurst] = useState(0);
  const [swapped, setSwapped] = useState(() => drafts.get(SWAP_KEY) === "1");
  const busy = running || submitting;

  useEffect(() => {
    setProblem(null);
    setRunResult(null);
    setSubmitResult(null);
    setStats(null);
    setLeftTab("description");
    setConsoleTab("testcase");
    setCustomCases([]);
    setActiveCase(0);
    api
      .problem(slug)
      .then((p) => {
        setProblem(p);
        setCode(drafts.get(`solution.${slug}`) ?? p.starterCode);
        setScratchCode(drafts.get(`scratch.${slug}`) ?? SCRATCH_TEMPLATE);
        setScratchStdin(drafts.get(`scratch-stdin.${slug}`) ?? "");
      })
      .catch((e: Error) => setLoadError(e.message));
  }, [slug]);

  const toggleSwap = () => {
    drafts.set(SWAP_KEY, swapped ? "0" : "1");
    setSwapped(!swapped);
  };

  const updateCode = (value: string) => {
    setCode(value);
    drafts.set(`solution.${slug}`, value);
  };

  const paramNames = problem?.params.map((p) => p.name) ?? [];
  const samples = problem?.samples ?? [];

  const run = useCallback(async () => {
    if (!problem || busy) return;
    let customInputs: unknown[] = [];
    try {
      customInputs = customCases.map((c) => parseArgs(c, problem.params.map((p) => p.name)));
    } catch (e) {
      setActionError(`Custom case: ${(e as Error).message}`);
      setConsoleTab("testcase");
      return;
    }
    setRunning(true);
    setActionError(null);
    setConsoleTab("result");
    try {
      const result = await api.run(problem.slug, code, customInputs);
      setRunResult(result);
      const firstFail = result.cases.findIndex((c) => !c.passed);
      setActiveCase(firstFail >= 0 ? firstFail : 0);
    } catch (e) {
      setActionError((e as Error).message);
    } finally {
      setRunning(false);
    }
  }, [problem, busy, customCases, code]);

  const submit = useCallback(async () => {
    if (!problem || busy) return;
    setSubmitting(true);
    setActionError(null);
    setLeftTab("result");
    try {
      const result = await api.submit(problem.slug, code);
      setSubmitResult(result);
      if (result.verdict === "ACCEPTED") {
        setBurst((b) => b + 1);
        api.stats(problem.slug).then(setStats).catch(() => setStats(null));
      }
      announceAchievements(result.newAchievements);
      notifyProgressChanged();
    } catch (e) {
      setActionError((e as Error).message);
    } finally {
      setSubmitting(false);
    }
  }, [problem, busy, code]);

  if (loadError) {
    return (
      <div className="container">
        <div className="empty error-text">{loadError}</div>
      </div>
    );
  }
  if (!problem) {
    return <div className="empty">Loading…</div>;
  }

  const left = (
    <div className="pane" style={{ flex: 1 }}>
      <div className="tabs">
        <button className={leftTab === "description" ? "active" : ""} onClick={() => setLeftTab("description")}>
          Problem
        </button>
        <button className={leftTab === "submissions" ? "active" : ""} onClick={() => setLeftTab("submissions")}>
          Attempts
        </button>
        {(submitResult || submitting) && (
          <button className={leftTab === "result" ? "active" : ""} onClick={() => setLeftTab("result")}>
            Verdict
          </button>
        )}
        <span className="spacer" />
        <button className="ghost" title="Swap the editor and problem sides" onClick={toggleSwap}>
          <Icon name="swap" size={16} />
        </button>
        {problem.previousSlug && (
          <Link to={`/problems/${problem.previousSlug}`} title="Previous problem">
            <button className="ghost">‹</button>
          </Link>
        )}
        {problem.nextSlug && (
          <Link to={`/problems/${problem.nextSlug}`} title="Next problem">
            <button className="ghost">›</button>
          </Link>
        )}
      </div>
      <div className="pane-body">
        {leftTab === "description" && <Description problem={problem} />}
        {leftTab === "submissions" && (
          <Submissions slug={problem.slug} onRestore={(c) => (updateCode(c), setMode("solution"))} />
        )}
        {leftTab === "result" && (
          <SubmitResult result={submitResult} submitting={submitting} stats={stats} error={actionError} />
        )}
      </div>
    </div>
  );

  const solutionEditor = (
    <div className="pane" style={{ flex: 1 }}>
      <div className="editor-toolbar">
        <ModeSwitch mode={mode} onChange={setMode} />
        <span className="lang-chip">Java 21</span>
        <button
          className="ghost"
          title="Reset to the starter code"
          onClick={() => {
            if (confirm("Replace your code with the starter code?")) updateCode(problem.starterCode);
          }}
        >
          ↺ Reset
        </button>
        <span className="spacer" />
        <button className="outline" onClick={run} disabled={busy} title="Run sample and custom inputs (Ctrl+')">
          {running ? <span className="spinner" /> : "▷"} Try
        </button>
        <button className="accent" onClick={submit} disabled={busy} title="Judge against every hidden test (Ctrl+Enter)">
          {submitting ? <span className="spinner" /> : "✓"} Submit
        </button>
      </div>
      <div className="editor-host">
        <CodeEditor
          path={`/solution/${problem.slug}/Solution.java`}
          value={code}
          onChange={updateCode}
          onDiagnostics={setDiagnostics}
          shortcuts={(monaco) => [
            { keys: monaco.KeyMod.CtrlCmd | monaco.KeyCode.Quote, run },
            { keys: monaco.KeyMod.CtrlCmd | monaco.KeyCode.Enter, run: submit },
          ]}
        />
      </div>
      <DiagnosticsStatus diagnostics={diagnostics} extra={`time limit ${problem.timeLimitMs} ms`} />
    </div>
  );

  const consolePane = (
    <div className="pane console" style={{ flex: 1 }}>
      <div className="tabs">
        <button className={consoleTab === "testcase" ? "active" : ""} onClick={() => setConsoleTab("testcase")}>
          Inputs
        </button>
        <button className={consoleTab === "result" ? "active" : ""} onClick={() => setConsoleTab("result")}>
          Output
        </button>
        <span className="spacer" />
        <span className="small muted console-hint">
          <kbd>Ctrl</kbd>+<kbd>'</kbd> try · <kbd>Ctrl</kbd>+<kbd>Enter</kbd> submit
        </span>
      </div>
      <div className="pane-body">
        {actionError && consoleTab === "testcase" && <div className="error-text" style={{ marginBottom: 8 }}>{actionError}</div>}
        {consoleTab === "testcase" && (
          <TestcaseEditor
            samples={samples.map((s) => describeArgs(s.input, paramNames))}
            custom={customCases}
            onCustom={setCustomCases}
            active={activeCase}
            onActive={setActiveCase}
          />
        )}
        {consoleTab === "result" && (
          <RunResultView
            result={runResult}
            running={running}
            error={actionError}
            paramNames={paramNames}
            active={activeCase}
            onActive={setActiveCase}
          />
        )}
      </div>
    </div>
  );

  const editorSide =
    mode === "solution" ? (
      <Split direction="vertical" initial={64} first={solutionEditor} second={consolePane} />
    ) : (
      <ScratchWorkbench
        path={`/scratch/problem/${problem.slug}/Main.java`}
        code={scratchCode}
        stdin={scratchStdin}
        onCode={(c) => (setScratchCode(c), drafts.set(`scratch.${slug}`, c))}
        onStdin={(s) => (setScratchStdin(s), drafts.set(`scratch-stdin.${slug}`, s))}
        toolbar={<ModeSwitch mode={mode} onChange={setMode} />}
      />
    );

  return (
    <div className={`workspace ${swapped ? "swapped" : ""}`}>
      {swapped ? (
        <Split key="info-first" initial={42} first={left} second={editorSide} />
      ) : (
        <Split key="editor-first" initial={58} first={editorSide} second={left} />
      )}
      <Confetti burst={burst} />
    </div>
  );
}

function ModeSwitch({ mode, onChange }: { mode: "solution" | "scratch"; onChange: (m: "solution" | "scratch") => void }) {
  return (
    <div className="segmented" role="tablist">
      <button className={mode === "solution" ? "active" : ""} onClick={() => onChange("solution")}>
        Solution
      </button>
      <button
        className={mode === "scratch" ? "active" : ""}
        onClick={() => onChange("scratch")}
        title="A free-form Java program for trying ideas on this problem"
      >
        Scratchpad
      </button>
    </div>
  );
}

function Description({ problem }: { problem: ProblemDetail }) {
  return (
    <div className="description">
      <h1>{problem.title}</h1>
      <div className="row" style={{ marginBottom: 12, flexWrap: "wrap" }}>
        <Difficulty value={problem.difficulty} />
        {problem.tags.map((t) => (
          <span key={t} className="tag">
            {t}
          </span>
        ))}
      </div>
      <Markdown>{problem.description}</Markdown>
      {problem.hints.map((hint, i) => (
        <details key={i} className="hint">
          <summary>Hint {i + 1}</summary>
          <p>{hint}</p>
        </details>
      ))}
    </div>
  );
}

function TestcaseEditor({
  samples,
  custom,
  onCustom,
  active,
  onActive,
}: {
  samples: string[];
  custom: string[];
  onCustom: (c: string[]) => void;
  active: number;
  onActive: (i: number) => void;
}) {
  const all = [...samples, ...custom];
  const index = Math.min(active, all.length - 1);
  const isCustom = index >= samples.length;
  return (
    <>
      <div className="case-chips">
        {all.map((_, i) => (
          <button key={i} className={`case-chip ${i === index ? "active" : ""}`} onClick={() => onActive(i)}>
            {i < samples.length ? `Sample ${i + 1}` : `Custom ${i - samples.length + 1}`}
          </button>
        ))}
        {custom.length < 5 && (
          <button
            className="case-chip"
            title="Add a custom input; the expected answer comes from the reference solution"
            onClick={() => {
              onCustom([...custom, samples[index] ?? samples[0] ?? ""]);
              onActive(all.length);
            }}
          >
            +
          </button>
        )}
      </div>
      {isCustom ? (
        <>
          <textarea
            style={{ width: "100%", minHeight: 90 }}
            value={custom[index - samples.length]}
            spellCheck={false}
            onChange={(e) => onCustom(custom.map((c, i) => (i === index - samples.length ? e.target.value : c)))}
          />
          <div className="row small muted">
            One <code>name = JSON value</code> per line.
            <span className="spacer" />
            <button
              className="ghost"
              onClick={() => {
                onCustom(custom.filter((_, i) => i !== index - samples.length));
                onActive(Math.max(0, index - 1));
              }}
            >
              Remove case
            </button>
          </div>
        </>
      ) : (
        <div className="value-box">{all[index]}</div>
      )}
    </>
  );
}

function RunResultView({
  result,
  running,
  error,
  paramNames,
  active,
  onActive,
}: {
  result: RunResponse | null;
  running: boolean;
  error: string | null;
  paramNames: string[];
  active: number;
  onActive: (i: number) => void;
}) {
  if (running) return <div className="muted row"><span className="spinner" /> Compiling and running…</div>;
  if (error) return <div className="error-text">{error}</div>;
  if (!result) return <div className="muted">Run your code to see results here.</div>;
  if (result.verdict === "COMPILE_ERROR") {
    return (
      <>
        <div className="verdict bad" style={{ fontSize: 18, marginBottom: 8 }}>
          Compile Error
        </div>
        <pre className="trace">
          {result.diagnostics
            .filter((d) => d.severity === "ERROR")
            .map((d) => `Line ${d.startLine}:${d.startColumn}  ${d.message}`)
            .join("\n")}
        </pre>
      </>
    );
  }
  const c: CaseResult | undefined = result.cases[Math.min(active, result.cases.length - 1)];
  const firstCustom = result.cases.findIndex((rc) => rc.custom);
  return (
    <>
      <div className="row" style={{ marginBottom: 10 }}>
        <span className={`verdict ${verdictTone(result.verdict)}`} style={{ fontSize: 18 }}>
          {verdictLabel(result.verdict)}
        </span>
        <span className="muted small">
          Runtime {formatMs(result.runtimeMs)} · peak heap {formatBytes(result.memoryBytes)}
        </span>
      </div>
      {result.message && <div className="error-text" style={{ marginBottom: 8 }}>{result.message}</div>}
      <div className="case-chips">
        {result.cases.map((rc, i) => (
          <button key={i} className={`case-chip ${i === active ? "active" : ""}`} onClick={() => onActive(i)}>
            <span className={`dot ${rc.passed ? "pass" : "fail"}`} />
            {rc.custom ? `Custom ${i - firstCustom + 1}` : `Sample ${i + 1}`}
          </button>
        ))}
      </div>
      {c && <CaseView c={c} paramNames={paramNames} />}
    </>
  );
}

function CaseView({ c, paramNames }: { c: CaseResult; paramNames: string[] }) {
  return (
    <>
      <div className="field-label">Input</div>
      <div className="value-box">{describeArgs(c.input, paramNames)}</div>
      {c.error ? (
        <>
          <div className="field-label">Exception</div>
          <pre className="trace">{c.error}</pre>
        </>
      ) : (
        <>
          <div className="field-label">Output {c.timeMs != null && <span>· {formatMs(c.timeMs)}</span>}</div>
          <div className={`value-box ${c.passed ? "" : "bad"}`}>{c.output ?? "—"}</div>
        </>
      )}
      <div className="field-label">Expected</div>
      <div className="value-box">{c.expected ?? "(the reference solution could not run this input)"}</div>
      {c.stdout && (
        <>
          <div className="field-label">Stdout</div>
          <div className="value-box">{c.stdout}</div>
        </>
      )}
    </>
  );
}

function SubmitResult({
  result,
  submitting,
  stats,
  error,
}: {
  result: SubmitResponse | null;
  submitting: boolean;
  stats: Stats | null;
  error: string | null;
}) {
  if (submitting) return <div className="muted row"><span className="spinner" /> Judging against all tests…</div>;
  if (error) return <div className="error-text">{error}</div>;
  if (!result) return null;
  const accepted = result.verdict === "ACCEPTED";
  return (
    <>
      <div className="result-head">
        <span className={`verdict ${verdictTone(result.verdict)}`}>{verdictLabel(result.verdict)}</span>
        <span className="muted">
          {result.passed} / {result.total} tests passed
        </span>
      </div>
      {(result.xpGained > 0 || result.newAchievements.length > 0) && (
        <div className="reward">
          {result.xpGained > 0 && <span className="xp">+{result.xpGained} XP</span>}
          <span className="muted small">
            Level {result.level} · 🔥 {result.streak}-day streak
          </span>
          <span className="spacer" />
          {result.newAchievements.map((a) => (
            <span key={a.id} title={a.description}>
              {a.icon} {a.title}
            </span>
          ))}
        </div>
      )}
      {accepted && (
        <div className="metric-cards">
          <Metric label="Runtime" value={formatMs(result.runtimeMs)} beats={result.runtimeBeats} verb="Faster" />
          <Metric label="Peak memory" value={formatBytes(result.memoryBytes)} beats={result.memoryBeats} verb="Leaner" />
        </div>
      )}
      {accepted && result.allocatedBytes != null && (
        <div className="small muted" style={{ marginBottom: 16 }}>
          Your solution allocated {formatBytes(result.allocatedBytes)} in total across all tests.
        </div>
      )}
      {accepted && stats && (
        <div className="grid" style={{ marginBottom: 12 }}>
          <div>
            <div className="field-label">Runtime distribution</div>
            <Histogram distribution={stats.runtimeMs} format={formatMs} label="runtime" />
          </div>
          <div>
            <div className="field-label">Memory distribution</div>
            <Histogram distribution={stats.memoryBytes} format={formatBytes} label="memory" />
          </div>
        </div>
      )}
      {result.message && !accepted && <div className="error-text" style={{ marginBottom: 8 }}>{result.message}</div>}
      {result.verdict === "COMPILE_ERROR" && (
        <pre className="trace">
          {result.diagnostics
            .filter((d) => d.severity === "ERROR")
            .map((d) => `Line ${d.startLine}:${d.startColumn}  ${d.message}`)
            .join("\n")}
        </pre>
      )}
      {result.failedCase && <CaseView c={result.failedCase} paramNames={[]} />}
    </>
  );
}

function Metric({ label, value, beats, verb }: { label: string; value: string; beats: number | null; verb: string }) {
  return (
    <div className="metric">
      <div className="label">{label}</div>
      <div className="value">{value}</div>
      {beats == null ? (
        <div className="beats">First accepted solution here, so you set the bar.</div>
      ) : (
        <>
          <div className="percentile" aria-hidden="true">
            <span style={{ width: `${beats}%` }} />
          </div>
          <div className="beats">
            {verb} than <strong>{beats.toFixed(1)}%</strong> of accepted runs
          </div>
        </>
      )}
    </div>
  );
}

function Submissions({ slug, onRestore }: { slug: string; onRestore: (code: string) => void }) {
  const [items, setItems] = useState<SubmissionSummary[] | null>(null);
  const [selected, setSelected] = useState<SubmissionDetail | null>(null);
  const mounted = useRef(true);
  useEffect(() => {
    mounted.current = true;
    api.submissions(slug).then((s) => mounted.current && setItems(s));
    return () => {
      mounted.current = false;
    };
  }, [slug]);

  if (!items) return <div className="muted">Loading…</div>;
  if (items.length === 0) return <div className="empty">No submissions yet. Submit with Ctrl+Enter.</div>;
  return (
    <>
      <div className="list">
        {items.map((s) => (
          <div
            key={s.id}
            className="list-row"
            style={{ cursor: "pointer" }}
            onClick={() => api.submission(s.id).then(setSelected)}
          >
            <span className={`verdict ${verdictTone(s.verdict)}`} style={{ width: 170 }}>
              {verdictLabel(s.verdict)}
            </span>
            <span className="muted small" style={{ width: 90 }}>
              {formatMs(s.runtimeMs)}
            </span>
            <span className="muted small" style={{ width: 90 }}>
              {formatBytes(s.memoryBytes)}
            </span>
            <span className="spacer" />
            <span className="muted small">{relativeTime(s.createdAt)}</span>
          </div>
        ))}
      </div>
      {selected && (
        <div style={{ marginTop: 16 }}>
          <div className="row">
            <span className={`verdict ${verdictTone(selected.summary.verdict)}`}>{verdictLabel(selected.summary.verdict)}</span>
            <span className="spacer" />
            <button onClick={() => onRestore(selected.code)}>Restore this code</button>
          </div>
          <pre className="value-box" style={{ maxHeight: 320 }}>
            {selected.code}
          </pre>
        </div>
      )}
    </>
  );
}
