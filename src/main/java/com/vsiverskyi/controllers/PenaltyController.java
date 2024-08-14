package com.vsiverskyi.controllers;

import com.vsiverskyi.model.GameStatistics;
import com.vsiverskyi.service.GameService;
import com.vsiverskyi.service.GameStatisticsService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import net.rgielen.fxweaver.core.FxWeaver;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class PenaltyController {
    private final MusicController musicController;
    private final GameStatisticsService gameStatisticsService;
    private final GameService gameService;
    private final FxWeaver fxWeaver;

    public void giveYellowCard(int playerNumber, Button yellowButton, Button redButton, Stage stage) {
        // Get the current number of yellow cards from the database
        GameStatistics gs = gameStatisticsService.getGameStatisticsByGameIdSortedByInGameNumber(SelectionController.currentGameId)
                .stream()
                .filter(stat -> stat.getInGameNumber() == playerNumber)
                .findFirst()
                .orElse(null);

        if (gs != null) {
            int yellowCards = gs.getYellowCards();
            yellowCards++;
            gs.setYellowCards(yellowCards);

            // Save the updated yellow card count back to the database
            gameStatisticsService.updateYellowCards(gs.getGame().getId(), gs.getInGameNumber(), yellowCards);

            if (yellowCards >= 4) {
                giveRedCard(playerNumber, yellowButton, redButton, stage);
            } else if (yellowCards >= 3) {
                gameStatisticsService.setSkipNextVoting(gs);
            } else {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Гравець " + playerNumber + " отримав жовту картку");
                alert.initOwner(stage);
                alert.show();
            }
        }
    }

    public void giveRedCard(int playerNumber, Button yellowButton, Button redButton, Stage stage) {
        yellowButton.setDisable(true);
        redButton.setDisable(true);
        gameStatisticsService.resetYellowCardsAmountAndGiveRedOne(SelectionController.currentGameId, playerNumber);
        gameStatisticsService.removePlayerFromGame(SelectionController.currentGameId, playerNumber);
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Гравець " + playerNumber + " отримав червону картку");
        alert.initOwner(stage);
        alert.show();
        if (gameService.checkIfGameIsOver(SelectionController.currentGameId)) {
            fxWeaver.loadController(GameEndingController.class);
        }
    }

    public void initializePlayerCardList(
            List<GameStatistics> gameStatisticsList,
            Stage stage,
            DisplayedPlayersController controller,
            ListView<HBox> playerCardListView
    ) {
        ObservableList<HBox> playerCards = FXCollections.observableArrayList();

        for (GameStatistics gs : gameStatisticsList) {
            HBox playerCardRow = new HBox(5); // Reduced spacing between elements
            playerCardRow.setAlignment(Pos.CENTER_LEFT); // Align items to center-left
            playerCardRow.setPadding(new Insets(2, 5, 2, 5)); // Minimal padding for compactness

            String nickname = gs.getInGameNickname() != null ? gs.getInGameNickname() : "Незнайомець";
            String displayNickname = nickname;
            if (nickname.length() > 15) {
                displayNickname = nickname.substring(0, 12) + "...";
            }

            Label playerLabel = new Label(gs.getInGameNumber() + ". " + displayNickname.toUpperCase());
            playerLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #ffffff;"); // Reduced font size and set text color

            // Tooltip with full nickname
            Tooltip fullNicknameTooltip = new Tooltip(nickname);
            Tooltip.install(playerLabel, fullNicknameTooltip);

            // Create smaller yellow card button
            Button yellowCardButton = new Button();
            yellowCardButton.setStyle("-fx-background-color: yellow; -fx-min-width: 10px; -fx-min-height: 10px; -fx-max-width: 10px; -fx-max-height: 10px; -fx-cursor: hand;");
            yellowCardButton.setTooltip(new Tooltip("Додати жовту картку")); // Tooltip for clarity

            // Create smaller red card button
            Button redCardButton = new Button();
            redCardButton.setStyle("-fx-background-color: red; -fx-min-width: 10px; -fx-min-height: 10px; -fx-max-width: 10px; -fx-max-height: 10px; -fx-cursor: hand;");
            redCardButton.setTooltip(new Tooltip("Додати червону картку")); // Tooltip for clarity

            // Create a larger pause button with an image

            int playerNumber = gs.getInGameNumber();
            yellowCardButton.setOnMouseClicked(e -> {

                giveYellowCard(playerNumber, yellowCardButton, redCardButton, stage);
                controller.displayRolePlayers(gameStatisticsList.size());
            });
            redCardButton.setOnMouseClicked(e -> {
                giveRedCard(playerNumber, yellowCardButton, redCardButton, stage);
                controller.displayRolePlayers(gameStatisticsList.size());
            });

            Button pauseButton = new Button();

            if (controller instanceof VotingController) {
                Image pauseImage = new Image(getClass().getResourceAsStream("/images/pause.png")); // Ensure the path is correct
                ImageView pauseImageView = new ImageView(pauseImage);
                pauseImageView.setFitWidth(10); // Set image width
                pauseImageView.setFitHeight(10); // Set image height
                pauseButton.setGraphic(pauseImageView);
                pauseButton.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-min-width: 10px; -fx-min-height: 10px; -fx-max-width: 10px; -fx-max-height: 10px; -fx-cursor: hand;");
                pauseButton.setTooltip(new Tooltip("Пропустити наступне голосування")); // Tooltip for clarity
                pauseButton.setOnMouseClicked(e -> {
                    gameStatisticsService.setSkipNextVoting(gs);
                    controller.displayRolePlayers(gameStatisticsList.size());
                });
            }
            // Disable buttons if the player is not in the game
            if (!gs.isInGame()) {
                yellowCardButton.setDisable(true);
                redCardButton.setDisable(true);
                pauseButton.setDisable(true);
                playerLabel.setStyle(playerLabel.getStyle() + "-fx-opacity: 0.5;"); // Dim label to indicate inactivity
            } else {
                yellowCardButton.setDisable(false);
                pauseButton.setDisable(false);
                redCardButton.setDisable(false);
            }

            // Create a Region to act as a spacer
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            if (controller instanceof VotingController) {
                playerCardRow.getChildren().addAll(playerLabel, spacer, yellowCardButton, redCardButton, pauseButton);
            } else {
                playerCardRow.getChildren().addAll(playerLabel, spacer, yellowCardButton, redCardButton);
            }
            playerCardRow.setStyle("-fx-background-color: #2b2b2b; -fx-background-radius: 5;"); // Optional: Set background color and rounded corners for better aesthetics

            playerCards.add(playerCardRow);
        }

        // Optional: Set a minimal height for the ListView rows
        playerCardListView.setFixedCellSize(30);
        playerCardListView.setItems(playerCards);
    }



    public void assignTechnicalDefeat(String side) {
        musicController.stopPlayback();
        // Implement the logic to assign a technical defeat to the specified side
        gameService.finishGameDueToTechnicalLoose(SelectionController.currentGameId, side);
        fxWeaver.loadController(GameEndingController.class).show();
    }

}
