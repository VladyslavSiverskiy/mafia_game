package com.vsiverskyi.controllers;

import com.vsiverskyi.model.Game;
import com.vsiverskyi.model.GameStatistics;
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
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static com.vsiverskyi.utils.StyleConstants.HOVERED_BUTTON_STYLE;
import static com.vsiverskyi.utils.StyleConstants.IDLE_BUTTON_STYLE;

@Component
@FxmlView("EveningRate.fxml")
public class EveningRateController implements Initializable {

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

    public void show() {
        stage.show();
    }

    public static class PlayerScore {
        private final String nickname;
        private final int totalPoints;
        private final Circle avatar; // Add avatar field

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
            return new Circle(10, Color.DARKGREY);
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

        // Initialize columns
        nickname.setCellValueFactory(new PropertyValueFactory<>("nickname"));
        totalPointsColumn.setCellValueFactory(new PropertyValueFactory<>("totalPoints"));
        avatarColumn.setCellValueFactory(new PropertyValueFactory<>("avatar"));

        // Load and display player scores
        loadPlayerScores();

        backToHome.setOnAction(ev -> {
            StarterController.primaryStage = (Stage) backToHome.getScene().getWindow();
            fxWeaver.loadController(StarterController.class).show();
        });
    }


    private void loadPlayerScores() {
        // Get today's date
        LocalDate today = LocalDate.now();

        // Retrieve all games
        List<Game> allGames = gameService.findLastGames();
        // Filter games played today
        List<Game> todaysGames = allGames.stream()
                .filter(game -> game.getLastUpdate().toLocalDate().isEqual(today))
                .collect(Collectors.toList());

        // Retrieve game statistics for the filtered games
        List<GameStatistics> allGameStatistics = todaysGames.stream()
                .flatMap(game -> gameStatisticsService.getGameStatisticsByGameId(game.getId()).stream())
                .collect(Collectors.toList());

        // Group by player nickname and sum points
        Map<String, Integer> playerPointsMap = allGameStatistics.stream()
                .filter(gameStatistics -> gameStatistics.getInGameNickname() != null)
                .collect(Collectors.groupingBy(
                        stats -> stats.getInGameNickname(),
                        Collectors.summingInt(GameStatistics::getPoints)
                ));

        // Create a map to store avatars
        Map<String, Circle> playerAvatarsMap = allGameStatistics.stream()
                .collect(Collectors.toMap(
                        GameStatistics::getInGameNickname,
                        GameStatistics::getAvatarCircle,
                        (existing, replacement) -> existing // Use existing avatar if there are duplicates
                ));

        // Convert to list of PlayerScore
        List<PlayerScore> playerScores = playerPointsMap.entrySet().stream()
                .map(entry -> new PlayerScore(entry.getKey(), entry.getValue(), playerAvatarsMap.get(entry.getKey())))
                .sorted(Comparator.comparingInt(PlayerScore::getTotalPoints).reversed())
                .collect(Collectors.toList());

        // Set items to the table view
        playerScoreTable.setItems(FXCollections.observableArrayList(playerScores));
    }


}
