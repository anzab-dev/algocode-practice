package dev.algopractice.problem;

public enum Difficulty {
    EASY(10), MEDIUM(25), HARD(50);

    private final int xp;

    Difficulty(int xp) {
        this.xp = xp;
    }

    /** Experience points awarded for the first accepted solution. */
    public int xp() {
        return xp;
    }
}
