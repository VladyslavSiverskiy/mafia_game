package com.vsiverskyi.controllers;

import com.vsiverskyi.model.Game;
import com.vsiverskyi.model.GameStatistics;
import com.vsiverskyi.model.enums.ERoleOrder;
import com.vsiverskyi.model.enums.ETeam;
import com.vsiverskyi.service.GameService;
import com.vsiverskyi.service.GameStatisticsService;
import com.vsiverskyi.service.PointsService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static com.vsiverskyi.utils.StyleConstants.HOVERED_BUTTON_STYLE;
import static com.vsiverskyi.utils.StyleConstants.IDLE_BUTTON_STYLE;

@Component
@FxmlView("GameEnding.fxml")
public class GameEndingController implements Initializable {

    @Autowired
    private ViewController viewController;
    @Autowired
    private GameService gameService;
    @Autowired
    private GameStatisticsService gameStatisticsService;
    @Autowired
    private PointsService pointsService;
    @Autowired
    private FxWeaver fxWeaver;
    private Stage stage;
    private Scene scene;
    private Parent root;
    @FXML
    private AnchorPane endingGameAp;
    @FXML
    private AnchorPane playersLeft;
    @FXML
    private Button toStarterPage;
    @FXML
    private Label winnerTitleLabel;
    @FXML
    private TableView<GameStatistics> gameStatisticsTable;
    @FXML
    private TableColumn<GameStatistics, String> playerNameColumn;
    @FXML
    private TableColumn<GameStatistics, Integer> pointsColumn;
    @FXML
    private TableColumn<GameStatistics, Circle> avatarColumn;
    @FXML
    private HBox podiumBox; // New VBox for the podium

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.stage = StarterController.primaryStage;
        scene = new Scene(endingGameAp);
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.setFullScreen(true);

        // Apply general styles
        endingGameAp.getStyleClass().add("anchor-pane");
        podiumBox.getStyleClass().add("podium-box");

        stage.getIcons().add(new Image("/images/title.jpg"));
        stage.setTitle("STOP КОРУПЦІЯ");
        scene.getStylesheets().add(getClass().getResource("/style/end-page.css").toExternalForm());
        Game game = gameService.getGameInfo(SelectionController.currentGameId);
        pointsService.countPointsAfterGameWasFinished(game.getId());
        List<GameStatistics> gameStatisticsList = gameStatisticsService.getGameStatisticsByGameId(game.getId());
        // Sort the list by points in descending order
        List<GameStatistics> sortedList = gameStatisticsList.stream()
                .sorted(Comparator.comparingInt(GameStatistics::getPoints).reversed())
                .collect(Collectors.toList());
        // Get the top 3 players by points
        List<GameStatistics> top3Players = sortedList.stream().limit(3).collect(Collectors.toList());
        // Get the rest of the players
        List<GameStatistics> restOfPlayers = sortedList.stream().skip(3).collect(Collectors.toList());

        playerNameColumn.setCellValueFactory(new PropertyValueFactory<>("inGameNickname"));
        pointsColumn.setCellValueFactory(new PropertyValueFactory<>("points"));
        avatarColumn.setCellValueFactory(new PropertyValueFactory<>("avatarCircle")); // Assuming you have a method in your model to get Circle
        // Custom cell factory to add "+" sign to points
        pointsColumn.setCellFactory(column -> new TableCell<GameStatistics, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    HBox hbox = new HBox(item);
                    hbox.setStyle("-fx-alignment: CENTER;");
                    setText("+" + item);
                    setTextFill(Color.BLACK); // Set text color to black
                }
            }
        });
        gameStatisticsTable.getStyleClass().add("table-view");
        gameStatisticsTable.setItems(FXCollections.observableArrayList(restOfPlayers));
        avatarColumn.setCellFactory(column -> new TableCell<GameStatistics, Circle>() {
            @Override
            protected void updateItem(Circle item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    HBox hbox = new HBox(item);
                    hbox.setStyle("-fx-alignment: CENTER;");
                    setGraphic(hbox);
                }
            }
        });
        String winners = game.getWinnerSide().getTitle();
        if (winners.equals(ETeam.MAFIA.getTitle())) {
            if (checkIfYanucharMafiaExisted(game)) {
                winners = "Яничар";
            } else {
                winners = "Корупціонери";
            }
        } else {
            winners = "Мирні";
        }
        winnerTitleLabel.setText("Перемогли: " + winners);
        winnerTitleLabel.setStyle("-fx-text-fill: black; -fx-font-size: 24px; -fx-font-weight: bold; -fx-alignment: center;"); // Set text color to black
        toStarterPage.setOnMouseClicked(ev -> {
            StarterController.primaryStage = (Stage) toStarterPage.getScene().getWindow();
            fxWeaver.loadController(StarterController.class).show();
        });

        playersLeft.getChildren().add(viewController.createPlayerStatisticsPanel(true));

        toStarterPage.setStyle(IDLE_BUTTON_STYLE);
        toStarterPage.setOnMouseEntered(ev -> toStarterPage.setStyle(HOVERED_BUTTON_STYLE));
        toStarterPage.setOnMouseExited(ev -> toStarterPage.setStyle(IDLE_BUTTON_STYLE));
        // Create the podium
        createPodium(top3Players);
    }

    private boolean checkIfYanucharMafiaExisted(Game game) {
        List<GameStatistics> gameStatisticsList = game.getGameStatistics();
        for (GameStatistics gameStatistics: gameStatisticsList) {
            if (gameStatistics.getRole().getRoleNameConstant().equals(ERoleOrder.PEREVERTEN_MAFIA.name())) {
                return true;
            }
        }
        return false;
    }

    private void createPodium(List<GameStatistics> top3Players) {
        podiumBox.getChildren().clear();
        podiumBox.getStyleClass().add("podium-box");

        String[] medals = {"/images/gold.png", "/images/silver.png", "/images/bronze.png"};

        // Define fixed width for each card
        final double cardWidth = 150;

        for (int i = 0; i < top3Players.size(); i++) {
            GameStatistics player = top3Players.get(i);

            ImageView medalView = new ImageView(new Image(getClass().getResourceAsStream(medals[i])));
            medalView.setFitHeight(100);
            medalView.setFitWidth(50);
            medalView.getStyleClass().add("medal-image");

            // Create a grey circle to represent the avatar
            Circle avatarCircle = new Circle(30, Color.GREY);
            if (player != null) {
                Image avatarImage = null;
                try {
                    avatarImage = new Image("images/" + player.getRole().getRoleNameConstant() + ".jpg");
                } catch (Exception e) {
                    avatarImage = new Image("images/icon.ico");
                }
                ImagePattern imagePattern = new ImagePattern(avatarImage);
                avatarCircle.setFill(imagePattern);
            }
            avatarCircle.setStroke(Color.WHITE);
            avatarCircle.setStrokeWidth(2);

            Label nameLabel = new Label(player.getInGameNickname());
            nameLabel.setStyle("-fx-text-fill: black; -fx-font-size: 16px; -fx-font-family: 'Arial'; -fx-text-alignment: center"); // Set text color to black
            nameLabel.setWrapText(true);  // Wrap text if it exceeds width
            nameLabel.setMaxWidth(cardWidth - 20);  // Subtract padding
            nameLabel.setAlignment(javafx.geometry.Pos.CENTER);

            Label roleLabel = new Label(player.getRole().getTitle());
            roleLabel.setStyle("-fx-text-fill: black; -fx-font-size: 14px; -fx-font-family: 'Arial'; -fx-text-alignment: center"); // Set text color to black
            roleLabel.setWrapText(true);  // Wrap text if it exceeds width
            roleLabel.setMaxWidth(cardWidth - 20);  // Subtract padding
            roleLabel.setAlignment(javafx.geometry.Pos.CENTER);

            Label pointsLabel = new Label("+" + player.getPoints());
            pointsLabel.setStyle("-fx-text-fill: black; -fx-font-size: 14px; -fx-font-family: 'Arial'; -fx-text-alignment: center"); // Set text color to black
            pointsLabel.setWrapText(true);  // Wrap text if it exceeds width
            pointsLabel.setMaxWidth(cardWidth - 20);  // Subtract padding
            pointsLabel.setAlignment(javafx.geometry.Pos.CENTER);

            VBox playerBox = new VBox(medalView, avatarCircle, nameLabel, roleLabel, pointsLabel);
            playerBox.getStyleClass().add("player-box"); // Apply player-box style

            nameLabel.getStyleClass().add("player-label"); // Apply player-label style
            roleLabel.getStyleClass().add("player-label"); // Apply player-label style
            pointsLabel.getStyleClass().add("player-label"); // Apply player-label style


            podiumBox.getChildren().add(playerBox);
        }
    }

    public void show() {
        stage.show();
    }

    @FXML
    private void handleCopyButtonAction() {
        StringBuilder sb = new StringBuilder();

        // Get all items from the TableView
        for (GameStatistics item : gameStatisticsTable.getItems()) {
            String nickname = item.getInGameNickname();
            int totalPoints = item.getPoints();
            sb.append(nickname).append(": +").append(totalPoints).append("\n");
        }

        // Copy the text to the clipboard
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(sb.toString());
        clipboard.setContent(content);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Дані було скопійовано");
        alert.setHeaderText(null);
        alert.setContentText("Скопійовано в буфер обміну!");
        DialogPane dialogPane1 = alert.getDialogPane();
        dialogPane1.getStylesheets().add(
                getClass().getResource("/style/myDialogs.css").toExternalForm());
        dialogPane1.getStyleClass().add("myDialog");

        alert.showAndWait();
    }
}
