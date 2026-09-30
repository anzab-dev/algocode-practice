import { useState, type ReactNode } from "react";
import { api, type Diagnostic, type ScratchReport } from "../api";
import { CodeEditor } from "../editor/CodeEditor";
import { formatBytes, formatMs, verdictLabel } from "../format";
import { Split } from "./Split";
import { DiagnosticsStatus } from "./DiagnosticsStatus";

export const SCRATCH_TEMPLATE = `import java.util.*;

public class Main {
    public static void main(String[] args) {
        // Try an idea here. Type "sout" + Tab to print, Ctrl+Space to complete.
        // Read input with: Scanner in = new Scanner(System.in);
        List<Integer> xs = new ArrayList<>(List.of(5, 3, 8, 1));
        Collections.sort(xs);
        System.out.println("Hello, scratchpad! " + xs);
    }
}
`;

interface Props {
  path: string;
  code: string;
  stdin: string;
  onCode: (code: string) => void;
  onStdin: (stdin: string) => void;
  toolbar?: ReactNode;
  onSave?: () => void;
}

/** Editor + stdin + console for free-form Java programs. Runs with Ctrl/Cmd+Enter. */
export function ScratchWorkbench({ path, code, stdin, onCode, onStdin, toolbar, onSave }: Props) {
  const [running, setRunning] = useState(false);
  const [report, setReport] = useState<ScratchReport | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [diagnostics, setDiagnostics] = useState<Diagnostic[]>([]);

  const run = async () => {
    if (running) return;
    setRunning(true);
    setError(null);
    try {
      setReport(await api.runScratch(code, stdin));
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setRunning(false);
    }
  };

  const ok = report?.status === "COMPLETED" && !report.exception;

  return (
    <div className="pane" style={{ flex: 1 }}>
      <div className="editor-toolbar">
        {toolbar}
        <span className="spacer" />
        <span className="small muted">
          <kbd>Ctrl</kbd>+<kbd>Enter</kbd> run
        </span>
        {onSave && <button onClick={onSave}>Save</button>}
        <button className="primary" onClick={run} disabled={running}>
          {running ? <span className="spinner" /> : "▶"} Run
        </button>
      </div>
      <Split
        direction="vertical"
        initial={62}
        first={
          <div style={{ display: "flex", flexDirection: "column", flex: 1, minHeight: 0 }}>
            <div className="editor-host">
              <CodeEditor
                path={path}
                value={code}
                onChange={onCode}
                onDiagnostics={setDiagnostics}
                shortcuts={(monaco) => [
                  { keys: monaco.KeyMod.CtrlCmd | monaco.KeyCode.Enter, run },
                  ...(onSave ? [{ keys: monaco.KeyMod.CtrlCmd | monaco.KeyCode.KeyS, run: onSave }] : []),
                ]}
              />
            </div>
            <DiagnosticsStatus diagnostics={diagnostics} />
          </div>
        }
        second={
          <div className="pane-body" style={{ paddingTop: 10 }}>
            <div className="io-grid">
              <div>
                <div className="field-label" style={{ marginTop: 0 }}>
                  Input (stdin)
                </div>
                <textarea value={stdin} onChange={(e) => onStdin(e.target.value)} spellCheck={false} />
              </div>
              <div>
                <div className="field-label row" style={{ marginTop: 0 }}>
                  <span>Output</span>
                  {report && (
                    <>
                      <span className={`verdict ${ok ? "good" : "bad"}`}>{verdictLabel(report.status)}</span>
                      {report.status !== "COMPILE_ERROR" && (
                        <span>
                          {formatMs(report.timeMs)} · peak heap {formatBytes(report.peakHeapBytes)} · allocated{" "}
                          {formatBytes(report.allocatedBytes)}
                        </span>
                      )}
                    </>
                  )}
                </div>
                <pre className="output">
                  {error && <span className="stderr">{error}</span>}
                  {!report && !error && <span className="muted">Run the program to see its output here.</span>}
                  {report?.message && <span className="stderr">{report.message + "\n"}</span>}
                  {report?.status === "COMPILE_ERROR" &&
                    report.diagnostics
                      .filter((d) => d.severity === "ERROR")
                      .map((d, i) => (
                        <span key={i} className="stderr">
                          {`Line ${d.startLine}:${d.startColumn}  ${d.message}\n`}
                        </span>
                      ))}
                  {report?.stdout}
                  {report?.stdoutTruncated && <span className="muted">{"\n… output truncated"}</span>}
                  {report?.stderr && <span className="stderr">{report.stderr}</span>}
                  {report?.exception && <span className="stderr">{"\n" + report.exception.trace}</span>}
                </pre>
              </div>
            </div>
          </div>
        }
      />
    </div>
  );
}
