package UI;

import Controller.GameController;
import Controller.SongController;
import Game.BeatDetector;
import Game.SongEngine;
import Model.GameMode;
import Model.HighScoreManager;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;

public class MainFrame {

    private final Stage stage;
    private final CanvasState canvasState;
    private final HighScoreManager highScoreManager;

    public MainFrame(Stage stage, CanvasState canvasState) {
        this.stage = stage;
        this.canvasState = canvasState;
        this.highScoreManager = new HighScoreManager();
    }

    // ------------------------------------------------------------------ //
    //  Menú principal
    // ------------------------------------------------------------------ //

    public void showMenu() {
        // Mostramos el high score de arcade en el menú (el más común)
        MenuView menuView = new MenuView(highScoreManager.getHighScore(GameMode.ARCADE));
        new MenuController(menuView, this);

        Scene scene = new Scene(menuView, 800, 600);
        stage.setScene(scene);
        stage.setTitle("Rhythm Space");
        stage.show();
    }

    // ------------------------------------------------------------------ //
    //  Modo Arcade
    // ------------------------------------------------------------------ //

    public void startArcadeGame() {
        GameView gameView = new GameView(highScoreManager.getHighScore(GameMode.ARCADE));
        GameController controller = new GameController(
                canvasState,
                gameView,
                (score, scoreSystem) -> showGameOver(score, GameMode.ARCADE, scoreSystem)
        );

        stage.setScene(new Scene(gameView, 800, 600));
        controller.startGame();
    }

    // ------------------------------------------------------------------ //
    //  Modo Canción — selección
    // ------------------------------------------------------------------ //

    public void showSongSelect() {
        SongSelectView selectView = new SongSelectView(highScoreManager);

        // Click en una canción de la lista o upload manual → arrancar juego
        selectView.setOnSongSelected(this::startSongGame);

        // Upload manual abre el FileChooser
        selectView.getUploadButton().setOnAction(e ->
                selectView.openFileChooser(stage));

        selectView.getBackButton().setOnAction(e -> showMenu());

        stage.setScene(new Scene(selectView, 800, 600));
    }

    // ------------------------------------------------------------------ //
    //  Modo Canción — juego
    // ------------------------------------------------------------------ //

    public void startSongGame(File audioFile) {
        new Thread(() -> {
            try {
                List<Long> beats = BeatDetector.detect(audioFile);

                Platform.runLater(() -> {
                    if (beats.isEmpty()) {
                        showError("No beats detected",
                                "The audio file could not be analyzed.\nTry a different file.");
                        return;
                    }

                    String songName = audioFile.getName();
                    int songHighScore = highScoreManager.getSongHighScore(songName);

                    SongEngine engine = new SongEngine(beats, audioFile);
                    GameView gameView = new GameView(songHighScore);

                    SongController controller = new SongController(
                            engine,
                            gameView,
                            (score, scoreSystem) -> showSongGameOver(score, audioFile, scoreSystem)
                    );

                    stage.setScene(new Scene(gameView, 800, 600));
                    controller.startGame();
                });

            } catch (Exception ex) {
                Platform.runLater(() ->
                        showError("Error loading audio", ex.getMessage()));
            }
        }).start();
    }

    // ------------------------------------------------------------------ //
    //  Game Over (ambos modos)
    // ------------------------------------------------------------------ //

    public void showGameOver(int finalScore, GameMode mode, Model.ScoreSystem scoreSystem) {
        highScoreManager.submitScore(finalScore, mode);

        GameOverView gameOverView = new GameOverView(
                finalScore,
                highScoreManager.getHighScore(mode),
                mode,
                scoreSystem
        );

        gameOverView.getRetryButton().setOnAction(e -> showMenu());
        stage.setScene(new Scene(gameOverView, 800, 600));
    }

    public void showSongGameOver(int finalScore, File audioFile, Model.ScoreSystem scoreSystem) {
        highScoreManager.submitSongScore(finalScore, audioFile.getName());
        highScoreManager.submitScore(finalScore, GameMode.SONG);

        GameOverView gameOverView = new GameOverView(
                finalScore,
                highScoreManager.getSongHighScore(audioFile.getName()),
                GameMode.SONG,
                scoreSystem
        );

        gameOverView.getRetryButton().setOnAction(e -> showMenu());
        stage.setScene(new Scene(gameOverView, 800, 600));
    }

    // ------------------------------------------------------------------ //
    //  Utilidades
    // ------------------------------------------------------------------ //

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}