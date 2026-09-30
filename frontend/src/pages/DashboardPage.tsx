import { useState } from "react";
import { Link } from "react-router-dom";
import { Difficulty } from "../components/Difficulty";
import { Heatmap } from "../components/Heatmap";
import { useMe } from "../components/progress";
import { formatBytes, formatMs, relativeTime, verdictLabel } from "../format";
import { getHandle, isValidHandle, setHandle } from "../user";
import type { Difficulty as D } from "../api";

export function DashboardPage() {
  const { me, error } = useMe();
  const [editing, setEditing] = useState(false);
  const [handle, setHandleInput] = useState(getHandle());

  if (error) return <div className="container empty error-text">{error}</div>;
  if (!me) return <div className="container empty">Loading…</div>;
  const p = me.profile;
  const span = Math.max(1, p.nextLevelXp - p.levelStartXp);

  return (
    <div className="container">
      <div className="row" style={{ marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>@{p.handle}</h2>
        {editing ? (
          <>
            <input value={handle} onChange={(e) => setHandleInput(e.target.value)} style={{ width: 200 }} />
            <button
              disabled={!isValidHandle(handle)}
              onClick={() => {
                setHandle(handle);
                setEditing(false);
              }}
            >
              Switch
            </button>
            <span className="small muted">Progress is kept per handle. There are no passwords yet.</span>
          </>
        ) : (
          <button className="ghost" onClick={() => setEditing(true)}>
            Change handle
          </button>
        )}
      </div>

      <div className="stat-row">
        <div className="card">
          <h3>Level</h3>
          <div className="row">
            <span className="level-badge" style={{ height: 36, minWidth: 36, fontSize: 16, borderRadius: 18 }}>
              {p.level}
            </span>
            <div style={{ flex: 1 }}>
              <div className="bar" style={{ width: "100%" }}>
                <span style={{ width: `${(100 * (p.xp - p.levelStartXp)) / span}%` }} />
              </div>
              <div className="small muted" style={{ marginTop: 4 }}>
                {p.xp} XP · {p.nextLevelXp - p.xp} to level {p.level + 1}
              </div>
            </div>
          </div>
        </div>
        <div className="card">
          <h3>Streak</h3>
          <div className="big-number">🔥 {p.currentStreak}</div>
          <div className="small muted">longest {p.longestStreak} day(s)</div>
        </div>
        <div className="card">
          <h3>Solved</h3>
          <div className="big-number">
            {p.solved}
            <span className="muted" style={{ fontSize: 16 }}>
              /{p.totalProblems}
            </span>
          </div>
          <div className="small">
            {(["EASY", "MEDIUM", "HARD"] as D[]).map((d) => (
              <span key={d} style={{ marginRight: 10 }}>
                <Difficulty value={d} /> {p.byDifficulty[d].solved}
              </span>
            ))}
          </div>
        </div>
        <div className="card">
          <h3>Acceptance</h3>
          <div className="big-number">{p.acceptanceRate.toFixed(0)}%</div>
          <div className="small muted">{p.submissions} submission(s)</div>
        </div>
      </div>

      <div className="card" style={{ marginBottom: 16 }}>
        <h3>Activity</h3>
        <Heatmap activity={p.activity} />
      </div>

      <div className="card" style={{ marginBottom: 16 }}>
        <h3>
          Achievements · {p.achievements.filter((a) => a.unlocked).length}/{p.achievements.length}
        </h3>
        <div className="achievements">
          {p.achievements.map(({ achievement: a, unlocked }) => (
            <div key={a.id} className={`achievement ${unlocked ? "" : "locked"}`} title={a.description}>
              <span className="icon">{a.icon}</span>
              <div>
                <div className="title">{a.title}</div>
                <div className="small muted">{a.description}</div>
              </div>
            </div>
          ))}
        </div>
      </div>

      <div className="card">
        <h3>Recent submissions</h3>
        {me.recent.length === 0 && <div className="muted">Nothing yet. <Link to="/">Pick a problem</Link> to get going.</div>}
        <div className="list">
          {me.recent.map((s) => (
            <div key={s.id} className="list-row">
              <Link to={`/problems/${s.problemSlug}`} style={{ flex: 1, color: "var(--text)" }}>
                {s.problemTitle}
              </Link>
              <span className={`verdict ${s.verdict === "ACCEPTED" ? "good" : "bad"}`} style={{ width: 170 }}>
                {verdictLabel(s.verdict)}
              </span>
              <span className="small muted" style={{ width: 150 }}>
                {s.runtimeMs != null ? `${formatMs(s.runtimeMs)} · ${formatBytes(s.memoryBytes)}` : ""}
              </span>
              <span className="small muted" style={{ width: 80, textAlign: "right" }}>
                {relativeTime(s.createdAt)}
              </span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
