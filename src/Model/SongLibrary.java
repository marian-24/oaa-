package Model;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SongLibrary {

    private static final String SONGS_FOLDER = "C:/Users/maria/OneDrive/Escritorio/songs";
    private static final String MAPS_FOLDER  = "C:/Users/maria/OneDrive/Escritorio/songs/maps";

    private static final String[] AUDIO_EXTS = {".wav", ".mp3", ".aiff"};

    // ------------------------------------------------------------------ //
    //  Canciones de audio (para Song Mode — beat detection automático)
    // ------------------------------------------------------------------ //

    public static List<SongEntry> loadAll() {
        List<SongEntry> entries = new ArrayList<>();
        File[] files = getSongsFolder().listFiles(SongLibrary::isAudioFile);
        if (files == null) return entries;
        for (File f : files) {
            entries.add(new SongEntry(f, SongEntry.cleanName(f), getDurationSeconds(f)));
        }
        entries.sort((a, b) -> a.displayName().compareToIgnoreCase(b.displayName()));
        return entries;
    }

    // ------------------------------------------------------------------ //
    //  Mapas manuales (para Map Editor — archivos .rhythmmap)
    // ------------------------------------------------------------------ //

    public static List<SongEntry> loadMaps() {
        List<SongEntry> entries = new ArrayList<>();
        File[] files = getMapsFolder().listFiles(
                f -> f.isFile() && f.getName().toLowerCase().endsWith(".rhythmmap"));
        if (files == null) return entries;
        for (File f : files) {
            entries.add(new SongEntry(f, SongEntry.cleanName(f), 0));
        }
        entries.sort((a, b) -> a.displayName().compareToIgnoreCase(b.displayName()));
        return entries;
    }

    // ------------------------------------------------------------------ //
    //  Carpetas
    // ------------------------------------------------------------------ //

    public static File getSongsFolder() {
        File f = new File(SONGS_FOLDER);
        if (!f.exists()) f.mkdirs();
        return f;
    }

    public static File getMapsFolder() {
        File f = new File(MAPS_FOLDER);
        if (!f.exists()) f.mkdirs();
        return f;
    }

    public static String getSongsFolderPath() { return getSongsFolder().getAbsolutePath(); }
    public static String getMapsFolderPath()  { return getMapsFolder().getAbsolutePath(); }

    // ------------------------------------------------------------------ //

    private static boolean isAudioFile(File file) {
        if (!file.isFile()) return false;
        String lower = file.getName().toLowerCase();
        for (String ext : AUDIO_EXTS) if (lower.endsWith(ext)) return true;
        return false;
    }

    private static long getDurationSeconds(File file) {
        try {
            AudioInputStream s = AudioSystem.getAudioInputStream(file);
            AudioFormat fmt = s.getFormat();
            long frames = s.getFrameLength();
            s.close();
            if (frames > 0 && fmt.getFrameRate() > 0)
                return (long)(frames / fmt.getFrameRate());
        } catch (Exception ignored) {}
        return 0;
    }
}