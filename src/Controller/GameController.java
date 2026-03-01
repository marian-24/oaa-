package Controller;

import Game.GameEngine;
import Model.Note;
import UI.CanvasState;
import UI.GameView;
import UI.GameView.HitRating;
import javafx.animation.AnimationTimer;

import java.util.List;
import java.util.function.IntConsumer;

public class GameController {

    private final GameEngine engine;
    private final GameView view;
    private final IntConsumer gameOverCallback;
    private AnimationTimer gameLoop;

    // Radio de la nota. GREAT si el click está en el 50% central, GOOD el resto
    private static final double NOTE_RADIUS = 30;
    private static final double GREAT_RADIUS = NOTE_RADIUS * 0.5;

    public GameController(CanvasState state, GameView view, IntConsumer gameOverCallback) {
        this.engine = new GameEngine(state);
        this.view = view;
        this.gameOverCallback = gameOverCallback;

        setupInputHandling();
        setupGameLoop();
    }

    private void setupInputHandling() {
        view.setOnMouseClick((x, y) -> {
            GameEngine.HitResult result = engine.checkHitWithDistance(x, y, System.nanoTime());

            if (result != null) {
                HitRating rating = result.distance() <= GREAT_RADIUS
                        ? HitRating.GREAT
                        : HitRating.GOOD;
                // Pasamos el rating al ScoreSystem para puntaje diferenciado
                engine.getScoreSystem().registerHit(rating);
                view.spawnHitEffect(x, y, rating);
            }

            view.updateScore(engine.getScoreSystem());
        });
    }

    private void setupGameLoop() {
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // Capturamos las notas ANTES del update para detectar misses
                List<Note> before = engine.getActiveNotes();

                engine.update(now);

                List<Note> after = engine.getActiveNotes();

                // Si había una nota activa y ahora desapareció sin ser HIT → fue MISS
                if (before.size() > after.size() && engine.isGameOver()) {
                    Note missed = before.get(0);
                    view.spawnMissEffect(missed.getX(), missed.getY());
                }

                view.renderFrame(engine.getActiveNotes());
                view.updateScore(engine.getScoreSystem());

                if (engine.isGameOver()) {
                    stop();
                    gameOverCallback.accept(engine.getScoreSystem().getScore());
                }
            }
        };
    }

    public void startGame() {
        engine.start(System.nanoTime());
        gameLoop.start();
    }
}