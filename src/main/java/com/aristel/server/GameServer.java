package com.aristel.server;

import java.io.*;
import java.net.*;
import java.util.*;

public class GameServer {
    private static final int PORT = 12345;
    private static Map<String, GameRoom> rooms = new HashMap<>();

    public static void main(String[] args) {
        System.out.println("Memory Game Server running on port " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket);
                handler.start(); 
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static synchronized boolean createRoom(String roomId) {
        if (rooms.containsKey(roomId)) {
            return false;
        }
        rooms.put(roomId, new GameRoom(roomId));
        System.out.println("Created new room: " + roomId);
        return true;
    }

    public static synchronized GameRoom findRoom(String roomId) {
        return rooms.get(roomId);
    }

    public static synchronized void removeRoom(String roomId) {
        if (rooms.containsKey(roomId)) {
            rooms.remove(roomId);
            System.out.println("Room '" + roomId + "' is empty and has been deleted.");
        }
    }

    // --- NEW METHOD FOR LOBBY ---
    public static synchronized String getRoomList() {
        if (rooms.isEmpty()) return "ROOM_LIST:";
        
        StringBuilder sb = new StringBuilder("ROOM_LIST:");
        for (GameRoom room : rooms.values()) {
            sb.append(room.getRoomId()).append(",")
              .append(room.getMasterName()).append(",")
              .append(room.getPlayerCount()).append(";");
        }
        return sb.toString();
    }
}