package it.polimi.ingsw.server;

import it.polimi.ingsw.game_controller.CommunicationMessage;
import it.polimi.ingsw.game_controller.GameController;
import it.polimi.ingsw.game_model.Game;
import it.polimi.ingsw.game_model.GameExpertMode;
import it.polimi.ingsw.game_model.Player;
import it.polimi.ingsw.game_model.character.character_utils.DeckType;
import it.polimi.ingsw.game_view.RemoteGameView;
import it.polimi.ingsw.game_view.board.GameBoard;
import it.polimi.ingsw.game_view.board.GameBoardAdvanced;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static it.polimi.ingsw.game_controller.CommunicationMessage.MessageType.ERROR;
import static it.polimi.ingsw.game_controller.CommunicationMessage.MessageType.GAME_READY;

public class Server {
    private static final int PORT = 12345;
    private final ServerSocket serverSocket;
    private final List<ClientConnection> waitingConnection = new ArrayList<>();
    private final List<Lobby> activeGames = new ArrayList<>();

    private int numberOfPlayer = 0;
    private boolean expertMode = false;

    public List<ClientConnection> getWaitingConnection() {
        return waitingConnection;
    }

    //Deregister connection
    public synchronized void deregisterConnection(ClientConnection c) {
        /*
        Optional<List<ClientConnection>> opponent = playingConnection.stream().reduce((list1, list2) -> list1.contains(c) ? list1 : list2);
        opponent.ifPresent(list -> {
            playingConnection.remove(list);
            list.forEach(ClientConnection::closeConnection);
        });

        waitingConnection.remove(c);
        */
    }

    //Wait for other players
    public synchronized void handleLobbyState(Lobby lobbyToHandle){
        if(lobbyToHandle.isFull()) {
            // The game is startable
            new Thread(lobbyToHandle).start();
        } else {
            // Notify all the lobby partecipants that a new player has joined
            for(ClientConnection lobbyPartecipant : lobbyToHandle.getConnectedPlayersToLobby()) {
                ((SocketClientConnection)lobbyPartecipant).send(new CommunicationMessage(ERROR, lobbyToHandle.getLastJoined() + " has joined the lobby."));
            }
        }
        /*
        List<String> keys = new ArrayList<>(waitingConnection.keySet());

        waitingConnection.put(name, c);
        if(waitingConnection.size() == 1){
            numberOfPlayer = ((SocketClientConnection)c).askGameNumberOfPlayer();
            expertMode = ((SocketClientConnection)c).askGameType();;
        }

        keys = new ArrayList<>(waitingConnection.keySet());

        if (waitingConnection.size() == numberOfPlayer) {
            var game = expertMode ? new GameExpertMode(numberOfPlayer) : new Game(numberOfPlayer);
            GameController controller = new GameController(game);
            for(String playerName: keys){
                ClientConnection connection = waitingConnection.get(playerName);
                DeckType deck = ((SocketClientConnection)connection).askDeckType(controller.getAvailableDeckType());
                Player player = controller.createPlayer(playerName, deck);

                RemoteGameView view = new RemoteGameView(player.getNickname(), connection);
                game.addObserver(view);
                view.addObserver(controller);

                executor.submit(((SocketClientConnection)connection));
            }
            for(String playerName: keys) {
                ((SocketClientConnection) waitingConnection.get(playerName)).send(
                        new CommunicationMessage(GAME_READY, expertMode ? new GameBoardAdvanced(game) : new GameBoard(game))
                );
            }
                playingConnection.add(waitingConnection.values().stream().toList());
            waitingConnection.clear();
        }
        else{
            ((SocketClientConnection)c).send(new CommunicationMessage(ERROR, "Waiting for other players"));

            for (String key : keys) {
                ClientConnection connection = waitingConnection.get(key);
                ((SocketClientConnection)connection).send(new CommunicationMessage(ERROR, "Lobby: " + name));
            }
        }
       */
    }

    public Server() throws IOException {
        this.serverSocket = new ServerSocket(PORT);
    }

    public void run() throws IOException {
        int connections = 0;
        boolean running = true;
        System.out.println("Server is running");

        while(running){
            try {
                Socket newSocket = serverSocket.accept();
                connections++;
                System.out.println("Ready for the new connection - " + connections);
                SocketClientConnection socketConnection = new SocketClientConnection(newSocket, this);
            } catch (IOException e) {
                running = false;
                serverSocket.close();
                System.out.println("Connection Error!");
            }
        }
    }

    public synchronized Set<String> getConnectedPlayersName() {
        Set<String> currentlyPlayingNicknames = new HashSet<>();

        for (ClientConnection connection : waitingConnection) {
            currentlyPlayingNicknames.add(((SocketClientConnection)connection).getClientName());
        }

        for(Lobby gameLobby : activeGames) {
            for(ClientConnection player : gameLobby.getConnectedPlayersToLobby()) {
                currentlyPlayingNicknames.add(((SocketClientConnection)player).getClientName());
            }
        }

        return currentlyPlayingNicknames;
    }

    public synchronized void addGameLobby(Lobby newLobby) {
        activeGames.add(newLobby);
    }

    public synchronized List<Lobby> getActiveGames() {
        return activeGames;
    }

    public void newWaitingConnection(ClientConnection connection) {
        synchronized (this) {
            waitingConnection.add(connection);
        }

        ((SocketClientConnection)connection).askJoiningAction();
    }
}