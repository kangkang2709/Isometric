package ctu.game.isometric.model.quest;


import java.util.HashMap;
import java.util.Map;

public class QuestReward {
    private int experience;
    private int gold;
    private HashMap<String, Integer> items;

    public QuestReward() {
    }

    public QuestReward(int experience, int gold) {
        this.experience = experience;
        this.gold = gold;
        this.items = new HashMap<>();
    }

    public void addItem(String itemId, int quantity) {
        items.put(itemId, quantity);
    }

    // Getters
    public int getExperience() { return experience; }
    public int getGold() { return gold; }
    public Map<String, Integer> getItems() { return items; }
}