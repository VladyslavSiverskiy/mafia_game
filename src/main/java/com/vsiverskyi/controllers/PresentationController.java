package com.vsiverskyi.controllers;

import com.vsiverskyi.service.GameService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.temporal.ChronoUnit;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

@Component
@RequiredArgsConstructor
@FxmlView("Presentation.fxml")
public class PresentationController implements Initializable {
    private Stage stage;
    private Scene scene;
    private Parent root;
    @Autowired
    private GameService gameService;
    @Autowired
    private FxWeaver fxWeaver;
    @FXML
    private AnchorPane presentationAp;
    @FXML
    private Label secondsLeft;
    @FXML
    private Button startVoting;

    //set the delay as 0
    int secondsPerPresentation = 5;
    int secondsTillEnd = 5;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.stage = StarterController.primaryStage;
        stage.setScene(new Scene(presentationAp));
        // Create time line to lower remaining duration every second:
        Timeline countDownTimeLine = new Timeline(new KeyFrame(Duration.seconds(1), (ActionEvent event) ->
                secondsLeft.setText(String.valueOf(secondsTillEnd--))));
        // Set number of cycles (remaining duration in seconds):
        countDownTimeLine.setCycleCount((int) secondsPerPresentation);
        // Show alert when time is up:
        countDownTimeLine.setOnFinished(event -> new Alert(Alert.AlertType.INFORMATION).show());
        countDownTimeLine.play();

        startVoting.setOnAction(actionEvent -> fxWeaver.loadController(VotingController.class).show());
    }

    public void show() {
        stage.show();
    }
}
