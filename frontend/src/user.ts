const KEY = "algopractice.handle";
const listeners = new Set<() => void>();

function randomHandle() {
  return `coder-${Math.random().toString(36).slice(2, 7)}`;
}

function read(key: string): string | null {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

let cached: string | null = null;

/** The practice handle sent with every request. There are no accounts yet; see UserResolver on the backend. */
export function getHandle(): string {
  if (cached) return cached;
  cached = read(KEY);
  if (!cached || !isValidHandle(cached)) {
    cached = randomHandle();
    try {
      localStorage.setItem(KEY, cached);
    } catch {
      // storage unavailable: keep the handle for this tab only
    }
  }
  return cached;
}

export function setHandle(handle: string) {
  cached = handle;
  try {
    localStorage.setItem(KEY, handle);
  } catch {
    // ignore
  }
  listeners.forEach((l) => l());
}

export function onHandleChange(listener: () => void) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function isValidHandle(handle: string) {
  return /^[A-Za-z0-9_.-]{2,32}$/.test(handle);
}

/** Per-browser storage helpers for drafts; failures are silently ignored. */
export const drafts = {
  get(key: string): string | null {
    return read(`algopractice.draft.${key}`);
  },
  set(key: string, value: string) {
    try {
      localStorage.setItem(`algopractice.draft.${key}`, value);
    } catch {
      // ignore
    }
  },
  remove(key: string) {
    try {
      localStorage.removeItem(`algopractice.draft.${key}`);
    } catch {
      // ignore
    }
  },
};
