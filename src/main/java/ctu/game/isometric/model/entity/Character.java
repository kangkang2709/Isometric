package ctu.game.isometric.model.entity;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

public class Character {
    private float gridX, gridY;
    private String texturePath;
    private String direction = "down"; // down, up, left, right
    private boolean isMoving = false;
    private float animationTime = 0;

    // Animation constants
    private static final float FRAME_DURATION = 0.15f;

    public Character(String texturePath, float startX, float startY) {
        this.texturePath = texturePath;
        this.gridX = startX;
        this.gridY = startY;
    }

    public float getGridX() { return gridX; }
    public float getGridY() { return gridY; }
    public String getTexturePath() { return texturePath; }
    public String getDirection() { return direction; }
    public boolean isMoving() { return isMoving; }
    public float getAnimationTime() { return animationTime; }

    public void setPosition(float x, float y) {
        // Update direction based on movement
        updateDirection(x, y);

        this.gridX = x;
        this.gridY = y;
    }

    public void update(float delta) {
        animationTime += delta;
    }

    public void setMoving(boolean moving) {
        this.isMoving = moving;
    }

    private void updateDirection(float newX, float newY) {
        float dx = newX - gridX;
        float dy = newY - gridY;

        if (dx > 0) direction = "right";
        else if (dx < 0) direction = "left";
        else if (dy > 0) direction = "down";
        else if (dy < 0) direction = "up";
    }
}