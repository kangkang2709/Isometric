package ctu.game.isometric.model.game;

public class QuestObjective {
    private String description;
    private int required;
    private int current;

    // Add default constructor for Jackson
    public QuestObjective() {
    }

    public QuestObjective(String description, int required) {
        this.description = description;
        this.required = required;
        this.current = 0;
    }

    public boolean isComplete() {
        return current >= required;
    }

    public void updateProgress(int amount) {
        current += amount;
        if (current > required) {
            current = required;
        }
    }

    // Getters and setters (add setters for Jackson)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getRequired() {
        return required;
    }

    public void setRequired(int required) {
        this.required = required;
    }

    public int getCurrent() {
        return current;
    }

    public void setCurrent(int current) {
        this.current = current;
    }
}
