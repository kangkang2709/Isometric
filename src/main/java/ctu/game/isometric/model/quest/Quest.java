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

    private boolean visible;

    private String prerequisiteQuestId;

    public enum QuestStatus {
        AVAILABLE,
        IN_PROGRESS,
        COMPLETED,
        CLAIMED
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

    /**
     * Check if all requirements for this quest are met
     * @return true if all requirements are satisfied
     */
    public boolean checkRequirements() {
        for (Map.Entry<String, Integer> entry : requirements.entrySet()) {
            String item = entry.getKey();
            int required = entry.getValue();
            int current = progress.getOrDefault(item, 0);

            if (current < required) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if the character has the items required by this quest
     * @param character The character to check
     * @return true if character has all required items in sufficient quantities
     */
    public boolean characterHasRequiredItems(Character character) {
        for (Map.Entry<String, Integer> entry : requirements.entrySet()) {
            String itemName = entry.getKey();
            int requiredQuantity = entry.getValue();

            if (!character.hasItem(itemName) ||
                    character.getItems().get(itemName) < requiredQuantity) {
                return false;
            }
        }
        return true;
    }

    /**
     * Start the quest, changing its status to IN_PROGRESS
     */
    public void startQuest() {
        if (status == QuestStatus.AVAILABLE) {
            status = QuestStatus.IN_PROGRESS;
        }
    }

    /**
     * Complete the quest, changing status to COMPLETED
     */
    public void completeQuest() {
        if (status == QuestStatus.IN_PROGRESS && checkRequirements()) {
            status = QuestStatus.COMPLETED;
        }
    }

    /**
     * Award quest rewards to the character and mark as CLAIMED
     * @param character The character to receive rewards
     * @return true if rewards were successfully claimed
     */
    public boolean claimRewards(Character character) {
        if (status != QuestStatus.COMPLETED) {
            return false;
        }

        // Award experience
        character.expToLevelUp(reward.getExperience());

        // Award gold (assuming character has a way to add gold)
        // This might need additional implementation
        character.addScore(reward.getGold());

        // Award items
        for (Map.Entry<String, Integer> item : reward.getItems().entrySet()) {
            // This assumes items in rewards match the format expected by character.addItem
            // You might need to fetch the actual Item object first
            if (item.getValue() > 0) {
                ctu.game.isometric.model.game.Items gameItem =
                        ctu.game.isometric.util.ItemLoader.getItemByName(item.getKey());
                if (gameItem != null) {
                    character.addItem(gameItem, item.getValue());
                }
            }
        }

        status = QuestStatus.CLAIMED;
        return true;
    }

    /**
     * Set a prerequisite quest that must be completed before this one
     * @param questId The ID of the prerequisite quest
     */
    public void setPrerequisiteQuest(String questId) {
        this.prerequisiteQuestId = questId;
    }

    /**
     * Check if this quest's prerequisites are met
     * @param character The character to check against
     * @return true if prerequisites are met or if there are no prerequisites
     */
    public boolean prerequisitesMet(Character character) {
        if (prerequisiteQuestId == null) {
            return true;
        }

        for (Quest quest : character.getCompletedQuests()) {
            if (quest.getId().equals(prerequisiteQuestId) &&
                    (quest.getStatus() == QuestStatus.COMPLETED ||
                            quest.getStatus() == QuestStatus.CLAIMED)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get the progress percentage for this quest
     * @return progress as a value between 0 and 1
     */
    public float getProgressPercentage() {
        if (requirements.isEmpty()) {
            return status == QuestStatus.COMPLETED || status == QuestStatus.CLAIMED ? 1.0f : 0.0f;
        }

        int totalRequired = 0;
        int totalProgress = 0;

        for (Map.Entry<String, Integer> entry : requirements.entrySet()) {
            String item = entry.getKey();
            int required = entry.getValue();
            int current = progress.getOrDefault(item, 0);

            totalRequired += required;
            totalProgress += Math.min(current, required);
        }

        return totalRequired > 0 ? (float)totalProgress / totalRequired : 0.0f;
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