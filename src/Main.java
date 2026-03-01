import Controller.GameController;
import Model.Note;
import UI.CanvasState;
import UI.GameView;
import UI.MainFrame;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class Main extends Application {

    @Override
    public void start(Stage stage) {

        CanvasState canvasState = new CanvasState();
        MainFrame frame = new MainFrame(stage, canvasState);
        frame.showMenu();
    }

    public static void main(String[] args) {
        launch();
    }
}