import java.util.HashMap;
import java.util.Map;

class Solution {
    public int lengthOfLongestSubstring(String s) {
        Map<Character, Integer> last = new HashMap<>();
        int start = 0;
        int best = 0;
        for (int i = 0; i < s.length(); i++) {
            Integer seen = last.put(s.charAt(i), i);
            if (seen != null && seen >= start) {
                start = seen + 1;
            }
            best = Math.max(best, i - start + 1);
        }
        return best;
    }
}
