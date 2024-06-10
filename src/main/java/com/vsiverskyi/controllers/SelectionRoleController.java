package com.vsiverskyi.controllers;

import com.vsiverskyi.exception.CantStartGameException;
import com.vsiverskyi.exception.ExceptionConstants;
import com.vsiverskyi.exception.NoGameWithSuchIdException;
import com.vsiverskyi.exception.NoRoleWithSuchTitleException;
import com.vsiverskyi.model.GameStatistics;
import com.vsiverskyi.model.Player;
import com.vsiverskyi.model.Role;
import com.vsiverskyi.service.GameService;
import com.vsiverskyi.service.GameStatisticsService;
import com.vsiverskyi.service.RoleService;
import com.vsiverskyi.utils.StyleConstants;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
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
@FxmlView("SelectionRole.fxml")
public class SelectionRoleController implements Initializable {

    @Autowired
    private GameService gameService;
    @Autowired
    private RoleService roleService;
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
    private List<GameStatistics> gameStatisticsList;
    private List<Role> roles;
    private ObservableList<String> assignedRolesList = FXCollections.observableArrayList();
    private Map<Integer, Role> playerIdRoleIdMap = new HashMap<>();
    private Map<Integer, Button> playerButtonsMap = new HashMap<>(); // Map to store buttons
    private Role currentRole;
    private int roleSelectionIndex = 0;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.stage = StarterController.primaryStage;
        scene = new Scene(selectionRoleAP);
        scene.getStylesheets().add(getClass().getResource("/style/style.css").toExternalForm());

        stage.setScene(scene);
        stage.setMaximized(true);
        try {
            // get list of gamers and sort them by their number
            gameStatisticsList = gameStatisticsService
                    .getGameStatisticsByGameIdSortedByInGameNumber(SelectionController.currentGameId);
            //init map keys
            for (GameStatistics gameStatistics : gameStatisticsList) {
                playerIdRoleIdMap.put(gameStatistics.getInGameNumber(), null);
            }
            //init roles
            roles = roleService.getRoleListFromListOfRoleId(GameSettingsController.roleIdPerGameList);

            //show buttons
            displayRolePlayers(gameStatisticsList.size());

            // start selection process
            currentRole = roles.get(roleSelectionIndex++);
            roleTitle.setText(currentRole.getTitle());

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
            alert.show();
        }
    }

    private void startPresentationProcess() {
        // save all roles
        try {
            int c = 0;
            for (Map.Entry<Integer, Role> entry : playerIdRoleIdMap.entrySet()) {
                if (entry.getValue() == null) {
                    c++;
                }
            }
            if(c > 0) {
                new Alert(Alert.AlertType.INFORMATION, "Не всі ролі було розподілено").show();
            } else {
                roleService.applyRoles(SelectionController.currentGameId, playerIdRoleIdMap);
                fxWeaver.loadController(PresentationController.class).show();
            }
        }catch (RuntimeException ex) {
            new Alert(Alert.AlertType.ERROR, ex.getMessage());
        }

    }

    private void displayRolePlayers(int totalPlayers) {
        double centerX = selectionRolePane.getWidth() / 2;
        double centerY = selectionRolePane.getHeight() / 2;
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
            selectionRolePane.getChildren().add(playerPanel);
            Button button = createPlayerButton(x, y, i);
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
            playerIdRoleIdMap.put(playerNumber, currentRole);
            playerButtonsMap.get(playerNumber).setDisable(true);
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
            playerIdRoleIdMap.forEach((playerId, role) -> {
                if (role == null) {
                    // Ми отримуємо відсортований масив, де останній буде мирним
                    // тому записуємо у всі порожні елементи мирного
                    playerIdRoleIdMap.put(playerId, roles.get(roles.size() - 1));
                    playerButtonsMap.get(playerId).setDisable(true);
                    roleSelectionIndex++;
                }
            });
        }
        roleTitle.setText(currentRole.getTitle() + "Ін " + roleSelectionIndex);
        System.out.println(playerIdRoleIdMap);
    }

    private void updatePlayerRole(String oldAssignment, String newPlayerNumber) {
        if (newPlayerNumber.isEmpty()) return;
        String[] parts = oldAssignment.split(": ");
        int oldPlayerNumber = Integer.parseInt(parts[0].split(" ")[1]);
        String roleTitle = parts[1];

        int newPlayerNumberInt = Integer.parseInt(newPlayerNumber);

        // Update the map
        Role previousPlayerRole = playerIdRoleIdMap.get(newPlayerNumberInt);

        playerIdRoleIdMap.remove(oldPlayerNumber);
        Role role = roles
                .stream()
                .filter(roleToFind -> roleToFind.getTitle().equalsIgnoreCase(roleTitle))
                .findFirst()
                .orElseThrow(() ->
                        new NoRoleWithSuchTitleException(ExceptionConstants.NO_ROLE_WITH_SUCH_TITLE + roleTitle));

        playerIdRoleIdMap.put(newPlayerNumberInt, role);
        playerIdRoleIdMap.put(oldPlayerNumber, previousPlayerRole);
        updateAssignedRolesList();
    }

    private void updateAssignedRolesList() {
        assignedRolesList.clear();
        playerIdRoleIdMap.forEach((playerNumber, role) -> {
            if (role != null) {
                assignedRolesList.add("Гравець " + playerNumber + ": " + role.getTitle());
            }
        });
    }

    public void show() {
        stage.show();
    }
}
