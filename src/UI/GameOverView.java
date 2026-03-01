package UI;

import Model.GameMode;
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

    public GameOverView(int finalScore, int highScore, GameMode mode) {

        setAlignment(Pos.CENTER);
        setSpacing(20);
        setStyle("-fx-background-color: #000010;");

        // Título
        Label title = new Label(mode == GameMode.SONG ? "SONG COMPLETE" : "GAME OVER");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 52));
        title.setTextFill(mode == GameMode.SONG ? Color.web("#CE93D8") : Color.web("#FF4081"));
        DropShadow glow = new DropShadow(20, (Color) title.getTextFill());
        title.setEffect(glow);

        // Modo
        Label modeLabel = new Label(mode == GameMode.SONG ? "♪ Song Mode" : "▶ Arcade Mode");
        modeLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 16));
        modeLabel.setTextFill(Color.LIGHTBLUE);

        // Score
        Label scoreLabel = new Label("Score: " + finalScore);
        scoreLabel.setFont(Font.font("Arial", FontWeight.BOLD, 30));
        scoreLabel.setTextFill(Color.WHITE);

        getChildren().addAll(title, modeLabel);

        // Nuevo récord
        if (finalScore >= highScore && finalScore > 0) {
            Label newRecord = new Label("🏆  NEW BEST SCORE!");
            newRecord.setFont(Font.font("Arial", FontWeight.BOLD, 22));
            newRecord.setTextFill(Color.GOLD);
            getChildren().addAll(newRecord, scoreLabel);
        } else {
            Label bestLabel = new Label("Best: " + highScore);
            bestLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 20));
            bestLabel.setTextFill(Color.LIGHTBLUE);
            getChildren().addAll(scoreLabel, bestLabel);
        }

        // Botón
        retryButton = new Button("↩  Back to Menu");
        retryButton.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        String base  = "-fx-background-color: #1565C0;-fx-text-fill: white;-fx-background-radius: 25;-fx-padding: 12 35 12 35;-fx-cursor: hand;";
        String hover = "-fx-background-color: #1E88E5;-fx-text-fill: white;-fx-background-radius: 25;-fx-padding: 12 35 12 35;-fx-cursor: hand;";
        retryButton.setStyle(base);
        retryButton.setOnMouseEntered(e -> retryButton.setStyle(hover));
        retryButton.setOnMouseExited(e  -> retryButton.setStyle(base));

        getChildren().add(retryButton);
    }

    public Button getRetryButton() { return retryButton; }
}