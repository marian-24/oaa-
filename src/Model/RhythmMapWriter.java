package Model;

import java.io.*;
import java.util.List;

/**
 * Escribe y lee archivos .rhythmmap
 *
 * Formato:
 *   AUDIO:nombre_del_archivo.wav
 *   BEAT:1523          ← tiempo en ms desde el inicio
 *   BEAT:2045
 *   ...
 */
public class RhythmMapWriter {

    public static final String EXTENSION = ".rhythmmap";

    // ------------------------------------------------------------------ //
    //  Escritura
    // ------------------------------------------------------------------ //

    /**
     * Guarda un mapa en la misma carpeta que el audio.
     * El nombre del archivo .rhythmmap es igual al audio pero con otra extensión.
     *
     * @param audioFile  archivo de audio de referencia
     * @param beatsMs    lista de tiempos de beat en milisegundos
     * @return el archivo .rhythmmap creado
     */
    public static File write(File audioFile, List<Long> beatsMs) throws IOException {
        String baseName = audioFile.getName();
        int dot = baseName.lastIndexOf('.');
        String mapName = (dot > 0 ? baseName.substring(0, dot) : baseName) + EXTENSION;

        // Guardar siempre en la carpeta maps/ separada de los audios
        File mapsFolder = Model.SongLibrary.getMapsFolder();
        File mapFile = new File(mapsFolder, mapName);

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(mapFile))) {
            bw.write("AUDIO:" + audioFile.getName());
            bw.newLine();
            for (long ms : beatsMs) {
                bw.write("BEAT:" + ms);
                bw.newLine();
            }
        }

        return mapFile;
    }

    // ------------------------------------------------------------------ //
    //  Lectura
    // ------------------------------------------------------------------ //

    /**
     * Lee un archivo .rhythmmap y devuelve los tiempos de beat en nanosegundos
     * (que es lo que espera SongEngine).
     */
    public static LoadedMap read(File mapFile) throws IOException {
        String audioName = null;
        java.util.List<Long> beatsNs = new java.util.ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(mapFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("AUDIO:")) {
                    audioName = line.substring(6).trim();
                } else if (line.startsWith("BEAT:")) {
                    long ms = Long.parseLong(line.substring(5).trim());
                    beatsNs.add(ms * 1_000_000L);   // ms → ns
                }
            }
        }

        if (audioName == null) throw new IOException("Missing AUDIO line in map file");

        // El mapa está en songs/maps/, el audio en songs/
        // Buscar primero en la carpeta padre (songs/), luego en la misma carpeta
        File mapsFolder  = mapFile.getParentFile();
        File songsFolder = mapsFolder.getParentFile();
        File audioFile   = new File(songsFolder, audioName);
        if (!audioFile.exists()) audioFile = new File(mapsFolder, audioName);
        if (!audioFile.exists()) {
            throw new IOException("Audio file not found: " + audioFile.getAbsolutePath());
        }

        return new LoadedMap(audioFile, beatsNs);
    }

    public record LoadedMap(File audioFile, List<Long> beatsNs) {}
}