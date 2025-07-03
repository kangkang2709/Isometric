package ctu.game.isometric.model.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import ctu.game.isometric.controller.GameController;
import ctu.game.isometric.controller.quiz.QuizCompletionListener;
import ctu.game.isometric.util.AnimationManager;

import java.util.Map;

public class Dice {
    private final AnimationManager animationManager;
    private final float diceX;
    private final float diceY;
    private final ParticleEffect rollEffect;

    private int currentFaceValue = 1;
    private boolean isRolling = false;
    private float diceRollingTime = 0f;
    private float bounceTime = 0f;
    private float scale = 1f;
    private float rotation = 0f;

    private static final float DICE_ROLL_DURATION = 1f;
    private static final float BOUNCE_DURATION = 0.5f;
    private static final int DICE_SIZE = 90;
    private static final int MIN_DICE_VALUE = 1;
    private static final int MAX_DICE_VALUE = 20;

    private final GameController gameController;
    private final BitmapFont font = new BitmapFont();

    private QuizCompletionListener completionListener;

    private boolean isBonusRoll = false;
    private int bonusCount = 0;
    private boolean isSuccessful = false;


    public Dice(AnimationManager animationManager, float x, float y, GameController gameController) {
        this.animationManager = animationManager;
        this.diceX = x + 4;
        this.diceY = y;
        this.gameController = gameController;

        rollEffect = new ParticleEffect();
        rollEffect.load(Gdx.files.internal("effects/dice_roll/dice_roll.p"), Gdx.files.internal("effects/dice_roll/"));
        rollEffect.setPosition(diceX + DICE_SIZE / 2f, diceY + DICE_SIZE / 2f);
    }

    public void setCompletionListener(QuizCompletionListener listener) {
        this.completionListener = listener;
    }

    public int rollDice() {
        if (gameController.getCharacter().isMoving()) return 0;

        isRolling = true;
        diceRollingTime = 0f;
        bounceTime = 0f;
        rollEffect.start();

        currentFaceValue = MathUtils.random(MIN_DICE_VALUE, MAX_DICE_VALUE);
        isSuccessful = true;
        return currentFaceValue;
    }

    public int rollDice(int target) {
        if (gameController.getCharacter().isMoving()) return 0;

        if (gameController.getCurrentEvent() == null) {
            return 0;
        }

        if (isBonusRoll) updateBonusRoll();

        isRolling = true;
        diceRollingTime = 0f;
        bounceTime = 0f;
        rollEffect.start();

        currentFaceValue = MathUtils.random(MIN_DICE_VALUE,  3);
        isSuccessful = currentFaceValue >= target;

        return currentFaceValue;
    }

    public void update(float delta) {
        if (isRolling) {
            diceRollingTime += delta;

            scale = 1f + 0.3f * MathUtils.sin(diceRollingTime * 10);
            rotation = diceRollingTime * 360 % 360;

            if (diceRollingTime >= DICE_ROLL_DURATION) {
                isRolling = false;
                bounceTime = 0f;
                if (completionListener != null) {
                    completionListener.onQuizCompleted(isSuccessful);
                }
            }
        } else if (bounceTime < BOUNCE_DURATION) {
            bounceTime += delta;
            float progress = Math.min(bounceTime / BOUNCE_DURATION, 1.0f);
            scale = 1f + 0.5f * Interpolation.bounceOut.apply(1f - progress);
            rotation = 0f;
        }

        rollEffect.update(delta);
    }

    public void render(SpriteBatch batch) {
        rollEffect.draw(batch);

        TextureRegion frame = animationManager.getDiceFrame(isRolling, currentFaceValue, diceRollingTime);

        batch.draw(
                frame,
                diceX - (DICE_SIZE * scale - DICE_SIZE) / 2,
                diceY - (DICE_SIZE * scale - DICE_SIZE) / 2,
                DICE_SIZE / 2f,
                DICE_SIZE / 2f,
                DICE_SIZE,
                DICE_SIZE,
                scale,
                scale,
                rotation
        );

        font.draw(batch, "Bonus Roll: " + bonusCount, diceX, diceY - 40);
        font.draw(batch, "Bonus Roll Active: " + isBonusRoll, diceX, diceY - 60);
    }


    public int getBonusCount() {
        return bonusCount;
    }

    public void setBonusCount(int bonusCount) {
        this.bonusCount = bonusCount;
    }

    public boolean handleClick(float screenX, float screenY) {
        return !isAnimating() &&
                screenX >= diceX && screenX <= diceX + DICE_SIZE &&
                screenY >= diceY && screenY <= diceY + DICE_SIZE;
    }

    public boolean isAnimating() {
        return isRolling || bounceTime < BOUNCE_DURATION;
    }

    public boolean isBonusRoll() {
        return isBonusRoll;
    }

    public void setBonusRoll(boolean bonusRoll) {
        bonusCount++;
    }

    public void activeBonusRoll() {
        isBonusRoll = true;
    }

    public void updateBonusRoll() {
        if (isBonusRoll) {
            bonusCount = Math.max(0, bonusCount - 1);
            isBonusRoll = false;
        }
    }

    public int getCurrentFaceValue() {
        return currentFaceValue;
    }


    public void dispose() {
        if (rollEffect != null) {
            rollEffect.dispose();
        }
    }
}
