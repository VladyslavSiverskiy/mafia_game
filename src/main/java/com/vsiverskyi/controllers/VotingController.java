package com.vsiverskyi.controllers;

import com.vsiverskyi.model.GameStatistics;
import com.vsiverskyi.model.Player;
import com.vsiverskyi.model.Role;
import com.vsiverskyi.model.enums.ERoleOrder;
import com.vsiverskyi.service.GameService;
import com.vsiverskyi.service.GameStatisticsService;
import com.vsiverskyi.service.PointsService;
import com.vsiverskyi.utils.SettingsUtil;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.sql.Time;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.vsiverskyi.utils.StyleConstants.HOVERED_BUTTON_STYLE;
import static com.vsiverskyi.utils.StyleConstants.IDLE_BUTTON_STYLE;

@Component
@RequiredArgsConstructor
@FxmlView("Voting.fxml")
public class VotingController implements Initializable, DisplayedPlayersController {

    @Autowired
    private ViewController viewController;
    @Autowired
    private GameService gameService;
    @Autowired
    private PointsService pointsService;
    @Autowired
    private PenaltyController penaltyController;
    @Autowired
    private GameStatisticsService gameStatisticsService;
    @Autowired
    private FxWeaver fxWeaver;
    @Autowired
    private GeneralSettingsController generalSettingsController;
    private Stage stage;
    private Scene scene;
    private Parent root;
    @FXML
    private AnchorPane votingAp;
    @FXML
    private Label votingStateLabel;
    @FXML
    private AnchorPane votingPlayersPane;
    @FXML
    private Button beginVoting;
    @FXML
    private Button beginVotingReverse;
    @FXML
    private VBox votesDisplay;
    @FXML
    private Label secondsLeft;
    @FXML
    private Button technicalDefeatPeaceful;
    @FXML
    private Button technicalDefeatMafia;
    @FXML
    private Button fullScreen;
    @FXML
    private ListView<HBox> playerCardListView;
    @FXML
    private Button startButton;
    @FXML
    private Label startLabel;
    @FXML
    private Button resetVote;
    @FXML
    private Button discussionButton;
    private Timeline countDownTimeLine;
    private Map<Integer, Integer> playerIdVotesMap;
    private Map<Integer, Button> playerIdButton;
    private List<GameStatistics> gameStatisticsList;
    private Deque<Integer> gamersOrder;
    private Integer currentVoterIndex;
    private Integer reverseCurrentVoterIndex;
    private boolean reverse;
    private boolean kradiyHasStolenVoice;
    private Timeline excuseTimeLine;
    int secondsTillEnd = 10;

    int lastVoterNumber;
    int lastVotedNumber;
    int lastVoiceAmount;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.stage = StarterController.primaryStage;
        scene = new Scene(votingAp);
        stage.setScene(scene);
        scene.getStylesheets().add(getClass().getResource("/style/style.css").toExternalForm());
        stage.setMaximized(true);
        stage.setFullScreen(true);
        playerIdVotesMap = new HashMap<>();
        playerIdButton = new HashMap<>();
        kradiyHasStolenVoice = false;

        int secondsPerMove = SettingsUtil.getSecondsPerMove();

        gameStatisticsList = gameStatisticsService.getGameStatisticsByGameIdSortedByInGameNumber(SelectionController.currentGameId);
        reverseCurrentVoterIndex = gameStatisticsList.size() - 1;
        currentVoterIndex = 0;
// Initialize player card list view
        // Initialize technical defeat buttons
        technicalDefeatPeaceful.setOnAction(e -> penaltyController.assignTechnicalDefeat("PEACE"));
        technicalDefeatMafia.setOnAction(e -> penaltyController.assignTechnicalDefeat("MAFIA"));

        penaltyController.initializePlayerCardList(gameStatisticsList, stage, this, playerCardListView);

        displayRolePlayers(gameStatisticsList.size());

        fullScreen.setOnAction(ev -> stage.setFullScreen(true));
        ImageView imageView = new ImageView(getClass().getResource("/images/fullscreen.png").toExternalForm());
        fullScreen.setGraphic(imageView);
        imageView.fitWidthProperty().bind(fullScreen.widthProperty().divide(10));
        imageView.setPreserveRatio(true);

        startButton.setOnAction(actionEvent -> startVoting());
        resetVote.setOnAction(actionEvent -> resetVote());

        discussionButton.setOnAction(actionEvent -> endEachPlayerPresentation());
        discussionButton.setStyle(IDLE_BUTTON_STYLE);
        discussionButton.setOnMouseEntered(ev -> discussionButton.setStyle(HOVERED_BUTTON_STYLE));
        discussionButton.setOnMouseExited(ev -> discussionButton.setStyle(IDLE_BUTTON_STYLE));

        resetVote.setStyle(IDLE_BUTTON_STYLE);
        resetVote.setOnMouseEntered(ev -> resetVote.setStyle(HOVERED_BUTTON_STYLE));
        resetVote.setOnMouseExited(ev -> resetVote.setStyle(IDLE_BUTTON_STYLE));

        startButton.setStyle(IDLE_BUTTON_STYLE);
        startButton.setOnMouseEntered(ev -> startButton.setStyle(HOVERED_BUTTON_STYLE));
        startButton.setOnMouseExited(ev -> startButton.setStyle(IDLE_BUTTON_STYLE));
    }

    private void resetVote() {
        playerIdVotesMap.put(lastVotedNumber, playerIdVotesMap.get(lastVotedNumber) - lastVoiceAmount);
        System.out.println(gamersOrder);
//        Queue<Integer> q1= Collections.revers(gamersOrder);
        gamersOrder.addFirst(lastVoterNumber);
//        System.out.println(q1);
        excuseTimeLine = null;
        countDownTimeLine.stop();
        countDownTimeLine = null;
        System.out.println(gamersOrder);
        updateVotesDisplay();
        if (reverse) {
            giveVoiceReverse(lastVoterNumber - 1);
        } else {
            giveVoiceForward(lastVoterNumber - 1);
        }

    }

    private void startVoting() {
        resetTimerAndSetVotingLabel();
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
                    beginVoting(entry.getKey() - 1);
                } else {
                    beginVotingReverse(entry.getKey() - 1);
                }
            });
        }
    }


    @Override
    public void displayRolePlayers(int totalPlayers) {
        votingPlayersPane.getChildren().clear();

        double centerX = votingPlayersPane.getWidth() / 2;
        double centerY = votingPlayersPane.getHeight() / 2;
        double radius = Math.min(centerX, centerY) - 5;
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
                ImagePattern imagePattern = new ImagePattern(avatarImage);
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
            }
            avatarContainer.getChildren().add(roleLabel);

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
//                hbox.getChildren().addAll(roleLabel, createNicknameLabel(i));
                hbox.getChildren().addAll(viewController.createNicknameLabel(i, gameStatisticsList));
                playerPanel.getChildren().add(hbox);
            }

            Button button = createPlayerButton(x, y, i);

            votingPlayersPane.getChildren().add(playerPanel);
            if (!checkIfAlive(i, totalPlayers)) {
//                playerPanel.setDisable(true);
                playerPanel.setVisible(true);
                avatar.setFill(Color.DARKGREY);
                button.setDisable(true);
                button.setStyle(IDLE_BUTTON_STYLE);
            }

            if (checkIfSkipVoting(i, totalPlayers)) {
                Label label = new Label("S");
                playerPanel.getChildren().add(label);
            }

            if (checkIfMarkedByKradiy(i, totalPlayers)) {
                Label label = new Label("K");
                playerPanel.getChildren().add(label);
                kradiyHasStolenVoice = true;
            }

            if (i == 0 || i == totalPlayers + 1) {
                playerPanel.setVisible(false);
                button.setVisible(false);
            } else {
                playerIdButton.put(i, button);
            }
            votingPlayersPane.getChildren().add(button);
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

    private Boolean checkIfSkipVoting(int playerNumber, int totalPlayers) {
        return playerNumber != 0 && playerNumber != totalPlayers + 1
               && gameStatisticsService
                       .getGameStatisticsByGameIdSortedByInGameNumber(SelectionController.currentGameId)
                       .get(playerNumber - 1).isSkipNextVoting();
    }

    private Boolean checkIfMarkedByKradiy(int playerNumber, int totalPlayers) {
        return playerNumber != 0 && playerNumber != totalPlayers + 1
               && gameStatisticsService
                       .getGameStatisticsByGameIdSortedByInGameNumber(SelectionController.currentGameId)
                       .get(playerNumber - 1).isWasMarkedByKradiy();
    }

    private void beginVoting(int beginFromIndex) {
        reverse = false;
        gamersOrder = getOrderOfInGameNumbersNaturalOrder(beginFromIndex);
        giveVoiceForward(gamersOrder.peek() - 1); // peek() повертає ігровий номер, віднімаємо 1 щоб отримати і
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

    private void endEachPlayerPresentation() {
        // TODO: поміняти не нормальні змінні, а не в коді
        votingStateLabel.setText("Обговорення");
        secondsTillEnd = SettingsUtil.getSecondsPerDiscussion();
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

    private void beginVotingReverse(int beginFromIndex) {
        reverse = true;
        gamersOrder = getOrderOfInGameNumbersReverseOrder(beginFromIndex);
        giveVoiceReverse(gamersOrder.peek() - 1);
    }

    private void giveVoiceForward(Integer currentVoterIndex) {
        startLabel.setText("");
        if (!checkIfAlive(currentVoterIndex + 1, gameStatisticsList.size()) ||
            checkIfSkipVoting(currentVoterIndex + 1, gameStatisticsList.size()) ||
            checkIfMarkedByKradiy(currentVoterIndex + 1, gameStatisticsList.size())
        ) {
            // якщо гравець не живий
            gamersOrder.remove();// видаляємо його
            //take next. If next is null and game
            Integer nextPlayerNumber = gamersOrder.peek();
            if (nextPlayerNumber == null) {
                blockAllButtons();
                defineVotingResult();
            } else {
                giveVoiceForward(nextPlayerNumber - 1); // і дістаємо номер наступного, робимо - 1
            }
        } else {
            for (Map.Entry<Integer, Button> entry : playerIdButton.entrySet()) {
                Button button = entry.getValue();
                Integer finalCurrentVoterIndex1 = currentVoterIndex;
                button.setOnAction(actionEvent -> {
                    if (countDownTimeLine != null) {
                        countDownTimeLine.stop();
                    }
                    setVote(Integer.parseInt(button.getText()), finalCurrentVoterIndex1);
                });
                button.setDisable(true);
            }

            secondsTillEnd = SettingsUtil.getSecondsPerMove();
            Integer finalCurrentVoterIndex = currentVoterIndex;

            countDownTimeLine = new Timeline(new KeyFrame(Duration.seconds(1), (ActionEvent event) -> {
                if (secondsTillEnd > 0) {
                    secondsLeft.setText(String.valueOf(secondsTillEnd--));
                } else {
                    Platform.runLater(() -> {
                        int setVoteTo = 0;
                        if (finalCurrentVoterIndex == findLastAliveIndex()) {
                            for (int i = gameStatisticsList.size() - 1; i >= 0; i--) {
                                if (checkIfAlive(i + 1, gameStatisticsList.size())) {
                                    setVoteTo = i + 1;
                                }
                            }
                        } else {
                            for (int i = finalCurrentVoterIndex + 1; i < gameStatisticsList.size(); i++) {
                                if (checkIfAlive(i + 1, gameStatisticsList.size())) {
                                    setVoteTo = i + 1;
                                    break; // Exit loop as soon as a valid player is found
                                }
                            }
                        }
                        setVote(setVoteTo, finalCurrentVoterIndex);
                    });
                    countDownTimeLine.stop();
                }
            }));

            countDownTimeLine.setCycleCount(Timeline.INDEFINITE);
            countDownTimeLine.play();
            unblockAllButtons();
            updateButtonStates(currentVoterIndex);
            Button button = playerIdButton.get(currentVoterIndex + 1);
            button.setStyle("-fx-background-color: #00f100");
            //            button.setDisable(true);
            return;  // Exit the loop and method after starting the countdown
        }
    }

    private void resetTimerAndSetVotingLabel() {
        if (countDownTimeLine != null) {
            countDownTimeLine.stop();
        }
        votingStateLabel.setText("Голосування");
    }

    private void giveVoiceReverse(Integer reverseCurrentVoterIndex) {
        startLabel.setText("");
        if (!checkIfAlive(reverseCurrentVoterIndex + 1, gameStatisticsList.size()) ||
            checkIfSkipVoting(currentVoterIndex + 1, gameStatisticsList.size()) ||
            checkIfMarkedByKradiy(currentVoterIndex + 1, gameStatisticsList.size()
            )) {
            gamersOrder.remove();// видаляємо його
            //take next. If next is null and game
            Integer nextPlayerNumber = gamersOrder.peek();
            if (nextPlayerNumber == null) {
                blockAllButtons();
                defineVotingResult();
            } else {
                giveVoiceForward(nextPlayerNumber - 1); // і дістаємо номер наступного, робимо - 1
            }
        } else {
            for (Map.Entry<Integer, Button> entry : playerIdButton.entrySet()) {
                Button button = entry.getValue();
                Integer finalReverseCurrentVoterIndex1 = reverseCurrentVoterIndex;
                button.setOnAction(actionEvent -> {
                    if (countDownTimeLine != null) {
                        countDownTimeLine.stop();
                    }
                    setVote(Integer.parseInt(button.getText()), finalReverseCurrentVoterIndex1);
                });
                button.setDisable(true);
            }
            secondsTillEnd = 10;
            Integer finalReverseCurrentVoterIndex = reverseCurrentVoterIndex;
            countDownTimeLine = new Timeline(new KeyFrame(Duration.seconds(1), (ActionEvent event) -> {
                if (secondsTillEnd > 0) {
                    secondsLeft.setText(String.valueOf(secondsTillEnd--));
                } else {
                    Platform.runLater(() -> {
                        int setVoteTo = 0;
                        if (finalReverseCurrentVoterIndex == findFirstAliveIndex()) {
                            for (int i = 0; i < gameStatisticsList.size(); i++) {
                                if (checkIfAlive(i + 1, gameStatisticsList.size())) {
                                    setVoteTo = i + 1;
                                }
                            }
                        } else {
                            for (int i = 0; i < finalReverseCurrentVoterIndex; i++) {
                                if (checkIfAlive(i + 1, gameStatisticsList.size())) {
                                    setVoteTo = i + 1;
                                }
                            }
                        }
                        setVote(setVoteTo, finalReverseCurrentVoterIndex);
                    });
                    countDownTimeLine.stop();
                }
            }));
            countDownTimeLine.setCycleCount(Timeline.INDEFINITE);
            countDownTimeLine.play();
            unblockAllButtons();
            updateButtonStates(reverseCurrentVoterIndex);
            Button button = playerIdButton.get(reverseCurrentVoterIndex + 1);
            button.setStyle("-fx-background-color: #00f100");

            return;  // Exit the loop and method after starting the count down
        }
    }

    private void updateButtonStates(int reverseCurrentVoterIndex) {
        for (Map.Entry<Integer, Button> entry : playerIdButton.entrySet()) {
            int playerId = entry.getKey();
            Button anotherPlayerButton = entry.getValue();
            if (playerId != reverseCurrentVoterIndex + 1) {
                if (!checkIfAlive(playerId, gameStatisticsList.size())) {
                    anotherPlayerButton.setDisable(true);
                } else {
                    anotherPlayerButton.setDisable(false);
                    anotherPlayerButton.setStyle(IDLE_BUTTON_STYLE);
                }
            } else {
                anotherPlayerButton.setDisable(true);
                anotherPlayerButton.setStyle("-fx-background-color: #00f100");
            }
        }
    }

    private void updateVotesDisplay() {
        votesDisplay.getChildren().clear(); // Clear existing labels
        for (Map.Entry<Integer, Integer> entry : playerIdVotesMap.entrySet()) {
            int playerId = entry.getKey();
            int votes = entry.getValue();
            Label voteLabel = new Label();
            if (votes == 1) {
                voteLabel.setText("Гравець " + playerId + ": " + votes + " голос");
            } else {
                voteLabel.setText("Гравець " + playerId + ": " + votes + " голосів");
            }
            voteLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #ffffff");
            votesDisplay.getChildren().add(voteLabel);
        }
    }

    private void setVote(int playerNumber, Integer voterIndex) {
        Integer playerVotes = playerIdVotesMap.get(playerNumber);

        GameStatistics gameStatisticsVoter = gameStatisticsService
                .getGameStatisticsByGameIdSortedByInGameNumber(SelectionController.currentGameId).get(voterIndex);
        boolean isKradiy = kradiyHasStolenVoice && gameStatisticsVoter.getRole().getRoleNameConstant().equals(ERoleOrder.KRADIY.name());

        int votesToAdd = isKradiy ? 2 : 1;

        if (playerVotes == null) {
//            playerIdVotesMap.put(playerNumber, votesToAdd);
            doVote(playerNumber, 0, votesToAdd, voterIndex);
            addPointsAndChangeVoterIndex(playerNumber, voterIndex);
        } else if (playerVotes == 2) {
            //Отримати гравця в якого голосують, щоб взяти його excusesAttempts
            GameStatistics gameStatistics = gameStatisticsService
                    .getGameStatisticsByGameIdSortedByInGameNumber(SelectionController.currentGameId).get(playerNumber - 1);
            if (gameStatistics.getExcusesAttempts() < SettingsConstantsController.AVAILABLE_ATTEMPTS_TO_EXCUSE) {
                excuseTimeLine = null;
                countDownTimeLine = null;
                ButtonType foo = new ButtonType("Так", ButtonBar.ButtonData.YES);
                ButtonType bar = new ButtonType("Ні", ButtonBar.ButtonData.CANCEL_CLOSE);
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Скористатись правом на оправдання?", foo, bar);
                alert.setTitle("Оправдання");
                alert.initOwner(stage);
                alert.setHeaderText("Гравець має право на оправдання");
                alert.setResizable(false);
                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get().getText().equals("Так")) {
                    gameStatistics.setExcusesAttempts((short) (gameStatistics.getExcusesAttempts() + 1));
                    gameStatisticsService.save(gameStatistics);
                    startExcuseTimer(playerNumber, voterIndex, gameStatistics, votesToAdd);
                } else {
                    doVote(playerNumber, playerVotes, votesToAdd, voterIndex);
                    addPointsAndChangeVoterIndex(playerNumber, voterIndex);
                }
            } else {
                doVote(playerNumber, playerVotes, votesToAdd, voterIndex);
                addPointsAndChangeVoterIndex(playerNumber, voterIndex);
            }
        } else {
            doVote(playerNumber, playerVotes, votesToAdd, voterIndex);
            addPointsAndChangeVoterIndex(playerNumber, voterIndex);
        }
    }

    private void doVote(int playerNumber, int playerVotes, int votesToAdd, int lastVoterIndex) {
        playerIdVotesMap.put(playerNumber, playerVotes + votesToAdd);
        lastVoiceAmount = votesToAdd;
        lastVotedNumber = playerNumber;
        lastVoterNumber = lastVoterIndex + 1;
    }

    private void startExcuseTimer(int playerNumber, int voterIndex, GameStatistics gameStatistics, int votesToAdd) {
        secondsTillEnd = 10;
        Timeline excuseTimeLineTextChanger = new Timeline(new KeyFrame(Duration.seconds(1), (ActionEvent event) -> {
            secondsLeft.setText(String.valueOf(secondsTillEnd--));
        }));
        // Set number of cycles (remaining duration in seconds):
        excuseTimeLineTextChanger.setCycleCount((int) secondsTillEnd);
        excuseTimeLineTextChanger.play();

        excuseTimeLine = new Timeline(new KeyFrame(Duration.seconds(secondsTillEnd), ae -> {
            Platform.runLater(() -> {
                excuseTimeLine.stop(); // Stop the timer when it ends
                showExcuseChoiceDialog(playerNumber, voterIndex, gameStatistics, votesToAdd);
                secondsLeft.setText(String.valueOf(secondsTillEnd--));
            });
        }));
        excuseTimeLine.setCycleCount(1);
        excuseTimeLine.play();
    }

    private void showExcuseChoiceDialog(int playerNumber, int voterIndex, GameStatistics gameStatistics, int votesToAdd) {
        Alert excuseAlert = new Alert(Alert.AlertType.CONFIRMATION);
        excuseAlert.setTitle("Оправдання");
        excuseAlert.setHeaderText("Час на оправдання завершився");
        excuseAlert.setContentText("Виберіть опцію:");
        excuseAlert.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        excuseAlert.initOwner(stage);

        Optional<ButtonType> excuseResult = excuseAlert.showAndWait();
        if (excuseResult.isPresent() && excuseResult.get() == ButtonType.OK) {
            // Proceed with voting logic here if the player chooses to vote
            playerIdVotesMap.put(playerNumber, playerIdVotesMap.get(playerNumber) + votesToAdd);
            addPointsAndChangeVoterIndex(playerNumber, voterIndex);
        } else {
            // Player decides not to vote, handle accordingly
            if (reverse) {
                giveVoiceReverse(gamersOrder.peek() - 1);
            } else {
                giveVoiceForward(gamersOrder.peek() - 1);
            }
            // Do not change the voter index to allow the player to vote again
        }
    }

    private void addPointsAndChangeVoterIndex(int playerNumber, int voterIndex) {

        //оновити вікно із результатом
        updateVotesDisplay();

        // Якщо мирний голосує в мирного, то не отримує очок, якщо в мафію, то йому дають + 3
        // мафія вкидає в мирного отримує +2
        pointsService.countPointsInOrderToDayAction(
                SelectionController.currentGameId,
                playerNumber,
                voterIndex + 1
        );
        //перевірити на кінець голосування
        if (checkTheEndOfVoting(voterIndex)) {
            blockAllButtons();
            defineVotingResult();
        } else {
            gamersOrder.remove();//видаляємо з черги того хто голосував
            Integer nextPlayerNumber = gamersOrder.peek();
            if (nextPlayerNumber == null) {
                blockAllButtons();
                defineVotingResult();
            } else if (reverse) {
                giveVoiceReverse(nextPlayerNumber - 1);
            } else {
                giveVoiceForward(nextPlayerNumber - 1); // передаємо індекс першого в черзі
            }
        }
    }

    /**
     * Повертає gameStatistic, що вибув під час голосування
     */
    private void defineVotingResult() {
        List<Integer> playersIdWithMaxVotes = findPlayersWithMaxVotesAmount(playerIdVotesMap);
        Integer playerInGameNumberToDelete;
        if (playersIdWithMaxVotes.size() > 1) {
            showRouletteWindow(playersIdWithMaxVotes, eliminatedPlayer -> {
                // Handle the elimination of the player here
                gameStatisticsService.deletePlayerAfterVoting(SelectionController.currentGameId, eliminatedPlayer);
                //тут можливо ще зробити сервіс, який буде перевіряти чи гру закінчено, і дьоргати його методи
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Кінець голосування");
                alert.initOwner(stage);
                alert.show();
                pointsService.countOnePointAfterDayAndNight(SelectionController.currentGameId);
                fxWeaver.loadController(NightStageController.class).show();
            });
        } else {
            playerInGameNumberToDelete = playersIdWithMaxVotes.get(0);
            gameStatisticsService.deletePlayerAfterVoting(SelectionController.currentGameId, playerInGameNumberToDelete);
            pointsService.countOnePointAfterDayAndNight(SelectionController.currentGameId);
            //тут можливо ще зробити сервіс, який буде перевіряти чи гру закінчено, і дьоргати його методи
            if (gameService.checkIfGameIsOver(SelectionController.currentGameId)) {
                fxWeaver.loadController(GameEndingController.class);
            } else {
                fxWeaver.loadController(NightStageController.class).show();
            }
        }
    }

    private void showRouletteWindow(List<Integer> playersIdWithMaxVotes, Consumer<Integer> onWindowClosed) {
        Stage window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        window.setTitle("Roulette");
        GridPane layout = new GridPane();

        final Integer[] playerToDeleteInGameNumber = new Integer[1];
        int numberOfPlayers = playersIdWithMaxVotes.size();
        int totalButtons = numberOfPlayers * 3;

        Random random = new Random();
        int deathButtonIndex = random.nextInt(totalButtons);
        int[] currentPlayerIndex = {0};

        for (int i = 0; i < totalButtons; i++) {
            Button button = new Button(String.valueOf(i + 1));
            int buttonIndex = i;
            button.setOnAction(event -> {
                Integer currentPlayer = playersIdWithMaxVotes.get(currentPlayerIndex[0]);
                if (buttonIndex == deathButtonIndex) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.initOwner(stage);
                    alert.setContentText("Player " + currentPlayer + " clicked the death button! Player is out.");
                    alert.setOnHidden(e -> {
                        playerToDeleteInGameNumber[0] = currentPlayer;
                        window.close();
                    });
                    alert.show();
                } else {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.initOwner(stage);
                    alert.setContentText("Player " + currentPlayer + " is safe! Next player's turn.");
                    alert.setOnHidden(e -> {
                        currentPlayerIndex[0] = (currentPlayerIndex[0] + 1) % numberOfPlayers;
                    });
                    alert.show();
                }
                button.setDisable(true);
            });
            layout.add(button, i % 3, i / 3);
        }

        Scene scene = new Scene(layout, 300, 200);
        countDownTimeLine.stop();
        window.setScene(scene);
        window.show();

        // Add a listener to handle the window closing event
        window.setOnHidden(e -> onWindowClosed.accept(playerToDeleteInGameNumber[0]));
    }

    private List<Integer> findPlayersWithMaxVotesAmount(Map<Integer, Integer> map) {
        if (map == null || map.isEmpty()) {
            return List.of();
        }
        // Find the maximum value in the map
        int maxValue = map.values().stream()
                .max(Integer::compareTo)
                .orElseThrow();

        // Find all keys with the maximum value
        return map.entrySet().stream()
                .filter(entry -> entry.getValue() == maxValue)
                .map(Map.Entry::getKey)
                .toList();
    }

    private void blockAllButtons() {
        playerIdButton.forEach((id, btn) -> btn.setDisable(true));
    }

    private void unblockAllButtons() {
        playerIdButton.forEach((id, btn) -> btn.setDisable(false));
    }

    private boolean checkTheEndOfVoting(Integer voterIndex) {
        return gamersOrder.isEmpty();
    }

    private Integer findLastAliveIndex() {
        Integer lastAliveIndex = 0;
        for (int i = 0; i < gameStatisticsList.size(); i++) {
            if (gameStatisticsList.get(i).isInGame()) {
                lastAliveIndex = i;
            }
        }
        return lastAliveIndex;
    }

    private Integer findFirstAliveIndex() {
        Integer firstAliveIndex = 0;
        for (int i = 0; i < gameStatisticsList.size(); i++) {
            if (gameStatisticsList.get(i).isInGame()) {
                firstAliveIndex = i;
                break;
            }
        }
        return firstAliveIndex;
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
        button.setCursor(Cursor.OPEN_HAND);
//        button.setOnMouseEntered(e -> button.setStyle(HOVERED_BUTTON_STYLE));
//        button.setOnMouseExited(e -> button.setStyle(IDLE_BUTTON_STYLE));
        return button;
    }

    public void show() {
        stage.show();
    }
}
