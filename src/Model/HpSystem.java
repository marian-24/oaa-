package Model;

/**
 * Sistema de HP continuo estilo osu!
 *
 * - El HP drena constantemente con el tiempo
 * - Cada hit GREAT recupera más HP que un GOOD
 * - Un miss baja HP significativamente
 * - Si llega a 0 → game over
 * - HP va de 0.0 a 1.0 (la vista lo convierte a píxeles)
 */
public class HpSystem {

    // HP inicial y máximo
    public static final double MAX_HP = 1.0;

    // Drain por nanosegundo (moderado)
    // 1.0 / 60_000_000_000L = se vaciaría en 60 segundos sin clickear nada
    private static final double DRAIN_PER_NS = 1.0 / 60_000_000_000.0;

    // Recuperación por hit
    private static final double RECOVER_GREAT = 0.12;
    private static final double RECOVER_GOOD  = 0.06;

    // Penalización por miss
    private static final double PENALTY_MISS  = 0.15;

    // ------------------------------------------------------------------ //

    private double hp = MAX_HP;
    private long lastUpdateNs = -1;
    private boolean dead = false;

    // ------------------------------------------------------------------ //

    /**
     * Debe llamarse cada frame con el tiempo actual en nanosegundos.
     * Aplica el drain continuo y detecta si el HP llegó a 0.
     */
    public void update(long nowNs) {
        if (dead) return;

        if (lastUpdateNs < 0) {
            lastUpdateNs = nowNs;
            return;
        }

        long delta = nowNs - lastUpdateNs;
        lastUpdateNs = nowNs;

        hp -= DRAIN_PER_NS * delta;

        if (hp <= 0) {
            hp   = 0;
            dead = true;
        }
    }

    public void registerHit(UI.GameView.HitRating rating) {
        if (dead) return;
        hp += rating == UI.GameView.HitRating.GREAT ? RECOVER_GREAT : RECOVER_GOOD;
        if (hp > MAX_HP) hp = MAX_HP;
    }

    public void registerMiss() {
        if (dead) return;
        hp -= PENALTY_MISS;
        if (hp <= 0) {
            hp   = 0;
            dead = true;
        }
    }

    public double getHp()     { return hp; }
    public boolean isDead()   { return dead; }
}