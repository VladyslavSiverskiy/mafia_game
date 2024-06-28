package com.vsiverskyi.controllers;

import com.vsiverskyi.model.GameStatistics;
import com.vsiverskyi.model.Player;
import com.vsiverskyi.model.Role;
import com.vsiverskyi.model.enums.ERoleOrder;
import com.vsiverskyi.model.enums.ETeam;
import com.vsiverskyi.service.GameService;
import com.vsiverskyi.service.GameStatisticsService;
import com.vsiverskyi.service.RoleService;
import com.vsiverskyi.utils.Action;
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
import java.time.LocalDateTime;
import java.util.*;

import static com.vsiverskyi.utils.StyleConstants.HOVERED_BUTTON_STYLE;
import static com.vsiverskyi.utils.StyleConstants.IDLE_BUTTON_STYLE;

@Component
@RequiredArgsConstructor
@FxmlView("NightStage.fxml")
public class NightStageController implements Initializable, DisplayedPlayersController {
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
    private List<Role> actualInGameRoles; //here should be converted set -> list
    private Map<Integer, Role> playerIdRoleMap;
    private Map<Integer, Button> playerButtonsMap; // Map to store buttons
    private Map<Integer, Label> playerRoleLabelsMap; // Map to store labels
    private Queue<Action> actionsQueue = new LinkedList<>();
    private Role currentRole;
    private int currentRoleIndex;
    private int selectedToKillPlayerNumber;
    /**
     * Sum of all game statistics` 'timesWasKilled' fields of current game
     * */
    private int archerAttemptsAmount;
    private int strilochnykIndex;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.stage = StarterController.primaryStage;
        scene = new Scene(nightStageAp);
        stage.setScene(scene);
        scene.getStylesheets().add(getClass().getResource("/style/style.css").toExternalForm());

        currentRoleIndex = 0;
        archerAttemptsAmount = 0;
        strilochnykIndex = 0;
        actualInGameRoles = new ArrayList<>();
        playerIdRoleMap = new HashMap<>();
        playerButtonsMap = new HashMap<>();
        playerRoleLabelsMap = new HashMap<>();

        stage.setMaximized(true);

        gameService.resetStrilochnykAttempts(SelectionController.currentGameId);
        updatePlayersList();
        initPlayerRoleMap();
        gameStatisticsService.removeAllVotingSkipsPerDay(SelectionController.currentGameId);

        // Initialize player card list view
        technicalDefeatPeaceful.setOnAction(e -> penaltyController.assignTechnicalDefeat("PEACE"));
        technicalDefeatMafia.setOnAction(e -> penaltyController.assignTechnicalDefeat("MAFIA"));
        penaltyController.initializePlayerCardList(gameStatisticsListSortedByInGameNumber, stage, this, playerCardListView);
        fullScreen.setOnAction(ev -> stage.setFullScreen(true));

        displayRolePlayers(gameStatisticsListSortedByRoleOrder.size());
        startRoleIterating();
    }

    private String queueToString(Queue<Action> actions) {
        StringBuilder stringBuilder = new StringBuilder();
        while (!actions.isEmpty()) {
            Action action = actions.poll(); // Retrieves and removes the head of the queue
            stringBuilder.append(action.toString()); // Append the string representation of the action
            if (!actions.isEmpty()) {
                stringBuilder.append("\n"); // Append a new line if there are more actions
            }
        }
        return stringBuilder.toString();
    }

    private void initPlayerRoleMap() {
        for (GameStatistics gs : gameStatisticsListSortedByInGameNumber) {
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
        currentRole = actualInGameRoles.get(currentRoleIndex);
        currentRoleTitle.setText(currentRole.getTitle()); // буде писати мафія
    }

    private void setNextRole() {
//        updatePlayersList();
        currentRoleIndex++;
        System.out.println("Current role index: " + currentRoleIndex);
        if (currentRoleIndex == actualInGameRoles.size()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, queueToString(actionsQueue));
            alert.showAndWait();
            if (gameService.checkIfGameIsOver(SelectionController.currentGameId)) {
                fxWeaver.loadController(GameEndingController.class).show();
            } else {
                fxWeaver.loadController(VotingController.class).show();
            }
        }else {
            currentRole = actualInGameRoles.get(currentRoleIndex);
            currentRoleTitle.setText(currentRole.getTitle() + " " + currentRoleIndex);
            System.out.println(actualInGameRoles);
            if (currentRole.getRoleNameConstant().equalsIgnoreCase(ERoleOrder.PEACE.name())) {
                setNextRole();
            }
        }
    }

    private void updatePlayersList() {
        gameStatisticsListSortedByRoleOrder = gameStatisticsService
                .getGameStatisticsByGameId(SelectionController.currentGameId);
        gameStatisticsListSortedByInGameNumber = gameStatisticsService
                .getGameStatisticsByGameIdSortedByInGameNumber(SelectionController.currentGameId);
        actualInGameRoles = gameStatisticsListSortedByRoleOrder.stream()
                .filter(GameStatistics::isInGame)
                .map(GameStatistics::getRole)
                .distinct()
                .toList();
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
        System.out.println(actualInGameRoles);
        System.out.println(ERoleOrder.valueOf(currentRole.getRoleNameConstant()));
        switch (ERoleOrder.valueOf(currentRole.getRoleNameConstant())) {
            case MAFIA:
                if (playerIdRoleMap.get(chosenPlayerNumber).getRoleNameConstant().equals(ERoleOrder.MAFIA.name())) {
                    Alert mafiaCantChooseHimselfAlert =
                            new Alert(Alert.AlertType.INFORMATION, "Мафія не може проголосувати за себе");
                    mafiaCantChooseHimselfAlert.show();
                } else {
                    Action mafiaMoveLogger = null;
                    System.out.println("Mafia");
                    if (gameStatisticsService.checkIfDonIsAlive(SelectionController.currentGameId)) {
                        // якщо живий то мафія нікого не вбиває
                        mafiaMoveLogger = gameService.doMafiaSelectionMove(SelectionController.currentGameId, chosenPlayerNumber);
                        mafiaMoveLogger.setActionText("Мафія " + chosenPlayerNumber + mafiaMoveLogger.getActionText());
                        Alert alert = new Alert(Alert.AlertType.INFORMATION, mafiaMoveLogger.getActionText());
                        alert.show();
                    } else {
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
                if (playerIdRoleMap.get(chosenPlayerNumber).getRoleNameConstant().equals(ERoleOrder.DON.name())) {
                    Alert donCantChooseHimselfAlert =
                            new Alert(Alert.AlertType.INFORMATION, "Дон не може проголосувати за себе");
                    donCantChooseHimselfAlert.show();
                } else {
                    Action donMoveLogger = gameService.doMafiaKillMove(SelectionController.currentGameId, chosenPlayerNumber);
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
                } else {
                    doDoctorMove(chosenPlayerNumber);
                    setNextRole();
                }
                break;
            case LEDY:
                if (playerIdRoleMap.get(chosenPlayerNumber).getRoleNameConstant().equals(ERoleOrder.LEDY.name())) {
                    Alert ladyCantChooseHerselfAlert =
                            new Alert(Alert.AlertType.INFORMATION, "Леді не може голосувати за себе");
                    ladyCantChooseHerselfAlert.show();
                } else {
                    doLedyMove(chosenPlayerNumber);
                    setNextRole();
                }
                break;
            case SHERYF:
                if (playerIdRoleMap.get(chosenPlayerNumber).getRoleNameConstant().equals(ERoleOrder.SHERYF.name())) {
                    Alert sheryfCantChooseHimselfAlert =
                            new Alert(Alert.AlertType.INFORMATION, "Шериф не може голосувати за себе");
                    sheryfCantChooseHimselfAlert.show();
                } else {
                    doSheryfMove(chosenPlayerNumber);
                    setNextRole();
                }
                break;
            case MANIAK:
                if (playerIdRoleMap.get(chosenPlayerNumber).getRoleNameConstant().equals(ERoleOrder.MANIAK.name())) {
                    Alert maniakCantChooseHimselfAlert =
                            new Alert(Alert.AlertType.INFORMATION, "Маніяк не може голосувати за себе");
                    maniakCantChooseHimselfAlert.show();
                }else {
                    doManiakMove(chosenPlayerNumber);
                    setNextRole();
                }
                break;
            case STRILOCHNYK:
                archerAttemptsAmount
                        = gameStatisticsService.getSumOfStrilochnykAttempts(SelectionController.currentGameId);
                System.out.println(archerAttemptsAmount);
                if (strilochnykIndex < archerAttemptsAmount) {
                    System.out.println("DOING STRILOCKNYK");
                    doStrilochnykMove(chosenPlayerNumber);
                    System.out.println("Strilochnyk index " + strilochnykIndex);
                    System.out.println("Attempts " + archerAttemptsAmount);
                    if(strilochnykIndex == archerAttemptsAmount - 1) {
                        setNextRole();
                    } else {
                        strilochnykIndex++;
                    }
                }else if (archerAttemptsAmount == 0){
                    setNextRole();
                }
                break;
        }
    }

    private void doStrilochnykMove(int chosenPlayerNumber) {
        Action strilochnykMoveLogger = gameService.doStrilochnykMove(SelectionController.currentGameId, chosenPlayerNumber);
        // тут додавати logger в чергу?
        actionsQueue.add(strilochnykMoveLogger);
        System.out.println("Doing STRILOCHNYK");
    }

    private void doManiakMove(int chosenPlayerNumber) {
        Action maniakMoveLogger = gameService.doManiakMove(SelectionController.currentGameId, chosenPlayerNumber);
        // тут додавати logger в чергу?
        actionsQueue.add(maniakMoveLogger);
    }

    private void doSheryfMove(int chosenPlayerNumber) {
        Action sheryfMoveLogger = new Action();
        sheryfMoveLogger.setActionText("Шериф обирає гравця "
                                       + chosenPlayerNumber
                                       +  " з роллю '" + playerIdRoleMap.get(chosenPlayerNumber).getTitle() + "'");
        sheryfMoveLogger.setLocalDateTime(LocalDateTime.now());
        Alert alert = new Alert(Alert.AlertType.INFORMATION, sheryfMoveLogger.getActionText());
        // тут додавати logger в чергу?
        alert.show();
        actionsQueue.add(sheryfMoveLogger);
    }

    private void doDoctorMove(int chosenPlayerNumber) {
        Action doctorMoveLogger = gameService.doDoctorMove(SelectionController.currentGameId, chosenPlayerNumber);
        // тут додавати logger в чергу?
        actionsQueue.add(doctorMoveLogger);
    }

    private void doLedyMove(int chosenPlayerNumber) {
        Action ledyMoveLogger = gameService.doLedyMove(SelectionController.currentGameId, chosenPlayerNumber);
        // тут додавати logger в чергу?
        actionsQueue.add(ledyMoveLogger);
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
