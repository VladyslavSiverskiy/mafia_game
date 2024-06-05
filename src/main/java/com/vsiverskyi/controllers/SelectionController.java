package com.vsiverskyi.controllers;

import com.vsiverskyi.exception.NoGameWithSuchIdException;
import com.vsiverskyi.model.GameStatistics;
import com.vsiverskyi.model.enums.ERoleOrder;
import com.vsiverskyi.service.GameService;
import com.vsiverskyi.service.GameStatisticsService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

@Component
@FxmlView("Selection.fxml")
public class SelectionController implements Initializable {

    @Autowired
    private GameService gameService;
    @Autowired
    private GameStatisticsService gameStatisticsService;
    @Autowired
    private FxWeaver fxWeaver;
    public static Long currentGameId;
    private Stage stage;
    private Scene scene;
    private Parent root;

    @FXML
    private GridPane selectionGrid;
    @FXML
    private AnchorPane selectionAP;
    @FXML
    private AnchorPane selectionPane;
    @FXML
    private Label roleName;
    @FXML
    private Button startVoting;
    private List<GameStatistics> gameStatisticsList;
    private int currentPlayerIndex;
    private final static Integer NUM_BUTTON_LINES = 5;
    private final static Integer BUTTONS_PER_LINE = 5;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.stage = StarterController.primaryStage;
        stage.setScene(new Scene(selectionAP));
        stage.setMaximized(true);

        try {
            gameStatisticsList = gameStatisticsService.getGameStatisticsByGameId(currentGameId);
        }catch (NoGameWithSuchIdException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, e.getMessage());
            alert.show();
        }
        System.out.println(gameStatisticsList);

        roleName.setText(gameStatisticsList.get(0).getRole().getTitle());
        int totalPlayers = gameStatisticsList.size();

        double centerX = selectionPane.getWidth() / 2;
        double centerY = selectionPane.getHeight() / 2;
        double radius = Math.min(centerX, centerY) - 5;
        double startAngle = Math.PI / 1.8 ;

        for (int i = 0; i < totalPlayers + 2; i++) { //
            double angle = startAngle + 2 * Math.PI * i / (totalPlayers + 2);
            double x = centerX + radius * Math.cos(angle);
            double y = centerY + radius * Math.sin(angle);
            // Create a panel to represent each player
            VBox playerPanel = new VBox();
            playerPanel.setAlignment(Pos.CENTER);
            playerPanel.setLayoutX(x - 50); // Offset to center panel
            playerPanel.setLayoutY(y - 50); // Offset to center panel
            playerPanel.setSpacing(5); // Adjust spacing as needed

            // Avatar (You may replace this with an ImageView)
            Circle avatar = new Circle(18, Color.LIGHTGRAY); // Example avatar
            playerPanel.getChildren().add(avatar);

            // Selection of nickname from a list (You may replace this with a ComboBox)
            ComboBox<String> nicknameComboBox = new ComboBox<>();
            // Add nicknames to the ComboBox
            nicknameComboBox.getItems().addAll("Nickname 1", "Nickname 2", "Nickname 3"); // Example nicknames
            nicknameComboBox.getSelectionModel().selectFirst(); // Select the first nickname by default
            nicknameComboBox.setStyle("-fx-font-size: 12px;");
            nicknameComboBox.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
                System.out.println(newValue);
                gameStatisticsService.setInGameNickname();
            });
            playerPanel.getChildren().add(nicknameComboBox);

            // Add the player panel to the selectionPane
            selectionPane.getChildren().add(playerPanel);


            Button button = new Button(String.valueOf(i));
            button.setLayoutX(x - 25);
            button.setLayoutY(y - 25);
            button.setOnAction(event -> handleButtonClick());
            if (i == 0 || i == totalPlayers + 1) {
                playerPanel.setVisible(false);
                button.setVisible(false);
            }
            selectionPane.getChildren().add(button);
        }
        startVoting.setOnAction(actionEvent -> fxWeaver.loadController(PresentationController.class).show());
    }

    private void handleButtonClick() {
        roleName.setText(gameStatisticsList.get(++currentPlayerIndex).getRole().getTitle());

    }

    public void show() {
        stage.show();
    }
}
