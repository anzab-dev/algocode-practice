A string `s` contains only the characters `(`, `)`, `[`, `]`, `{` and `}`. Decide whether it is **balanced**:

- every opening bracket is closed by a bracket of the same type,
- brackets close in the reverse order they were opened,
- every closing bracket has a matching opening bracket.

### Examples
```
"()"      -> true
"()[]{}"  -> true
"(]"      -> false
"([)]"    -> false
"{[]}"    -> true
```

### Constraints
- `1 <= s.length <= 10^5`
