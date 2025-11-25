package com.aristel.terminal;

import java.io.*;
import java.net.Socket;

public class ClientHandler extends Thread {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private GameRoom currentRoom;
    public int playerID;
    public int score = 0;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                String[] parts = inputLine.split(":");
                String command = parts[0];

                if (command.equals("CREATE")) {
                    String roomId = parts[1];
                    boolean created = GameServer.createRoom(roomId);
                    if (created) {
                        sendMessage("MSG:Room '" + roomId + "' created successfully! Now type JOIN:" + roomId);
                    } else {
                        sendMessage("ERROR:Room '" + roomId + "' already exists.");
                    }
                } else if (command.equals("JOIN")) {
                    String roomId = parts[1];
                    GameRoom room = GameServer.findRoom(roomId);
                    
                    if (room == null) {
                        sendMessage("ERROR:Room '" + roomId + "' does not exist. Create it first.");
                    } else {
                        if (room.addPlayer(this)) {
                            this.currentRoom = room;
                            sendMessage("JOINED:" + this.playerID);
                        } else {
                            sendMessage("ERROR:Room is full or game already started.");
                        }
                    }
                } else if (command.equals("START")) {
                    if (currentRoom != null) currentRoom.startGame();
                } else if (command.equals("CLICK")) {
                    if (currentRoom != null) {
                        int cardIndex = Integer.parseInt(parts[1]);
                        currentRoom.processTurn(this.playerID, cardIndex);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Player disconnected");
        } finally {
            try { 
                if (currentRoom != null) {
                    currentRoom.removePlayer(this);
                }
                socket.close(); 
            } catch (IOException e) {}
        }
    }

    public void sendMessage(String msg) {
        if (out != null) out.println(msg);
    }
}