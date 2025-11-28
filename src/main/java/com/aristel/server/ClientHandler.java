package com.aristel.server;

import java.io.*;
import java.net.Socket;

public class ClientHandler extends Thread {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private GameRoom currentRoom;
    public int playerID;
    public int score = 0;
    public boolean isActive = true;

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
                        sendMessage("MSG:Room '" + roomId + "' created.");
                        
                        GameRoom room = GameServer.findRoom(roomId);
                        if (room.addPlayer(this)) {
                            this.currentRoom = room;
                            sendMessage("JOINED:" + this.playerID);
                            sendMessage("MSG:You are the Room Master!");
                        }
                    } else {
                        sendMessage("ERROR:Room '" + roomId + "' already exists.");
                    }
                } 
                else if (command.equals("GET_ROOMS")) {
                    sendMessage(GameServer.getRoomList());
                }
                else if (command.equals("JOIN")) {
                    String roomId = parts[1];
                    GameRoom room = GameServer.findRoom(roomId);
                    
                    if (room == null) {
                        sendMessage("ERROR:Room does not exist.");
                    } else {
                        if (room.getRoomStatus().equals("IN GAME")) {
                            sendMessage("ERROR:IN_GAME");
                        } else if (room.getPlayerCount() >= 4) {
                            sendMessage("ERROR:FULL");
                        } else {
                            if (room.addPlayer(this)) {
                                this.currentRoom = room;
                                sendMessage("JOINED:" + this.playerID);
                            }
                        }
                    }
                } else if (command.equals("KICK")) {
                    try {
                        int targetId = Integer.parseInt(parts[1]);
                        if (currentRoom != null) {
                            currentRoom.kickPlayer(this, targetId);
                        }
                    } catch (Exception e) { System.out.println("Kick error"); }
                } else if (command.equals("START")) {
                    if (currentRoom != null) {
                        currentRoom.startGame(this); 
                    }
                } else if (command.equals("CLICK")) {
                    if (currentRoom != null) {
                        int cardIndex = Integer.parseInt(parts[1]);
                        currentRoom.processTurn(this.playerID, cardIndex);
                    }
                } else if (command.equals("LEAVE")) {
                    if (currentRoom != null) {
                        currentRoom.removePlayer(this);
                        currentRoom = null;
                        sendMessage("LEFT_ROOM");
                    }
                } else if (command.equals("GET_STATE")) {
                    if (currentRoom != null) {
                        currentRoom.sendStateToPlayer(this);
                    }
                } else if (command.equals("GET_GAME_STATE")) {
                    if (currentRoom != null) {
                        currentRoom.sendGameState(this);
                    }
                } else if (command.equals("RESET_GAME")) {
                    if (currentRoom != null) {
                        currentRoom.stopGame();
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Player disconnected");
        } finally {
            try { 
                if (currentRoom != null) currentRoom.removePlayer(this);
                socket.close(); 
            } catch (IOException e) {}
        }
    }

    public void sendMessage(String msg) {
        if (isActive && out != null) out.println(msg);
    }
}