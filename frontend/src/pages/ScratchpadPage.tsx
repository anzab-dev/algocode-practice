import { useCallback, useEffect, useState } from "react";
import { api, type Scratchpad } from "../api";
import { notifyProgressChanged } from "../components/progress";
import { SCRATCH_TEMPLATE, ScratchWorkbench } from "../components/ScratchWorkbench";
import { relativeTime } from "../format";
import { drafts } from "../user";

interface Draft {
  id: number | null;
  title: string;
  code: string;
  stdin: string;
  dirty: boolean;
}

const UNSAVED_KEY = "scratch.unsaved";

function freshDraft(): Draft {
  return { id: null, title: "Untitled idea", code: drafts.get(UNSAVED_KEY) ?? SCRATCH_TEMPLATE, stdin: "", dirty: false };
}

/** Saved Java snippets for trying ideas outside any problem. */
export function ScratchpadPage() {
  const [pads, setPads] = useState<Scratchpad[]>([]);
  const [draft, setDraft] = useState<Draft>(freshDraft);
  const [status, setStatus] = useState<string | null>(null);

  const reload = useCallback(() => api.scratchpads().then(setPads).catch(() => setPads([])), []);
  useEffect(() => {
    reload();
  }, [reload]);

  const open = (pad: Scratchpad) => {
    if (draft.dirty && !confirm("Discard unsaved changes?")) return;
    setDraft({ id: pad.id, title: pad.title, code: pad.code, stdin: pad.stdin, dirty: false });
    setStatus(null);
  };

  const save = useCallback(async () => {
    const body = { title: draft.title.trim() || "Untitled idea", code: draft.code, stdin: draft.stdin };
    try {
      const saved = draft.id == null ? await api.createScratchpad(body) : await api.updateScratchpad(draft.id, body);
      setDraft({ id: saved.id, title: saved.title, code: saved.code, stdin: saved.stdin, dirty: false });
      if (draft.id == null) drafts.remove(UNSAVED_KEY);
      setStatus("Saved");
      reload();
      notifyProgressChanged();
    } catch (e) {
      setStatus(`Save failed: ${(e as Error).message}`);
    }
  }, [draft, reload]);

  const remove = async (pad: Scratchpad) => {
    if (!confirm(`Delete "${pad.title}"?`)) return;
    await api.deleteScratchpad(pad.id);
    if (draft.id === pad.id) setDraft(freshDraft());
    reload();
  };

  return (
    <div className="scratch-layout">
      <div className="pane scratch-sidebar">
        <div className="tabs">
          <strong style={{ padding: "0 6px" }}>Scratchpads</strong>
          <span className="spacer" />
          <button
            className="ghost"
            title="New scratchpad"
            onClick={() => {
              if (draft.dirty && !confirm("Discard unsaved changes?")) return;
              setDraft({ ...freshDraft(), code: SCRATCH_TEMPLATE });
            }}
          >
            ＋
          </button>
        </div>
        <div className="pane-body" style={{ padding: 6 }}>
          {pads.length === 0 && <div className="small muted" style={{ padding: 8 }}>Saved ideas appear here. Press Ctrl+S to save.</div>}
          {pads.map((pad) => (
            <div key={pad.id} className={`scratch-item ${draft.id === pad.id ? "active" : ""}`} onClick={() => open(pad)}>
              <span>🧪</span>
              <span className="name" title={pad.title}>
                {pad.title}
              </span>
              <span className="small">{relativeTime(pad.updatedAt)}</span>
              <button
                className="ghost"
                style={{ padding: "0 6px" }}
                title="Delete"
                onClick={(e) => {
                  e.stopPropagation();
                  remove(pad);
                }}
              >
                ×
              </button>
            </div>
          ))}
        </div>
      </div>
      <ScratchWorkbench
        path={`/scratch/pad/${draft.id ?? "new"}/Main.java`}
        code={draft.code}
        stdin={draft.stdin}
        onCode={(code) => {
          setDraft((d) => ({ ...d, code, dirty: true }));
          if (draft.id == null) drafts.set(UNSAVED_KEY, code);
        }}
        onStdin={(stdin) => setDraft((d) => ({ ...d, stdin, dirty: true }))}
        onSave={save}
        toolbar={
          <>
            <input
              value={draft.title}
              onChange={(e) => setDraft((d) => ({ ...d, title: e.target.value, dirty: true }))}
              style={{ width: 220, padding: "3px 8px" }}
              aria-label="Scratchpad title"
            />
            <span className="small muted">
              {draft.dirty ? "● unsaved" : status ?? ""}
            </span>
          </>
        }
      />
    </div>
  );
}
