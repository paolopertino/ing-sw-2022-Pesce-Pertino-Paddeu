package it.polimi.ingsw.network.client;

import it.polimi.ingsw.game_controller.CommunicationMessage;
import it.polimi.ingsw.network.utils.ClientConnectionStatusHandler;
import it.polimi.ingsw.network.utils.Logger;
import it.polimi.ingsw.observer.Observable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.NoSuchElementException;

public class Client extends Observable<CommunicationMessage> {
    private final String ip;
    private final int port;
    private boolean active = true;
    private ObjectOutputStream socketOut;
    private String name;
    private ClientConnectionStatusHandler connectionStatusHandler;

    public Client(String ip, int port){
        this.ip = ip;
        this.port = port;
    }

    public synchronized boolean isActive(){
        return connectionStatusHandler.isConnectionActive();
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
                        notify((CommunicationMessage)inputObject);
                    }
                    else {
                        throw new IllegalArgumentException();
                    }
                }
            } catch (Exception e){
                setActive(false);
                Logger.ERROR("Connection interrupted since the socket is now closed server side. Exiting...", e.getMessage());
                //e.printStackTrace();
            }
        });
        t.start();
        return t;
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
        Logger.INFO("Connection established");
        ObjectInputStream socketIn = new ObjectInputStream(socket.getInputStream());
        socketOut = new ObjectOutputStream(socket.getOutputStream());
        connectionStatusHandler = new ClientConnectionStatusHandler();
        connectionStatusHandler.setClient(this);
        this.addObserver(connectionStatusHandler);
        connectionStatusHandler.start();

        try{
            Thread t0 = asyncReadFromSocket(socketIn);
            t0.join();
        } catch(InterruptedException | NoSuchElementException e){
            Logger.INFO("Connection closed from the client side");
        } finally {
            connectionStatusHandler.kill();
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
