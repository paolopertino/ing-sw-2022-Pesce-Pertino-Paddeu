package it.polimi.ingsw.network.utils;

import it.polimi.ingsw.game_controller.CommunicationMessage;
import it.polimi.ingsw.network.server.SocketClientConnection;
import it.polimi.ingsw.observer.Observer;

import java.io.IOException;
import java.util.Timer;

import static it.polimi.ingsw.game_controller.CommunicationMessage.MessageType.PING;
import static it.polimi.ingsw.game_controller.CommunicationMessage.MessageType.PONG;

public class ServerConnectionStatusHandler extends ConnectionStatusHandler implements Observer<CommunicationMessage> {
    private SocketClientConnection connection;

    public ServerConnectionStatusHandler() {
        super();
    }

    public void setConnection(SocketClientConnection connectionToHandle) {
        connection = connectionToHandle;
    }

    @Override
    public void run() {
        while (connectionActive) {
            try {
                connection.send(new CommunicationMessage(PING, null));
                pingTimer.schedule(new PingTimeoutExceededTask(this), 2*PING_TIMEOUT_DELAY);
                Thread.sleep(PING_TIMEOUT_DELAY);
            } catch (InterruptedException sleepError) {
                Logger.ERROR("Connection handler failed to sleep...", sleepError.getMessage());
                abortConnection();
            } catch(IOException sendError) {
                Logger.ERROR("Failed to send the ping message.", sendError.getMessage());
                abortConnection();
            } catch (IllegalStateException alreadyCanceled) {
                // If the schedule was already been canceled, everything it's ok.
            }
        }
    }

    @Override
    public boolean isConnectionActive() {
        return connectionActive;
    }

    @Override
    public void abortConnection() {
        Logger.ERROR(connection.getClientName() + "'s connection aborted due to max ping limit exceeded.", "Ping time limit exceeded");
        Thread.currentThread().interrupt();
        connection.close();
        kill();
    }

    @Override
    public void kill() {
        connectionActive = false;
    }

    @Override
    public void update(CommunicationMessage message) {
        if(message.getID() == PONG) {
            //Logger.INFO(new Timestamp(new Date().getTime()) + " - Received pong from " + connection.getClientName());
            pingTimer.cancel();
            pingTimer = new Timer();
        }
    }
}
