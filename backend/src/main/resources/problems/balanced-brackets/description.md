The string `s` is made only of the bracket characters `( ) [ ] { }`. Return `true` when the brackets are **properly nested**, which means:

- each closing bracket closes the most recent bracket that is still open,
- both brackets of a pair have the same shape,
- nothing is left open at the end, and nothing is closed that was never opened.

### Examples
```
"[()]{}"  -> true
"{(})"    -> false   ("}" arrives while "(" is still open)
"(("      -> false   (never closed)
```

### Constraints
- `1 <= s.length <= 10^5`
