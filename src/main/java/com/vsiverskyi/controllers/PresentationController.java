package com.vsiverskyi.controllers;

import com.vsiverskyi.model.GameStatistics;
import com.vsiverskyi.model.Player;
import com.vsiverskyi.model.Role;
import com.vsiverskyi.service.GameService;
import com.vsiverskyi.service.GameStatisticsService;
import com.vsiverskyi.utils.StyleConstants;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

import static com.vsiverskyi.utils.StyleConstants.HOVERED_BUTTON_STYLE;
import static com.vsiverskyi.utils.StyleConstants.IDLE_BUTTON_STYLE;

@Component
@RequiredArgsConstructor
@FxmlView("Presentation.fxml")
public class PresentationController implements Initializable, DisplayedPlayersController {
    private Stage stage;
    private Scene scene;
    private Parent root;
    @Autowired
    private GameService gameService;
    @Autowired
    private GameStatisticsService gameStatisticsService;
    @Autowired
    private PenaltyController penaltyController;
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
    @FXML
    private Button technicalDefeatPeaceful;
    @FXML
    private Button technicalDefeatMafia;
    @FXML
    private Button fullScreen;
    @FXML
    private ListView<HBox> playerCardListView;
    private List<GameStatistics> gameStatisticsList;
    //set the delay as 0
    private int secondsPerPresentation = 5;
    private int secondsTillEnd = 5;
    private boolean presentationFinished;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.stage = StarterController.primaryStage;
        scene = new Scene(presentationAp);
        stage.setMaximized(true);
        stage.setFullScreen(true);
        stage.setScene(scene);
        scene.getStylesheets().add(getClass().getResource("/style/style.css").toExternalForm());
        fullScreen.setOnAction(ev -> stage.setFullScreen(true));

        startVoting.setStyle(StyleConstants.IDLE_BUTTON_STYLE);
        startVoting.setOnMouseEntered(e -> startVoting.setStyle(HOVERED_BUTTON_STYLE));
        startVoting.setOnMouseExited(e -> startVoting.setStyle(IDLE_BUTTON_STYLE));

        technicalDefeatPeaceful.setOnAction(e -> penaltyController.assignTechnicalDefeat("PEACE"));
        technicalDefeatMafia.setOnAction(e -> penaltyController.assignTechnicalDefeat("MAFIA"));

        gameStatisticsList = gameStatisticsService
                .getGameStatisticsByGameIdSortedByInGameNumber(SelectionController.currentGameId);
        penaltyController.initializePlayerCardList(gameStatisticsList, stage, this, playerCardListView);

        displayRolePlayers(gameStatisticsList.size());
        startVoting.setOnAction(actionEvent -> startPresentation(0));
        skip.setOnAction(event -> fxWeaver.loadController(VotingController.class).show());
    }

    @Override
    public void displayRolePlayers(int totalPlayers) {
        // Clear the previous content from the selectionRolePane
        presentationPlayersPane.getChildren().clear();

        double centerX = presentationPlayersPane.getWidth() / 2;
        double centerY = presentationPlayersPane.getHeight() / 2;
        double radius = Math.min(centerX, centerY) - 3;
        double startAngle = Math.PI / 1.8;

        for (int i = 0; i < totalPlayers + 2; i++) { //
            double angle = startAngle + 2 * Math.PI * i / (totalPlayers + 2);
            double x = centerX + radius * Math.cos(angle);
            double y = centerY + radius * Math.sin(angle);

            GameStatistics gameStatistics = null;
            if (i != 0 && i != totalPlayers + 1) {
                int finalI1 = i;
                gameStatisticsList = gameStatisticsService
                        .getGameStatisticsByGameIdSortedByInGameNumber(SelectionController.currentGameId);
                gameStatistics = gameStatisticsList
                        .stream()
                        .filter(gs -> gs.getInGameNumber() == finalI1).findFirst().get();
            }

            // Create a panel to represent each player
            Circle avatar = new Circle(18, Color.LIGHTGRAY); // Example avatar
            VBox playerPanel = createPlayerPanel(x, y);
            // Create an HBox to hold the avatar and other elements
            HBox avatarContainer = new HBox();
            avatarContainer.setAlignment(Pos.CENTER_LEFT); // Align content to the left
            avatarContainer.setSpacing(10); // Adjust spacing as needed
            avatarContainer.setPadding(new Insets(0, 0, 0, 10)); // Add padding from the left side
            avatarContainer.getChildren().add(avatar);

            int yellowCardsIterator = Objects.isNull(gameStatistics) ? 0 : gameStatistics.getYellowCards();
            // Add small yellow cards in a row near the circle avatar
            for (int j = 0; j < yellowCardsIterator; j++) { // Adjust the number of yellow cards as needed
                Rectangle yellowCard = new Rectangle(8, 12, Color.YELLOW);
                yellowCard.setStyle("-fx-border-radius: 1px");
                avatarContainer.getChildren().add(yellowCard);
            }
            playerPanel.getChildren().add(avatarContainer);


            if (i > 0 && i < totalPlayers + 1) {
                Label roleLabel = new Label("");
                roleLabel.setStyle("-fx-text-fill: #f4ff67; -fx-border-radius: 5px; -fx-font-size: 12px;");
                // Create an HBox to hold the nickname label and the role label
                Role role = gameStatistics.getRole();
                if (role != null) {
                    roleLabel.setText(role.getTitle());
                }
                HBox hbox = new HBox();
                hbox.setSpacing(10); // Adjust spacing as needed
                // Set a transparent background for the HBox
                hbox.setStyle("-fx-background-color: rgba(31,31,31,0.5); -fx-border-radius: 5px; ");
                hbox.setPadding(new Insets(0, 0, 0, 10));
                hbox.getChildren().addAll(roleLabel, createNicknameLabel(i));
                playerPanel.getChildren().add(hbox);
            }
            presentationPlayersPane.getChildren().add(playerPanel);
            Button button = createPlayerButton(x, y, i);
            if (!checkIfAlive(i, totalPlayers)) {
                playerPanel.setDisable(true);
                playerPanel.setVisible(true);
                avatar.setFill(Color.DARKGREY);
                button.setDisable(true);
            }

            if (i == 0 || i == totalPlayers + 1) {
                playerPanel.setVisible(false);
                button.setVisible(false);
            }
            presentationPlayersPane.getChildren().add(button);
        }
    }

    /**
     * returns true if player is alive
     * TODO: make that method in one controller, and reuse it
     */
    private Boolean checkIfAlive(int playerNumber, int totalPlayers) {
        return playerNumber != 0 && playerNumber != totalPlayers + 1
               && gameStatisticsService
                       .getGameStatisticsByGameIdSortedByInGameNumber(SelectionController.currentGameId)
                       .get(playerNumber - 1).isInGame();
    }

    private void startPresentation(int index) {
        //обговорення після
        if (index > gameStatisticsList.size() - 1) {
            // TODO: поміняти не нормальні змінні, а не в коді
            secondsTillEnd = 50;
            presentationPlayerId.setText("-");
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
            if (!gameStatistics.isInGame()) {
                int finalIndex = index + 1;
                startPresentation(finalIndex);
            } else {
                secondsTillEnd = 5;
                System.out.println(gameStatistics);
                presentationPlayerId.setText(gameStatistics.getInGameNumber().toString());
                Timeline countDownTimeLine = new Timeline(new KeyFrame(Duration.seconds(1), (ActionEvent event) -> {
                    secondsLeft.setText(String.valueOf(secondsTillEnd--));
                    presentationPlayerId.setText(gameStatistics.getInGameNumber().toString());
                }));
                // Set number of cycles (remaining duration in seconds):
                countDownTimeLine.setCycleCount((int) secondsPerPresentation);
                // Show alert when time is up:
                int finalIndex = index + 1;
                countDownTimeLine.setOnFinished(event -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.initOwner(stage);
                    alert.show();
                    alert.setOnHidden(evt -> startPresentation(finalIndex));
                });
                countDownTimeLine.play();
            }
        }
    }

    private void startVoting() {
        fxWeaver.loadController(VotingController.class).show();
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
