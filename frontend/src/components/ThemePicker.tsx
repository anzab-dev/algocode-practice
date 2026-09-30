import { setTheme, THEMES, useTheme } from "../theme";

/** Swatch buttons for the colour themes; the choice is kept in this browser. */
export function ThemePicker({ compact = false }: { compact?: boolean }) {
  const current = useTheme();
  const active = THEMES.find((t) => t.id === current)!;
  if (compact) {
    const next = THEMES[(THEMES.indexOf(active) + 1) % THEMES.length];
    return (
      <button className="theme-cycle" title={`Theme: ${active.label}. Click for ${next.label}.`} onClick={() => setTheme(next.id)}>
        <Swatch colors={active.swatch} />
      </button>
    );
  }
  return (
    <div className="theme-picker">
      <div className="theme-picker-label">
        Theme <span>{active.label}</span>
      </div>
      <div className="theme-swatches" role="radiogroup" aria-label="Colour theme">
        {THEMES.map((t) => (
          <button
            key={t.id}
            role="radio"
            aria-checked={t.id === current}
            className={t.id === current ? "active" : ""}
            title={`${t.label}: ${t.description}`}
            onClick={() => setTheme(t.id)}
          >
            <Swatch colors={t.swatch} />
          </button>
        ))}
      </div>
    </div>
  );
}

function Swatch({ colors: [bg, surface, accent] }: { colors: [string, string, string] }) {
  return (
    <svg width="22" height="22" viewBox="0 0 22 22" aria-hidden="true">
      <circle cx="11" cy="11" r="10" fill={bg} stroke={surface} strokeWidth="2" />
      <path d="M11 1a10 10 0 0 1 0 20Z" fill={surface} />
      <circle cx="11" cy="11" r="4" fill={accent} />
    </svg>
  );
}
