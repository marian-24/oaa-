package Controller;

import Game.SongEngine;
import Model.ScoreSystem;
import UI.GameView;
import UI.GameView.HitRating;
import javafx.animation.AnimationTimer;

import java.util.function.BiConsumer;

public class SongController {

    private final SongEngine engine;
    private final GameView view;
    private final BiConsumer<Integer, ScoreSystem> gameOverCallback;
    private AnimationTimer gameLoop;

    private static final double NOTE_RADIUS  = 30;
    private static final double GREAT_RADIUS = NOTE_RADIUS * 0.5;

    public SongController(SongEngine engine, GameView view,
                          BiConsumer<Integer, ScoreSystem> gameOverCallback) {
        this.engine = engine;
        this.view   = view;
        this.gameOverCallback = gameOverCallback;

        setupInputHandling();
        setupGameLoop();
    }

    private void setupInputHandling() {
        view.setOnMouseClick((x, y) -> {
            SongEngine.HitResult result = engine.checkHitWithDistance(x, y);

            if (result != null) {
                if (result.trap()) {
                    view.spawnTrapEffect(x, y);
                    engine.registerTrapHit();
                } else {
                    HitRating rating = result.distance() <= GREAT_RADIUS
                            ? HitRating.GREAT : HitRating.GOOD;
                    engine.registerHit(rating);
                    view.spawnHitEffect(x, y, rating);
                }
            }
            view.updateScore(engine.getScoreSystem());
        });
    }

    private void setupGameLoop() {
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                engine.update(now);
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