package com.vsiverskyi.controllers;

import com.vsiverskyi.model.GameStatistics;
import com.vsiverskyi.model.Player;
import com.vsiverskyi.service.GameService;
import com.vsiverskyi.service.GameStatisticsService;
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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
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
import java.util.stream.Collectors;

import static com.vsiverskyi.utils.StyleConstants.HOVERED_BUTTON_STYLE;
import static com.vsiverskyi.utils.StyleConstants.IDLE_BUTTON_STYLE;

@Component
@RequiredArgsConstructor
@FxmlView("Voting.fxml")
public class VotingController implements Initializable {

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
    private AnchorPane votingAp;
    @FXML
    private AnchorPane votingPlayersPane;
    @FXML
    private Button beginVoting;
    @FXML
    private VBox votesDisplay;
    @FXML
    private Label secondsLeft;
    private Timeline countDownTimeLine;
    private Map<Integer, Integer> playerIdVotesMap = new HashMap<>();
    private Map<Integer, Button> playerIdButton = new HashMap<>();
    private List<GameStatistics> gameStatisticsList;
    private int currentVoterIndex = 0;
    int secondsTillEnd = 10;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.stage = StarterController.primaryStage;
        stage.setScene(new Scene(votingAp));
        stage.setMaximized(true);
        gameStatisticsList = gameService.getGameInfo(SelectionController.currentGameId).getGameStatistics();


        displayRolePlayers(gameStatisticsList.size());

        beginVoting.setOnAction(actionEvent -> beginVoting());
    }

    private void beginVoting() {
        giveVoice(currentVoterIndex);
    }

    /**
     * Метод, який відповідає за те щоб конкретний гравець проголосував за когось
     */
    private void giveVoice(int currentVoterIndex) {
        if (currentVoterIndex < gameStatisticsList.size()) {
            secondsTillEnd = 10;
            countDownTimeLine = new Timeline(new KeyFrame(Duration.seconds(1), (ActionEvent event) -> {
                if (secondsTillEnd == 1) {
                    blockAllButtons();
                }
                secondsLeft.setText(String.valueOf(secondsTillEnd--));
            }));
            // Set number of cycles (remaining duration in seconds):
            countDownTimeLine.setCycleCount((int) secondsTillEnd);
            countDownTimeLine.setOnFinished(event -> {
                if (currentVoterIndex == gameStatisticsList.size() - 1) {
                    setVote(1);
                } else {
                    setVote(currentVoterIndex + 1 + 1); // + 2 тому що перша одиничка - номер, друга - наступний номер
                }
            });

            countDownTimeLine.play();
            unblockAllButtons();
            Button button = playerIdButton.get(currentVoterIndex + 1);
            button.setDisable(true);
            for (Map.Entry<Integer, Button> entry : playerIdButton.entrySet()) {
                int playerId = entry.getKey();
                Button anotherPlayerButton = entry.getValue();
                if (playerId != currentVoterIndex + 1) {
                    anotherPlayerButton.setDisable(false);
                } else {
                    anotherPlayerButton.setDisable(true);
                }
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

    private void setVote(int playerNumber) {
        Integer playerVotes = playerIdVotesMap.get(playerNumber);
        if (playerVotes == null) {
            playerIdVotesMap.put(playerNumber, 1);
        } else {
            playerIdVotesMap.put(playerNumber, playerVotes + 1);
        }
        //оновити вікно із результатом
        updateVotesDisplay();

        //перевірити на кінець голосування
        if (checkTheEndOfVoting()) {
            /// stop game
            blockAllButtons();
            defineVotingResult();
            //тут можливо ще зробити сервіс, який буде перевіряти чи гру закінчено, і дьоргати його методи
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Кінець голосування");
            alert.show();
            fxWeaver.loadController(NightStageController.class).show();
        } else {
            giveVoice(++currentVoterIndex);
        }
    }

    /**
     * Повертає gameStatistic, що вибув під час голосування
     * */
    private GameStatistics defineVotingResult() {
        List<Integer> playersIdWithMaxVotes = findPlayersWithMaxVotesAmount(playerIdVotesMap);
        Integer playerInGameNumberToDelete;
        if (playersIdWithMaxVotes.size() > 1) {
            playerInGameNumberToDelete = showRouletteWindow(playersIdWithMaxVotes);
        } else {
            playerInGameNumberToDelete = playersIdWithMaxVotes.get(0);
        }
        return gameStatisticsService.deletePlayerAfterVoting(SelectionController.currentGameId, playerInGameNumberToDelete);
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

    private boolean checkTheEndOfVoting() {
        return currentVoterIndex == gameStatisticsList.size() - 1;
    }

    private void displayRolePlayers(int totalPlayers) {
        double centerX = votingPlayersPane.getWidth() / 2;
        double centerY = votingPlayersPane.getHeight() / 2;
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
            votingPlayersPane.getChildren().add(playerPanel);
            Button button = createPlayerButton(x, y, i);
            int finalI = i;
            button.setOnAction(actionEvent -> {
                if (countDownTimeLine != null) {
                    countDownTimeLine.stop();
                }
                setVote(Integer.parseInt(button.getText()));
            });
            button.setDisable(true);

            if (i == 0 || i == totalPlayers + 1) {
                playerPanel.setVisible(false);
                button.setVisible(false);
            } else {
                playerIdButton.put(i, button);
            }
            votingPlayersPane.getChildren().add(button);
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
