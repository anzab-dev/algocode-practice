class Solution {
    public int bestProfit(int[] prices) {
        int best = 0;
        int low = Integer.MAX_VALUE;
        for (int p : prices) {
            low = Math.min(low, p);
            best = Math.max(best, p - low);
        }
        return best;
    }
}
