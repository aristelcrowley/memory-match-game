package com.aristel.terminal;

import java.util.*;

public class GameRoom {
    private String roomId;
    private List<ClientHandler> players = new ArrayList<>();
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
        broadcast("MSG:Player " + p.playerID + " connected. Total: " + players.size());
        return true;
    }

    public synchronized void removePlayer(ClientHandler p) {
        players.remove(p);
        broadcast("MSG:Player " + p.playerID + " left the room.");
        
        if (players.isEmpty()) {
            GameServer.removeRoom(this.roomId);
        } else {
            if (isGameRunning && players.size() < 2) {
                isGameRunning = false;
                broadcast("MSG:Not enough players. Game Stopped.");
            }
        }
    }

    public synchronized void startGame() {
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
        
        if (players.get(currentPlayerIndex).playerID != playerID) {
            return; 
        }
        
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
                
                if (isGameRunning) {
                    broadcast("TURN:" + players.get(currentPlayerIndex).playerID);
                }

            } else {
                isWaitingForDelay = true;
                int c1 = firstCardIndex;
                int c2 = cardIndex;
                firstCardIndex = -1;

                MistakeTimer timer = new MistakeTimer(c1, c2);
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

    private class MistakeTimer extends Thread {
        int card1, card2;

        public MistakeTimer(int c1, int c2) {
            this.card1 = c1;
            this.card2 = c2;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(2000); 
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            finishMismatch(card1, card2);
        }
    }
}