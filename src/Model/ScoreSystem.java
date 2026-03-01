package Model;

import UI.GameView.HitRating;

public class ScoreSystem {

    private int score;
    private int combo;
    private int maxCombo;
    private int hits;
    private int misses;

    // Puntos base por tipo de hit
    private static final int POINTS_GREAT = 300;
    private static final int POINTS_GOOD  = 100;

    // ------------------------------------------------------------------ //

    /**
     * Registra un hit con rating específico.
     * GREAT → 300 pts base + bonus de combo
     * GOOD  → 100 pts base + bonus de combo
     */
    public void registerHit(HitRating rating) {
        hits++;
        combo++;
        if (combo > maxCombo) maxCombo = combo;

        int base = rating == HitRating.GREAT ? POINTS_GREAT : POINTS_GOOD;
        score += base + (combo * 10);
    }

    /** Retrocompatibilidad — cuenta como GOOD */
    public void registerHit() {
        registerHit(HitRating.GOOD);
    }

    public void registerMiss() {
        misses++;
        combo = 0;
    }

    // ------------------------------------------------------------------ //

    public int getScore()    { return score; }
    public int getCombo()    { return combo; }
    public int getMaxCombo() { return maxCombo; }
    public int getHits()     { return hits; }
    public int getMisses()   { return misses; }
}