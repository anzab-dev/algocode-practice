import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { api, type Difficulty as D, type ProblemSummary } from "../api";
import { Difficulty, DIFFICULTY_LABEL } from "../components/Difficulty";
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
  const solvedCount = (problems ?? []).filter((p) => p.status === "SOLVED").length;
  return (
    <div className="container problems-page">
      <section className="problems-main">
        <header className="page-head">
          <div>
            <h1>Problem set</h1>
            <p className="muted">
              {problems ? `${problems.length} problems · ${solvedCount} solved` : "Loading…"}
            </p>
          </div>
          <input
            className="search"
            placeholder="Search by title"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
        </header>

        <div className="filters">
          <div className="chip-group" role="radiogroup" aria-label="Difficulty">
            {(["", "EASY", "MEDIUM", "HARD"] as (D | "")[]).map((d) => (
              <button
                key={d || "all"}
                role="radio"
                aria-checked={difficulty === d}
                className={`chip ${difficulty === d ? "active" : ""}`}
                onClick={() => setDifficulty(d)}
              >
                {d ? DIFFICULTY_LABEL[d] : "All levels"}
              </button>
            ))}
          </div>
          <select value={tag} onChange={(e) => setTag(e.target.value)} aria-label="Topic">
            <option value="">All topics</option>
            {tags.map((t) => (
              <option key={t}>{t}</option>
            ))}
          </select>
        </div>

        {error && <div className="empty error-text">Could not load problems: {error}</div>}
        {!error && !problems && <div className="empty">Loading problems…</div>}
        {problems && (
          <div className="problem-grid">
            {visible.map((p) => (
              <Link key={p.slug} to={`/problems/${p.slug}`} className={`problem-tile ${p.status ?? ""}`}>
                <div className="tile-top">
                  <span className="tile-number mono">#{String(problems.indexOf(p) + 1).padStart(2, "0")}</span>
                  <Difficulty value={p.difficulty} />
                  <span className="spacer" />
                  <span className={`status-icon ${p.status ?? ""}`} title={p.status ? p.status.toLowerCase() : "Not attempted"}>
                    {p.status === "SOLVED" ? "✓" : p.status === "ATTEMPTED" ? "◐" : ""}
                  </span>
                </div>
                <div className="tile-title">{p.title}</div>
                <div className="tile-foot">
                  <span>
                    {p.tags.slice(0, 3).map((t) => (
                      <span key={t} className="tag">
                        {t}
                      </span>
                    ))}
                  </span>
                  <span className="small muted" title="Share of submissions accepted">
                    {p.submissions ? `${p.acceptanceRate.toFixed(0)}% pass` : "new"}
                  </span>
                </div>
              </Link>
            ))}
            {visible.length === 0 && <div className="empty">No problems match these filters.</div>}
          </div>
        )}
      </section>

      <aside className="problems-aside">
        <div className="card daily">
          <div className="eyebrow">Today's pick</div>
          {daily ? (
            <>
              <h2>{daily.title}</h2>
              <div className="row" style={{ marginBottom: 14, flexWrap: "wrap" }}>
                <Difficulty value={daily.difficulty} />
                {daily.tags.map((t) => (
                  <span key={t} className="tag">
                    {t}
                  </span>
                ))}
              </div>
              <Link to={`/problems/${daily.slug}`}>
                <button className="accent">{daily.status === "SOLVED" ? "Beat your time" : "Open it"} →</button>
              </Link>
            </>
          ) : (
            <h2 className="muted">Loading…</h2>
          )}
        </div>
        <div className="card">
          <h3>Your progress</h3>
          {profile ? (
            <>
              <div className="big-number">
                {profile.solved}
                <span className="muted" style={{ fontSize: 16 }}>
                  /{profile.totalProblems} solved
                </span>
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
            </>
          ) : (
            <div className="muted">Loading…</div>
          )}
        </div>
      </aside>
    </div>
  );
}
