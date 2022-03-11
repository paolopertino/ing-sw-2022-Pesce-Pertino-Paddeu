package game_model.school;

import game_model.character.Assistant;

import java.util.List;

public class School {
    private DiningHall diningHall = new DiningHall();
    private List<Assistant> assistants;
    private Entrance entrance = new Entrance();
    private List<Tower> towersAvailable;

    public School() {
    }

    public List<Assistant> getAssistants() {
        return assistants;
    }

    public Entrance getEntrance() {
        return entrance;
    }


}
