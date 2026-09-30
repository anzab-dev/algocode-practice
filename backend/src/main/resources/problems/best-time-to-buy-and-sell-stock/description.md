`prices[i]` is the price of a stock on day `i`. You may buy once and sell once, on a **later** day.

Return the largest profit you can make, or `0` if no trade makes money.

### Examples
```
prices = [7, 1, 5, 3, 6, 4]  -> 5   (buy at 1, sell at 6)
prices = [7, 6, 4, 3, 1]     -> 0   (prices only fall)
```

### Constraints
- `1 <= prices.length <= 10^5`
- `0 <= prices[i] <= 10^4`
