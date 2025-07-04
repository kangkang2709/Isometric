package ctu.game.isometric.model.quest;

import ctu.game.isometric.model.entity.Character;

import java.util.ArrayList;
import java.util.List;

public class QuestTracker {
    private List<Quest> activeQuests;
    private List<Quest> completedQuests;
    private List<Quest> availableQuests;
    private List<Quest> lockedQuests;

    public QuestTracker() {
        this.activeQuests = new ArrayList<>();
        this.completedQuests = new ArrayList<>();
        this.availableQuests = new ArrayList<>();
        this.lockedQuests = new ArrayList<>();
    }

    // Getters for quest lists
    public List<Quest> getActiveQuests() {
        return activeQuests;
    }

    public List<Quest> getCompletedQuests() {
        return completedQuests;
    }

    public List<Quest> getLockedQuests() {
        return lockedQuests;
    }

    public void setAvailableQuests(List<Quest> availableQuests) {
        this.availableQuests = availableQuests;
    }

    public void setLockedQuests(List<Quest> lockedQuests) {
        this.lockedQuests = lockedQuests;
    }
    public List<Quest> getAvailableQuests() {
        return availableQuests;
    }
}