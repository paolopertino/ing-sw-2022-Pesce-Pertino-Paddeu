package it.polimi.ingsw.network.client;

import it.polimi.ingsw.game_controller.CommunicationMessage;
import it.polimi.ingsw.game_view.GameViewClient;
import it.polimi.ingsw.game_view.board.GameBoard;
import it.polimi.ingsw.game_view.board.Printable;
import it.polimi.ingsw.network.utils.Logger;
import it.polimi.ingsw.observer.Observer;

import static it.polimi.ingsw.game_controller.CommunicationMessage.MessageType.PONG;

public class ClientMessageObserverHandler implements Observer<CommunicationMessage> {
    private final GameViewClient view;
    private GameViewClient.InputStateMachine state;
    boolean actionSent = true;

    public ClientMessageObserverHandler(GameViewClient view){
        this.view = view;

    }

    public void updateBoardMessage(GameBoard board){
        Printable.clearScreen();
        view.setBoard(board);
        if(board.getCurrentlyPlaying().equals(view.getClient().getName())){
            view.updateBoard(board);
            view.displayYourTurn();
            if(board.isExpertMode()){
                view.displayExpertMode();
            }
            switch (board.getPhase()){
                case PLANNING_PHASE -> state = GameViewClient.InputStateMachine.PLANNING_PHASE_START;
                case ACTION_PHASE_CHOOSING_CLOUD -> state = GameViewClient.InputStateMachine.CHOOSE_CLOUD_CARD_START;
                case ACTION_PHASE_MOVING_STUDENTS -> state = GameViewClient.InputStateMachine.MOVING_STUDENT_PHASE_START;
                case ACTION_PHASE_MOVING_MOTHER_NATURE -> state = GameViewClient.InputStateMachine.MOVE_MOTHER_NATURE_START;
            }
            actionSent = false;
        }
    }

    @Override
    public void update(CommunicationMessage message) {
        switch (message.getID()){
            case REASK_NAME -> new Thread(view::reaskName).start();
            case NAME_CONFIRMED, JOINING_ACTION_INFO -> new Thread(view::askJoiningAction).start();
            case JOIN_LOBBY_ACTION_CONFIRMED, LOBBY_TO_JOIN_INFO -> new Thread(() -> view.askLobbyToJoin(message.getMessage())).start();
            case CREATE_LOBBY_ACTION_CONFIRMED, NUMBER_OF_PLAYER_INFO -> new Thread(view::askPlayerNumber).start();
            case NUMBER_OF_PLAYER_CONFIRMED, GAME_TYPE_INFO -> new Thread(view::askGameType).start();
            case LOBBY_JOINED_CONFIRMED -> {} // DO NOTHING - now the user will wait until the game starts.
            case ASK_DECK   -> new Thread(() -> view.askDeck(message.getMessage())).start();
            case ASSISTANT_NOT_PLAYABLE -> new Thread(view::reaskAssistant).start();
            case ERROR -> Logger.INFO((String) message.getMessage());
            case GAME_READY -> new Thread(() -> view.gameReady((GameBoard) message.getMessage())).start();
            case VIEW_UPDATE -> new Thread(() -> updateBoardMessage((GameBoard) message.getMessage())).start();
            case YOU_WIN -> Logger.INFO("You have won the match!");
            case YOU_LOSE -> Logger.INFO("You lost the match!");
            case PING -> new Thread(view::pongServer).start();
        }
    }

    public GameViewClient.InputStateMachine getState() {
        return state;
    }

    public void setState(GameViewClient.InputStateMachine state) {
        this.state = state;
    }

    public boolean isActionSent() {
        return actionSent;
    }

    public void setActionSent(boolean actionSent) {
        this.actionSent = actionSent;
    }
}