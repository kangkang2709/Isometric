package ctu.game.isometric.model.game;

public class Achievement {
    private String id;
    private String title;
    private String description;
    private String iconPath;
    private boolean unlocked;
    private AchievementType type;
    private int targetValue;
    private int currentValue;
    private String targetEnemy;

    public enum AchievementType {
        WORD_COUNT,      // Number of words found
        COMBAT_WIN,      // Number of combat victories
        SPECIFIC_WORD,   // Find specific word
        ENEMY_DEFEAT,
        FALLEN,
        FAILED_WORD,
        ENEMY_WIN_1,
        ENEMY_WIN_2,
        ENEMY_WIN_3,
        ENEMY_WIN
    }
    public void update(int value) {
        this.currentValue = value;
    }
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIconPath() { return iconPath; }
    public void setIconPath(String iconPath) { this.iconPath = iconPath; }

    public boolean isUnlocked() { return unlocked; }
    public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }

    public AchievementType getType() { return type; }
    public void setType(AchievementType type) { this.type = type; }

    public int getTargetValue() { return targetValue; }
    public void setTargetValue(int targetValue) { this.targetValue = targetValue; }

    public int getCurrentValue() { return currentValue; }
    public void setCurrentValue(int currentValue) { this.currentValue = currentValue; }

    public void updateProgress(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Progress amount cannot be negative.");
        }
        currentValue += amount;
        if (currentValue >= targetValue && !unlocked) {
            unlocked = true;
        }
    }

    public String getTargetEnemy() {
        return targetEnemy;
    }

    public void setTargetEnemy(String targetEnemy) {
        this.targetEnemy = targetEnemy;
    }
}