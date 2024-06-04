package com.vsiverskyi.controllers;

import com.vsiverskyi.model.Game;
import com.vsiverskyi.service.GameService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.ResourceBundle;

@Component
@FxmlView("GameSettings.fxml")
public class GameSettingsController implements Initializable {
    private Stage stage;
    private Scene scene;
    private Parent root;
    @FXML
    private AnchorPane anchorPane;
    private final static Integer MAX_PLAYERS_AMOUNT = 26;
    private ConfigurableApplicationContext applicationContext;
    private GameService gameService;
    private FxWeaver fxWeaver;

    @Autowired
    public GameSettingsController(GameService gameService, FxWeaver fxWeaver) {
        this.gameService = gameService;
        this.fxWeaver = fxWeaver;
    }
    @FXML
    private Spinner<Integer> playersAmountSpinner;
    @FXML
    private Spinner<Integer> mafiaAmountSpinner;
    @FXML
    private Spinner<Integer> doctorsAmountSpinner;
    @FXML
    private Spinner<Integer> secondsPerMoveSpinner;
    int currentPlayersAmount;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.stage = StarterController.primaryStage;
        stage.setScene(new Scene(anchorPane));

        SpinnerValueFactory<Integer> playersAmountSpinnerValueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(4,MAX_PLAYERS_AMOUNT);
        playersAmountSpinnerValueFactory.setValue(10);
        playersAmountSpinner.setValueFactory(playersAmountSpinnerValueFactory);
        currentPlayersAmount = playersAmountSpinner.getValue();

        SpinnerValueFactory<Integer> mafiaAmountSpinnerValueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1,3); // можливо додати розрахунок на основі к-сті гравців
        mafiaAmountSpinnerValueFactory.setValue(1);
        mafiaAmountSpinner.setValueFactory(mafiaAmountSpinnerValueFactory);

        SpinnerValueFactory<Integer> doctorsAmountSpinnerValueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1,3); // можливо додати розрахунок на основі к-сті гравців
        doctorsAmountSpinnerValueFactory.setValue(1);
        doctorsAmountSpinner.setValueFactory(doctorsAmountSpinnerValueFactory);

        SpinnerValueFactory<Integer> secondsPerMoveSpinnerValueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(45,60);
        secondsPerMoveSpinnerValueFactory.setValue(50);
        secondsPerMoveSpinner.setValueFactory(secondsPerMoveSpinnerValueFactory);
    }

    public void show() {
        stage.show();
    }

    @FXML
    public void startGame(ActionEvent actionEvent) throws IOException {
        System.out.println(playersAmountSpinner.getValue());
        System.out.println(mafiaAmountSpinner.getValue());
        System.out.println(doctorsAmountSpinner.getValue());
        System.out.println(secondsPerMoveSpinner.getValue());
        System.out.println(playersAmountSpinner.getValue() - mafiaAmountSpinner.getValue() - doctorsAmountSpinner.getValue());

        SelectionController.currentGameId = gameService.beginGame(playersAmountSpinner.getValue(), new HashMap<>()).getId();
        //TODO: тут сервіс працює бо у Starter Controller ми використовували fxWeaver
        // тому далі потрібно переробити за прикладом, а я йду спати:(

//        root = FXMLLoader.load(getClass().getResource("/pages/Selection.fxml"));
        fxWeaver.loadController(SelectionController.class).show();

//        stage = (Stage) ((Node)actionEvent.getSource()).getScene().getWindow();
//        scene = new Scene(root);
//        scene.getStylesheets().add(getClass().getResource("/style/loginPage.css").toExternalForm());
//        stage.setScene(scene);
//        stage.getStyle();
//        stage.show();
    }
}
