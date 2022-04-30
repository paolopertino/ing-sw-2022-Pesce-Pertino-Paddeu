package it.polimi.ingsw.network.utils;

import it.polimi.ingsw.game_controller.CommunicationMessage;
import it.polimi.ingsw.network.client.Client;
import it.polimi.ingsw.observer.Observer;

import java.io.IOException;
import java.util.Timer;

import static it.polimi.ingsw.game_controller.CommunicationMessage.MessageType.PING;
import static it.polimi.ingsw.game_controller.CommunicationMessage.MessageType.PONG;

public class ClientConnectionStatusHandler extends ConnectionStatusHandler implements Observer<CommunicationMessage> {
    private Client clientHandled;

    public ClientConnectionStatusHandler() {
        super();
    }

    public void setClient(Client client) {
        clientHandled = client;
    }

    @Override
    public void run() {
        while (connectionActive) {
            try {
                pingTimer.schedule(new PingTimeoutExceededTask(this), PING_TIMEOUT_DELAY);
                Thread.sleep(10000);
            } catch (InterruptedException sleepError) {
                Logger.ERROR("Connection handler failed to sleep...", sleepError.getMessage());
                abortConnection();
            }
        }
    }

    public void kill() {
        connectionActive = false;
    }

    public boolean isConnectionActive() {
        return connectionActive;
    }

    public void abortConnection() {
        Logger.ERROR( "Connection timed out with the server.", "Ping time limit exceeded");
        Thread.currentThread().interrupt();
        kill();
    }

    @Override
    public void update(CommunicationMessage message) {
        if(message.getID() == PING) {
            clientHandled.asyncWriteToSocket(new CommunicationMessage(PONG, null));
            pingTimer.cancel();
            pingTimer = new Timer();
        }
    }
}