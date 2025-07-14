package ctu.game.isometric.view.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Timer;
import ctu.game.isometric.controller.GameController;
import ctu.game.isometric.model.game.GameState;

import static ctu.game.isometric.util.FontGenerator.generateVietNameseFont;

public class CutsceneRenderer {
    private Array<Texture> pages;
    private Array<String> subtitles;
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
    private CutsceneType cutsceneType;

    // Subtitle variables
    private BitmapFont subtitleFont;
    private Texture backgroundTexture;
    private float subtitleY;
    private float subtitleAlpha;
    private GlyphLayout glyphLayout;

    public enum TransitionType {
        SLIDE_HORIZONTAL,
        SLIDE_VERTICAL, // New
        FADE,
        ZOOM, // New
        NONE
    }

    public enum CutsceneType {
        PAGES,
        BACKGROUND_WITH_SUBTITLES,
        MULTIPLE_BACKGROUNDS_WITH_SUBTITLES // New type
    }

    private Array<Texture> backgroundTextures;

    public CutsceneRenderer(GameController gameController) {
        this.gameController = gameController;
        this.pages = new Array<>();
        this.subtitles = new Array<>();
        this.currentPage = 0;
        this.transitionDuration = 1.2f;
        this.isTransitioning = false;

        // Initialize new auto progression properties
        this.autoProgressEnabled = true;
        this.pageDisplayDuration = 3.0f;
        this.pageDisplayTimer = 0f;
        this.fadeAlpha = 1.0f;
        this.transitionType = TransitionType.FADE;
        this.cutsceneType = CutsceneType.PAGES;

        // Initialize subtitle properties
        this.subtitleFont = generateVietNameseFont("Roboto-Italic.ttf", 24);
        this.subtitleY = 100f;
        this.subtitleAlpha = 1.0f;
        this.glyphLayout = new GlyphLayout();
        this.backgroundTextures = new Array<>();
    }

    public void loadCutscene(String cutsceneName) {
        cutsceneEnded = false;
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
        cutsceneType = CutsceneType.PAGES;
    }

    public void loadBackgroundCutscene(String cutsceneName, Array<String> subtitleTexts) {
        cutsceneEnded = false;
        disposePages();
        String basePath = "cutscenes/" + cutsceneName + "/";

        // Load background texture
        if (Gdx.files.internal(basePath + "background.png").exists()) {
            backgroundTexture = new Texture(Gdx.files.internal(basePath + "background.png"));
        }

        // Load subtitles
        subtitles.clear();
        for (String subtitle : subtitleTexts) {
            subtitles.add(subtitle);
        }

        currentPage = 0;
        pageDisplayTimer = 0f;
        isTransitioning = false;
        cutsceneType = CutsceneType.BACKGROUND_WITH_SUBTITLES;
    }

    public void loadMultipleBackgroundsCutscene(String cutsceneName, Array<String> subtitleTexts) {
        cutsceneEnded = false;
        disposePages();
        disposeBackgrounds();

        String basePath = "cutscenes/" + cutsceneName + "/";

        // Load background textures for each subtitle
        backgroundTextures.clear();
        for (int i = 0; i < subtitleTexts.size; i++) {
            String backgroundPath = basePath + "background" + (i + 1) + ".png";
            if (Gdx.files.internal(backgroundPath).exists()) {
                backgroundTextures.add(new Texture(Gdx.files.internal(backgroundPath)));
            } else {
                // If specific background doesn't exist, try default background
                if (Gdx.files.internal(basePath + "background.png").exists()) {
                    backgroundTextures.add(new Texture(Gdx.files.internal(basePath + "background.png")));
                } else {
                    backgroundTextures.add(null); // No background for this subtitle
                }
            }
        }

        // Load subtitles
        subtitles.clear();
        for (String subtitle : subtitleTexts) {
            subtitles.add(subtitle);
        }

        currentPage = 0;
        pageDisplayTimer = 0f;
        isTransitioning = false;
        cutsceneType = CutsceneType.MULTIPLE_BACKGROUNDS_WITH_SUBTITLES;
    }


    private boolean cutsceneEnded = false;

    public void update(float delta) {
        // Add this check at the beginning
        if (cutsceneEnded) {
            return;
        }

        if (isTransitioning) {
            transitionTimer += delta;
            if (transitionType == TransitionType.FADE) {
                fadeAlpha = Math.max(0, 1 - (transitionTimer / transitionDuration));
                subtitleAlpha = fadeAlpha;
            }

            if (transitionTimer >= transitionDuration) {
                transitionTimer = 0f;
                isTransitioning = false;
                currentPage++;
                fadeAlpha = 1.0f;
                subtitleAlpha = 0.0f;

                // Check if we've reached the end
                int maxPages = cutsceneType == CutsceneType.PAGES ? pages.size : subtitles.size;
                if (currentPage >= maxPages) {
                    endCutscene();
                    return;
                }
            }
        } else if (autoProgressEnabled) {
            pageDisplayTimer += delta;
            if (pageDisplayTimer >= pageDisplayDuration) {
                int maxPages = cutsceneType == CutsceneType.PAGES ? pages.size : subtitles.size;

                // Check if this is the last page
                if (currentPage >= maxPages - 1) {
                    endCutscene();
                    return;
                }

                isTransitioning = true;
                pageDisplayTimer = 0f;
            } else {
                subtitleAlpha = Math.min(1.0f, pageDisplayTimer / (pageDisplayDuration / 2));
            }
        }
    }

    public void render(SpriteBatch batch) {
        float screenWidth = 1280; // Assuming a fixed screen width for simplicity
        float screenHeight = 720;

        Matrix4 originalMatrix = batch.getProjectionMatrix().cpy();
        batch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, screenWidth, screenHeight));

        if (cutsceneType == CutsceneType.PAGES) {
            renderPagesCutscene(batch, screenWidth, screenHeight);
        } else if (cutsceneType == CutsceneType.BACKGROUND_WITH_SUBTITLES || cutsceneType == CutsceneType.MULTIPLE_BACKGROUNDS_WITH_SUBTITLES) {
            renderBackgroundCutscene(batch, screenWidth, screenHeight);
        }

        batch.setProjectionMatrix(originalMatrix);
    }

    private void renderPagesCutscene(SpriteBatch batch, float screenWidth, float screenHeight) {
        if (!isTransitioning) {
            if (currentPage < pages.size) {
                Texture currentTexture = pages.get(currentPage);
                batch.setColor(1, 1, 1, 1);
                batch.draw(currentTexture, 0, 0, screenWidth, screenHeight);
            }
        } else {
            float progress = transitionTimer / transitionDuration;

            switch (transitionType) {
                case SLIDE_HORIZONTAL:
                    renderSlideTransition(batch, progress, screenWidth, screenHeight);
                    break;

                case SLIDE_VERTICAL: // New transition
                    renderSlideVerticalTransition(batch, progress, screenWidth, screenHeight);
                    break;

                case ZOOM: // New transition
                    renderZoomTransition(batch, progress, screenWidth, screenHeight);
                    break;

                case FADE:
                    renderFadeTransition(batch, screenWidth, screenHeight);
                    break;

                case NONE:
                    if (currentPage < pages.size) {
                        Texture currentTexture = pages.get(currentPage);
                        batch.draw(currentTexture, 0, 0, 1280, screenHeight);
                    }
                    break;
            }
        }
    }

    private void renderSlideVerticalTransition(SpriteBatch batch, float progress, float screenWidth, float screenHeight) {
        float offsetY = progress * screenHeight;

        Texture currentTexture = pages.get(currentPage);
        Texture nextTexture = currentPage + 1 < pages.size ? pages.get(currentPage + 1) : null;

        batch.setColor(1, 1, 1, 1);
        if (currentTexture != null) {
            batch.draw(currentTexture, 0, -offsetY, screenWidth, screenHeight);
        }

        if (nextTexture != null) {
            batch.draw(nextTexture, 0, screenHeight - offsetY, screenWidth, screenHeight);
        }
    }

    // New method for zoom transition
    private void renderZoomTransition(SpriteBatch batch, float progress, float screenWidth, float screenHeight) {
        float scale = 1 + progress; // Zoom out effect

        Texture currentTexture = pages.get(currentPage);
        Texture nextTexture = currentPage + 1 < pages.size ? pages.get(currentPage + 1) : null;

        batch.setColor(1, 1, 1, 1);
        if (nextTexture != null) {
            batch.draw(nextTexture, 0, 0, screenWidth, screenHeight);
        }

        if (currentTexture != null) {
            float scaledWidth = screenWidth * scale;
            float scaledHeight = screenHeight * scale;
            float x = (screenWidth - scaledWidth) / 2;
            float y = (screenHeight - scaledHeight) / 2;

            batch.draw(currentTexture, x, y, scaledWidth, scaledHeight);
        }
    }

    private void renderBackgroundCutscene(SpriteBatch batch, float screenWidth, float screenHeight) {
        // Draw background based on cutscene type
        screenWidth = 1024;

        if (cutsceneType == CutsceneType.BACKGROUND_WITH_SUBTITLES) {
            // Single background for all subtitles
            if (backgroundTexture != null) {
                batch.setColor(1, 1, 1, 1);
                batch.draw(backgroundTexture, 0, 0, screenWidth, screenHeight);
            }
        } else if (cutsceneType == CutsceneType.MULTIPLE_BACKGROUNDS_WITH_SUBTITLES) {
            // Different background for each subtitle
            if (currentPage < backgroundTextures.size && backgroundTextures.get(currentPage) != null) {
                batch.setColor(1, 1, 1, 1);
                batch.draw(backgroundTextures.get(currentPage), 128, 0, screenWidth, screenHeight);
            }
        }

        // Draw subtitle (same for both types)
        if (currentPage < subtitles.size) {
            String currentSubtitle = subtitles.get(currentPage);
            if (currentSubtitle != null && !currentSubtitle.isEmpty()) {
                subtitleFont.setColor(1, 1, 1, subtitleAlpha);
                glyphLayout.setText(subtitleFont, currentSubtitle);
                float x = (screenWidth - glyphLayout.width) / 2;
                subtitleFont.draw(batch, currentSubtitle, x+128, subtitleY);
            }
        }
    }

    private void disposeBackgrounds() {
        for (Texture bg : backgroundTextures) {
            if (bg != null) {
                try {
                    bg.dispose();
                } catch (Exception e) {
                    Gdx.app.error("CutsceneRenderer", "Failed to dispose background texture", e);
                }
            }
        }
        backgroundTextures.clear();
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
        if (cutsceneType == CutsceneType.PAGES) {
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
        } else if (cutsceneType == CutsceneType.BACKGROUND_WITH_SUBTITLES) {
            // Background stays the same, only subtitle fades
            if (backgroundTexture != null) {
                batch.setColor(1, 1, 1, 1);
                batch.draw(backgroundTexture, 0, 0, screenWidth, screenHeight);
            }

            // Draw current subtitle with fade effect
            if (currentPage < subtitles.size) {
                String currentSubtitle = subtitles.get(currentPage);
                if (currentSubtitle != null && !currentSubtitle.isEmpty()) {
                    subtitleFont.setColor(1, 1, 1, subtitleAlpha);
                    glyphLayout.setText(subtitleFont, currentSubtitle);
                    float x = (screenWidth - glyphLayout.width) / 2;
                    subtitleFont.draw(batch, currentSubtitle, x, subtitleY);
                }
            }
        }

        batch.setColor(1, 1, 1, 1);
    }

    public void nextPage() {
        int maxPages = cutsceneType == CutsceneType.PAGES ? pages.size : subtitles.size;

        if (!isTransitioning && currentPage < maxPages - 1) {
            isTransitioning = true;
            transitionTimer = 0f;
            pageDisplayTimer = 0f;
        } else if (!isTransitioning && currentPage == maxPages - 1) {
            endCutscene();
        }
    }

    public void skipCutscene() {
        endCutscene();
    }

    public void endCutscene() {
        if (cutsceneEnded) {
            return; // Prevent multiple calls
        }

        cutsceneEnded = true;

        Timer.schedule(new Timer.Task() {

            @Override
            public void run() {
                if (gameController != null) {
                    System.out.println("Cutscene ended, returning to game state.");
                    gameController.setState(GameState.EXPLORING);
                    gameController.setPreviousState(GameState.EXPLORING);
                }
                dispose();
            }
        }, 2f); // Delay to allow any final transitions to complete

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

    public void setSubtitleFont(BitmapFont font) {
        this.subtitleFont = font;
    }

    public void setSubtitleY(float y) {
        this.subtitleY = y;
    }

    public void dispose() {
        disposePages();
        disposeBackgrounds();
        if (backgroundTexture != null) {
            backgroundTexture.dispose();
            backgroundTexture = null;
        }

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
        subtitles.clear();
    }
}