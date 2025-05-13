package ctu.game.isometric.model.entity;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import ctu.game.isometric.model.dictionary.Word;
import ctu.game.isometric.model.game.Items;
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
    private float health = 100; // Health points
    private float gridX, gridY;
    private float maxHealth = 100; // Maximum health points

    private Map<String, Integer> items; // Inventory of items
    private List<String> flags; // Flags for events
    private List<String> quests; // List of quests
    private Map<String, List<String>> status;  // Status effects (e.g., buffs, debuffs)
    private float damage = 1; // Damage dealt by the character

    private IsometricMap gameMap;
    private float targetX, targetY;
    private float moveSpeed = 2.5f; // Grid cells per second
    private static final float DIAGONAL_THRESHOLD = 0.3f; // For determining diagonal movement

    private float score; // Score for the character

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

        this.newlearneWords = new HashSet<>();
        this.learnedWords = new HashSet<>();
        this.learnedWords.add("HELLO");
        this.score=0;
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

        this.learnedWords = new HashSet<>();
        this.newlearneWords = new HashSet<>();
        this.learnedWords.add("HELLO");

        this.score=0;

    }

    // Existing getters/setters...


    public void addItem(Items item, int amount) {
        if (item == null || item.getItemName() == null) {
            throw new IllegalArgumentException("Item or item name cannot be null");
        }

        if (items == null) {
            items = new HashMap<>();
        }

        if (items.containsKey(item.getItemName())) {
            items.put(item.getItemName(), items.get(item.getItemName()) + amount);
        } else {
            items.put(item.getItemName(), amount);
        }
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
        if (items == null || !items.containsKey(item.getItemName())) {
            throw new IllegalArgumentException("Item not found in inventory");
        }
        if (items.get(item.getItemName()) <= 0) {
            throw new IllegalArgumentException("No items left to use");
        }

        int currentCount = items.get(item.getItemName());
        if (currentCount > 1) {
            items.put(item.getItemName(), currentCount - 1);
        } else {
            items.remove(item.getItemName());
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

    public Map<String, Integer> getItemsWithoutDebuff() {
        return items.entrySet().stream()
                .filter(entry -> {
                    Items item = ItemLoader.getItemByName(entry.getKey());
                    return item != null && !item.getItemEffect().equals("debuff");
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public void addLearnedWord(String word) {
        if (newlearneWords == null) {
            newlearneWords = new HashSet<>();
        }

        if (word != null && !word.isEmpty()) {
            if (learnedWords != null && !learnedWords.contains(word.toUpperCase())) {
                newlearneWords.add(word.toUpperCase());
            }
        }
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
            isMoving = false;
            animationTime = 0; // Reset animation time when stopping
        } else {
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
                isMoving = false;
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

    public float getGridX() {
        return gridX;
    }

    public void setGridX(float gridX) {
        this.gridX = gridX;
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

    public Set<String> getNewlearneWords() {
        return newlearneWords;
    }

    public void setNewlearneWords(Set<String> newlearneWords) {
        this.newlearneWords = newlearneWords;
    }
}