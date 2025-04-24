package ctu.game.isometric.controller.cutscene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import ctu.game.isometric.controller.GameController;

public class CutsceneController {
    private Array<Texture> pages;
    private int currentPage;
    private float pageFlipTimer;
    private float pageFlipDuration;
    private boolean isFlipping;
    private float currentAngle;
    private GameController gameController;

    public CutsceneController(GameController gameController) {
        this.gameController = gameController;
        this.pages = new Array<>();
        this.currentPage = 0;
        this.pageFlipDuration = 0.8f; // seconds for page flip animation
        this.isFlipping = false;
    }

    public void loadCutscene(String cutsceneName) {
        // Clear previous pages
        disposePages();

        // Load the images for this cutscene
        // Example: loading from a directory structure like "cutscenes/[cutsceneName]/page1.png"
        int pageIndex = 1;
        String basePath = "cutscenes/" + cutsceneName + "/";

        while (Gdx.files.internal(basePath + "page" + pageIndex + ".png").exists()) {
            pages.add(new Texture(Gdx.files.internal(basePath + "page" + pageIndex + ".png")));
            pageIndex++;
        }

        currentPage = 0;
    }

    public void update(float delta) {
        if (isFlipping) {
            pageFlipTimer += delta;

            // Update flip animation
            currentAngle = (pageFlipTimer / pageFlipDuration) * 180;

            if (pageFlipTimer >= pageFlipDuration) {
                isFlipping = false;
                pageFlipTimer = 0;

                // Move to next page or end cutscene if at last page
                currentPage++;
                if (currentPage >= pages.size) {
                    endCutscene();
                }
            }
        }
    }

    public void render(SpriteBatch batch) {
        float centerX = Gdx.graphics.getWidth() / 2f;
        float centerY = Gdx.graphics.getHeight() / 2f;

        // Draw current page as a book
        if (!isFlipping && currentPage < pages.size) {
            Texture currentTexture = pages.get(currentPage);
            batch.draw(currentTexture,
                    centerX - currentTexture.getWidth() / 2f,
                    centerY - currentTexture.getHeight() / 2f);
        } else if (isFlipping && currentPage < pages.size - 1) {
            // Draw page flip animation
            Texture currentTexture = pages.get(currentPage);
            Texture nextTexture = pages.get(currentPage + 1);

            // Draw current page
            batch.draw(currentTexture,
                    centerX - currentTexture.getWidth() / 2f,
                    centerY - currentTexture.getHeight() / 2f);

            // Draw page flipping animation (simplified)
            float flipProgress = pageFlipTimer / pageFlipDuration;
            float width = nextTexture.getWidth() * (1 - flipProgress);

            batch.draw(nextTexture,
                    centerX + (currentTexture.getWidth() / 2f) - width,
                    centerY - nextTexture.getHeight() / 2f,
                    width, nextTexture.getHeight());
        }
    }

    public void nextPage() {
        if (!isFlipping && currentPage < pages.size - 1) {
            isFlipping = true;
            pageFlipTimer = 0;
        } else if (!isFlipping && currentPage == pages.size - 1) {
            endCutscene();
        }
    }

    private void endCutscene() {
        // Return to previous game state
        gameController.setState(gameController.getPreviousState());
    }

    public void dispose() {
        disposePages();
    }

    private void disposePages() {
        for (Texture page : pages) {
            page.dispose();
        }
        pages.clear();
    }
}