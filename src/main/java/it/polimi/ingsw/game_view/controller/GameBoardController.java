package it.polimi.ingsw.game_view.controller;

import it.polimi.ingsw.game_controller.CommunicationMessage;
import it.polimi.ingsw.game_controller.action.PlayAssistantCardAction;
import it.polimi.ingsw.game_model.character.character_utils.AssistantType;
import it.polimi.ingsw.game_view.board.GameBoard;
import it.polimi.ingsw.game_view.board.IslandBoard;
import it.polimi.ingsw.network.client.Client;
import javafx.animation.RotateTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class GameBoardController implements Initializable {
    private Client client;
    private String clientName;
    private boolean firstTime = true;
    private final boolean[] isUp = {false, false, false, false, false, false, false, false, false, false};
    private final IntegerProperty showedBoard = new SimpleIntegerProperty(0);
    final RotateTransition rotateTransition = new RotateTransition();
    private final List<Button> playerBoardButtons = new ArrayList<>();
    private final List<ImageView> assistants = new ArrayList<>();
    private final List<IslandController> islands = new ArrayList<>();
    @FXML
    ImageView assistant1, assistant2, assistant3, assistant4, assistant5, assistant6, assistant7, assistant8, assistant9, assistant10;
    @FXML
    private Button player1Board, player2Board, player3Board, player4Board;
    @FXML
    private StackPane mainPane;
    @FXML
    private HBox cards;
    @FXML
    private IslandController island0Controller, island1Controller, island2Controller, island3Controller, island4Controller, island5Controller, island6Controller, island7Controller, island8Controller, island9Controller, island10Controller, island11Controller;
    @FXML
    private RotatingBoardController rotatingBoardController;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        playerBoardButtons.addAll(Arrays.asList(player1Board, player2Board, player3Board, player4Board));
        islands.addAll(Arrays.asList(island0Controller, island1Controller, island2Controller, island3Controller, island4Controller, island5Controller, island6Controller, island7Controller, island8Controller, island9Controller, island10Controller, island11Controller));
        mainPane.setBackground(new Background(new BackgroundImage(new Image("img/table.jpg"), BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, new BackgroundSize(1, 1, false, false, true, true))));

        assistants.addAll(Arrays.asList(assistant1, assistant2, assistant3, assistant4, assistant5, assistant6, assistant7, assistant8, assistant9, assistant10));

        rotateTransition.setAxis(Rotate.Z_AXIS);
        rotateTransition.setCycleCount(1);
        rotateTransition.setDuration(Duration.millis(1500));
        rotateTransition.setAutoReverse(false);
        rotateTransition.setNode(rotatingBoardController.getPane());
    }

    public void setClient(Client client) {
        this.client = client;
        clientName = client.getName();
        showedBoard.addListener((observable, oldValue, newValue) -> new Thread(() -> setUpDecks((Integer) newValue)).start());
    }

    public void setClientName(String name){
        //TODO delete, just for testing purpose
        this.clientName = name;
        showedBoard.addListener((observable, oldValue, newValue) -> new Thread(() -> setUpDecks((Integer) newValue)).start());

    }

    private void setUpDecks(int pos){
        if(rotatingBoardController.getBoardX(pos).getName().getText().equals(clientName)){
            setAssistantsCardsFront();
        }
        else {
            setAssistantsCardsRetro(new Image(rotatingBoardController.getBoardX(pos).getDeckBoard().getDeckType().getPath()));
        }
    }

    private void setAssistantsCardsRetro(Image image){
        for(int i = 0; i < assistants.size(); i++){
            assistants.get(i).setImage(image);
            cards.setTranslateY(assistants.get(i).getFitHeight() * 0.8);
            setGoUpEffectOnAssistantCard(assistants.get(i), i);
            setGoDownEffectOnAssistantCard(assistants.get(i), i);
        }
    }

    private void setAssistantsCardsFront(){
        cards.setTranslateY(assistants.get(0).getFitHeight() * 0.8);
        for(int i = 0; i < assistants.size(); i++){
            ImageView assistant = assistants.get(i);
            assistant.setImage(new Image("img/assistant/Assistente (" + (i + 1) + ").png"));

            ColorAdjust ca = new ColorAdjust();
            if(!rotatingBoardController.getBoardOfPlayerWithName(clientName).getDeckBoard().getCards().stream().map(AssistantType::getCardTurnValue).toList().contains(i + 1)){
                ca.setBrightness(-0.5);
                assistant.setEffect(ca);
                if(assistant.getOnMouseEntered() != null){
                    assistant.removeEventHandler(MouseEvent.MOUSE_ENTERED, assistant.getOnMouseEntered());
                }
                if(assistant.getOnMouseExited() != null){
                    assistant.removeEventHandler(MouseEvent.MOUSE_EXITED, assistant.getOnMouseExited());
                }
            }
            else{
                ca.setBrightness(0);
                assistant.setEffect(ca);
                setGoUpEffectOnAssistantCard(assistant, i);
                setGoDownEffectOnAssistantCard(assistant, i);
                int finalI = i;
                assistant.setOnMouseClicked(ActionEvent -> client.asyncWriteToSocket(
                        new CommunicationMessage(CommunicationMessage.MessageType.GAME_ACTION, new PlayAssistantCardAction(clientName, getAssistantTypeIndex(finalI)))));
            }
        }
    }

    private int getAssistantTypeIndex(int value){
        for(int i = 0; i < rotatingBoardController.getBoardOfPlayerWithName(clientName).getDeckBoard().getCards().size(); i++){
            if(rotatingBoardController.getBoardOfPlayerWithName(clientName).getDeckBoard().getCards().get(i).getCardTurnValue() == value){
                return i;
            }
        }
        //not reachable the card clickable are also the card playable so the value will always be found in the array
        return 0;
    }

    private void setGoUpEffectOnAssistantCard(ImageView assistant, int i){
        TranslateTransition moveUpEffect = new TranslateTransition(Duration.millis(500), assistant);
        assistant.setOnMouseEntered(ActionEvent -> new Thread(() -> {
            if(!isUp[i]){
                moveUpEffect.setByY(- assistant.getFitHeight() * 0.9);
                moveUpEffect.play();
                moveUpEffect.setOnFinished(a -> isUp[i] = true);
            }
        }).start());
    }

    private void setGoDownEffectOnAssistantCard(ImageView assistant, int i){
        TranslateTransition moveDownEffect = new TranslateTransition(Duration.millis(500), assistant);
        assistant.setOnMouseExited(ActionEvent -> new Thread(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // Impossible to reach since the animation duration is ALWAYS > 0
                e.printStackTrace();
            }
            if(isUp[i]){
                moveDownEffect.setByY(assistant.getFitHeight() * 0.9);
                moveDownEffect.play();
                moveDownEffect.setOnFinished(a -> isUp[i] = false);
            }
        }).start());
    }



    public void updateBoard(GameBoard board){
        rotatingBoardController.update(board);
        if(firstTime){
            firstTime = false;
            for(int i = 0; i < 4; i++){
                if(i < board.getNames().size()){
                    playerBoardButtons.get(i).setText(board.getNames().get(i));
                    int finalI = i;
                    playerBoardButtons.get(i).setOnAction(ActionEvent -> {
                        synchronized (rotateTransition) {
                            setRotatingButtonDisabled(true);
                            rotateTransition.setByAngle(getDegreeTurn(finalI));
                            showedBoard.set(finalI);
                            rotateTransition.setOnFinished(a -> {
                                setRotatingButtonDisabled(false);
                                playerBoardButtons.get(finalI).setDisable(true);
                                playerBoardButtons.get(finalI).setDisable(true);
                            });
                            rotateTransition.play();
                        }
                    });
                }
                else {
                    rotatingBoardController.getBoardX(i).hide();
                    playerBoardButtons.get(i).setDisable(true);
                    playerBoardButtons.get(i).setVisible(false);
                }
            }
            setUpDecks(showedBoard.get());
            //TODO delete just for testing purpose
            makeStudentEntranceSelectable();
        }
        for(int i = 0; i < board.getNames().size(); i++){
            if(playerBoardButtons.get(i).getText().equals(clientName)){
                playerBoardButtons.get(i).fire();
            }
        }
        for (IslandController island: islands){
            island.hide();
        }

        for(IslandBoard island: board.getTerrain().getIslands()){
            islands.get(island.getID()).unHide();
            islands.get(island.getID()).update(island);
        }
    }

    public void makeStudentEntranceSelectable(){
        List<ImageView> entranceStudents = rotatingBoardController.getBoardOfPlayerWithName(clientName).getSchool().getEntranceStudents();
        for(int i = 0; i < entranceStudents.size(); i++){
            setHoverEffect(entranceStudents.get(i), entranceStudents.get(i).getFitHeight() / 2 + 5);
            int finalI = i;
            entranceStudents.get(i).setOnMouseClicked(actionEvent -> {
                for(int k = 0; k < entranceStudents.size(); k++) {
                    resetHoverEffect(entranceStudents.get(k));
                    if(finalI == k) {
                        DropShadow shadow = new DropShadow();
                        shadow.setColor(Color.YELLOW);
                        shadow.setRadius(entranceStudents.get(k).getFitHeight() / 2 + 5);
                        entranceStudents.get(k).setEffect(shadow);
                    }
                }
                makeVisibleIslandsSelectable();
            });
        }
    }

    public void makeVisibleIslandsSelectable(){
        for(int i = 0; i < islands.size(); i++){
            if(islands.get(i).isVisible()){
                setHoverEffect(islands.get(i).getIsland(), islands.get(i).getIsland().getFitWidth() / 2);
            }
        }
    }

    private int getDegreeTurn(int finalPos){
        return Math.floorMod(finalPos - showedBoard.get(), 4) == 3 ? 90 : Math.floorMod(finalPos - showedBoard.get(), 4) == 2 ? 180 : Math.floorMod(finalPos - showedBoard.get(), 4) == 1 ? -90 : 0;
    }

    private void setRotatingButtonDisabled(boolean value){
        for(Button playerButton: playerBoardButtons){
            playerButton.setDisable(value);
        }
    }

    private void setHoverEffect(Node node, double radius){
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.YELLOW);
        shadow.setRadius(radius);
        node.setOnMouseEntered(ActionEvent -> node.setEffect(shadow));
        node.setOnMouseExited(ActionEvent -> node.setEffect(null));
    }

    private void resetHoverEffect(Node node){
        node.setOnMouseEntered(null);
        node.setOnMouseExited(null);
    }
}
