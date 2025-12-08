package com.aristel.controller;

import com.aristel.App;
import com.aristel.network.ClientConnection;
import com.aristel.network.IncomingMessageListener;
import com.aristel.util.SoundManager;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.*;
import java.util.stream.Collectors;

public class GameBoardController implements IncomingMessageListener {

    @FXML private GridPane boardGrid;
    @FXML private Label turnLabel;
    @FXML private Label instructionLabel;
    @FXML private VBox scoreContainer;

    @FXML private AnchorPane startPopup;
    @FXML private VBox startOrderContainer; 
    @FXML private Label countdownLabel;

    @FXML private AnchorPane endPopup;
    @FXML private Label resultTitle;
    @FXML private Label resultMessage;
    @FXML private Label finalScoreLabel; 
    @FXML private ImageView resultImage;
    @FXML private Label flavorLine1;     
    @FXML private Label flavorLine2;     
    @FXML private Label flavorLine3;     
    @FXML private Label returnTimerLabel;

    private List<Button> cardButtons = new ArrayList<>();
    private Map<Integer, Image> imageCache = new HashMap<>();
    private Image backCardImage;
    private int myPlayerId = -1;
    private int currentTurnId = -1; 
    private Map<Integer, Integer> playerScores = new HashMap<>();
    private boolean inputLocked = true; 
    private Set<Integer> disconnectedPlayers = new HashSet<>();

    public void initialize() {
        this.myPlayerId = ClientConnection.getInstance().myPlayerId;
        ClientConnection.getInstance().setMessageListener(this);
        loadImages();
        
        ClientConnection.getInstance().sendMessage("GET_GAME_STATE");
        disconnectedPlayers.clear();
        inputLocked = true; 
    }

    private void loadImages() {
        try {
            backCardImage = new Image(getClass().getResourceAsStream("/com/aristel/assets/images/back.png"));
            for (int i = 0; i < 20; i++) {
                String path = "/com/aristel/assets/images/" + i + ".png";
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

            case "PLAYER_DISCONNECTED":
                int pid = Integer.parseInt(message.split(":")[1]);
                disconnectedPlayers.add(pid);
                Platform.runLater(this::renderScoreboard);
                SoundManager.getInstance().play("disconnect");
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
                this.currentTurnId = currentTurn; 
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
            
            case "BACK_TO_ROOM":
                Platform.runLater(() -> App.loadView("views/RoomView.fxml"));
                break;
        }
    }

    private void startSequence(int totalCards) {
        setupBoard(totalCards);
        
        SoundManager.getInstance().play("start");

        startOrderContainer.getChildren().clear();
        List<Integer> sortedIds = new ArrayList<>(playerScores.keySet());
        Collections.sort(sortedIds); 
        
        int rank = 1;
        int startIndex = sortedIds.indexOf(currentTurnId);
        if (startIndex != -1) {
            Collections.rotate(sortedIds, -startIndex);
        }

        for (Integer pid : sortedIds) {
            Label lbl = new Label(rank + ". Player " + pid);
            lbl.getStyleClass().add("popup-big-text");
            lbl.setStyle("-fx-font-size: 24px;"); 
            
            if (pid == myPlayerId) {
                lbl.setStyle("-fx-text-fill: #4cd137; -fx-font-size: 24px;"); 
            } else {
                lbl.setStyle("-fx-text-fill: white; -fx-font-size: 24px;");
            }
            startOrderContainer.getChildren().add(lbl);
            rank++;
        }

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
                SoundManager.getInstance().play("shuffled");
            })
        );
        timeline.play();
    }

    private void setupBoard(int totalCards) {
        boardGrid.getChildren().clear();
        cardButtons.clear();
        int rows = 5;
        int columns = totalCards / rows; 
        
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
        view.setFitWidth(100);
        view.setFitHeight(140);
        view.setPreserveRatio(false); 
        btn.setGraphic(view);
    }

    private void flipCard(int index, int imageId) {
        if (index >= 0 && index < cardButtons.size()) {
            Button btn = cardButtons.get(index);
            setButtonImage(btn, imageCache.get(imageId));
            SoundManager.getInstance().play("flip");
        }
    }

    private void hideCards(int idx1, int idx2) {
        instructionLabel.setText("Fate denies you.");
        SoundManager.getInstance().play("fail");
        if (idx1 >= 0) setButtonImage(cardButtons.get(idx1), backCardImage);
        if (idx2 >= 0) setButtonImage(cardButtons.get(idx2), backCardImage);
    }

    private void updateTurn(int playerId) {
        String displayName = "Player " + playerId;
        
        if (playerId == myPlayerId) {
            turnLabel.setText(displayName + " (You)");
            turnLabel.setStyle("-fx-text-fill: #4cd137;"); 
            instructionLabel.setText("Choose your destiny.");
            SoundManager.getInstance().play("turn");
        } else {
            turnLabel.setText(displayName);
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
        SoundManager.getInstance().play("match");
        if (playerId == myPlayerId) instructionLabel.setText("Destiny favors you!");
        else instructionLabel.setText("Player " + playerId + " matched!");
    }

    private void renderScoreboard() {
        scoreContainer.getChildren().clear();
        
        List<Map.Entry<Integer, Integer>> sortedList = playerScores.entrySet().stream()
            .sorted((k1, k2) -> k2.getValue().compareTo(k1.getValue()))
            .collect(Collectors.toList());

        int rank = 1;
        for (Map.Entry<Integer, Integer> entry : sortedList) {
            int pid = entry.getKey();
            String name = "Player " + pid;
            
            Label lbl = new Label(rank + ". " + name + ": " + entry.getValue() + " pts");
            
            if (disconnectedPlayers.contains(pid)) {
                lbl.getStyleClass().add("score-text-disconnected");

                HBox ghostBox = new HBox(10); 
                ghostBox.setAlignment(Pos.CENTER_LEFT);
                
                try {
                    ImageView disImg = new ImageView(new Image(getClass().getResourceAsStream("/com/aristel/assets/images/disconnected.png")));
                    disImg.setFitWidth(30);
                    disImg.setFitHeight(30);
                    ghostBox.getChildren().addAll(lbl, disImg);
                    scoreContainer.getChildren().add(ghostBox);
                } catch (Exception e) { 
                    scoreContainer.getChildren().add(lbl); 
                }
            } else {
                lbl.getStyleClass().add("score-text-rank");
                if (pid == myPlayerId) {
                    lbl.setStyle("-fx-text-fill: #4cd137;"); 
                } else {
                    lbl.setStyle("-fx-text-fill: white;"); 
                }
                scoreContainer.getChildren().add(lbl);
            }
            
            rank++;
        }
    }

    private void showEndScreen() {
        endPopup.setVisible(true);
        
        int maxScore = -1;
        List<Integer> winners = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : playerScores.entrySet()) {
            int pid = entry.getKey();
            
            if (disconnectedPlayers.contains(pid)) continue; 

            if (entry.getValue() > maxScore) {
                maxScore = entry.getValue();
                winners.clear();
                winners.add(pid);
            } else if (entry.getValue() == maxScore) {
                winners.add(pid);
            }
        }

        int myScore = playerScores.getOrDefault(myPlayerId, 0);
        finalScoreLabel.setText("Final Score: " + myScore);

        if (winners.contains(myPlayerId)) {
            if (winners.size() > 1) {
                SoundManager.getInstance().play("draw");
                setEndScreenContent(
                    "DRAW", "-fx-text-fill: #f1c40f;", "draw.png",
                    "The scales of destiny remain balanced.",
                    "Neither light nor shadow could claim you.",
                    "Fate demands a rematch to decide."
                );
            } else {
                SoundManager.getInstance().play("win");
                setEndScreenContent(
                    "VICTORY", "-fx-text-fill: #4cd137;", "win.png",
                    "The threads of fate weave in your favor.",
                    "You have ascended beyond the mortal coil.",
                    "The Major Arcana bow to your will."
                );
            }
        } else {
            SoundManager.getInstance().play("lose");
            setEndScreenContent(
                "DEFEAT", "-fx-text-fill: #e74c3c;", "lose.png",
                "Your thread has been severed by destiny.",
                "The cards reveal only ruin and despair.",
                "Return to the void, forgotten soul."
            );
        }

        Timeline timeline = new Timeline();
        for (int i = 0; i < 10; i++) {
            int secondsLeft = 10 - i;
            timeline.getKeyFrames().add(
                new KeyFrame(Duration.seconds(i), e -> returnTimerLabel.setText("Returning in " + secondsLeft + "..."))
            );
        }
        
        timeline.getKeyFrames().add(
            new KeyFrame(Duration.seconds(10), e -> {
                ClientConnection.getInstance().sendMessage("RESET_GAME");
            })
        );
        
        timeline.play();
    }

    private void setEndScreenContent(String title, String style, String imgName, String l1, String l2, String l3) {
        resultTitle.setText(title);
        resultTitle.setStyle(style);
        
        flavorLine1.setText(l1);
        flavorLine2.setText(l2);
        flavorLine3.setText(l3);

        try {
            Image img = new Image(getClass().getResourceAsStream("/com/aristel/assets/images/" + imgName));
            resultImage.setImage(img);
        } catch (Exception e) {
            System.err.println("Missing Image: " + imgName);
        }
    }
}