const WEEKS = 26;

function level(count: number) {
  if (count === 0) return "";
  if (count === 1) return "l1";
  if (count <= 3) return "l2";
  if (count <= 6) return "l3";
  return "l4";
}

function isoDay(d: Date) {
  return d.toISOString().slice(0, 10);
}

/** GitHub-style activity grid: one cell per UTC day, columns are weeks. */
export function Heatmap({ activity }: { activity: Record<string, number> }) {
  const today = new Date();
  const start = new Date(Date.UTC(today.getUTCFullYear(), today.getUTCMonth(), today.getUTCDate()));
  start.setUTCDate(start.getUTCDate() - (WEEKS * 7 - 1) - start.getUTCDay());
  const cells = [];
  for (let i = 0; i < WEEKS * 7 + today.getUTCDay() + 1; i++) {
    const d = new Date(start);
    d.setUTCDate(start.getUTCDate() + i);
    const key = isoDay(d);
    const count = activity[key] ?? 0;
    cells.push(<span key={key} className={level(count)} title={`${key}: ${count} submission(s)`} />);
  }
  return <div className="heatmap">{cells}</div>;
}
