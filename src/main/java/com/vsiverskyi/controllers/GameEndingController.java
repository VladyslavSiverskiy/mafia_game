package com.vsiverskyi.controllers;

import com.vsiverskyi.model.Game;
import com.vsiverskyi.model.enums.ETeam;
import com.vsiverskyi.service.GameService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ResourceBundle;

@Component
@FxmlView("GameEnding.fxml")
public class GameEndingController implements Initializable {

    @Autowired
    private GameService gameService;
    private Stage stage;
    private Scene scene;
    private Parent root;
    @FXML
    private AnchorPane endingGameAp;
    @FXML
    private Button toStarterPage;
    @FXML
    private Label winnerTitleLabel;
    @Autowired
    private FxWeaver fxWeaver;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.stage = StarterController.primaryStage;
        stage.setScene(new Scene(endingGameAp));
        stage.setMaximized(true);
        stage.setFullScreen(true);
        Game game = gameService.getGameInfo(SelectionController.currentGameId);
        winnerTitleLabel.setText(game.getWinnerSide().getTitle());
        toStarterPage.setOnAction(ev ->  fxWeaver.loadController(StarterController.class).show());
    }

    public void show() {
        stage.show();
    }
}
