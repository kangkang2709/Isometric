package ctu.game.isometric.controller;

import ctu.game.isometric.model.game.Tutorial;

import java.util.*;

public class TutorialManager {
    private final Map<String, List<Tutorial>> tutorials;


    public TutorialManager() {
        this.tutorials = new HashMap<>();
        initializeTutorials();
    }


    public void initializeTutorials() {
        addTutorial("movement", new Tutorial("1", "Movement Tutorial", "Learn how to move your character with keyboard.Learn how to move your character with keyboard.Learn how to move your character with keyboard.Learn how to move your character with keyboard.Learn how to move your character with keyboard.", "movement.png", "movement", false));
        addTutorial("movement", new Tutorial("2", "Movement Tutorial", "Learn how to move your character with mouse click.", "movement_mouse.png", "movement", false));
        addTutorial("combat", new Tutorial("1", "Combat Tutorial", "Learn how to fight enemies.", "combat.png", "combat", false));
        addTutorial("inventory", new Tutorial("1", "Inventory Tutorial", "Learn how to manage your inventory.", "inventory.png", "inventory", false));
    }

    public void addTutorial(String type, Tutorial tutorial) {
        if (tutorials.containsKey(type)) {
            tutorials.get(type).add(tutorial);
            tutorials.get(type).sort(Comparator.comparing(Tutorial::getId));
        } else {
            LinkedList<Tutorial> tutorialList = new LinkedList<>();
            tutorialList.add(tutorial);
            tutorials.put(type, tutorialList);
        }
    }



    public List<Tutorial> getTutorialsByType(String type) {
        return tutorials.getOrDefault(type, List.of());
    }

}