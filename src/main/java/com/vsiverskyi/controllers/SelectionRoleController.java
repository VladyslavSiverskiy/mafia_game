package com.vsiverskyi.controllers;

import com.vsiverskyi.exception.ExceptionConstants;
import com.vsiverskyi.exception.NoGameWithSuchIdException;
import com.vsiverskyi.exception.NoRoleWithSuchTitleException;
import com.vsiverskyi.model.GameStatistics;
import com.vsiverskyi.model.Player;
import com.vsiverskyi.model.Role;
import com.vsiverskyi.service.GameService;
import com.vsiverskyi.service.GameStatisticsService;
import com.vsiverskyi.service.RoleService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.*;

import static com.vsiverskyi.utils.StyleConstants.HOVERED_BUTTON_STYLE;
import static com.vsiverskyi.utils.StyleConstants.IDLE_BUTTON_STYLE;

@Component
@FxmlView("SelectionRole.fxml")
public class SelectionRoleController implements Initializable,DisplayedPlayersController {

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
    private Stage stage;
    private Scene scene;
    private Parent root;
    @FXML
    private AnchorPane selectionRoleAP;
    @FXML
    private AnchorPane selectionRolePane;
    @FXML
    private AnchorPane selectedRolesAp;
    @FXML
    private Label roleTitle;
    @FXML
    private Button startVoting;
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
    private List<GameStatistics> gameStatisticsList;
    private List<Role> roles;
    private ObservableList<String> assignedRolesList = FXCollections.observableArrayList();
    private Map<Integer, Role> playerIdRoleMap = new HashMap<>();
    private Map<Integer, Button> playerButtonsMap = new HashMap<>(); // Map to store buttons
    private Map<Integer, Label> playerRoleLabelsMap = new HashMap<>(); // Map to store labels
    private Map<Integer, Integer> yellowCardsMap = new HashMap<>(); // Map to store yellow cards count
//    private Set<Integer> redCardedPlayers = new HashSet<>(); // Set to store players with red cards

    private Role currentRole;
    private int roleSelectionIndex = 0;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.stage = StarterController.primaryStage;
        scene = new Scene(selectionRoleAP);
        scene.getStylesheets().add(getClass().getResource("/style/style.css").toExternalForm());

        stage.setScene(scene);
        stage.setMaximized(true);
        stage.setFullScreen(true);
        fullScreen.setOnAction(ev -> stage.setFullScreen(true));
        try {
            // get list of gamers and sort them by their number
            gameStatisticsList = gameStatisticsService
                    .getGameStatisticsByGameIdSortedByInGameNumber(SelectionController.currentGameId);
            //init map keys
            for (GameStatistics gameStatistics : gameStatisticsList) {
                playerIdRoleMap.put(gameStatistics.getInGameNumber(), null);
            }
            //init roles
            roles = roleService.getRoleListFromListOfRoleId(GameSettingsController.roleIdPerGameList);

            //show buttons
            displayRolePlayers(gameStatisticsList.size());

            // start selection process
            currentRole = roles.get(roleSelectionIndex++);
            roleTitle.setText(currentRole.getTitle());

            // Initialize technical defeat buttons
            technicalDefeatPeaceful.setOnAction(e -> penaltyController.assignTechnicalDefeat("PEACE"));
            technicalDefeatMafia.setOnAction(e -> penaltyController.assignTechnicalDefeat("MAFIA"));

            // Initialize player card list view
            penaltyController.initializePlayerCardList(gameStatisticsList, stage,this, playerCardListView);

            // Create ListView for roles
            ListView<String> roleListView = new ListView<>();
            roleListView.getItems().addAll(roles.stream().map(Role::getTitle).toList());

            // Add listener to ListView to set selectedRole
            roleListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
                currentRole = roles
                        .stream()
                        .filter(role -> role.getTitle().equalsIgnoreCase(newSelection))
                        .findFirst()
                        .orElseThrow(() -> new NoRoleWithSuchTitleException(ExceptionConstants.NO_ROLE_WITH_SUCH_TITLE + newSelection));
                new Alert(Alert.AlertType.INFORMATION, currentRole.getTitle());
            });

            // Create ListView for assigned roles
            ListView<String> assignedRolesListView = new ListView<>(assignedRolesList);
            assignedRolesListView.setCellFactory(param -> new ListCell<>() {
                private final TextField textField = new TextField();
                private final Button updateButton = new Button("Змінити №");
                HBox hbox = new HBox(textField, updateButton);

                {
                    hbox.setSpacing(5);
                    updateButton.setOnAction(event -> updatePlayerRole(getItem(), textField.getText()));
                    updateButton.setStyle(IDLE_BUTTON_STYLE);
                    updateButton.setOnMouseEntered(e -> updateButton.setStyle(HOVERED_BUTTON_STYLE));
                    updateButton.setOnMouseExited(e -> updateButton.setStyle(IDLE_BUTTON_STYLE));
                    setGraphic(hbox);
                }

                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        String[] parts = item.split(": ");
                        setText(parts[0] + ": " + parts[1]);
                        textField.setPromptText("№");
                        textField.setMaxWidth(30);

                        setGraphic(hbox);
                        textField.setText("");
//                        setText(null); // Clear any text setting
                    }
                }
            });

            AnchorPane.setTopAnchor(assignedRolesListView, 0.0);
            AnchorPane.setRightAnchor(assignedRolesListView, 0.0);
            AnchorPane.setBottomAnchor(assignedRolesListView, 0.0);
            AnchorPane.setLeftAnchor(assignedRolesListView, 0.0);

            selectedRolesAp.getChildren().add(assignedRolesListView);
            //end
            startVoting.setOnAction(actionEvent -> startPresentationProcess());
        } catch (NoGameWithSuchIdException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, e.getMessage());
            alert.initOwner(stage);
            alert.show();
        }
    }

    private void startPresentationProcess() {
        // save all roles
        try {
            int c = 0;
            for (Map.Entry<Integer, Role> entry : playerIdRoleMap.entrySet()) {
                if (entry.getValue() == null) {
                    c++;
                }
            }
            if (c > 0) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Не всі ролі було розподілено");
                alert.initOwner(stage);
                alert.show();
            } else {
                roleService.applyRoles(SelectionController.currentGameId, playerIdRoleMap);
                fxWeaver.loadController(PresentationController.class).show();
            }
        } catch (RuntimeException ex) {
            new Alert(Alert.AlertType.ERROR, ex.getMessage());
        }

    }

    @Override
    public void displayRolePlayers(int totalPlayers) {
        // Clear the previous content from the selectionRolePane
        selectionRolePane.getChildren().clear();

        double centerX = selectionRolePane.getWidth() / 2;
        double centerY = selectionRolePane.getHeight() / 2;
        double radius = Math.min(centerX, centerY) - 3;
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
            selectionRolePane.getChildren().add(playerPanel);
            int finalI = i;
            button.setOnAction(event -> assignRoleToPlayer(finalI));
            if (i == 0 || i == totalPlayers + 1) {
                playerPanel.setVisible(false);
                button.setVisible(false);
            }

            playerButtonsMap.put(i, button);
            selectionRolePane.getChildren().add(button);
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

    private void assignRoleToPlayer(int playerNumber) {
        if (currentRole != null) {
            playerIdRoleMap.put(playerNumber, currentRole);
            playerButtonsMap.get(playerNumber).setDisable(true);
            playerRoleLabelsMap.get(playerNumber).setText(currentRole.getTitle()); // Update label with role
            showNextRole();
            updateAssignedRolesList();
        }
    }

    /**
     * Take number of current gamer (let's say from 1 to 10)
     * then get such element from list of roles
     */
    private void showNextRole() {
        currentRole = roles.get(roleSelectionIndex++);
        // Роздати всім іншим ролі мирного;
        if (currentRole.getTitle().equals("Мирний")) {
            playerIdRoleMap.forEach((playerId, role) -> {
                if (role == null) {
                    // Ми отримуємо відсортований масив, де останній буде мирним
                    // тому записуємо у всі порожні елементи мирного
                    Role peace = roles.get(roles.size() - 1);
                    playerIdRoleMap.put(playerId, peace);
                    playerRoleLabelsMap.get(playerId).setText("" + peace.getTitle()); // Update new player's label

                    playerButtonsMap.get(playerId).setDisable(true);
                    roleSelectionIndex++;
                }
            });
        }
        roleTitle.setText(currentRole.getTitle());
    }

    private void updatePlayerRole(String oldAssignment, String newPlayerNumber) {
        if (newPlayerNumber.isEmpty()) return;
        String[] parts = oldAssignment.split(": ");
        int oldPlayerNumber = Integer.parseInt(parts[0].split(" ")[1]);
        String roleTitle = parts[1];

        int newPlayerNumberInt = Integer.parseInt(newPlayerNumber);

        // Update the map
        Role previousPlayerRole = playerIdRoleMap.get(newPlayerNumberInt);

        playerIdRoleMap.remove(oldPlayerNumber);
        Role role = roles
                .stream()
                .filter(roleToFind -> roleToFind.getTitle().equalsIgnoreCase(roleTitle))
                .findFirst()
                .orElseThrow(() ->
                        new NoRoleWithSuchTitleException(ExceptionConstants.NO_ROLE_WITH_SUCH_TITLE + roleTitle));

        playerIdRoleMap.put(newPlayerNumberInt, role);
        playerIdRoleMap.put(oldPlayerNumber, previousPlayerRole);

        playerRoleLabelsMap.get(newPlayerNumberInt).setText("" + role.getTitle()); // Update new player's label
        if (previousPlayerRole != null) {
            playerRoleLabelsMap.get(oldPlayerNumber).setText("" + previousPlayerRole.getTitle()); // Update old player's label
        } else {
            playerRoleLabelsMap.get(oldPlayerNumber).setText(""); // Clear old player's label
        }

        updateAssignedRolesList();
    }

    private void updateAssignedRolesList() {
        assignedRolesList.clear();
        playerIdRoleMap.forEach((playerNumber, role) -> {
            if (role != null) {
                assignedRolesList.add("Гравець " + playerNumber + ": " + role.getTitle());
            }
        });
    }

    public void show() {
        stage.show();
    }
}
