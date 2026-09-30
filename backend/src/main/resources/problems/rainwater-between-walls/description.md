Picture a row of walls, each one unit wide, where `height[i]` is the height of wall `i`. Rain fills every dip until the water would spill over the lower of the walls around it. Return how many units of water stay on top of the walls.

### Examples
```
[3, 0, 1, 0, 4]     -> 8   (3 + 2 + 3)
[2, 1, 3, 0, 1, 2]  -> 4
```

### Constraints
- `1 <= height.length <= 2 * 10^4`
- `0 <= height[i] <= 10^5`
