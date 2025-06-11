package ctu.game.isometric.controller.quest;

import com.badlogic.gdx.Gdx;
import ctu.game.isometric.controller.GameController;
import ctu.game.isometric.model.entity.Character;
import ctu.game.isometric.model.game.Items;
import ctu.game.isometric.model.quest.Quest;
import ctu.game.isometric.model.quest.QuestReward;
import ctu.game.isometric.model.quest.QuestTracker;
import ctu.game.isometric.util.ItemLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BountyBoardController {
    private GameController gameController;
    private List<Quest> availableQuests;
    private Map<String, Quest> allQuests;

    public BountyBoardController(GameController gameController) {
        this.gameController = gameController;
        this.availableQuests = new ArrayList<>();
        this.allQuests = new HashMap<>();
        loadjson("game/quests.json");
    }

    private void loadjson(String fileName) {
        try {
            // Read the JSON file using LibGDX
            String jsonContent = Gdx.files.internal(fileName).readString();

            // Parse the JSON content
            Quest[] quests = new com.badlogic.gdx.utils.Json().fromJson(Quest[].class, jsonContent);

            // Add quests to the lists
            for (Quest quest : quests) {
                allQuests.put(quest.getId(), quest);
                if (quest.getStatus() == Quest.QuestStatus.AVAILABLE) {
                    availableQuests.add(quest);
                }
            }

            System.out.println("Quests loaded successfully from " + fileName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateFormQuestTrack(QuestTracker tracker) {
        availableQuests.clear();
        for (Quest quest : tracker.getAvailableQuests()) {
            if (quest.getStatus() == Quest.QuestStatus.AVAILABLE && quest.isVisible()) {
                availableQuests.add(quest);
            }
        }
    }

    public List<Quest> getAvailableQuests() {
        return availableQuests.stream()
                .filter(Quest::isVisible)
                .filter(q -> q.getStatus() == Quest.QuestStatus.AVAILABLE)
                .toList();
    }

    public void acceptQuest(String questId) {
        Character character = gameController.getCharacter();
        Quest quest = allQuests.get(questId);

        if (quest != null && quest.getStatus() == Quest.QuestStatus.AVAILABLE) {
            quest.setStatus(Quest.QuestStatus.IN_PROGRESS);
            character.getQuestTracker().getActiveQuests().add(quest);
            gameController.getDialogController().showSimpleMessage("Quest accepted: " + quest.getTitle());
        }
    }

    public boolean checkQuestCompletion(String questId) {
        Character character = gameController.getCharacter();
        Quest quest = allQuests.get(questId);

        if (quest != null && quest.getStatus() == Quest.QuestStatus.IN_PROGRESS) {
            Map<String, Integer> requirements = quest.getRequirements();
            Map<String, Integer> inventory = character.getItems();

            for (String item : requirements.keySet()) {
                int requiredAmount = requirements.get(item);
                int availableAmount = inventory.getOrDefault(item, 0);

                if (availableAmount < requiredAmount) {
                    return false;
                }
            }
            quest.setStatus(Quest.QuestStatus.COMPLETED);
            return true;
        }
        return false;
    }

    public void submitQuest(String questId) {
        Character character = gameController.getCharacter();
        Quest quest = allQuests.get(questId);

        if (quest != null && quest.getStatus() == Quest.QuestStatus.COMPLETED) {
            // Remove required items from inventory
            for (Map.Entry<String, Integer> requirement : quest.getRequirements().entrySet()) {
                character.removeItem(requirement.getKey(), requirement.getValue());
            }

            // Award rewards
            QuestReward reward = quest.getReward();
            character.expToLevelUp(reward.getExperience());

            // Add gold - need to add a method to Character
            Map<String, Integer> items = character.getItems();
            int currentGold = items.getOrDefault("Gold", 0);
            items.put("Gold", currentGold + reward.getGold());

            // Add reward items
            for (Map.Entry<String, Integer> itemReward : reward.getItems().entrySet()) {
                Items item = ItemLoader.getItemByName(itemReward.getKey());
                if (item != null) {
                    character.addItem(item, itemReward.getValue());
                }
            }

            // Update quest status
            quest.setStatus(Quest.QuestStatus.CLAIMED);
            character.getQuestTracker().getActiveQuests().remove(quest);
            character.getQuestTracker().getCompletedQuests().add(quest);

            // Show completion message
            gameController.getDialogController().showSimpleMessage(
                    "Quest completed! Received: " +
                            reward.getExperience() + " XP, " +
                            reward.getGold() + " gold"
            );

            // Check if character leveled up
            if (character.getExp() < character.getExp()) {
                gameController.showLevelUpNotification();
            }
        }
    }

    public List<Quest> getActiveQuests() {
        return gameController.getCharacter().getQuestTracker().getActiveQuests();
    }

    public List<Quest> getCompletedQuests() {
        return gameController.getCharacter().getQuestTracker().getCompletedQuests();
    }
}