package Game;

import javax.sound.sampled.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Detector de beats simple basado en cambios de energía de audio.
 *
 * Algoritmo:
 *   1. Lee el audio como muestras PCM (mono, 44100 Hz)
 *   2. Divide en ventanas de ~10ms
 *   3. Calcula la energía RMS de cada ventana
 *   4. Si la energía supera el promedio local × THRESHOLD, detecta un beat
 *   5. Aplica cooldown mínimo entre beats para evitar duplicados
 */
public class BeatDetector {

    // Cuántas veces sobre el promedio local debe estar la energía para contar como beat
    // Valores de referencia: 1.4 = muy sensible, 1.7 = moderado, 2.0+ = solo beats muy fuertes
    private static final double THRESHOLD = 1.8;

    // Ventana de análisis: ~10ms a 44100 Hz
    private static final int WINDOW_SIZE = 441;

    // Cuántas ventanas usamos para calcular el promedio local (~1.5 segundos)
    // Más historia = promedio más estable = menos falsos positivos
    private static final int HISTORY_SIZE = 150;

    // Tiempo mínimo entre beats en nanosegundos (evita doble detección)
    // 500ms = máximo ~120 BPM
    private static final long MIN_BEAT_GAP_NS = 500_000_000L; // 500ms

    /**
     * Analiza el archivo de audio y devuelve una lista de tiempos de beats
     * expresados en nanosegundos desde el inicio del archivo.
     *
     * Soporta WAV directamente. Para MP3 se necesita una librería extra
     * (como JLayer/mp3spi), si no está disponible lanza una excepción.
     *
     * @param audioFile archivo de audio a analizar
     * @return lista de tiempos de beat en nanosegundos
     */
    public static List<Long> detect(File audioFile) throws Exception {

        List<Long> beats = new ArrayList<>();

        AudioInputStream rawStream = AudioSystem.getAudioInputStream(audioFile);
        AudioFormat baseFormat = rawStream.getFormat();

        // Convertir a PCM mono 44100 Hz 16-bit si hace falta
        AudioFormat targetFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                44100, 16, 1, 2, 44100, false
        );

        AudioInputStream pcmStream = rawStream;
        if (!baseFormat.matches(targetFormat)) {
            pcmStream = AudioSystem.getAudioInputStream(targetFormat, rawStream);
        }

        byte[] window = new byte[WINDOW_SIZE * 2]; // 2 bytes por sample (16-bit)
        double[] energyHistory = new double[HISTORY_SIZE];
        int historyIndex = 0;
        long windowIndex = 0;
        long lastBeatNs = -MIN_BEAT_GAP_NS;

        int bytesRead;
        while ((bytesRead = pcmStream.read(window)) != -1) {

            // Calcular energía RMS de la ventana
            double energy = 0;
            int samples = bytesRead / 2;
            for (int i = 0; i < samples; i++) {
                // Little-endian 16-bit signed
                short sample = (short) ((window[i * 2 + 1] << 8) | (window[i * 2] & 0xFF));
                energy += (double) sample * sample;
            }
            energy = Math.sqrt(energy / samples);

            // Promedio local de energía
            energyHistory[historyIndex % HISTORY_SIZE] = energy;
            historyIndex++;

            double avgEnergy = 0;
            int filled = Math.min(historyIndex, HISTORY_SIZE);
            for (int i = 0; i < filled; i++) avgEnergy += energyHistory[i];
            avgEnergy /= filled;

            // Tiempo actual en nanosegundos
            long timeNs = (windowIndex * WINDOW_SIZE * 1_000_000_000L) / 44100;

            // ¿Es un beat?
            if (energy > avgEnergy * THRESHOLD && (timeNs - lastBeatNs) >= MIN_BEAT_GAP_NS) {
                beats.add(timeNs);
                lastBeatNs = timeNs;
            }

            windowIndex++;
        }

        pcmStream.close();
        return beats;
    }
}