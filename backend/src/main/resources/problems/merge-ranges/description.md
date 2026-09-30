Every entry of `intervals` is a closed range `[start, end]`. The ranges arrive in no particular order. Combine each group of ranges that overlap or share an endpoint into one range, and return the combined list in any order.

### Examples
```
[[5,9],[1,2],[8,12],[2,3]] -> [[1,3],[5,12]]
[[0,0],[1,1]]              -> [[0,0],[1,1]]   (a gap between 0 and 1, so nothing merges)
```

### Constraints
- `1 <= intervals.length <= 10^4`
- `0 <= start <= end <= 10^6`
