import { useState } from "react";
import { NavLink, Outlet } from "react-router-dom";
import { getHandle } from "../user";
import { Icon, type IconName } from "./Icon";
import { useMe } from "./progress";
import { ThemePicker } from "./ThemePicker";
import { Toasts } from "./Toasts";

const NAV: { to: string; label: string; icon: IconName; end?: boolean }[] = [
  { to: "/", label: "Problems", icon: "problems", end: true },
  { to: "/scratchpad", label: "Scratchpad", icon: "scratch" },
  { to: "/me", label: "Progress", icon: "progress" },
  { to: "/leaderboard", label: "Leaderboard", icon: "leaderboard" },
];

const COLLAPSED_KEY = "algopractice.sidebar.collapsed";

function readCollapsed() {
  try {
    return localStorage.getItem(COLLAPSED_KEY) === "1";
  } catch {
    return false;
  }
}

export function Layout() {
  const { me } = useMe();
  const [collapsed, setCollapsed] = useState(readCollapsed);
  const p = me?.profile;
  const span = p ? Math.max(1, p.nextLevelXp - p.levelStartXp) : 1;
  const into = p ? p.xp - p.levelStartXp : 0;

  const toggle = () => {
    setCollapsed((c) => {
      try {
        localStorage.setItem(COLLAPSED_KEY, c ? "0" : "1");
      } catch {
        // ignore
      }
      return !c;
    });
  };

  return (
    <div className={`app ${collapsed ? "collapsed" : ""}`}>
      <aside className="sidebar">
        <NavLink to="/" className="brand" title="AlgoPractice">
          <span className="brand-mark" aria-hidden="true">
            ›_
          </span>
          <span className="brand-name">
            Algo<b>Practice</b>
          </span>
        </NavLink>
        <nav className="nav">
          {NAV.map((n) => (
            <NavLink key={n.to} to={n.to} end={n.end} title={n.label}>
              <Icon name={n.icon} />
              <span className="nav-label">{n.label}</span>
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-spacer" />
        {p && (
          <NavLink to="/me" className="player" title={`${p.xp} XP · ${p.nextLevelXp - p.xp} XP to level ${p.level + 1}`}>
            <span className="level-ring" style={{ ["--pct" as string]: `${(100 * into) / span}` }}>
              <span>{p.level}</span>
            </span>
            <span className="player-meta">
              <span className="handle-chip">@{getHandle()}</span>
              <span className="small muted">{p.xp} XP</span>
            </span>
            <span
              className={`streak-pill ${p.currentStreak > 0 ? "" : "cold"}`}
              title={`Current streak: ${p.currentStreak} day(s). Longest: ${p.longestStreak}.`}
            >
              <Icon name="flame" size={14} />
              {p.currentStreak}
            </span>
          </NavLink>
        )}
        <ThemePicker compact={collapsed} />
        <button className="ghost collapse-toggle" onClick={toggle} title={collapsed ? "Expand sidebar" : "Collapse sidebar"}>
          <Icon name={collapsed ? "expand" : "collapse"} size={16} />
        </button>
      </aside>
      <main className="page">
        <Outlet />
      </main>
      <Toasts />
    </div>
  );
}
