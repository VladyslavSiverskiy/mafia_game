package com.vsiverskyi.controllers;

import com.vsiverskyi.ApplicationRunner;
import javafx.event.ActionEvent;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

@Component
@RequiredArgsConstructor
@FxmlView("Starter.fxml")
public class StarterController {

    //variables to manipulate javaFx elements
    private Stage stage;
    private Scene scene;
    private Parent root;
    public static Stage primaryStage;
    private final FxWeaver fxWeaver;
    @FXML
    Button start_btn;

    @FXML
    public void openGameSettingsPage(ActionEvent actionEvent) throws IOException {
        primaryStage = (Stage) start_btn.getScene().getWindow();
        fxWeaver.loadController(GameSettingsController.class).show();
    }

    public void show() {
        Parent root = fxWeaver.loadView(StarterController.class);
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();

    }
}
