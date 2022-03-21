package it.polimi.ingsw.game_model.game_type;

import it.polimi.ingsw.game_model.Player;
import it.polimi.ingsw.game_model.character.advanced.AdvancedCharacter;
import it.polimi.ingsw.game_model.character.advanced.ColorPickerAdvancedCharacter;
import it.polimi.ingsw.game_model.character.basic.Student;
import it.polimi.ingsw.game_model.character.basic.Teacher;
import it.polimi.ingsw.game_model.school.DiningTable;
import it.polimi.ingsw.game_model.utils.ColorCharacter;
import it.polimi.ingsw.game_model.world.Island;

public class Game2PlayerAdvanced extends Game2Player implements ExpertMode{
    AdvancedCharacter playerCard; //TODO this card will be assigned if a card is played


    @Override
    public void updateProfessorOwnershipCondition(DiningTable table1, DiningTable table2, Player pl1) {
        switch(playerCard.getAdvanceCharacterType()){
            case BARTENDER:
                if (pl1.hasPlayedSpecialCard() && table1.getColor() == table2.getColor() &&
                        table1.getNumberOfStudents() >= table2.getNumberOfStudents()) {
                    pl1.getSchool().addTeacher(getTeacherOfColorFromAllPlayers(table1.getColor()));
                }
                break;

            default:
                normalUpdateProfessorOwnership(table1, table2, pl1);
                break;
        }
    }

    @Override
    public int playerInfluence(Player pl, Island island){
        int influence = playerStudentInfluence(pl, island) + playerTowerInfluence(pl, island);
        if(pl.hasPlayedSpecialCard()) {
            switch (playerCard.getAdvanceCharacterType()) {
                case CENTAURUS:
                    influence = playerStudentInfluence(pl, island);
                    break;

                case KNIGHT:
                    influence = playerStudentInfluence(pl, island) + playerTowerInfluence(pl, island) + 2;
                    break;

                case LANDLORD:
                    influence = playerStudentInfluenceWithoutColor(pl, island, ((ColorPickerAdvancedCharacter)playerCard).getColor())
                            + playerTowerInfluence(pl, island);
                    break;

                default:
                    influence = playerStudentInfluence(pl, island) + playerTowerInfluence(pl, island);
                    break;
            }
        }
        return influence;
    }

    @Override
    public void pickAdvancedCards(){
        terrain.pickAdvancedCard();
    }


    private int playerStudentInfluenceWithoutColor(Player pl, Island island, ColorCharacter color){
        int influence = 0;
        for(Teacher t: pl.getTeachers()){
            for(Student s: island.getStudents()){
                if(t.getColor() == s.getColor() && s.getColor() != color){
                    influence++;
                }
            }
        }
        return influence;
    }

}
