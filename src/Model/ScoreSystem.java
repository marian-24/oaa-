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
    // Milestones de combo que disparan efectos especiales
    public static final int[] COMBO_MILESTONES = { 10, 25, 50, 100 };

    // Último milestone alcanzado (para que la vista sepa cuál mostrar)
    private int lastMilestone = 0;

    public void registerHit(HitRating rating) {
        hits++;
        combo++;
        if (combo > maxCombo) maxCombo = combo;

        int base = rating == HitRating.GREAT ? POINTS_GREAT : POINTS_GOOD;
        score += base + (combo * 10);

        // Chequear milestone
        for (int m : COMBO_MILESTONES) {
            if (combo == m) { lastMilestone = m; break; }
        }
    }

    /** Devuelve el milestone recién alcanzado y lo resetea (consume-once) */
    public int consumeMilestone() {
        int m = lastMilestone;
        lastMilestone = 0;
        return m;
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