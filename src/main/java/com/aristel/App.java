package com.aristel;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

import com.aristel.network.ClientConnection;


public class App extends Application {

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
            FXMLLoader loader = new FXMLLoader(App.class.getResource(fxmlPath));
            Parent newRoot = loader.load();
            
            if (primaryStage.getScene() == null) {
                Scene scene = new Scene(newRoot);
                primaryStage.setScene(scene);
                primaryStage.setMaximized(true);
            } else {
                primaryStage.getScene().setRoot(newRoot);
            }
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