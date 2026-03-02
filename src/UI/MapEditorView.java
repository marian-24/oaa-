package UI;

import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.List;

/**
 * Pantalla del editor de mapas.
 *
 * Muestra:
 *  - Nombre de la canción y tiempo actual
 *  - Barra de progreso de la canción
 *  - Líneas verticales por cada beat registrado
 *  - Contador de beats grabados
 *  - Instrucciones: click para grabar, ESC/botón para terminar
 */
public class MapEditorView extends Pane {

    private static final double W = 800;
    private static final double H = 600;

    private final Canvas canvas;
    private final GraphicsContext gc;

    // Estado que la vista recibe cada frame
    private double progress       = 0;    // 0.0 a 1.0 (posición en la canción)
    private long   currentMs      = 0;
    private long   durationMs     = 0;
    private int    beatCount      = 0;
    private String songName       = "";
    private boolean recording     = false;

    // Marcadores de beats: posición relativa 0..1 en la canción
    private List<Double> beatPositions; // fracción de la canción

    private Button finishButton;
    private Button cancelButton;

    public MapEditorView(String songName, long durationMs) {
        this.songName  = songName;
        this.durationMs = durationMs;

        setPrefSize(W, H);
        canvas = new Canvas(W, H);
        gc = canvas.getGraphicsContext2D();

        // Botones
        finishButton = styledButton("✔  Save Map",   "#2E7D32", "#43A047");
        cancelButton = styledButton("✗  Cancel",     "#B71C1C", "#E53935");

        HBox btnRow = new HBox(16, finishButton, cancelButton);
        btnRow.setAlignment(Pos.CENTER);
        btnRow.setLayoutX(0);
        btnRow.setLayoutY(H - 70);
        btnRow.setPrefWidth(W);

        getChildren().addAll(canvas, btnRow);
    }

    // ------------------------------------------------------------------ //
    //  Actualización de estado
    // ------------------------------------------------------------------ //

    public void updateState(long currentMs, int beatCount, List<Double> beatPositions,
                            boolean recording) {
        this.currentMs     = currentMs;
        this.beatCount     = beatCount;
        this.beatPositions = beatPositions;
        this.recording     = recording;
        this.progress      = durationMs > 0 ? (double) currentMs / durationMs : 0;
        render();
    }

    // ------------------------------------------------------------------ //
    //  Render
    // ------------------------------------------------------------------ //

    private void render() {
        // Fondo
        gc.setFill(Color.web("#000018"));
        gc.fillRect(0, 0, W, H);

        drawTitle();
        drawTimeline();
        drawBeatMarkers();
        drawProgressHead();
        drawStats();
        drawInstructions();
    }

    private void drawTitle() {
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        gc.setFill(Color.web("#CE93D8"));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("MAP EDITOR  —  " + songName, W / 2, 50);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    private void drawTimeline() {
        double barX = 40, barY = 100, barW = W - 80, barH = 24;

        // Fondo de la barra
        gc.setFill(Color.web("#1a1a3a"));
        gc.fillRoundRect(barX, barY, barW, barH, 8, 8);

        // Progreso
        gc.setFill(Color.web("#CE93D8", 0.7));
        gc.fillRoundRect(barX, barY, barW * progress, barH, 8, 8);

        // Borde
        gc.setStroke(Color.web("#444466"));
        gc.setLineWidth(1.5);
        gc.strokeRoundRect(barX, barY, barW, barH, 8, 8);

        // Tiempo
        gc.setFont(Font.font("Arial", FontWeight.NORMAL, 13));
        gc.setFill(Color.web("#AAAACC"));
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.fillText(formatMs(currentMs) + " / " + formatMs(durationMs), W - 40, 90);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    private void drawBeatMarkers() {
        if (beatPositions == null) return;

        double barX = 40, barY = 140, barW = W - 80, barH = 200;

        // Área de beats
        gc.setFill(Color.web("#050515"));
        gc.fillRect(barX, barY, barW, barH);
        gc.setStroke(Color.web("#222244"));
        gc.setLineWidth(1);
        gc.strokeRect(barX, barY, barW, barH);

        // Líneas de beat
        gc.setStroke(Color.web("#CE93D8", 0.8));
        gc.setLineWidth(1.5);
        for (double pos : beatPositions) {
            double lx = barX + pos * barW;
            gc.strokeLine(lx, barY + 4, lx, barY + barH - 4);
        }

        // Cursor de posición actual
        double headX = barX + progress * barW;
        gc.setStroke(Color.web("#FFFFFF"));
        gc.setLineWidth(2);
        gc.strokeLine(headX, barY, headX, barY + barH);
    }

    private void drawProgressHead() {
        // Triángulo indicador sobre la barra de progreso
        double barX = 40, barY = 100, barW = W - 80;
        double hx = barX + progress * barW;

        gc.setFill(Color.WHITE);
        gc.fillPolygon(
                new double[]{ hx - 6, hx + 6, hx },
                new double[]{ barY - 10, barY - 10, barY },
                3);
    }

    private void drawStats() {
        // Contador de beats
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 64));
        gc.setFill(Color.web("#FFD700", 0.9));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(String.valueOf(beatCount), W / 2, 430);

        gc.setFont(Font.font("Arial", FontWeight.NORMAL, 18));
        gc.setFill(Color.web("#AAAACC"));
        gc.fillText("beats recorded", W / 2, 460);

        // Indicador REC parpadeante
        if (recording) {
            boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
            if (blink) {
                gc.setFont(Font.font("Arial", FontWeight.BOLD, 20));
                gc.setFill(Color.web("#FF4444"));
                gc.fillText("● REC", W / 2, 500);
            }
        }

        gc.setTextAlign(TextAlignment.LEFT);
    }

    private void drawInstructions() {
        gc.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        gc.setFill(Color.web("#666688"));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("Click anywhere on screen to record a beat", W / 2, H - 90);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    // ------------------------------------------------------------------ //
    //  Helpers
    // ------------------------------------------------------------------ //

    private String formatMs(long ms) {
        long secs = ms / 1000;
        long centis = (ms % 1000) / 10;
        return String.format("%d:%02d.%02d", secs / 60, secs % 60, centis);
    }

    private Button styledButton(String text, String base, String hover) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        String bs = "-fx-background-color:" + base + ";-fx-text-fill:white;" +
                "-fx-background-radius:25;-fx-padding:10 28 10 28;-fx-cursor:hand;";
        String hs = "-fx-background-color:" + hover + ";-fx-text-fill:white;" +
                "-fx-background-radius:25;-fx-padding:10 28 10 28;-fx-cursor:hand;";
        btn.setStyle(bs);
        btn.setOnMouseEntered(e -> btn.setStyle(hs));
        btn.setOnMouseExited(e  -> btn.setStyle(bs));
        return btn;
    }

    // ------------------------------------------------------------------ //
    //  API pública
    // ------------------------------------------------------------------ //

    public Button getFinishButton() { return finishButton; }
    public Button getCancelButton() { return cancelButton; }
}