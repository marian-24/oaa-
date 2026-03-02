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

    // --- Sakura (tema SAKURA) ---
    private static final int GPETAL_COUNT = 45;
    private final double[] gpetalX       = new double[GPETAL_COUNT];
    private final double[] gpetalY       = new double[GPETAL_COUNT];
    private final double[] gpetalSize    = new double[GPETAL_COUNT];
    private final double[] gpetalSpeedY  = new double[GPETAL_COUNT];
    private final double[] gpetalSpeedX  = new double[GPETAL_COUNT];
    private final double[] gpetalAngle   = new double[GPETAL_COUNT];
    private final double[] gpetalSpin    = new double[GPETAL_COUNT];
    private final double[] gpetalOpacity = new double[GPETAL_COUNT];

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
        initSakuraPetals();
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

    // --- HP y tiempo para approach circle ---
    private double currentHp  = 1.0;
    private long   currentGameTime = 0;

    // --- Flash de combo milestone ---
    private double comboFlashAlpha = 0;    // 0 = sin flash, >0 = mostrando
    private String comboFlashText  = "";
    private String comboFlashColor = "#FFFFFF";

    // --- Flash de daño (pantalla roja) ---
    private double damageFlashAlpha = 0;

    // --- Efectos de fondo reactivos ---
    private final List<RippleEffect> ripples = new ArrayList<>();
    private double vignetteCombo   = 0;   // 0..1 brillo del borde por combo
    private double vignetteDamage  = 0;   // 0..1 rojo de borde por daño
    private double bgPulse         = 0;   // 0..1 pulso central en milestone
    private String bgPulseColor    = "#7B2FFF";
    private int    currentCombo    = 0;

    public void setLives(int lives) { /* reemplazado por HP drain */ }

    /** Dispara el efecto visual de milestone de combo */
    public void triggerComboMilestone(int milestone) {
        comboFlashText  = milestone + "x COMBO!";
        comboFlashAlpha = 1.0;
        comboFlashColor = switch (milestone) {
            case 10  -> "#FFD700";
            case 25  -> "#FF6600";
            case 50  -> "#FF00FF";
            default  -> "#FF0000";
        };
        bgPulseColor = comboFlashColor;
        bgPulse      = 1.0;
        spawnMilestoneParticles(milestone);
        playMilestoneSound(milestone);
    }

    /** Dispara el flash rojo de daño */
    public void triggerDamageFlash() {
        damageFlashAlpha = 0.35;
        vignetteDamage   = 1.0;
    }

    /** Registra un hit para efectos de fondo (llamar con la pos del click) */
    public void triggerHitRipple(double x, double y, HitRating rating) {
        String color = rating == HitRating.GREAT
                ? (theme == MenuTheme.MATRIX ? "#00FF41" : theme == MenuTheme.SAKURA ? "#FF85A1" : "#7B2FFF")
                : (theme == MenuTheme.MATRIX ? "#AAFFAA" : theme == MenuTheme.SAKURA ? "#FFB7C5" : "#00BFFF");
        ripples.add(new RippleEffect(x, y, color));
    }

    /** Actualiza el combo actual para la vignette reactiva */
    public void setCurrentCombo(int combo) {
        this.currentCombo = combo;
        // Objetivo de brillo según combo: sube gradualmente
        double target = Math.min(1.0, combo / 50.0);
        vignetteCombo = vignetteCombo * 0.9 + target * 0.1; // suavizado
    }

    public void renderFrame(List<Note> activeNotes, double hp, long gameTime) {
        this.currentHp       = hp;
        this.currentGameTime = gameTime;
        drawBackground();
        drawBgPulse();
        drawStars();
        updateAndDrawRipples();
        updateAndDrawParticles();
        drawNotes(activeNotes);
        updateAndDrawFeedbackLabels();
        drawHUD();
        drawVignette();
        drawDamageFlash();
        drawComboFlash();
    }

    /** Retrocompatibilidad */
    public void renderFrame(List<Note> activeNotes) {
        renderFrame(activeNotes, currentHp, currentGameTime);
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
            gc.setFill(Color.web("#000000", 0.2));
            gc.fillRect(0, 0, WIDTH, HEIGHT);
        } else if (theme == MenuTheme.SAKURA) {
            gc.setFill(Color.web("#1a0010"));
            gc.fillRect(0, 0, WIDTH, HEIGHT);
        } else {
            gc.setFill(Color.web("#000010"));
            gc.fillRect(0, 0, WIDTH, HEIGHT);
        }
    }

    private void drawStars() {
        if (theme == MenuTheme.MATRIX) {
            drawMatrix();
        } else if (theme == MenuTheme.SAKURA) {
            drawSakuraPetals();
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
        for (Note note : notes) {
            double progress = (double)(currentGameTime - note.getAppearTime()) / note.getDuration();
            progress = Math.max(0, Math.min(1, progress));
            double nx = note.getX(currentGameTime);
            double ny = note.getY(currentGameTime);
            if (note.isTrap())   drawTrapNote(nx, ny, progress);
            else if (note.isMoving()) drawMovingNote(nx, ny, progress);
            else                 drawNote(nx, ny, progress);
        }
    }

    private void drawNote(double x, double y, double progress) {
        boolean isMatrix = theme == MenuTheme.MATRIX;

        String haloColor  = isMatrix ? "#00FF41" : "#7B2FFF";
        String fillStart  = isMatrix ? "#00CC33" : "#00BFFF";
        String fillEnd    = isMatrix ? "#003311" : "#4B0082";
        String borderColor= isMatrix ? "#00FF41" : "#A78BFA";
        String centerColor= isMatrix ? "#AAFFAA" : "#E0E7FF";
        String approachColor = isMatrix ? "#00FF41" : "#FFFFFF";

        // --- Approach circle ---
        // Empieza en 3x el radio y se achica hasta 1x cuando progress=1
        double approachScale = 3.0 - 2.0 * progress;
        double ar = NOTE_RADIUS * approachScale;
        gc.setStroke(Color.web(approachColor, 0.85 * (1.0 - progress * 0.5)));
        gc.setLineWidth(2.0);
        gc.strokeOval(x - ar, y - ar, ar * 2, ar * 2);

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

    /** Pulso central de fondo en milestone — gradiente que aparece y desaparece */
    private void drawBgPulse() {
        if (bgPulse <= 0) return;
        RadialGradient pulse = new RadialGradient(
                0, 0, WIDTH / 2, HEIGHT / 2, WIDTH * 0.7, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web(bgPulseColor, bgPulse * 0.18)),
                new Stop(1.0, Color.web(bgPulseColor, 0.0)));
        gc.setFill(pulse);
        gc.fillRect(0, 0, WIDTH, HEIGHT);
        bgPulse = Math.max(0, bgPulse - 0.012);
    }

    /** Ondas de ripple que se expanden desde cada hit */
    private void updateAndDrawRipples() {
        Iterator<RippleEffect> it = ripples.iterator();
        while (it.hasNext()) {
            RippleEffect r = it.next();
            r.update();
            if (r.isDead()) { it.remove(); continue; }
            gc.setGlobalAlpha(r.alpha());
            gc.setStroke(Color.web(r.color, r.alpha() * 0.6));
            gc.setLineWidth(2.5 * (1 - r.progress()));
            double d = r.radius() * 2;
            gc.strokeOval(r.x - r.radius(), r.y - r.radius(), d, d);
        }
        gc.setGlobalAlpha(1.0);
        gc.setLineWidth(1.0);
    }

    /** Vignette reactiva: brilla con el color del tema según combo, rojo en daño */
    private void drawVignette() {
        // Decay
        vignetteCombo  = Math.max(0, vignetteCombo  - 0.005);
        vignetteDamage = Math.max(0, vignetteDamage - 0.03);

        // Capa de combo (azul/verde según tema)
        if (vignetteCombo > 0.01) {
            String vColor = theme == MenuTheme.MATRIX ? "#00FF41" : theme == MenuTheme.SAKURA ? "#FF85A1" : "#7B2FFF";
            drawVignetteLayer(vColor, vignetteCombo * 0.55);
        }
        // Capa de daño (rojo, tiene prioridad visual)
        if (vignetteDamage > 0.01) {
            drawVignetteLayer("#FF0000", vignetteDamage * 0.65);
        }
    }

    private void drawVignetteLayer(String hexColor, double maxAlpha) {
        double r = Math.max(WIDTH, HEIGHT) * 0.75;
        RadialGradient vg = new RadialGradient(
                0, 0, WIDTH / 2, HEIGHT / 2, r, false, CycleMethod.NO_CYCLE,
                new Stop(0.55, Color.web(hexColor, 0.0)),
                new Stop(1.0,  Color.web(hexColor, maxAlpha)));
        gc.setFill(vg);
        gc.fillRect(0, 0, WIDTH, HEIGHT);
    }

    private void drawDamageFlash() {
        if (damageFlashAlpha <= 0) return;
        gc.setFill(Color.web("#FF0000", damageFlashAlpha));
        gc.fillRect(0, 0, WIDTH, HEIGHT);
        damageFlashAlpha = Math.max(0, damageFlashAlpha - 0.025);
    }

    private void drawComboFlash() {
        if (comboFlashAlpha <= 0) return;
        gc.setGlobalAlpha(comboFlashAlpha);
        double size = 48 + (1.0 - comboFlashAlpha) * 20; // crece al aparecer
        gc.setFont(Font.font("Arial", FontWeight.BOLD, size));
        gc.setFill(Color.web(comboFlashColor));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(comboFlashText, WIDTH / 2, HEIGHT / 2 - 30);
        gc.setGlobalAlpha(1.0);
        gc.setTextAlign(TextAlignment.LEFT);
        comboFlashAlpha = Math.max(0, comboFlashAlpha - 0.018);
    }

    private void spawnMilestoneParticles(int milestone) {
        Random rand = new Random();
        int count = milestone >= 50 ? 60 : (milestone >= 25 ? 40 : 25);
        String[] colors = milestone >= 50
                ? new String[]{"#FF00FF", "#FF66FF", "#FFFFFF", "#FFD700"}
                : milestone >= 25
                ? new String[]{"#FF6600", "#FFB347", "#FFFFFF"}
                : new String[]{"#FFD700", "#FFFACD", "#FFFFFF"};
        for (int i = 0; i < count; i++) {
            double angle = rand.nextDouble() * Math.PI * 2;
            double speed = 2.0 + rand.nextDouble() * 6.0;
            double life  = 0.7 + rand.nextDouble() * 0.5;
            Color c = Color.web(colors[rand.nextInt(colors.length)]);
            particles.add(new Particle(
                    WIDTH / 2, HEIGHT / 2,
                    Math.cos(angle) * speed, Math.sin(angle) * speed,
                    life, c));
        }
    }

    private void playMilestoneSound(int milestone) {
        float freq = milestone >= 50 ? 1400f : (milestone >= 25 ? 1100f : 880f);
        // Acorde de dos tonos para más impacto
        playTone(freq,        200, 0.35f);
        playTone(freq * 1.25f, 200, 0.25f);
    }

    private void drawHUD() {
        // --- Barra de HP (abajo del todo, ancho completo) ---
        double barW  = WIDTH;
        double barH  = 8;
        double barY  = HEIGHT - barH;
        double fillW = barW * Math.max(0, currentHp);

        // Fondo gris
        gc.setFill(Color.web("#222222"));
        gc.fillRect(0, barY, barW, barH);

        // Color de la barra según nivel de HP
        String hpColor;
        if (currentHp > 0.5)      hpColor = "#FF66AA";   // rosa saludable
        else if (currentHp > 0.25) hpColor = "#FF9900";  // naranja de alerta
        else                       hpColor = "#FF2222";   // rojo crítico

        gc.setFill(Color.web(hpColor));
        gc.fillRect(0, barY, fillW, barH);

        // Borde sutil
        gc.setStroke(Color.web("#444444"));
        gc.setLineWidth(1);
        gc.strokeRect(0, barY, barW, barH);

        // --- Score arriba a la derecha ---
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        gc.setFill(Color.web("#FFFFFF"));
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.fillText(scoreText, WIDTH - 16, 46);

        // High score debajo
        gc.setFont(Font.font("Arial", FontWeight.NORMAL, 16));
        gc.setFill(Color.web("#FFD700", 0.85));
        gc.fillText(highScoreText, WIDTH - 16, 68);

        gc.setTextAlign(TextAlignment.LEFT);

        // --- Combo abajo a la izquierda ---
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

    private void drawMovingNote(double x, double y, double progress) {
        boolean isMatrix = theme == MenuTheme.MATRIX;
        String approachColor = isMatrix ? "#FFFF00" : "#FFA500";
        String borderColor   = isMatrix ? "#FFFF00" : "#FFA500";

        // Approach circle naranja/amarillo
        double approachScale = 3.0 - 2.0 * progress;
        double ar = NOTE_RADIUS * approachScale;
        gc.setStroke(Color.web(approachColor, 0.85 * (1.0 - progress * 0.5)));
        gc.setLineWidth(2.0);
        gc.strokeOval(x - ar, y - ar, ar * 2, ar * 2);

        // Halo naranja
        RadialGradient halo = new RadialGradient(
                0, 0, x, y, NOTE_RADIUS * 1.8, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#FF8800", 0.3)),
                new Stop(1.0, Color.web("#FF8800", 0.0)));
        gc.setFill(halo);
        double hs = NOTE_RADIUS * 1.8 * 2;
        gc.fillOval(x - NOTE_RADIUS * 1.8, y - NOTE_RADIUS * 1.8, hs, hs);

        // Relleno
        RadialGradient fill = new RadialGradient(
                0, 0, x - NOTE_RADIUS * 0.3, y - NOTE_RADIUS * 0.3, NOTE_RADIUS,
                false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#FFB347", 0.9)),
                new Stop(1.0, Color.web("#8B2500", 0.85)));
        gc.setFill(fill);
        gc.fillOval(x - NOTE_RADIUS, y - NOTE_RADIUS, NOTE_RADIUS * 2, NOTE_RADIUS * 2);

        // Borde naranja
        gc.setStroke(Color.web(borderColor));
        gc.setLineWidth(2.5);
        gc.strokeOval(x - NOTE_RADIUS, y - NOTE_RADIUS, NOTE_RADIUS * 2, NOTE_RADIUS * 2);

        // Punto central
        gc.setFill(Color.web("#FFE5CC", 0.9));
        gc.fillOval(x - 4, y - 4, 8, 8);
    }

    private void drawTrapNote(double x, double y, double progress) {
        double r = NOTE_RADIUS;

        // --- Approach circle rojo ---
        double approachScale = 3.0 - 2.0 * progress;
        double ar = r * approachScale;
        gc.setStroke(Color.web("#FF4444", 0.85 * (1.0 - progress * 0.5)));
        gc.setLineWidth(2.0);
        gc.strokeOval(x - ar, y - ar, ar * 2, ar * 2);

        // Halo rojo exterior
        RadialGradient halo = new RadialGradient(
                0, 0, x, y, r * 1.8, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#FF0000", 0.3)),
                new Stop(1.0, Color.web("#FF0000", 0.0)));
        gc.setFill(halo);
        double hs = r * 1.8 * 2;
        gc.fillOval(x - r * 1.8, y - r * 1.8, hs, hs);

        // Rombo
        double[] px = { x,     x + r, x,     x - r };
        double[] py = { y - r, y,     y + r, y     };

        RadialGradient fill = new RadialGradient(
                0, 0, x, y - r * 0.3, r,
                false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#FF4444", 0.95)),
                new Stop(1.0, Color.web("#880000", 0.9)));
        gc.setFill(fill);
        gc.fillPolygon(px, py, 4);

        gc.setStroke(Color.web("#FF2222"));
        gc.setLineWidth(2.5);
        gc.strokePolygon(px, py, 4);

        gc.setFill(Color.web("#FFAAAA", 0.9));
        gc.fillOval(x - 3, y - 3, 6, 6);
    }

    /** Efecto visual al clickear una trampa: partículas rojas + label de penalización */
    public void spawnTrapEffect(double x, double y) {
        Random rand = new Random();
        for (int i = 0; i < 18; i++) {
            double angle = rand.nextDouble() * Math.PI * 2;
            double speed = 2.0 + rand.nextDouble() * 4.0;
            double life  = 0.5 + rand.nextDouble() * 0.4;
            particles.add(new Particle(x, y,
                    Math.cos(angle) * speed, Math.sin(angle) * speed,
                    life, Color.web("#FF2222")));
        }
        feedbackLabels.add(new FeedbackLabel(x, y - NOTE_RADIUS - 10, "TRAP!", HitRating.MISS));
        playMissSound();
    }

    // ------------------------------------------------------------------ //
    /** Retrocompatibilidad */
    public void spawnHitParticles(double x, double y) {
        spawnHitEffect(x, y, HitRating.GREAT);
    }
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
    // ------------------------------------------------------------------ //
    //  Tema SAKURA — pétalos en juego
    // ------------------------------------------------------------------ //

    private void initSakuraPetals() {
        Random rand = new Random();
        for (int i = 0; i < GPETAL_COUNT; i++) {
            gpetalX[i]       = rand.nextDouble() * WIDTH;
            gpetalY[i]       = rand.nextDouble() * HEIGHT;
            gpetalSize[i]    = 4 + rand.nextDouble() * 8;
            gpetalSpeedY[i]  = 0.5 + rand.nextDouble() * 1.0;
            gpetalSpeedX[i]  = -0.3 + rand.nextDouble() * 0.6;
            gpetalAngle[i]   = rand.nextDouble() * Math.PI * 2;
            gpetalSpin[i]    = (rand.nextBoolean() ? 1 : -1) * (0.008 + rand.nextDouble() * 0.025);
            gpetalOpacity[i] = 0.3 + rand.nextDouble() * 0.5;
        }
    }

    private void drawSakuraPetals() {
        for (int i = 0; i < GPETAL_COUNT; i++) {
            gc.save();
            gc.setGlobalAlpha(gpetalOpacity[i]);
            gc.translate(gpetalX[i], gpetalY[i]);
            gc.rotate(Math.toDegrees(gpetalAngle[i]));
            double s = gpetalSize[i];
            // 5 pétalos elípticos rotados
            for (int p = 0; p < 5; p++) {
                gc.save();
                gc.rotate(p * 72.0);
                gc.setFill(Color.web("#FFB7C5", 0.8));
                gc.fillOval(-s * 0.3, -s, s * 0.6, s);
                gc.setFill(Color.web("#FFD6E0", 0.35));
                gc.fillOval(-s * 0.15, -s * 0.85, s * 0.3, s * 0.55);
                gc.restore();
            }
            gc.restore();

            gpetalY[i]     += gpetalSpeedY[i];
            gpetalX[i]     += gpetalSpeedX[i] + Math.sin(gpetalAngle[i] * 2) * 0.25;
            gpetalAngle[i] += gpetalSpin[i];
            if (gpetalY[i] > HEIGHT + 20) {
                gpetalY[i] = -20;
                gpetalX[i] = new Random().nextDouble() * WIDTH;
            }
        }
        gc.setGlobalAlpha(1.0);
    }

    // ------------------------------------------------------------------ //
    //  Clase interna: RippleEffect
    // ------------------------------------------------------------------ //

    private static class RippleEffect {
        final double x, y;
        final String color;
        private double life = 1.0;
        private static final double MAX_RADIUS = 120;

        RippleEffect(double x, double y, String color) {
            this.x = x; this.y = y; this.color = color;
        }

        void   update()    { life -= 0.035; }
        double alpha()     { return Math.max(0, life); }
        double progress()  { return 1.0 - life; }
        double radius()    { return progress() * MAX_RADIUS; }
        boolean isDead()   { return life <= 0; }
    }
}