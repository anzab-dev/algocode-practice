import { useEffect, useRef } from "react";

const COLORS = ["#f0a33c", "#5fb865", "#548af7", "#f75464", "#e0b64c", "#c77dff"];

/** A short burst of confetti, drawn on a full-screen canvas. Remount (change `burst`) to replay. */
export function Confetti({ burst }: { burst: number }) {
  const canvas = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    if (!burst || !canvas.current) return;
    if (window.matchMedia?.("(prefers-reduced-motion: reduce)").matches) return;
    const el = canvas.current;
    const ctx = el.getContext("2d");
    if (!ctx) return;
    el.width = window.innerWidth;
    el.height = window.innerHeight;
    const pieces = Array.from({ length: 140 }, () => ({
      x: el.width / 2 + (Math.random() - 0.5) * 200,
      y: el.height * 0.35,
      vx: (Math.random() - 0.5) * 14,
      vy: -Math.random() * 14 - 4,
      size: 5 + Math.random() * 6,
      spin: Math.random() * Math.PI,
      color: COLORS[Math.floor(Math.random() * COLORS.length)],
    }));
    let frame = 0;
    let raf = 0;
    const tick = () => {
      ctx.clearRect(0, 0, el.width, el.height);
      for (const p of pieces) {
        p.vy += 0.35;
        p.vx *= 0.99;
        p.x += p.vx;
        p.y += p.vy;
        p.spin += 0.2;
        ctx.save();
        ctx.translate(p.x, p.y);
        ctx.rotate(p.spin);
        ctx.fillStyle = p.color;
        ctx.globalAlpha = Math.max(0, 1 - frame / 150);
        ctx.fillRect(-p.size / 2, -p.size / 4, p.size, p.size / 2);
        ctx.restore();
      }
      if (++frame < 150) raf = requestAnimationFrame(tick);
      else ctx.clearRect(0, 0, el.width, el.height);
    };
    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
  }, [burst]);

  return <canvas ref={canvas} className="confetti" aria-hidden="true" />;
}
