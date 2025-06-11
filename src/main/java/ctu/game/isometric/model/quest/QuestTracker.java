package ctu.game.isometric.model.quest;

import ctu.game.isometric.model.entity.Character;

import java.util.ArrayList;
import java.util.List;

public class QuestTracker {
    private List<Quest> activeQuests;
    private List<Quest> completedQuests;
    private List<Quest> availableQuests;

    public QuestTracker() {
        this.activeQuests = new ArrayList<>();
        this.completedQuests = new ArrayList<>();
        this.availableQuests = new ArrayList<>();
    }

    // Add a quest to the available list
    public void addAvailableQuest(Quest quest) {
        if (!availableQuests.contains(quest)) {
            availableQuests.add(quest);
        }
    }

    // Start a quest
    public boolean startQuest(Quest quest, Character character) {
        if (availableQuests.contains(quest) && quest.prerequisitesMet(character)) {
            quest.startQuest();
            activeQuests.add(quest);
            availableQuests.remove(quest);
            return true;
        }
        return false;
    }

    // Complete a quest
    public boolean completeQuest(Quest quest) {
        if (activeQuests.contains(quest) && quest.checkRequirements()) {
            quest.completeQuest();
            activeQuests.remove(quest);
            completedQuests.add(quest);
            return true;
        }
        return false;
    }

    // Claim rewards for a completed quest
    public boolean claimQuestReward(Quest quest, Character character) {
        if (completedQuests.contains(quest) && quest.getStatus() == Quest.QuestStatus.COMPLETED) {
            return quest.claimRewards(character);
        }
        return false;
    }

    // Getters for quest lists
    public List<Quest> getActiveQuests() {
        return activeQuests;
    }

    public List<Quest> getCompletedQuests() {
        return completedQuests;
    }

    public List<Quest> getAvailableQuests() {
        return availableQuests;
    }
}