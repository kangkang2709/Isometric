package ctu.game.isometric.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

import java.util.HashMap;
import java.util.Map;

public class AnimationManager {
    private Map<String, Animation<TextureRegion>> animations;

    public AnimationManager() {
        animations = new HashMap<>();
    }

    public void loadCharacterAnimations(String baseTexturePath) {
        // Load sprite sheet
        Texture spriteSheet = new Texture(Gdx.files.internal(baseTexturePath));

        // Assuming your sprite sheet has multiple rows for different directions
        // and each row contains frames for that direction
        int frameWidth = 32; // Adjust based on your sprite sheet
        int frameHeight = 32; // Adjust based on your sprite sheet

        // Create texture regions for each direction
        createAnimation(spriteSheet, "down_idle", 0, 0, frameWidth, frameHeight, 1, 0.5f);
        createAnimation(spriteSheet, "down_walk", 0, 0, frameWidth, frameHeight, 4, 0.15f);
        createAnimation(spriteSheet, "up_idle", 0, frameHeight, frameWidth, frameHeight, 1, 0.5f);
        createAnimation(spriteSheet, "up_walk", 0, frameHeight, frameWidth, frameHeight, 4, 0.15f);
        createAnimation(spriteSheet, "left_idle", 0, frameHeight*2, frameWidth, frameHeight, 1, 0.5f);
        createAnimation(spriteSheet, "left_walk", 0, frameHeight*2, frameWidth, frameHeight, 4, 0.15f);
        createAnimation(spriteSheet, "right_idle", 0, frameHeight*3, frameWidth, frameHeight, 1, 0.5f);
        createAnimation(spriteSheet, "right_walk", 0, frameHeight*3, frameWidth, frameHeight, 4, 0.15f);
    }

    private void createAnimation(Texture sheet, String name, int x, int y, int frameWidth, int frameHeight,
                                 int frameCount, float frameDuration) {
        Array<TextureRegion> frames = new Array<>(TextureRegion.class);

        for (int i = 0; i < frameCount; i++) {
            frames.add(new TextureRegion(sheet, x + i * frameWidth, y, frameWidth, frameHeight));
        }

        animations.put(name, new Animation<>(frameDuration, frames, Animation.PlayMode.LOOP));
    }

    public TextureRegion getFrame(String direction, boolean isMoving, float stateTime) {
        String key = direction + (isMoving ? "_walk" : "_idle");
        Animation<TextureRegion> animation = animations.get(key);

        if (animation != null) {
            return animation.getKeyFrame(stateTime);
        }

        return animations.get("down_idle").getKeyFrame(0);
    }
    public TextureRegion getCharacterFrame(String direction, boolean isMoving, float stateTime) {
        return getFrame(direction, isMoving, stateTime);
    }
    public void dispose() {
        // Dispose of textures if needed
    }
}