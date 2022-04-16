package it.polimi.ingsw.game_model.character.advanced;

import it.polimi.ingsw.game_model.character.character_utils.AdvancedCharacterType;
import it.polimi.ingsw.game_model.Game;
import it.polimi.ingsw.game_model.utils.ColorCharacter;
import it.polimi.ingsw.game_model.world.Island;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

public class Healer extends AdvancedCharacter{
    private int numberOfDeniableIslands = 4;


    public Healer(Game game) {
        super(AdvancedCharacterType.HEALER, game);
    }

    /**
     * Place a No Entry tile on an island of your choice. The first time Mother Nature ends her movement
     * there put the no entry tile back onto this card without calculating influence of placing any tower.
     * @param attributes
     */
    @Override
    public boolean playEffect(Object... attributes) {
        if(!validateArgs(attributes)){
            return false;
        }
        Island islandToDeny = (Island) attributes[0];

        if(numberOfDeniableIslands > 0) {
            islandToDeny.denyIsland();
            numberOfDeniableIslands--;

            //TODO: controllare se funziona dovrebbe creare un listener al valore di deny tyle che ci sono sull'isola e viene eliminato nel caso calasse
            islandToDeny.getIsBlocked().addListener(new ChangeListener<>() {
                @Override
                public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, Number newValue) {
                    if((Integer) newValue < (Integer) oldValue) {
                        numberOfDeniableIslands++;
                        islandToDeny.getIsBlocked().removeListener(this);
                    }
                }
            });
        }

        return true;
    }

    @Override
    protected boolean validateArgs(Object... attributes) {
        if(attributes.length != 1){
            return false;
        }
        try {
            Island islandToDeny = (Island) attributes[0];
        }
        catch (Exception e){
            return false;
        }
        return true;
    }


}