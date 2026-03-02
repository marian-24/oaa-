package Game;

import Model.*;

import java.util.*;

public class GameEngine {

    private final List<Note> allNotes;
    private final Queue<Note> activeNotes;
    private final ScoreSystem scoreSystem;
    private final DifficultyManager difficulty;
    private final HpSystem hpSystem;

    private long startTime;
    private boolean running;
    private long lastSpawnTime = 0;

    public GameEngine(List<Note> notes) {
        this.allNotes    = notes;
        this.activeNotes = new LinkedList<>();
        this.scoreSystem = new ScoreSystem();
        this.difficulty  = new DifficultyManager();
        this.hpSystem    = new HpSystem();
        this.running     = false;
    }

    public void start(long now) {
        this.startTime = now;
        this.running   = true;
    }

    public void update(long now) {
        if (!running) return;

        long gameTime = now - startTime;

        difficulty.update(gameTime);
        hpSystem.update(now);

        if (hpSystem.isDead()) {
            running = false;
            return;
        }

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

            double roll = Math.random();
            Note note;
            if (roll < 0.10) {
                // Nota MOVING — velocidad aleatoria entre 80 y 160 px/s
                double speed = 80 + Math.random() * 80;
                double angle = Math.random() * Math.PI * 2;
                double vx = Math.cos(angle) * speed;
                double vy = Math.sin(angle) * speed;
                note = new Note(x, y, gameTime, difficulty.getNoteDuration(),
                        NoteType.MOVING, vx, vy);
            } else if (roll < 0.30) {
                note = new Note(x, y, gameTime, difficulty.getNoteDuration(), NoteType.TRAP);
            } else {
                note = new Note(x, y, gameTime, difficulty.getNoteDuration(), NoteType.NORMAL);
            }

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

            if (!current.isTrap()) {
                scoreSystem.registerMiss();
                hpSystem.registerMiss();
                // Ya no cortamos running aquí — el HP drain se encarga
            }
        }
    }

    // ------------------------------------------------------------------ //

    public HitResult checkHitWithDistance(double x, double y, long now) {
        if (!running) return null;

        long gameTime = now - startTime;

        Iterator<Note> iterator = activeNotes.iterator();
        while (iterator.hasNext()) {
            Note note = iterator.next();
            if (note.getState() == NoteState.ACTIVE && note.isInside(x, y, 30, gameTime)) {
                double cx = note.getX(gameTime);
                double cy = note.getY(gameTime);
                double distance = Math.sqrt((x - cx) * (x - cx) + (y - cy) * (y - cy));

                note.setState(NoteState.HIT);
                iterator.remove();
                return new HitResult(distance, note.isTrap(), cx, cy);
            }
        }
        return null;
    }

    public record HitResult(double distance, boolean trap, double noteX, double noteY) {}

    // ------------------------------------------------------------------ //

    public List<Note> getActiveNotes()       { return new ArrayList<>(activeNotes); }
    public ScoreSystem getScoreSystem()      { return scoreSystem; }
    public HpSystem getHpSystem()            { return hpSystem; }
    public DifficultyManager getDifficulty() { return difficulty; }
    public boolean isRunning()               { return running; }
    public boolean isGameOver()              { return !running; }
    public void forceGameOver()              { running = false; }
    public long getGameTime(long now)        { return now - startTime; }
}
