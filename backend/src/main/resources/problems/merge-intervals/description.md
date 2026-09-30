Each element of `intervals` is a pair `[start, end]`. Merge all overlapping intervals (touching ones count as overlapping) and return the result. The merged intervals may be returned in any order.

### Examples
```
[[1,3],[2,6],[8,10],[15,18]] -> [[1,6],[8,10],[15,18]]
[[1,4],[4,5]]                -> [[1,5]]
```

### Constraints
- `1 <= intervals.length <= 10^4`
- `0 <= start <= end <= 10^6`
