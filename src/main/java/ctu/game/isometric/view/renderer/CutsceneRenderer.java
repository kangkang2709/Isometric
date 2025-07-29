package ctu.game.isometric.view.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
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
        MULTIPLE_BACKGROUNDS_WITH_SUBTITLES,
        OCTOPATH_STYLE // New Octopath Traveler style
    }

    // Octopath style variables
    private Texture overlayTexture;
    private Texture frameTexture;
    private String currentDisplayedText;
    private float typewriterSpeed;
    private float typewriterTimer;
    private int currentCharIndex;
    private boolean isTyping;
    private float centerX;
    private float centerY;


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

        // Initialize Octopath style properties
        this.currentDisplayedText = "";
        this.typewriterSpeed = 25f; // Characters per second
        this.typewriterTimer = 0f;
        this.currentCharIndex = 0;
        this.isTyping = false;
        this.centerX = 640f; // Center of 1280 width
        this.centerY = 360f; // Center of 720 height
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

    String cutSceneName;

    public void loadOctopathStyleCutscene(String cutsceneName, Array<String> subtitleTexts) {
        cutsceneEnded = false;
        this.cutSceneName = cutsceneName;
        disposePages();
        disposeBackgrounds();

        String basePath = "cutscenes/" + cutsceneName + "/";

        // Load overlay texture
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 0.5f); // Màu đen với alpha 50%, đổi thành 0f nếu muốn trong suốt hoàn toàn
        pixmap.fill();
        overlayTexture = new Texture(pixmap);
        pixmap.dispose(); // Giải phóng Pixmap sau khi tạo Texture


        // Load frame texture
        frameTexture = new Texture(Gdx.files.internal("ui/bg_frame.png"));

        // Load background textures for each subtitle
        backgroundTextures.clear();
        for (int i = 0; i < subtitleTexts.size; i++) {
            String backgroundPath = basePath + "background" + (i + 1) + ".png";
            if (Gdx.files.internal(backgroundPath).exists()) {
                backgroundTextures.add(new Texture(Gdx.files.internal(backgroundPath)));
            } else {
                if (Gdx.files.internal(basePath + "background.png").exists()) {
                    backgroundTextures.add(new Texture(Gdx.files.internal(basePath + "background.png")));
                } else {
                    backgroundTextures.add(null);
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
        cutsceneType = CutsceneType.OCTOPATH_STYLE;

        // Initialize typewriter effect
        startTypewriterEffect();
    }

    private void startTypewriterEffect() {
        if (currentPage < subtitles.size) {
            currentDisplayedText = "";
            currentCharIndex = 0;
            typewriterTimer = 0f;
            isTyping = true;
        }
    }

    private void updateTypewriterEffect(float delta) {
        if (!isTyping || currentPage >= subtitles.size) {
            return;
        }

        String fullText = subtitles.get(currentPage);
        if (currentCharIndex >= fullText.length()) {
            isTyping = false;
            return;
        }

        typewriterTimer += delta;
        float timePerChar = 1f / typewriterSpeed;

        if (typewriterTimer >= timePerChar) {
            if (currentCharIndex < fullText.length()) {
                currentDisplayedText = fullText.substring(0, currentCharIndex + 1);
                currentCharIndex++;
            }
            typewriterTimer = 0f;

            if (currentCharIndex >= fullText.length()) {
                isTyping = false;
            }
        }
    }

    private boolean cutsceneEnded = false;

    public void update(float delta) {
        if (cutsceneEnded) {
            return;
        }

        if (cutsceneType == CutsceneType.OCTOPATH_STYLE) {
            updateTypewriterEffect(delta);

            // Auto progress after typewriter finishes
            if (!isTyping && autoProgressEnabled) {
                pageDisplayTimer += delta;
                if (pageDisplayTimer >= pageDisplayDuration) {
                    if (currentPage >= subtitles.size - 1) {
                        endCutscene();
                        return;
                    }

                    currentPage++;
                    pageDisplayTimer = 0f;
                    startTypewriterEffect();
                }
            }
            return;
        }

        // Rest of the existing update logic...
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
        float screenWidth = 1280;
        float screenHeight = 720;

        Matrix4 originalMatrix = batch.getProjectionMatrix().cpy();
        batch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, screenWidth, screenHeight));

        if (cutsceneType == CutsceneType.PAGES) {
            renderPagesCutscene(batch, screenWidth, screenHeight);
        } else if (cutsceneType == CutsceneType.BACKGROUND_WITH_SUBTITLES || cutsceneType == CutsceneType.MULTIPLE_BACKGROUNDS_WITH_SUBTITLES) {
            renderBackgroundCutscene(batch, screenWidth, screenHeight);
        } else if (cutsceneType == CutsceneType.OCTOPATH_STYLE) {
            renderOctopathStyleCutscene(batch, screenWidth, screenHeight);
        }

        batch.setProjectionMatrix(originalMatrix);
    }

    private void renderOctopathStyleCutscene(SpriteBatch batch, float screenWidth, float screenHeight) {
        // Draw background
        if (currentPage < backgroundTextures.size && backgroundTextures.get(currentPage) != null) {
            batch.setColor(1, 1, 1, 1);
            batch.draw(backgroundTextures.get(currentPage), 40, 0, 1200, screenHeight);
        }

        // Draw overlay
        if (overlayTexture != null) {
            batch.setColor(1, 1, 1, 0.7f);
            batch.draw(overlayTexture, 0, 0, screenWidth, screenHeight);
        }

        // Draw frame
        if (frameTexture != null) {
            batch.setColor(1, 1, 1, 1);
            batch.draw(frameTexture, 0, 0, screenWidth, screenHeight);
        }

        // Draw wrapped typewriter text
        if (!currentDisplayedText.isEmpty()) {
            subtitleFont.setColor(1, 1, 1, 1);

            float maxTextWidth = screenWidth - 200; // Padding for frame
            Array<String> wrappedLines = wrapText(currentDisplayedText, maxTextWidth);

            float lineHeight = subtitleFont.getLineHeight() * lineSpacing;
            float totalHeight = wrappedLines.size * lineHeight;
            float startY = (screenHeight + totalHeight) / 2;

            for (int i = 0; i < wrappedLines.size; i++) {
                String line = wrappedLines.get(i);

                if ("##PARAGRAPH_BREAK##".equals(line)) {
                    // This is an explicit paragraph break from \n
                    startY -= (lineHeight * paragraphSpacing);
                    continue;
                } else if (line.isEmpty()) {
                    // This is a regular line break
                    startY -= lineHeight;
                    continue;
                }

                glyphLayout.setText(subtitleFont, line);
                float x = (screenWidth - glyphLayout.width) / 2;
                // (additional x adjustments as needed)

                subtitleFont.draw(batch, line, x, startY - (i * lineHeight));
            }
        }

        batch.setColor(1, 1, 1, 1);
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
        screenWidth = 1024;

        if (cutsceneType == CutsceneType.BACKGROUND_WITH_SUBTITLES) {
            if (backgroundTexture != null) {
                batch.setColor(1, 1, 1, 1);
                batch.draw(backgroundTexture, 0, 0, screenWidth, screenHeight);
            }
        } else if (cutsceneType == CutsceneType.MULTIPLE_BACKGROUNDS_WITH_SUBTITLES) {
            if (currentPage < backgroundTextures.size && backgroundTextures.get(currentPage) != null) {
                batch.setColor(1, 1, 1, 1);
                batch.draw(backgroundTextures.get(currentPage), 128, 0, screenWidth, screenHeight);
            }
        }

        // Draw wrapped subtitles
        if (currentPage < subtitles.size) {
            String currentSubtitle = subtitles.get(currentPage);
            if (currentSubtitle != null && !currentSubtitle.isEmpty()) {
                subtitleFont.setColor(1, 1, 1, subtitleAlpha);

                float maxTextWidth = screenWidth - 100; // Padding from edges
                Array<String> wrappedLines = wrapText(currentSubtitle, maxTextWidth);

                float lineHeight = subtitleFont.getLineHeight() * lineSpacing;
                float totalHeight = wrappedLines.size * lineHeight;
                float startY = subtitleY + totalHeight;

                for (int i = 0; i < wrappedLines.size; i++) {
                    String line = wrappedLines.get(i);

                    if ("##PARAGRAPH_BREAK##".equals(line)) {
                        // This is an explicit paragraph break from \n
                        startY -= (lineHeight * paragraphSpacing);
                        continue;
                    } else if (line.isEmpty()) {
                        // This is a regular line break
                        startY -= lineHeight;
                        continue;
                    }

                    glyphLayout.setText(subtitleFont, line);
                    float x = (screenWidth - glyphLayout.width) / 2;
                    // (additional x adjustments as needed)

                    subtitleFont.draw(batch, line, x, startY - (i * lineHeight));
                }
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

    // Add these variables to the class
    private float lineSpacing = 1.5f; // Multiplier for line height
    private float paragraphSpacing = 1f; // Multiplier for \n breaks

    private Array<String> wrapText(String text, float maxWidth) {
        Array<String> lines = new Array<>();

        // Split by explicit line breaks first
        String[] paragraphs = text.split("\\n");

        for (int i = 0; i < paragraphs.length; i++) {
            String paragraph = paragraphs[i];

            // Add paragraph breaks after the first paragraph
            if (i > 0) {
                lines.add("##PARAGRAPH_BREAK##"); // Special marker for paragraph breaks
            }

            if (paragraph.trim().isEmpty()) {
                continue;
            }

            // Word wrapping for each paragraph
            String[] words = paragraph.split(" ");
            StringBuilder currentLine = new StringBuilder();

            for (String word : words) {
                String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
                glyphLayout.setText(subtitleFont, testLine);

                if (glyphLayout.width <= maxWidth) {
                    currentLine = new StringBuilder(testLine);
                } else {
                    if (currentLine.length() > 0) {
                        lines.add(currentLine.toString());
                        currentLine = new StringBuilder(word);
                    } else {
                        // Single word is too long, add it anyway
                        lines.add(word);
                    }
                }
            }

            if (currentLine.length() > 0) {
                lines.add(currentLine.toString());
            }
        }

        return lines;
    }

    public void nextPage() {
        if (cutsceneType == CutsceneType.OCTOPATH_STYLE) {
            if (isTyping) {
                // Skip to end of current text
                if (currentPage < subtitles.size) {
                    currentDisplayedText = subtitles.get(currentPage);
                    currentCharIndex = currentDisplayedText.length();
                    isTyping = false;
                }
            } else {
                // Move to next page
                if (currentPage < subtitles.size - 1) {
                    currentPage++;
                    pageDisplayTimer = 0f;
                    startTypewriterEffect();
                } else {
                    endCutscene();
                }
            }
            return;
        }

        // Rest of existing nextPage logic...
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
                if (cutSceneName.equals("true_ending"))
                    gameController.changeCreditScreen();
                else if (gameController != null) {
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
        if (overlayTexture != null) {
            overlayTexture.dispose();
            overlayTexture = null;
        }
        if (frameTexture != null) {
            frameTexture.dispose();
            frameTexture = null;
        }
    }

    public void setTypewriterSpeed(float charactersPerSecond) {
        this.typewriterSpeed = charactersPerSecond;
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

    public void setLineSpacing(float spacing) {
        this.lineSpacing = spacing;
    }

    public void setParagraphSpacing(float spacing) {
        this.paragraphSpacing = spacing;
    }
}