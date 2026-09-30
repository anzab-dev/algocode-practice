import { NavLink, Outlet } from "react-router-dom";
import { getHandle } from "../user";
import { useMe } from "./progress";
import { Toasts } from "./Toasts";

export function Layout() {
  const { me } = useMe();
  const p = me?.profile;
  const span = p ? Math.max(1, p.nextLevelXp - p.levelStartXp) : 1;
  const into = p ? p.xp - p.levelStartXp : 0;

  return (
    <div className="app">
      <header className="topbar">
        <NavLink to="/" className="brand">
          <span className="brand-mark">{"{}"}</span>
          AlgoPractice
        </NavLink>
        <nav className="nav">
          <NavLink to="/" end>
            Problems
          </NavLink>
          <NavLink to="/scratchpad">Scratchpad</NavLink>
          <NavLink to="/me">Progress</NavLink>
          <NavLink to="/leaderboard">Leaderboard</NavLink>
        </nav>
        <div className="topbar-right">
          {p && (
            <>
              <span
                className={`streak-pill ${p.currentStreak > 0 ? "" : "cold"}`}
                title={`Current streak: ${p.currentStreak} day(s). Longest: ${p.longestStreak}.`}
              >
                🔥 {p.currentStreak}
              </span>
              <NavLink to="/me" className="xp-meter" title={`${p.xp} XP · ${p.nextLevelXp - p.xp} XP to level ${p.level + 1}`}>
                <span className="level-badge">{p.level}</span>
                <span className="bar">
                  <span style={{ width: `${(100 * into) / span}%` }} />
                </span>
                <span>{p.xp} XP</span>
              </NavLink>
            </>
          )}
          <span className="handle-chip">@{getHandle()}</span>
        </div>
      </header>
      <main className="page">
        <Outlet />
      </main>
      <Toasts />
    </div>
  );
}
