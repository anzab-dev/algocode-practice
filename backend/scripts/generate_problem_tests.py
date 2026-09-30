#!/usr/bin/env python3
"""Regenerates tests.json for the bundled problems.

Hand-written samples come first; large random cases (fixed seed) follow so that runtime and
memory measurements have something to chew on. Expected answers are computed here in Python
and cross-checked by ProblemCatalogIntegrityTest, which runs every reference Solution.java
against every test.

    python3 backend/scripts/generate_problem_tests.py
"""
import json
import random
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/problems"
rng = random.Random(20260930)


def case(args, expected, sample=False):
    return {"args": args, "expected": expected, "sample": sample}


def write(slug, cases):
    path = ROOT / slug / "tests.json"
    path.write_text(json.dumps(cases, separators=(",", ":")) + "\n")
    print(f"{slug}: {len(cases)} tests, {path.stat().st_size // 1024} KiB")


# ---------------------------------------------------------------- solutions

def two_sum(nums, target):
    seen = {}
    for i, x in enumerate(nums):
        if target - x in seen:
            return [seen[target - x], i]
        seen[x] = i


def is_valid(s):
    stack, pairs = [], {")": "(", "]": "[", "}": "{"}
    for c in s:
        if c in pairs:
            if not stack or stack.pop() != pairs[c]:
                return False
        else:
            stack.append(c)
    return not stack


def max_profit(prices):
    best, low = 0, float("inf")
    for p in prices:
        low = min(low, p)
        best = max(best, p - low)
    return best


def climb(n):
    a, b = 1, 1
    for _ in range(n):
        a, b = b, a + b
    return a


def max_sub(nums):
    best = cur = nums[0]
    for x in nums[1:]:
        cur = max(x, cur + x)
        best = max(best, cur)
    return best


def longest_unique(s):
    last, start, best = {}, 0, 0
    for i, c in enumerate(s):
        if c in last and last[c] >= start:
            start = last[c] + 1
        last[c] = i
        best = max(best, i - start + 1)
    return best


def group_anagrams(strs):
    groups = defaultdict(list)
    for s in strs:
        groups["".join(sorted(s))].append(s)
    return list(groups.values())


def merge(intervals):
    out = []
    for a, b in sorted(intervals):
        if out and a <= out[-1][1]:
            out[-1][1] = max(out[-1][1], b)
        else:
            out.append([a, b])
    return out


def product_except_self(nums):
    n = len(nums)
    out = [1] * n
    left = 1
    for i in range(n):
        out[i] = left
        left *= nums[i]
    right = 1
    for i in range(n - 1, -1, -1):
        out[i] *= right
        right *= nums[i]
    return out


def trap(h):
    left, right, lmax, rmax, water = 0, len(h) - 1, 0, 0, 0
    while left < right:
        if h[left] < h[right]:
            lmax = max(lmax, h[left])
            water += lmax - h[left]
            left += 1
        else:
            rmax = max(rmax, h[right])
            water += rmax - h[right]
            right -= 1
    return water


def median(a, b):
    m = sorted(a + b)
    n = len(m)
    return float(m[n // 2]) if n % 2 else (m[n // 2 - 1] + m[n // 2]) / 2.0


def rotate(nums, k):
    k %= len(nums)
    return nums[-k:] + nums[:-k] if k else list(nums)


# ---------------------------------------------------------------- generators

def gen_two_sum():
    cases = [
        case([[2, 7, 11, 15], 9], [0, 1], True),
        case([[3, 2, 4], 6], [1, 2], True),
        case([[3, 3], 6], [0, 1], True),
        case([[-4, 10, 5, -1], 1], two_sum([-4, 10, 5, -1], 1)),
        case([[0, 4, 3, 0], 0], two_sum([0, 4, 3, 0], 0)),
    ]
    for n in (1_000, 20_000, 60_000):
        nums = rng.sample(range(-1_000_000_000, 1_000_000_000), n)
        # distinct values, so the two largest form the only pair with this sum
        nums.sort()
        a, b = nums[-1], nums[-2]
        rng.shuffle(nums)
        cases.append(case([nums, a + b], two_sum(nums, a + b)))
    return cases


def gen_valid_parentheses():
    samples = [("()", True), ("()[]{}", True), ("(]", False), ("([)]", False), ("{[]}", True)]
    cases = [case([s], v, i < 3) for i, (s, v) in enumerate(samples)]
    cases += [case(["("], False), case(["]"], False), case(["(((((())))))"], True)]

    def balanced(n):
        out, stack = [], []
        opens = "([{"
        close = {"(": ")", "[": "]", "{": "}"}
        while len(out) < n:
            if stack and (rng.random() < 0.5 or len(out) + len(stack) >= n):
                out.append(close[stack.pop()])
            else:
                c = rng.choice(opens)
                stack.append(c)
                out.append(c)
        while stack:
            out.append(close[stack.pop()])
        return "".join(out)

    big = balanced(100_000)
    cases.append(case([big], True))
    broken = big[:50_000] + ("]" if big[50_000] != "]" else ")") + big[50_001:]
    cases.append(case([broken], is_valid(broken)))
    return cases


def gen_stock():
    cases = [
        case([[7, 1, 5, 3, 6, 4]], 5, True),
        case([[7, 6, 4, 3, 1]], 0, True),
        case([[1]], 0),
        case([[2, 4, 1]], 2),
        case([[3, 2, 6, 5, 0, 3]], 4),
    ]
    for n in (10_000, 100_000):
        prices = [rng.randint(0, 10_000) for _ in range(n)]
        cases.append(case([prices], max_profit(prices)))
    return cases


def gen_climb():
    return [case([n], climb(n), n in (2, 3)) for n in (2, 3, 1, 5, 10, 20, 30, 38, 45)]


def gen_max_subarray():
    cases = [
        case([[-2, 1, -3, 4, -1, 2, 1, -5, 4]], 6, True),
        case([[1]], 1, True),
        case([[5, 4, -1, 7, 8]], 23, True),
        case([[-3, -2, -5]], -2),
    ]
    for n in (10_000, 100_000):
        nums = [rng.randint(-10_000, 10_000) for _ in range(n)]
        cases.append(case([nums], max_sub(nums)))
    return cases


def gen_longest_substring():
    samples = ["abcabcbb", "bbbbb", "pwwkew"]
    cases = [case([s], longest_unique(s), True) for s in samples]
    for s in ["", " ", "dvdf", "abba", "tmmzuxt"]:
        cases.append(case([s], longest_unique(s)))
    alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 !?"
    for n in (5_000, 50_000):
        s = "".join(rng.choice(alphabet) for _ in range(n))
        cases.append(case([s], longest_unique(s)))
    return cases


def gen_group_anagrams():
    cases = [
        case([["eat", "tea", "tan", "ate", "nat", "bat"]], [["bat"], ["nat", "tan"], ["ate", "eat", "tea"]], True),
        case([[""]], [[""]], True),
        case([["a"]], [["a"]], True),
        case([["abc", "bca", "cab", "xyz", "zyx", "q"]], group_anagrams(["abc", "bca", "cab", "xyz", "zyx", "q"])),
    ]
    words = []
    bases = ["".join(rng.choice("abcdefghijklmnopqrstuvwxyz") for _ in range(rng.randint(1, 10)))
             for _ in range(3_000)]
    for _ in range(20_000):
        w = list(rng.choice(bases))
        rng.shuffle(w)
        words.append("".join(w))
    cases.append(case([words], group_anagrams(words)))
    return cases


def gen_merge_intervals():
    cases = [
        case([[[1, 3], [2, 6], [8, 10], [15, 18]]], [[1, 6], [8, 10], [15, 18]], True),
        case([[[1, 4], [4, 5]]], [[1, 5]], True),
        case([[[1, 4], [0, 4]]], [[0, 4]]),
        case([[[1, 4], [2, 3]]], [[1, 4]]),
        case([[[5, 7]]], [[5, 7]]),
    ]
    for n in (5_000, 20_000):
        iv = []
        for _ in range(n):
            a = rng.randint(0, 1_000_000)
            iv.append([a, a + rng.randint(0, 40)])
        cases.append(case([iv], merge(iv)))
    return cases


def gen_product():
    cases = [
        case([[1, 2, 3, 4]], [24, 12, 8, 6], True),
        case([[-1, 1, 0, -3, 3]], [0, 0, 9, 0, 0], True),
        case([[2, 3]], [3, 2]),
        case([[0, 0, 5]], [0, 0, 0]),
    ]
    for n in (10_000, 50_000):
        nums = [rng.choice([-1, 1, 1, 1, 2, -2]) if rng.random() < 0.0005 else rng.choice([-1, 1]) for _ in range(n)]
        cases.append(case([nums], product_except_self(nums)))
    return cases


def gen_trap():
    cases = [
        case([[0, 1, 0, 2, 1, 0, 1, 3, 2, 1, 2, 1]], 6, True),
        case([[4, 2, 0, 3, 2, 5]], 9, True),
        case([[1]], 0),
        case([[5, 4, 3, 2, 1]], 0),
        case([[2, 0, 2]], 2),
    ]
    for n in (10_000, 20_000):
        h = [rng.randint(0, 100_000) for _ in range(n)]
        cases.append(case([h], trap(h)))
    return cases


def gen_median():
    cases = [
        case([[1, 3], [2]], 2.0, True),
        case([[1, 2], [3, 4]], 2.5, True),
        case([[], [1]], 1.0),
        case([[2], []], 2.0),
        case([[0, 0], [0, 0]], 0.0),
    ]
    for n, m in ((1_000, 999), (50_000, 40_001)):
        a = sorted(rng.randint(-1_000_000, 1_000_000) for _ in range(n))
        b = sorted(rng.randint(-1_000_000, 1_000_000) for _ in range(m))
        cases.append(case([a, b], median(a, b)))
    return cases


def gen_rotate():
    cases = [
        case([[1, 2, 3, 4, 5, 6, 7], 3], [5, 6, 7, 1, 2, 3, 4], True),
        case([[-1, -100, 3, 99], 2], [3, 99, -1, -100], True),
        case([[1], 5], [1]),
        case([[1, 2], 3], [2, 1]),
        case([[1, 2, 3], 0], [1, 2, 3]),
    ]
    for n in (10_000, 50_000):
        nums = [rng.randint(-1_000, 1_000) for _ in range(n)]
        k = rng.randint(0, 10 * n)
        cases.append(case([nums, k], rotate(nums, k)))
    return cases


GENERATORS = {
    "two-sum": gen_two_sum,
    "valid-parentheses": gen_valid_parentheses,
    "best-time-to-buy-and-sell-stock": gen_stock,
    "climbing-stairs": gen_climb,
    "maximum-subarray": gen_max_subarray,
    "longest-substring-without-repeating-characters": gen_longest_substring,
    "group-anagrams": gen_group_anagrams,
    "merge-intervals": gen_merge_intervals,
    "product-of-array-except-self": gen_product,
    "rotate-array": gen_rotate,
    "trapping-rain-water": gen_trap,
    "median-of-two-sorted-arrays": gen_median,
}

if __name__ == "__main__":
    for slug, gen in GENERATORS.items():
        (ROOT / slug).mkdir(parents=True, exist_ok=True)
        write(slug, gen())
