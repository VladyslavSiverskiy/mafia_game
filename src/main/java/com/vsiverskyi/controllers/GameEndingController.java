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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

@Component
@FxmlView("GameEnding.fxml")
public class GameEndingController implements Initializable {

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
    private Button toStarterPage;
    @FXML
    private Label winnerTitleLabel;
    @FXML
    private TableView<GameStatistics> gameStatisticsTable;
    @FXML
    private TableColumn<GameStatistics, String> playerNameColumn;
    @FXML
    private TableColumn<GameStatistics, Integer> pointsColumn;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.stage = StarterController.primaryStage;
        scene = new Scene(endingGameAp);
        stage.setScene(scene);
//        stage.setMaximized(true);
//        stage.setFullScreen(true);
        scene.getStylesheets().add(getClass().getResource("/style/style.css").toExternalForm());
        Game game = gameService.getGameInfo(SelectionController.currentGameId);
        pointsService.countPointsAfterGameWasFinished(game.getId());
        List<GameStatistics> gameStatisticsList = gameStatisticsService.getGameStatisticsByGameId(game.getId());

        playerNameColumn.setCellValueFactory(new PropertyValueFactory<>("inGameNickname"));
        pointsColumn.setCellValueFactory(new PropertyValueFactory<>("points"));
        gameStatisticsTable.setItems(FXCollections.observableArrayList(gameStatisticsList));

        winnerTitleLabel.setText(game.getWinnerSide().getTitle());
        toStarterPage.setOnAction(ev -> {
            StarterController.primaryStage = (Stage) toStarterPage.getScene().getWindow();
            fxWeaver.loadController(StarterController.class).show();
        });
    }

    public void show() {
        stage.show();
    }
}
