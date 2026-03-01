package Model;

import java.io.File;

/**
 * Representa una canción en la librería.
 * La duración se calcula al crear la entrada.
 */

//la diferencia con una clase es que no tengo que definir
// todo el constructor, getters, equals, hashcode, toString, etc. El record lo hace por mi
public record SongEntry(File file, String displayName, long durationSeconds) {

    /** Nombre limpio sin extensión para mostrar en la UI */
    public static String cleanName(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    /** Formatea la duración como "m:ss" */
    public String formattedDuration() {
        long mins = durationSeconds / 60;
        long secs = durationSeconds % 60;
        return mins + ":" + String.format("%02d", secs);
    }
}