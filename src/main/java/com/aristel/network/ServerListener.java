package com.aristel.network;

import javafx.application.Platform;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ServerListener extends Thread {
    private Socket socket;
    private BufferedReader in;
    private volatile boolean running = true;
    private IncomingMessageListener currentListener; 

    public ServerListener(Socket socket) {
        this.socket = socket;
    }

    public void setMessageListener(IncomingMessageListener listener) {
        this.currentListener = listener;
    }

    public void stopListening() {
        running = false;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String message;

            while (running && (message = in.readLine()) != null) {
                System.out.println("[CLIENT RECEIVED] " + message); 

                if (currentListener != null) {
                    final String finalMsg = message;
                    Platform.runLater(() -> {
                        currentListener.onMessageReceived(finalMsg);
                    });
                }
            }
        } catch (IOException e) {
            System.out.println("Connection lost or closed.");
        }
    }
}