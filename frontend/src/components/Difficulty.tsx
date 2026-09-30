import type { Difficulty as D } from "../api";

export const DIFFICULTY_LABEL: Record<D, string> = { EASY: "Starter", MEDIUM: "Core", HARD: "Expert" };
const PIPS: Record<D, number> = { EASY: 1, MEDIUM: 2, HARD: 3 };

/** Difficulty as a small pip meter plus its name. */
export function Difficulty({ value }: { value: D }) {
  return (
    <span className={`diff ${value}`} title={`Difficulty: ${DIFFICULTY_LABEL[value]}`}>
      <span className="pips" aria-hidden="true">
        {[1, 2, 3].map((i) => (
          <i key={i} className={i <= PIPS[value] ? "on" : ""} />
        ))}
      </span>
      {DIFFICULTY_LABEL[value]}
    </span>
  );
}
