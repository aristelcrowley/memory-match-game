package com.aristel.controller;

import com.aristel.MainApp;
import com.aristel.network.ClientConnection;
import com.aristel.network.IncomingMessageListener;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.*;

public class GameBoardController implements IncomingMessageListener {

    @FXML private GridPane boardGrid;
    @FXML private Label turnLabel;
    @FXML private Label instructionLabel;
    @FXML private VBox scoreContainer;

    @FXML private AnchorPane startPopup;
    @FXML private Label startTurnLabel;
    @FXML private Label countdownLabel;

    @FXML private AnchorPane endPopup;
    @FXML private Label resultTitle;
    @FXML private Label resultMessage;
    @FXML private Label returnTimerLabel;

    private List<Button> cardButtons = new ArrayList<>();
    private Map<Integer, Image> imageCache = new HashMap<>();
    private Image backCardImage;
    
    private int myPlayerId = -1;
    private Map<Integer, Integer> playerScores = new HashMap<>();
    private boolean inputLocked = true; 

    public void initialize() {
        this.myPlayerId = ClientConnection.getInstance().myPlayerId;
        ClientConnection.getInstance().setMessageListener(this);
        loadImages();
        
        ClientConnection.getInstance().sendMessage("GET_GAME_STATE");
        
        inputLocked = true; 
    }

    private void loadImages() {
        try {
            backCardImage = new Image(getClass().getResourceAsStream("/com/aristel/assets/back.png"));
            for (int i = 0; i < 20; i++) {
                String path = "/com/aristel/assets/" + i + ".png";
                Image img = new Image(getClass().getResourceAsStream(path));
                imageCache.put(i, img);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public void onMessageReceived(String message) {
        String[] parts = message.split(":");
        String cmd = parts[0];

        switch (cmd) {
            case "GAME_INIT": 
                int totalCards = Integer.parseInt(parts[1]);
                Platform.runLater(() -> startSequence(totalCards));
                break;

            case "SCORES":
                parseScores(parts.length > 1 ? parts[1] : "");
                break;

            case "FLIP": 
                int fIdx = Integer.parseInt(parts[1]);
                int fImg = Integer.parseInt(parts[2]);
                Platform.runLater(() -> flipCard(fIdx, fImg));
                break;

            case "HIDE": 
                int h1 = Integer.parseInt(parts[1]);
                int h2 = Integer.parseInt(parts[2]);
                Platform.runLater(() -> hideCards(h1, h2));
                break;

            case "TURN":
                int currentTurn = Integer.parseInt(parts[1]);
                Platform.runLater(() -> updateTurn(currentTurn));
                break;
                
            case "MATCH": 
                int pId = Integer.parseInt(parts[1]);
                int score = Integer.parseInt(parts[2]);
                Platform.runLater(() -> updateOneScore(pId, score));
                break;
                
            case "GAME_OVER":
                Platform.runLater(this::showEndScreen);
                break;
        }
    }

    private void startSequence(int totalCards) {
        setupBoard(totalCards);
        
        startPopup.setVisible(true);
        
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.seconds(0), e -> countdownLabel.setText("5")),
            new KeyFrame(Duration.seconds(1), e -> countdownLabel.setText("4")),
            new KeyFrame(Duration.seconds(2), e -> countdownLabel.setText("3")),
            new KeyFrame(Duration.seconds(3), e -> countdownLabel.setText("2")),
            new KeyFrame(Duration.seconds(4), e -> countdownLabel.setText("1")),
            new KeyFrame(Duration.seconds(5), e -> {
                startPopup.setVisible(false);
                inputLocked = false; 
            })
        );
        timeline.play();
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
            btn.setOnAction(e -> {
                if (!inputLocked) ClientConnection.getInstance().sendMessage("CLICK:" + index);
            });

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
        instructionLabel.setText("Fate denies you.");
        if (idx1 >= 0) setButtonImage(cardButtons.get(idx1), backCardImage);
        if (idx2 >= 0) setButtonImage(cardButtons.get(idx2), backCardImage);
    }

    private void updateTurn(int playerId) {
        String name = (playerId == myPlayerId) ? "YOUR TURN" : "Player " + playerId;
        turnLabel.setText(name);
        startTurnLabel.setText(name); 
        
        if (playerId == myPlayerId) {
            turnLabel.setStyle("-fx-text-fill: #4cd137;"); 
            instructionLabel.setText("Choose your destiny.");
        } else {
            turnLabel.setStyle("-fx-text-fill: #f1c40f;"); 
            instructionLabel.setText("Player " + playerId + " is choosing...");
        }
    }

    private void parseScores(String data) {
        String[] entries = data.split(",");
        for (String entry : entries) {
            if (entry.contains("=")) {
                String[] parts = entry.split("=");
                int pid = Integer.parseInt(parts[0]);
                int sc = Integer.parseInt(parts[1]);
                playerScores.put(pid, sc);
            }
        }
        Platform.runLater(this::renderScoreboard);
    }

    private void updateOneScore(int playerId, int score) {
        playerScores.put(playerId, score);
        renderScoreboard();
        if (playerId == myPlayerId) instructionLabel.setText("Destiny favors you!");
        else instructionLabel.setText("Player " + playerId + " matched!");
    }

    private void renderScoreboard() {
        scoreContainer.getChildren().clear();
        
        playerScores.entrySet().stream()
            .sorted((k1, k2) -> k2.getValue().compareTo(k1.getValue()))
            .forEach(entry -> {
                String name = (entry.getKey() == myPlayerId) ? "YOU" : "Player " + entry.getKey();
                Label lbl = new Label(name + ": " + entry.getValue() + " pts");
                lbl.getStyleClass().add("score-text-rank");
                if (entry.getKey() == myPlayerId) lbl.setStyle("-fx-text-fill: #4cd137;"); 
                scoreContainer.getChildren().add(lbl);
            });
    }

    private void showEndScreen() {
        endPopup.setVisible(true);
        
        int maxScore = -1;
        List<Integer> winners = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : playerScores.entrySet()) {
            if (entry.getValue() > maxScore) {
                maxScore = entry.getValue();
                winners.clear();
                winners.add(entry.getKey());
            } else if (entry.getValue() == maxScore) {
                winners.add(entry.getKey());
            }
        }

        if (winners.contains(myPlayerId)) {
            if (winners.size() > 1) {
                resultTitle.setText("DRAW");
                resultTitle.setStyle("-fx-text-fill: yellow;");
                resultMessage.setText("Fate is undecided.");
            } else {
                resultTitle.setText("VICTORY");
                resultTitle.setStyle("-fx-text-fill: #4cd137;");
                resultMessage.setText("You are the Arcana Master.");
            }
        } else {
            resultTitle.setText("DEFEAT");
            resultTitle.setStyle("-fx-text-fill: #e74c3c;");
            resultMessage.setText("Destiny was not with you.");
        }

        Timeline timeline = new Timeline(
            new KeyFrame(Duration.seconds(5), e -> {
                ClientConnection.getInstance().sendMessage("LEAVE"); 
                MainApp.loadView("views/LobbyView.fxml");
            })
        );
        timeline.play();
    }
}