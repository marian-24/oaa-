package UI;

import Model.Note;
import Model.ScoreSystem;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;

import java.util.prefs.Preferences;

public class GameView extends Pane {

    private final Canvas canvas;
    private final GraphicsContext gc;
    private BiConsumer<Double, Double> clickHandler;

    // --- Tema ---
    private final MenuTheme theme;

    // --- Estrellas (tema SPACE) ---
    private static final int STAR_COUNT = 100;
    private final double[] starX       = new double[STAR_COUNT];
    private final double[] starY       = new double[STAR_COUNT];
    private final double[] starR       = new double[STAR_COUNT];
    private final double[] starOp      = new double[STAR_COUNT];
    private final double[] starOpDelta = new double[STAR_COUNT];

    // --- Matrix (tema MATRIX) ---
    private static final int COL_COUNT = 30;
    private static final String CHARS  = "ｦｧｨｩｪｫｬｭｮｯｰｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐﾑﾒﾓﾔﾕﾖﾗﾘﾙﾚﾛﾜﾝ0123456789";
    private final double[] colX        = new double[COL_COUNT];
    private final double[] colY        = new double[COL_COUNT];
    private final double[] colSpeed    = new double[COL_COUNT];
    private final int[]    colChar     = new int[COL_COUNT];
    private final double[] colOpacity  = new double[COL_COUNT];
    private int matrixFrame = 0;

    // --- Partículas ---
    private final List<Particle> particles = new ArrayList<>();

    // --- Feedback labels (texto flotante sobre la nota) ---
    private final List<FeedbackLabel> feedbackLabels = new ArrayList<>();

    // --- HUD ---
    private String scoreText     = "Score: 0";
    private String comboText     = "";
    private String highScoreText = "";
    private int lives            = -1;  // -1 = no mostrar (modo arcade)

    private static final double WIDTH       = 800;
    private static final double HEIGHT      = 600;
    private static final double NOTE_RADIUS = 30;

    // ------------------------------------------------------------------ //

    public GameView(int highScore) {
        setPrefSize(WIDTH, HEIGHT);

        // Leer tema guardado desde Preferences
        Preferences prefs = Preferences.userNodeForPackage(MenuView.class);
        String saved = prefs.get("menu_theme", MenuTheme.SPACE.name());
        theme = MenuTheme.valueOf(saved);

        canvas = new Canvas(WIDTH, HEIGHT);
        gc = canvas.getGraphicsContext2D();
        getChildren().add(canvas);

        highScoreText = "Best: " + highScore;

        initStars();
        initMatrixData();
        setupMouseHandling();
    }

    // ------------------------------------------------------------------ //
    //  Inicialización
    // ------------------------------------------------------------------ //

    private void initStars() {
        Random rand = new Random();
        for (int i = 0; i < STAR_COUNT; i++) {
            starX[i]       = rand.nextDouble() * WIDTH;
            starY[i]       = rand.nextDouble() * HEIGHT;
            starR[i]       = 0.5 + rand.nextDouble() * 1.8;
            starOp[i]      = rand.nextDouble();
            starOpDelta[i] = 0.003 + rand.nextDouble() * 0.007;
        }
    }

    private void initMatrixData() {
        Random rand = new Random();
        double colWidth = WIDTH / COL_COUNT;
        for (int i = 0; i < COL_COUNT; i++) {
            colX[i]       = i * colWidth + colWidth / 2;
            colY[i]       = rand.nextDouble() * HEIGHT;
            colSpeed[i]   = 1.5 + rand.nextDouble() * 3.0;
            colChar[i]    = rand.nextInt(CHARS.length());
            colOpacity[i] = 0.3 + rand.nextDouble() * 0.4;
        }
    }

    private void setupMouseHandling() {
        canvas.setOnMouseClicked(event -> {
            if (clickHandler != null) {
                clickHandler.accept(event.getX(), event.getY());
            }
        });
    }

    // ------------------------------------------------------------------ //
    //  API pública
    // ------------------------------------------------------------------ //

    public void setOnMouseClick(BiConsumer<Double, Double> handler) {
        this.clickHandler = handler;
    }

    /** Actualiza las vidas mostradas en el HUD. Llamar con -1 para ocultarlas (modo arcade). */
    public void setLives(int lives) {
        this.lives = lives;
    }

    public void renderFrame(List<Note> activeNotes) {
        drawBackground();
        drawStars();
        updateAndDrawParticles();
        drawNotes(activeNotes);
        updateAndDrawFeedbackLabels();
        drawHUD();
    }

    public void renderNotes(List<Note> activeNotes) {
        renderFrame(activeNotes);
    }

    public void updateScore(ScoreSystem scoreSystem) {
        scoreText = String.valueOf(scoreSystem.getScore());
        int combo = scoreSystem.getCombo();
        comboText = combo >= 2 ? combo + "x" : "";
    }

    /** Spawnea partículas + feedback label + sonido en la posición del hit */
    public void spawnHitEffect(double x, double y, HitRating rating) {
        spawnParticles(x, y, rating);
        feedbackLabels.add(new FeedbackLabel(x, y - NOTE_RADIUS - 10, rating));
        playHitSound(rating);
    }

    /** Muestra un MISS en la posición de la nota expirada */
    public void spawnMissEffect(double x, double y) {
        feedbackLabels.add(new FeedbackLabel(x, y - NOTE_RADIUS - 10, HitRating.MISS));
        playMissSound();
    }

    // ------------------------------------------------------------------ //
    //  Dibujo
    // ------------------------------------------------------------------ //

    private void drawBackground() {
        if (theme == MenuTheme.MATRIX) {
            // Fondo con transparencia para efecto de estela
            gc.setFill(Color.web("#000000", 0.2));
            gc.fillRect(0, 0, WIDTH, HEIGHT);
        } else {
            gc.setFill(Color.web("#000010"));
            gc.fillRect(0, 0, WIDTH, HEIGHT);
        }
    }

    private void drawStars() {
        if (theme == MenuTheme.MATRIX) {
            drawMatrix();
        } else {
            drawSpaceStars();
        }
    }

    private void drawSpaceStars() {
        for (int i = 0; i < STAR_COUNT; i++) {
            starOp[i] += starOpDelta[i];
            if (starOp[i] >= 1.0 || starOp[i] <= 0.0) starOpDelta[i] = -starOpDelta[i];
            gc.setGlobalAlpha(Math.max(0, Math.min(1, starOp[i])));
            gc.setFill(Color.WHITE);
            gc.fillOval(starX[i] - starR[i], starY[i] - starR[i], starR[i] * 2, starR[i] * 2);
        }
        gc.setGlobalAlpha(1.0);
    }

    private void drawMatrix() {
        matrixFrame++;
        Random rand = new Random();
        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 14));

        for (int i = 0; i < COL_COUNT; i++) {
            gc.setGlobalAlpha(colOpacity[i]);
            gc.setFill(Color.web("#00FF41"));
            gc.fillText(String.valueOf(CHARS.charAt(colChar[i])), colX[i], colY[i]);

            // Estela más oscura
            gc.setGlobalAlpha(colOpacity[i] * 0.35);
            gc.setFill(Color.web("#007A1F"));
            if (colY[i] > 16)
                gc.fillText(String.valueOf(CHARS.charAt(rand.nextInt(CHARS.length()))),
                        colX[i], colY[i] - 16);

            colY[i] += colSpeed[i];
            if (matrixFrame % 4 == 0) colChar[i] = rand.nextInt(CHARS.length());
            if (colY[i] > HEIGHT + 20) {
                colY[i]       = -20;
                colSpeed[i]   = 1.5 + rand.nextDouble() * 3.0;
                colOpacity[i] = 0.3 + rand.nextDouble() * 0.4;
            }
        }
        gc.setGlobalAlpha(1.0);
    }

    private void drawNotes(List<Note> notes) {
        for (Note note : notes) drawNote(note.getX(), note.getY());
    }

    private void drawNote(double x, double y) {
        boolean isMatrix = theme == MenuTheme.MATRIX;

        String haloColor  = isMatrix ? "#00FF41" : "#7B2FFF";
        String fillStart  = isMatrix ? "#00CC33" : "#00BFFF";
        String fillEnd    = isMatrix ? "#003311" : "#4B0082";
        String borderColor= isMatrix ? "#00FF41" : "#A78BFA";
        String centerColor= isMatrix ? "#AAFFAA" : "#E0E7FF";

        // Halo exterior
        RadialGradient halo = new RadialGradient(
                0, 0, x, y, NOTE_RADIUS * 1.8, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web(haloColor, 0.25)),
                new Stop(1.0, Color.web(haloColor, 0.0)));
        gc.setFill(halo);
        double hs = NOTE_RADIUS * 1.8 * 2;
        gc.fillOval(x - NOTE_RADIUS * 1.8, y - NOTE_RADIUS * 1.8, hs, hs);

        // Relleno
        RadialGradient fill = new RadialGradient(
                0, 0, x - NOTE_RADIUS * 0.3, y - NOTE_RADIUS * 0.3, NOTE_RADIUS,
                false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web(fillStart, 0.9)),
                new Stop(1.0, Color.web(fillEnd, 0.85)));
        gc.setFill(fill);
        gc.fillOval(x - NOTE_RADIUS, y - NOTE_RADIUS, NOTE_RADIUS * 2, NOTE_RADIUS * 2);

        // Borde
        gc.setStroke(Color.web(borderColor));
        gc.setLineWidth(2.5);
        gc.strokeOval(x - NOTE_RADIUS, y - NOTE_RADIUS, NOTE_RADIUS * 2, NOTE_RADIUS * 2);

        // Punto central
        gc.setFill(Color.web(centerColor, 0.8));
        gc.fillOval(x - 4, y - 4, 8, 8);
    }

    private void updateAndDrawParticles() {
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.update();
            if (p.isDead()) { it.remove(); continue; }
            gc.setGlobalAlpha(p.alpha());
            gc.setFill(p.color);
            gc.fillOval(p.x - p.size, p.y - p.size, p.size * 2, p.size * 2);
        }
        gc.setGlobalAlpha(1.0);
    }

    private void updateAndDrawFeedbackLabels() {
        Iterator<FeedbackLabel> it = feedbackLabels.iterator();
        while (it.hasNext()) {
            FeedbackLabel fl = it.next();
            fl.update();
            if (fl.isDead()) { it.remove(); continue; }

            gc.setGlobalAlpha(fl.alpha());
            gc.setFont(Font.font("Arial", FontWeight.BOLD, fl.fontSize()));
            gc.setFill(fl.color());
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText(fl.text, fl.x, fl.y);
        }
        gc.setGlobalAlpha(1.0);
        gc.setTextAlign(TextAlignment.LEFT); // resetear para el HUD
    }

    private void drawHUD() {
        // Score — grande, arriba a la derecha (estilo osu!)
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        gc.setFill(Color.web("#FFFFFF"));
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.fillText(scoreText, WIDTH - 16, 46);

        // High score — más chico, debajo del score
        gc.setFont(Font.font("Arial", FontWeight.NORMAL, 16));
        gc.setFill(Color.web("#FFD700", 0.85));
        gc.fillText(highScoreText, WIDTH - 16, 68);

        gc.setTextAlign(TextAlignment.LEFT);

        // Vidas — corazones arriba a la izquierda (solo en modo canción)
        if (lives >= 0) {
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 26));
            StringBuilder hearts = new StringBuilder();
            for (int i = 0; i < lives; i++) {
                hearts.append("❤ ");
            }
            gc.fillText(hearts.toString().trim(), 16, 36);
        }

        // Combo — abajo a la izquierda (estilo osu!)
        if (!comboText.isEmpty()) {
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 32));
            gc.setFill(Color.web("#FFFFFF"));
            gc.fillText(comboText, 16, HEIGHT - 20);
        }
    }

    // ------------------------------------------------------------------ //
    //  Partículas
    // ------------------------------------------------------------------ //

    private void spawnParticles(double x, double y, HitRating rating) {
        Random rand = new Random();
        int count = rating == HitRating.GREAT ? 22 : 12;
        for (int i = 0; i < count; i++) {
            double angle = rand.nextDouble() * Math.PI * 2;
            double speed = 1.5 + rand.nextDouble() * 3.5;
            double life  = 0.6 + rand.nextDouble() * 0.4;
            Color color  = rating == HitRating.GREAT
                    ? (rand.nextBoolean() ? Color.web("#7B2FFF") : Color.web("#00BFFF"))
                    : Color.web("#00E5FF", 0.8);
            particles.add(new Particle(x, y,
                    Math.cos(angle) * speed, Math.sin(angle) * speed, life, color));
        }
    }

    /** Retrocompatibilidad */
    public void spawnHitParticles(double x, double y) {
        spawnHitEffect(x, y, HitRating.GREAT);
    }

    // ------------------------------------------------------------------ //
    //  Sonido (generado por síntesis, sin archivos externos)
    // ------------------------------------------------------------------ //

    private void playHitSound(HitRating rating) {
        float freq = rating == HitRating.GREAT ? 880f : 660f;
        playTone(freq, 80, 0.25f);
    }

    private void playMissSound() {
        playTone(220f, 120, 0.15f);
    }

    /**
     * Genera y reproduce un tono sintético en un hilo separado.
     * freq      → frecuencia en Hz
     * durationMs → duración en milisegundos
     * volume    → amplitud 0..1
     */
    private void playTone(float freq, int durationMs, float volume) {
        new Thread(() -> {
            try {
                AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
                int samples = (int) (44100 * durationMs / 1000.0);
                byte[] buf = new byte[samples * 2];

                for (int i = 0; i < samples; i++) {
                    // Onda senoidal con envelope de decaimiento
                    double envelope = 1.0 - (double) i / samples;
                    short val = (short) (Math.sin(2 * Math.PI * freq * i / 44100)
                            * volume * envelope * Short.MAX_VALUE);
                    buf[i * 2]     = (byte) (val & 0xFF);
                    buf[i * 2 + 1] = (byte) ((val >> 8) & 0xFF);
                }

                SourceDataLine line = AudioSystem.getSourceDataLine(format);
                line.open(format, buf.length);
                line.start();
                line.write(buf, 0, buf.length);
                line.drain();
                line.close();
            } catch (Exception ignored) {
                // Si el sistema no tiene audio, simplemente no suena
            }
        }).start();
    }

    // ------------------------------------------------------------------ //
    //  Enum público para tipos de hit
    // ------------------------------------------------------------------ //

    public enum HitRating { GREAT, GOOD, MISS }

    // ------------------------------------------------------------------ //
    //  Clase interna: Particle
    // ------------------------------------------------------------------ //

    private static class Particle {
        double x, y;
        final double vx, vy;
        double life;
        final double maxLife;
        final Color color;
        final double size;

        Particle(double x, double y, double vx, double vy, double life, Color color) {
            this.x = x; this.y = y;
            this.vx = vx; this.vy = vy;
            this.life = life; this.maxLife = life;
            this.color = color;
            this.size = 2.5 + Math.random() * 2.5;
        }

        void update()         { x += vx; y += vy; life -= 0.02; }
        double alpha()        { return Math.max(0, life / maxLife); }
        boolean isDead()      { return life <= 0; }
    }

    // ------------------------------------------------------------------ //
    //  Clase interna: FeedbackLabel  (texto flotante que sube y desaparece)
    // ------------------------------------------------------------------ //

    private static class FeedbackLabel {
        double x, y;
        final String text;
        final HitRating rating;
        double life = 1.0;

        FeedbackLabel(double x, double y, String text, HitRating rating) {
            this.x = x; this.y = y;
            this.text = text; this.rating = rating;
        }

        FeedbackLabel(double x, double y, HitRating rating) {
            this(x, y, labelText(rating), rating);
        }

        private static String labelText(HitRating r) {
            return switch (r) {
                case GREAT -> "GREAT!";
                case GOOD  -> "GOOD";
                case MISS  -> "MISS";
            };
        }

        void update() {
            y    -= 0.8;   // sube lentamente
            life -= 0.025;
        }

        double alpha()   { return Math.max(0, life); }
        boolean isDead() { return life <= 0; }

        double fontSize() {
            return switch (rating) {
                case GREAT -> 28;
                case GOOD  -> 22;
                case MISS  -> 20;
            };
        }

        Color color() {
            return switch (rating) {
                case GREAT -> Color.web("#FFD700");  // dorado
                case GOOD  -> Color.web("#00BFFF");  // azul
                case MISS  -> Color.web("#FF4444");  // rojo
            };
        }
    }
}