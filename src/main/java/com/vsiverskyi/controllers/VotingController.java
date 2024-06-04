package com.vsiverskyi.controllers;

import com.vsiverskyi.model.GameStatistics;
import com.vsiverskyi.service.GameService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

@Component
@RequiredArgsConstructor
@FxmlView("Voting.fxml")
public class VotingController implements Initializable {

    @Autowired
    private GameService gameService;
    private Stage stage;
    private Scene scene;
    private Parent root;
    @FXML
    private AnchorPane votingAp;
    @FXML
    private GridPane votingGrid;
    @FXML
    private Button beginVoting;
    private List<GameStatistics> gameStatisticsList;
    private int currentPlayerIndex = 0;
    private final static Integer NUM_BUTTON_LINES = 5;
    private final static Integer BUTTONS_PER_LINE = 5;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.stage = StarterController.primaryStage;
        stage.setScene(new Scene(votingAp));

        gameStatisticsList = gameService.getGameInfo(SelectionController.currentGameId).getGameStatistics();
        for (int r = 0; r < NUM_BUTTON_LINES; r++) {
            for (int c = 0; c < BUTTONS_PER_LINE; c++) {
                int number = NUM_BUTTON_LINES * r + c;
                if (number > 0) {
                    Button button = new Button(String.valueOf(number));
                    button.setOnAction(event -> {
                        setVote(number);
                    });
                    button.setDisable(true);
                    votingGrid.add(button, r, c);
                }
            }
        }

        beginVoting.setOnAction(actionEvent -> beginVoting());
        //TODO: всіх гравців проініціалізовано
        // потрібно перед тим ще етап представлення
        // беремо запис із game_stats і в нас є гравець, що голосує за
        // потрібно далі таблицю, або хешмап, де зберігаються результати голос.
        // берем масив game_stats, берем перий елемент, вибираєм за кого голосувати,
        // Робим хеш_мап, де ключ номер гравця у грі, значення кількість проголосовано

    }

    private void beginVoting() {
        for (Node node : votingGrid.getChildren()) {
            Button btn = (Button) node;
            if(Integer.parseInt(btn.getText()) != currentPlayerIndex + 1) {
                btn.setDisable(false);
            } else {
                btn.setDisable(true);
            }
        }
    }

    private void setVote (int playerNumber) {
        // TODO: тут вже в хеш мап додавати голоси за ключем playerNumber
        currentPlayerIndex++;
        beginVoting();
    }

    public void show() {
        stage.show();
    }
}
