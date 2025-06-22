package ctu.game.isometric.view.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import ctu.game.isometric.util.AnimationManager;

public class DiceRenderer {
    private static final float DICE_SIZE = 90;
    private static final float DICE_ROLL_DURATION = 1.6f;
    private static final float BOUNCE_DURATION = 0.5f;

    private AnimationManager animationManager;
    private float diceRollingTime = 0f;
    private boolean isRolling = false;
    private int currentFaceValue = 1;
    private float diceX, diceY;
    private float scale = 1f;
    private float rotation = 0f;
    private float bounceTime = 0f;

    private ParticleEffect rollEffect;

    public DiceRenderer(AnimationManager animationManager, float x, float y) {
        this.animationManager = animationManager;
        this.diceX = x;
        this.diceY = y;

        // Initialize particle effect
        rollEffect = new ParticleEffect();
        rollEffect.load(Gdx.files.internal("effects/dice_roll/dice_roll.p"), Gdx.files.internal("effects/dice_roll/"));
        rollEffect.setPosition(diceX + DICE_SIZE/2, diceY + DICE_SIZE/2);
    }

    public int rollDice() {
        currentFaceValue = MathUtils.random(1, 20);
        isRolling = true;
        diceRollingTime = 0f;
        bounceTime = 0f;
        rollEffect.start();
        return currentFaceValue;
    }

    public void update(float delta) {
        if (isRolling) {
            diceRollingTime += delta;

            // Update scale and rotation during rolling
            scale = 1f + 0.3f * MathUtils.sin(diceRollingTime * 10);
            rotation = diceRollingTime * 360 % 360;

            // Check if rolling animation is complete
            if (diceRollingTime >= DICE_ROLL_DURATION) {
                isRolling = false;
                bounceTime = 0f;
            }
        } else if (bounceTime < BOUNCE_DURATION) {
            // Apply bounce effect when showing the result
            bounceTime += delta;
            float progress = Math.min(bounceTime / BOUNCE_DURATION, 1.0f);
            scale = 1f + 0.5f * Interpolation.bounceOut.apply(1f - progress);
            rotation = 0f;
        }

        rollEffect.update(delta);
    }

    public void render(SpriteBatch batch) {
        // Draw particle effect under the dice
        rollEffect.draw(batch);

        TextureRegion frame = animationManager.getDiceFrame(isRolling, currentFaceValue, diceRollingTime);

        // Draw dice with scale and rotation
        batch.draw(
                frame,
                diceX - (DICE_SIZE * scale - DICE_SIZE) / 2,  // Adjust position to keep centered
                diceY - (DICE_SIZE * scale - DICE_SIZE) / 2,
                DICE_SIZE / 2,  // Origin X
                DICE_SIZE / 2,  // Origin Y
                DICE_SIZE,      // Width
                DICE_SIZE,      // Height
                scale,          // Scale X
                scale,          // Scale Y
                rotation        // Rotation
        );
    }

    public boolean isAnimating() {
        return isRolling || bounceTime < BOUNCE_DURATION;
    }

    public boolean handleClick(float screenX, float screenY) {
        // Check if click is within dice bounds
        return !isAnimating() &&
                screenX >= diceX && screenX <= diceX + DICE_SIZE &&
                screenY >= diceY && screenY <= diceY + DICE_SIZE;
    }

    public int getCurrentFaceValue() {
        return currentFaceValue;
    }

    public void dispose() {
        rollEffect.dispose();
    }
}