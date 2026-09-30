import { useCallback, useEffect, useState } from "react";
import { api, type Achievement, type Me } from "../api";
import { onHandleChange } from "../user";

const PROGRESS_EVENT = "algopractice:progress";
const ACHIEVEMENT_EVENT = "algopractice:achievement";

/** Tells every progress widget (top bar, dashboard) to refetch, e.g. after a submission. */
export function notifyProgressChanged() {
  window.dispatchEvent(new Event(PROGRESS_EVENT));
}

export function announceAchievements(achievements: Achievement[]) {
  if (achievements.length > 0) {
    window.dispatchEvent(new CustomEvent<Achievement[]>(ACHIEVEMENT_EVENT, { detail: achievements }));
  }
}

export function onAchievements(listener: (a: Achievement[]) => void) {
  const handler = (e: Event) => listener((e as CustomEvent<Achievement[]>).detail);
  window.addEventListener(ACHIEVEMENT_EVENT, handler);
  return () => window.removeEventListener(ACHIEVEMENT_EVENT, handler);
}

export function useMe() {
  const [me, setMe] = useState<Me | null>(null);
  const [error, setError] = useState<string | null>(null);
  const reload = useCallback(() => {
    api
      .me()
      .then((m) => {
        setMe(m);
        setError(null);
      })
      .catch((e: Error) => setError(e.message));
  }, []);
  useEffect(() => {
    reload();
    window.addEventListener(PROGRESS_EVENT, reload);
    const off = onHandleChange(reload);
    return () => {
      window.removeEventListener(PROGRESS_EVENT, reload);
      off();
    };
  }, [reload]);
  return { me, error, reload };
}
