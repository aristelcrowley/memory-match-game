package com.aristel.controller;

import com.aristel.MainApp;
import com.aristel.network.ClientConnection;
import com.aristel.network.IncomingMessageListener;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class RoomController implements IncomingMessageListener {

    @FXML private Label roomNameLabel;
    @FXML private Label playerCountLabel;
    @FXML private VBox playerContainer;
    @FXML private Button startButton;
    @FXML private TextArea roomLog;

    private int myPlayerId = -1; 

    public void initialize() {
        this.myPlayerId = ClientConnection.getInstance().myPlayerId;
        ClientConnection.getInstance().setMessageListener(this);
        ClientConnection.getInstance().sendMessage("GET_STATE");
        
        addToLog("Destiny confronted you as you entered the room.");
    }

    @FXML private void handleStart() { ClientConnection.getInstance().sendMessage("START"); }
    @FXML private void handleLeave() { ClientConnection.getInstance().sendMessage("LEAVE"); }

    @Override
    public void onMessageReceived(String message) {
        if (message.startsWith("ROOM_STATE:")) {
            Platform.runLater(() -> updateRoomUI(message));
        }
        else if (message.startsWith("MSG:")) {
            String text = message.substring(4);
            System.out.println("[ROOM LOG] Adding: " + text); 
            Platform.runLater(() -> addToLog(text));
        }
        else if (message.startsWith("GAME_START:")) {
            Platform.runLater(() -> MainApp.loadView("views/GameBoardView.fxml"));
        }
        else if (message.equals("LEFT_ROOM")) {
            Platform.runLater(() -> MainApp.loadView("views/LobbyView.fxml"));
        }
    }

    private void addToLog(String text) {
        if (roomLog != null) {
            roomLog.appendText("> " + text + "\n");
            roomLog.positionCaret(roomLog.getLength()); 
        } else {
            System.err.println("ERROR: roomLog is NULL! Check FXML fx:id");
        }
    }

    private void updateRoomUI(String message) {
        try {
            String[] parts = message.split(":");
            String roomName = parts[1];
            int masterId = Integer.parseInt(parts[2]);
            int count = Integer.parseInt(parts[3]);
            String[] playerIds = (parts.length > 4) ? parts[4].split(",") : new String[0];

            roomNameLabel.setText(roomName);
            playerCountLabel.setText(count + "/4 Players");

            if (myPlayerId == masterId) {
                startButton.setDisable(false);
                startButton.setText("COMMENCE");
            } else {
                startButton.setDisable(true);
                startButton.setText("WAITING...");
            }

            renderCards(playerIds, masterId);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void renderCards(String[] playerIds, int masterId) {
        playerContainer.getChildren().clear();
        playerContainer.setSpacing(20); 

        for (int i = 0; i < 4; i++) {
            if (i < playerIds.length) {
                int pid = Integer.parseInt(playerIds[i]);
                boolean isMaster = (pid == masterId);
                playerContainer.getChildren().add(createPlayerCard(pid, isMaster));
            } else {
                playerContainer.getChildren().add(createVacantCard());
            }
        }
    }

    private HBox createPlayerCard(int playerId, boolean isMaster) {
        HBox card = new HBox(15); 
        card.getStyleClass().add("player-card");
        card.setPrefHeight(80); 
        card.setMaxWidth(Double.MAX_VALUE); 
        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        try {
            ImageView profileImg = new ImageView(new Image(getClass().getResourceAsStream("/com/aristel/assets/profile.png")));
            profileImg.setFitWidth(40);
            profileImg.setFitHeight(40);
            card.getChildren().add(profileImg);
        } catch (Exception e) { System.err.println("Missing profile.png"); }

        Label nameLbl = new Label("Player " + playerId);
        
        if (playerId == this.myPlayerId) {
            nameLbl.getStyleClass().add("player-name-self");
            nameLbl.setText(nameLbl.getText() + " (YOU)"); 
        } else {
            nameLbl.getStyleClass().add("player-name");
        }
        
        card.getChildren().add(nameLbl);

        if (isMaster) {
            try {
                ImageView masterImg = new ImageView(new Image(getClass().getResourceAsStream("/com/aristel/assets/roommaster.png")));
                masterImg.setFitWidth(25);
                masterImg.setFitHeight(25);
                card.getChildren().add(masterImg);
            } catch (Exception e) { System.err.println("Missing roommaster.png"); }
        }

        HBox spacer = new HBox();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        card.getChildren().add(spacer);

        return card;
    }

    private VBox createVacantCard() {
        VBox card = new VBox();
        card.getStyleClass().add("vacant-card");
        card.setPrefHeight(80); 
        card.setAlignment(javafx.geometry.Pos.CENTER);
        Label vacLbl = new Label("VACANT");
        vacLbl.getStyleClass().add("vacant-text");
        card.getChildren().add(vacLbl);
        return card;
    }
}