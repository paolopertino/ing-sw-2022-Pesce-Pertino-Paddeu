package it.polimi.ingsw.game_model.game_type;

import it.polimi.ingsw.game_model.Player;
import it.polimi.ingsw.game_model.character.MotherNature;
import it.polimi.ingsw.game_model.character.basic.Teacher;
import it.polimi.ingsw.game_model.school.DiningTable;
import it.polimi.ingsw.game_model.world.Island;
import it.polimi.ingsw.game_model.world.Terrain;

public class Game3Player extends Game{


    @Override
    public void updateProfessorOwnershipCondition(Teacher t, DiningTable table, Player pl1, Player pl2) {
        normalUpdateProfessorOwnership(t, table, pl1, pl2);
    }

    @Override
    public int playerInfluence(Player pl, Island island) {
        return playerTowerInfluence(pl, island) + playerStudentInfluence(pl, island);
    }

    @Override
    public void refillClouds() {

    }

}
