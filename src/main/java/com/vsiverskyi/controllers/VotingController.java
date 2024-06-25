package com.vsiverskyi.controllers;

import com.vsiverskyi.model.GameStatistics;
import com.vsiverskyi.model.Player;
import com.vsiverskyi.model.Role;
import com.vsiverskyi.service.GameService;
import com.vsiverskyi.service.GameStatisticsService;
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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
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
    private GameService gameService;
    @Autowired
    private PenaltyController penaltyController;
    @Autowired
    private GameStatisticsService gameStatisticsService;
    @Autowired
    private FxWeaver fxWeaver;
    private Stage stage;
    private Scene scene;
    private Parent root;
    @FXML
    private AnchorPane votingAp;
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

    private Timeline countDownTimeLine;
    private Map<Integer, Integer> playerIdVotesMap = new HashMap<>();
    private Map<Integer, Button> playerIdButton = new HashMap<>();
    private List<GameStatistics> gameStatisticsList;
    private Integer currentVoterIndex;
    private Integer reverseCurrentVoterIndex;
    private boolean reverse;

    int secondsTillEnd = 10;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.stage = StarterController.primaryStage;
        scene = new Scene(votingAp);
        stage.setScene(scene);
        scene.getStylesheets().add(getClass().getResource("/style/style.css").toExternalForm());
        stage.setMaximized(true);
        gameStatisticsList = gameService.getGameInfo(SelectionController.currentGameId).getGameStatistics();
        reverseCurrentVoterIndex = gameStatisticsList.size() - 1;
        currentVoterIndex = 0;
// Initialize player card list view
        penaltyController.initializePlayerCardList(gameStatisticsList, stage,this, playerCardListView);

        displayRolePlayers(gameStatisticsList.size());

        fullScreen.setOnAction(ev -> stage.setFullScreen(true));
        beginVoting.setOnAction(actionEvent -> beginVoting());
        beginVotingReverse.setOnAction(actionEvent -> beginVotingReverse());
    }

    @Override
    public void displayRolePlayers(int totalPlayers) {
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
            votingPlayersPane.getChildren().add(playerPanel);

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


    private void beginVoting() {
        giveVoiceForward(currentVoterIndex);
    }

    private void beginVotingReverse() {
        reverse = true;
        giveVoiceReverse(reverseCurrentVoterIndex);
    }

    private void giveVoiceForward(Integer currentVoterIndex) {
        while (currentVoterIndex < gameStatisticsList.size()) {
            System.out.println("CURRENT INDEX BEGINNING " + currentVoterIndex);

            if (!checkIfAlive(currentVoterIndex + 1, gameStatisticsList.size())) {
                currentVoterIndex = currentVoterIndex + 1;
                System.out.println("CURRENT INDEX CHANGE " + currentVoterIndex);
            } else {
                for (Map.Entry<Integer, Button> entry : playerIdButton.entrySet()) {
                    Button button = entry.getValue();
                    System.out.println("Click " + currentVoterIndex);
                    Integer finalCurrentVoterIndex1 = currentVoterIndex;
                    button.setOnAction(actionEvent -> {
                        if (countDownTimeLine != null) {
                            countDownTimeLine.stop();
                        }
                        setVote(Integer.parseInt(button.getText()), finalCurrentVoterIndex1);
                    });
                    button.setDisable(true);
                }

                secondsTillEnd = 10;
                countDownTimeLine = new Timeline(new KeyFrame(Duration.seconds(1), (ActionEvent event) -> {
                    if (secondsTillEnd == 1) {
                        blockAllButtons();
                    }
                    secondsLeft.setText(String.valueOf(secondsTillEnd--));
                }));

                // Set number of cycles (remaining duration in seconds)
                countDownTimeLine.setCycleCount((int) secondsTillEnd);
                System.out.println("CURRENT INDEX " + currentVoterIndex);
                Integer finalCurrentVoterIndex = currentVoterIndex;
                countDownTimeLine.setOnFinished(event -> {
                    int setVoteTo = 0;
                    if (finalCurrentVoterIndex == findLastAliveIndex()) {
                        System.out.println("Final current voter index " + finalCurrentVoterIndex);
                        for (int i = gameStatisticsList.size() - 1; i >= 0 ; i--) {
                            if (checkIfAlive(i + 1, gameStatisticsList.size())) {
                                setVoteTo = i + 1;
                            }
                        }
                        System.out.println("Setting vote to " + setVoteTo);
                    } else {
                        System.out.println("FINAL INDEX " + finalCurrentVoterIndex);
                        for (int i = finalCurrentVoterIndex + 1; i < gameStatisticsList.size(); i++) {
                            if (checkIfAlive(i + 1, gameStatisticsList.size())) {
                                setVoteTo = i + 1;
                                break; // Exit loop as soon as a valid player is found
                            }
                        }
                        System.out.println("setVoteTo " + setVoteTo);
                    }
                    setVote(setVoteTo, finalCurrentVoterIndex);
                });

                countDownTimeLine.play();
                unblockAllButtons();
                Button button = playerIdButton.get(currentVoterIndex + 1);
                button.setDisable(true);
                updateButtonStates(currentVoterIndex);
                return;  // Exit the loop and method after starting the countdown
            }
        }
    }

    private void giveVoiceReverse(Integer reverseCurrentVoterIndex) {
        while (reverseCurrentVoterIndex >= 0) {
            System.out.println("REVERSE INDEX BEGINNING " + reverseCurrentVoterIndex);
            if (!checkIfAlive(reverseCurrentVoterIndex + 1, gameStatisticsList.size())) {
                reverseCurrentVoterIndex--;
                System.out.println("REVERSE INDEX CHANGE " + reverseCurrentVoterIndex);
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
                countDownTimeLine = new Timeline(new KeyFrame(Duration.seconds(1), (ActionEvent event) -> {
                    if (secondsTillEnd == 1) {
                        blockAllButtons();
                    }
                    secondsLeft.setText(String.valueOf(secondsTillEnd--));
                }));

                // Set number of cycles (remaining duration in seconds)
                countDownTimeLine.setCycleCount((int) secondsTillEnd);
                System.out.println("REVERSE CURRENT " + reverseCurrentVoterIndex);
                Integer finalReverseCurrentVoterIndex = reverseCurrentVoterIndex;
                countDownTimeLine.setOnFinished(event -> {
                    int setVoteTo = 0;
                    System.out.println("First alive");
                    if (finalReverseCurrentVoterIndex == findFirstAliveIndex()) {
                        System.out.println("Reverse current voter index " + finalReverseCurrentVoterIndex);
                        for (int i = 0; i < gameStatisticsList.size(); i++) {
                            if (checkIfAlive(i + 1, gameStatisticsList.size())) {
                                setVoteTo = i + 1;
                            }
                        }
                        System.out.println("Setting vote to " + setVoteTo);
                    } else {
                        System.out.println("FINAL REVERSE IN " + finalReverseCurrentVoterIndex);
                        for (int i = 0; i < finalReverseCurrentVoterIndex; i++) {
                            if (checkIfAlive(i + 1, gameStatisticsList.size())) {
                                setVoteTo = i + 1;
                            }
                        }
                        System.out.println("setVoteTo " + setVoteTo);
                    }
                    setVote(setVoteTo, finalReverseCurrentVoterIndex);
                });

                countDownTimeLine.play();
                unblockAllButtons();
                Button button = playerIdButton.get(reverseCurrentVoterIndex + 1);
                button.setDisable(true);
                updateButtonStates(reverseCurrentVoterIndex);
                return;  // Exit the loop and method after starting the count down
            }
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
                }
            } else {
                anotherPlayerButton.setDisable(true);
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
        System.out.println("Setting vote to player " + playerNumber);
        System.out.println("Setting vote number " + playerVotes);
        if (playerVotes == null) {
            playerIdVotesMap.put(playerNumber, 1);
        } else {
            playerIdVotesMap.put(playerNumber, playerVotes + 1);
        }
        //оновити вікно із результатом
        updateVotesDisplay();

        //перевірити на кінець голосування
        if (checkTheEndOfVoting(voterIndex)) {
            blockAllButtons();
            defineVotingResult();

        } else if(reverse){
            voterIndex = voterIndex - 1;
            giveVoiceReverse(voterIndex);
        } else {
            voterIndex = voterIndex + 1;
            giveVoiceForward(voterIndex);
        }
    }

    /**
     * Повертає gameStatistic, що вибув під час голосування
     * */
    private void defineVotingResult() {
        List<Integer> playersIdWithMaxVotes = findPlayersWithMaxVotesAmount(playerIdVotesMap);
        Integer playerInGameNumberToDelete;
        if (playersIdWithMaxVotes.size() > 1) {
//            playerInGameNumberToDelete = showRouletteWindow(playersIdWithMaxVotes);
            showRouletteWindow(playersIdWithMaxVotes, eliminatedPlayer -> {
                System.out.println("Player eliminated: " + eliminatedPlayer);
                // Handle the elimination of the player here
                gameStatisticsService.deletePlayerAfterVoting(SelectionController.currentGameId, eliminatedPlayer);
                //тут можливо ще зробити сервіс, який буде перевіряти чи гру закінчено, і дьоргати його методи
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Кінець голосування");
                alert.initOwner(stage);
                alert.show();
                fxWeaver.loadController(NightStageController.class).show();
            });
        } else {
            playerInGameNumberToDelete = playersIdWithMaxVotes.get(0);
            gameStatisticsService.deletePlayerAfterVoting(SelectionController.currentGameId, playerInGameNumberToDelete);
            //тут можливо ще зробити сервіс, який буде перевіряти чи гру закінчено, і дьоргати його методи
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Кінець голосування");
            alert.initOwner(stage);
            alert.show();
            fxWeaver.loadController(NightStageController.class).show();
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
                    alert.setContentText("Player " + currentPlayer + " clicked the death button! Player is out.");
                    alert.setOnHidden(e -> {
                        playerToDeleteInGameNumber[0] = currentPlayer;
                        window.close();
                    });
                    alert.show();
                } else {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
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

    private Integer showRouletteWindow(List<Integer> playersIdWithMaxVotes) {
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
                    alert.setContentText("Player " + currentPlayer + " clicked the death button! Player is out.");
                    alert.showAndWait();
                    playerToDeleteInGameNumber[0] = currentPlayer;
                    window.close();
                } else {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setContentText("Player " + currentPlayer + " is safe! Next player's turn.");
                    alert.showAndWait();
                    currentPlayerIndex[0] = (currentPlayerIndex[0] + 1) % numberOfPlayers;
                }
                button.setDisable(true);
            });
            layout.add(button, i % 3, i / 3);
        }
        Scene scene = new Scene(layout, 300, 200);
        countDownTimeLine.stop();
        window.setScene(scene);
        window.showAndWait();
        return playerToDeleteInGameNumber[0];
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
        if (reverse) {
            return voterIndex <= 0;
        }else {
            return Objects.equals(voterIndex, findLastAliveIndex());
        }
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
        button.setOnMouseEntered(e -> button.setStyle(HOVERED_BUTTON_STYLE));
        button.setOnMouseExited(e -> button.setStyle(IDLE_BUTTON_STYLE));
        return button;
    }

    public void show() {
        stage.show();
    }
}
