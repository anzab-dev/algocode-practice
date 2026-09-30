import { useEffect, useState } from "react";
import { api, type LeaderboardEntry } from "../api";
import { getHandle } from "../user";

const MEDALS = ["🥇", "🥈", "🥉"];

export function LeaderboardPage() {
  const [rows, setRows] = useState<LeaderboardEntry[] | null>(null);
  useEffect(() => {
    api.leaderboard().then(setRows).catch(() => setRows([]));
  }, []);
  const me = getHandle();
  return (
    <div className="container" style={{ maxWidth: 760 }}>
      <h2>Leaderboard</h2>
      <div className="card" style={{ padding: 0, overflow: "hidden" }}>
        {!rows && <div className="empty">Loading…</div>}
        {rows && rows.length === 0 && <div className="empty">No one has submitted yet. Be the first!</div>}
        {rows && rows.length > 0 && (
          <table className="problem-table">
            <thead>
              <tr>
                <th style={{ width: 60 }}>#</th>
                <th>Handle</th>
                <th>Level</th>
                <th>XP</th>
                <th>Solved</th>
                <th>Streak</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r, i) => (
                <tr key={r.handle} style={r.handle === me ? { background: "rgba(84,138,247,0.12)" } : undefined}>
                  <td>{MEDALS[i] ?? i + 1}</td>
                  <td className="mono">@{r.handle}</td>
                  <td>
                    <span className="level-badge" style={{ display: "inline-grid" }}>
                      {r.level}
                    </span>
                  </td>
                  <td>{r.xp}</td>
                  <td>{r.solved}</td>
                  <td>🔥 {r.streak}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
