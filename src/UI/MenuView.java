package UI;

import javafx.animation.AnimationTimer;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.Random;
import java.util.prefs.Preferences;

public class MenuView extends StackPane {

    private static final double W = 800;
    private static final double H = 600;

    private Button startButton;
    private Button songButton;
    private Button editorButton;
    private AnimationTimer animator;
    private MenuTheme currentTheme;

    // Preferencias para recordar el tema elegido
    private static final String PREF_THEME = "menu_theme";
    private final Preferences prefs = Preferences.userNodeForPackage(MenuView.class);

    // Canvas de fondo (se reutiliza al cambiar de tema)
    private final Canvas canvas = new Canvas(W, H);
    private final GraphicsContext gc = canvas.getGraphicsContext2D();

    // ------------------------------------------------------------------ //
    //  Datos tema SPACE
    // ------------------------------------------------------------------ //
    private static final int STAR_COUNT = 120;
    private final double[] starX        = new double[STAR_COUNT];
    private final double[] starY        = new double[STAR_COUNT];
    private final double[] starRadius   = new double[STAR_COUNT];
    private final double[] starOpacity  = new double[STAR_COUNT];
    private final double[] starOpDelta  = new double[STAR_COUNT];

    // ------------------------------------------------------------------ //
    //  Datos tema MATRIX
    // ------------------------------------------------------------------ //
    private static final int COL_COUNT  = 40;   // columnas de caracteres
    private static final String CHARS   = "ｦｧｨｩｪｫｬｭｮｯｰｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐﾑﾒﾓﾔﾕﾖﾗﾘﾙﾚﾛﾜﾝ0123456789";
    private final double[] colX         = new double[COL_COUNT];
    private final double[] colY         = new double[COL_COUNT];
    private final double[] colSpeed     = new double[COL_COUNT];
    private final int[]    colChar      = new int[COL_COUNT];
    private final double[] colOpacity   = new double[COL_COUNT];
    private int matrixFrame = 0;        // para cambiar chars cada N frames

    // ------------------------------------------------------------------ //
    //  Datos tema SAKURA
    // ------------------------------------------------------------------ //
    private static final int PETAL_COUNT = 60;
    private final double[] petalX      = new double[PETAL_COUNT];
    private final double[] petalY      = new double[PETAL_COUNT];
    private final double[] petalSize   = new double[PETAL_COUNT];
    private final double[] petalSpeedY = new double[PETAL_COUNT];
    private final double[] petalSpeedX = new double[PETAL_COUNT];
    private final double[] petalAngle  = new double[PETAL_COUNT];
    private final double[] petalSpin   = new double[PETAL_COUNT];
    private final double[] petalOpacity= new double[PETAL_COUNT];

    // ------------------------------------------------------------------ //
    //  Construcción
    // ------------------------------------------------------------------ //

    public MenuView(int highScore) {
        setPrefSize(W, H);

        // Cargar tema guardado (por defecto SPACE)
        String saved = prefs.get(PREF_THEME, MenuTheme.SPACE.name());
        currentTheme = MenuTheme.valueOf(saved);

        initSpaceData();
        initMatrixData();
        initSakuraData();
        startAnimation();

        // --- Contenido ---
        VBox content = new VBox(22);
        content.setAlignment(Pos.CENTER);

        // Título — cambia color según tema
        Label title = buildTitle();

        // Subtítulo
        Label subtitle = new Label("Click the circles. Feel the rhythm.");
        subtitle.setFont(Font.font("Arial", FontWeight.NORMAL, 18));
        subtitle.setTextFill(Color.web("#AAAACC"));

        // High score
        String hsText = highScore > 0
                ? "★  Best Score: " + highScore + "  ★"
                : "No record yet — be the first!";
        Label highScoreLabel = new Label(hsText);
        highScoreLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        highScoreLabel.setTextFill(Color.GOLD);

        // Botones de juego
        startButton = new Button("▶  ARCADE MODE");
        startButton.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        applyButtonStyle(startButton, "#1565C0", "#1E88E5", "#42A5F5");

        songButton = new Button("♪  SONG MODE");
        songButton.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        applyButtonStyle(songButton, "#6A1B9A", "#8E24AA", "#CE93D8");

        editorButton = new Button("✏  MAP EDITOR");
        editorButton.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        applyButtonStyle(editorButton, "#1B5E20", "#2E7D32", "#81C784");

        // Selector de tema
        HBox themeRow = buildThemeSelector();

        content.getChildren().addAll(title, subtitle, highScoreLabel,
                startButton, songButton, editorButton, themeRow);
        getChildren().addAll(canvas, content);
    }

    // ------------------------------------------------------------------ //
    //  Selector de tema
    // ------------------------------------------------------------------ //

    private HBox buildThemeSelector() {
        Label label = new Label("Theme:");
        label.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        label.setTextFill(Color.web("#666688"));

        Button spaceBtn  = themeButton("🌌 Space",  MenuTheme.SPACE);
        Button matrixBtn = themeButton("💻 Matrix", MenuTheme.MATRIX);
        Button sakuraBtn = themeButton("🌸 Sakura", MenuTheme.SAKURA);

        highlightThemeButtons(spaceBtn, matrixBtn, sakuraBtn);

        spaceBtn.setOnAction(e -> {
            switchTheme(MenuTheme.SPACE);
            highlightThemeButtons(spaceBtn, matrixBtn, sakuraBtn);
        });
        matrixBtn.setOnAction(e -> {
            switchTheme(MenuTheme.MATRIX);
            highlightThemeButtons(spaceBtn, matrixBtn, sakuraBtn);
        });
        sakuraBtn.setOnAction(e -> {
            switchTheme(MenuTheme.SAKURA);
            highlightThemeButtons(spaceBtn, matrixBtn, sakuraBtn);
        });

        HBox row = new HBox(10, label, spaceBtn, matrixBtn, sakuraBtn);
        row.setAlignment(Pos.CENTER);
        return row;
    }

    private Button themeButton(String text, MenuTheme theme) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        return btn;
    }

    private void highlightThemeButtons(Button spaceBtn, Button matrixBtn, Button sakuraBtn) {
        String active   = "-fx-background-color: #333355; -fx-text-fill: white;" +
                "-fx-background-radius: 15; -fx-padding: 6 16 6 16;" +
                "-fx-border-color: #A78BFA; -fx-border-radius: 15; -fx-cursor: hand;";
        String inactive = "-fx-background-color: #111122; -fx-text-fill: #666688;" +
                "-fx-background-radius: 15; -fx-padding: 6 16 6 16;" +
                "-fx-border-color: #333355; -fx-border-radius: 15; -fx-cursor: hand;";

        spaceBtn.setStyle(currentTheme == MenuTheme.SPACE   ? active : inactive);
        matrixBtn.setStyle(currentTheme == MenuTheme.MATRIX ? active : inactive);
        sakuraBtn.setStyle(currentTheme == MenuTheme.SAKURA ? active : inactive);
    }

    private void switchTheme(MenuTheme theme) {
        currentTheme = theme;
        prefs.put(PREF_THEME, theme.name());  // persistir elección
    }

    // ------------------------------------------------------------------ //
    //  Título adaptado al tema
    // ------------------------------------------------------------------ //

    private Label buildTitle() {
        Label title = new Label("RHYTHM SPACE");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 64));
        title.setTextFill(Color.WHITE);
        title.setTextAlignment(TextAlignment.CENTER);

        Glow glow = new Glow(0.8);
        DropShadow shadow = new DropShadow(20,
                currentTheme == MenuTheme.MATRIX ? Color.LIMEGREEN
                        : currentTheme == MenuTheme.SAKURA  ? Color.web("#FF85A1")
                        : Color.DODGERBLUE);
        shadow.setInput(glow);
        title.setEffect(shadow);
        return title;
    }

    // ------------------------------------------------------------------ //
    //  Animación principal — delega según tema activo
    // ------------------------------------------------------------------ //

    private void startAnimation() {
        animator = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (currentTheme == MenuTheme.SPACE) {
                    drawSpaceBackground();
                    drawStars();
                } else if (currentTheme == MenuTheme.MATRIX) {
                    drawMatrixBackground();
                    drawMatrix();
                } else {
                    drawSakuraBackground();
                    drawPetals();
                }
            }
        };
        animator.start();
    }

    // ------------------------------------------------------------------ //
    //  Tema SPACE
    // ------------------------------------------------------------------ //

    private void initSpaceData() {
        Random rand = new Random();
        for (int i = 0; i < STAR_COUNT; i++) {
            starX[i]       = rand.nextDouble() * W;
            starY[i]       = rand.nextDouble() * H;
            starRadius[i]  = 0.5 + rand.nextDouble() * 2.0;
            starOpacity[i] = rand.nextDouble();
            starOpDelta[i] = 0.003 + rand.nextDouble() * 0.007;
        }
    }

    private void drawSpaceBackground() {
        LinearGradient bg = new LinearGradient(
                0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0,   Color.web("#000010")),
                new Stop(0.5, Color.web("#000830")),
                new Stop(1,   Color.web("#000010"))
        );
        gc.setFill(bg);
        gc.fillRect(0, 0, W, H);
    }

    private void drawStars() {
        for (int i = 0; i < STAR_COUNT; i++) {
            starOpacity[i] += starOpDelta[i];
            if (starOpacity[i] >= 1.0 || starOpacity[i] <= 0.0)
                starOpDelta[i] = -starOpDelta[i];

            gc.setGlobalAlpha(Math.max(0, Math.min(1, starOpacity[i])));
            gc.setFill(Color.WHITE);
            gc.fillOval(starX[i] - starRadius[i], starY[i] - starRadius[i],
                    starRadius[i] * 2, starRadius[i] * 2);
        }
        gc.setGlobalAlpha(1.0);
    }

    // ------------------------------------------------------------------ //
    //  Tema MATRIX
    // ------------------------------------------------------------------ //

    private void initMatrixData() {
        Random rand = new Random();
        double colWidth = W / COL_COUNT;
        for (int i = 0; i < COL_COUNT; i++) {
            colX[i]       = i * colWidth + colWidth / 2;
            colY[i]       = rand.nextDouble() * H;
            colSpeed[i]   = 1.5 + rand.nextDouble() * 3.0;
            colChar[i]    = rand.nextInt(CHARS.length());
            colOpacity[i] = 0.4 + rand.nextDouble() * 0.6;
        }
    }

    private void drawMatrixBackground() {
        // Fondo negro con leve transparencia para efecto de estela
        gc.setFill(Color.web("#000000", 0.15));
        gc.fillRect(0, 0, W, H);
    }

    private void drawMatrix() {
        matrixFrame++;
        Random rand = new Random();

        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 16));

        for (int i = 0; i < COL_COUNT; i++) {
            // Carácter principal (blanco brillante)
            gc.setGlobalAlpha(colOpacity[i]);
            gc.setFill(Color.web("#AAFFAA"));
            String ch = String.valueOf(CHARS.charAt(colChar[i]));
            gc.fillText(ch, colX[i], colY[i]);

            // Estela verde más oscura arriba
            gc.setGlobalAlpha(colOpacity[i] * 0.4);
            gc.setFill(Color.web("#00AA00"));
            if (colY[i] > 20) {
                gc.fillText(String.valueOf(CHARS.charAt(rand.nextInt(CHARS.length()))),
                        colX[i], colY[i] - 18);
            }

            // Mover hacia abajo
            colY[i] += colSpeed[i];

            // Cambiar carácter cada 4 frames
            if (matrixFrame % 4 == 0) {
                colChar[i] = rand.nextInt(CHARS.length());
            }

            // Resetear columna al salir de pantalla
            if (colY[i] > H + 20) {
                colY[i]       = -20;
                colSpeed[i]   = 1.5 + rand.nextDouble() * 3.0;
                colOpacity[i] = 0.4 + rand.nextDouble() * 0.6;
            }
        }
        gc.setGlobalAlpha(1.0);
    }

    // ------------------------------------------------------------------ //
    //  Tema SAKURA
    // ------------------------------------------------------------------ //

    private void initSakuraData() {
        Random rand = new Random();
        for (int i = 0; i < PETAL_COUNT; i++) {
            petalX[i]       = rand.nextDouble() * W;
            petalY[i]       = rand.nextDouble() * H;
            petalSize[i]    = 5 + rand.nextDouble() * 10;
            petalSpeedY[i]  = 0.6 + rand.nextDouble() * 1.2;
            petalSpeedX[i]  = -0.4 + rand.nextDouble() * 0.8;
            petalAngle[i]   = rand.nextDouble() * Math.PI * 2;
            petalSpin[i]    = (rand.nextBoolean() ? 1 : -1) * (0.01 + rand.nextDouble() * 0.03);
            petalOpacity[i] = 0.4 + rand.nextDouble() * 0.6;
        }
    }

    private void drawSakuraBackground() {
        // Degradado vertical rosa oscuro → magenta oscuro → rosa oscuro
        LinearGradient bg = new LinearGradient(
                0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#1a0010")),
                new Stop(0.4, Color.web("#2d0020")),
                new Stop(0.7, Color.web("#200018")),
                new Stop(1.0, Color.web("#150010"))
        );
        gc.setFill(bg);
        gc.fillRect(0, 0, W, H);
    }

    private void drawPetals() {
        for (int i = 0; i < PETAL_COUNT; i++) {
            gc.save();
            gc.setGlobalAlpha(petalOpacity[i]);
            gc.translate(petalX[i], petalY[i]);
            gc.rotate(Math.toDegrees(petalAngle[i]));

            drawPetalShape(petalSize[i]);

            gc.restore();

            // Mover pétalo
            petalY[i]     += petalSpeedY[i];
            petalX[i]     += petalSpeedX[i] + Math.sin(petalAngle[i] * 2) * 0.3;
            petalAngle[i] += petalSpin[i];

            // Resetear si sale de pantalla
            if (petalY[i] > H + 20) {
                petalY[i] = -20;
                petalX[i] = new Random().nextDouble() * W;
            }
        }
        gc.setGlobalAlpha(1.0);
    }

    private void drawPetalShape(double size) {
        // Forma de pétalo: 5 elipses rotadas alrededor del centro
        for (int p = 0; p < 5; p++) {
            gc.save();
            gc.rotate(p * 72.0);
            // Degradado rosa
            gc.setFill(Color.web("#FFB7C5", 0.85));
            gc.fillOval(-size * 0.3, -size, size * 0.6, size);
            // Borde más claro
            gc.setFill(Color.web("#FFD6E0", 0.4));
            gc.fillOval(-size * 0.15, -size * 0.9, size * 0.3, size * 0.6);
            gc.restore();
        }
    }

    // ------------------------------------------------------------------ //
    //  Helpers
    // ------------------------------------------------------------------ //

    private void applyButtonStyle(Button btn, String base, String hover, String glow) {
        String baseStyle  = "-fx-background-color: " + base + ";" +
                "-fx-text-fill: white; -fx-background-radius: 30;" +
                "-fx-padding: 14 40 14 40; -fx-cursor: hand;";
        String hoverStyle = "-fx-background-color: " + hover + ";" +
                "-fx-text-fill: white; -fx-background-radius: 30;" +
                "-fx-padding: 14 40 14 40; -fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, " + glow + ", 16, 0.5, 0, 0);";
        btn.setStyle(baseStyle);
        btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
        btn.setOnMouseExited(e  -> btn.setStyle(baseStyle));
    }

    public void stopAnimation() {
        if (animator != null) animator.stop();
    }

    public Button getStartButton()  { return startButton; }
    public Button getSongButton()   { return songButton; }
    public Button getEditorButton() { return editorButton; }
}