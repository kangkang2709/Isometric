package ctu.game.isometric.view.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;

public class TransitionRenderer {
    private float alpha = 0f;
    private float duration = 0.2f; // transition duration in seconds
    private boolean isTransitioning = false;
    private boolean isFadingIn = false;
    private Runnable onCompleteAction;
    private ShapeRenderer shapeRenderer;
    private boolean showLoadingScreen = false;

    // Loading screen properties
    private float loadingProgress = 0f;
    private BitmapFont loadingFont;
    private String loadingText = "Loading....";
    private GlyphLayout glyphLayout = new GlyphLayout();

    // Transition type enum
    public enum TransitionType {
        FADE,
        RADIAL_WIPE
    }

    private TransitionType currentType = TransitionType.FADE;

    public TransitionRenderer() {
        shapeRenderer = new ShapeRenderer();
        loadingFont = new BitmapFont();
        loadingFont.setColor(Color.WHITE);
        loadingFont.getData().setScale(2.0f);
    }

    // Start a fade out transition
    public void startFadeOut(Runnable onComplete) {
        alpha = 0f;
        isFadingIn = false;
        isTransitioning = true;
        this.onCompleteAction = onComplete;
    }

    // Start a fade in transition
    public void startFadeIn() {
        alpha = 1f;
        isFadingIn = true;
        isTransitioning = true;
        this.onCompleteAction = null;
    }

    public void startLoadingScreen(Runnable onComplete) {
        showLoadingScreen = true;
        loadingProgress = 0f;
        startFadeOut(() -> {
            if (onComplete != null) {
                onComplete.run();
            }
            showLoadingScreen = false;
            startFadeIn();
        });
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
        if (alpha <= 0 && !showLoadingScreen) return;

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

        // Draw loading screen elements if needed
        if (showLoadingScreen) {
            renderLoadingScreen(batch);
        }

        Gdx.gl.glDisable(GL20.GL_BLEND);

        if (wasBatchDrawing) batch.begin();
    }

    private void renderLoadingScreen(SpriteBatch batch) {
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        // Start batch for text rendering
        batch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, screenWidth, screenHeight));
        batch.begin();

        // Draw loading text
        glyphLayout.setText(loadingFont, loadingText);
        loadingFont.draw(batch, loadingText,
                50,
                80);
        // Draw loading text
//        glyphLayout.setText(loadingFont, loadingText);
//        loadingFont.draw(batch, loadingText,
//                (screenWidth - glyphLayout.width) / 2,
//                (screenHeight + glyphLayout.height) / 2);
        batch.end();
    }

    public void setLoadingProgress(float progress) {
        this.loadingProgress = MathUtils.clamp(progress, 0f, 1f);
    }

    public void setLoadingText(String text) {
        this.loadingText = text;
    }

    private void renderFadeTransition() {
        shapeRenderer.setColor(0, 0, 0, alpha);
        shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private void renderRadialWipeTransition() {
        int segments = 36;
        float centerX = Gdx.graphics.getWidth() / 2f;
        float centerY = Gdx.graphics.getHeight() / 2f;
        float maxRadius = (float) Math.sqrt(centerX * centerX + centerY * centerY);
        float currentRadius = isFadingIn ?
                maxRadius * (1 - alpha) :
                maxRadius * alpha;

        shapeRenderer.setColor(0, 0, 0, 1);

        if (isFadingIn) {
            for (int i = 0; i < segments; i++) {
                float angle1 = MathUtils.PI2 * i / segments;
                float angle2 = MathUtils.PI2 * (i + 1) / segments;

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
            for (int i = 0; i < segments; i++) {
                float angle1 = MathUtils.PI2 * i / segments;
                float angle2 = MathUtils.PI2 * (i + 1) / segments;

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

    public boolean isShowingLoadingScreen() {
        return showLoadingScreen;
    }

    // Set transition duration in seconds
    public void setTransitionDuration(float seconds) {
        this.duration = Math.max(0.1f, seconds);
    }

    public void dispose() {
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
        if (loadingFont != null) {
            loadingFont.dispose();
        }
    }

    public TransitionType getCurrentType() {
        return currentType;
    }

    public void setTransitionType(TransitionType type) {
        this.currentType = type;
    }
}