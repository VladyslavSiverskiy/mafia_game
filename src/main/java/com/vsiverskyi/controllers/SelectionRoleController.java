package com.vsiverskyi.controllers;

import com.vsiverskyi.exception.NoGameWithSuchIdException;
import com.vsiverskyi.model.GameStatistics;
import com.vsiverskyi.model.Player;
import com.vsiverskyi.service.GameService;
import com.vsiverskyi.service.GameStatisticsService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

@Component
@FxmlView("SelectionRole.fxml")
public class SelectionRoleController implements Initializable {

    @Autowired
    private GameService gameService;
    @Autowired
    private GameStatisticsService gameStatisticsService;
    @Autowired
    private FxWeaver fxWeaver;
    private Stage stage;
    private Scene scene;
    private Parent root;
    @FXML
    private AnchorPane selectionRoleAP;
    @FXML
    private AnchorPane selectionRolePane;
    @FXML
    private Label roleName;
    @FXML
    private Button startVoting;
    private List<GameStatistics> gameStatisticsList;
    private int currentPlayerIndex;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.stage = StarterController.primaryStage;
        stage.setScene(new Scene(selectionRoleAP));
        stage.setMaximized(true);
        try {
            gameStatisticsList = gameStatisticsService.getGameStatisticsByGameId(SelectionController.currentGameId);
        }catch (NoGameWithSuchIdException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, e.getMessage());
            alert.show();
        }

        int totalPlayers = gameStatisticsList.size();
        displayRolePlayers(totalPlayers);

        startVoting.setOnAction(actionEvent -> fxWeaver.loadController(PresentationController.class).show());
    }

    public void displayRolePlayers(int totalPlayers) {
        double centerX = selectionRolePane.getWidth() / 2;
        double centerY = selectionRolePane.getHeight() / 2;
        double radius = Math.min(centerX, centerY) - 5;
        double startAngle = Math.PI / 1.8 ;

        for (int i = 0; i < totalPlayers + 2; i++) { //
            double angle = startAngle + 2 * Math.PI * i / (totalPlayers + 2);
            double x = centerX + radius * Math.cos(angle);
            double y = centerY + radius * Math.sin(angle);

            // Create a panel to represent each player
            VBox playerPanel = new VBox();
            playerPanel.setAlignment(Pos.CENTER);
            playerPanel.setLayoutX(x - 50); // Offset to center panel
            playerPanel.setLayoutY(y - 50); // Offset to center panel
            playerPanel.setSpacing(5); // Adjust spacing as needed

            Circle avatar = new Circle(18, Color.LIGHTGRAY); // Example avatar
            playerPanel.getChildren().add(avatar);

            Player player = null;
            System.out.println(i);
            if (i > 0 && i < totalPlayers + 1) {
                player = gameStatisticsList.get(i-1)
                        .getPlayer();
            }
            Label nicknameLabel = new Label();
            if (player != null && player.getNickname() != null) {
                nicknameLabel.setText(player.getNickname());
            } else {
                nicknameLabel.setText("Незнайомець"); // You can set a default text if player or nickname is null
            }
            nicknameLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #ffffff");
            playerPanel.getChildren().add(nicknameLabel);

            selectionRolePane.getChildren().add(playerPanel);
            Button button = new Button(String.valueOf(i));

            button.setLayoutX(x - 46);
            button.setLayoutY(y - 33);
            button.setStyle("-fx-background-color: #161616; -fx-text-fill: #ffffff;  -fx-border-color: #ffffff; -fx-border-radius: 5px;");
//            button.setOnAction(event -> handleButtonClick());
//            button.setDisable(true);
            if (i == 0 || i == totalPlayers + 1) {
                playerPanel.setVisible(false);
                button.setVisible(false);
            }
            selectionRolePane.getChildren().add(button);
        }
    }

    public void show() {
        stage.show();
    }
}
