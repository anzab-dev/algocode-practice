import { describe, expect, it } from "vitest";
import { describeArgs, formatBytes, formatMs, parseArgs, relativeTime } from "./format";

describe("format", () => {
  it("formats durations and sizes", () => {
    expect(formatMs(0.1234)).toBe("0.123 ms");
    expect(formatMs(12.345)).toBe("12.35 ms");
    expect(formatMs(1500)).toBe("1500 ms");
    expect(formatMs(null)).toBe("–");
    expect(formatBytes(512)).toBe("512 B");
    expect(formatBytes(2048)).toBe("2.0 KB");
    expect(formatBytes(3 * 1024 * 1024)).toBe("3.00 MB");
  });

  it("round-trips arguments through the editable text form", () => {
    const text = describeArgs("[[2,7,11,15],9]", ["nums", "target"]);
    expect(text).toBe("nums = [2,7,11,15]\ntarget = 9");
    expect(parseArgs(text, ["nums", "target"])).toEqual([[2, 7, 11, 15], 9]);
    expect(parseArgs('"abc"', ["s"])).toEqual(["abc"]);
  });

  it("explains malformed arguments", () => {
    expect(() => parseArgs("nums = [1,", ["nums"])).toThrow("not valid JSON");
    expect(() => parseArgs("1", ["a", "b"])).toThrow("Expected 2 line(s)");
  });

  it("describes relative times", () => {
    const now = Date.parse("2026-09-30T12:00:00Z");
    expect(relativeTime("2026-09-30T11:59:30Z", now)).toBe("just now");
    expect(relativeTime("2026-09-30T09:00:00Z", now)).toBe("3 h ago");
  });
});
