package com.vsiverskyi.controllers;

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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
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
@FxmlView("NightStage.fxml")
public class NightStageController implements Initializable,DisplayedPlayersController {
    private Stage stage;
    private Scene scene;
    private Parent root;
    @Autowired
    private GameService gameService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private PenaltyController penaltyController;
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
    @FXML
    private VBox adminMenu;
    @FXML
    private Label selectedPlayerLabel;
    @FXML
    private Button technicalDefeatPeaceful;
    @FXML
    private Button technicalDefeatMafia;
    @FXML
    private Button fullScreen;
    @FXML
    private ListView<HBox> playerCardListView;
    private List<GameStatistics> gameStatisticsListSortedByRoleOrder;
    private List<GameStatistics> gameStatisticsListSortedByInGameNumber;
    private Map<Integer, Role> playerIdRoleMap = new HashMap<>();
    private Map<Integer, Button> playerButtonsMap = new HashMap<>(); // Map to store buttons
    private Map<Integer, Label> playerRoleLabelsMap = new HashMap<>(); // Map to store labels
    private Queue<ActionLogger> actionsQueue = new LinkedList<>();
    private Role currentRole;
    private int currentRoleIndex;
    private int selectedToKillPlayerNumber;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.stage = StarterController.primaryStage;
        scene = new Scene(nightStageAp);
        stage.setScene(scene);
        scene.getStylesheets().add(getClass().getResource("/style/style.css").toExternalForm());

        stage.setMaximized(true);

        updatePlayersList();
        initPlayerRoleMap();

        // Initialize player card list view
        technicalDefeatPeaceful.setOnAction(e -> penaltyController.assignTechnicalDefeat("PEACE"));
        technicalDefeatMafia.setOnAction(e -> penaltyController.assignTechnicalDefeat("MAFIA"));
        penaltyController.initializePlayerCardList(gameStatisticsListSortedByInGameNumber, stage,this, playerCardListView);
        fullScreen.setOnAction(ev -> stage.setFullScreen(true));

        displayRolePlayers(gameStatisticsListSortedByRoleOrder.size());
        startRoleIterating();
    }

    private void initPlayerRoleMap() {
        for (GameStatistics gs: gameStatisticsListSortedByInGameNumber) {
            playerIdRoleMap.put(gs.getInGameNumber(), gs.getRole());
        }
    }

    private GameStatistics findByInGameNumber(Integer inGameNumber) {
        return gameStatisticsListSortedByInGameNumber
                .stream()
                .filter(gs -> gs.getInGameNumber() == inGameNumber)
                .findFirst().orElseThrow(() -> new RuntimeException("No player with number " + inGameNumber));

    }

    private void startRoleIterating() {
        // можна ліст замінити на сет ролей
        currentRole = gameStatisticsListSortedByRoleOrder.get(currentRoleIndex).getRole();
        currentRoleTitle.setText(currentRole.getTitle()); // буде писати мафія
    }

    private void setNextRole() {
        updatePlayersList();
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

    private void updatePlayersList() {
        gameStatisticsListSortedByRoleOrder = gameStatisticsService
                .getGameStatisticsByGameId(SelectionController.currentGameId);
        gameStatisticsListSortedByInGameNumber = gameStatisticsService
                .getGameStatisticsByGameIdSortedByInGameNumber(SelectionController.currentGameId);
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
                if (playerIdRoleMap.get(chosenPlayerNumber).getRoleNameConstant().equals(ERoleOrder.MAFIA.name())) {
                    Alert mafiaCantChooseHimselfAlert =
                            new Alert(Alert.AlertType.INFORMATION, "Мафія не може проголосувати за себе");
                    mafiaCantChooseHimselfAlert.show();
                }else{
                    ActionLogger mafiaMoveLogger = null;
                    System.out.println("Mafia");
                    if(gameStatisticsService.checkIfDonIsAlive(SelectionController.currentGameId)){
                        // якщо живий то мафія нікого не вбиває
                        mafiaMoveLogger = gameService.doMafiaSelectionMove(SelectionController.currentGameId, chosenPlayerNumber);
                        mafiaMoveLogger.setActionText("Мафія " + chosenPlayerNumber + mafiaMoveLogger.getActionText());
                        Alert alert = new Alert(Alert.AlertType.INFORMATION, mafiaMoveLogger.getActionText());
                        alert.show();
                    }else {
                        // якщо ні, то мафія вибирає кого вбити
                        selectedToKillPlayerNumber = chosenPlayerNumber;
                        mafiaMoveLogger = gameService.doMafiaKillMove(SelectionController.currentGameId, chosenPlayerNumber);
                        // у випадку, якщо дві мафії ходять за одну - не треба цю перевірку
//                    if(isNextRolePeace()) {
                        // kill
//                         mafiaMoveLogger = gameService.doMafiaKillMove(SelectionController.currentGameId, chosenPlayerNumber);
//                    }
                    }
                    setNextRole();
                    actionsQueue.add(mafiaMoveLogger);
                }
                break;
            // тут додавати logger в чергу?
            case DON:
                if(playerIdRoleMap.get(chosenPlayerNumber).getRoleNameConstant().equals(ERoleOrder.DON.name())) {
                    Alert donCantChooseHimselfAlert =
                            new Alert(Alert.AlertType.INFORMATION, "Дон не може проголосувати за себе");
                    donCantChooseHimselfAlert.show();
                } else {
                    ActionLogger donMoveLogger = gameService.doMafiaKillMove(SelectionController.currentGameId, chosenPlayerNumber);
                    actionsQueue.add(donMoveLogger);
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, donMoveLogger.getActionText());
                    alert.show();
                    setNextRole();
                }
                break;
            case DOCTOR:
                if (playerIdRoleMap.get(chosenPlayerNumber).getRoleNameConstant().equals(ERoleOrder.DOCTOR.name())
                    && (findByInGameNumber(chosenPlayerNumber).getTimesWasHealed() >= 2)) {
                        Alert doctorCantHealHimselfAlert =
                                new Alert(Alert.AlertType.INFORMATION, "Лікар не може лікувати себе більше двох разів");
                        doctorCantHealHimselfAlert.show();
                }else {
                    doDoctorMove(chosenPlayerNumber);
                    setNextRole();
                }
                break;
        }
    }

    private void doDoctorMove(int chosenPlayerNumber) {
        System.out.println("Doctor");
        System.out.println(playerIdRoleMap);
        System.out.println(playerIdRoleMap.get(chosenPlayerNumber));
        ActionLogger doctorMoveLogger = gameService.doDoctorMove(SelectionController.currentGameId, chosenPlayerNumber);
        // тут додавати logger в чергу?
        actionsQueue.add(doctorMoveLogger);

    }
    @Override
    public void displayRolePlayers(int totalPlayers) {
        double centerX = nightStagePlayersPane.getWidth() / 2;
        double centerY = nightStagePlayersPane.getHeight() / 2;
        double radius = Math.min(centerX, centerY) - 5;
        double startAngle = Math.PI / 1.8;

        for (int i = 0; i < totalPlayers + 2; i++) { //
            double angle = startAngle + 2 * Math.PI * i / (totalPlayers + 2);
            double x = centerX + radius * Math.cos(angle);
            double y = centerY + radius * Math.sin(angle);


            GameStatistics gameStatistics = null;
            if (i != 0 && i != totalPlayers + 1) {
                int finalI1 = i;
                gameStatisticsListSortedByInGameNumber = gameStatisticsService
                        .getGameStatisticsByGameIdSortedByInGameNumber(SelectionController.currentGameId);
                gameStatistics = gameStatisticsListSortedByInGameNumber
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
                Role role = playerIdRoleMap.get(i);
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
                playerRoleLabelsMap.put(i, roleLabel);
            }

            Button button = playerButtonsMap.get(i);
            if (button == null) {
                button = createPlayerButton(x, y, i);
            }

            if (!checkIfAlive(i, totalPlayers)) {
                playerPanel.setDisable(true);
                playerPanel.setVisible(true);
                avatar.setFill(Color.DARKGREY);
                button.setDisable(true);
            }
            nightStagePlayersPane.getChildren().add(playerPanel);
            int finalI = i;
            button.setOnAction(e -> handlePlayerAction(finalI)); // Set the click handler
            if (i == 0 || i == totalPlayers + 1) {
                playerPanel.setVisible(false);
                button.setVisible(false);
            }

            playerButtonsMap.put(i, button);
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
