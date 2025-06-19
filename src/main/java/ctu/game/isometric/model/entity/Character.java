package ctu.game.isometric.model.entity;

import com.badlogic.gdx.utils.Array;
import ctu.game.isometric.model.game.Achievement;
import ctu.game.isometric.model.game.Items;
import ctu.game.isometric.model.quest.Quest;
import ctu.game.isometric.model.quest.QuestTracker;
import ctu.game.isometric.model.world.IsometricMap;
import ctu.game.isometric.util.ItemLoader;

import java.util.*;
import java.util.stream.Collectors;

public class Character {

    private boolean isMoving = false;
    private float animationTime = 0;

    private String direction = "down";
    private String name = "player"; // Character name
    private Gender gender = Gender.MALE;
    private float health = 20; // Health points
    private float gridX, gridY;
    private float maxHealth = 20; // Maximum health points
    private float maxMana = 20;
    private Map<String, Integer> items; // Inventory of items
    private List<String> flags; // Flags for events
    private List<String> quests; // List of quests
    private Map<String, List<String>> status;
    private Map<String, Integer> ettempFlags;
    // Status effects (e.g., buffs, debuffs)

    Map<String, Boolean> isTutorials; // Track if tutorials are completed

    private float damage = 1; // Damage dealt by the character

    private float defend = 1;
    private float mana = 20;
    private int level = 1;// Defense points of the character
    private float exp = 0; // Experience points
    private IsometricMap gameMap;
    private float targetX, targetY;
    private float moveSpeed = 2.5f; // Grid cells per second

    // Quest tracking
    private QuestTracker questTracker;

    private float score;
    // Score for the character

    public boolean levelUp(int level) {
        this.level += level; // Increase level

        float scale = level * 0.4f;

        this.maxHealth = this.maxHealth + 10;
        this.maxMana = this.maxMana + 10;

        this.health = maxHealth;
        this.mana = maxHealth;// Restore health to max
        this.damage += scale; // Increase damage by level
        this.defend += scale; // Increase defense by level

        return true; // Indicates level up occurred
    }


    public int expToLevelUp(float exp) {
        if (exp < 0) {
            throw new IllegalArgumentException("Experience points cannot be negative");
        }
        this.exp += exp; // Add experience points
        int requiredExp = level * 50; // Scale experience requirement dynamically
        if (this.exp >= requiredExp) {
            this.exp -= requiredExp; // Reset exp after leveling up
            levelUp(1); // Level up the character
        }
        return level;
    }


    private Date lastSaveTime;

    private Set<Achievement> achievements;

    public static final String[] VALID_DIRECTIONS = {
            "up", "down", "left", "right", "left_down", "right_down", "left_up", "right_up"
    };

    private String wordFilePath;

    private Set<String> learnedWords;
    private Set<String> newlearneWords;

    public Character() {
        this.flags = new ArrayList<>();
        this.quests = new ArrayList<>();
        this.items = new HashMap<>();
        this.status = new HashMap<>();
        this.status.put("buffs", new ArrayList<>());
        this.status.put("debuffs", new ArrayList<>());
        this.achievements = new HashSet<>();
        this.newlearneWords = new HashSet<>();
        this.learnedWords = new HashSet<>();
        this.learnedWords.add("HELLO");
        this.score = 0;
        this.ettempFlags = new HashMap<>();
        this.ettempFlags.put("quizAttempts", 0);
        this.ettempFlags.put("mulQuizAttempts", 0);
        this.ettempFlags.put("fallen", 0);
        this.ettempFlags.put("wrongWord", 0);

        this.questTracker = new QuestTracker();

        initialTutorial();

        addItem(ItemLoader.getItemByName("CCCD"), 1);
    }


    public void initialTutorial() {
        this.isTutorials = new HashMap<>();

        this.isTutorials.put("movement", false);
        this.isTutorials.put("combat", false);
        this.isTutorials.put("item", false);
        this.isTutorials.put("map", false);
        this.isTutorials.put("quest", false);
        this.isTutorials.put("word", false);
        this.isTutorials.put("achievement", false);
        this.isTutorials.put("dialog", false);
        this.isTutorials.put("npc", false);


        addItem(ItemLoader.getItemByName("CCCD"), 1);

    }


    public boolean isTutorialCompleted(String tutorialType) {
        if (tutorialType == null || tutorialType.isEmpty()) {
            throw new IllegalArgumentException("Tutorial ID cannot be null or empty");
        }
        if (isTutorials == null) {
            return false; // No tutorials initialized
        }
        return isTutorials.getOrDefault(tutorialType, false);
    }

    public void setTutorialCompleted(String tutorialType) {
        if (tutorialType == null || tutorialType.isEmpty()) {
            throw new IllegalArgumentException("Tutorial ID cannot be null or empty");
        }
        if (isTutorials == null) {
            isTutorials = new HashMap<>();
        }
        isTutorials.put(tutorialType, true);
    }

    public Character(float startX, float startY) {
        this.gridX = startX;
        this.gridY = startY;
        this.targetX = startX;
        this.targetY = startY;
        this.flags = new ArrayList<>();
        this.quests = new ArrayList<>();
        this.items = new HashMap<>();
        this.status = new HashMap<>();
        this.status.put("buffs", new ArrayList<>());
        this.status.put("debuffs", new ArrayList<>());
        this.achievements = new HashSet<>();
        this.learnedWords = new HashSet<>();
        this.newlearneWords = new HashSet<>();
        this.learnedWords.add("HELLO");
        this.ettempFlags = new HashMap<>();
        this.ettempFlags.put("quizAttempts", 0);
        this.ettempFlags.put("mulQuizAttempts", 0);
        this.ettempFlags.put("fallen", 0);
        this.ettempFlags.put("wrongWord", 0);
        this.score = 0;
        this.questTracker = new QuestTracker();


        initialTutorial();
    }

    public void teleportToLocation(String location) {
        if (location == null || location.isEmpty()) {
            throw new IllegalArgumentException("Location cannot be null or empty");
        }

        switch (location) {
            case "Location_Village":
                this.gridX = 15;
                this.gridY = 15;
                break;
            case "Location_Home":
                this.gridX = 20;
                this.gridY = 20;
                break;
            default:
                break;
        }
        this.mana = Math.max(0, this.mana - 10); // Reduce mana cost for teleportation
    }

    public QuestTracker getQuestTracker() {
        return questTracker;
    }

    public void setQuestTracker(QuestTracker questTracker) {
        this.questTracker = questTracker;
    }

    public List<Quest> getActiveQuests() {
        return questTracker.getActiveQuests();
    }

    public List<Quest> getCompletedQuests() {
        return questTracker.getCompletedQuests();
    }

    public boolean gameOver() {
        int amount = (items != null) ? items.getOrDefault("Potion of Healing", 0) : 0;
        if (amount <= 0) {
            return true; // Game over if no healing or mana potions left
        } else {
            Items healingPotion = ItemLoader.getItemByName("Potion of Healing");
            if (healingPotion != null) {
                this.mana = healingPotion.getManaCost();
                useItem(healingPotion);
                this.exp = Math.max(0, this.exp - this.exp * 0.1f); // Reset exp to 0 after using potion
                this.ettempFlags.put("fallen", this.ettempFlags.getOrDefault("fallen", 0) + 1);
            }
        }
        return false;
    }

//    // Modify the other levelUp method to return a boolean
//    public boolean levelUp() {
//        if (level < 10) { // Assuming max level is 10
//            level++;
//            maxHealth += 10; // Increase max health by 20 on level up
//            health = maxHealth;
//            mana = maxMana + 10; // Increase max mana by 20 on level up
//            mana = maxMana;// Restore health to max
//            damage += 1; // Increase damage by 2 on level up
//            moveSpeed += 0.5f; // Increase move speed by 0.5 on level up
//            return true; // Indicates level up occurred
//        }
//        return false; // No level up occurred (at max level)
//    }

    // Existing getters/setters...
    private Array<int[]> currentPath = new Array<>();
    private int currentPathIndex = 0;

    public void setPath(Array<int[]> path) {
        this.currentPath = path;
        this.currentPathIndex = 0;
        if (path.size > 0) {
            isMoving = true;
            int[] nextPoint = path.get(0);
            moveToward(nextPoint[0], nextPoint[1]);
        }
    }

    public void removeItem(String itemName, int amount) {
        if (itemName == null || itemName.isEmpty()) {
            throw new IllegalArgumentException("Item name cannot be null or empty");
        }
        if (items == null || !items.containsKey(itemName)) {
            throw new IllegalArgumentException("Item not found in inventory");
        }

        int currentCount = items.get(itemName);
        if (currentCount < amount) {
            throw new IllegalArgumentException("Not enough items to remove");
        }

        if (currentCount > amount) {
            items.put(itemName, currentCount - amount);
        } else {
            items.remove(itemName);
        }

    }

    public boolean hasItem(String itemName) {
        if (items == null || items.isEmpty()) {
            this.items = new HashMap<>();
        }
        return items.containsKey(itemName);
    }

    public void addScore(float score) {
        this.score = Math.max(0, this.score + score);
    }

    public void addItem(Items item, int amount) {
        if (item == null || item.getItemName() == null) {
            throw new IllegalArgumentException("Item or item name cannot be null");
        }

        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        if (items.containsKey(item.getItemName())) {
            items.put(item.getItemName(), items.get(item.getItemName()) + amount);
        } else {
            items.put(item.getItemName(), amount);
        }
    }


    public void recovery() {
        this.health = this.maxHealth;
        this.mana = this.maxMana;
    }

    public void healing(float amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Healing amount cannot be negative");
        }
        if (maxHealth < 0) {
            throw new IllegalStateException("Maximum health cannot be negative");
        }
        this.health = Math.min(this.health + amount, maxHealth);
    }

    public void buff(String effect, float value) {
        switch (effect) {
            case "speed":
                this.moveSpeed = Math.min(3, this.moveSpeed + value);
                break;
            case "strength":
                this.damage = Math.min(10, this.damage + value);
                break;
            case "health":
                this.maxHealth = Math.min(200, this.maxHealth + value);
                break;
            default:
                throw new IllegalArgumentException("Invalid buff effect");
        }
    }

    public void debuff(String effect, float value) {
        switch (effect) {
            case "slow":
                this.moveSpeed = Math.max(2, this.moveSpeed - value);
                break;
            case "curse":
                this.health = Math.max(0, this.health - value);
                break;
        }
    }

    public void useItem(Items item) {
        if (item == null || item.getItemName() == null) {
            throw new IllegalArgumentException("Item or item name cannot be null");
        }
        if (items == null) {
            throw new IllegalStateException("Inventory is not initialized");
        }
        if (!items.containsKey(item.getItemName())) {
            throw new IllegalArgumentException("Item not found in inventory");
        }
        if (items.get(item.getItemName()) <= 0) {
            throw new IllegalArgumentException("No items left to use");
        }


        if (mana < item.getManaCost()) {
            throw new IllegalStateException("Not enough mana to use the item");
        }

        switch (item.getItemEffect()) {
            case "heal":
                healing(item.getValue());
                break;
            case "buff":
                buff("strength", item.getValue());
                break;
            case "debuff":
                debuff("curse", item.getValue());
                break;
            default:
                throw new IllegalArgumentException("Invalid item effect");
        }

        int currentCount = items.get(item.getItemName());
        if (currentCount > 1) {
            items.put(item.getItemName(), currentCount - 1);
        } else {
            items.remove(item.getItemName());
        }
        // Reduce mana cost
        this.mana = Math.max(0, this.mana - item.getManaCost());
    }

    public void deleteItem(Items item) {
        if (item == null || item.getItemName() == null) {
            throw new IllegalArgumentException("Item or item name cannot be null");
        }
        if (items == null || !items.containsKey(item.getItemName())) {
            throw new IllegalArgumentException("Item not found in inventory");
        }
        items.remove(item.getItemName());
    }

    public Map<String, Integer> getBuffItems() {
        return items.entrySet().stream()
                .filter(entry -> {
                    Items item = ItemLoader.getItemByName(entry.getKey());
                    return item != null && item.getItemEffect().equals("buff") || item.getItemEffect().equals("heal");
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public boolean updateDict(String word) {
        if (newlearneWords == null) {
            newlearneWords = new HashSet<>();
        }

        if (word != null && !word.isEmpty()) {
            if (!newlearneWords.contains(word.toUpperCase()) && !learnedWords.contains(word.toUpperCase())) {
                newlearneWords.add(word.toUpperCase());
                return true;
            }
        }
        return false;
    }

    // Check if the character has already learned a word
    public boolean hasLearnedWord(String word) {
        return word != null && learnedWords.contains(word.toUpperCase());
    }

    // Get the count of learned words
    public int getLearnedWordsCount() {
        return learnedWords.size();
    }

    // Getter and setter for JSON serialization
    public Set<String> getLearnedWords() {
        return learnedWords;
    }

    public void setLearnedWords(Set<String> learnedWords) {
        this.learnedWords = learnedWords != null ? learnedWords : new HashSet<>();
    }

    public float getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(float maxHealth) {
        this.maxHealth = maxHealth;
    }

    public void moveToward(float targetX, float targetY) {
        // Ensure target is within map bounds and walkable
        int tx = (int) Math.floor(targetX);
        int ty = (int) Math.floor(targetY);

        if (tx >= 0 && tx < gameMap.getMapWidth() &&
                ty >= 0 && ty < gameMap.getMapHeight() &&
                gameMap.isWalkable(tx, ty)) {

            // Clamp target position to ensure it's safely within grid boundaries
            this.targetX = Math.max(0.001f, Math.min(gameMap.getMapWidth() - 0.001f, targetX));
            this.targetY = Math.max(0.001f, Math.min(gameMap.getMapHeight() - 0.001f, targetY));
            this.isMoving = true;

            // Calculate initial direction
            float dx = this.targetX - gridX;
            float dy = this.targetY - gridY;
            updateDirectionFromVector(dx, dy);
        }
    }


    // In Character class
    public void update(float delta) {
        animationTime += delta;

        if (!isMoving) return;

        float dx = targetX - gridX;
        float dy = targetY - gridY;
        float distanceSquared = dx * dx + dy * dy;

        if (distanceSquared < 0.0001f) {
            gridX = targetX;
            gridY = targetY;

            // Check if we have more points in the path
            if (currentPath.size > 0 && currentPathIndex < currentPath.size - 1) {
                currentPathIndex++;
                int[] nextPoint = currentPath.get(currentPathIndex);
                moveToward(nextPoint[0], nextPoint[1]);
            } else {
                isMoving = false;
                animationTime = 0;
                currentPath.clear();
                currentPathIndex = 0;
            }
        } else {
            // Rest of your existing update code
            float moveAmount = moveSpeed * delta;
            float distance = (float) Math.sqrt(distanceSquared);

            if (moveAmount >= distance) {
                // Check if target position is still valid
                int tx = (int) Math.floor(targetX);
                int ty = (int) Math.floor(targetY);

                if (tx >= 0 && tx < gameMap.getMapWidth() &&
                        ty >= 0 && ty < gameMap.getMapHeight() &&
                        gameMap.isWalkable(tx, ty)) {

                    gridX = targetX;
                    gridY = targetY;
                }
                isMoving = true; // Keep moving if we have more path points
                animationTime = 0;
            } else {
                float ratio = moveAmount / distance;
                float newX = gridX + dx * ratio;
                float newY = gridY + dy * ratio;

                // Check if new position is valid before moving
                int nx = (int) Math.floor(newX);
                int ny = (int) Math.floor(newY);

                if (nx >= 0 && nx < gameMap.getMapWidth() &&
                        ny >= 0 && ny < gameMap.getMapHeight() &&
                        gameMap.isWalkable(nx, ny)) {

                    gridX = newX;
                    gridY = newY;
                    // Recalculate direction vector after position update
                    float newDx = targetX - gridX;
                    float newDy = targetY - gridY;
                    updateDirectionFromVector(newDx, newDy);
                } else {
                    // Stop movement if we hit an invalid tile
                    isMoving = false;
                    animationTime = 0;
                    currentPath.clear();
                    currentPathIndex = 0;
                }
            }
        }
    }

    // Optimized updateDirection method
    private void updateDirectionFromVector(float dx, float dy) {
        // Ignore negligible movement
        if (Math.abs(dx) < 0.1f && Math.abs(dy) < 0.1f) {
            return;
        }

        if (dx > 0 && dy > 0) {
//            (1, 1) → "right_up" (Northeast)
            direction = "up";
        } else if (dx > 0 && dy < 0) {
//            (1, -1) → "right_down" (Southeast)
            direction = "down";
        } else if (dx < 0 && dy > 0) {
//            (-1, 1) → "left_up" (Northwest)
            direction = "right_down";
        } else if (dx < 0 && dy < 0) {
//            (-1, -1) → "left_down" (Southwest) *
            direction = "left_down";
        } else if (dx > 0) {
//            (1, 0) → "up" (North)
            direction = "up";
        } else if (dx < 0) {
//            (-1, 0) → "down" (South) *
            direction = "left_up";
        } else if (dy > 0) {
//            (0, 1) → "right" (East)
            direction = "right_up";
        } else {
//            (0, -1) → "left" (West)
            direction = "left_down";
        }
    }


    public void interactNPC(String npcId) {

    }

    public void updateAchievements(Achievement.AchievementType type, int value) {
        if (type == null) {
            throw new IllegalArgumentException("Achievement type cannot be null");
        }

        if (achievements.isEmpty()) {
            return; // Skip processing if no achievements exist
        }

        // Find existing achievements of the same type
        for (Achievement existingAchievement : achievements) {
            if (existingAchievement.getType() == type) {
                if (existingAchievement.isUnlocked()) {
                    continue;
                }
                existingAchievement.updateProgress(value);

                if (existingAchievement.isUnlocked() &&
                        existingAchievement.getCurrentValue() >= existingAchievement.getTargetValue()) {
                    // If the achievement is now unlocked, set it to unlocked and update the score
                    score += existingAchievement.getTargetValue();
                    existingAchievement.setUnlocked(true);
                }
            }
        }
    }

    public void updateWrongWordCount() {
        int wrongWordCount = ettempFlags.getOrDefault("wrongWord", 0);
        ettempFlags.put("wrongWord", wrongWordCount + 1);
        updateAchievements(Achievement.AchievementType.FAILED_WORD, 1);
    }

    public void setAchievements(HashSet<Achievement> achievements) {
        this.achievements = achievements;
    }

    public Set<Achievement> getAchievements() {
        if (achievements == null) {
            achievements = new HashSet<>();
        }
        return achievements;
    }

    /**
     * Check if character has an achievement
     */
    public boolean hasAchievement(String achievementId) {
        if (achievements == null) return false;

        for (Achievement achievement : achievements) {
            if (achievement.getId().equals(achievementId)) {
                return true;
            }
        }
        return false;
    }


    public float getGridX() {
        return gridX;
    }

    public void setGridX(float gridX) {
        this.gridX = gridX;
    }

    public float getMaxMana() {
        return maxMana;
    }

    public void setMaxMana(float maxMana) {
        this.maxMana = maxMana;
    }

    public float getGridY() {
        return gridY;
    }

    public void setGridY(float gridY) {
        this.gridY = gridY;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public boolean isMoving() {
        return isMoving;
    }

    public void setMoving(boolean moving) {
        isMoving = moving;
    }

    public float getAnimationTime() {
        return animationTime;
    }

    public void setPosition(float x, float y) {
        this.gridX = x;
        this.gridY = y;
        this.targetX = x;
        this.targetY = y;
    }


    public Map<String, Integer> getAttempFlags() {
        return ettempFlags;
    }

    public void setAttempFlags(Map<String, Integer> ettempFlags) {
        this.ettempFlags = ettempFlags;
    }

    public Date getLastSaveTime() {
        return lastSaveTime;
    }

    public void setLastSaveTime(Date lastSaveTime) {
        this.lastSaveTime = lastSaveTime;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getHealth() {
        return health;
    }

    public void setHealth(float health) {
        this.health = health;
    }

    public void setGameMap(IsometricMap gameMap) {
        this.gameMap = gameMap;
    }

    public List<String> getFlags() {
        return flags;
    }

    public void setFlags(List<String> flags) {
        this.flags = flags;
    }


    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }


    public Map<String, Integer> getItems() {
        return items;
    }

    public void setItems(Map<String, Integer> items) {
        this.items = items;
    }

    public void setAnimationTime(float animationTime) {
        this.animationTime = animationTime;
    }

    public IsometricMap getGameMap() {
        return gameMap;
    }

    public int getCurrentPathIndex() {
        return currentPathIndex;
    }

    public void setCurrentPathIndex(int currentPathIndex) {
        this.currentPathIndex = currentPathIndex;
    }

    public Array<int[]> getCurrentPath() {
        return currentPath;
    }

    public void setCurrentPath(Array<int[]> currentPath) {
        this.currentPath = currentPath;
    }

    public void setAchievements(Set<Achievement> achievements) {
        this.achievements = achievements;
    }

    public float getExp() {
        return exp;
    }

    public void setExp(float exp) {
        this.exp = exp;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public float getMana() {
        return mana;
    }

    public void setMana(float mana) {
        this.mana = mana;
    }

    public float getDefend() {
        return defend;
    }

    public void setDefend(float defend) {
        this.defend = defend;
    }

    public Map<String, Integer> getEttempFlags() {
        return ettempFlags;
    }

    public void setEttempFlags(Map<String, Integer> ettempFlags) {
        this.ettempFlags = ettempFlags;
    }

    public float getTargetX() {
        return targetX;
    }

    public void setTargetX(float targetX) {
        this.targetX = targetX;
    }

    public float getTargetY() {
        return targetY;
    }

    public void setTargetY(float targetY) {
        this.targetY = targetY;
    }

    public List<String> getQuests() {
        return quests;
    }

    public void setQuests(List<String> quests) {
        this.quests = quests;
    }

    public Map<String, List<String>> getStatus() {
        return status;
    }

    public void setStatus(Map<String, List<String>> status) {
        this.status = status;
    }

    public float getDamage() {
        return damage;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public void setDamage(int damage) {
        this.damage = damage;
    }

    public float getMoveSpeed() {
        return moveSpeed;
    }

    public void setMoveSpeed(float moveSpeed) {
        this.moveSpeed = moveSpeed;
    }

    public String getWordFilePath() {
        return wordFilePath;
    }

    public void setWordFilePath(String wordFilePath) {
        this.wordFilePath = wordFilePath;
    }

    public Map<String, Boolean> getIsTutorials() {
        return isTutorials;
    }

    public void setIsTutorials(Map<String, Boolean> isTutorials) {
        this.isTutorials = isTutorials;
    }

    public Set<String> getNewlearneWords() {
        return newlearneWords;
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }

    public void setNewlearneWords(Set<String> newlearneWords) {
        this.newlearneWords = newlearneWords;
    }
}

