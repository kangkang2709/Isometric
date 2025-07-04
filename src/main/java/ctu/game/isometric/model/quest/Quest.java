package ctu.game.isometric.model.quest;

import com.fasterxml.jackson.annotation.JsonProperty;
import ctu.game.isometric.model.entity.Character;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Quest {
    private String id;
    private String title;
    private String description;
    private QuestStatus status;
    private QuestReward reward;
    private HashMap<String, Integer> requirements; // Concrete implementation
    private HashMap<String, Integer> progress; // Concrete implementation


    private String conditions;

    private boolean visible;

    private String prerequisiteQuestId;

    public enum QuestStatus {
        AVAILABLE,
        IN_PROGRESS,
        COMPLETED,
        CLAIMED,
        LOCKED
    }

    public Quest() {
    }

    public Quest(String title, String description, QuestReward reward) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.description = description;
        this.status = QuestStatus.AVAILABLE;
        this.reward = reward;
        this.requirements = new HashMap<>();
        this.progress = new HashMap<>();
        this.visible = true;
    }

    public void addRequirement(String item, int quantity) {
        requirements.put(item, quantity);
        // Initialize progress tracking for this requirement
        progress.put(item, 0);
    }

    /**
     * Update progress for a specific requirement
     * @param item The item or objective to update
     * @param amount The amount to add to current progress
     * @return true if the requirement is now complete, false otherwise
     */
    public boolean updateProgress(String item, int amount) {
        if (!requirements.containsKey(item)) {
            return false;
        }

        int currentProgress = progress.getOrDefault(item, 0);
        int requiredAmount = requirements.get(item);
        int newProgress = Math.min(currentProgress + amount, requiredAmount);

        progress.put(item, newProgress);
        return newProgress >= requiredAmount;
    }

    public String getConditions() {
        return conditions;
    }

    public void setConditions(String conditions) {
        this.conditions = conditions;
    }

    // Existing getters and setters
    public String getId() { return id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public QuestStatus getStatus() { return status; }
    public void setStatus(QuestStatus status) { this.status = status; }

    public QuestReward getReward() { return reward; }

    public void setId(String id) {
        this.id = id;
    }

    public void setReward(QuestReward reward) {
        this.reward = reward;
    }

    public HashMap<String, Integer> getRequirements() {
        return requirements;
    }

    public void setRequirements(HashMap<String, Integer> requirements) {
        this.requirements = requirements;
    }

    public HashMap<String, Integer> getProgress() {
        return progress;
    }

    public void setProgress(HashMap<String, Integer> progress) {
        this.progress = progress;
    }

    public void setPrerequisiteQuestId(String prerequisiteQuestId) {
        this.prerequisiteQuestId = prerequisiteQuestId;
    }

    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { visible = visible; }

    public String getPrerequisiteQuestId() { return prerequisiteQuestId; }
}