package UI;

public class MenuController {

    public MenuController(MenuView view, MainFrame frame) {

        view.getStartButton().setOnAction(e -> {
            view.stopAnimation();
            frame.startArcadeGame();
        });

        view.getSongButton().setOnAction(e -> {
            view.stopAnimation();
            frame.showSongSelect();
        });

        view.getEditorButton().setOnAction(e -> {
            view.stopAnimation();
            frame.showMapEditorSelect();
        });
    }
}