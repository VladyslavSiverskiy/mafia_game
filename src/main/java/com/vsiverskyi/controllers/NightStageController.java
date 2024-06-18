package com.vsiverskyi.controllers;

import com.vsiverskyi.model.Game;
import com.vsiverskyi.model.GameStatistics;
import com.vsiverskyi.model.Player;
import com.vsiverskyi.model.Role;
import com.vsiverskyi.model.enums.ERoleOrder;
import com.vsiverskyi.model.enums.ETeam;
import com.vsiverskyi.service.GameService;
import com.vsiverskyi.service.GameStatisticsService;
import com.vsiverskyi.service.RoleService;
import com.vsiverskyi.utils.ActionLogger;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.ResourceBundle;

import static com.vsiverskyi.utils.StyleConstants.HOVERED_BUTTON_STYLE;
import static com.vsiverskyi.utils.StyleConstants.IDLE_BUTTON_STYLE;

@Component
@RequiredArgsConstructor
@FxmlView("NightStage.fxml")
public class NightStageController implements Initializable {
    private Stage stage;
    private Scene scene;
    private Parent root;
    @Autowired
    private GameService gameService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private GameStatisticsService gameStatisticsService;
    @Autowired
    private FxWeaver fxWeaver;
    @FXML
    private AnchorPane nightStageAp;
    @FXML
    private AnchorPane nightStagePlayersPane;
    @FXML
    private Label currentRoleTitle;
    private List<GameStatistics> gameStatisticsListSortedByRoleOrder;
    private List<GameStatistics> gameStatisticsListSortedByInGameNumber;
    private Queue<ActionLogger> actionsQueue = new LinkedList<>();
    private Role currentRole;
    private int currentRoleIndex;
    private int selectedToKillPlayerNumber;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.stage = StarterController.primaryStage;
        stage.setScene(new Scene(nightStageAp));
        stage.setMaximized(true);

        gameStatisticsListSortedByRoleOrder = gameStatisticsService
                .getGameStatisticsByGameId(SelectionController.currentGameId);
        gameStatisticsListSortedByInGameNumber = gameStatisticsService
                .getGameStatisticsByGameIdSortedByInGameNumber(SelectionController.currentGameId);

        displayRolePlayers(gameStatisticsListSortedByRoleOrder.size());
        startRoleIterating();
    }

    private void startRoleIterating() {
        currentRole = gameStatisticsListSortedByRoleOrder.get(currentRoleIndex).getRole();
        currentRoleTitle.setText(currentRole.getTitle());
    }

    private void setNextRole() {
        currentRoleIndex++;
        System.out.println("Current role index: " + currentRoleIndex);
        if (currentRoleIndex == gameStatisticsListSortedByRoleOrder.size()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Кінець ночі");
            alert.show();
            return;
        }
        GameStatistics gameStatistics = gameStatisticsListSortedByRoleOrder.get(currentRoleIndex);
        if(gameStatistics.isInGame()) {
            currentRole = gameStatistics.getRole();
            currentRoleTitle.setText(currentRole.getTitle() + " " + currentRoleIndex);
            if(currentRole.getRoleNameConstant().equalsIgnoreCase(ERoleOrder.PEACE.name())) {
                System.out.println("Мирний");
                setNextRole();
            }
         }else {
            System.out.println("Next");
            setNextRole();
        }
    }

    private Boolean isNextRolePeace() {
        int tempCurrentRoleIndex = currentRoleIndex;
        tempCurrentRoleIndex++;
        if (tempCurrentRoleIndex == gameStatisticsListSortedByRoleOrder.size()) {
            return false;
        }
        while (!gameStatisticsListSortedByRoleOrder.get(tempCurrentRoleIndex).isInGame()) {
            tempCurrentRoleIndex++;
        }
        Role roleToCheck = gameStatisticsListSortedByRoleOrder.get(tempCurrentRoleIndex).getRole();
        System.out.println(roleToCheck);
        return roleToCheck.getTeam() == ETeam.PEACE;
    }

    private void handlePlayerAction(int chosenPlayerNumber) {
        switch (ERoleOrder.valueOf(currentRole.getRoleNameConstant())) {
            case MAFIA:
                ActionLogger mafiaMoveLogger = null;
                System.out.println("Mafia");
                if(gameStatisticsService.checkIfDonIsAlive(SelectionController.currentGameId)){
                    // якщо живий то мафія нікого не вбиває
                    mafiaMoveLogger = gameService.doMafiaSelectionMove(SelectionController.currentGameId, chosenPlayerNumber);
                    mafiaMoveLogger.setActionText("Мафія з ігровим номером " + chosenPlayerNumber + mafiaMoveLogger.getActionText());
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, mafiaMoveLogger.getActionText());
                    alert.show();
                }else {
                    // якщо ні, то мафія вибирає кого вбити
                    selectedToKillPlayerNumber = chosenPlayerNumber;
                    if(isNextRolePeace()) {
                        // kill
                         mafiaMoveLogger = gameService.doMafiaKillMove(SelectionController.currentGameId, chosenPlayerNumber);
                    }
                }
                setNextRole();
                actionsQueue.add(mafiaMoveLogger);
                break;
            // тут додавати logger в чергу?
            case DON:
                ActionLogger donMoveLogger = gameService.doMafiaKillMove(SelectionController.currentGameId, chosenPlayerNumber);
                actionsQueue.add(donMoveLogger);
                Alert alert = new Alert(Alert.AlertType.INFORMATION, donMoveLogger.getActionText());
                setNextRole();
                break;
            case DOCTOR:
                System.out.println("Doctor");
                ActionLogger doctorMoveLogger = gameService.doDoctorMove(SelectionController.currentGameId, chosenPlayerNumber);
                // тут додавати logger в чергу?
                actionsQueue.add(doctorMoveLogger);
                setNextRole();
                break;
        }
    }


    private void displayRolePlayers(int totalPlayers) {
        double centerX = nightStagePlayersPane.getWidth() / 2;
        double centerY = nightStagePlayersPane.getHeight() / 2;
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
            Button button = createPlayerButton(x, y, i);
            if (!checkIfAlive(i, totalPlayers)) {
                playerPanel.setStyle("-fx-opacity: 0.3");
                button.setDisable(true);
            }
            nightStagePlayersPane.getChildren().add(playerPanel);
            int finalI = i;
            button.setOnAction(e -> handlePlayerAction(finalI)); // Set the click handler
            if (i == 0 || i == totalPlayers + 1) {
                playerPanel.setVisible(false);
                button.setVisible(false);
            }
            nightStagePlayersPane.getChildren().add(button);
        }
    }


    /**
     * returns true if player is alive
     */
    private Boolean checkIfAlive(int playerNumber, int totalPlayers) {
        return playerNumber != 0 && playerNumber != totalPlayers + 1
               && gameStatisticsListSortedByInGameNumber.get(playerNumber - 1).isInGame();
    }

    private Label createNicknameLabel(int i) { // When value of button is "1", then get element with 0 index
        GameStatistics currentGamer = gameStatisticsListSortedByInGameNumber.get(i - 1);
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
