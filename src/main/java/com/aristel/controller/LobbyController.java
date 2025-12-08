package com.aristel.controller;

import com.aristel.MainApp;
import com.aristel.network.ClientConnection;
import com.aristel.network.IncomingMessageListener;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

public class LobbyController implements IncomingMessageListener {

    @FXML private FlowPane roomContainer;
    @FXML private TextField searchInput;
    @FXML private Label statusLabel;
    
    @FXML private AnchorPane createPopup;
    @FXML private TextField newRoomNameInput;
    @FXML private RadioButton publicRadio;
    @FXML private RadioButton privateRadio;
    @FXML private VBox passwordSection;
    @FXML private PasswordField newRoomPassInput;

    @FXML private AnchorPane errorPopup;
    @FXML private Label errorLabel;
    @FXML private Label errorTitle; 

    @FXML private AnchorPane passwordPopup;
    @FXML private PasswordField joinPassInput;

    @FXML private AnchorPane validationPopup; 

    private ToggleGroup typeGroup;
    private String lastRoomData = ""; 
    private boolean isSearchActive = false;
    private String pendingJoinRoomName = "";

    public void initialize() {
        boolean connected = ClientConnection.getInstance().connect("localhost", 12345);
        if (connected) {
            statusLabel.setText("Connected. Fetching rooms...");
            ClientConnection.getInstance().setMessageListener(this);
            isSearchActive = false;
            ClientConnection.getInstance().sendMessage("GET_ROOMS");
        } else {
            statusLabel.setText("Failed to connect to server!");
        }

        typeGroup = new ToggleGroup();
        publicRadio.setToggleGroup(typeGroup);
        privateRadio.setToggleGroup(typeGroup);
        publicRadio.setSelected(true);
    }

    @FXML private void hideCreatePopup() { 
        createPopup.setVisible(false); 
    }

    @FXML private void hidePasswordPopup() { 
        passwordPopup.setVisible(false); 
    }

    @FXML private void hideErrorPopup() { 
        errorPopup.setVisible(false); 
    }

    @FXML private void hideValidationPopup() { 
        validationPopup.setVisible(false); 
    }

    @FXML private void showCreatePopup() { 
        createPopup.setVisible(true); 
        createPopup.toFront(); 
    }

    @FXML private void handleTypeToggle() {
        boolean isPrivate = privateRadio.isSelected();
        passwordSection.setVisible(isPrivate);
        passwordSection.setManaged(isPrivate);
    }

    @FXML private void handleConfirmCreate() {
        String room = newRoomNameInput.getText();
        String pass = "";

        if (privateRadio.isSelected()) {
            pass = newRoomPassInput.getText().trim();
            if (pass.isEmpty()) {
                validationPopup.setVisible(true);
                validationPopup.toFront();
                return;
            }
        }
        
        if (!room.isEmpty()) {
            ClientConnection.getInstance().sendMessage("CREATE:" + room + ":" + pass);
            hideCreatePopup();
            newRoomNameInput.clear();
            newRoomPassInput.clear();
        }
    }

    private void handleJoinClick(String roomName, String status, int currentPlayers, boolean isPrivate) {
        if (currentPlayers >= 4) {
            showError("ROOM FULL", "There are too many souls in that room already.");
            return;
        }
        if (status.equals("IN GAME")) {
            showError("FATE SEALED", "They done vainly wagered their fates in that room.");
            return;
        }

        if (isPrivate) {
            this.pendingJoinRoomName = roomName;
            joinPassInput.clear();
            passwordPopup.setVisible(true);
            passwordPopup.toFront(); 
        } else {
            ClientConnection.getInstance().sendMessage("JOIN:" + roomName);
        }
    }

    @FXML private void handleConfirmJoinPrivate() {
        String pass = joinPassInput.getText();
        if (!pendingJoinRoomName.isEmpty()) {
            ClientConnection.getInstance().sendMessage("JOIN:" + pendingJoinRoomName + ":" + pass);
            hidePasswordPopup();
        }
    }

    @FXML private void handleSearch() {
        isSearchActive = true;
        ClientConnection.getInstance().sendMessage("GET_ROOMS");
    }
    @FXML private void handleRefresh() {
        isSearchActive = false;
        searchInput.clear();
        ClientConnection.getInstance().sendMessage("GET_ROOMS");
    }

    private void showError(String title, String body) {
        if (errorTitle != null) errorTitle.setText(title);
        if (errorLabel != null) errorLabel.setText(body);
        errorPopup.setVisible(true);
        errorPopup.toFront();
    }

    @Override
    public void onMessageReceived(String rawMessage) {
        String message = rawMessage.trim();

        if (message.startsWith("ROOM_LIST:")) {
            lastRoomData = message.substring(10); 
            String filterText = isSearchActive ? searchInput.getText() : "";
            Platform.runLater(() -> renderRooms(lastRoomData, filterText));
        } 
        else if (message.equals("ERROR:WRONG_PASSWORD")) {
            Platform.runLater(() -> showError("ACCESS DENIED", "Thou art not invited to this destiny."));
        }
        else if (message.equals("ERROR:FULL")) {
            Platform.runLater(() -> showError("ROOM FULL", "There are too many souls in that room already."));
        } 
        else if (message.equals("ERROR:IN_GAME")) {
            Platform.runLater(() -> showError("FATE SEALED", "They done vainly wagered their fates in that room."));
        } else if (message.equals("ERROR:ROOM_EXIST")) {
            Platform.runLater(() -> showError("DESTINY TAKEN", "A realm by that name has already been woven into existence."));
        } else if (message.startsWith("MSG:") || message.startsWith("ERROR:")) {
            Platform.runLater(() -> statusLabel.setText(message.split(":", 2)[1]));
        } 
        else if (message.startsWith("JOINED:")) {
            int myId = Integer.parseInt(message.split(":")[1]);
            ClientConnection.getInstance().myPlayerId = myId;
            Platform.runLater(() -> MainApp.loadView("views/RoomView.fxml"));
        }
    }

    private void renderRooms(String data, String filter) {
        roomContainer.getChildren().clear();
        if (data.isEmpty()) {
            statusLabel.setText("No rooms found. Create one!");
            return;
        }

        String[] rooms = data.split(";");
        boolean foundMatch = false;

        for (String roomStr : rooms) {
            if (roomStr.isEmpty()) continue;
            
            String[] details = roomStr.split(",");
            if (details.length < 5) continue; 

            String name = details[0];
            String master = details[1];
            String countStr = details[2];
            String status = details[3];
            String type = details[4];

            int count = 0;
            try { count = Integer.parseInt(countStr); } catch(NumberFormatException e) {}

            if (filter != null && !filter.isEmpty() && !name.toLowerCase().contains(filter.toLowerCase())) {
                continue;
            }

            VBox card = createRoomCard(name, master, count, status, type);
            roomContainer.getChildren().add(card);
            foundMatch = true;
        }
        
        if (!foundMatch && filter != null && !filter.isEmpty()) statusLabel.setText("No rooms match '" + filter + "'");
        else statusLabel.setText("Rooms Available");
    }

    private VBox createRoomCard(String name, String master, int count, String status, String type) {
        StackPane overlay = new StackPane();

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
        
        Label lblStatus = new Label(status);
        lblStatus.getStyleClass().add("card-detail");
        if (status.equals("IN GAME")) lblStatus.setStyle("-fx-text-fill: #e74c3c;"); 
        else lblStatus.setStyle("-fx-text-fill: #2ecc71;"); 

        Button joinBtn = new Button("JOIN FATE");
        joinBtn.getStyleClass().add("join-button");
        boolean isPrivate = type.equals("PRIVATE");
        
        joinBtn.setOnAction(e -> handleJoinClick(name, status, count, isPrivate));

        card.getChildren().addAll(lblName, lblMaster, lblCount, lblStatus, joinBtn);
        overlay.getChildren().add(card);

        try {
            String iconPath = isPrivate ? "/com/aristel/assets/images/private.png" : "/com/aristel/assets/images/public.png";
            ImageView lockIcon = new ImageView(new Image(getClass().getResourceAsStream(iconPath)));
            lockIcon.setFitWidth(32);
            lockIcon.setFitHeight(32);
            lockIcon.getStyleClass().add("privacy-icon");
            StackPane.setAlignment(lockIcon, Pos.TOP_RIGHT);
            StackPane.setMargin(lockIcon, new javafx.geometry.Insets(10));
            overlay.getChildren().add(lockIcon);
        } catch (Exception e) {}

        return new VBox(overlay);
    }
}