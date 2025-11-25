package com.aristel.network;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientConnection {
    private static ClientConnection instance;
    private Socket socket;
    private PrintWriter out;
    private ServerListener listenerThread;

    private ClientConnection() {}

    public static ClientConnection getInstance() {
        if (instance == null) {
            instance = new ClientConnection();
        }
        return instance;
    }

    public boolean connect(String ip, int port) {
        try {
            socket = new Socket(ip, port);
            out = new PrintWriter(socket.getOutputStream(), true);

            listenerThread = new ServerListener(socket);
            listenerThread.start();
            
            return true;
        } catch (IOException e) {
            System.err.println("Connection failed: " + e.getMessage());
            return false;
        }
    }

    public void sendMessage(String message) {
        if (out != null) {
            System.out.println("[CLIENT SENT] " + message); 
            out.println(message);
        }
    }

    public void disconnect() {
        try {
            if (listenerThread != null) listenerThread.stopListening();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setMessageListener(IncomingMessageListener listener) {
        if (listenerThread != null) {
            listenerThread.setMessageListener(listener);
        }
    }
}