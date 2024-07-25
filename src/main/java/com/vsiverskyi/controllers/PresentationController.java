package com.vsiverskyi.controllers;

import com.vsiverskyi.model.GameStatistics;
import com.vsiverskyi.model.Player;
import com.vsiverskyi.model.Role;
import com.vsiverskyi.service.GameService;
import com.vsiverskyi.service.GameStatisticsService;
import com.vsiverskyi.utils.SettingsUtil;
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
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.*;

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
    private ViewController viewController;
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
    private Label startLabel;
    @FXML
    private Label presentationPlayerId;
    @FXML
    private Button startVoting;
    @FXML
    private Button skip;
    @FXML
    private Button nextPlayerButton;
    @FXML
    private Button technicalDefeatPeaceful;
    @FXML
    private Button technicalDefeatMafia;
    @FXML
    private Button fullScreen;
    @FXML
    private ListView<HBox> playerCardListView;
    private List<GameStatistics> gameStatisticsList;
    private Map<Integer, Button> playerIdButton;
    private Queue<Integer> gamersOrder;
    //set the delay as 0
    Timeline countDownTimeLine;
    private int secondsPerPresentation = 5;
    private int secondsTillEnd = 5;
    private boolean presentationFinished;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.stage = StarterController.primaryStage;
        scene = new Scene(presentationAp);
        stage.setMaximized(true);
        stage.setScene(scene);
        scene.getStylesheets().add(getClass().getResource("/style/style.css").toExternalForm());
        fullScreen.setOnAction(ev -> stage.setFullScreen(true));

        playerIdButton = new HashMap<>();
        startVoting.setStyle(StyleConstants.IDLE_BUTTON_STYLE);
        startVoting.setOnMouseEntered(e -> startVoting.setStyle(HOVERED_BUTTON_STYLE));
        startVoting.setOnMouseExited(e -> startVoting.setStyle(IDLE_BUTTON_STYLE));

        nextPlayerButton.setStyle(StyleConstants.IDLE_BUTTON_STYLE);
        nextPlayerButton.setOnMouseEntered(e -> nextPlayerButton.setStyle(HOVERED_BUTTON_STYLE));
        nextPlayerButton.setOnMouseExited(e -> nextPlayerButton.setStyle(IDLE_BUTTON_STYLE));

        skip.setStyle(StyleConstants.IDLE_BUTTON_STYLE);
        skip.setOnMouseEntered(e -> skip.setStyle(HOVERED_BUTTON_STYLE));
        skip.setOnMouseExited(e -> skip.setStyle(IDLE_BUTTON_STYLE));

        technicalDefeatPeaceful.setOnAction(e -> penaltyController.assignTechnicalDefeat("PEACE"));
        technicalDefeatMafia.setOnAction(e -> penaltyController.assignTechnicalDefeat("MAFIA"));

        gameStatisticsList = gameStatisticsService
                .getGameStatisticsByGameIdSortedByInGameNumber(SelectionController.currentGameId);
        penaltyController.initializePlayerCardList(gameStatisticsList, stage, this, playerCardListView);

        displayRolePlayers(gameStatisticsList.size());
        startVoting.setOnAction(actionEvent -> startPresentation());
        nextPlayerButton.setOnAction(actionEvent -> skipToNextPlayer());
        skip.setOnAction(event -> {
            if (countDownTimeLine != null) {
                countDownTimeLine.stop();
            }
            fxWeaver.loadController(VotingController.class).show();
        });
        stage.setFullScreen(true);
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
            Circle avatar = new Circle(18); // Example avatar
            if (gameStatistics != null) {
                Image avatarImage = null;
                try {
                    avatarImage = new Image("images/" + gameStatistics.getRole().getRoleNameConstant() + ".jpg");
                } catch (Exception e) {
                    avatarImage = new Image("images/icon.jpg");
                }
                // Create an ImagePattern using the loaded image
                ImagePattern imagePattern = new ImagePattern(avatarImage);
                // Set the ImagePattern as the fill for the Circle
                avatar.setFill(imagePattern);
            }

            VBox playerPanel = createPlayerPanel(x, y);
            // Create an HBox to hold the avatar and other elements
            HBox avatarContainer = new HBox();
            avatarContainer.setAlignment(Pos.CENTER_LEFT); // Align content to the left
            avatarContainer.setSpacing(10); // Adjust spacing as needed
            avatarContainer.setPadding(new Insets(0, 0, 0, 10)); // Add padding from the left side
            avatarContainer.getChildren().add(avatar);
            Label roleLabel = new Label("");
            roleLabel.setStyle("-fx-text-fill: #f4ff67; -fx-border-radius: 5px; -fx-font-size: 12px;");
            // Create an HBox to hold the nickname label and the role label
            if (gameStatistics != null) {
                Role role = gameStatistics.getRole();
                roleLabel.setText(role.getTitle());
                avatarContainer.getChildren().add(roleLabel);
            }

            int yellowCardsIterator = Objects.isNull(gameStatistics) ? 0 : gameStatistics.getYellowCards();
            int redCardsIterator = Objects.isNull(gameStatistics) ? 0 : gameStatistics.getRedCards();
            int inGameNumber = Objects.isNull(gameStatistics) ? 0 : gameStatistics.getInGameNumber();

            viewController.showCards(
                    yellowCardsIterator,
                    redCardsIterator,
                    avatarContainer,
                    inGameNumber,
                    this,
                    gameStatisticsList.size(),
                    stage,
                    playerCardListView,
                    gameStatisticsList
            );
            playerPanel.getChildren().add(avatarContainer);

            if (i > 0 && i < totalPlayers + 1) {
                HBox hbox = new HBox();
                hbox.setSpacing(10); // Adjust spacing as needed
                // Set a transparent background for the HBox
                hbox.setStyle("-fx-background-color: rgba(31,31,31,0.5); -fx-border-radius: 5px; ");
                hbox.setPadding(new Insets(0, 0, 0, 10));
                hbox.getChildren().addAll(viewController.createNicknameLabel(i, gameStatisticsList));
                playerPanel.getChildren().add(hbox);
            }
            presentationPlayersPane.getChildren().add(playerPanel);
            Button button = createPlayerButton(x, y, i);
            if (!checkIfAlive(i, totalPlayers)) {
//                playerPanel.setDisable(true);
                playerPanel.setVisible(true);
                avatar.setFill(Color.DARKGREY);
                button.setDisable(true);
            }

            if (i == 0 || i == totalPlayers + 1) {
                playerPanel.setVisible(false);
                button.setVisible(false);
            } else {
                playerIdButton.put(i, button);
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

    private void endEachPlayerPresentation() {
        // TODO: поміняти не нормальні змінні, а не в коді
        secondsTillEnd = SettingsUtil.getSecondsPerDiscussion();
        presentationPlayerId.setText("-");
        countDownTimeLine = new Timeline(new KeyFrame(Duration.seconds(1), (ActionEvent event) -> {
            secondsLeft.setText(String.valueOf(secondsTillEnd--));
        }));
        // Set number of cycles (remaining duration in seconds):
        countDownTimeLine.setCycleCount((int) SettingsUtil.getSecondsPerDiscussion());
        countDownTimeLine.setOnFinished(event -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.initOwner(stage);
            alert.show();
            alert.setOnHidden(evt -> startVoting());
        });
        countDownTimeLine.play();
        return;
    }

    private void doPresentation(int index) {
        //обговорення після
        if (gamersOrder.isEmpty()) { // якщо всі проголосували - почати обговорення
            endEachPlayerPresentation();
        }
        GameStatistics gameStatistics = gameStatisticsList.get(index);
        if (gameStatistics != null) {
            // Create time line to lower remaining duration every second:
            if (!gameStatistics.isInGame()) {
                gamersOrder.remove();
                Integer currentPlayerNumber = gamersOrder.peek();
                if (currentPlayerNumber == null) {
                    endEachPlayerPresentation();
                } else {
                    doPresentation(currentPlayerNumber - 1);
                }
            } else {
                secondsTillEnd = SettingsUtil.getSecondsPerPresentation();
                presentationPlayerId.setText(gameStatistics.getInGameNumber().toString());
                countDownTimeLine = new Timeline(new KeyFrame(Duration.seconds(1), (ActionEvent event) -> {
                    secondsLeft.setText(String.valueOf(secondsTillEnd--));
                    presentationPlayerId.setText(gameStatistics.getInGameNumber().toString());
                }));
                // Set number of cycles (remaining duration in seconds):
                countDownTimeLine.setCycleCount(SettingsUtil.getSecondsPerPresentation());
                // Show alert when time is up:
                int finalIndex = index + 1;
                countDownTimeLine.setOnFinished(event -> {
//                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
//                    alert.initOwner(stage);
//                    alert.show();
//                    alert.setOnHidden(evt -> doPresentation(finalIndex));
                    gamersOrder.remove();
                    Integer nextGamer = gamersOrder.peek();
                    if (nextGamer == null) {
                        endEachPlayerPresentation();
                    } else {
                        doPresentation(nextGamer - 1);
                    }
                });
                countDownTimeLine.play();
            }
        }
    }

    private void startPresentation() {
        startLabel.setText("Оберіть гравця, з якого розпочнемо");

        for (Map.Entry<Integer, Button> entry : playerIdButton.entrySet()) {
            Button button = entry.getValue();
            button.setOnAction(actionEvent -> {
                ButtonType foo = new ButtonType("За годинниковою стрілкою", ButtonBar.ButtonData.YES);
                ButtonType bar = new ButtonType("Проти годинникової стрілки", ButtonBar.ButtonData.YES);
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Виберіть напрямок", foo, bar);
                alert.setTitle("Напрямок голосування");
                alert.initOwner(stage);
                alert.setHeaderText("Оберіть напрямок голосування");
                alert.setResizable(false);
                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get().getText().equals("За годинниковою стрілкою")) {
                    gamersOrder = getOrderOfInGameNumbersNaturalOrder(entry.getKey() - 1);
                    doPresentation(entry.getKey() - 1);
                    startLabel.setText("");
                    blockAllButtons();
                } else {
                    gamersOrder = getOrderOfInGameNumbersReverseOrder(entry.getKey() - 1);
                    doPresentation(entry.getKey() - 1);
                    startLabel.setText("");
                    blockAllButtons();
                }
            });
        }
    }

    private void blockAllButtons() {
        for (Button button : playerIdButton.values()) {
            button.setDisable(true);
        }
        startVoting.setDisable(true);
    }

    private LinkedList<Integer> getOrderOfInGameNumbersNaturalOrder(int beginFromIndex) {
        LinkedList<Integer> inGameNumbers = new LinkedList<>();
        // add in game numbers from selected to the end
        for (int i = beginFromIndex; i < gameStatisticsList.size(); i++) {
            inGameNumbers.add(gameStatisticsList.get(i).getInGameNumber());
        }
        // add in game numbers before selected
        for (int i = 0; i < beginFromIndex; i++) {
            inGameNumbers.add(gameStatisticsList.get(i).getInGameNumber());
        }
        return inGameNumbers;
    }

    private LinkedList<Integer> getOrderOfInGameNumbersReverseOrder(int beginFromIndex) {
        LinkedList<Integer> inGameNumbers = new LinkedList<>();
        // add in game numbers from selected to the end
        for (int i = beginFromIndex; i >= 0; i--) {
            inGameNumbers.add(gameStatisticsList.get(i).getInGameNumber());
        }
        // add in game numbers before selected
        for (int i = gameStatisticsList.size() - 1; i > beginFromIndex; i--) {
            inGameNumbers.add(gameStatisticsList.get(i).getInGameNumber());
        }
        return inGameNumbers;
    }

    private void skipToNextPlayer() {
        if (countDownTimeLine != null) {
            countDownTimeLine.stop();
            gamersOrder.remove();
            Integer currentPlayerNumber = gamersOrder.peek();
            if (currentPlayerNumber == null) {
                endEachPlayerPresentation();
            } else {
                doPresentation(currentPlayerNumber - 1);
            }
        }
    }

    private void startVoting() {
        fxWeaver.loadController(VotingController.class).show();
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
