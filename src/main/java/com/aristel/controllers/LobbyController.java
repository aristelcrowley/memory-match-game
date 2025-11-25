package com.aristel.controllers;

import com.aristel.MainApp;
import com.aristel.network.ClientConnection;
import com.aristel.network.IncomingMessageListener;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class LobbyController implements IncomingMessageListener {

    @FXML private TextField roomInput;
    @FXML private Label statusLabel;

    public void initialize() {
        boolean connected = ClientConnection.getInstance().connect("localhost", 12345);
        if (connected) {
            statusLabel.setText("Connected to Server. Create or Join a room.");
            ClientConnection.getInstance().setMessageListener(this);
        } else {
            statusLabel.setText("Failed to connect to server!");
        }
    }

    @FXML
    private void handleCreate() {
        String room = roomInput.getText();
        if (!room.isEmpty()) {
            ClientConnection.getInstance().sendMessage("CREATE:" + room);
        }
    }

    @FXML
    private void handleJoin() {
        String room = roomInput.getText();
        if (!room.isEmpty()) {
            ClientConnection.getInstance().sendMessage("JOIN:" + room);
        }
    }

    @Override
    public void onMessageReceived(String message) {
        if (message.startsWith("MSG:") || message.startsWith("ERROR:")) {
            String text = message.split(":", 2)[1];
            statusLabel.setText(text);
        } 
        else if (message.startsWith("JOINED:")) {
            Platform.runLater(() -> {
                System.out.println("Join successful, switching to GameBoard...");
                MainApp.loadView("views/GameBoardView.fxml");
            });
        }
    }
}