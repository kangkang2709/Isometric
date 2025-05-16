package ctu.game.isometric.view.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import ctu.game.isometric.controller.GameController;
import ctu.game.isometric.model.game.GameState;

public class CutsceneRenderer {
    private Array<Texture> pages;
    private int currentPage;
    private float transitionTimer;
    private float transitionDuration;
    private boolean isTransitioning;
    private GameController gameController;

    // New variables for auto progression
    private boolean autoProgressEnabled;
    private float pageDisplayDuration;
    private float pageDisplayTimer;
    private float fadeAlpha;
    private TransitionType transitionType;

    public enum TransitionType {
        SLIDE_HORIZONTAL,
        FADE,
        NONE
    }

    public CutsceneRenderer(GameController gameController) {
        this.gameController = gameController;
        this.pages = new Array<>();
        this.currentPage = 0;
        this.transitionDuration = 1.2f;
        this.isTransitioning = false;

        // Initialize new auto progression properties
        this.autoProgressEnabled = true;
        this.pageDisplayDuration = 1.0f; // Show each page for 5 seconds by default
        this.pageDisplayTimer = 0f;
        this.fadeAlpha = 1.0f;
        this.transitionType = TransitionType.FADE;
    }

    public void loadCutscene(String cutsceneName) {
        disposePages();
        int pageIndex = 1;
        String basePath = "cutscenes/" + cutsceneName + "/";

        while (Gdx.files.internal(basePath + "page" + pageIndex + ".png").exists()) {
            pages.add(new Texture(Gdx.files.internal(basePath + "page" + pageIndex + ".png")));
            pageIndex++;
        }

        currentPage = 0;
        pageDisplayTimer = 0f;
        isTransitioning = false;
    }

    public void update(float delta) {
        if (isTransitioning) {
            // Handle transition animation
            transitionTimer += delta;
            if (transitionType == TransitionType.FADE) {
                fadeAlpha = Math.max(0, 1 - (transitionTimer / transitionDuration));
            }

            if (transitionTimer >= transitionDuration) {
                transitionTimer = 0f;
                isTransitioning = false;
                currentPage++;
                fadeAlpha = 1.0f;

                if (currentPage >= pages.size) {
                    endCutscene();
                }
            }
        } else if (autoProgressEnabled && currentPage < pages.size) {
            // Auto-progress timer
            pageDisplayTimer += delta;
            if (pageDisplayTimer >= pageDisplayDuration) {
                nextPage();
                pageDisplayTimer = 0f;
            }
        }
    }

    public void render(SpriteBatch batch) {
        float screenWidth = 1280;
        float screenHeight = 720;

        Matrix4 originalMatrix = batch.getProjectionMatrix().cpy();
        batch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, screenWidth, screenHeight));

        if (!isTransitioning) {
            if (currentPage < pages.size) {
                Texture currentTexture = pages.get(currentPage);
                batch.setColor(1, 1, 1, 1);
                batch.draw(currentTexture, 0, 0, screenWidth, screenHeight);
            }
        } else {
            // Handle different transition types
            float progress = transitionTimer / transitionDuration;

            switch (transitionType) {
                case SLIDE_HORIZONTAL:
                    renderSlideTransition(batch, progress, screenWidth, screenHeight);
                    break;

                case FADE:
                    renderFadeTransition(batch, screenWidth, screenHeight);
                    break;

                case NONE:
                    // Immediate transition, should not actually render in this state
                    if (currentPage < pages.size) {
                        Texture currentTexture = pages.get(currentPage);
                        batch.draw(currentTexture, 0, 0, screenWidth, screenHeight);
                    }
                    break;
            }
        }

        batch.setProjectionMatrix(originalMatrix);
    }

    private void renderSlideTransition(SpriteBatch batch, float progress, float screenWidth, float screenHeight) {
        float offsetX = progress * screenWidth;

        Texture currentTexture = pages.get(currentPage);
        Texture nextTexture = currentPage + 1 < pages.size ? pages.get(currentPage + 1) : null;

        batch.setColor(1, 1, 1, 1);
        if (currentTexture != null) {
            batch.draw(currentTexture, -offsetX, 0, screenWidth, screenHeight);
        }

        if (nextTexture != null) {
            batch.draw(nextTexture, screenWidth - offsetX, 0, screenWidth, screenHeight);
        }
    }

    private void renderFadeTransition(SpriteBatch batch, float screenWidth, float screenHeight) {
        Texture currentTexture = pages.get(currentPage);
        Texture nextTexture = currentPage + 1 < pages.size ? pages.get(currentPage + 1) : null;

        if (nextTexture != null) {
            batch.setColor(1, 1, 1, 1);
            batch.draw(nextTexture, 0, 0, screenWidth, screenHeight);
        }

        if (currentTexture != null) {
            batch.setColor(1, 1, 1, fadeAlpha);
            batch.draw(currentTexture, 0, 0, screenWidth, screenHeight);
        }

        batch.setColor(1, 1, 1, 1);
    }

    public void nextPage() {
        if (!isTransitioning && currentPage < pages.size - 1) {
            isTransitioning = true;
            transitionTimer = 0f;
            pageDisplayTimer = 0f;
        } else if (!isTransitioning && currentPage == pages.size - 1) {
            endCutscene();
        }
    }

    public void skipCutscene() {
        endCutscene();
    }

    public void endCutscene() {
        if (gameController != null) {
            gameController.setState(GameState.EXPLORING);
            gameController.setPreviousState(GameState.EXPLORING);
        }
        dispose();
    }

    // Configuration methods
    public void setAutoProgress(boolean enabled) {
        this.autoProgressEnabled = enabled;
    }

    public void setPageDisplayDuration(float seconds) {
        this.pageDisplayDuration = seconds;
    }

    public void setTransitionDuration(float seconds) {
        this.transitionDuration = seconds;
    }

    public void setTransitionType(TransitionType type) {
        this.transitionType = type;
    }

    public void dispose() {
        disposePages();
    }

    private void disposePages() {
        for (Texture page : pages) {
            try {
                page.dispose();
            } catch (Exception e) {
                Gdx.app.error("CutsceneController", "Failed to dispose texture: " + page, e);
            }
        }
        pages.clear();
    }
}