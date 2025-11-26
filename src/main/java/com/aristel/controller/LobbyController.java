package com.aristel.controller;

import com.aristel.MainApp;
import com.aristel.network.ClientConnection;
import com.aristel.network.IncomingMessageListener;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

public class LobbyController implements IncomingMessageListener {

    @FXML private FlowPane roomContainer;
    @FXML private TextField searchInput;
    @FXML private Label statusLabel;
    
    // Popup Controls
    @FXML private AnchorPane createPopup;
    @FXML private TextField newRoomNameInput;

    private String lastRoomData = ""; 

    public void initialize() {
        boolean connected = ClientConnection.getInstance().connect("localhost", 12345);
        if (connected) {
            statusLabel.setText("Connected. Fetching rooms...");
            ClientConnection.getInstance().setMessageListener(this);
            ClientConnection.getInstance().sendMessage("GET_ROOMS");
        } else {
            statusLabel.setText("Failed to connect to server!");
        }
    }

    @FXML
    private void handleSearch() {
        renderRooms(lastRoomData, searchInput.getText());
    }

    @FXML
    private void handleRefresh() {
        ClientConnection.getInstance().sendMessage("GET_ROOMS");
    }

    @FXML private void showCreatePopup() { createPopup.setVisible(true); }
    @FXML private void hideCreatePopup() { createPopup.setVisible(false); }

    @FXML
    private void handleConfirmCreate() {
        String room = newRoomNameInput.getText();
        if (!room.isEmpty()) {
            ClientConnection.getInstance().sendMessage("CREATE:" + room);
            hideCreatePopup();
        }
    }

    @Override
    public void onMessageReceived(String message) {
        if (message.startsWith("ROOM_LIST:")) {
            lastRoomData = message.substring(10); 
            Platform.runLater(() -> renderRooms(lastRoomData, searchInput.getText()));
        }
        else if (message.startsWith("MSG:") || message.startsWith("ERROR:")) {
            Platform.runLater(() -> statusLabel.setText(message.split(":", 2)[1]));
        } 
        else if (message.startsWith("JOINED:")) {
            Platform.runLater(() -> {
                System.out.println("Join successful, switching to GameBoard...");
                MainApp.loadView("views/GameBoardView.fxml");
            });
        }
    }

    private void renderRooms(String data, String filter) {
        roomContainer.getChildren().clear();
        
        if (data.isEmpty()) {
            statusLabel.setText("No rooms found. Create one!");
            return;
        }

        String[] rooms = data.split(";");
        for (String roomStr : rooms) {
            if (roomStr.isEmpty()) continue;
            
            String[] details = roomStr.split(",");
            if (details.length < 3) continue;

            String name = details[0];
            String master = details[1];
            String count = details[2];

            if (filter != null && !filter.isEmpty() && !name.toLowerCase().contains(filter.toLowerCase())) {
                continue;
            }

            VBox card = createRoomCard(name, master, count);
            roomContainer.getChildren().add(card);
        }
    }

    private VBox createRoomCard(String name, String master, String count) {
        VBox card = new VBox(10);
        card.getStyleClass().add("room-card");
        card.setPrefSize(200, 260);
        card.setAlignment(Pos.CENTER);

        Label lblName = new Label(name);
        lblName.getStyleClass().add("card-title");

        Label lblMaster = new Label("Master: " + master);
        lblMaster.getStyleClass().add("card-detail");

        Label lblCount = new Label("Players: " + count + "/4");
        lblCount.getStyleClass().add("card-detail");

        Button joinBtn = new Button("JOIN FATE");
        joinBtn.getStyleClass().add("join-button");
        joinBtn.setOnAction(e -> ClientConnection.getInstance().sendMessage("JOIN:" + name));

        card.getChildren().addAll(lblName, lblMaster, lblCount, joinBtn);
        return card;
    }
}