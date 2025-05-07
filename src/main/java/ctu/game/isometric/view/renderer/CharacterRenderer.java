package ctu.game.isometric.view.renderer;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import ctu.game.isometric.model.entity.Character;
import ctu.game.isometric.model.entity.Gender;
import ctu.game.isometric.util.AssetManager;
import ctu.game.isometric.util.AnimationManager;

public class CharacterRenderer {
    private Character character;
    private AssetManager assetManager;
    private AnimationManager animationManager;
    private MapRenderer mapRenderer;
    Gender gender;

    public CharacterRenderer(Character character, AssetManager assetManager, MapRenderer mapRenderer) {
        this.character = character;
        character.setGameMap(mapRenderer.getMap());
        this.assetManager = assetManager;
        this.mapRenderer = mapRenderer;
        this.animationManager = assetManager.getAnimationManager();

        // Use the character's gender instead of the uninitialized field
        Gender characterGender = character.getGender();
        if (characterGender == null || characterGender.equals(Gender.MALE))
            this.animationManager.loadCharacterAnimations("characters/idle.png", "characters/walk.png");
        else
            this.animationManager.loadCharacterAnimations("characters/female_idle.png", "characters/female_walk.png");
    }

    public void render(SpriteBatch batch) {
        float gridX = character.getGridX();
        float gridY = character.getGridY();

        float[] screenPos = mapRenderer.toIsometric(gridX, gridY);

        float isoX = screenPos[0];
        float isoY = screenPos[1];

        // Get animation frame with translated direction
        String direction = translateDirection(character.getDirection());
        TextureRegion currentFrame = animationManager.getCharacterFrame(
                direction,
                character.isMoving(),
                character.getAnimationTime()
        );
        // Position character at the center of the tile
        float offsetPlayerX = 11; // Half of sprite width (48/2)
        float offsetPlayerY = -5; // Position the feet at tile base (character sprite height - tile height)

        batch.draw(currentFrame, isoX + offsetPlayerX, isoY + offsetPlayerY);
    }

    // Convert simplified direction to sprite sheet direction
    private String translateDirection(String direction) {
        // Map all 8-way directions to our available 6 sprite directions
        switch(direction) {
            case "up":
                return "up";
            case "down":
                return "down";
            case "left":
                return "left_down"; // In isometric, pure left maps to left_down
            case "right":
                return "right_down"; // In isometric, pure right maps to right_down
            case "left_up":
                return "left_up";
            case "right_up":
                return "right_up";
            case "left_down":
                return "left_down";
            default:
                return "right_down";
        }
    }

}