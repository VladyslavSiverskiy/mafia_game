package com.vsiverskyi.controllers;

import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import lombok.Getter;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.Properties;

import static com.vsiverskyi.utils.StyleConstants.HOVERED_BUTTON_STYLE;
import static com.vsiverskyi.utils.StyleConstants.IDLE_BUTTON_STYLE;

@Component
@Getter
@FxmlView("GeneralSettings.fxml")
public class GeneralSettingsController {
    Stage stage;
    @Autowired
    private FxWeaver fxWeaver;
    @FXML
    private Spinner<Integer> secondsPerMoveSpinner;
    @FXML
    private Spinner<Integer> secondsPerPresentationSpinner;
    @FXML
    private Spinner<Integer> secondsPerDiscussionSpinner;
    @FXML
    private Spinner<Integer> secondsPerDefendSpinner;
    @FXML
    private Button applyButton;
    @FXML
    private AnchorPane settingsPane;
    private static final String SETTINGS_FILE = "settings.properties";

    @FXML
    public void initialize() {
        this.stage = StarterController.primaryStage;
        stage.setScene(new Scene(settingsPane));
        stage.getIcons().add(new Image("/images/title.jpg"));
        stage.setTitle("STOP КОРУПЦІЯ");
        // Initialize the spinner with a value factory
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 60, 10);
        secondsPerMoveSpinner.setValueFactory(valueFactory);

        SpinnerValueFactory<Integer> valueFactoryPresentation = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 60, 10);
        secondsPerPresentationSpinner.setValueFactory(valueFactoryPresentation);

        SpinnerValueFactory<Integer> valueFactoryDiscussion = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 120, 30);
        secondsPerDiscussionSpinner.setValueFactory(valueFactoryDiscussion);

        SpinnerValueFactory<Integer> valueFactoryDefence = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 60, 10);
        secondsPerDefendSpinner.setValueFactory(valueFactoryDefence);

        // Load settings from the properties file
        loadSettings();

        // Set the action for the apply button
        applyButton.setOnAction(event ->{
            saveSettings();
            StarterController.primaryStage = (Stage) applyButton.getScene().getWindow();
            fxWeaver.loadController(StarterController.class).show();
        });

        applyButton.setStyle(IDLE_BUTTON_STYLE);
        applyButton.setOnMouseEntered(ev -> applyButton.setStyle(HOVERED_BUTTON_STYLE));
        applyButton.setOnMouseExited(ev -> applyButton.setStyle(IDLE_BUTTON_STYLE));
    }

    private void loadSettings() {
        Properties properties = new Properties();
        try (BufferedReader reader = new BufferedReader(new FileReader(SETTINGS_FILE))) {
            properties.load(reader);

            // Get the value for secondsPerMove
            String secondsPerMove = properties.getProperty("secondsPerMove");
            if (secondsPerMove != null) {
                secondsPerMoveSpinner.getValueFactory().setValue(Integer.parseInt(secondsPerMove));
            }

            String secondsPerPresentation = properties.getProperty("secondsPerPresentation");
            if (secondsPerPresentation != null) {
                secondsPerPresentationSpinner.getValueFactory().setValue(Integer.parseInt(secondsPerPresentation));
            }

            String secondsPerDiscussion = properties.getProperty("secondsPerDiscussion");
            if (secondsPerPresentation != null) {
                secondsPerPresentationSpinner.getValueFactory().setValue(Integer.parseInt(secondsPerDiscussion));
            }

            String secondsPerDefence = properties.getProperty("secondsPerDefence");
            if (secondsPerDefence != null) {
                secondsPerDefendSpinner.getValueFactory().setValue(Integer.parseInt(secondsPerDefence));
            }

        } catch (IOException e) {
            System.out.println("Error loading settings: " + e.getMessage());
        }
    }

    private void saveSettings() {
        Properties properties = new Properties();
        properties.setProperty("secondsPerMove", String.valueOf(secondsPerMoveSpinner.getValue()));
        properties.setProperty("secondsPerPresentation", String.valueOf(secondsPerPresentationSpinner.getValue()));
        properties.setProperty("secondsPerDiscussion", String.valueOf(secondsPerDiscussionSpinner.getValue()));
        properties.setProperty("secondsPerDefence", String.valueOf(secondsPerDefendSpinner.getValue()));


        try (BufferedWriter writer = new BufferedWriter(new FileWriter(SETTINGS_FILE))) {
            properties.store(writer, null);
        } catch (IOException e) {
            System.out.println("Error saving settings: " + e.getMessage());
        }
    }

    public void show() {
        // Your existing show method
    }
}
