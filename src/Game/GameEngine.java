package Game;

import Model.Note;
import Model.NoteState;
import Model.ScoreSystem;

import java.util.*;

public class GameEngine {

    private final List<Note> allNotes;
    private final Queue<Note> activeNotes;
    private final ScoreSystem scoreSystem;
    private final DifficultyManager difficulty;

    private long startTime;
    private boolean running;
    private long lastSpawnTime = 0;

    public GameEngine(List<Note> notes) {
        this.allNotes    = notes;
        this.activeNotes = new LinkedList<>();
        this.scoreSystem = new ScoreSystem();
        this.difficulty  = new DifficultyManager();
        this.running     = false;
    }

    public void start(long now) {
        this.startTime = now;
        this.running   = true;
    }

    public void update(long now) {

        if (!running) return;

        long gameTime = now - startTime;

        // 1. Actualizar dificultad según tiempo
        difficulty.update(gameTime);

        // 2. Lógica de notas
        activateNotes(gameTime);
        spawnNote(gameTime);
        checkExpiredNotes(gameTime);
    }

    // ------------------------------------------------------------------ //

    private void activateNotes(long gameTime) {
        for (Note note : allNotes) {
            if (note.shouldActivate(gameTime)) {
                note.setState(NoteState.ACTIVE);
                activeNotes.add(note);
            }
        }
    }

    private void spawnNote(long gameTime) {

        if (gameTime - lastSpawnTime >= difficulty.getSpawnInterval()) {

            double x = 100 + Math.random() * 600;
            double y = 100 + Math.random() * 300;

            Note note = new Note(x, y, gameTime, difficulty.getNoteDuration());
            note.setState(NoteState.ACTIVE);
            activeNotes.add(note);

            lastSpawnTime = gameTime;
        }
    }

    private void checkExpiredNotes(long gameTime) {

        if (activeNotes.isEmpty()) return;

        Note current = activeNotes.peek();

        if (current.isExpired(gameTime)) {
            current.setState(NoteState.MISSED);
            activeNotes.poll();
            scoreSystem.registerMiss();
            running = false;
        }
    }

    // ------------------------------------------------------------------ //

    public boolean checkHit(double x, double y, long now) {
        return checkHitWithDistance(x, y, now) != null;
    }

    /** Devuelve la distancia al centro de la nota golpeada, o null si no hubo hit */
    public HitResult checkHitWithDistance(double x, double y, long now) {

        if (!running) return null;

        Iterator<Note> iterator = activeNotes.iterator();

        while (iterator.hasNext()) {
            Note note = iterator.next();

            if (note.getState() == NoteState.ACTIVE && note.isInside(x, y, 30)) {
                double dx = x - note.getX();
                double dy = y - note.getY();
                double distance = Math.sqrt(dx * dx + dy * dy);

                note.setState(NoteState.HIT);
                iterator.remove();
                // El controller llama a scoreSystem.registerHit(rating) con el rating correcto
                return new HitResult(distance);
            }
        }

        return null;
    }

    /** Resultado de un hit con la distancia al centro de la nota */
    public record HitResult(double distance) {}

    // ------------------------------------------------------------------ //

    public List<Note> getActiveNotes()   { return new ArrayList<>(activeNotes); }
    public ScoreSystem getScoreSystem()  { return scoreSystem; }
    public DifficultyManager getDifficulty() { return difficulty; }
    public boolean isRunning()           { return running; }
    public boolean isGameOver()          { return !running; }
}