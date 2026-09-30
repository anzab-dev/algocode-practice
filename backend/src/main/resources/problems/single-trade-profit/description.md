`prices[d]` is what one share costs on day `d`. You are allowed a single round trip: buy on one day, then sell on a **strictly later** day.

Return the biggest gain you can lock in. If every possible trade loses money, stay out and return `0`.

### Examples
```
prices = [9, 4, 6, 2, 8, 5]  -> 6   (buy on day 3 at 2, sell on day 4 at 8)
prices = [10, 8, 8, 3]       -> 0   (the price never rises)
```

### Constraints
- `1 <= prices.length <= 10^5`
- `0 <= prices[d] <= 10^4`
