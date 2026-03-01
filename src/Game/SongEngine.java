package Game;

import Model.Note;
import Model.NoteState;
import Model.ScoreSystem;
import UI.GameView.HitRating;

import javax.sound.sampled.*;
import java.io.File;
import java.util.*;

/**
 * Engine para el modo canción.
 *
 * Diferencias con GameEngine (modo arcade):
 *  - Las notas aparecen en los tiempos de beat detectados, no aleatoriamente
 *  - El juego termina cuando se acaban los beats (no por un miss)
 *  - Un miss no termina el juego, solo rompe el combo y resta vida
 *  - Reproduce el audio en paralelo sincronizado con el gameTime
 *  - No hay DifficultyManager (la canción define el ritmo)
 */
public class SongEngine {

    // Duración visible de cada nota en nanosegundos
    private static final long NOTE_DURATION = 1_500_000_000L; // 1.5 seg

    // Radio de hit
    private static final double HIT_RADIUS = 30;

    // ------------------------------------------------------------------ //

    private final List<Long> beatTimesNs;       // tiempos de beat del audio
    private final Queue<Note> activeNotes = new LinkedList<>();
    private final ScoreSystem scoreSystem  = new ScoreSystem();

    private long startTime;
    private boolean running = false;
    private boolean finished = false;

    // Vidas
    private static final int MAX_LIVES = 3;
    private int lives = MAX_LIVES;

    private int nextBeatIndex = 0;              // cuál beat spawneamos a continuación
    private Clip audioClip;                     // reproducción del audio

    // ------------------------------------------------------------------ //

    public SongEngine(List<Long> beatTimesNs, File audioFile) {
        this.beatTimesNs = beatTimesNs;
        loadAudio(audioFile);
    }

    // ------------------------------------------------------------------ //
    //  Ciclo de vida
    // ------------------------------------------------------------------ //

    public void start(long now) {
        startTime = now;
        running   = true;
        if (audioClip != null) audioClip.start();
    }

    public void update(long now) {
        if (!running) return;

        long gameTime = now - startTime;

        spawnBeats(gameTime);
        checkExpiredNotes(gameTime);
        checkSongFinished(gameTime);
    }

    public void stop() {
        running = false;
        if (audioClip != null) audioClip.stop();
    }

    // ------------------------------------------------------------------ //
    //  Lógica de notas
    // ------------------------------------------------------------------ //

    private void spawnBeats(long gameTime) {
        while (nextBeatIndex < beatTimesNs.size()
                && beatTimesNs.get(nextBeatIndex) <= gameTime) {

            double x = 80 + Math.random() * 640;
            double y = 80 + Math.random() * 400;
            boolean trap = Math.random() < 0.20;  // 20% de chances de trampa

            Note note = new Note(x, y, gameTime, NOTE_DURATION, trap);
            note.setState(NoteState.ACTIVE);
            activeNotes.add(note);
            nextBeatIndex++;
        }
    }

    private void checkExpiredNotes(long gameTime) {
        Iterator<Note> it = activeNotes.iterator();
        while (it.hasNext()) {
            Note note = it.next();
            if (note.isExpired(gameTime)) {
                note.setState(NoteState.MISSED);
                it.remove();

                if (!note.isTrap()) {
                    // Solo penalizar si era una nota normal, no una trampa
                    scoreSystem.registerMiss();
                    lives--;
                    if (lives <= 0) {
                        lives = 0;
                        running = false;
                        if (audioClip != null) audioClip.stop();
                    }
                }
            }
        }
    }

    private void checkSongFinished(long gameTime) {
        // La canción terminó si ya spawneamos todos los beats y no quedan notas activas
        if (nextBeatIndex >= beatTimesNs.size() && activeNotes.isEmpty()) {
            running  = false;
            finished = true;
            if (audioClip != null) audioClip.stop();
        }
    }

    // ------------------------------------------------------------------ //
    //  Input
    // ------------------------------------------------------------------ //

    public HitResult checkHitWithDistance(double x, double y) {
        if (!running) return null;

        Iterator<Note> it = activeNotes.iterator();
        while (it.hasNext()) {
            Note note = it.next();
            if (note.getState() == NoteState.ACTIVE && note.isInside(x, y, HIT_RADIUS)) {
                double dx = x - note.getX();
                double dy = y - note.getY();
                double distance = Math.sqrt(dx * dx + dy * dy);

                note.setState(NoteState.HIT);
                it.remove();
                return new HitResult(distance, note.getX(), note.getY(), note.isTrap());
            }
        }
        return null;
    }

    public void registerHit(HitRating rating) {
        scoreSystem.registerHit(rating);
    }

    /** Penaliza al jugador por clickear una trampa: pierde una vida */
    public void registerTrapHit() {
        scoreSystem.registerMiss();
        lives--;
        if (lives <= 0) {
            lives = 0;
            running = false;
            if (audioClip != null) audioClip.stop();
        }
    }

    // ------------------------------------------------------------------ //
    //  Audio
    // ------------------------------------------------------------------ //

    private void loadAudio(File audioFile) {
        if (audioFile == null) return;
        try {
            AudioInputStream stream = AudioSystem.getAudioInputStream(audioFile);
            audioClip = AudioSystem.getClip();
            audioClip.open(stream);
        } catch (Exception e) {
            System.err.println("No se pudo cargar el audio: " + e.getMessage());
            audioClip = null;
        }
    }

    // ------------------------------------------------------------------ //
    //  Getters
    // ------------------------------------------------------------------ //

    public List<Note> getActiveNotes()  { return new ArrayList<>(activeNotes); }
    public ScoreSystem getScoreSystem() { return scoreSystem; }
    public boolean isRunning()          { return running; }
    public boolean isFinished()         { return finished; }
    public boolean isGameOver()         { return !running; }
    public int getLives()               { return lives; }
    public int getMaxLives()            { return MAX_LIVES; }

    // ------------------------------------------------------------------ //
    //  Records de resultado
    // ------------------------------------------------------------------ //

    public record HitResult(double distance, double noteX, double noteY, boolean trap) {}
}