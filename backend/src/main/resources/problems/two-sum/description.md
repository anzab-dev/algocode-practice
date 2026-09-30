Given an array of integers `nums` and an integer `target`, return the **indices** of the two numbers that add up to `target`.

Each input has exactly one solution, and you may not use the same element twice. The two indices may be returned in either order.

### Example 1
```
nums = [2, 7, 11, 15], target = 9
answer: [0, 1]   (2 + 7 = 9)
```

### Example 2
```
nums = [3, 2, 4], target = 6
answer: [1, 2]
```

### Constraints
- `2 <= nums.length <= 10^5`
- `-10^9 <= nums[i], target <= 10^9`
- Exactly one valid answer exists.

**Follow-up:** the obvious solution is O(n²). Can you get it to O(n) time?
