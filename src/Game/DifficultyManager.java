package Game;

/**

 *   - spawnInterval  → se reduce (spawns más frecuentes)
 *   - noteDuration   → se reduce (círculos desaparecen más rápido)
 */
public class DifficultyManager {

    // Cada cuántos nanosegundos sube un nivel
    private static final long LEVEL_DURATION = 5_000_000_000L; // 10 segundos

    // Valores iniciales
    private static final long INITIAL_SPAWN_INTERVAL = 1_500_000_000L; // 1.5 seg entre spawns
    private static final long INITIAL_NOTE_DURATION  = 2_500_000_000L; // 2.5 seg para clickear

    // Cuánto se reduce por nivel (en nanosegundos)
    private static final long SPAWN_REDUCTION_PER_LEVEL = 100_000_000L; // -0.1 seg
    private static final long DURATION_REDUCTION_PER_LEVEL = 150_000_000L; // -0.15 seg

    // Mínimos para que el juego no se vuelva imposible
    private static final long MIN_SPAWN_INTERVAL = 400_000_000L;  // 0.4 seg
    private static final long MIN_NOTE_DURATION  = 700_000_000L;  // 0.7 seg

    private int currentLevel = 0;

    /**
     * Debe llamarse cada frame con el gameTime actual.
     * Actualiza el nivel si corresponde.
     */
    public void update(long gameTime) {
        int newLevel = (int) (gameTime / LEVEL_DURATION);
        currentLevel = newLevel;
    }

    public int getLevel() {
        return currentLevel;
    }

    public long getSpawnInterval() {
        long value = INITIAL_SPAWN_INTERVAL - (currentLevel * SPAWN_REDUCTION_PER_LEVEL);
        return Math.max(value, MIN_SPAWN_INTERVAL);
    }

    public long getNoteDuration() {
        long value = INITIAL_NOTE_DURATION - (currentLevel * DURATION_REDUCTION_PER_LEVEL);
        return Math.max(value, MIN_NOTE_DURATION);
    }
}