`nums1` and `nums2` are each sorted from smallest to largest. Treat them as one pool of numbers and return that pool's **median**: the middle value, or the mean of the two middle values when the pool has an even size. Answers within `1e-5` of the exact value are accepted.

### Examples
```
nums1 = [1, 4, 9],  nums2 = [2, 3]    -> 3.0   (pool 1 2 3 4 9)
nums1 = [-5, 0],    nums2 = [5, 10]   -> 2.5   ((0 + 5) / 2)
```

### Constraints
- `0 <= nums1.length, nums2.length <= 10^5`, and the pool is never empty.
- `-10^6 <= nums1[i], nums2[i] <= 10^6`

**Follow-up:** merging is O(m + n). Can you reach O(log(m + n))?
