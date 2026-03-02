package UI;

import Model.GameMode;
import Model.RankCalculator;
import Model.RankCalculator.Rank;
import Model.ScoreSystem;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class GameOverView extends VBox {

    private Button retryButton;

    public GameOverView(int finalScore, int highScore, GameMode mode, ScoreSystem scoreSystem) {

        setAlignment(Pos.CENTER);
        setSpacing(16);
        setStyle("-fx-background-color: #000010;");

        // --- Rank ---
        Rank rank = RankCalculator.calculate(scoreSystem.getHits(), scoreSystem.getMisses());
        String rankColor = RankCalculator.colorFor(rank);
        String accuracy  = RankCalculator.accuracyText(scoreSystem.getHits(), scoreSystem.getMisses());

        Label rankLabel = new Label(rank.name());
        rankLabel.setFont(Font.font("Arial", FontWeight.BOLD, 100));
        rankLabel.setTextFill(Color.web(rankColor));
        DropShadow rankGlow = new DropShadow(40, Color.web(rankColor));
        rankLabel.setEffect(rankGlow);

        // --- Título ---
        Label title = new Label(mode == GameMode.SONG ? "SONG COMPLETE" : "GAME OVER");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 42));
        Color titleColor = mode == GameMode.SONG ? Color.web("#CE93D8") : Color.web("#FF4081");
        title.setTextFill(titleColor);
        title.setEffect(new DropShadow(15, titleColor));

        // --- Accuracy ---
        Label accLabel = new Label("Accuracy: " + accuracy);
        accLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 18));
        accLabel.setTextFill(Color.web(rankColor));

        // --- Stats ---
        Label statsLabel = new Label(
                scoreSystem.getHits() + " hits  ·  " +
                        scoreSystem.getMisses() + " misses  ·  best combo " + scoreSystem.getMaxCombo() + "x"
        );
        statsLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 15));
        statsLabel.setTextFill(Color.web("#888899"));

        // --- Score ---
        Label scoreLabel = new Label("Score: " + finalScore);
        scoreLabel.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        scoreLabel.setTextFill(Color.WHITE);

        getChildren().addAll(rankLabel, title, accLabel, statsLabel);

        // Nuevo récord
        if (finalScore >= highScore && finalScore > 0) {
            Label newRecord = new Label("🏆  NEW BEST SCORE!");
            newRecord.setFont(Font.font("Arial", FontWeight.BOLD, 20));
            newRecord.setTextFill(Color.GOLD);
            getChildren().addAll(newRecord, scoreLabel);
        } else {
            Label bestLabel = new Label("Best: " + highScore);
            bestLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 18));
            bestLabel.setTextFill(Color.LIGHTBLUE);
            getChildren().addAll(scoreLabel, bestLabel);
        }

        // --- Botón ---
        retryButton = new Button("↩  Back to Menu");
        retryButton.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        String base  = "-fx-background-color: #1565C0;-fx-text-fill: white;-fx-background-radius: 25;-fx-padding: 12 35 12 35;-fx-cursor: hand;";
        String hover = "-fx-background-color: #1E88E5;-fx-text-fill: white;-fx-background-radius: 25;-fx-padding: 12 35 12 35;-fx-cursor: hand;";
        retryButton.setStyle(base);
        retryButton.setOnMouseEntered(e -> retryButton.setStyle(hover));
        retryButton.setOnMouseExited(e  -> retryButton.setStyle(base));

        getChildren().add(retryButton);
    }

    /** Retrocompatibilidad sin ScoreSystem */
    public GameOverView(int finalScore, int highScore, GameMode mode) {
        this(finalScore, highScore, mode, new ScoreSystem());
    }

    public Button getRetryButton() { return retryButton; }
}