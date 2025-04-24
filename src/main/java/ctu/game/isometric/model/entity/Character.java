package ctu.game.isometric.model.entity;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import ctu.game.isometric.model.world.IsometricMap;

import java.util.List;
import java.util.Map;

public class Character {
    private float gridX, gridY;
    private String direction = "down";
    private boolean isMoving = false;
    private float animationTime = 0;
    private String name = "player"; // Character name
    private Gender gender = Gender.MALE;
    private int health = 100; // Health points
    private Map<String,?> items; // Inventory items
    private List<String> flags; // Flags for quests or events

    // For smooth movement

    private IsometricMap gameMap;
    private float targetX, targetY;
    private float moveSpeed = 2.5f; // Grid cells per second
    private static final float DIAGONAL_THRESHOLD = 0.3f; // For determining diagonal movement

    public static final String[] VALID_DIRECTIONS = {
            "up", "down", "left", "right", "left_down", "right_down", "left_up", "right_up"
    };

    public Character(float startX, float startY) {
        this.gridX = startX;
        this.gridY = startY;
        this.targetX = startX;
        this.targetY = startY;
    }

    // Existing getters/setters...

    public void moveToward(float targetX, float targetY) {
        // Ensure target is within map bounds and walkable
        int tx = (int) Math.floor(targetX);
        int ty = (int) Math.floor(targetY);

        if (tx >= 0 && tx < gameMap.getMapWidth() &&
                ty >= 0 && ty < gameMap.getMapHeight() &&
                gameMap.isWalkable(tx, ty)) {

            this.targetX = targetX;
            this.targetY = targetY;
            this.isMoving = true;

            // Calculate initial direction
            float dx = targetX - gridX;
            float dy = targetY - gridY;
            updateDirectionFromVector(dx, dy);
        }
    }

    // Optimized update method
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
                    updateDirectionFromVector(dx, dy);
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

        if (dx > 0) {
            direction = "up";
        } else if (dx < 0) {
            direction = "left_up";
        } else if (dy > 0) {
            direction = "right_up";
        } else {
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

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
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

    public Map<String, ?> getItems() {
        return items;
    }

    public void setItems(Map<String, ?> items) {
        this.items = items;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }
}