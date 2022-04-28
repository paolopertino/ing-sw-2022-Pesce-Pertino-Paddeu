package it.polimi.ingsw.network.server;

import it.polimi.ingsw.game_controller.CommunicationMessage;
import it.polimi.ingsw.game_model.character.character_utils.DeckType;
import it.polimi.ingsw.network.utils.ConnectionStatusHandler;
import it.polimi.ingsw.network.utils.LobbyInfo;
import it.polimi.ingsw.network.utils.Logger;
import it.polimi.ingsw.observer.Observable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.*;

import static it.polimi.ingsw.game_controller.CommunicationMessage.MessageType.*;

public class SocketClientConnection extends Observable<CommunicationMessage> implements ClientConnection, Runnable {
    private final Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private final Server server;
    private String clientName;
    private final ConnectionStatusHandler connectionStatusHandler;
    private final LinkedList<CommunicationMessage> incomingMessages = new LinkedList<>();

    public SocketClientConnection(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<Lobby> lobbies = server.getActiveGames().stream().filter(lobby -> lobby.getConnectedPlayersToLobby().stream().anyMatch(player -> !player.isActive())).toList();
        for (Lobby lobby : lobbies) {
            lobby.closeLobby(lobby.getConnectedPlayersToLobby().stream().filter(connection -> !connection.isActive()).toList().get(0));
        }

        new Thread(() -> {
            try {
                askName();
            } catch (Exception e) {
                Logger.ERROR("A problem was found during the player setup. Connection aborted.", e.getMessage());
                close();
            }
        }).start();

        this.connectionStatusHandler = new ConnectionStatusHandler(this);
        this.addObserver(connectionStatusHandler);
        connectionStatusHandler.start();
    }

    public synchronized boolean isActive() {
        return connectionStatusHandler.isConnectionActive();
    }

    public synchronized void send(CommunicationMessage message) throws IOException {
        out.reset();
        out.writeObject(message);
        out.flush();
    }

    @Override
    public synchronized void closeConnection() {
        try {
            send(new CommunicationMessage(ERROR, "Connection closed"));
        } catch (IOException e) {
            Logger.WARNING("Cannot send a message to " + clientName + " because his socket is already closed maybe due to a disconnection.");
        }

        try {
            socket.close();
        } catch (IOException e) {
            Logger.ERROR("Error when closing socket!", e.getMessage());
        }
    }

    public void close() {
        if(isActive()) {
            connectionStatusHandler.kill();
            closeConnection();
            Logger.INFO("Unregistering " + clientName + "'s connection...");
            server.deregisterConnection(this);
            Logger.INFO(clientName + " has successfully been unregistered.");
        }
    }

    @Override
    public void asyncSend(final CommunicationMessage message) {
        new Thread(() -> {
            try {
                send(message);
            } catch (IOException e) {
                Logger.ERROR("Failed to async send the message..." + clientName + " will now be closed.", e.getMessage());
                close();
            }
        }).start();
    }

    @Override
    public void run() {
        try {
            while (isActive()) {
                CommunicationMessage message = (CommunicationMessage) in.readObject();
                notify(message);
                if(message.getID() != PONG) {
                    synchronized (incomingMessages) {
                        incomingMessages.add(message);
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            Logger.ERROR(clientName + " connection with the remote host has been interrupted.", e.getMessage());
            // e.printStackTrace();
        } finally {
            close();
        }
    }

    private void askName() throws IOException, ClassNotFoundException {
        String name = null;
        //send(new CommunicationMessage(ASK_NAME, null));

        CommunicationMessage messageReceived = getResponse().get();
        name = messageReceived.getMessage().toString();
        while ((messageReceived.getID() != NAME_MESSAGE) || server.getConnectedPlayersName().contains(name)) {
            send(new CommunicationMessage(REASK_NAME, null));
            name = getResponse().get().getMessage().toString();
        }

        send(new CommunicationMessage(NAME_CONFIRMED, null));
        clientName = name;
        server.newWaitingConnection(this);
        askJoiningAction();
    }

    public void askJoiningAction() throws IOException, ClassNotFoundException {
        int joiningActionChosen; // 0 for creating a new match | 1 for joining an existing one if present

        CommunicationMessage messageReceived =  getResponse().get();
        while (messageReceived.getID() != JOINING_ACTION_INFO) {
            send(new CommunicationMessage(JOINING_ACTION_INFO, null));
            messageReceived = getResponse().get();
        }

        joiningActionChosen = (int)messageReceived.getMessage();

        if (joiningActionChosen == 0) {
            send(new CommunicationMessage(CREATE_LOBBY_ACTION_CONFIRMED, null));
            createNewGame();
        } else {
            if(server.getActiveGames().size() <= 0) {
                send(new CommunicationMessage(ERROR, "No lobbies are available."));
                send(new CommunicationMessage(JOINING_ACTION_INFO, null));
                askJoiningAction();
            } else {
                send(new CommunicationMessage(JOIN_LOBBY_ACTION_CONFIRMED, fetchLobbyInfos()));
                joinExistingGame();
            }
        }
    }


    private void createNewGame() throws IOException, ClassNotFoundException {
        int numberOfPlayer = askGameNumberOfPlayer();
        boolean expertMode = askGameType();
        Lobby newLobby = new Lobby(server, this, numberOfPlayer, expertMode);
        newLobby.registerClientToLobby(this);
        server.addGameLobby(newLobby);
        server.handleLobbyState(newLobby, this);
    }

    private int askGameNumberOfPlayer() throws IOException, ClassNotFoundException {
        int size = 0;

        CommunicationMessage messageReceived = getResponse().get();
        while(messageReceived.getID() != NUMBER_OF_PLAYER_INFO) {
            send(new CommunicationMessage(NUMBER_OF_PLAYER_INFO, null));
            messageReceived = getResponse().get();
        }
        size = (int) messageReceived.getMessage();
        send(new CommunicationMessage(NUMBER_OF_PLAYER_CONFIRMED, null));
        return size;
    }

    private boolean askGameType() throws IOException, ClassNotFoundException {
        boolean mode = false;

        CommunicationMessage messageReceived = getResponse().get();
        while(messageReceived.getID() != GAME_TYPE_INFO) {
            send(new CommunicationMessage(GAME_TYPE_INFO, null));
            messageReceived = getResponse().get();
        }

        mode = (boolean) messageReceived.getMessage();
        send(new CommunicationMessage(LOBBY_JOINED_CONFIRMED, null));

        return mode;
    }

    private void joinExistingGame() throws IOException, ClassNotFoundException {
        CommunicationMessage receivedMessage = getResponse().get();
        while(receivedMessage.getID() != LOBBY_TO_JOIN_INFO) {
            send(new CommunicationMessage(LOBBY_TO_JOIN_INFO, fetchLobbyInfos()));
            receivedMessage = getResponse().get();
        }

        String lobbyChosen = receivedMessage.getMessage().toString();
        Lobby selectedLobby = server.getActiveGames().stream().filter(lobby -> lobby.getLobbyName().equals(lobbyChosen)).toList().get(0);
        if (selectedLobby.isFull()) {
            send(new CommunicationMessage(ERROR, "The lobby you selected is already full or got full while you were choosing."));
            send(new CommunicationMessage(JOINING_ACTION_INFO, null));
            askJoiningAction();
        } else {
            send(new CommunicationMessage(LOBBY_JOINED_CONFIRMED, null));
            selectedLobby.registerClientToLobby(this);
            server.handleLobbyState(selectedLobby, this);
        }
    }

    private List<LobbyInfo> fetchLobbyInfos() {
        List<LobbyInfo> lobbyInfosToSend = new ArrayList<>();
        // Setting up the lobbies to a serializable version
        for (Lobby lobby : server.getActiveGames()) {
            lobbyInfosToSend.add(new LobbyInfo(lobby));
        }

        return lobbyInfosToSend;
    }

    protected DeckType askDeckType(List<DeckType> availableDecks) throws IOException, ClassNotFoundException {
        DeckType type = null;
        send(new CommunicationMessage(ASK_DECK, availableDecks));
        type = (DeckType) getResponse().get().getMessage();

        return type;
    }

    private Optional<CommunicationMessage> getResponse() throws IOException, ClassNotFoundException {
        Optional<CommunicationMessage> message = Optional.empty();
        do {
            synchronized (incomingMessages) {
                message = incomingMessages.size() > 0 ? Optional.of(incomingMessages.removeFirst()) : message;
            }
        } while (message.isEmpty());
        return message;
    }





    public String getClientName() {
        return clientName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SocketClientConnection that = (SocketClientConnection) o;
        return Objects.equals(socket, that.socket) && Objects.equals(out, that.out) && Objects.equals(in, that.in) && Objects.equals(server, that.server) && Objects.equals(clientName, that.clientName);
    }
}