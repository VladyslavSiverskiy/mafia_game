package com.vsiverskyi.controllers;

import com.vsiverskyi.model.GameStatistics;
import com.vsiverskyi.model.Player;
import com.vsiverskyi.model.Role;
import com.vsiverskyi.model.enums.ERoleOrder;
import com.vsiverskyi.repository.RoleRepository;
import com.vsiverskyi.service.GameService;
import com.vsiverskyi.service.GameStatisticsService;
import com.vsiverskyi.service.PointsService;
import com.vsiverskyi.service.RoleService;
import com.vsiverskyi.utils.Action;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.vsiverskyi.utils.StyleConstants.*;

@Component
@RequiredArgsConstructor
@FxmlView("NightStage.fxml")
public class NightStageController implements Initializable, DisplayedPlayersController {
    private Stage stage;
    private Scene scene;
    private Parent root;
    @Autowired
    private ViewController viewController;
    @Autowired
    private GameService gameService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private PointsService pointsService;
    @Autowired
    private PenaltyController penaltyController;
    @Autowired
    private GameStatisticsService gameStatisticsService;
    @Autowired
    private RoleRepository roleRepository;
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
    @FXML
    private ListView<String> allRolesPerGameList;
    private List<GameStatistics> gameStatisticsListSortedByRoleOrder;
    private List<GameStatistics> gameStatisticsListSortedByInGameNumber;
    private List<Role> actualInGameRoles; //here should be converted set -> list
    private Map<Integer, Role> playerIdRoleMap;
    private Map<Integer, Button> playerButtonsMap; // Map to store buttons
    private Map<Integer, Button> selectedByBombPlayerButtonsMap; // Map to store buttons
    private Map<Integer, Label> playerRoleLabelsMap; // Map to store labels
    private Queue<Action> actionsQueue = new LinkedList<>();
    private Role currentRole;
    private int currentRoleIndex;
    private int selectedToKillPlayerNumber;
    /**
     * Sum of all game statistics` 'timesWasKilled' fields of current game
     */
    private int archerAttemptsAmount;
    private int strilochnykIndex;
    private int chosenByBombPlayerAmount;
    private int currentNightIndicator;
    private int availableAmountOfPlayersForMarking;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.stage = StarterController.primaryStage;
        scene = new Scene(nightStageAp);
        stage.setScene(scene);
        scene.getStylesheets().add(getClass().getResource("/style/style.css").toExternalForm());
        stage.setMaximized(true);
        stage.setFullScreen(true);
        stage.getIcons().add(new Image("/images/title.jpg"));
        stage.setTitle("STOP КОРУПЦІЯ");

        currentNightIndicator = gameService.increaseNightIndicator(SelectionController.currentGameId)
                .getCurrentNightIndicator();

        currentRoleIndex = 0;
        archerAttemptsAmount = 0;
        strilochnykIndex = 0;
        chosenByBombPlayerAmount = gameStatisticsService
                .getMarkedByBombAlivePlayersInGameNumbers(SelectionController.currentGameId).size();

        actualInGameRoles = new ArrayList<>();
        playerIdRoleMap = new HashMap<>();
        playerButtonsMap = new HashMap<>();
        playerRoleLabelsMap = new HashMap<>();
        selectedByBombPlayerButtonsMap = new HashMap<>();
        gameService.resetStrilochnykAttempts(SelectionController.currentGameId);
        updatePlayersList();
        initPlayerRoleMap();

        gameStatisticsService.removeAllVotingSkipsPerDay(SelectionController.currentGameId);
        gameStatisticsService.removeAllVotingDefencesPerDay(SelectionController.currentGameId);
        gameStatisticsService.removeAllKradiyChoices(SelectionController.currentGameId);

        // Initialize player card list view
        technicalDefeatPeaceful.setOnAction(e -> penaltyController.assignTechnicalDefeat("PEACE"));
        technicalDefeatMafia.setOnAction(e -> penaltyController.assignTechnicalDefeat("MAFIA"));
        penaltyController.initializePlayerCardList(gameStatisticsListSortedByInGameNumber, stage, this, playerCardListView);
        fullScreen.setOnAction(ev -> stage.setFullScreen(true));
        ImageView imageView = new ImageView(getClass().getResource("/images/fullscreen.png").toExternalForm());
        fullScreen.setGraphic(imageView);
        imageView.fitWidthProperty().bind(fullScreen.widthProperty().divide(10));
        imageView.setPreserveRatio(true);

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
        updateAllRolesList();
    }

    private void setNextRole() {
//        updatePlayersList();
        updateAllRolesList();
        currentRoleIndex++;
        if (currentRoleIndex == actualInGameRoles.size()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, queueToString(actionsQueue));
            alert.initOwner(stage);
            alert.showAndWait();
            List<GameStatistics> killedDueToPoisoning = gameStatisticsService.processPoisonedPlayers(SelectionController.currentGameId);
            if (killedDueToPoisoning.size() > 0) {
                String killedString = " Через отруєння загинули: ";
                for (GameStatistics killedDueToPoisoningPlayer : killedDueToPoisoning) {
                    killedString += killedDueToPoisoningPlayer.getInGameNumber() + " ";
                }
                Alert killedAlert = new Alert(Alert.AlertType.INFORMATION, killedString);
                killedAlert.initOwner(stage);
                killedAlert.showAndWait();
            }
            pointsService.countOnePointAfterDayAndNight(SelectionController.currentGameId);
            if (gameService.checkIfGameIsOver(SelectionController.currentGameId)) {
                fxWeaver.loadController(GameEndingController.class).show();
            } else {
                fxWeaver.loadController(VotingController.class).show();
            }
        } else if (currentRoleIndex < actualInGameRoles.size()) {
            currentRole = actualInGameRoles.get(currentRoleIndex);
            if (!checkIfAliveOrDontHaveRedCardsWithRole(currentRole, SelectionController.currentGameId)) {
                setNextRole();
            }

            currentRoleTitle.setText(currentRole.getTitle());
            if (currentRole.getRoleNameConstant().equalsIgnoreCase(ERoleOrder.STRILOCHNYK.name())) {
                archerAttemptsAmount
                        = gameStatisticsService.getSumOfStrilochnykAttempts(SelectionController.currentGameId);
                if (archerAttemptsAmount == 0) {
                    setNextRole();
                }
            }
            if (currentNightIndicator == 2 && currentRole.getRoleNameConstant().equalsIgnoreCase(ERoleOrder.BOMBA.name())) {
                List<Integer> markedInGameNumbers =
                        gameStatisticsService
                                .getMarkedByBombAlivePlayersInGameNumbers(SelectionController.currentGameId);
                for (Integer markedInGameNumber : markedInGameNumbers) {
                    Button buttonToSelect = playerButtonsMap.get(markedInGameNumber);
                    buttonToSelect.setStyle(IDLE_BUTTON_STYLE_RED);
                    buttonToSelect.setOnMouseEntered(e -> buttonToSelect.setStyle(HOVERED_BUTTON_STYLE_RED));
                    buttonToSelect.setOnMouseExited(e -> buttonToSelect.setStyle(IDLE_BUTTON_STYLE_RED));
                    buttonToSelect.setOnMouseClicked(ev -> {
                        selectedByBombPlayerButtonsMap.remove(markedInGameNumber);
                        buttonToSelect.setStyle(IDLE_BUTTON_STYLE);
                        buttonToSelect.setOnMouseEntered(e -> buttonToSelect.setStyle(HOVERED_BUTTON_STYLE));
                        buttonToSelect.setOnMouseExited(e -> buttonToSelect.setStyle(IDLE_BUTTON_STYLE));
                        chosenByBombPlayerAmount--;
                        availableAmountOfPlayersForMarking++;
                        buttonToSelect.setOnAction(actionEvent1 -> handlePlayerAction(markedInGameNumber));
                    });
                    selectedByBombPlayerButtonsMap.put(markedInGameNumber, buttonToSelect);
                }
            }
            if (currentRole.getRoleNameConstant().equalsIgnoreCase(ERoleOrder.PEACE.name())
                || currentRole.getRoleNameConstant().equalsIgnoreCase(ERoleOrder.PEREVERTEN_PEACE.name())) {
                setNextRole();
            }
        }
    }

    private boolean checkIfAliveOrDontHaveRedCardsWithRole(Role currentRole, long currentGameId) {
        return gameStatisticsService.checkIfAliveOrDontHaveRedCardsWithRole(currentRole, currentGameId);
    }

    private void updateAllRolesList() {
        if (currentRole != null) {
            List<String> roleTitles = gameStatisticsListSortedByRoleOrder.stream()
                    .map(gameStatistics -> gameStatistics.getRole().getTitle()).distinct().collect(Collectors.toList());
            allRolesPerGameList.getItems().clear(); // Clear existing items

            allRolesPerGameList.getItems().clear(); // Clear existing items
            allRolesPerGameList.getItems().addAll(roleTitles);

            // Set a custom cell factory to style cells based on the role title
            allRolesPerGameList.setCellFactory(lv -> new ListCell<String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        // Apply style if the title matches the current selected role title
                        if (item.equals(currentRole.getTitle())) {
                            setStyle("-fx-background-color: #bd1c1c; -fx-text-fill: white;");
                        } else {
                            setStyle("");
                        }
                    }
                }
            });
        }
    }

    private void updatePlayersList() {
        gameStatisticsListSortedByRoleOrder = gameStatisticsService
                .getGameStatisticsByGameId(SelectionController.currentGameId);
        gameStatisticsListSortedByInGameNumber = gameStatisticsService
                .getGameStatisticsByGameIdSortedByInGameNumber(SelectionController.currentGameId);
        actualInGameRoles = gameStatisticsListSortedByRoleOrder.stream()
                .filter(GameStatistics::isInGame)
                .map(gameStat -> {
                    if (gameStat.getRole().getTitle().equals(ERoleOrder.DON.getTitle())) {
                        return roleRepository.findByRoleNameConstant(ERoleOrder.MAFIA.name());
                    } else {
                        return gameStat.getRole();
                    }
                })
                .distinct()
                .toList();
        updateAllRolesList();
    }

    // Implement this method to find the Role object by its title
    private Role getRoleByTitle(String title) {
        for (GameStatistics gameStatistics : gameStatisticsListSortedByRoleOrder) {
            if (gameStatistics.getRole().getTitle().equals(title)) {
                return gameStatistics.getRole();
            }
        }
        return null;
    }

    private void handlePlayerAction(int chosenPlayerNumber) {
        switch (ERoleOrder.valueOf(currentRole.getRoleNameConstant())) {
            case MAFIA:
                if (playerIdRoleMap.get(chosenPlayerNumber).getRoleNameConstant().equals(ERoleOrder.MAFIA.name())) {
                    Alert mafiaCantChooseHimselfAlert =
                            new Alert(Alert.AlertType.INFORMATION, ERoleOrder.MAFIA.getTitle() + " не може проголосувати за себе");
                    mafiaCantChooseHimselfAlert.initOwner(stage);
                    mafiaCantChooseHimselfAlert.show();
                } else {
                    Action mafiaMoveLogger = null;
                    // якщо ні, то мафія вибирає кого вбити
                    selectedToKillPlayerNumber = chosenPlayerNumber;
                    mafiaMoveLogger = gameService.doMafiaKillMove(SelectionController.currentGameId, chosenPlayerNumber);
//                    Alert alert = new Alert(Alert.AlertType.INFORMATION, mafiaMoveLogger.getActionText());
//                    alert.initOwner(stage);
//                    alert.show();

                    pointsService.countPointsInOrderToNightAction(
                            SelectionController.currentGameId,
                            currentRole,
                            chosenPlayerNumber
                    );
                    setNextRole();
                    actionsQueue.add(mafiaMoveLogger);
                }
                break;
            // тут додавати logger в чергу?
            case PEREVERTEN_PEACE:
                setNextRole();
                break;
            case PEREVERTEN_MAFIA:
                if (playerIdRoleMap.get(chosenPlayerNumber).getRoleNameConstant().equals(ERoleOrder.PEREVERTEN_MAFIA.name())) {
                    Alert donCantChooseHimselfAlert =
                            new Alert(Alert.AlertType.INFORMATION, ERoleOrder.PEREVERTEN_MAFIA.getTitle() + " не може проголосувати за себе");
                    donCantChooseHimselfAlert.initOwner(stage);
                    donCantChooseHimselfAlert.showAndWait();
                } else {
                    Action perevertenMoveLogger = gameService.doMafiaKillMove(SelectionController.currentGameId, chosenPlayerNumber);
                    pointsService.countPointsInOrderToNightAction(
                            SelectionController.currentGameId,
                            currentRole,
                            chosenPlayerNumber
                    );
                    actionsQueue.add(perevertenMoveLogger);
//                    Alert alert = new Alert(Alert.AlertType.INFORMATION, perevertenMoveLogger.getActionText());
//                    alert.initOwner(stage);
//                    alert.showAndWait();
                    setNextRole();
                }
                break;
            case DOCTOR:
                if (playerIdRoleMap.get(chosenPlayerNumber).getRoleNameConstant().equals(ERoleOrder.DOCTOR.name())
                    && (findByInGameNumber(chosenPlayerNumber).getTimesWasHealed() >= 2)) {
                    Alert doctorCantHealHimselfAlert =
                            new Alert(Alert.AlertType.INFORMATION, "Лікар не може лікувати себе більше двох разів");
                    doctorCantHealHimselfAlert.initOwner(stage);
                    doctorCantHealHimselfAlert.showAndWait();
                } else if (gameStatisticsService.checkIfPlayerWasHealed(chosenPlayerNumber, SelectionController.currentGameId)) {
                    Alert doctorCantHealHimselfAlert =
                            new Alert(Alert.AlertType.INFORMATION, "Лікар не може лікувати два рази підряд");
                    doctorCantHealHimselfAlert.initOwner(stage);
                    doctorCantHealHimselfAlert.showAndWait();
                }else {
                    // Метод - нарахувати поінти за хід вночі.
                    // Передамо поточну роль, і всі з такою роллю отримають стільки то балів
                    doDoctorMove(chosenPlayerNumber);
                    pointsService.countPointsInOrderToNightAction(
                            SelectionController.currentGameId,
                            currentRole,
                            chosenPlayerNumber
                    );
                    setNextRole();
                }
                break;
            case SHERYF:
                if (playerIdRoleMap.get(chosenPlayerNumber).getRoleNameConstant().equals(ERoleOrder.SHERYF.name())) {
                    Alert sheryfCantChooseHimselfAlert =
                            new Alert(Alert.AlertType.INFORMATION, ERoleOrder.SHERYF.getTitle() + " не може голосувати за себе");
                    sheryfCantChooseHimselfAlert.initOwner(stage);
                    sheryfCantChooseHimselfAlert.showAndWait();
                } else {
                    doSheryfMove(chosenPlayerNumber);
                    pointsService.countPointsInOrderToNightAction(
                            SelectionController.currentGameId,
                            currentRole,
                            chosenPlayerNumber
                    );
                    setNextRole();
                }
                break;
            case MANIAK:
                if (playerIdRoleMap.get(chosenPlayerNumber).getRoleNameConstant().equals(ERoleOrder.MANIAK.name())) {
                    Alert maniakCantChooseHimselfAlert =
                            new Alert(Alert.AlertType.INFORMATION, ERoleOrder.MANIAK.getTitle() + " не може голосувати за себе");
                    maniakCantChooseHimselfAlert.initOwner(stage);
                    maniakCantChooseHimselfAlert.showAndWait();
                } else {
                    doManiakMove(chosenPlayerNumber);
                    pointsService.countPointsInOrderToNightAction(
                            SelectionController.currentGameId,
                            currentRole,
                            chosenPlayerNumber
                    );
                    setNextRole();
                }
                break;
            case STRILOCHNYK:
                if (strilochnykIndex < archerAttemptsAmount) {
                    doStrilochnykMove(chosenPlayerNumber);
                    pointsService.countPointsInOrderToNightAction(
                            SelectionController.currentGameId,
                            currentRole,
                            chosenPlayerNumber
                    );
                    if (strilochnykIndex == archerAttemptsAmount - 1) {
                        setNextRole();
                    } else {
                        strilochnykIndex++;
                    }
                }
                break;
            case OTAMAN:
                pointsService.countPointsInOrderToNightAction(
                        SelectionController.currentGameId,
                        currentRole,
                        chosenPlayerNumber
                );
                doOtamanMove(chosenPlayerNumber);
                setNextRole();
                break;
            case LEDY:
                if (playerIdRoleMap.get(chosenPlayerNumber).getRoleNameConstant().equals(ERoleOrder.LEDY.name())) {
                    Alert ladyCantChooseHerselfAlert =
                            new Alert(Alert.AlertType.INFORMATION, "Леді не може голосувати за себе");
                    ladyCantChooseHerselfAlert.initOwner(stage);
                    ladyCantChooseHerselfAlert.showAndWait();
                } else {
                    doLedyMove(chosenPlayerNumber);
                    pointsService.countPointsInOrderToNightAction(
                            SelectionController.currentGameId,
                            currentRole,
                            chosenPlayerNumber
                    );
                    setNextRole();
                }
                break;
            case ZATYCHKA:
                pointsService.countPointsInOrderToNightAction(
                        SelectionController.currentGameId,
                        currentRole,
                        chosenPlayerNumber
                );
                doZatychkaMove(chosenPlayerNumber);
                Action zatychkaMoveLogger = new Action();
                zatychkaMoveLogger.setActionText(ERoleOrder.ZATYCHKA.getTitle() + " голосує в гравця " + chosenPlayerNumber);
                zatychkaMoveLogger.setLocalDateTime(LocalDateTime.now());
                // тут додавати logger в чергу?
                actionsQueue.add(zatychkaMoveLogger);
                setNextRole();
                break;
            case ZATYCHKA_SUDDYA:
                pointsService.countPointsInOrderToNightAction(
                        SelectionController.currentGameId,
                        currentRole,
                        chosenPlayerNumber
                );
                doZatychkaMove(chosenPlayerNumber);
                setNextRole();
                Action zatychkaSuddyaMoveLogger = new Action();
                zatychkaSuddyaMoveLogger.setActionText(ERoleOrder.ZATYCHKA_SUDDYA.getTitle() + " голосує в гравця " + chosenPlayerNumber);
                zatychkaSuddyaMoveLogger.setLocalDateTime(LocalDateTime.now());
                // тут додавати logger в чергу?
                actionsQueue.add(zatychkaSuddyaMoveLogger);
                break;
            case BOMBA:
                if (currentNightIndicator == 1) {
                    if (chosenByBombPlayerAmount < 5) {
                        Button button = playerButtonsMap.get(chosenPlayerNumber);
                        selectedByBombPlayerButtonsMap.put(chosenPlayerNumber, button);
                        button.setStyle(IDLE_BUTTON_STYLE_RED);
                        button.setOnMouseEntered(e -> button.setStyle(HOVERED_BUTTON_STYLE_RED));
                        button.setOnMouseExited(e -> button.setStyle(IDLE_BUTTON_STYLE_RED));
                        button.setOnAction(actionEvent -> {
                            if (currentRole.getTitle().equals(ERoleOrder.BOMBA.getTitle())) {
                                selectedByBombPlayerButtonsMap.remove(chosenPlayerNumber);
                                button.setStyle(IDLE_BUTTON_STYLE);
                                button.setOnMouseEntered(e -> button.setStyle(HOVERED_BUTTON_STYLE));
                                button.setOnMouseExited(e -> button.setStyle(IDLE_BUTTON_STYLE));
                                chosenByBombPlayerAmount--;
                                button.setOnAction(actionEvent1 -> handlePlayerAction(chosenPlayerNumber));
                            }
                        });
                        chosenByBombPlayerAmount++;
                        if (chosenByBombPlayerAmount == 5) {
                            endBombMove();
                        }
                    } else {
                        endBombMove();
                    }
                } else if (currentNightIndicator == 2) {
                    if (selectedByBombPlayerButtonsMap.containsKey(chosenPlayerNumber)) {
                        System.out.println("TRUE");
                    } else {
                        if (chosenByBombPlayerAmount < 5) {
                            Button button = playerButtonsMap.get(chosenPlayerNumber);
                            selectedByBombPlayerButtonsMap.put(chosenPlayerNumber, button);
                            button.setStyle(IDLE_BUTTON_STYLE_RED);
                            button.setOnMouseEntered(e -> button.setStyle(HOVERED_BUTTON_STYLE_RED));
                            button.setOnMouseExited(e -> button.setStyle(IDLE_BUTTON_STYLE_RED));
                            button.setOnAction(actionEvent -> {
                                if (currentRole.getTitle().equals(ERoleOrder.BOMBA.getTitle())) {
                                    selectedByBombPlayerButtonsMap.remove(chosenPlayerNumber);
                                    button.setStyle(IDLE_BUTTON_STYLE);
                                    button.setOnMouseEntered(e -> button.setStyle(HOVERED_BUTTON_STYLE));
                                    button.setOnMouseExited(e -> button.setStyle(IDLE_BUTTON_STYLE));
                                    chosenByBombPlayerAmount--;
                                    availableAmountOfPlayersForMarking++;
                                    button.setOnAction(actionEvent1 -> handlePlayerAction(chosenPlayerNumber));
                                }
                            });
                            chosenByBombPlayerAmount++;
                            if (chosenByBombPlayerAmount == 5) {
                                endBombMove();
                            }
                        } else {
                            endBombMove();
                        }
                    }

                } else {
                    setNextRole();
                }
                break;
            case KRADIY:
                pointsService.countPointsInOrderToNightAction(
                        SelectionController.currentGameId,
                        currentRole,
                        chosenPlayerNumber
                );
                doKradiyMove(chosenPlayerNumber);
                setNextRole();
                break;
        }
    }

    private void doKradiyMove(int chosenPlayerNumber) {
        Action action = gameStatisticsService.markPlayerByKradiy(chosenPlayerNumber, SelectionController.currentGameId);
        actionsQueue.add(action);
    }

    private void endBombMove() {
        Action action = gameStatisticsService.markPlayersByBomb(selectedByBombPlayerButtonsMap.keySet(), SelectionController.currentGameId);
        for (Integer number : selectedByBombPlayerButtonsMap.keySet()) {
            pointsService.countPointsInOrderToNightAction(
                    SelectionController.currentGameId,
                    currentRole,
                    number
            );
        }
        actionsQueue.add(action);
        // for all chosen buttons set default handler
        setNextRole();
    }

    private void doZatychkaMove(int chosenPlayerNumber) {
         gameService.doZatychkaMove(SelectionController.currentGameId, chosenPlayerNumber);

    }

    private void doOtamanMove(int chosenPlayerNumber) {
        Action otamanMoveLogger = gameService.doOtamanMove(SelectionController.currentGameId, chosenPlayerNumber);
        // тут додавати logger в чергу?
        actionsQueue.add(otamanMoveLogger);
    }

    private void doStrilochnykMove(int chosenPlayerNumber) {
        Action strilochnykMoveLogger = gameService.doStrilochnykMove(SelectionController.currentGameId, chosenPlayerNumber);
        // тут додавати logger в чергу?
        actionsQueue.add(strilochnykMoveLogger);
    }

    private void doManiakMove(int chosenPlayerNumber) {
        Action maniakMoveLogger = gameService.doManiakMove(SelectionController.currentGameId, chosenPlayerNumber);
        // тут додавати logger в чергу?
//        Alert alert = new Alert(Alert.AlertType.INFORMATION, maniakMoveLogger.getActionText());
//        // тут додавати logger в чергу?
//        alert.initOwner(stage);
//        alert.showAndWait();
        actionsQueue.add(maniakMoveLogger);
    }

    private void doSheryfMove(int chosenPlayerNumber) {
        Action sheryfMoveLogger = new Action();
        sheryfMoveLogger.setActionText(ERoleOrder.SHERYF.getTitle() + " обирає гравця "
                                       + chosenPlayerNumber
                                       + " з роллю '" + playerIdRoleMap.get(chosenPlayerNumber).getTitle() + "'");
        sheryfMoveLogger.setLocalDateTime(LocalDateTime.now());
//        Alert alert = new Alert(Alert.AlertType.INFORMATION, sheryfMoveLogger.getActionText());
        // тут додавати logger в чергу?
//        alert.initOwner(stage);
//        alert.showAndWait();
        actionsQueue.add(sheryfMoveLogger);
    }

    private void doDoctorMove(int chosenPlayerNumber) {
        Action doctorMoveLogger = gameService.doDoctorMove(SelectionController.currentGameId, chosenPlayerNumber);
        // тут додавати logger в чергу?
//        Alert alert = new Alert(Alert.AlertType.INFORMATION, doctorMoveLogger.getActionText());
//        alert.initOwner(stage);
//        alert.showAndWait();
        actionsQueue.add(doctorMoveLogger);
    }

    private void doLedyMove(int chosenPlayerNumber) {
        Action ledyMoveLogger = gameService.doLedyMove(SelectionController.currentGameId, chosenPlayerNumber);
        // тут додавати logger в чергу?
//        Alert alert = new Alert(Alert.AlertType.INFORMATION, ledyMoveLogger.getActionText());
//        alert.initOwner(stage);
//        alert.showAndWait();
        actionsQueue.add(ledyMoveLogger);
    }

    @Override
    public void displayRolePlayers(int totalPlayers) {
        nightStagePlayersPane.getChildren().clear();

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
            Circle avatar = new Circle(18); // Example avatar
            if (gameStatistics != null) {
                Image avatarImage = null;
                try {
                    avatarImage = new Image("images/" + gameStatistics.getRole().getRoleNameConstant() + ".jpg");
                } catch (Exception e) {
                    avatarImage = new Image("images/icon.ico");
                }
                // Create an ImagePattern using the loaded image
                ImagePattern imagePattern = new ImagePattern(avatarImage);
                // Set the ImagePattern as the fill for the Circle
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
            roleLabel.setStyle("-fx-text-fill: #f4ff67; -fx-border-radius: 5px; -fx-font-size: 12px;");
            // Create an HBox to hold the nickname label and the role label
            Role role = playerIdRoleMap.get(i);
            if (role != null) {
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
                    gameStatisticsListSortedByInGameNumber.size(),
                    stage,
                    playerCardListView,
                    gameStatisticsListSortedByInGameNumber
            );
            playerPanel.getChildren().add(avatarContainer);

            if (i > 0 && i < totalPlayers + 1) {

                HBox hbox = new HBox();
                hbox.setSpacing(10); // Adjust spacing as needed
                // Set a transparent background for the HBox
                hbox.setStyle("-fx-background-color: rgba(31,31,31,0.5); -fx-border-radius: 5px; ");
                hbox.setPadding(new Insets(0, 0, 0, 10));
                hbox.getChildren().addAll(viewController.createNicknameLabel(i, gameStatisticsListSortedByInGameNumber));
                playerPanel.getChildren().add(hbox);
                playerRoleLabelsMap.put(i, roleLabel);
            }

            Button button = playerButtonsMap.get(i);
            if (button == null) {
                button = createPlayerButton(x, y, i);
            }

            if (!checkIfAlive(i, totalPlayers)) {
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
