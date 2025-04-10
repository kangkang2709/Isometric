package ctu.game.isometric.model.entity;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

public class Character {
    private float gridX, gridY;
    private String texturePath;
    private String direction = "down";
    private boolean isMoving = false;
    private float animationTime = 0;

    // For smooth movement
    private float targetX, targetY;
    private float moveSpeed = 2.0f; // Grid cells per second
    private static final float DIAGONAL_THRESHOLD = 0.3f; // For determining diagonal movement

    public static final String[] VALID_DIRECTIONS = {
            "up", "down", "left", "right", "left_down", "right_down", "left_up", "right_up"
    };

    public Character(String texturePath, float startX, float startY) {
        this.texturePath = texturePath;
        this.gridX = startX;
        this.gridY = startY;
        this.targetX = startX;
        this.targetY = startY;
    }

    // Existing getters/setters...

    public void moveToward(float targetX, float targetY) {
        this.targetX = targetX;
        this.targetY = targetY;
        this.isMoving = true;

        // Calculate initial direction
        float dx = targetX - gridX;
        float dy = targetY - gridY;
        updateDirectionFromVector(dx, dy);
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
                gridX = targetX;
                gridY = targetY;
                isMoving = false;
                animationTime = 0; // Reset animation time when stopping
            } else {
                float ratio = moveAmount / distance;
                float newX = gridX + dx * ratio;
                float newY = gridY + dy * ratio;

                // Ensure we're not moving out of bounds
                gridX = newX;
                gridY = newY;
                updateDirectionFromVector(dx, dy);
            }
        }
    }

    // Optimized updateDirection method
    private void updateDirectionFromVector(float dx, float dy) {
        // Ignore negligible movement
        if (Math.abs(dx) < 0.1f && Math.abs(dy) < 0.1f) {
            return;
        }


        if (Math.abs(dy) > 0.8f && Math.abs(dx) < 0.2f) {
            direction = dy > 0 ? "left_up" : "up";
            return;
        }
        // For pure grid directions (the common case in grid movement)
        if (Math.abs(dx) > 0.8f && Math.abs(dy) < 0.2f) {
            direction = dx > 0 ? "right_up" : "left_down";
            return;
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
}