package ctu.game.isometric.view.renderer;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import ctu.game.isometric.controller.GameController;
import ctu.game.isometric.model.entity.Character;
import ctu.game.isometric.util.AssetManager;

public class ArrowIndicator {
    private GameController gameController;
    private AssetManager assetManager;
    private float tileWidth = 64;
    private float tileHeight = 32;

    // Animation properties for pulsing effect
    private float animTime = 0;
    private final float pulseSpeed = 2.0f;
    private final float minAlpha = 0.5f;
    private final float maxAlpha = 1.0f;

    public ArrowIndicator(GameController gameController, AssetManager assetManager) {
        this.gameController = gameController;
        this.assetManager = assetManager;
    }

    public void update(float delta) {
        animTime += delta * pulseSpeed;
        if (animTime > 2.0f * Math.PI) {
            animTime -= 2.0f * Math.PI;
        }
    }

    public void render(SpriteBatch batch, float offsetX, float offsetY) {
        Character character = gameController.getCharacter();

        // Calculate current alpha for pulsing effect
        float alpha = minAlpha + (float)(Math.sin(animTime) + 1) / 2 * (maxAlpha - minAlpha);
        batch.setColor(1, 1, 1, alpha);

        // Check each direction and render arrows if movement is possible
        checkAndRenderArrow(batch, character, 0, -1, "up", offsetX, offsetY);
        checkAndRenderArrow(batch, character, 0, 1, "down", offsetX, offsetY);
        checkAndRenderArrow(batch, character, -1, 0, "left", offsetX, offsetY);
        checkAndRenderArrow(batch, character, 1, 0, "right", offsetX, offsetY);

        // Reset batch color
        batch.setColor(1, 1, 1, 1);
    }

    private void checkAndRenderArrow(SpriteBatch batch, Character character, int dx, int dy,
                                     String direction, float offsetX, float offsetY) {
        if (gameController.canMove(dx, dy)) {
            float gridX = character.getGridX() + dx;
            float gridY = character.getGridY() + dy;

            float screenX = getScreenX(gridX, gridY, offsetX);
            float screenY = getScreenY(gridX, gridY, offsetY);

            // Add some offset to position arrows nicely
            screenY += 16; // Lift the arrow up a bit from the tile

            Texture arrowTexture = assetManager.getTexture("ui/arrow_" + direction + ".png");
            if (arrowTexture != null) {
                batch.draw(arrowTexture, screenX - 16, screenY - 16, 32, 32);
            }
        }
    }

    // Convert grid coordinates to screen coordinates for isometric rendering
    private float getScreenX(float gridX, float gridY, float offsetX) {
        return (gridX - gridY) * (tileWidth / 2) + offsetX;
    }

    private float getScreenY(float gridX, float gridY, float offsetY) {
        return (gridX + gridY) * (tileHeight / 2) + offsetY;
    }
}