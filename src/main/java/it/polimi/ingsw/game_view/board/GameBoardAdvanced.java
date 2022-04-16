package it.polimi.ingsw.game_view.board;

import it.polimi.ingsw.game_model.Game;
import it.polimi.ingsw.game_model.GameExpertMode;
import it.polimi.ingsw.game_model.Player;

import java.io.Serializable;

public class GameBoardAdvanced extends GameBoard implements Serializable {

    public GameBoardAdvanced(Game game) {
        super(game);
        setGameToExpertMode();
        setTreasury(((GameExpertMode)game).getTreasury());
        for(Player player: game.getPlayers()){
            bankAccounts.add(player.getMoney());
        }
    }
}