package com.vsiverskyi.controllers;

import com.vsiverskyi.model.GameStatistics;
import com.vsiverskyi.service.GameService;
import com.vsiverskyi.service.GameStatisticsService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
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
        System.out.println("INIT");
        ObservableList<HBox> playerCards = FXCollections.observableArrayList();

        for (GameStatistics gs : gameStatisticsList) {
            HBox playerCardRow = new HBox(5);

            String nickname = gs.getInGameNickname() != null ? gs.getInGameNickname() : "Незнайомець";
            String displayNickname = nickname;
            if (nickname.length() > 15) {
                displayNickname = nickname.substring(0, 12) + "...";
            }

            Label playerLabel = new Label(gs.getInGameNumber() + "." + displayNickname.toUpperCase());

            Tooltip fullNicknameTooltip = new Tooltip(nickname);
            playerLabel.setTooltip(fullNicknameTooltip);
// Create a Tooltip with the full nickname
//            Tooltip.install(playerLabel, fullNicknameTooltip);

//            playerCardRow.getChildren().add(playerLabel);
            Button yellowCardButton = new Button();
            yellowCardButton.setStyle("-fx-background-color: yellow; -fx-width: 15px; -fx-height: 20px;");

            Button redCardButton = new Button();
            redCardButton.setStyle("-fx-background-color: red; -fx-width: 15px; -fx-min-height: 20px;");

            int playerNumber = gs.getInGameNumber();
            yellowCardButton.setOnAction(e -> {
                giveYellowCard(playerNumber, yellowCardButton, redCardButton, stage);
                controller.displayRolePlayers(gameStatisticsList.size());
            });
            redCardButton.setOnAction(e -> {
                giveRedCard(playerNumber, yellowCardButton, redCardButton, stage);
                controller.displayRolePlayers(gameStatisticsList.size());
            });
            // Create a Region to act as a spacer
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            System.out.println(gs);
            if (!gs.isInGame()) {
                yellowCardButton.setDisable(true);
                redCardButton.setDisable(true);
            }else {
                yellowCardButton.setDisable(false);
                redCardButton.setDisable(false);
            }
            playerCardRow.getChildren().addAll(playerLabel, spacer, yellowCardButton, redCardButton);
            playerCards.add(playerCardRow);
        }

        playerCardListView.setItems(playerCards);
    }

    public void assignTechnicalDefeat(String side) {
        // Implement the logic to assign a technical defeat to the specified side
        gameService.finishGameDueToTechnicalLoose(SelectionController.currentGameId, side);
        fxWeaver.loadController(GameEndingController.class).show();
    }

}
