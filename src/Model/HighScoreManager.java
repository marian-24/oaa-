package Model;

import java.util.prefs.Preferences;

public class HighScoreManager {

    private static final String KEY_ARCADE     = "high_score_arcade";
    private static final String KEY_SONG       = "high_score_song";
    private static final String KEY_SONG_PREFIX = "song_hs_"; // + nombre del archivo

    private final Preferences prefs;

    public HighScoreManager() {
        prefs = Preferences.userNodeForPackage(HighScoreManager.class);
    }

    // ------------------------------------------------------------------ //
    //  High score por modo (Arcade / Song global)
    // ------------------------------------------------------------------ //

    public int getHighScore(GameMode mode) {
        return prefs.getInt(keyFor(mode), 0);
    }

    public void submitScore(int score, GameMode mode) {
        if (score > getHighScore(mode)) {
            prefs.putInt(keyFor(mode), score);
        }
    }

    // ------------------------------------------------------------------ //
    //  High score por canción individual
    // ------------------------------------------------------------------ //

    public int getSongHighScore(String songFileName) {
        return prefs.getInt(KEY_SONG_PREFIX + sanitize(songFileName), 0);
    }

    public void submitSongScore(int score, String songFileName) {
        String key = KEY_SONG_PREFIX + sanitize(songFileName);
        if (score > prefs.getInt(key, 0)) {
            prefs.putInt(key, score);
        }
    }

    // ------------------------------------------------------------------ //
    //  Reset
    // ------------------------------------------------------------------ //

    public void reset(GameMode mode) {
        prefs.remove(keyFor(mode));
    }

    public void resetSong(String songFileName) {
        prefs.remove(KEY_SONG_PREFIX + sanitize(songFileName));
    }

    // ------------------------------------------------------------------ //
    //  Privados
    // ------------------------------------------------------------------ //

    private String keyFor(GameMode mode) {
        return mode == GameMode.SONG ? KEY_SONG : KEY_ARCADE;
    }

    /** Limpia el nombre para usarlo como clave de Preferences (sin caracteres raros) */
    private String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9_\\-]", "_").toLowerCase();
    }
}