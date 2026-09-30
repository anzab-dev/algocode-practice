const PATHS = {
  problems: "M4 6h16M4 12h16M4 18h10",
  scratch: "M9 3h6M10 3v6l-5 9a2 2 0 0 0 1.8 3h10.4a2 2 0 0 0 1.8-3l-5-9V3",
  progress: "M4 20V10M10 20V4M16 20v-7M22 20H2",
  leaderboard: "M8 21h8M12 17v4M7 4h10v5a5 5 0 0 1-10 0V4ZM17 5h3v2a3 3 0 0 1-3 3M7 5H4v2a3 3 0 0 0 3 3",
  collapse: "M15 6l-6 6 6 6",
  expand: "M9 6l6 6-6 6",
  swap: "M7 7h13l-4-4M17 17H4l4 4",
  flame: "M12 22c4 0 7-3 7-7 0-5-5-7-5-12-3 2-6 6-6 9-1-1-2-2-2-4-2 2-2 4-2 6 0 5 4 8 8 8Z",
} as const;

export type IconName = keyof typeof PATHS;

/** Stroke icons drawn in the current text colour. */
export function Icon({ name, size = 18 }: { name: IconName; size?: number }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d={PATHS[name]} />
    </svg>
  );
}
