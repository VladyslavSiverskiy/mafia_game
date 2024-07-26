package com.vsiverskyi.controllers;

import com.vsiverskyi.exception.NoGameWithSuchIdException;
import com.vsiverskyi.model.GameStatistics;
import com.vsiverskyi.model.Nickname;
import com.vsiverskyi.model.enums.ERoleOrder;
import com.vsiverskyi.repository.NicknameRepository;
import com.vsiverskyi.service.GameService;
import com.vsiverskyi.service.GameStatisticsService;
import com.vsiverskyi.utils.StyleConstants;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
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
    private NicknameRepository nicknameRepository;
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
    private AnchorPane selectionAP;
    @FXML
    private AnchorPane selectionPane;
    @FXML
    private Label roleName;
    @FXML
    private Button startVoting;
    @FXML
    private Button fullScreen;
    private List<GameStatistics> gameStatisticsList;
    private int currentPlayerIndex;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.stage = StarterController.primaryStage;
        stage.setScene(new Scene(selectionAP));
        stage.setMaximized(true);
        stage.setFullScreen(true);
        fullScreen.setOnAction(ev -> stage.setFullScreen(true));
        ImageView imageView = new ImageView(getClass().getResource("/images/fullscreen.png").toExternalForm());
        fullScreen.setGraphic(imageView);
        imageView.fitWidthProperty().bind(fullScreen.widthProperty().divide(10));
        imageView.setPreserveRatio(true);

        try {
            gameStatisticsList = gameStatisticsService.getGameStatisticsByGameId(currentGameId);
        }catch (NoGameWithSuchIdException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, e.getMessage());
            alert.initOwner(stage);
            alert.show();
        }
        int totalPlayers = gameStatisticsList.size();
        displayPlayers(totalPlayers);
        startVoting.setOnAction(actionEvent -> fxWeaver.loadController(SelectionRoleController.class).show());
    }

    public void show() {
        stage.show();
    }

    public void displayPlayers(int totalPlayers) {
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
//            Circle avatar = new Circle(18, Color.LIGHTGRAY); // Example avatar
//            playerPanel.getChildren().add(avatar);
            // Selection of nickname from a list (You may replace this with a ComboBox)
            ComboBox<String> nicknameComboBox = new ComboBox<>();
            // Add nicknames to the ComboBox

            List<Nickname> nicknames = nicknameRepository.findAll();
            nicknameComboBox.getItems().addAll(nicknames.stream().map(nickname -> nickname.getNickname().toUpperCase()).toList()); // Example nicknames
            nicknameComboBox.setTooltip(new Tooltip());
            nicknameComboBox.getSelectionModel().isEmpty(); // Select the first nickname by default
            //TODO: можливо дописати умову (якщо Player != null) тоді брати його nickname
//            nicknameComboBox.getSelectionModel().select(); // Select the first nickname by default
            nicknameComboBox.setStyle("-fx-font-size: 12px;");
            new ComboBoxAutoComplete<>(nicknameComboBox, x, y);
            int finalI = i;
            nicknameComboBox.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
                if (finalI > 0 && finalI < totalPlayers + 1) {
                    gameStatisticsService.setInGameNickname(currentGameId, finalI, newValue);
                }
            });
            playerPanel.getChildren().add(nicknameComboBox);
            // Add the player panel to the selectionPane
            selectionPane.getChildren().add(playerPanel);

            Button button = new Button(String.valueOf(i));
            button.setLayoutX(x - 70);
            button.setLayoutY(y - 50);
            button.setStyle(StyleConstants.IDLE_BUTTON_STYLE);

            if (i == 0 || i == totalPlayers + 1) {
                playerPanel.setVisible(false);
                button.setVisible(false);
            }
            selectionPane.getChildren().add(button);
        }
    }


}
