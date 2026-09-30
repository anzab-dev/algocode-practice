import type { Difficulty as D } from "../api";

const LABEL: Record<D, string> = { EASY: "Easy", MEDIUM: "Medium", HARD: "Hard" };

export function Difficulty({ value }: { value: D }) {
  return <span className={`diff ${value}`}>{LABEL[value]}</span>;
}
