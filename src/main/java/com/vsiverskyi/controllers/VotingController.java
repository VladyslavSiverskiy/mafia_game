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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
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
    private Label dayLbl;
    @FXML
    private Button resetVote;
    @FXML
    private Button discussionButton;
    @FXML
    private Button goAheadButton;
    private Timeline countDownTimeLine;
    private Map<Integer, Integer> playerIdVotesMap;
    private Map<Integer, GameStatistics> playerInGameNumberGameStatistics;
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
        stage.getIcons().add(new Image("/images/title.jpg"));
        stage.setTitle("STOP КОРУПЦІЯ");
        playerIdVotesMap = new HashMap<>();
        playerIdButton = new HashMap<>();
        playerInGameNumberGameStatistics = new HashMap<>();
        kradiyHasStolenVoice = false;

        int secondsPerMove = SettingsUtil.getSecondsPerMove();

        gameStatisticsList = gameStatisticsService.getGameStatisticsByGameIdSortedByInGameNumber(SelectionController.currentGameId);
        initPlayerInGameNumberGameStatistics();
        reverseCurrentVoterIndex = gameStatisticsList.size() - 1;
        currentVoterIndex = 0;
// Initialize player card list view
        // Initialize technical defeat buttons
        technicalDefeatPeaceful.setOnMouseClicked(e -> penaltyController.assignTechnicalDefeat("PEACE"));
        technicalDefeatMafia.setOnMouseClicked(e -> penaltyController.assignTechnicalDefeat("MAFIA"));

        penaltyController.initializePlayerCardList(gameStatisticsList, stage, this, playerCardListView);

        displayRolePlayers(gameStatisticsList.size());

        fullScreen.setOnMouseClicked(ev -> stage.setFullScreen(true));
        ImageView imageView = new ImageView(getClass().getResource("/images/fullscreen.png").toExternalForm());
        fullScreen.setGraphic(imageView);
        imageView.fitWidthProperty().bind(fullScreen.widthProperty().divide(10));
        imageView.setPreserveRatio(true);

        startButton.setOnMouseClicked(actionEvent -> startVoting());
        resetVote.setOnMouseClicked(actionEvent -> resetVote());
        resetVote.setDisable(true);

        discussionButton.setOnMouseClicked(actionEvent -> endEachPlayerPresentation());
        discussionButton.setStyle(IDLE_BUTTON_STYLE);
        discussionButton.setOnMouseEntered(ev -> discussionButton.setStyle(HOVERED_BUTTON_STYLE));
        discussionButton.setOnMouseExited(ev -> discussionButton.setStyle(IDLE_BUTTON_STYLE));

        resetVote.setStyle(IDLE_BUTTON_STYLE);
        resetVote.setOnMouseEntered(ev -> resetVote.setStyle(HOVERED_BUTTON_STYLE));
        resetVote.setOnMouseExited(ev -> resetVote.setStyle(IDLE_BUTTON_STYLE));

        startButton.setStyle(IDLE_BUTTON_STYLE);
        startButton.setOnMouseEntered(ev -> startButton.setStyle(HOVERED_BUTTON_STYLE));
        startButton.setOnMouseExited(ev -> startButton.setStyle(IDLE_BUTTON_STYLE));
        startButton.toFront();

    }

    private void initPlayerInGameNumberGameStatistics() {
        for (GameStatistics gameStatistics : gameStatisticsList) {
            playerInGameNumberGameStatistics.put(gameStatistics.getInGameNumber(), gameStatistics);
        }
    }

    private void resetVote() {
        playerIdVotesMap.put(lastVotedNumber, playerIdVotesMap.get(lastVotedNumber) - lastVoiceAmount);
        gamersOrder.addFirst(lastVoterNumber);

        if (excuseTimeLine != null) {
            excuseTimeLine.stop();
            excuseTimeLine = null;
        }
        if (countDownTimeLine != null) {
            countDownTimeLine.stop();
            countDownTimeLine = null;
        }

        updateVotesDisplay();
        if (reverse) {
            giveVoiceReverse(lastVoterNumber - 1);
        } else {
            giveVoiceForward(lastVoterNumber - 1);
        }
        resetVote.setDisable(true);
    }

    private void startVoting() {
        discussionButton.setDisable(true);
        resetTimerAndSetVotingLabel();
        startLabel.setText("Оберіть гравця, з якого розпочнемо");

        for (Map.Entry<Integer, Button> entry : playerIdButton.entrySet()) {
            Button button = entry.getValue();
            button.setOnMouseClicked(actionEvent -> {
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
        votingPlayersPane.getChildren().add(startButton);
        votingPlayersPane.getChildren().add(discussionButton);
        votingPlayersPane.getChildren().add(goAheadButton);
        votingPlayersPane.getChildren().add(secondsLeft);
        votingPlayersPane.getChildren().add(dayLbl);
        votingPlayersPane.getChildren().add(startLabel);
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
                    avatarImage = new Image("images/icon.ico");
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
            roleLabel.setStyle("-fx-text-fill: #ffffff; -fx-border-radius: 5px; -fx-font-size: 12px;");
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
                Image deathImage = new Image(getClass().getResource("/images/pause.png").toExternalForm());
                ImageView deathImageView = new ImageView(deathImage);
                deathImageView.setFitWidth(15); // Adjust width as needed
                deathImageView.setFitHeight(15); // Adjust height as needed
                playerPanel.getChildren().add(deathImageView);
            }

            if (checkIfMarkedByKradiy(i, totalPlayers)) {
                Image deathImage = new Image(getClass().getResource("/images/kradiy.png").toExternalForm());
                ImageView deathImageView = new ImageView(deathImage);
                deathImageView.setFitWidth(15); // Adjust width as needed
                deathImageView.setFitHeight(15); // Adjust height as needed
                playerPanel.getChildren().add(deathImageView);
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
        startButton.setDisable(true);
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
        startButton.setDisable(true);
        reverse = true;
        gamersOrder = getOrderOfInGameNumbersReverseOrder(beginFromIndex);
        giveVoiceReverse(gamersOrder.peek() - 1);
    }

    private void giveVoiceForward(Integer currentVoterIndex) {
        startLabel.setText("");
        startLabel.setText("Голосує: " + playerInGameNumberGameStatistics.get(currentVoterIndex + 1).getInGameNickname());
        if (!checkIfAlive(currentVoterIndex + 1, gameStatisticsList.size()) ||
            checkIfSkipVoting(currentVoterIndex + 1, gameStatisticsList.size()) ||
            checkIfMarkedByKradiy(currentVoterIndex + 1, gameStatisticsList.size())
        ) {
            // якщо гравець не живий
            gamersOrder.remove();// видаляємо його
            //take next. If next is null and game
            Integer nextPlayerNumber = gamersOrder.peek();
            if (nextPlayerNumber == null) {
                lastVoiceReverse();
            } else {
                giveVoiceForward(nextPlayerNumber - 1); // і дістаємо номер наступного, робимо - 1
            }
        } else {
            goAheadButton.setDisable(false);

            Scene scene = goAheadButton.getScene();
            scene.setOnKeyTyped(event -> {

                if (event.getCharacter().equals(" ")) {
                    // Check if the button is not disabled to avoid re-triggering
                    if (!goAheadButton.isDisabled()) {
                        // Disable the "Start Timer" button
                        goAheadButton.setDisable(true);
                        startTimerForPlayer(currentVoterIndex); // Start the timer for the player
                    }
                }
            });

            goAheadButton.setOnMouseClicked(actionEvent -> {
                // Disable the "Start Timer" button
                goAheadButton.setDisable(true);
                startTimerForPlayer(currentVoterIndex); // Start the timer for the player
            });


            updateButtonStates(currentVoterIndex);
            blockAllButtons();
            Button button = playerIdButton.get(currentVoterIndex + 1);
            button.setStyle("-fx-background-color: #00f100");
        }
    }

    private void startTimerForPlayer(Integer currentVoterIndex) {
        for (Map.Entry<Integer, Button> entry : playerIdButton.entrySet()) {
            Button button = entry.getValue();
            Integer finalCurrentVoterIndex = currentVoterIndex;
            button.setOnMouseClicked(actionEvent -> {
                if (countDownTimeLine != null) {
                    countDownTimeLine.stop();
                }
                setVote(Integer.parseInt(button.getText()), finalCurrentVoterIndex);
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
        blockAllButtons();
        updateButtonStates(currentVoterIndex);
    }


    private void resetTimerAndSetVotingLabel() {
        if (countDownTimeLine != null) {
            countDownTimeLine.stop();
        }
        votingStateLabel.setText("Голосування");
    }

    private void giveVoiceReverse(Integer reverseCurrentVoterIndex) {
        startLabel.setText("");
        startLabel.setText("Голосує: " + playerInGameNumberGameStatistics.get(reverseCurrentVoterIndex + 1).getInGameNickname());
        if (!checkIfAlive(reverseCurrentVoterIndex + 1, gameStatisticsList.size()) ||
            checkIfSkipVoting(currentVoterIndex + 1, gameStatisticsList.size()) ||
            checkIfMarkedByKradiy(currentVoterIndex + 1, gameStatisticsList.size()
            )) {
            gamersOrder.remove();// видаляємо його
            //take next. If next is null and game
            Integer nextPlayerNumber = gamersOrder.peek();
            if (nextPlayerNumber == null) {
                lastVoiceReverse();
            } else {
                giveVoiceReverse(nextPlayerNumber - 1); // і дістаємо номер наступного, робимо - 1
            }
        } else {
            goAheadButton.setDisable(false);
            Scene scene = goAheadButton.getScene();
            scene.setOnKeyTyped(event -> {

                if (event.getCharacter().equals(" ")) {
                    // Check if the button is not disabled to avoid re-triggering
                    if (!goAheadButton.isDisabled()) {
                        // Disable the "Start Timer" button
                        goAheadButton.setDisable(true);
                        startTimerForPlayer(reverseCurrentVoterIndex); // Start the timer for the player
                    }
                }
            });

            goAheadButton.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.SPACE) {
                    // Check if the button is not disabled to avoid re-triggering
                    if (!goAheadButton.isDisabled()) {
                        // Disable the "Start Timer" button
                        goAheadButton.setDisable(true);
                        startTimerForPlayer(reverseCurrentVoterIndex); // Start the timer for the player
                    }
                }
            });
            goAheadButton.setOnMouseClicked(actionEvent -> {
                // Disable the "Start Timer" button
                goAheadButton.setDisable(true);
                unblockAllButtons();
                startTimerForReversePlayer(reverseCurrentVoterIndex); // Start the timer for the player
            });



            updateButtonStates(reverseCurrentVoterIndex);
            blockAllButtons();
            Button button = playerIdButton.get(reverseCurrentVoterIndex + 1);
            button.setStyle("-fx-background-color: #00f100");
        }
    }

    private void startTimerForReversePlayer(Integer reverseCurrentVoterIndex) {
        for (Map.Entry<Integer, Button> entry : playerIdButton.entrySet()) {
            Button button = entry.getValue();
            Integer finalReverseCurrentVoterIndex = reverseCurrentVoterIndex;
            button.setOnMouseClicked(actionEvent -> {
                if (countDownTimeLine != null) {
                    countDownTimeLine.stop();
                }
                setVote(Integer.parseInt(button.getText()), finalReverseCurrentVoterIndex);
            });
            button.setDisable(true);

        }

        secondsTillEnd = SettingsUtil.getSecondsPerMove(); // Set your countdown duration here
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
        // Convert the map to a list and sort it based on votes
        List<Map.Entry<Integer, Integer>> sortedVotesList = playerIdVotesMap.entrySet()
                .stream()
                .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
                .collect(Collectors.toList());

        // Iterate through the sorted list and update the display
        for (Map.Entry<Integer, Integer> entry : sortedVotesList) {
            int playerId = entry.getKey();
            int votes = entry.getValue();
            Label voteLabel = new Label();
            String nickname = playerInGameNumberGameStatistics.get(playerId).getInGameNickname();
            if (nickname == null) {
                nickname = "НЕЗНАЙОМЕЦЬ";
            }
            if (votes == 1) {
                voteLabel.setText(nickname + ": " + votes + " голос");
            } else if (votes > 1) {
                voteLabel.setText(nickname + ": " + votes + " голосів");
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
                blockAllButtons();
                ButtonType foo = new ButtonType("Так", ButtonBar.ButtonData.YES);
                ButtonType bar = new ButtonType("Ні", ButtonBar.ButtonData.CANCEL_CLOSE);
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Скористатись правом на оправдання?", foo, bar);
                alert.setTitle("Оправдання");
                alert.initOwner(stage);
                alert.setHeaderText(gameStatistics.getInGameNickname() + " має право на оправдання");
                alert.setResizable(false);
                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get().getText().equals("Так")) {
                    votingStateLabel.setText("Оправдання");
                    gameStatistics.setExcusesAttempts((short) (gameStatistics.getExcusesAttempts() + 1));
                    gameStatisticsService.save(gameStatistics);
                    startExcuseTimer(playerNumber, voterIndex, gameStatistics, votesToAdd);
                } else {
                    votingStateLabel.setText("Голосування");
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
        resetVote.setDisable(false);
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
        ButtonType foo = new ButtonType("Зберегти голос", ButtonBar.ButtonData.YES);
        ButtonType bar = new ButtonType("Інший гравець", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert excuseAlert = new Alert(Alert.AlertType.CONFIRMATION, "Зберегти голос?", foo, bar);
        excuseAlert.setTitle("Оправдання");
        excuseAlert.setHeaderText("Час на оправдання завершився");
        excuseAlert.setContentText("Виберіть опцію:");
        excuseAlert.initOwner(stage);
        Optional<ButtonType> excuseResult = excuseAlert.showAndWait();
        if (excuseResult.isPresent() && excuseResult.get().getText().equals("Зберегти голос")) {
            playerIdVotesMap.put(playerNumber, playerIdVotesMap.get(playerNumber) + votesToAdd);
            addPointsAndChangeVoterIndex(playerNumber, voterIndex);
        } else {
            if (reverse) {
                giveVoiceReverse(gamersOrder.peek() - 1);
            } else {
                giveVoiceForward(gamersOrder.peek() - 1);
            }
        }
        votingStateLabel.setText("Голосування");
    }

    private void lastVoiceReverse() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(stage);
        alert.setTitle("Кінець голосування");
        alert.setHeaderText(null);
        alert.setContentText("Це ОСТАННІЙ голос перед етапом НОЧІ, бажаєте його залишити?");

        ButtonType buttonYes = new ButtonType("Так");
        ButtonType buttonNo = new ButtonType("Ні");

        alert.getButtonTypes().setAll(buttonYes, buttonNo);

        alert.showAndWait().ifPresent(response -> {
            if (response == buttonYes) {
                if (countDownTimeLine != null) {
                    countDownTimeLine.stop();
                }
                blockAllButtons();
                defineVotingResult();
            } else if (response == buttonNo) {
                // Let the player vote again
                resetVote();
            }
        });
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
            lastVoiceReverse();

        } else {
            gamersOrder.remove();//видаляємо з черги того хто голосував
            Integer nextPlayerNumber = gamersOrder.peek();
            if (nextPlayerNumber == null) {
                lastVoiceReverse();
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
                GameStatistics gameStatistics = gameStatisticsService.deletePlayerAfterVoting(SelectionController.currentGameId, eliminatedPlayer);
                showResultWindow(gameStatistics);
                pointsService.countOnePointAfterDayAndNight(SelectionController.currentGameId);
                fxWeaver.loadController(NightStageController.class).show();
            });
        } else {
            playerInGameNumberToDelete = playersIdWithMaxVotes.get(0);
            GameStatistics gameStatistics =  gameStatisticsService.deletePlayerAfterVoting(SelectionController.currentGameId, playerInGameNumberToDelete);
            pointsService.countOnePointAfterDayAndNight(SelectionController.currentGameId);
            //тут можливо ще зробити сервіс, який буде перевіряти чи гру закінчено, і дьоргати його методи
            showResultWindow(gameStatistics);
            if (gameService.checkIfGameIsOver(SelectionController.currentGameId)) {
                fxWeaver.loadController(GameEndingController.class);
            } else {
                fxWeaver.loadController(NightStageController.class).show();
            }
        }
    }

    private void showResultWindow(GameStatistics gameStatistics) {

        Image image = new Image(getClass().getResource("/images/stop.png").toExternalForm());
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(50);  // Set the desired width
        imageView.setFitHeight(50); // Set the desired height

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setGraphic(imageView);
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(
                getClass().getResource("/style/myDialogs.css").toExternalForm());
        dialogPane.getStyleClass().add("myDialog");
        alert.setTitle("Підсумки дня");
        alert.setHeaderText("Результат голосування");
        String res = gameStatistics.isDefendedPerNextVoting() ? " не вибуває, бо отримав захист на день."
                : " вибуває. Його роль " + gameStatistics.getRole().getTitle();
        alert.setContentText(gameStatistics.getInGameNickname() + res);

        alert.initOwner(stage);
        alert.showAndWait();
    }

    private void showRouletteWindow(List<Integer> playersIdWithMaxVotes, Consumer<Integer> onWindowClosed) {
        Stage window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        window.setTitle("Перестрілка");
        GridPane layout = new GridPane();
        layout.setHgap(10);
        layout.setVgap(10);
        layout.setPadding(new Insets(10));
        layout.setStyle("-fx-background-color: #161616;");

        final Integer[] playerToDeleteInGameNumber = new Integer[1];
        int numberOfPlayers = playersIdWithMaxVotes.size();
        int totalButtons = numberOfPlayers * 3;

        Random random = new Random();
        int deathButtonIndex = random.nextInt(totalButtons);
        int[] currentPlayerIndex = {0};

        Label currentPlayerLabel = new Label();
        currentPlayerLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
        layout.add(currentPlayerLabel, 0, 0, 3, 1);

        List<String> allPerRoulette = new ArrayList<>();
        for (Integer number : playersIdWithMaxVotes) {
            allPerRoulette.add(gameStatisticsService
                    .findByInGameNumberAndGameId(number, SelectionController.currentGameId)
                    .getInGameNickname());
        }

        GameStatistics gameStatistics = gameStatisticsService
                .findByInGameNumberAndGameId(playersIdWithMaxVotes.get(currentPlayerIndex[0]), SelectionController.currentGameId);
        currentPlayerLabel.setText("Стріляє: " + gameStatistics.getInGameNickname());

        // Create and populate ListView with nicknames
        ObservableList<String> nicknames = FXCollections.observableArrayList(allPerRoulette);
        ListView<String> listView = new ListView<>(nicknames);
        listView.setPrefSize(200, 150);
        listView.setStyle("-fx-font-size: 14px; -fx-text-fill: #ffffff; -fx-background-color: #161616;");

        // Add ListView to layout
        layout.add(listView, 3, 0, 1, totalButtons / 3 + 2);

        for (int i = 0; i < totalButtons; i++) {
            Button button = new Button(String.valueOf(i + 1));
            button.setStyle("-fx-font-size: 12px; -fx-min-width: 40px; -fx-min-height: 30px; -fx-background-color: #9e9e9e; -fx-text-fill: #ffffff; -fx-border-radius: 5px;");
            int buttonIndex = i;
            button.setOnMouseClicked(event -> {
                Integer currentPlayer = playersIdWithMaxVotes.get(currentPlayerIndex[0]);
                GameStatistics gameStatistics2 = gameStatisticsService
                        .findByInGameNumberAndGameId(playersIdWithMaxVotes.get(currentPlayerIndex[0]), SelectionController.currentGameId);
                if (buttonIndex == deathButtonIndex) {
                    Image image = new Image(getClass().getResource("/images/death.png").toExternalForm());
                    ImageView imageView = new ImageView(image);
                    imageView.setFitWidth(50);  // Set the desired width
                    imageView.setFitHeight(50); // Set the desired height

                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setGraphic(imageView);
                    alert.initOwner(window);
                    alert.setTitle("СМЕРТЬ");
                    alert.setContentText("СМЕРТЬ");
                    alert.setContentText(gameStatistics2.getInGameNickname() + " обрав смертельну кнопку! Гравець вибуває.");
                    alert.setOnHidden(e -> {
                        playerToDeleteInGameNumber[0] = currentPlayer;
                        window.close();
                    });
                    alert.show();
                } else {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("БЕЗПЕКА");
                    alert.setContentText("БЕЗПЕКА");
                    alert.initOwner(window);

                    alert.setContentText(gameStatistics2.getInGameNickname() + " у безпеці! Хід наступного гравця.");
                    alert.setOnHidden(e -> {
                        currentPlayerIndex[0] = (currentPlayerIndex[0] + 1) % numberOfPlayers;
                        GameStatistics nextGameStatistics = gameStatisticsService
                                .findByInGameNumberAndGameId(playersIdWithMaxVotes.get(currentPlayerIndex[0]), SelectionController.currentGameId);
                        currentPlayerLabel.setText("Стріляє: " + nextGameStatistics.getInGameNickname());
                    });
                    alert.show();
                }
                button.setDisable(true);
                button.setAlignment(Pos.CENTER);
                button.setStyle("-fx-background-color: #f44336; -fx-alignment: center"); // Change color when button is disabled
            });
            layout.add(button, i % 3, (i / 3) + 1);
        }

        Scene scene = new Scene(layout, 500, 300); // Adjust size to accommodate ListView
        if (countDownTimeLine != null) {
            countDownTimeLine.stop();
        }
        window.setScene(scene);
        scene.getStylesheets().add(getClass().getResource("/style/style.css").toExternalForm()); // Add CSS stylesheet
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
        return button;
    }

    public void show() {
        stage.show();
    }
}
