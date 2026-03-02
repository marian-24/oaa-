package Game;

import Model.HpSystem;
import Model.Note;
import Model.NoteState;
import Model.NoteType;
import Model.NoteType;
import Model.NoteType;
import Model.ScoreSystem;
import UI.GameView.HitRating;

import javax.sound.sampled.*;
import java.io.File;
import java.util.*;

public class SongEngine {

    private static final long NOTE_DURATION = 1_500_000_000L;
    private static final double HIT_RADIUS  = 30;

    // ------------------------------------------------------------------ //

    private final List<Long> beatTimesNs;
    private final Queue<Note> activeNotes = new LinkedList<>();
    private final ScoreSystem scoreSystem = new ScoreSystem();
    private final HpSystem hpSystem       = new HpSystem();

    private long startTime;
    private boolean running  = false;
    private boolean finished = false;

    private int nextBeatIndex = 0;
    private Clip audioClip;

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

        hpSystem.update(now);
        if (hpSystem.isDead()) {
            running  = false;
            finished = false;
            if (audioClip != null) audioClip.stop();
            return;
        }

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

            double roll = Math.random();
            Note note;
            if (roll < 0.10) {
                double speed = 80 + Math.random() * 80;
                double angle = Math.random() * Math.PI * 2;
                note = new Note(x, y, gameTime, NOTE_DURATION, NoteType.MOVING,
                        Math.cos(angle) * speed, Math.sin(angle) * speed);
            } else if (roll < 0.30) {
                note = new Note(x, y, gameTime, NOTE_DURATION, NoteType.TRAP);
            } else {
                note = new Note(x, y, gameTime, NOTE_DURATION, NoteType.NORMAL);
            }

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
                    scoreSystem.registerMiss();
                    hpSystem.registerMiss();
                }
            }
        }
    }

    private void checkSongFinished(long gameTime) {
        if (nextBeatIndex >= beatTimesNs.size() && activeNotes.isEmpty()) {
            running  = false;
            finished = true;
            if (audioClip != null) audioClip.stop();
        }
    }

    // ------------------------------------------------------------------ //
    //  Input
    // ------------------------------------------------------------------ //

    public HitResult checkHitWithDistance(double x, double y, long gameTime) {
        if (!running) return null;

        Iterator<Note> it = activeNotes.iterator();
        while (it.hasNext()) {
            Note note = it.next();
            if (note.getState() == NoteState.ACTIVE && note.isInside(x, y, HIT_RADIUS, gameTime)) {
                double cx = note.getX(gameTime);
                double cy = note.getY(gameTime);
                double distance = Math.sqrt((x-cx)*(x-cx)+(y-cy)*(y-cy));
                note.setState(NoteState.HIT);
                it.remove();
                return new HitResult(distance, cx, cy, note.isTrap());
            }
        }
        return null;
    }

    public void registerHit(HitRating rating) {
        scoreSystem.registerHit(rating);
        hpSystem.registerHit(rating);
    }

    /** Penaliza por clickear una trampa */
    public void registerTrapHit() {
        scoreSystem.registerMiss();
        hpSystem.registerMiss();
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
    public HpSystem getHpSystem()       { return hpSystem; }
    public boolean isRunning()          { return running; }
    public boolean isFinished()         { return finished; }
    public boolean isGameOver()         { return !running; }
    public long getGameTime(long now)   { return now - startTime; }

    // ------------------------------------------------------------------ //
    //  Records de resultado
    // ------------------------------------------------------------------ //

    public record HitResult(double distance, double noteX, double noteY, boolean trap) {}
}