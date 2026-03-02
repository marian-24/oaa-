package UI;

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

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

/**
 * Pantalla de selección de mapas manuales (.rhythmmap).
 * Separada de SongSelectView para no mezclar audios con mapas.
 */
public class MapSelectView extends VBox {

    private final Button newMapButton;
    private final Button backButton;

    private Consumer<File> onMapSelected;   // jugar un mapa existente
    private Runnable       onNewMap;        // crear un mapa nuevo

    public MapSelectView() {
        setAlignment(Pos.TOP_CENTER);
        setSpacing(16);
        setPadding(new Insets(30, 40, 30, 40));
        setStyle("-fx-background-color: #000010;");

        // Título
        Label title = new Label("MAP EDITOR");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 42));
        title.setTextFill(Color.web("#81C784"));

        // Ruta de la carpeta de mapas
        Label folderLabel = new Label("📁  maps folder: " + SongLibrary.getMapsFolderPath());
        folderLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 13));
        folderLabel.setTextFill(Color.web("#446644"));
        folderLabel.setWrapText(true);

        // Lista de mapas existentes
        List<SongEntry> maps = SongLibrary.loadMaps();

        VBox mapList = new VBox(8);
        mapList.setPadding(new Insets(8));

        if (maps.isEmpty()) {
            Label empty = new Label("No maps yet.\nCreate your first map with the button below!");
            empty.setFont(Font.font("Arial", FontWeight.NORMAL, 15));
            empty.setTextFill(Color.web("#666688"));
            empty.setWrapText(true);
            mapList.getChildren().add(empty);
        } else {
            for (SongEntry map : maps) {
                mapList.getChildren().add(buildMapCard(map));
            }
        }

        ScrollPane scroll = new ScrollPane(mapList);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(300);
        scroll.setStyle("-fx-background: #000020; -fx-background-color: #000020; -fx-border-color: #1a3a1a;");

        // Botones
        newMapButton = styledButton("✏  New Map",      "#1B5E20", "#2E7D32");
        backButton   = styledButton("←  Back to Menu", "#2a2a50", "#3a3a70");

        HBox buttons = new HBox(16, newMapButton, backButton);
        buttons.setAlignment(Pos.CENTER);

        getChildren().addAll(title, folderLabel, scroll, buttons);
    }

    // ------------------------------------------------------------------ //

    private HBox buildMapCard(SongEntry map) {
        HBox card = new HBox(16);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(12, 16, 12, 16));
        card.setStyle(normalStyle());

        Label icon = new Label("🗺");
        icon.setFont(Font.font("Arial", FontWeight.BOLD, 22));

        VBox info = new VBox(3);
        Label nameLabel = new Label(map.displayName());
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        nameLabel.setTextFill(Color.WHITE);

        Label subLabel = new Label("Manual map  ·  .rhythmmap");
        subLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 13));
        subLabel.setTextFill(Color.web("#668866"));

        info.getChildren().addAll(nameLabel, subLabel);
        HBox.setHgrow(info, Priority.ALWAYS);

        // Botón Play individual
        Button playBtn = new Button("▶ Play");
        playBtn.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        String pb = "-fx-background-color:#1565C0;-fx-text-fill:white;-fx-background-radius:15;-fx-padding:6 16 6 16;-fx-cursor:hand;";
        String ph = "-fx-background-color:#1E88E5;-fx-text-fill:white;-fx-background-radius:15;-fx-padding:6 16 6 16;-fx-cursor:hand;";
        playBtn.setStyle(pb);
        playBtn.setOnMouseEntered(e -> playBtn.setStyle(ph));
        playBtn.setOnMouseExited(e  -> playBtn.setStyle(pb));
        playBtn.setOnAction(e -> { if (onMapSelected != null) onMapSelected.accept(map.file()); });

        card.getChildren().addAll(icon, info, playBtn);

        card.setOnMouseEntered(e -> card.setStyle(hoverStyle()));
        card.setOnMouseExited(e  -> card.setStyle(normalStyle()));

        return card;
    }

    // ------------------------------------------------------------------ //

    private String normalStyle() {
        return "-fx-background-color:#0d1f0d;-fx-background-radius:10;" +
                "-fx-border-color:#1e4a1e;-fx-border-radius:10;-fx-cursor:hand;";
    }

    private String hoverStyle() {
        return "-fx-background-color:#1a3a1a;-fx-background-radius:10;" +
                "-fx-border-color:#81C784;-fx-border-radius:10;-fx-cursor:hand;";
    }

    private Button styledButton(String text, String base, String hover) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        String bs = "-fx-background-color:" + base + ";-fx-text-fill:white;" +
                "-fx-background-radius:25;-fx-padding:11 30 11 30;-fx-cursor:hand;";
        String hs = "-fx-background-color:" + hover + ";-fx-text-fill:white;" +
                "-fx-background-radius:25;-fx-padding:11 30 11 30;-fx-cursor:hand;";
        btn.setStyle(bs);
        btn.setOnMouseEntered(e -> btn.setStyle(hs));
        btn.setOnMouseExited(e  -> btn.setStyle(bs));
        return btn;
    }

    // ------------------------------------------------------------------ //
    //  API pública
    // ------------------------------------------------------------------ //

    public void setOnMapSelected(Consumer<File> handler) { this.onMapSelected = handler; }
    public void setOnNewMap(Runnable handler)             { this.onNewMap = handler; }

    public Button getNewMapButton() { return newMapButton; }
    public Button getBackButton()   { return backButton; }
}