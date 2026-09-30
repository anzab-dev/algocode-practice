import { useEffect, useState } from "react";
import type { Achievement } from "../api";
import { onAchievements } from "./progress";

interface Toast extends Achievement {
  key: number;
}

/** Pops a card for every achievement unlocked by a submission. */
export function Toasts() {
  const [toasts, setToasts] = useState<Toast[]>([]);
  useEffect(
    () =>
      onAchievements((unlocked) => {
        const stamped = unlocked.map((a, i) => ({ ...a, key: Date.now() + i }));
        setToasts((t) => [...t, ...stamped]);
        for (const toast of stamped) {
          setTimeout(() => setToasts((t) => t.filter((x) => x.key !== toast.key)), 6000);
        }
      }),
    [],
  );
  return (
    <div className="toasts" aria-live="polite">
      {toasts.map((t) => (
        <div key={t.key} className="toast">
          <span className="icon">{t.icon}</span>
          <div>
            <div className="small muted">Achievement unlocked</div>
            <div style={{ fontWeight: 700 }}>{t.title}</div>
            <div className="small muted">{t.description}</div>
          </div>
        </div>
      ))}
    </div>
  );
}
