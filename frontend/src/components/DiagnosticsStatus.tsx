import type { Diagnostic } from "../api";

/** IntelliJ-style inspection widget: error/warning counts under the editor. */
export function DiagnosticsStatus({ diagnostics, extra }: { diagnostics: Diagnostic[]; extra?: string }) {
  const errors = diagnostics.filter((d) => d.severity === "ERROR");
  const warnings = diagnostics.filter((d) => d.severity === "WARNING");
  const first = errors[0] ?? warnings[0];
  return (
    <div className="status-line">
      {errors.length === 0 && warnings.length === 0 ? (
        <span style={{ color: "var(--ok)" }}>✓ No problems</span>
      ) : (
        <>
          {errors.length > 0 && <span className="errors">● {errors.length} error{errors.length > 1 ? "s" : ""}</span>}
          {warnings.length > 0 && (
            <span className="warnings">▲ {warnings.length} warning{warnings.length > 1 ? "s" : ""}</span>
          )}
          {first && (
            <span style={{ overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
              Ln {first.startLine}: {first.message}
            </span>
          )}
        </>
      )}
      <span className="spacer" />
      {extra && <span>{extra}</span>}
      <span>
        <kbd>F2</kbd> next problem · <kbd>Ctrl</kbd>+<kbd>Space</kbd> complete
      </span>
    </div>
  );
}
