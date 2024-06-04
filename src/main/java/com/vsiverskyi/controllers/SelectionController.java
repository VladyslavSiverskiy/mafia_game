package com.vsiverskyi.controllers;

import com.vsiverskyi.model.GameStatistics;
import com.vsiverskyi.service.GameService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

@Component
@FxmlView("Selection.fxml")
public class SelectionController implements Initializable {

    @Autowired
    private GameService gameService;
    @Autowired
    private FxWeaver fxWeaver;
    public static Long currentGameId;
    private Stage stage;
    private Scene scene;
    private Parent root;

    @FXML
    private GridPane selectionGrid;
    @FXML
    private AnchorPane selectionAP;
    @FXML
    private AnchorPane selectionPane;
    @FXML
    private Label roleName;
    @FXML
    private Button startVoting;
    private List<GameStatistics> gameStatisticsList;
    private int currentPlayerIndex;
    private final static Integer NUM_BUTTON_LINES = 5;
    private final static Integer BUTTONS_PER_LINE = 5;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.stage = StarterController.primaryStage;
        stage.setScene(new Scene(selectionAP));
        stage.setMaximized(true);
//        roleName.setText();
        gameStatisticsList = gameService.getGameInfo(currentGameId).getGameStatistics();
        roleName.setText(gameStatisticsList.get(0).getRole().getTitle());

        int totalPlayers = gameStatisticsList.size();

        totalPlayers = 16;


        double centerX = selectionPane.getWidth() / 2;
        double centerY = selectionPane.getHeight() / 2;
        double radius = Math.min(centerX, centerY) - 5;

        double startAngle = Math.PI / 1.8 ;

        System.out.println(totalPlayers);
        for (int i = 0; i < totalPlayers + 2; i++) {
            double angle = startAngle + 2 * Math.PI * i / (totalPlayers + 2);
            double x = centerX + radius * Math.cos(angle);
            double y = centerY + radius * Math.sin(angle);
            Button button = new Button(String.valueOf(i));
            button.setLayoutX(x - 25); // Offset to center button
            button.setLayoutY(y - 25); // Offset to center button
            button.setOnAction(event -> handleButtonClick());
            if (i == 0 || i == totalPlayers + 1) {
                button.setVisible(false);
            }
            selectionPane.getChildren().add(button);
        }


        startVoting.setOnAction(actionEvent -> fxWeaver.loadController(PresentationController.class).show());
    }

    private void handleButtonClick() {
        roleName.setText(gameStatisticsList.get(++currentPlayerIndex).getRole().getTitle());

    }

    public void show() {
        stage.show();
    }
}
