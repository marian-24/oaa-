package Controller;

import Game.GameEngine;
import Model.Note;
import Model.NoteType;
import Model.ScoreSystem;
import UI.CanvasState;
import UI.GameView;
import UI.GameView.HitRating;
import javafx.animation.AnimationTimer;

import java.util.List;
import java.util.function.BiConsumer;

public class GameController {

    private final GameEngine engine;
    private final GameView view;
    private final BiConsumer<Integer, ScoreSystem> gameOverCallback;
    private AnimationTimer gameLoop;

    private static final double NOTE_RADIUS  = 30;
    private static final double GREAT_RADIUS = NOTE_RADIUS * 0.5;

    public GameController(CanvasState state, GameView view,
                          BiConsumer<Integer, ScoreSystem> gameOverCallback) {
        this.engine = new GameEngine(state);
        this.view   = view;
        this.gameOverCallback = gameOverCallback;

        setupInputHandling();
        setupGameLoop();
    }

    private void setupInputHandling() {
        view.setOnMouseClick((x, y) -> {
            GameEngine.HitResult result = engine.checkHitWithDistance(x, y, System.nanoTime());

            if (result != null) {
                if (result.trap()) {
                    view.spawnTrapEffect(result.noteX(), result.noteY());
                    engine.getScoreSystem().registerMiss();
                    engine.getHpSystem().registerMiss();
                } else {
                    HitRating rating = result.distance() <= GREAT_RADIUS
                            ? HitRating.GREAT : HitRating.GOOD;
                    engine.getScoreSystem().registerHit(rating);
                    engine.getHpSystem().registerHit(rating);
                    view.spawnHitEffect(result.noteX(), result.noteY(), rating);
                }
            }
            view.updateScore(engine.getScoreSystem());
        });
    }

    private void setupGameLoop() {
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                List<Note> before = engine.getActiveNotes();
                engine.update(now);
                List<Note> after = engine.getActiveNotes();

                if (before.size() > after.size()) {
                    Note missed = before.get(0);
                    view.spawnMissEffect(missed.getX(), missed.getY());
                    view.triggerDamageFlash();
                }

                view.renderFrame(engine.getActiveNotes(), engine.getHpSystem().getHp(), engine.getGameTime(now));
                view.updateScore(engine.getScoreSystem());

                if (engine.isGameOver()) {
                    stop();
                    gameOverCallback.accept(
                            engine.getScoreSystem().getScore(),
                            engine.getScoreSystem()
                    );
                }
            }
        };
    }

    public void startGame() {
        engine.start(System.nanoTime());
        gameLoop.start();
    }
}