package com.aristel.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aristel.network.ClientConnection;
import com.aristel.network.IncomingMessageListener;

public class GameBoardController implements IncomingMessageListener {

    @FXML private GridPane boardGrid;
    @FXML private Label statusLabel;
    @FXML private Label turnLabel;

    private List<Button> cardButtons = new ArrayList<>();
    private Map<Integer, Image> imageCache = new HashMap<>();
    private Image backCardImage;

    public void initialize() {
        ClientConnection.getInstance().setMessageListener(this);
        
        statusLabel.setText("Waiting for game to start...");
        loadImages();
    }

    private void loadImages() {
        try {
            backCardImage = new Image(getClass().getResourceAsStream("/com/aristel/assets/back.png"));

            for (int i = 0; i < 20; i++) {
                String path = "/com/aristel/assets/" + i + ".png";
                Image img = new Image(getClass().getResourceAsStream(path));
                imageCache.put(i, img);
            }
        } catch (Exception e) {
            System.err.println("Error loading images: " + e.getMessage());
        }
    }

    @FXML
    private void handleStartGame() {
        ClientConnection.getInstance().sendMessage("START");
    }

    private void setupBoard(int totalCards) {
        boardGrid.getChildren().clear();
        cardButtons.clear();

        int columns = 5; 
        
        for (int i = 0; i < totalCards; i++) {
            Button btn = new Button();
            btn.setPrefSize(100, 140);
            btn.getStyleClass().add("card-button"); 

            setButtonImage(btn, backCardImage);

            int index = i;
            btn.setOnAction(e -> ClientConnection.getInstance().sendMessage("CLICK:" + index));

            cardButtons.add(btn);
            boardGrid.add(btn, i % columns, i / columns);
        }
    }

    private void setButtonImage(Button btn, Image img) {
        if (img == null) return;
        ImageView view = new ImageView(img);
        view.setFitWidth(90);
        view.setFitHeight(130);
        view.setPreserveRatio(true);
        btn.setGraphic(view);
    }

    private void flipCard(int index, int imageId) {
        if (index >= 0 && index < cardButtons.size()) {
            Button btn = cardButtons.get(index);
            setButtonImage(btn, imageCache.get(imageId));
        }
    }

    private void hideCards(int idx1, int idx2) {
        if (idx1 >= 0 && idx1 < cardButtons.size()) setButtonImage(cardButtons.get(idx1), backCardImage);
        if (idx2 >= 0 && idx2 < cardButtons.size()) setButtonImage(cardButtons.get(idx2), backCardImage);
    }

    @Override
    public void onMessageReceived(String message) {
        String[] parts = message.split(":");
        String cmd = parts[0];

        switch (cmd) {
            case "GAME_START":
                int count = Integer.parseInt(parts[1]);
                Platform.runLater(() -> setupBoard(count));
                break;

            case "FLIP": 
                flipCard(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                break;

            case "HIDE": 
                hideCards(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                break;

            case "TURN":
                Platform.runLater(() -> turnLabel.setText("Current Turn: Player " + parts[1]));
                break;
                
            case "MATCH": 
                Platform.runLater(() -> statusLabel.setText("Player " + parts[1] + " Scored! Total: " + parts[2]));
                break;
                
            case "GAME_OVER":
                Platform.runLater(() -> {
                    Alert a = new Alert(Alert.AlertType.INFORMATION, "Game Over!");
                    a.show();
                });
                break;
                
            default: 
                Platform.runLater(() -> statusLabel.setText(parts.length > 1 ? parts[1] : message));
                break;
        }
    }
}