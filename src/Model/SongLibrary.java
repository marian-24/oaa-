package Model;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Administra la librería de canciones locales.
 *
 * Lee todos los archivos WAV/MP3/AIFF de la carpeta songs/
 * (relativa al directorio de ejecución del juego).
 * Si la carpeta no existe, la crea automáticamente.
 */
public class SongLibrary {

    private static final String SONGS_FOLDER = "C:/Users/maria/OneDrive/Escritorio/songs";
    private static final String[] SUPPORTED = {".wav", ".mp3", ".aiff"};

    // ------------------------------------------------------------------ //

    /**
     * Escanea la carpeta songs/ y devuelve todas las canciones encontradas.
     * Cada entrada incluye nombre y duración calculada.
     */
    public static List<SongEntry> loadAll() {
        List<SongEntry> entries = new ArrayList<>();

        File folder = getSongsFolder();
        File[] files = folder.listFiles(SongLibrary::isAudioFile);

        if (files == null) return entries;

        for (File file : files) {
            long duration = getDurationSeconds(file);
            String name = SongEntry.cleanName(file);
            entries.add(new SongEntry(file, name, duration));
        }

        // Ordenar alfabéticamente por nombre
        entries.sort((a, b) -> a.displayName().compareToIgnoreCase(b.displayName()));

        return entries;
    }

    /** Devuelve (y crea si no existe) la carpeta songs/ */
    public static File getSongsFolder() {
        File folder = new File(SONGS_FOLDER);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return folder;
    }

    public static String getSongsFolderPath() {
        return getSongsFolder().getAbsolutePath();
    }

    // ------------------------------------------------------------------ //
    //  Privados
    // ------------------------------------------------------------------ //

    private static boolean isAudioFile(File file) {
        if (!file.isFile()) return false;
        String lower = file.getName().toLowerCase();
        for (String ext : SUPPORTED) {
            if (lower.endsWith(ext)) return true;
        }
        return false;
    }

    /**
     * Calcula la duración en segundos del archivo de audio.
     * Usa AudioSystem para WAV/AIFF. Para MP3 sin librería extra
     * devuelve 0 si no puede leerlo.
     */
    private static long getDurationSeconds(File file) {
        try {
            AudioInputStream stream = AudioSystem.getAudioInputStream(file);
            AudioFormat format = stream.getFormat();
            long frames = stream.getFrameLength();
            stream.close();

            if (frames > 0 && format.getFrameRate() > 0) {
                return (long) (frames / format.getFrameRate());
            }
        } catch (Exception ignored) {
            // MP3 sin librería o archivo corrupto → duración desconocida
        }
        return 0;
    }
}