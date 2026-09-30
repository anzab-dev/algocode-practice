import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { api, type Difficulty as D, type ProblemSummary } from "../api";
import { Difficulty } from "../components/Difficulty";
import { useMe } from "../components/progress";

const DIFF_COLORS: Record<D, string> = { EASY: "var(--easy)", MEDIUM: "var(--medium)", HARD: "var(--hard)" };

export function ProblemsPage() {
  const [problems, setProblems] = useState<ProblemSummary[] | null>(null);
  const [daily, setDaily] = useState<ProblemSummary | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [query, setQuery] = useState("");
  const [difficulty, setDifficulty] = useState<D | "">("");
  const [tag, setTag] = useState("");
  const { me } = useMe();

  useEffect(() => {
    api.problems().then(setProblems).catch((e: Error) => setError(e.message));
    api.daily().then(setDaily).catch(() => setDaily(null));
  }, []);

  const tags = useMemo(() => [...new Set(problems?.flatMap((p) => p.tags) ?? [])].sort(), [problems]);
  const visible = useMemo(
    () =>
      (problems ?? []).filter(
        (p) =>
          (!difficulty || p.difficulty === difficulty) &&
          (!tag || p.tags.includes(tag)) &&
          p.title.toLowerCase().includes(query.toLowerCase()),
      ),
    [problems, difficulty, tag, query],
  );

  const profile = me?.profile;
  return (
    <div className="container">
      <div className="hero">
        <div className="card daily">
          <div className="eyebrow">Daily challenge</div>
          {daily ? (
            <>
              <h2>{daily.title}</h2>
              <div className="row" style={{ marginBottom: 14 }}>
                <Difficulty value={daily.difficulty} />
                {daily.tags.map((t) => (
                  <span key={t} className="tag">
                    {t}
                  </span>
                ))}
              </div>
              <Link to={`/problems/${daily.slug}`}>
                <button className="accent">{daily.status === "SOLVED" ? "Solve it faster" : "Start challenge"} →</button>
              </Link>
            </>
          ) : (
            <h2 className="muted">Loading…</h2>
          )}
        </div>
        <div className="card">
          <h3>Your progress</h3>
          {profile ? (
            <div className="progress-rings">
              <div style={{ textAlign: "center" }}>
                <div className="big-number">
                  {profile.solved}
                  <span className="muted" style={{ fontSize: 16 }}>
                    /{profile.totalProblems}
                  </span>
                </div>
                <div className="small muted">solved</div>
              </div>
              <div className="diff-bars">
                {(["EASY", "MEDIUM", "HARD"] as D[]).map((d) => {
                  const s = profile.byDifficulty[d];
                  return (
                    <div key={d}>
                      <div className="diff-bar-label">
                        <Difficulty value={d} />
                        <span className="muted">
                          {s.solved}/{s.total}
                        </span>
                      </div>
                      <div className="diff-bar">
                        <span style={{ width: `${s.total ? (100 * s.solved) / s.total : 0}%`, background: DIFF_COLORS[d] }} />
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          ) : (
            <div className="muted">Loading…</div>
          )}
        </div>
      </div>

      <div className="filters">
        <input placeholder="Search problems" value={query} onChange={(e) => setQuery(e.target.value)} />
        <select value={difficulty} onChange={(e) => setDifficulty(e.target.value as D | "")}>
          <option value="">All difficulties</option>
          <option value="EASY">Easy</option>
          <option value="MEDIUM">Medium</option>
          <option value="HARD">Hard</option>
        </select>
        <select value={tag} onChange={(e) => setTag(e.target.value)}>
          <option value="">All topics</option>
          {tags.map((t) => (
            <option key={t}>{t}</option>
          ))}
        </select>
      </div>

      <div className="card" style={{ padding: 0, overflow: "hidden" }}>
        {error && <div className="empty error-text">Could not load problems: {error}</div>}
        {!error && !problems && <div className="empty">Loading problems…</div>}
        {problems && (
          <table className="problem-table">
            <thead>
              <tr>
                <th style={{ width: 40 }} />
                <th>Title</th>
                <th>Topics</th>
                <th style={{ width: 110 }}>Acceptance</th>
                <th style={{ width: 90 }}>Difficulty</th>
              </tr>
            </thead>
            <tbody>
              {visible.map((p) => (
                <tr key={p.slug}>
                  <td>
                    <span className={`status-icon ${p.status ?? ""}`} title={p.status ?? "Not attempted"}>
                      {p.status === "SOLVED" ? "✓" : p.status === "ATTEMPTED" ? "◐" : ""}
                    </span>
                  </td>
                  <td>
                    <Link to={`/problems/${p.slug}`}>
                      {problems.indexOf(p) + 1}. {p.title}
                    </Link>
                  </td>
                  <td>
                    {p.tags.slice(0, 3).map((t) => (
                      <span key={t} className="tag">
                        {t}
                      </span>
                    ))}
                  </td>
                  <td className="muted">{p.submissions ? `${p.acceptanceRate.toFixed(1)}%` : "–"}</td>
                  <td>
                    <Difficulty value={p.difficulty} />
                  </td>
                </tr>
              ))}
              {visible.length === 0 && (
                <tr>
                  <td colSpan={5} className="empty">
                    No problems match these filters.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
