package it.polimi.ingsw;

import it.polimi.ingsw.client.Client;
import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;

public class ClientApp{
    public static void main(String[] args){
        Client client = new Client("127.0.0.1", 12345, false);

        try{
            client.run();
        }catch (IOException e){
            System.err.println(e.getMessage());
        }

    }
}
