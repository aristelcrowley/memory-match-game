package com.aristel;

import com.aristel.network.ClientConnection;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        
        loadView("views/LobbyView.fxml");
        
        stage.setTitle("Memory Match Game Using Socket");
        stage.show();
    }

    public static void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            
            scene.getStylesheets().add(MainApp.class.getResource("styles/style.css").toExternalForm());
            
            primaryStage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Could not load FXML: " + fxmlPath);
        }
    }

    @Override
    public void stop() {
        System.out.println("Application closing, disconnecting socket...");
        ClientConnection.getInstance().disconnect();
    }

    public static void main(String[] args) {
        launch(args);
    }
}