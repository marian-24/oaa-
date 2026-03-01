package Game;

import javax.sound.sampled.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Detector de beats mejorado con dos técnicas:
 *
 * 1. FILTRO PASO-BAJO: antes de calcular la energía, cada muestra pasa por
 *    un filtro IIR simple que atenúa frecuencias por encima de ~200Hz.
 *    Esto hace que el detector responda principalmente al bombo/kick y no
 *    a hi-hats, voces ni instrumentos agudos.
 *
 * 2. OFFSET DE ANTICIPACIÓN: los beats detectados se adelantan 300ms para
 *    que la nota aparezca antes del golpe real, dándole tiempo al jugador
 *    de reaccionar y clickear justo en el beat.
 */
public class BeatDetector {

    // Umbral: cuántas veces sobre el promedio local debe estar la energía filtrada
    private static final double THRESHOLD = 1.6;

    // Ventana de análisis: ~10ms a 44100 Hz
    private static final int WINDOW_SIZE = 441;

    // Historia para promedio local (~2 segundos = más estabilidad)
    private static final int HISTORY_SIZE = 200;

    // Cooldown mínimo entre beats detectados
    private static final long MIN_BEAT_GAP_NS = 350_000_000L; // 350ms (~170 BPM máx)

    // Offset de anticipación: la nota aparece 300ms antes del beat
    private static final long ANTICIPATION_NS = 300_000_000L;

    // Coeficiente del filtro paso-bajo IIR
    // Fórmula: alpha = dt / (RC + dt), donde RC = 1/(2π*fc), fc = frecuencia de corte
    // Con fc=200Hz y dt=1/44100: alpha ≈ 0.028
    private static final double LP_ALPHA = 0.028;

    // ------------------------------------------------------------------ //

    /**
     * Analiza el archivo de audio y devuelve una lista de tiempos de beats
     * en nanosegundos, ajustados con el offset de anticipación.
     */
    public static List<Long> detect(File audioFile) throws Exception {

        List<Long> beats = new ArrayList<>();

        AudioInputStream rawStream = AudioSystem.getAudioInputStream(audioFile);
        AudioFormat baseFormat = rawStream.getFormat();

        // Convertir a PCM mono 44100 Hz 16-bit
        AudioFormat targetFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                44100, 16, 1, 2, 44100, false
        );

        AudioInputStream pcmStream = rawStream;
        if (!baseFormat.matches(targetFormat)) {
            pcmStream = AudioSystem.getAudioInputStream(targetFormat, rawStream);
        }

        byte[] window      = new byte[WINDOW_SIZE * 2];
        double[] history   = new double[HISTORY_SIZE];
        int historyIndex   = 0;
        long windowIndex   = 0;
        long lastBeatNs    = -MIN_BEAT_GAP_NS;
        double lpPrev      = 0.0;  // valor anterior del filtro paso-bajo

        int bytesRead;
        while ((bytesRead = pcmStream.read(window)) != -1) {

            int samples = bytesRead / 2;

            // --- Paso 1: aplicar filtro paso-bajo a cada muestra ---
            // y acumular la energía de las muestras filtradas
            double energy = 0;
            for (int i = 0; i < samples; i++) {
                short raw = (short) ((window[i * 2 + 1] << 8) | (window[i * 2] & 0xFF));

                // Filtro IIR de primer orden: y[n] = α·x[n] + (1-α)·y[n-1]
                // Solo pasan frecuencias bajas (bombo, bajo)
                double filtered = LP_ALPHA * raw + (1.0 - LP_ALPHA) * lpPrev;
                lpPrev = filtered;

                energy += filtered * filtered;
            }
            energy = Math.sqrt(energy / samples);

            // --- Paso 2: comparar contra promedio local ---
            history[historyIndex % HISTORY_SIZE] = energy;
            historyIndex++;

            double avgEnergy = 0;
            int filled = Math.min(historyIndex, HISTORY_SIZE);
            for (int i = 0; i < filled; i++) avgEnergy += history[i];
            avgEnergy /= filled;

            // Tiempo real en el audio
            long timeNs = (windowIndex * WINDOW_SIZE * 1_000_000_000L) / 44100;

            // --- Paso 3: detectar beat y aplicar offset de anticipación ---
            if (energy > avgEnergy * THRESHOLD && (timeNs - lastBeatNs) >= MIN_BEAT_GAP_NS) {
                // Restamos el offset para que la nota aparezca antes del beat
                long spawnTime = Math.max(0, timeNs - ANTICIPATION_NS);
                beats.add(spawnTime);
                lastBeatNs = timeNs;
            }

            windowIndex++;
        }

        pcmStream.close();
        return beats;
    }
}