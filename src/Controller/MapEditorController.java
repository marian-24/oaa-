package Controller;

import Model.RhythmMapWriter;
import UI.MapEditorView;
import javafx.animation.AnimationTimer;

import javax.sound.sampled.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Controla la sesión de grabación del editor de mapas.
 *
 * Flujo:
 *  1. Arranca el audio y el timer
 *  2. Cada click del usuario registra un beat (tiempo actual en ms)
 *  3. Al presionar "Save" escribe el .rhythmmap y llama a onSaved
 *  4. Al presionar "Cancel" para sin guardar y llama a onCancel
 */
public class MapEditorController {

    private final File audioFile;
    private final MapEditorView view;
    private final Consumer<File> onSaved;   // recibe el .rhythmmap guardado
    private final Runnable onCancel;

    private Clip audioClip;
    private long startTimeNs;
    private long durationMs;

    private final List<Long> beatsMs       = new ArrayList<>();
    private final List<Double> beatPositions = new ArrayList<>();  // fracciones 0..1

    private AnimationTimer timer;

    public MapEditorController(File audioFile, MapEditorView view,
                               Consumer<File> onSaved, Runnable onCancel) {
        this.audioFile = audioFile;
        this.view      = view;
        this.onSaved   = onSaved;
        this.onCancel  = onCancel;

        loadAudio();
        setupInput();
        setupTimer();
    }

    // ------------------------------------------------------------------ //
    //  Inicio
    // ------------------------------------------------------------------ //

    public void start() {
        startTimeNs = System.nanoTime();
        if (audioClip != null) audioClip.start();
        timer.start();
    }

    // ------------------------------------------------------------------ //
    //  Input
    // ------------------------------------------------------------------ //

    private void setupInput() {
        // Click en cualquier parte de la vista → registrar beat
        view.setOnMouseClicked(e -> recordBeat());

        view.getFinishButton().setOnAction(e -> save());
        view.getCancelButton().setOnAction(e -> cancel());
    }

    private void recordBeat() {
        long elapsedMs = (System.nanoTime() - startTimeNs) / 1_000_000L;
        if (elapsedMs < 0 || (durationMs > 0 && elapsedMs > durationMs)) return;

        beatsMs.add(elapsedMs);
        double pos = durationMs > 0 ? (double) elapsedMs / durationMs : 0;
        beatPositions.add(pos);
    }

    // ------------------------------------------------------------------ //
    //  Loop de actualización
    // ------------------------------------------------------------------ //

    private void setupTimer() {
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                long elapsedMs = (now - startTimeNs) / 1_000_000L;

                // Auto-terminar cuando acaba la canción
                if (durationMs > 0 && elapsedMs >= durationMs) {
                    stop();
                    save();
                    return;
                }

                view.updateState(elapsedMs, beatsMs.size(),
                        List.copyOf(beatPositions), true);
            }
        };
    }

    // ------------------------------------------------------------------ //
    //  Guardar / Cancelar
    // ------------------------------------------------------------------ //

    private void save() {
        timer.stop();
        if (audioClip != null) audioClip.stop();

        if (beatsMs.isEmpty()) {
            cancel();
            return;
        }

        try {
            File mapFile = RhythmMapWriter.write(audioFile, beatsMs);
            onSaved.accept(mapFile);
        } catch (Exception ex) {
            ex.printStackTrace();
            onCancel.run();
        }
    }

    private void cancel() {
        timer.stop();
        if (audioClip != null) audioClip.stop();
        onCancel.run();
    }

    // ------------------------------------------------------------------ //
    //  Audio
    // ------------------------------------------------------------------ //

    private void loadAudio() {
        try {
            AudioInputStream stream = AudioSystem.getAudioInputStream(audioFile);
            audioClip = AudioSystem.getClip();
            audioClip.open(stream);
            durationMs = audioClip.getMicrosecondLength() / 1000;
        } catch (Exception e) {
            System.err.println("MapEditor: no se pudo cargar el audio: " + e.getMessage());
            audioClip  = null;
            durationMs = 0;
        }
    }
}