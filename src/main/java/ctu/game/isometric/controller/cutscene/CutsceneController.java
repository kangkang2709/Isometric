package ctu.game.isometric.controller.cutscene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import ctu.game.isometric.controller.GameController;

public class CutsceneController {
    private Array<Texture> pages;
    private int currentPage;
    private float transitionTimer;
    private float transitionDuration;
    private boolean isTransitioning;
    private GameController gameController;

    public CutsceneController(GameController gameController) {
        this.gameController = gameController;
        this.pages = new Array<>();
        this.currentPage = 0;
        this.transitionDuration = 1.2f; // Thời gian hiệu ứng trượt
        this.isTransitioning = false;
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
    }

    public void update(float delta) {
        if (isTransitioning) {
            transitionTimer += delta;
            if (transitionTimer >= transitionDuration) {
                transitionTimer = 0f;
                isTransitioning = false;
                currentPage++;
                if (currentPage >= pages.size) {
                    endCutscene();
                }
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
                batch.draw(currentTexture, 0, 0, screenWidth, screenHeight);
            }
        } else {
            // Hiệu ứng slide trái -> phải
            float progress = transitionTimer / transitionDuration;
            float offsetX = progress * screenWidth;

            Texture currentTexture = pages.get(currentPage);
            Texture nextTexture = currentPage + 1 < pages.size ? pages.get(currentPage + 1) : null;

            if (currentTexture != null) {
                batch.draw(currentTexture, -offsetX, 0, screenWidth, screenHeight);
            }

            if (nextTexture != null) {
                batch.draw(nextTexture, screenWidth - offsetX, 0, screenWidth, screenHeight);
            }
        }

        batch.setProjectionMatrix(originalMatrix);
    }

    public void nextPage() {
        if (!isTransitioning && currentPage < pages.size - 1) {
            isTransitioning = true;
            transitionTimer = 0f;
        } else if (!isTransitioning && currentPage == pages.size - 1) {
            endCutscene();
        }
    }

    private void endCutscene() {
        if (gameController != null) {
            gameController.setState(gameController.getPreviousState());
        }
        dispose();
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
