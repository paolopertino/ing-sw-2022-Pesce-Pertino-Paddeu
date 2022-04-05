package it.polimi.ingsw.game_model.character.advanced;

import it.polimi.ingsw.custom_exceptions.BagEmptyException;
import it.polimi.ingsw.game_model.Player;
import it.polimi.ingsw.game_model.character.basic.Student;
import it.polimi.ingsw.game_model.character.character_utils.AdvancedCharacterType;
import it.polimi.ingsw.game_model.Game;
import it.polimi.ingsw.game_model.world.Island;

import java.util.ArrayList;
import java.util.List;

public class Monk extends AdvancedCharacter{
    private final List<Student> studentsOnCard;

    public Monk(Game game) {
        super(AdvancedCharacterType.MONK, game);
        this.studentsOnCard = new ArrayList<>();

        // Setting up 4 students on the card
        try {
            studentsOnCard.addAll(game.getBag().drawNStudentFromBag(4));
        } catch (BagEmptyException e) {
            // Impossible to reach since the cards are eventually setup at the beginning of the match
            e.printStackTrace();
        }
    }

    public List<Student> getStudentsOnCard() {
        return studentsOnCard;
    }

    /**
     * Take 1 student from this card and place it on an island of your choice. Then
     * draw a student from the game bag and place it on this card.
     * @param attributes
     */
    @Override
    public boolean playEffect(Object... attributes) {
        if(!validateArgs(attributes)){
            return false;
        }

        Island islandToPlaceStudentOn = (Island) attributes[0];
        Integer studentToPick = (Integer) attributes[1];

        // Adding the selected student to the selected island
        islandToPlaceStudentOn.addStudent(studentsOnCard.remove(studentToPick.intValue()));

        // picking up a new student from the bag and putting it on the card
        try {
            studentsOnCard.add(game.getBag().drawStudentFromBag());
        } catch (BagEmptyException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    protected boolean validateArgs(Object... args) {
        if(args.length != 2) {
            return false;
        }
        try {
            Island islandToPlaceStudentOn = (Island) args[0];
            Integer studentsToPick = (Integer) args[1];
        } catch(Exception ex) {
            ex.printStackTrace();
            return false;
        }

        return true;
    }
}
