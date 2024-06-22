package com.vsiverskyi.controllers;

import com.vsiverskyi.exception.CantStartGameException;
import com.vsiverskyi.model.Game;
import com.vsiverskyi.service.GameService;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.net.URL;
import java.util.*;

@Component
@FxmlView("GameSettings.fxml")
public class GameSettingsController implements Initializable {
    private Stage stage;
    private Scene scene;
    private Parent root;
    @FXML
    private AnchorPane anchorPane;
    @FXML
    private AnchorPane anchorPaneScrollPlace;
    private ConfigurableApplicationContext applicationContext;
    private GameService gameService;
    private FxWeaver fxWeaver;
    @FXML
    private Spinner<Integer> playersAmountSpinner;
    @FXML
    private Spinner<Integer> mafiaAmountSpinner;
    @FXML
    private Spinner<Integer> doctorsAmountSpinner;
    @FXML
    private Spinner<Integer> secondsPerMoveSpinner;

    public static List<Integer> roleIdPerGameList = new ArrayList<>();
    private final static Integer MAX_PLAYERS_AMOUNT = 26;
    SpinnerValueFactory<Integer> playersAmountSpinnerValueFactory =
            new SpinnerValueFactory.IntegerSpinnerValueFactory(10, MAX_PLAYERS_AMOUNT);
    int currentPlayersAmount;
    int selectedPlayersAmount;
    private Map<String, Boolean> settingsState = new HashMap<>();
    private Map<String, Integer> roleAmounts = new HashMap<>();


    @Autowired
    public GameSettingsController(GameService gameService, FxWeaver fxWeaver) {
        this.gameService = gameService;
        this.fxWeaver = fxWeaver;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.stage = StarterController.primaryStage;
        stage.setScene(new Scene(anchorPane));
        stage.setMaximized(true);

        playersAmountSpinnerValueFactory.setValue(10);
        playersAmountSpinner.setValueFactory(playersAmountSpinnerValueFactory);
        currentPlayersAmount = playersAmountSpinner.getValue();

        SpinnerValueFactory<Integer> mafiaAmountSpinnerValueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(3, 12); // можливо додати розрахунок на основі к-сті гравців
        mafiaAmountSpinnerValueFactory.setValue(3);
        mafiaAmountSpinner.setValueFactory(mafiaAmountSpinnerValueFactory);

        SpinnerValueFactory<Integer> secondsPerMoveSpinnerValueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(45, 60);
        secondsPerMoveSpinnerValueFactory.setValue(50);
        secondsPerMoveSpinner.setValueFactory(secondsPerMoveSpinnerValueFactory);

        // Create a VBox to hold all the settings
        VBox settingsBox = new VBox();
        settingsBox.setPadding(new Insets(10));
        settingsBox.setSpacing(10);

        settingsBox.getChildren().add(createSettingRow("Лікар"));
        settingsBox.getChildren().add(createSettingRow("Шериф"));
        settingsBox.getChildren().add(createSettingRow("Леді"));
        settingsBox.getChildren().add(createSettingRow("Маніяк"));
        settingsBox.getChildren().add(createSettingRow("Стрілочник"));
        settingsBox.getChildren().add(createSettingRow("Бомба"));
        settingsBox.getChildren().add(createSettingRow("Затичка"));

        // Create a ScrollPane and add the settingsBox to it
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(settingsBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: #161616");
        // Set the anchors to make ScrollPane fill the AnchorPane
        AnchorPane.setTopAnchor(scrollPane, 0.0);
        AnchorPane.setBottomAnchor(scrollPane, 0.0);
        AnchorPane.setLeftAnchor(scrollPane, 0.0);
        AnchorPane.setRightAnchor(scrollPane, 0.0);
        anchorPaneScrollPlace.getChildren().add(scrollPane);
    }

    public void show() {
        stage.show();
    }

    @FXML
    public void startGame(ActionEvent actionEvent) throws IOException {
        try {
            Game currentGame = gameService.beginGame(playersAmountSpinner.getValue());
            SelectionController.currentGameId = currentGame.getId();
            roleIdPerGameList = gameService.initRolesPerGame(
                    playersAmountSpinner.getValue(), mafiaAmountSpinner.getValue(), roleAmounts, currentGame
            );
            fxWeaver.loadController(SelectionController.class).show();
        } catch (CantStartGameException e) {
            new Alert(Alert.AlertType.WARNING, e.getMessage()).show();
        }
    }

    private HBox createSettingRow(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 14px");
        CheckBox checkBox = new CheckBox();
        settingsState.put(text, checkBox.isSelected());

        Spinner<Integer> roleAmountSpinner = new Spinner<>();
        SpinnerValueFactory<Integer> roleAmountSpinnerValueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 1);
        roleAmountSpinner.setValueFactory(roleAmountSpinnerValueFactory);
        roleAmountSpinner.setDisable(true);

        roleAmountSpinner.valueProperty().addListener((observable, oldValue, newValue) -> {
            roleAmounts.put(text, newValue);
            if (newValue > oldValue) {
                selectedPlayersAmount++;
            }else {
                if(newValue > 1)
                selectedPlayersAmount--;
            }
        });

        checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                selectedPlayersAmount++;
                roleAmounts.put(text, 1);
                roleAmountSpinner.setDisable(false);
            } else {
                selectedPlayersAmount--;
                roleAmountSpinner.setDisable(true);
                selectedPlayersAmount = selectedPlayersAmount - roleAmountSpinner.getValue() + 1;
                roleAmountSpinner.getValueFactory().setValue(1); // Reset spinner value to 1
                roleAmounts.remove(text); // Reset role amount to 1
            }
            if (selectedPlayersAmount + mafiaAmountSpinner.getValue() >= currentPlayersAmount) {
                playersAmountSpinnerValueFactory.setValue(++currentPlayersAmount);
                playersAmountSpinner.setValueFactory(playersAmountSpinnerValueFactory);
            }
            settingsState.put(text, newValue);
        });
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setSpacing(15);
        row.getChildren().addAll(label, checkBox, roleAmountSpinner);
        return row;
    }
}
