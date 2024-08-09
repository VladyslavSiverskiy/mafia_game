package com.vsiverskyi.controllers;

import com.vsiverskyi.model.GameStatistics;
import com.vsiverskyi.model.Player;
import com.vsiverskyi.model.enums.ERoleOrder;
import com.vsiverskyi.model.enums.ETeam;
import com.vsiverskyi.service.GameService;
import com.vsiverskyi.service.GameStatisticsService;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ViewController {

    @Autowired
    private GameService gameService;
    @Autowired
    private GameStatisticsService gameStatisticsService;
    @Autowired
    private PenaltyController penaltyController;

    public void showCards(
            int yellowCardsIterator,
            int redCardsIterator,
            HBox avatarContainer,
            int playerInGameNumber,
            DisplayedPlayersController controller,
            int amountOfPlayers,
            Stage stage,
            ListView<HBox> playerCardListView,
            List<GameStatistics> gameStatisticsList
    ) {
        for (int j = 0; j < yellowCardsIterator; j++) { // Adjust the number of yellow cards as needed
            Rectangle yellowCard = new Rectangle(8, 12, Color.YELLOW);
            yellowCard.setOnMouseClicked(mouseEvent -> {
                gameStatisticsService.removeYellowCard(SelectionController.currentGameId, playerInGameNumber);
                controller.displayRolePlayers(amountOfPlayers);
            });
            yellowCard.setStyle("-fx-border-radius: 1px; -fx-background-color: yellow");
            avatarContainer.getChildren().add(yellowCard);
        }
        for (int j = 0; j < redCardsIterator; j++) { // Adjust the number of yellow cards as needed
            Rectangle redCard = new Rectangle(8, 12, Color.RED);
            redCard.setOnMouseClicked(mouseEvent -> {
                gameStatisticsService.removeRedCard(SelectionController.currentGameId, playerInGameNumber);
                controller.displayRolePlayers(amountOfPlayers);
                penaltyController.initializePlayerCardList(
                        gameStatisticsService.getGameStatisticsByGameIdSortedByInGameNumber(SelectionController.currentGameId), stage, controller, playerCardListView);
            });
            redCard.setStyle("-fx-border-radius: 1px");
            avatarContainer.getChildren().add(redCard);
        }
    }

    public Label createNicknameLabel(int i, List<GameStatistics> gameStatisticsList) { // When value of button is "1", then get element with 0 index
        GameStatistics currentGamer = gameStatisticsList.get(i - 1);
        Player player = currentGamer.getPlayer();
        Label nicknameLabel = new Label();
        if (player != null) {
            nicknameLabel.setText(player.getNickname());
        } else if (currentGamer.getInGameNickname() != null) {
            nicknameLabel.setText(currentGamer.getInGameNickname().toUpperCase());
        } else {
            nicknameLabel.setText("НЕЗНАЙОМЕЦЬ");
        }

        if (currentGamer.isInGame()) {
            nicknameLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #f4ff67");
        }else {
            nicknameLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #cc2323");
        }
        return nicknameLabel;
    }

    public VBox createPlayerStatisticsPanel() {
        // Group by role and count the number of players in each role
        List<GameStatistics> gameStatisticsAlive =
                gameService.findById(SelectionController.currentGameId).getGameStatistics()
                        .stream()
                        .filter(gameStatistics -> gameStatistics.isInGame() && gameStatistics.getRedCards() == 0).collect(Collectors.toList());

        int mafiaAmount = (int) gameStatisticsAlive.stream()
                .filter(gameStatistics -> gameStatistics.getRole().getRoleNameConstant().equals(ERoleOrder.DON.name())
                    || gameStatistics.getRole().getRoleNameConstant().equals(ERoleOrder.MAFIA.name())
                ).count();
        int peaceAmount = (int) gameStatisticsAlive.stream()
                .filter(gameStatistics -> gameStatistics.getRole().getTeam().name().equals(ETeam.PEACE.name())).count();

        //Шукаємо яничара, але так як він у нас ще в мирних числиться, то мінусуємо одного від мирних
        int yanycharAmount = (int) gameStatisticsAlive.stream()
                .filter(gameStatistics ->
                        gameStatistics.getRole().getRoleNameConstant().equals(ERoleOrder.PEREVERTEN_PEACE.name()) ||
                        gameStatistics.getRole().getRoleNameConstant().equals(ERoleOrder.PEREVERTEN_MAFIA.name()))
                .count();

        if(mafiaAmount > 0 && yanycharAmount > 0) {
            peaceAmount = peaceAmount - yanycharAmount;
        }

        // Create labels for each role
        Label mafiaLabel = new Label("Корупціонерів: " + mafiaAmount);
        Label peaceLabel = new Label("Жителі Скіфії: " + peaceAmount);
        Label perevertenLabel = new Label("Яничар: " + yanycharAmount);

        // Set styles for labels
        mafiaLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #ffffff;");
        peaceLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #ffffff;");
        perevertenLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #ffffff;");

        // Create a VBox and add labels
        VBox statisticsPanel = new VBox(10); // 10 is the spacing between elements
        statisticsPanel.getChildren().addAll(mafiaLabel, peaceLabel, perevertenLabel);

        return statisticsPanel;
    }
}
