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
    private Map<String, Quest> allQuests;

    public BountyBoardController(GameController gameController) {
        this.gameController = gameController;
        this.allQuests = new HashMap<>();
    }

    private void loadjson(String fileName) {
        try {
            String jsonContent = Gdx.files.internal(fileName).readString();
            Quest[] quests = new com.badlogic.gdx.utils.Json().fromJson(Quest[].class, jsonContent);
            for (Quest quest : quests) {
                allQuests.put(quest.getId(), quest);
            }
            System.out.println("Quests loaded successfully from " + fileName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void reset() {
        allQuests.clear();
        loadjson("game/quests.json");
    }

    public void updateQuestStatusFromQuestTracker(QuestTracker questTracker) {
        System.out.println("Updating quest statuses from QuestTracker...");

        // Separate lists to track updated quest groups
        List<Quest> lockedQuests = new ArrayList<>();
        List<Quest> availableQuests = new ArrayList<>();

        // Update statuses from QuestTracker
        for (Quest activeQuest : questTracker.getActiveQuests()) {
            Quest boardQuest = allQuests.get(activeQuest.getId());
            if (boardQuest != null) {
                boardQuest.setStatus(Quest.QuestStatus.IN_PROGRESS);
            }
        }

        for (Quest completedQuest : questTracker.getCompletedQuests()) {
            Quest boardQuest = allQuests.get(completedQuest.getId());
            if (boardQuest != null) {
                boardQuest.setStatus(completedQuest.getStatus());
            }
        }

        // Classify remaining quests
        for (Quest quest : allQuests.values()) {
            Quest.QuestStatus status = quest.getStatus();
            if (status == Quest.QuestStatus.LOCKED) {
                lockedQuests.add(quest);
            } else if (status == Quest.QuestStatus.AVAILABLE) {
                availableQuests.add(quest);
            }
        }

        // Update the quest tracker with the new status lists
        questTracker.setLockedQuests(lockedQuests);
        questTracker.setAvailableQuests(availableQuests);
    }


    public Map<String, Quest> getAllQuests() {
        return allQuests;
    }

    public List<Quest> getActiveQuests() {
        return gameController.getCharacter().getQuestTracker().getActiveQuests();
    }


    public List<Quest> getCompletedQuests() {
        return gameController.getCharacter().getQuestTracker().getCompletedQuests();
    }

    public List<Quest> getLockedQuests() {
        return gameController.getCharacter().getQuestTracker().getLockedQuests();
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

            if (inventory == null) {
                return false;
            }

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
            for (Map.Entry<String, Integer> requirement : quest.getRequirements().entrySet()) {
                character.removeItem(requirement.getKey(), requirement.getValue());
            }

            QuestReward reward = quest.getReward();
            if (reward != null) {
                character.expToLevelUp(reward.getExperience());
                character.addScore(reward.getGold());
                if (reward.getItems() != null) {
                    for (Map.Entry<String, Integer> itemReward : reward.getItems().entrySet()) {
                        Items item = ItemLoader.getItemByName(itemReward.getKey());
                        if (item != null) {
                            character.addItem(item, itemReward.getValue());
                        }
                    }
                }

            }

            quest.setStatus(Quest.QuestStatus.CLAIMED);
            character.getQuestTracker().getActiveQuests().remove(quest);
            character.getQuestTracker().getCompletedQuests().add(quest);

            gameController.getDialogController().showSimpleMessage(
                    "Quest completed! Received: " +
                            reward.getExperience() + " XP, " +
                            reward.getGold() + " gold"
            );

            int currentLevel = character.getLevel();
            int newLevel = character.expToLevelUp(reward.getExperience());

            if (newLevel > currentLevel) {
                gameController.showLevelUpNotification();
            }


        }
    }

    public void dispose() {
        allQuests.clear();
        gameController = null;
    }

    public GameController getGameController() {
        return gameController;
    }

    public void setGameController(GameController gameController) {
        this.gameController = gameController;
    }
}