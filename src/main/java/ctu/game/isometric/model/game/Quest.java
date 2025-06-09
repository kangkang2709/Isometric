package ctu.game.isometric.model.game;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class Quest {
    private String questId;
    private String questName;
    private String questDescription;

    @JsonProperty("isCompleted")  // Add this annotation to map the JSON field name
    private boolean isCompleted;

    private List<QuestObjective> objectives;
    private Reward reward;

    public Quest() {
    }

    public Quest(String questName, String questDescription) {
        this.questId = questName.toLowerCase().replace(' ', '_');
        this.questName = questName;
        this.questDescription = questDescription;
        this.isCompleted = false;
        this.objectives = new ArrayList<>();
    }

    public void addObjective(String description, int required) {
        objectives.add(new QuestObjective(description, required));
    }

    public boolean checkObjectivesComplete() {
        return objectives.stream().allMatch(QuestObjective::isComplete);
    }

    public void updateObjective(String description, int progress) {
        for (QuestObjective objective : objectives) {
            if (objective.getDescription().equals(description)) {
                objective.updateProgress(progress);
                break;
            }
        }
    }

    public void setQuestId(String questId) {
        this.questId = questId;
    }

    public String getQuestDescription() {
        return questDescription;
    }

    public void setQuestDescription(String questDescription) {
        this.questDescription = questDescription;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public List<QuestObjective> getObjectives() {
        return objectives;
    }

    public void setObjectives(List<QuestObjective> objectives) {
        this.objectives = objectives;
    }

    // Getters/setters
    public String getQuestId() {
        return questId;
    }

    public String getQuestName() {
        return questName;
    }

    public void setQuestName(String questName) {
        this.questName = questName;
    }

    public Reward getReward() {
        return reward;
    }

    public void setReward(Reward reward) {
        this.reward = reward;
    }


}


