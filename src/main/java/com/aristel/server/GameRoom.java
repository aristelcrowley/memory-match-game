package com.aristel.server;

import java.util.*;

public class GameRoom {
    private String roomId;
    private List<ClientHandler> players = new ArrayList<>();
    private ClientHandler roomMaster; 
    
    private List<Integer> board = new ArrayList<>(); 
    private boolean[] matchedCards; 
    private boolean isGameRunning = false;
    private int currentPlayerIndex = 0; 
    private int firstCardIndex = -1; 
    private boolean isWaitingForDelay = false;

    public GameRoom(String id) {
        this.roomId = id;
    }

    public synchronized boolean addPlayer(ClientHandler p) {
        if (players.size() >= 4 || isGameRunning) return false;
        
        p.playerID = players.size();
        players.add(p);

        if (players.size() == 1) {
            roomMaster = p;
        }

        broadcast("MSG:Player " + p.playerID + " connected.");
        
        if (roomMaster != null) {
            broadcast("MSG:Current Room Master is Player " + roomMaster.playerID);
        }
        
        return true;
    }

    public synchronized void removePlayer(ClientHandler p) {
        players.remove(p);
        broadcast("MSG:Player " + p.playerID + " left the room.");

        if (players.isEmpty()) {
            GameServer.removeRoom(this.roomId);
        } else {
            if (p == roomMaster) {
                roomMaster = players.get(0);
                broadcast("MSG:Owner left. Player " + roomMaster.playerID + " is now the Room Master!");
            }
            
            if (isGameRunning && players.size() < 2) {
                isGameRunning = false;
                broadcast("MSG:Not enough players to continue. Game Stopped.");
            }
        }
    }

    public synchronized void startGame(ClientHandler requester) {
        if (requester != roomMaster) {
            requester.sendMessage("ERROR:Only the Room Master (Player " + roomMaster.playerID + ") can start the game.");
            return;
        }

        if (players.size() < 2) {
            broadcast("MSG:Need at least 2 players to start.");
            return;
        }

        int totalCards = players.size() * 10;
        matchedCards = new boolean[totalCards];
        generateBoard(totalCards);

        isGameRunning = true;
        Random rand = new Random();
        currentPlayerIndex = rand.nextInt(players.size());
        
        broadcast("GAME_START:" + totalCards);
        broadcast("MSG:Randomly selected Player " + players.get(currentPlayerIndex).playerID + " to start!");
        broadcast("TURN:" + players.get(currentPlayerIndex).playerID);
    }
    
    private void generateBoard(int size) {
        board.clear();
        for (int i = 0; i < size / 2; i++) {
            board.add(i);
            board.add(i);
        }
        Collections.shuffle(board);
        System.out.println("Room " + roomId + " board generated.");
    }

    public synchronized void processTurn(int playerID, int cardIndex) {
        if (!isGameRunning || isWaitingForDelay) return;
        if (players.get(currentPlayerIndex).playerID != playerID) return; 
        
        if (cardIndex < 0 || cardIndex >= board.size() || matchedCards[cardIndex]) return;
        if (cardIndex == firstCardIndex) return; 

        int imageId = board.get(cardIndex);
        broadcast("FLIP:" + cardIndex + ":" + imageId);

        if (firstCardIndex == -1) {
            firstCardIndex = cardIndex;
        } else {
            int firstImageId = board.get(firstCardIndex);
            
            if (firstImageId == imageId) {
                matchedCards[firstCardIndex] = true;
                matchedCards[cardIndex] = true;
                ClientHandler p = players.get(currentPlayerIndex);
                p.score++;
                broadcast("MATCH:" + p.playerID + ":" + p.score);
                broadcast("MSG:Player " + p.playerID + " found a match and KEEPS the turn!");
                firstCardIndex = -1;
                checkGameOver();
                if (isGameRunning) broadcast("TURN:" + players.get(currentPlayerIndex).playerID);
            } else {
                isWaitingForDelay = true;
                MistakeTimer timer = new MistakeTimer(firstCardIndex, cardIndex);
                firstCardIndex = -1; 
                timer.start();
            }
        }
    }

    public synchronized void finishMismatch(int c1, int c2) {
        broadcast("HIDE:" + c1 + ":" + c2);
        isWaitingForDelay = false;
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        broadcast("MSG:Mismatch! Turn passes to next player.");
        broadcast("TURN:" + players.get(currentPlayerIndex).playerID);
    }

    private void checkGameOver() {
        boolean allMatched = true;
        for (boolean b : matchedCards) {
            if (!b) { allMatched = false; break; }
        }
        if (allMatched) {
            isGameRunning = false;
            broadcast("GAME_OVER");
        }
    }

    private void broadcast(String msg) {
        for (ClientHandler p : players) p.sendMessage(msg);
    }

    public String getRoomId() {
        return roomId;
    }

    public int getPlayerCount() {
        return players.size();
    }

    public String getMasterName() {
        return (roomMaster != null) ? "Player " + roomMaster.playerID : "Unknown";
    }

    private class MistakeTimer extends Thread {
        int card1, card2;
        public MistakeTimer(int c1, int c2) { this.card1 = c1; this.card2 = c2; }
        @Override
        public void run() {
            try { Thread.sleep(2000); } catch (InterruptedException e) {}
            finishMismatch(card1, card2);
        }
    }
}