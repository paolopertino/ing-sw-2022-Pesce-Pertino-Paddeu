package it.polimi.ingsw.client;

import it.polimi.ingsw.game_controller.CommunicationMessage;
import it.polimi.ingsw.game_view.GameViewCLI;
import it.polimi.ingsw.game_view.GameViewClient;
import it.polimi.ingsw.game_view.GameViewGUI;
import it.polimi.ingsw.game_view.board.GameBoard;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.NoSuchElementException;

public class Client {
    private final String ip;
    private final int port;
    private final boolean gui;
    private boolean active = true;
    private ObjectOutputStream socketOut;
    private final GameViewClient view;
    private String name;

    public Client(String ip, int port, boolean gui){
        this.ip = ip;
        this.port = port;
        this.gui = gui;
        view = gui ? new GameViewGUI(this) : new GameViewCLI(this);
    }

    public synchronized boolean isActive(){
        return active;
    }

    public synchronized void setActive(boolean active){
        this.active = active;
    }

    public Thread asyncReadFromSocket(final ObjectInputStream socketIn){
        Thread t = new Thread(() -> {
            try {
                while (isActive()) {
                    Object inputObject = socketIn.readObject();
                    if(inputObject instanceof CommunicationMessage){
                        analyzeMessage((CommunicationMessage)inputObject);
                    }
                    else {
                        throw new IllegalArgumentException();
                    }
                }
            } catch (Exception e){
                setActive(false);
            }
        });
        t.start();
        return t;
    }



    private void analyzeMessage(CommunicationMessage message) {
        switch (message.getID()){
            case ASK_NAME   -> view.askName();
            case REASK_NAME -> view.reaskName();
            case ASK_DECK   -> view.askDeck(message.getMessage());
            case ASK_GAME_TYPE -> view.askGameType();
            case ASK_JOINING_ACTION -> view.askJoiningAction();
            case ASK_LOBBY_TO_JOIN -> view.askLobbyToJoin(message.getMessage());
            case ASK_PLAYER_NUMBER -> view.askPlayerNumber();
            case ASSISTANT_NOT_PLAYABLE -> System.out.println("Assistant already played, pick another one!\n");
            case ERROR -> System.out.println(message.getMessage());
            case GAME_READY -> view.gameReady((GameBoard) message.getMessage());
            case VIEW_UPDATE -> view.updateBoardMessage((GameBoard) message.getMessage());
            case YOU_WIN -> System.out.println("YOU WIN!!!");
            case YOU_LOSE -> System.out.println("YOU LOSE!");
        }
    }

    public void asyncWriteToSocket(CommunicationMessage message){
        new Thread(() -> {
            try {
                if (isActive()) {
                    socketOut.writeObject(message);
                    socketOut.flush();
                    socketOut.reset();
                }
            }catch(Exception e){
                setActive(false);
            }
        }).start();
    }

    public void run() throws IOException {
        Socket socket = new Socket(ip, port);
        System.out.println("Connection established");
        ObjectInputStream socketIn = new ObjectInputStream(socket.getInputStream());
        socketOut = new ObjectOutputStream(socket.getOutputStream());

        try{
            Thread t0 = asyncReadFromSocket(socketIn);
            t0.join();
        } catch(InterruptedException | NoSuchElementException e){
            System.out.println("Connection closed from the client side");
        } finally {
            socketIn.close();
            socketOut.close();
            socket.close();
        }
    }

    public String setName(String name){
        this.name = name;
        return this.name;
    }

    public String getName() {
        return name;
    }
}