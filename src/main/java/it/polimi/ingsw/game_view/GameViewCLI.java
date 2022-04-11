package it.polimi.ingsw.game_view;

import it.polimi.ingsw.client.Client;
import it.polimi.ingsw.game_controller.CommunicationMessage;
import it.polimi.ingsw.game_controller.action.*;
import it.polimi.ingsw.game_model.character.character_utils.DeckType;
import it.polimi.ingsw.game_view.board.DeckBoard;
import it.polimi.ingsw.game_view.board.GameBoard;
import it.polimi.ingsw.game_view.board.IslandBoard;
import it.polimi.ingsw.game_view.board.SchoolBoard;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import static it.polimi.ingsw.game_controller.CommunicationMessage.MessageType.*;

public class GameViewCLI extends GameViewClient{
    private final Scanner input;
    private int rangeA, rangeB, holder1;

    public GameViewCLI(Client client) {
        super(client);
        input = new Scanner(System.in);
    }

    @Override
    public void askName() {
        System.out.println(GameViewClient.ASK_NAME_QUESTION);
        client.asyncWriteToSocket(new CommunicationMessage(ASK_NAME, client.setName(input.nextLine())));
    }

    @Override
    public void reaskName() {
        System.out.println(GameViewClient.REASK_NAME_QUESTION);
        client.asyncWriteToSocket(new CommunicationMessage(REASK_NAME, client.setName(input.nextLine())));
    }

    @Override
    public void askDeck(Object decksAvailable) {
        System.out.println(GameViewClient.ASK_DECK_TYPE_QUESTION + decksToString((List<?>)decksAvailable));
        client.asyncWriteToSocket(new CommunicationMessage(
                ASK_DECK,
                ((List<?>) decksAvailable).get(whileInputNotIntegerInRange(0, ((List<?>) decksAvailable).size()-1))
        ));
    }

    @Override
    public void askGameType() {
        System.out.println(GameViewClient.ASK_GAME_TYPE_QUESTION);
        client.asyncWriteToSocket(new CommunicationMessage(
                ASK_GAME_TYPE,
                whileInputNotContainedInt(Arrays.asList("e", "n")).equals("e")));
    }

    @Override
    public void askPlayerNumber() {
        System.out.println(GameViewClient.ASK_PLAYER_NUMBER_QUESTION);
        client.asyncWriteToSocket(new CommunicationMessage(
                ASK_GAME_TYPE,
                whileInputNotIntegerInRange(2, 4)));
    }

    @Override
    public void gameReady(GameBoard board){
        updateBoardMessage(board);
        state = InputStateMachine.PLANNING_PHASE_START;
        asyncReadInput();
    }

    @Override
    public void updateBoard(GameBoard board) {
        System.out.println(board.print());
    }

    public void asyncReadInput(){
        new Thread(() -> {
            if(board.getCurrentlyPlaying().equals(client.getName())){
                stateMachine(0);
            }
            while(client.isActive()){
                String inputValue = input.nextLine();
                if(!board.getCurrentlyPlaying().equals(client.getName())){
                    displayNotYourTurn();
                }
                else if(!actionSent) {
                    if(state.toString().endsWith("SEND_MESSAGE") || state.toString().endsWith("SECOND_PHASE")){
                        if(inputValue.isEmpty() || !inputValue.chars().allMatch(Character::isDigit) ||
                                Integer.parseInt(inputValue) < rangeA || Integer.parseInt(inputValue) > rangeB){
                            inputValue = String.valueOf(whileInputNotIntegerInRange(rangeA, rangeB));
                        }
                        stateMachine(Integer.parseInt(inputValue));
                    }
                    else{
                        stateMachine(-1);
                    }
                }
            }
        }).start();
    }

    @Override
    public void displayNotYourTurn(){
        System.out.println("PLEASE WAIT FOR YOUR TURN");
    }

    @Override
    public void displayYourTurn() {
        System.out.println("IT'S YOUR TURN! [PRESS ENTER TO START]");
    }

    @Override
    public void displayExpertMode() {
        System.out.println("The game is played in expert mode to play a special card, write \"play\" in any moment");
        System.out.println(board.getTerrain().getAdvancedCard());
    }

    private int whileInputNotIntegerInRange(int a, int b){
        String read = input.nextLine();
        while(read.isEmpty() || !read.chars().allMatch(Character::isDigit) ||
                Integer.parseInt(read) < a || Integer.parseInt(read) > b){
            System.out.println("Not a number or not in the range given");
            read = input.nextLine();
        }
        return Integer.parseInt(read);
    }

    private String whileInputNotContainedInt(List<String> container){
        String read = input.nextLine();
        while(!container.contains(read)){
            System.out.println("Selection not available please follow instruction");
            read = input.nextLine();
        }
        return read;
    }

    private String decksToString(List<?> decks){
        String msg = "";
        for(int i = 0; i < decks.size(); i++){
            msg = msg.concat("\n" + i + " = " + ((DeckType)decks.get(i)).getName());
        }
        return msg;
    }

    protected void stateMachine(int selection){
        DeckBoard playerDeck = board.getDecks().get(board.getNames().indexOf(client.getName()));
        SchoolBoard school = board.getSchools().get(board.getNames().indexOf(client.getName()));
        List<IslandBoard> islands = board.getTerrain().getIslands();

        switch(state) {
            case PLANNING_PHASE_START:
                rangeA = 0;
                rangeB = playerDeck.getCards().size() - 1;
                System.out.println("Select an assistant card to play (use value from 0 to " + rangeB
                        + " to select the card):\n" + playerDeck.print());
                state = InputStateMachine.SELECT_ASSISTANT_CARD_SEND_MESSAGE;
                break;

            case SELECT_ASSISTANT_CARD_SEND_MESSAGE:
                System.out.println("You selected: " + playerDeck.getCards().get(selection).getName());
                client.asyncWriteToSocket(new CommunicationMessage(GAME_ACTION, new PlayAssistantCardAction(client.getName(), selection)));
                actionSent = true;
                break;

            case MOVING_STUDENT_PHASE_START:
                rangeA = 0;
                rangeB = school.getEntrance().size() - 1;
                System.out.println("Please select a student to move (use number from 0 to " + rangeB
                        + " starting counting from left to right and top to bottom");
                state = InputStateMachine.MOVE_STUDENT_SECOND_PHASE;
                break;

            case MOVE_STUDENT_SECOND_PHASE:
                rangeA = 0;
                rangeB = islands.size();
                System.out.println("You selected student " + selection
                        + GameBoard.getColorString(school.getEntrance().get(selection))
                        + GameBoard.STUDENT + GameBoard.TEXT_RESET);
                System.out.println("Please select where to move the student (use number from 0 to " +
                        rangeB + " to select an island and number " + rangeB + " to select the dining hall)");
                holder1 = selection;
                state = InputStateMachine.MOVE_STUDENT_SEND_MESSAGE;
                break;

            case MOVE_STUDENT_SEND_MESSAGE:
                if(selection == islands.size()){
                    System.out.println("Dining hall selected");
                    client.asyncWriteToSocket(new CommunicationMessage(GAME_ACTION, new MoveStudentToDiningHallAction(client.getName(), holder1)));
                }
                else {
                    System.out.println("Island " + selection + " selected");
                    client.asyncWriteToSocket(new CommunicationMessage(GAME_ACTION, new MoveStudentToIslandAction(client.getName(), holder1, selection)));
                }
                actionSent = true;
                break;

            case MOVE_MOTHER_NATURE_START:
                rangeA = 1;
                rangeB = playerDeck.getDiscardedCard().getPossibleSteps();
                System.out.println("How many step would you like to move mother nature (use a number between 1 and " + rangeB + ")");
                state = InputStateMachine.MOVE_MOTHER_NATURE_SEND_MESSAGE;
                break;

            case MOVE_MOTHER_NATURE_SEND_MESSAGE:
                client.asyncWriteToSocket(new CommunicationMessage(GAME_ACTION, new MoveMotherNatureAction(client.getName(), selection)));
                actionSent = true;
                break;

            case CHOOSE_CLOUD_CARD_START:
                rangeA = 0;
                rangeB = board.getTerrain().getCloudCards().size() - 1;
                System.out.println("Select a Cloud from where to pick student (use number from 0 to" + rangeB
                        + " to select the cloud):\n");
                state = InputStateMachine.CHOOSE_CLOUD_CARD_SEND_MESSAGE;

            case CHOOSE_CLOUD_CARD_SEND_MESSAGE:
                if(board.getTerrain().getCloudCards().get(selection).isEmpty()){
                    System.out.println("You selected an empty cloud, please pick another one");
                }
                else {
                    client.asyncWriteToSocket(new CommunicationMessage(GAME_ACTION, new ChooseCloudCardAction(client.getName(), selection)));
                    actionSent = true;
                }
        }
    }
}
