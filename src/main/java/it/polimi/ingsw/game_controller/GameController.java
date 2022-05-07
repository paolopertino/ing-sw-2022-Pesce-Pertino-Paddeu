package it.polimi.ingsw.game_controller;

import it.polimi.ingsw.custom_exceptions.*;
import it.polimi.ingsw.game_controller.action.GameAction;
import it.polimi.ingsw.game_model.GameExpertMode;
import it.polimi.ingsw.game_model.character.character_utils.AdvancedCharacterType;
import it.polimi.ingsw.network.utils.Logger;
import it.polimi.ingsw.observer.Observer;
import it.polimi.ingsw.game_model.utils.CalculatorInfluence;
import it.polimi.ingsw.game_model.utils.CalculatorTeacherOwnership;
import it.polimi.ingsw.game_model.Player;
import it.polimi.ingsw.game_model.character.Assistant;
import it.polimi.ingsw.game_model.character.advanced.AdvancedCharacter;
import it.polimi.ingsw.game_model.character.character_utils.DeckType;
import it.polimi.ingsw.game_model.Game;
import it.polimi.ingsw.game_model.utils.ColorCharacter;
import it.polimi.ingsw.game_model.utils.GamePhase;
import it.polimi.ingsw.game_model.world.CloudCard;
import javafx.util.Pair;

import java.security.SecureRandom;
import java.util.*;

public class GameController implements Observer<GameAction> {
    private final SecureRandom random = new SecureRandom();
    private final Game game;
    private int turn = 0;
    private final int[] planningOrder, actionOrder;

    public GameController(Game game) {
        this.game = game;
        planningOrder = new int[game.MAX_PLAYERS];
        actionOrder = new int[game.MAX_PLAYERS];
    }

    public Player createPlayer(String name, DeckType type){
        Player player = new Player(name, type);
        addPlayer(player);
        return player;
    }

    /**
     * Adds a player to the list of players.
     *
     * @param player  the player to be added to the game's player list
     *
     * @see Player
     */
    protected void addPlayer(Player player){
        game.getPlayers().add(player);
        if(game.getNumberOfPlayers() == game.MAX_PLAYERS){
            start();
        }
    }

    /**
     * Handle the game initial status. Before the game starts:
     * <ol><li>create next Planning phase order <li> Gameboard setup <li> play a Turn </ol>
     */
    public final void start() {
        game.setUpGamePhase(GamePhase.GAME_PENDING);
        // at the beginning a random player is chosen
        createNextPlanningOrder(random.nextInt(game.getNumberOfPlayers()));
        game.setCurrentlyPlaying(planningOrder[0]);
        try {
            game.setupBoard();
            game.setUpGamePhase(GamePhase.NEW_ROUND);
            playTurn();
        } catch (BagEmptyException | TooManyStudentsException e) {
            // impossible to reach
            Logger.ERROR("Unable to setup the board.", e.getMessage());
        }
    }

    /**
     *  This function states the beginning of a new turn consisted in up to 4 planning phase and action phase
     */
    private void playTurn(){
        if (game.winner().length == 0) {
            game.setUpGamePhase(GamePhase.PLANNING_PHASE);
            refillClouds();
            // every "turn" is divided in planning phase and action phase
            nextPlanningPhase();
        }
    }

    /**
     * Plays the planning phase for the next player following the game planning order, in which every player plays an
     * assistant card. <p> If all player have already played an assistant card, moves to nextActionPhase() </p>
     */
    private void nextPlanningPhase(){
        if(turn < game.getNumberOfPlayers()){
            game.setCurrentlyPlaying(planningOrder[turn]);
        }
        else{
            turn = 0;
            createActionPhaseOrder();
            game.setCurrentlyPlaying(actionOrder[turn]);
            resetPlayerNumberOfMovedStudents();
            nextActionPhase();
        }
    }

    /**
     * This function will be called when a player as chosen which Assistant card to play.
     * It notifies the game model which card as been chosen and then tries to play the next planning phase.
     * @param playerName the name of the player who request the action.
     * @param assistantIndex the played assistant card.
     */
    public void selectAssistantCard(String playerName, int assistantIndex){
        Player player = getPlayerFromName(playerName);
        Assistant assistant = player.getDeckAssistants().getAssistants().get(assistantIndex);
        if(game.getCurrentlyPlayingPlayer().equals(player)){
            if(isAssistantCardPlayable(player, assistant)) {
                game.getCurrentlyPlayingPlayer().playAssistant(assistant);

                turn++;
                nextPlanningPhase();
                game.runNotify(CommunicationMessage.MessageType.VIEW_UPDATE);
            }
            else{
                game.runNotify(CommunicationMessage.MessageType.ASSISTANT_NOT_PLAYABLE);
            }
        } else {
            Logger.WARNING("It's not " + playerName + " turn. Is he using cheats?");
            game.runNotify(CommunicationMessage.MessageType.NOT_YOUR_TURN);
        }
    }

    private Player getPlayerFromName(String playerName) {
        return game.getPlayers().stream().reduce((pl1, pl2) -> pl1.getNickname().equals(playerName) ? pl1 : pl2).get();
    }

    /**
     *  Check if the assistant card is playable, the assistant card cannot be played if it was already played by another
     *  player, unless the player has no other card available to play.
     *
     * @param player the player who request the action
     * @param assistant the played assistant card
     * @return <strong>true</strong> if the assistant is playable, <strong>false</strong> otherwise.
     */
    private boolean isAssistantCardPlayable(Player player, Assistant assistant){
        List<Assistant> playedAssistant = new ArrayList<>();
        for(int i = 0; i < turn; i++){
            playedAssistant.add(game.getPlayers().get(planningOrder[i]).getDiscardedCard());
        }
        return !playedAssistant.contains(assistant) || playedAssistant.containsAll(player.getDeckAssistants().getAssistants());
    }

    /**
     * Creates the order for the action phase, based on the value of the assistant card played.
     */
    private void createActionPhaseOrder(){
        List<Pair<Integer, Integer>> playerAndValue = new ArrayList<>();
        //cycle to create all position
        for(int i = 0; i < game.getNumberOfPlayers(); i++){
            playerAndValue.add(new Pair<>(i, game.getPlayers().get(i).getDiscardedCard().getValue()));
        }

        playerAndValue.sort(Comparator.comparing(Pair::getValue));
        for(int i = 0; i < game.getNumberOfPlayers(); i++){
            actionOrder[i] = playerAndValue.get(i).getKey();
        }
    }

    /**
     * For every player reset his count of NumberOfMovedStudents
     */
    private void resetPlayerNumberOfMovedStudents(){
        for(Player pl: game.getPlayers()){
            pl.resetNumberOfMovedStudents();
        }
    }

    /**
     * Plays the action phase for the next player following the game action order, that starts with the movement of the
     * students. <p> If all player have already played, moves to a new turn with playTurn() </p>
     */
    private void nextActionPhase(){
        if(turn < game.getNumberOfPlayers() && game.winner().length == 0){
            game.setUpGamePhase(GamePhase.ACTION_PHASE_MOVING_STUDENTS);
            game.setCurrentlyPlaying(actionOrder[turn]);
            moveStudentPhase(game.getCurrentlyPlayingPlayer());
        }
        else{
            turn = 0;
            createNextPlanningOrder(actionOrder[0]);
            game.setCurrentlyPlaying(planningOrder[turn]);
            game.setUpGamePhase(GamePhase.NEW_ROUND);
            playTurn();
        }
    }

    /**
     * Check if the player hasn't moved all his student
     * @param player the player who request the action
     */
    private void moveStudentPhase(Player player){
        if(game.studentsLeftToMove(player) == 0){
            game.setUpGamePhase(GamePhase.ACTION_PHASE_MOVING_MOTHER_NATURE);
        }
    }

    /**
     * Given the player name which made the move, the index of the student of his entrance he wants to move and
     * the island index, it moves the selected student to the selected island if it's the player's turn, otherwise
     * it sends a NOT_YOUR_TURN error message.
     *
     * @param playerName the nickname of the player who made the move.
     * @param student the student index in the player's entrance which has to be moved.
     * @param islandNumber the index of the island on which to move the student.
     */
    public void playerMoveStudentToIsland(String playerName, int student, int islandNumber){
        Player pl = game.getCurrentlyPlayingPlayer();
        Player player = getPlayerFromName(playerName);
        if(pl.equals(player)) {
            game.getTerrain().addStudentToIsland(player.getSchool().getEntrance().moveStudent(student), islandNumber);
            player.incrementNumberOfMovedStudents();

            moveStudentPhase(pl);
            game.runNotify(CommunicationMessage.MessageType.VIEW_UPDATE);
        } else {
            Logger.WARNING("It's not " + playerName + " turn. Is he using cheats?");
            game.runNotify(CommunicationMessage.MessageType.NOT_YOUR_TURN);
        }
    }

    public void playerMoveStudentToDiningHall(String playerName, int student){
        Player pl = game.getCurrentlyPlayingPlayer();
        Player player = getPlayerFromName(playerName);
        if(pl.equals(player)) {
            try {
                ColorCharacter diningHallColor = player.getSchool().getEntrance().moveStudent(student).getColor();
                game.moveStudentToDiningHall(player, diningHallColor);
                player.incrementNumberOfMovedStudents();
            } catch (TooManyStudentsException e) {
                Logger.GAME_LOG("Tried to move a student on a full table, skipping...", playerName);
                game.runNotify(CommunicationMessage.MessageType.MOVE_STUDENT_FAILED);
            }
            moveStudentPhase(player);
            game.runNotify(CommunicationMessage.MessageType.VIEW_UPDATE);
        } else {
            Logger.WARNING("It's not " + playerName + " turn. Is he using cheats?");
            game.runNotify(CommunicationMessage.MessageType.NOT_YOUR_TURN);
        }
    }

    /**
     * Given the max possible steps, it lets the user choose how many steps mother nature
     * has to do, and then it moves her.
     *  @param playerName : the player who called the action
     * @param numberOfSteps : selected steps that mother nature has to perform
     */
    public void moveMotherNatureOfSteps(String playerName, int numberOfSteps){
        Player player = getPlayerFromName(playerName);

        if(player.equals(game.getCurrentlyPlayingPlayer())){
            if(1 <= numberOfSteps && numberOfSteps <= player.getDiscardedCard().getPossibleSteps()){
                game.getMotherNature().moveOfIslands(game.getTerrain(), numberOfSteps);

                game.evaluateInfluences(game.getMotherNature().getPosition());

                if(game.winner().length == 0){
                    game.setUpGamePhase(GamePhase.ACTION_PHASE_CHOOSING_CLOUD);
                }
                else {
                    game.setUpGamePhase(GamePhase.GAME_ENDED);
                    endGame();
                }
                game.runNotify(CommunicationMessage.MessageType.VIEW_UPDATE);
            }
            else{
                Logger.GAME_LOG("Tried to move mother nature of an invalid number of steps, skipping...", playerName);
                game.runNotify(CommunicationMessage.MessageType.INVALID_MOTHER_NATURE_STEPS);
            }
        } else {
            Logger.WARNING("It's not " + playerName + " turn. Is he using cheats?");
            game.runNotify(CommunicationMessage.MessageType.NOT_YOUR_TURN);
        }
    }

    private void endGame(){
        game.runNotify(CommunicationMessage.MessageType.GAME_ACTION);
    }

    public void choseCloud(String playerName, int cloudCardIndex){
        Player player = getPlayerFromName(playerName);
        if(player.equals(game.getCurrentlyPlayingPlayer())){
            if(game.getTerrain().getCloudCards().get(cloudCardIndex).getStudent().size() > 0) {
                player.getSchool().getEntrance().addAllStudents(game.getTerrain().getCloudCards().get(cloudCardIndex).removeStudentsOnCloud());
                turn++;
                // Resetting the normal values
                player.resetPlayedSpecialCard();
                game.setInfluenceCalculator(new CalculatorInfluence());
                game.setTeacherOwnershipCalculator(new CalculatorTeacherOwnership());

                nextActionPhase();
                game.runNotify(CommunicationMessage.MessageType.VIEW_UPDATE);
            } else {
                game.runNotify(CommunicationMessage.MessageType.INVALID_CLOUD_CHOSEN);
            }
        } else {
            Logger.WARNING("It's not " + playerName + " turn. Is he using cheats?");
            game.runNotify(CommunicationMessage.MessageType.NOT_YOUR_TURN);
        }
    }

    public void playAdvancedCard(String playerName, AdvancedCharacterType cardType, Object... args) {
        Player player = getPlayerFromName(playerName);
        AdvancedCharacter card = game.getTerrain().getAdvancedCharacters().stream().filter(c -> c.getType().equals(cardType)).toList().get(0);

        if(game.isExpert()) {
            if (player.equals(game.getCurrentlyPlayingPlayer())) {
                if (game.getGamePhase().toString().startsWith("ACTION_PHASE")) {
                    if (!player.hasPlayedSpecialCard()) {
                        if (player.getMoney() >= card.getCardCost()) {
                            if (card.playEffect(args)) {
                                player.setPlayedSpecialCard();
                                player.setMoney(player.getMoney() - card.getCardCost());
                                ((GameExpertMode) game).addMoneyToTreasury(card.getCardCost());
                                card.incrementCardCost();
                                game.runNotify(CommunicationMessage.MessageType.VIEW_UPDATE);
                            } else {
                                Logger.GAME_LOG("Tried to play an advanced card with wrong arguments, skipping...", playerName);
                                game.runNotify(CommunicationMessage.MessageType.ADVANCED_NOT_PLAYABLE);
                            }
                        } else {
                            Logger.GAME_LOG("Tried to play an advanced without enough money, skipping...", playerName);
                            game.runNotify(CommunicationMessage.MessageType.NOT_ENOUGH_MONEY);
                        }
                    } else {
                        Logger.GAME_LOG("Tried to play an advanced card, but another one was played in the same turn by the player, skipping...", playerName);
                        game.runNotify(CommunicationMessage.MessageType.ALREADY_PLAYED_ADVANCED);
                    }
                } else {
                    Logger.GAME_LOG("Tried to play an advanced card in an invalid game phase, skipping...", playerName);
                    game.runNotify(CommunicationMessage.MessageType.NOT_ACTION_PHASE);
                }
            } else {
                Logger.WARNING("It's not " + playerName + " turn. Is he using cheats?");
                game.runNotify(CommunicationMessage.MessageType.NOT_YOUR_TURN);
            }
        } else {
            Logger.GAME_LOG("Tried to play a character card in a normal game. Is he cheating?", playerName);
            game.runNotify(CommunicationMessage.MessageType.NOT_EXPERT_GAME);
        }
    }

    private void refillClouds() {
        for(CloudCard cloudCard: game.getTerrain().getCloudCards()){
            try {
                cloudCard.refill(game.getBag().drawNStudentFromBag(game.NUMBER_OF_STUDENTS_ON_CLOUD));
            } catch (BagEmptyException e) {
                // If the cloud refill cannot be completed due to a lack of students, then the clouds are cleared. Since
                // they are empty players cannot add any student from the cloud to their entrance in the last turn, according
                // to the rules.
                for(CloudCard cloudCard1 : game.getTerrain().getCloudCards()) {
                    cloudCard1.clear();
                }
                break;
            }
        }
    }

    /**
     * Creates the planning order of the next turn.
     * The player which played the assistant card with the lowest value will start the Planning Phase of the next turn.
     */
    private void createNextPlanningOrder(int firstPlayerPlanningOrder){
        planningOrder[0] = firstPlayerPlanningOrder;
        for(int i = 1; i < game.getNumberOfPlayers(); i++){
            planningOrder[i] = (planningOrder[i-1] + 1) % game.getNumberOfPlayers();
        }
    }

    public Player getCurrentPlayer() {
        if(game.getGamePhase().toString().startsWith("ACTION")) return game.getPlayers().get(actionOrder[turn]);
        else return game.getPlayers().get(planningOrder[turn]);
    }

    public List<DeckType> getAvailableDeckType(){
        return Arrays.stream(DeckType.values()).filter(type ->
                !(game.getPlayers().stream().filter(pl -> pl.getDeckAssistants() != null)
                        .map(pl -> pl.getDeckAssistants().getType()).toList().contains(type))).toList();
    }

    @Override
    public void update(GameAction action) {
        action.perform(this);
    }
}
