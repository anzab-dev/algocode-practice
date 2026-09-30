import { useState } from "react";
import type { Distribution } from "../api";

interface Props {
  distribution: Distribution;
  format: (value: number) => string;
  label: string;
}

const WIDTH = 360;
const HEIGHT = 110;
const BASE = 92;

/**
 * Distribution of accepted results for a problem, with the viewer's best result
 * highlighted. One series, so no legend: the title names it and the "you" bar is labelled.
 */
export function Histogram({ distribution, format, label }: Props) {
  const [hover, setHover] = useState<number | null>(null);
  const { buckets, mine } = distribution;
  if (buckets.length === 0) {
    return <div className="small muted">Not enough accepted solutions yet to compare {label}.</div>;
  }
  const max = Math.max(...buckets.map((b) => b.count), 1);
  const slot = WIDTH / buckets.length;
  const barWidth = slot - 2; // 2px surface gap between bars
  const mineIndex =
    mine == null
      ? -1
      : Math.max(0, buckets.findIndex((b, i) => mine >= b.from && (mine < b.to || i === buckets.length - 1)));

  return (
    <div className="histogram-wrap">
      <svg
        className="histogram"
        viewBox={`0 0 ${WIDTH} ${HEIGHT}`}
        role="img"
        aria-label={`Distribution of ${label} across ${distribution.count} accepted submissions`}
        onMouseLeave={() => setHover(null)}
      >
        <line x1="0" x2={WIDTH} y1={BASE} y2={BASE} stroke="var(--border)" strokeWidth="1" />
        {buckets.map((b, i) => {
          const h = b.count === 0 ? 0 : Math.max(3, (b.count / max) * (BASE - 14));
          const x = i * slot + 1;
          const isMine = i === mineIndex;
          const fill = isMine ? "var(--accent)" : hover === i ? "#6f8fd6" : "#3d5a99";
          return (
            <g key={i} onMouseEnter={() => setHover(i)}>
              {/* generous hit target: the whole column */}
              <rect x={i * slot} y={0} width={slot} height={BASE} fill="transparent" />
              {h > 0 && (
                <path
                  d={`M${x},${BASE} v${-(h - 4)} q0,-4 4,-4 h${barWidth - 8} q4,0 4,4 v${h - 4} z`}
                  fill={fill}
                />
              )}
              {isMine && (
                <text x={x + barWidth / 2} y={BASE - h - 4} textAnchor="middle" fontSize="10" fill="var(--text)">
                  you
                </text>
              )}
            </g>
          );
        })}
        <text x="0" y={HEIGHT - 4} fontSize="10" fill="var(--muted)">
          {format(buckets[0].from)}
        </text>
        <text x={WIDTH} y={HEIGHT - 4} fontSize="10" fill="var(--muted)" textAnchor="end">
          {format(buckets[buckets.length - 1].to)}
        </text>
      </svg>
      {hover != null && (
        <div
          className="chart-tooltip"
          style={{ left: `${((hover + 0.5) / buckets.length) * 100}%`, top: `${(20 / HEIGHT) * 100}%` }}
        >
          {format(buckets[hover].from)} – {format(buckets[hover].to)}: <strong>{buckets[hover].count}</strong>
        </div>
      )}
    </div>
  );
}
