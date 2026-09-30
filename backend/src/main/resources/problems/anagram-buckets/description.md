Two words are **anagrams** when one is a reshuffle of the other's letters. Sort the words of `strs` into buckets so that every bucket holds words that are all anagrams of each other, and anagrams never end up in different buckets.

Return the buckets. The order of the buckets and the order of the words inside a bucket do not matter.

### Example
```
strs   = ["listen", "stone", "silent", "notes", "enlist", "cat"]
answer = [["listen", "silent", "enlist"], ["stone", "notes"], ["cat"]]
```

### Constraints
- `1 <= strs.length <= 2 * 10^4`
- `0 <= strs[i].length <= 100`, lowercase English letters only.
