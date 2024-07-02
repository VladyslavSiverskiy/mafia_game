package com.vsiverskyi.controllers;

import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class ViewController {
    public static void showCards(int yellowCardsIterator, int redCardsIterator, HBox avatarContainer) {
        for (int j = 0; j < yellowCardsIterator; j++) { // Adjust the number of yellow cards as needed
            Rectangle yellowCard = new Rectangle(8, 12, Color.YELLOW);
            yellowCard.setStyle("-fx-border-radius: 1px");
            avatarContainer.getChildren().add(yellowCard);
        }
        for (int j = 0; j < redCardsIterator; j++) { // Adjust the number of yellow cards as needed
            Rectangle redCard = new Rectangle(8, 12, Color.RED);
            redCard.setStyle("-fx-border-radius: 1px");
            avatarContainer.getChildren().add(redCard);
        }

    }
}
