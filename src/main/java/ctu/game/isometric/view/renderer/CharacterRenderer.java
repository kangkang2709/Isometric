package ctu.game.isometric.view.renderer;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import ctu.game.isometric.model.entity.Character;
import ctu.game.isometric.util.AssetManager;
import ctu.game.isometric.util.AnimationManager;

public class CharacterRenderer {
    private Character character;
    private AssetManager assetManager;
    private AnimationManager animationManager;

    private final float tileWidth = 64;
    private final float tileHeight = 32;

    public CharacterRenderer(Character character, AssetManager assetManager) {
        this.character = character;
        this.assetManager = assetManager;
        this.animationManager = new AnimationManager();
        // Initialize animations with the character's sprite sheet
        this.animationManager.loadCharacterAnimations("characters/player.png");
    }

    public void render(SpriteBatch batch, float offsetX, float offsetY) {
        if (character == null) return;

        TextureRegion currentFrame = animationManager.getFrame(
                character.getDirection(),
                character.isMoving(),
                character.getAnimationTime()
        );

        float screenX = getScreenX(character.getGridX(), character.getGridY(), offsetX);
        float screenY = getScreenY(character.getGridX(), character.getGridY(), offsetY);

        batch.draw(currentFrame, screenX, screenY);
    }

    // Convert grid coordinates to screen coordinates for isometric rendering
    private float getScreenX(float gridX, float gridY, float offsetX) {
        // Isometric transformation: (x - y) * (tileWidth/2)
        return (gridX - gridY) * (tileWidth / 2) + offsetX;
    }

    private float getScreenY(float gridX, float gridY, float offsetY) {
        // Isometric transformation: (x + y) * (tileHeight/2)
        return (gridX + gridY) * (tileHeight / 2) + offsetY;
    }

    // Add this method to help with disposing resources when needed
    public void dispose() {
        // Dispose of any resources if necessary
        if (animationManager != null) {
            animationManager.dispose();
        }
    }
}