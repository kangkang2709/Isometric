package ctu.game.isometric.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import java.util.HashMap;
import java.util.Map;

public class AnimationManager {
    private Map<String, Animation<TextureRegion>> characterAnimations = new HashMap<>();

    public void loadCharacterAnimations(String idleSpritePath, String walkSpritePath) {
        // Load texture sheets
        Texture idleSpriteSheet = new Texture(Gdx.files.internal(idleSpritePath));
        Texture walkSpriteSheet = new Texture(Gdx.files.internal(walkSpritePath));

        // Apply texture filtering for smoother rendering
        idleSpriteSheet.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        walkSpriteSheet.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        // Split sprites
        TextureRegion[][] idleTmp = TextureRegion.split(idleSpriteSheet, 48, 64);
        TextureRegion[][] walkTmp = TextureRegion.split(walkSpriteSheet, 48, 64);

        // Main directions (reduced set that matches your sprite sheet)
        String[] directions = {"down", "left_down", "left_up", "right_down", "right_up", "up"};

        for (int i = 0; i < directions.length; i++) {
            // IDLE: Create with just first frame
            TextureRegion[] idleFrames = new TextureRegion[8];
            for (int j = 0; j < 8; j++) {
                idleFrames[j] = idleTmp[i][j];
            }
            // Create idle animation with SLOWER frame rate for smoother idle animation
            characterAnimations.put(directions[i] + "_idle", new Animation<>(0.25f, idleFrames));

            // WALK: Create with all 8 frames
            TextureRegion[] walkFrames = new TextureRegion[8];
            for (int j = 0; j < 8; j++) {
                walkFrames[j] = walkTmp[i][j];
            }
            characterAnimations.put(
                    directions[i] + "_walk",
                    new Animation<>(0.1f, walkFrames)
            );
        }
    }

    // Helper method to efficiently extract frames
    private TextureRegion[] getFramesForDirection(TextureRegion[] sourceRow, int frameCount) {
        TextureRegion[] frames = new TextureRegion[frameCount];
        System.arraycopy(sourceRow, 0, frames, 0, frameCount);
        return frames;
    }

    public TextureRegion getCharacterFrame(String direction, boolean isMoving, float stateTime) {
        String animKey = direction + (isMoving ? "_walk" : "_idle");

        // If animation doesn't exist, find a fallback
        if (!characterAnimations.containsKey(animKey)) {
            if (direction.contains("right")) {
                animKey = "right_down" + (isMoving ? "_walk" : "_idle");
            } else {
                animKey = "left_down" + (isMoving ? "_walk" : "_idle");
            }
        }

        Animation<TextureRegion> animation = characterAnimations.get(animKey);
        if (animation == null) {
            // Ultimate fallback
            return characterAnimations.get("right_down").getKeyFrame(0);
        }

        // Return proper frame with looping enabled
        return animation.getKeyFrame(stateTime, true);
    }

    public Map<String, Animation<TextureRegion>> getCharacterAnimations() {
        return characterAnimations;
    }

    public void setCharacterAnimations(Map<String, Animation<TextureRegion>> characterAnimations) {
        this.characterAnimations = characterAnimations;
    }
}