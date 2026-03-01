package UI;

import Model.HighScoreManager;
import Model.SongEntry;
import Model.SongLibrary;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

/**
 * Pantalla de selección de canción.
 *
 * Muestra:
 *  - Lista scrolleable de canciones en la carpeta songs/ con nombre, duración y high score
 *  - Botón para subir un archivo manualmente
 *  - Ruta de la carpeta songs/ para que el usuario sepa dónde poner sus archivos
 */
public class SongSelectView extends VBox {

    private Button uploadButton;
    private Button backButton;
    private File selectedFile = null;

    // Callback que se llama cuando el usuario selecciona una canción (de la lista o manual)
    private Consumer<File> onSongSelected;

    public SongSelectView(HighScoreManager highScoreManager) {
        setAlignment(Pos.TOP_CENTER);
        setSpacing(16);
        setPadding(new Insets(30, 40, 30, 40));
        setStyle("-fx-background-color: #000010;");

        // --- Título ---
        Label title = new Label("SONG MODE");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 42));
        title.setTextFill(Color.WHITE);

        // --- Ruta de la carpeta ---
        Label folderLabel = new Label("📁  songs folder: " + SongLibrary.getSongsFolderPath());
        folderLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 13));
        folderLabel.setTextFill(Color.web("#666688"));
        folderLabel.setWrapText(true);

        // --- Lista de canciones ---
        List<SongEntry> songs = SongLibrary.loadAll();

        VBox songList = new VBox(8);
        songList.setPadding(new Insets(8));

        if (songs.isEmpty()) {
            Label empty = new Label("No songs found in the songs/ folder.\nAdd WAV or AIFF files there and restart.");
            empty.setFont(Font.font("Arial", FontWeight.NORMAL, 15));
            empty.setTextFill(Color.web("#888888"));
            empty.setWrapText(true);
            songList.getChildren().add(empty);
        } else {
            for (SongEntry song : songs) {
                songList.getChildren().add(buildSongCard(song, highScoreManager));
            }
        }

        ScrollPane scroll = new ScrollPane(songList);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(320);
        scroll.setStyle("-fx-background: #000020; -fx-background-color: #000020; -fx-border-color: #1a1a4a;");

        // --- Separador ---
        Label orLabel = new Label("— or upload a file manually —");
        orLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        orLabel.setTextFill(Color.web("#555577"));

        // --- Botones ---
        uploadButton = styledButton("📂  Upload .WAV", "#1565C0", "#1E88E5");
        backButton   = styledButton("←  Back to Menu",      "#2a2a50", "#3a3a70");

        HBox buttons = new HBox(16, uploadButton, backButton);
        buttons.setAlignment(Pos.CENTER);

        getChildren().addAll(title, folderLabel, scroll, orLabel, buttons);
    }

    // ------------------------------------------------------------------ //
    //  Card de canción individual
    // ------------------------------------------------------------------ //

    private HBox buildSongCard(SongEntry song, HighScoreManager hsManager) {
        HBox card = new HBox(16);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(12, 16, 12, 16));
        card.setStyle(
                "-fx-background-color: #0d0d2e;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: #1e1e5e;" +
                        "-fx-border-radius: 10;" +
                        "-fx-cursor: hand;"
        );

        // Icono musical
        Label icon = new Label("♪");
        icon.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        icon.setTextFill(Color.web("#A78BFA"));

        // Info central (nombre + duración)
        VBox info = new VBox(3);
        Label nameLabel = new Label(song.displayName());
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        nameLabel.setTextFill(Color.WHITE);

        String durText = song.durationSeconds() > 0
                ? "⏱  " + song.formattedDuration()
                : "⏱  unknown";
        Label durLabel = new Label(durText);
        durLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 13));
        durLabel.setTextFill(Color.web("#8888AA"));

        info.getChildren().addAll(nameLabel, durLabel);
        HBox.setHgrow(info, Priority.ALWAYS);

        // High score de esta canción
        int hs = hsManager.getSongHighScore(song.file().getName());
        Label hsLabel = new Label(hs > 0 ? "🏆 " + hs : "—");
        hsLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        hsLabel.setTextFill(hs > 0 ? Color.GOLD : Color.web("#444466"));

        card.getChildren().addAll(icon, info, hsLabel);

        // Hover
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: #1a1a4a;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: #A78BFA;" +
                        "-fx-border-radius: 10;" +
                        "-fx-cursor: hand;"
        ));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: #0d0d2e;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: #1e1e5e;" +
                        "-fx-border-radius: 10;" +
                        "-fx-cursor: hand;"
        ));

        // Click → seleccionar canción
        card.setOnMouseClicked(e -> {
            if (onSongSelected != null) onSongSelected.accept(song.file());
        });

        return card;
    }

    // ------------------------------------------------------------------ //
    //  FileChooser manual
    // ------------------------------------------------------------------ //

    public void openFileChooser(Stage stage) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Audio File");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Audio Files", "*.wav", "*.mp3", "*.aiff"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        // Abrir directamente en la carpeta songs/
        fc.setInitialDirectory(SongLibrary.getSongsFolder());

        File file = fc.showOpenDialog(stage);
        if (file != null) {
            selectedFile = file;
            if (onSongSelected != null) onSongSelected.accept(file);
        }
    }

    // ------------------------------------------------------------------ //
    //  Helpers
    // ------------------------------------------------------------------ //

    private Button styledButton(String text, String base, String hover) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        String bs = style(base);
        String hs = style(hover);
        btn.setStyle(bs);
        btn.setOnMouseEntered(e -> btn.setStyle(hs));
        btn.setOnMouseExited(e  -> btn.setStyle(bs));
        return btn;
    }

    private String style(String color) {
        return "-fx-background-color: " + color + ";" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 25;" +
                "-fx-padding: 11 30 11 30;" +
                "-fx-cursor: hand;";
    }

    // ------------------------------------------------------------------ //
    //  API pública
    // ------------------------------------------------------------------ //

    /** Se llama cuando el usuario hace click en una canción de la lista o sube un archivo */
    public void setOnSongSelected(Consumer<File> handler) { this.onSongSelected = handler; }

    public File getSelectedFile()   { return selectedFile; }
    public Button getUploadButton() { return uploadButton; }
    public Button getBackButton()   { return backButton; }
}