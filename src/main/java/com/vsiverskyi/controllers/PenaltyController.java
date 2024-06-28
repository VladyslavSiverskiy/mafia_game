package com.vsiverskyi.controllers;

import com.vsiverskyi.model.GameStatistics;
import com.vsiverskyi.service.GameService;
import com.vsiverskyi.service.GameStatisticsService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
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
        gameStatisticsService.removePlayerFromGame(SelectionController.currentGameId, playerNumber);
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Гравець " + playerNumber + " отримав червону картку");
        alert.initOwner(stage);
        alert.show();
        //TODO: перевірити на кінець
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
            HBox playerCardRow = new HBox(10);
            Label playerLabel = new Label("Гравець " + gs.getInGameNumber());
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
                //TODO: Check if game is over
            });
            // Create a Region to act as a spacer
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            if (!gs.isInGame()) {
                yellowCardButton.setDisable(true);
                redCardButton.setDisable(true);
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
