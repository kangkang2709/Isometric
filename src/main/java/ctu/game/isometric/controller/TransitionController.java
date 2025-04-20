package ctu.game.isometric.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

public class TransitionController {
    private float alpha = 0f;
    private float duration = 0.3f; // transition duration in seconds
    private boolean isTransitioning = false;
    private boolean isFadingIn = false;
    private Runnable onCompleteAction;
    private ShapeRenderer shapeRenderer;

    // Transition type enum
    public enum TransitionType {
        FADE,
        RADIAL_WIPE
    }

    private TransitionType currentType = TransitionType.RADIAL_WIPE;

    public TransitionController() {
        shapeRenderer = new ShapeRenderer();
    }

    public void setTransitionType(TransitionType type) {
        this.currentType = type;
    }

    public void startFadeOut(Runnable onComplete) {
        alpha = 0f;
        isTransitioning = true;
        isFadingIn = false;
        onCompleteAction = onComplete;
    }

    public void startFadeIn() {
        alpha = 1f;
        isTransitioning = true;
        isFadingIn = true;
        onCompleteAction = null;
    }

    public void update(float delta) {
        if (!isTransitioning) return;

        if (isFadingIn) {
            alpha -= (delta / duration);
            if (alpha <= 0f) {
                alpha = 0f;
                isTransitioning = false;
            }
        } else {
            alpha += (delta / duration);
            if (alpha >= 1f) {
                alpha = 1f;
                isTransitioning = false;
                if (onCompleteAction != null) {
                    onCompleteAction.run();
                    startFadeIn();
                }
            }
        }
    }

    public void render(SpriteBatch batch) {
        if (alpha <= 0) return;

        boolean wasBatchDrawing = batch.isDrawing();
        if (wasBatchDrawing) batch.end();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        switch (currentType) {
            case FADE:
                renderFadeTransition();
                break;
            case RADIAL_WIPE:
                renderRadialWipeTransition();
                break;
        }

        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        if (wasBatchDrawing) batch.begin();
    }

    private void renderFadeTransition() {
        shapeRenderer.setColor(0, 0, 0, alpha);
        shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private void renderRadialWipeTransition() {
        int segments = 36; // Number of triangles to use for the circle
        float centerX = Gdx.graphics.getWidth() / 2f;
        float centerY = Gdx.graphics.getHeight() / 2f;

        // Calculate the radius based on the screen size
        float maxRadius = (float) Math.sqrt(centerX * centerX + centerY * centerY);
        float currentRadius = isFadingIn ?
                maxRadius * (1 - alpha) :
                maxRadius * alpha;

        shapeRenderer.setColor(0, 0, 0, 1);

        if (isFadingIn) {
            // When fading in, draw the outer area
            for (int i = 0; i < segments; i++) {
                float angle1 = MathUtils.PI2 * i / segments;
                float angle2 = MathUtils.PI2 * (i + 1) / segments;

                // Create a triangle from the center to the edge of the screen
                shapeRenderer.triangle(
                        centerX + currentRadius * MathUtils.cos(angle1),
                        centerY + currentRadius * MathUtils.sin(angle1),
                        centerX + maxRadius * MathUtils.cos(angle1),
                        centerY + maxRadius * MathUtils.sin(angle1),
                        centerX + maxRadius * MathUtils.cos(angle2),
                        centerY + maxRadius * MathUtils.sin(angle2)
                );

                shapeRenderer.triangle(
                        centerX + currentRadius * MathUtils.cos(angle1),
                        centerY + currentRadius * MathUtils.sin(angle1),
                        centerX + maxRadius * MathUtils.cos(angle2),
                        centerY + maxRadius * MathUtils.sin(angle2),
                        centerX + currentRadius * MathUtils.cos(angle2),
                        centerY + currentRadius * MathUtils.sin(angle2)
                );
            }
        } else {
            // When fading out, draw the inner circle
            for (int i = 0; i < segments; i++) {
                float angle1 = MathUtils.PI2 * i / segments;
                float angle2 = MathUtils.PI2 * (i + 1) / segments;

                // Create a triangle from the center to the current radius
                shapeRenderer.triangle(
                        centerX,
                        centerY,
                        centerX + currentRadius * MathUtils.cos(angle1),
                        centerY + currentRadius * MathUtils.sin(angle1),
                        centerX + currentRadius * MathUtils.cos(angle2),
                        centerY + currentRadius * MathUtils.sin(angle2)
                );
            }
        }
    }

    public boolean isTransitioning() {
        return isTransitioning;
    }

    public void dispose() {
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
    }

    public TransitionType getCurrentType() {
        return currentType;
    }

    public void setCurrentType(TransitionType currentType) {
        this.currentType = currentType;
    }
}