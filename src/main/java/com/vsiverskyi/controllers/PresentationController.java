package com.vsiverskyi.controllers;

import com.vsiverskyi.model.GameStatistics;
import com.vsiverskyi.model.Player;
import com.vsiverskyi.service.GameService;
import com.vsiverskyi.service.GameStatisticsService;
import com.vsiverskyi.utils.StyleConstants;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import static com.vsiverskyi.utils.StyleConstants.HOVERED_BUTTON_STYLE;
import static com.vsiverskyi.utils.StyleConstants.IDLE_BUTTON_STYLE;

@Component
@RequiredArgsConstructor
@FxmlView("Presentation.fxml")
public class PresentationController implements Initializable {
    private Stage stage;
    private Scene scene;
    private Parent root;
    @Autowired
    private GameService gameService;
    @Autowired
    private GameStatisticsService gameStatisticsService;
    @Autowired
    private FxWeaver fxWeaver;
    @FXML
    private AnchorPane presentationAp;
    @FXML
    private AnchorPane presentationPlayersPane;
    @FXML
    private Label secondsLeft;
    @FXML
    private Label presentationPlayerId;
    @FXML
    private Button startVoting;
    @FXML
    private Button skip;
    private List<GameStatistics> gameStatisticsList;
    //set the delay as 0
    private int secondsPerPresentation = 5;
    private int secondsTillEnd = 5;
    private boolean presentationFinished;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.stage = StarterController.primaryStage;
        stage.setScene(new Scene(presentationAp));
        stage.setMaximized(true);
        startVoting.setStyle(StyleConstants.IDLE_BUTTON_STYLE);
        startVoting.setOnMouseEntered(e -> startVoting.setStyle(HOVERED_BUTTON_STYLE));
        startVoting.setOnMouseExited(e -> startVoting.setStyle(IDLE_BUTTON_STYLE));


        gameStatisticsList = gameStatisticsService
                .getGameStatisticsByGameIdSortedByInGameNumber(SelectionController.currentGameId);

        displayRolePlayers(gameStatisticsList.size());
        startVoting.setOnAction(actionEvent -> startPresentation(0));
        skip.setOnAction(event -> fxWeaver.loadController(VotingController.class).show());
    }

    private void startPresentation(int index) {
        if (index > gameStatisticsList.size() - 1) {
            // TODO: поміняти не нормальні змінні, а не в коді
            secondsTillEnd = 50;
            Timeline countDownTimeLine = new Timeline(new KeyFrame(Duration.seconds(1), (ActionEvent event) -> {
                secondsLeft.setText(String.valueOf(secondsTillEnd--));
            }));
            // Set number of cycles (remaining duration in seconds):
            countDownTimeLine.setCycleCount((int) 50);
            countDownTimeLine.setOnFinished(event -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.show();
                alert.setOnHidden(evt -> startVoting());
            });
            countDownTimeLine.play();
            return;
        }
        GameStatistics gameStatistics = gameStatisticsList.get(index);
        if (gameStatistics != null) {
            // Create time line to lower remaining duration every second:
            secondsTillEnd = 5;
            System.out.println(gameStatistics);
            presentationPlayerId.setText(gameStatistics.getInGameNumber().toString());
            Timeline countDownTimeLine = new Timeline(new KeyFrame(Duration.seconds(1), (ActionEvent event) -> {
                secondsLeft.setText(String.valueOf(secondsTillEnd--));
            }));
            // Set number of cycles (remaining duration in seconds):
            countDownTimeLine.setCycleCount((int) secondsPerPresentation);
            // Show alert when time is up:
            int finalIndex = index + 1;
            countDownTimeLine.setOnFinished(event -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.show();
                alert.setOnHidden(evt -> startPresentation(finalIndex));
            });
            countDownTimeLine.play();
        }
    }

    private void startVoting() {
        fxWeaver.loadController(VotingController.class).show();
    }

    private void displayRolePlayers(int totalPlayers) {
        double centerX = presentationPlayersPane.getWidth() / 2;
        double centerY = presentationPlayersPane.getHeight() / 2;
        double radius = Math.min(centerX, centerY) - 5;
        double startAngle = Math.PI / 1.8;

        for (int i = 0; i < totalPlayers + 2; i++) { //
            double angle = startAngle + 2 * Math.PI * i / (totalPlayers + 2);
            double x = centerX + radius * Math.cos(angle);
            double y = centerY + radius * Math.sin(angle);

            // Create a panel to represent each player
            VBox playerPanel = createPlayerPanel(x, y);
            Circle avatar = new Circle(18, Color.LIGHTGRAY); // Example avatar
            playerPanel.getChildren().add(avatar);
            if (i > 0 && i < totalPlayers + 1) {
                playerPanel.getChildren().add(createNicknameLabel(i));
            }
            presentationPlayersPane.getChildren().add(playerPanel);
            Button button = createPlayerButton(x, y, i);
            int finalI = i;
            if (i == 0 || i == totalPlayers + 1) {
                playerPanel.setVisible(false);
                button.setVisible(false);
            }
            presentationPlayersPane.getChildren().add(button);
        }
    }

    private Label createNicknameLabel(int i) { // When value of button is "1", then get element with 0 index
        GameStatistics currentGamer = gameStatisticsList.get(i - 1);
        Player player = currentGamer.getPlayer();
        Label nicknameLabel = new Label();
        if (player != null) {
            nicknameLabel.setText(player.getNickname());
        } else if (currentGamer.getInGameNickname() != null) {
            nicknameLabel.setText(currentGamer.getInGameNickname());
        } else {
            nicknameLabel.setText("Незнайомець");
        }
        nicknameLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #ffffff");
        return nicknameLabel;
    }

    private VBox createPlayerPanel(double x, double y) {
        VBox playerPanel = new VBox();
        playerPanel.setAlignment(Pos.CENTER);
        playerPanel.setLayoutX(x - 50); // Offset to center panel
        playerPanel.setLayoutY(y - 50); // Offset to center panel
        playerPanel.setSpacing(5); // Adjust spacing as needed
        return playerPanel;
    }

    private Button createPlayerButton(double x, double y, int i) {
        Button button = new Button(String.valueOf(i));
        button.setLayoutX(x - 46);
        button.setLayoutY(y - 33);
        button.setStyle(IDLE_BUTTON_STYLE);
        button.setOnMouseEntered(e -> button.setStyle(HOVERED_BUTTON_STYLE));
        button.setOnMouseExited(e -> button.setStyle(IDLE_BUTTON_STYLE));
        return button;
    }

    public void show() {
        stage.show();
    }
}
