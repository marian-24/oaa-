package Model;

/**
 * Calcula el rank final de una partida estilo osu!
 *
 * Basado en accuracy (porcentaje de hits correctos sobre total de notas):
 *   S  → 95% o más
 *   A  → 90% - 94%
 *   B  → 80% - 89%
 *   C  → 70% - 79%
 *   D  → menos del 70%
 */
public class RankCalculator {

    public enum Rank { S, A, B, C, D }

    /**
     * Calcula el rank a partir de hits y misses totales.
     * @param hits   cantidad de notas clickeadas correctamente
     * @param misses cantidad de notas perdidas o trampas clickeadas
     */
    public static Rank calculate(int hits, int misses) {
        int total = hits + misses;
        if (total == 0) return Rank.D;

        double accuracy = (double) hits / total;

        if (accuracy >= 0.95) return Rank.S;
        if (accuracy >= 0.90) return Rank.A;
        if (accuracy >= 0.80) return Rank.B;
        if (accuracy >= 0.70) return Rank.C;
        return Rank.D;
    }

    /** Devuelve el color asociado al rank para la UI */
    public static String colorFor(Rank rank) {
        return switch (rank) {
            case S -> "#FFD700";   // dorado
            case A -> "#00BFFF";   // azul
            case B -> "#00E676";   // verde
            case C -> "#FF9800";   // naranja
            case D -> "#FF4444";   // rojo
        };
    }

    /** Devuelve el porcentaje de accuracy como string "94.5%" */
    public static String accuracyText(int hits, int misses) {
        int total = hits + misses;
        if (total == 0) return "0.0%";
        double acc = (double) hits / total * 100.0;
        return String.format("%.1f%%", acc);
    }
}