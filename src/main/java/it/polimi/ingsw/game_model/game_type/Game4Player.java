package it.polimi.ingsw.game_model.game_type;

import it.polimi.ingsw.custom_exceptions.BagEmptyException;
import it.polimi.ingsw.game_model.Player;
import it.polimi.ingsw.game_model.school.DiningTable;
import it.polimi.ingsw.game_model.world.CloudCard;
import it.polimi.ingsw.game_model.world.Island;

public class Game4Player extends Game{
    private final static int MAX_PLAYERS = 4;
    private final int NUMBER_OF_STUDENTS_ON_CLOUD = 3;

    public Game4Player() {
        super(MAX_PLAYERS);
    }

    @Override
    protected void updateProfessorOwnershipCondition(DiningTable table1, DiningTable table2, Player pl1) {
        normalUpdateProfessorOwnership(table1, table2, pl1);
    }

    @Override
    protected int playerInfluence(Player pl, Island island) {
        return playerTowerInfluence(pl, island) + playerStudentInfluence(pl, island);
    }

    @Override
    public void refillClouds() {
        for(CloudCard cloudCard: terrain.getCloudCards()){
            while(cloudCard.getStudentsOnCloud().size() < NUMBER_OF_STUDENTS_ON_CLOUD){
                try {
                    cloudCard.getStudentsOnCloud().add(bag.drawStudentFromBag());
                } catch (BagEmptyException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void createCloudCard(){
        terrain.addCloudCard(new CloudCard(NUMBER_OF_STUDENTS_ON_CLOUD));
    }

    @Override
    public int studentsLeftToMove(Player player){
        return NUMBER_OF_STUDENTS_ON_CLOUD - player.getNumberOfMovedStudents();
    }
}
