You get a list of whole numbers `nums` and a number `target`. Exactly two entries of `nums`, at different positions, add up to `target`. Find them and return their **positions** (0-based) as a two-element array, in either order.

### Examples
```
nums = [5, 8, -2, 11], target = 9    -> [2, 3]   (-2 + 11 = 9)
nums = [6, 1, 6],      target = 12   -> [0, 2]   (the same value twice is fine, the same position is not)
```

### Constraints
- `2 <= nums.length <= 10^5`
- `-10^9 <= nums[i], target <= 10^9`
- There is always exactly one matching pair.

**Follow-up:** checking every pair takes O(n²). Can you find the pair in a single pass?
