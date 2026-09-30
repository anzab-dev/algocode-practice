import { useCallback, useRef, useState, type ReactNode } from "react";

interface Props {
  first: ReactNode;
  second: ReactNode;
  direction?: "horizontal" | "vertical";
  initial?: number;
  min?: number;
  className?: string;
}

/** Two panes with a draggable divider. `initial` is the first pane's share in percent. */
export function Split({ first, second, direction = "horizontal", initial = 50, min = 20, className }: Props) {
  const [share, setShare] = useState(initial);
  const host = useRef<HTMLDivElement>(null);
  const vertical = direction === "vertical";

  const startDrag = useCallback(
    (e: React.PointerEvent) => {
      e.preventDefault();
      const rect = host.current!.getBoundingClientRect();
      const move = (ev: PointerEvent) => {
        const pos = vertical ? (ev.clientY - rect.top) / rect.height : (ev.clientX - rect.left) / rect.width;
        setShare(Math.min(100 - min, Math.max(min, pos * 100)));
      };
      const up = () => {
        window.removeEventListener("pointermove", move);
        window.removeEventListener("pointerup", up);
      };
      window.addEventListener("pointermove", move);
      window.addEventListener("pointerup", up);
    },
    [vertical, min],
  );

  return (
    <div
      ref={host}
      className={className}
      style={{ display: "flex", flexDirection: vertical ? "column" : "row", minHeight: 0, minWidth: 0, flex: 1 }}
    >
      <div style={{ flex: `0 0 calc(${share}% - 4px)`, display: "flex", minHeight: 0, minWidth: 0 }}>{first}</div>
      <div className={`splitter ${vertical ? "horizontal" : ""}`} onPointerDown={startDrag} role="separator" />
      <div style={{ flex: 1, display: "flex", minHeight: 0, minWidth: 0 }}>{second}</div>
    </div>
  );
}
