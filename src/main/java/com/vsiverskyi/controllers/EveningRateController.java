package com.vsiverskyi.controllers;

import com.vsiverskyi.model.Game;
import com.vsiverskyi.model.GameStatistics;
import com.vsiverskyi.service.GameService;
import com.vsiverskyi.service.GameStatisticsService;
import com.vsiverskyi.utils.StyleConstants;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
@FxmlView("EveningRate.fxml")
public class EveningRateController implements Initializable {


    @Autowired
    private GameService gameService;
    @Autowired
    private GameStatisticsService gameStatisticsService;
    @Autowired
    private FxWeaver fxWeaver;
    public static Long gameIdToBeFound;
    private Stage stage;
    private Scene scene;
    private Parent root;
    @FXML
    private AnchorPane eveningRateAp;
    @FXML
    private Button backToHome;
    @FXML
    private TableView<PlayerScore> playerScoreTable;
    @FXML
    private TableColumn<PlayerScore, String> nickname;
    @FXML
    private TableColumn<PlayerScore, Integer> totalPointsColumn;
    @FXML
    private TableColumn<PlayerScore, Circle> avatarColumn;
    @FXML
    private TextField gameLimitTextField;
    @FXML
    private Button loadButton;
    @FXML
    private DatePicker startDatePicker;
    @FXML
    private DatePicker endDatePicker;
    @FXML
    private Button loadByDateButton;
    @FXML
    private ListView<Button> gamesListView; // Доданий ListView для кнопок
    private List<PlayerScore> playerScores;

    public void show() {
        stage.show();
    }

    public static class PlayerScore {
        private final String nickname;
        private final int totalPoints;
        private final Circle avatar;

        public PlayerScore(String nickname, int totalPoints, Circle avatar) {
            this.nickname = nickname;
            this.totalPoints = totalPoints;
            this.avatar = avatar;
        }

        public String getNickname() {
            return nickname;
        }

        public int getTotalPoints() {
            return totalPoints;
        }

        public Circle getAvatar() {
            return avatar;
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.stage = StarterController.primaryStage;
        scene = new Scene(eveningRateAp);
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.setFullScreen(true);
        stage.getIcons().add(new Image("/images/title.jpg"));
        stage.setTitle("STOP КОРУПЦІЯ");
        scene.getStylesheets().add(getClass().getResource("/style/style.css").toExternalForm());
        gameLimitTextField.setStyle("-fx-text-fill: #161616;");

        // Initialize columns
        nickname.setCellValueFactory(new PropertyValueFactory<>("nickname"));
        totalPointsColumn.setCellValueFactory(new PropertyValueFactory<>("totalPoints"));
        avatarColumn.setCellValueFactory(new PropertyValueFactory<>("avatar"));

        // Load and display player scores with default limit
        loadPlayerScores(5); // Default to 5 games

        loadButton.setOnAction(event -> {
            try {
                int limit = Integer.parseInt(gameLimitTextField.getText());

                loadPlayerScores(limit);
            } catch (NumberFormatException e) {
                showAlert("Хибно введене число", "Please enter a valid number.");
            }
        });

        loadByDateButton.setOnAction(event -> {
            LocalDate startDate = startDatePicker.getValue();
            LocalDate endDate = endDatePicker.getValue();
            if (startDate != null && endDate != null) {
                loadPlayerScoresByDateRange(startDate, endDate);
            } else {
                showAlert("Хибно введені дані", "Будь ласка оберіть початкову та кінцеві дати.");
            }
        });

        backToHome.setOnMouseClicked(ev -> {
            StarterController.primaryStage = (Stage) backToHome.getScene().getWindow();
            fxWeaver.loadController(StarterController.class).show();
        });

        // Load games into ListView
        loadGamesList();
    }

    private void loadPlayerScores(int limit) {
        // Retrieve the recent games based on the selected limit
        List<Game> recentGames = gameService.findRecentGames(limit);

        // Retrieve game statistics for the recent games
        List<GameStatistics> allGameStatistics = recentGames.stream()
                .flatMap(game -> gameStatisticsService.getGameStatisticsByGameId(game.getId()).stream())
                .collect(Collectors.toList());

        // Initialize a map to store player scores, defaulting to 0 points
        Map<String, Integer> playerPointsMap = new HashMap<>();

        // Create a map to store avatars
        Map<String, Circle> playerAvatarsMap = new HashMap<>();

        // Iterate over all game statistics to ensure all players are included
        for (GameStatistics stat : allGameStatistics) {
            String nickname = stat.getInGameNickname();
            if (nickname != null) {
                // Initialize player score and avatar if not present
                playerPointsMap.putIfAbsent(nickname, 0);
                playerAvatarsMap.putIfAbsent(nickname, new Circle(10, Color.DARKGREY));

                // Add points to the player
                playerPointsMap.put(nickname, playerPointsMap.get(nickname) + stat.getPoints());
            }
        }

        // Convert to list of PlayerScore
        playerScores = playerPointsMap.entrySet().stream()
                .map(entry -> new PlayerScore(entry.getKey(), entry.getValue(), playerAvatarsMap.get(entry.getKey())))
                .sorted(Comparator.comparingInt(PlayerScore::getTotalPoints).reversed())
                .collect(Collectors.toList());

        // Set items to the table view
        playerScoreTable.setItems(FXCollections.observableArrayList(playerScores));
    }

    private void loadPlayerScoresByDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // Retrieve games within the date range
        List<Game> gamesByDateRange = gameService.findGamesByDateRange(startDateTime, endDateTime);

        // Retrieve game statistics for the games within the date range
        List<GameStatistics> allGameStatistics = gamesByDateRange.stream()
                .flatMap(game -> gameStatisticsService.getGameStatisticsByGameId(game.getId()).stream())
                .collect(Collectors.toList());

        // Initialize a map to store player scores, defaulting to 0 points
        Map<String, Integer> playerPointsMap = new HashMap<>();

        // Create a map to store avatars
        Map<String, Circle> playerAvatarsMap = new HashMap<>();

        // Iterate over all game statistics to ensure all players are included
        for (GameStatistics stat : allGameStatistics) {
            String nickname = stat.getInGameNickname();
            if (nickname != null) {
                // Initialize player score and avatar if not present
                playerPointsMap.putIfAbsent(nickname, 0);
                playerAvatarsMap.putIfAbsent(nickname, new Circle(10, Color.DARKGREY));

                // Add points to the player
                playerPointsMap.put(nickname, playerPointsMap.get(nickname) + stat.getPoints());
            }
        }

        // Convert to list of PlayerScore
        playerScores = playerPointsMap.entrySet().stream()
                .map(entry -> new PlayerScore(entry.getKey(), entry.getValue(), playerAvatarsMap.get(entry.getKey())))
                .sorted(Comparator.comparingInt(PlayerScore::getTotalPoints).reversed())
                .collect(Collectors.toList());

        // Set items to the table view
        playerScoreTable.setItems(FXCollections.observableArrayList(playerScores));
    }

    private void loadGamesList() {
        // Retrieve the list of all games
        List<Game> allGames = gameService.findAllGames().stream().filter(game -> game.getWinnerSide() != null).collect(Collectors.toList());
        allGames.sort(Comparator.comparing(Game::getLastUpdate).reversed());
        // Create buttons for each game
        List<Button> gameButtons = allGames.stream()
                .map(game -> {
                    Button button = new Button("ID гри: " + game.getId() + " | Дата проведення: " + game.getLastUpdate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")));
                    button.setOnAction(event -> showGameStatistics(game));
                    button.setStyle(StyleConstants.IDLE_BUTTON_STYLE);
                    return button;
                })
                .collect(Collectors.toList());

        // Set buttons to the ListView
        gamesListView.setItems(FXCollections.observableArrayList(gameButtons));
    }

    private void showGameStatistics(Game game) {
        // Retrieve game statistics for the selected game
//        List<GameStatistics> gameStatistics = gameStatisticsService.getGameStatisticsByGameId(game.getId());
        gameIdToBeFound = game.getId();
        fxWeaver.loadController(ArchiveGameController.class).show();

//        // Display game statistics (e.g., in a new window or modal)
//        // Here, just showing a simple alert for demonstration
//        StringBuilder statsMessage = new StringBuilder("Статистика гри з ID: " + game.getId() + "\n");
//        gameStatistics.forEach(stat -> {
//            statsMessage.append("Гравець: ").append(stat.getInGameNickname())
//                    .append(", Очок отримано: ").append(stat.getPoints())
//                    .append("\n");
//        });
//
//        showAlert("Ігрова статистика", statsMessage.toString());
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        DialogPane dialogPane1 = alert.getDialogPane();
        dialogPane1.getStylesheets().add(
                getClass().getResource("/style/myDialogs.css").toExternalForm());
        dialogPane1.getStyleClass().add("myDialog");

        alert.showAndWait();
    }

    @FXML
    private void handleCopyButtonAction() {
        StringBuilder sb = new StringBuilder();

        int number = 1;
        // Get all items from the TableView
        for (PlayerScore item : playerScores) {
            String nickname = item.getNickname();
            int totalPoints = item.getTotalPoints();
            sb.append(number)
                    .append(".")
                    .append(nickname)
                    .append(": +").append(totalPoints).append("\n");
            number++;
        }

        // Copy the text to the clipboard
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(sb.toString());
        clipboard.setContent(content);

        showAlert("Дані було скопійовано","Скопійовано в буфер обміну!");
    }
}
