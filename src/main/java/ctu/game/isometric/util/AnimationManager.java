package ctu.game.isometric.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import java.util.HashMap;
import java.util.Map;

public class AnimationManager {
    private Map<String, Animation<TextureRegion>> characterAnimations = new HashMap<>();
    private Map<String, Animation<TextureRegion>> npcAnimations = new HashMap<>();

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
            characterAnimations.put(directions[i] + "_idle", new Animation<>(0.35f, idleFrames));

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


    public void loadNpcAnimations(String npcId, String idleSpritePath, String dialogueSpritePath) {
        // Load texture sheets
        Texture idleSpriteSheet = new Texture(Gdx.files.internal(idleSpritePath));
        Texture dialogueSpriteSheet = new Texture(Gdx.files.internal(dialogueSpritePath));

        // Apply texture filtering for smoother rendering
        idleSpriteSheet.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        dialogueSpriteSheet.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        // Calculate number of frames based on texture width (each frame is 128x128)
        int idleFrameCount = idleSpriteSheet.getWidth() / 128;
        int dialogueFrameCount = dialogueSpriteSheet.getWidth() / 128;

        // Create idle animation frames
        TextureRegion[] idleFrames = new TextureRegion[idleFrameCount];
        for (int i = 0; i < idleFrameCount; i++) {
            idleFrames[i] = new TextureRegion(idleSpriteSheet, i * 128, 0, 128, 128);
        }

        // Create dialogue animation frames
        TextureRegion[] dialogueFrames = new TextureRegion[dialogueFrameCount];
        for (int i = 0; i < dialogueFrameCount; i++) {
            dialogueFrames[i] = new TextureRegion(dialogueSpriteSheet, i * 128, 0, 128, 128);
        }

        // Add animations to map with unique keys for this NPC
        npcAnimations.put(npcId + "_idle", new Animation<>(0.1f, idleFrames));
        npcAnimations.put(npcId + "_dialogue", new Animation<>(0.1f, dialogueFrames));
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



    public TextureRegion getNpcFrame(String npcId, String behaviorState, float stateTime) {
        String animKey = npcId + "_" + behaviorState.toLowerCase();

        // If animation doesn't exist, use idle as fallback
        if (!npcAnimations.containsKey(animKey)) {
            animKey = npcId + "_idle";

            // If idle doesn't exist either, return null
            if (!npcAnimations.containsKey(animKey)) {
                return null;
            }
        }

        Animation<TextureRegion> animation = npcAnimations.get(animKey);
        return animation.getKeyFrame(stateTime, true);
    }

    public Map<String, Animation<TextureRegion>> getCharacterAnimations() {
        return characterAnimations;
    }

    public void setCharacterAnimations(Map<String, Animation<TextureRegion>> characterAnimations) {
        this.characterAnimations = characterAnimations;
    }
    public void dispose() {
        // Dispose character animations
        for (Animation<TextureRegion> animation : characterAnimations.values()) {
            if (animation.getKeyFrames().length > 0) {
                Texture texture = animation.getKeyFrames()[0].getTexture();
                if (texture != null) texture.dispose();
            }
        }
        characterAnimations.clear();

        // Dispose NPC animations
        for (Animation<TextureRegion> animation : npcAnimations.values()) {
            if (animation.getKeyFrames().length > 0) {
                Texture texture = animation.getKeyFrames()[0].getTexture();
                if (texture != null) texture.dispose();
            }
        }
        npcAnimations.clear();
    }
}