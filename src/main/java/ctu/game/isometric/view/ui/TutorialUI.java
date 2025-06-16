package ctu.game.isometric.view.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import ctu.game.isometric.controller.TutorialManager;
import ctu.game.isometric.model.game.Tutorial;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TutorialUI implements Disposable {
    // UI theme constants
    private static final Color BACKGROUND_COLOR = new Color(0.1f, 0.1f, 0.15f, 0.95f);
    private static final Color TITLE_COLOR = new Color(1f, 0.85f, 0.4f, 1f);
    private static final Color TEXT_COLOR = new Color(0.9f, 0.9f, 0.95f, 1f);
    private static final Color BUTTON_COLOR = new Color(0.3f, 0.6f, 0.9f, 1f);
    private static final Color BUTTON_HOVER_COLOR = new Color(0.4f, 0.7f, 1f, 1f);

    // Image display constants
    private static final int FIXED_IMAGE_HEIGHT = 250;
    private static final int FIXED_IMAGE_WIDTH = 400;

    private boolean isVisible= false;

    // Tutorial Page inner class
    public static class TutorialPage {
        public String title;
        public String content;
        public Texture image;

        public TutorialPage(String title, String content, Texture image) {
            this.title = title;
            this.content = content;
            this.image = image;
        }
    }

    private List<TutorialPage> pages;
    private int currentPageIndex;
    private boolean visible;
    private String currentTutorialType;

    // Tutorial management
    private TutorialManager tutorialManager;
    private Map<String, Texture> loadedTextures;

    // UI components
    private BitmapFont titleFont;
    private BitmapFont contentFont;
    private BitmapFont buttonFont;
    private TextureRegion background;
    private TextureRegion nextButton;
    private TextureRegion prevButton;
    private Texture whitePixel;  // Reusable white pixel texture

    // UI dimensions and position
    private float x, y;
    private float width, height;
    private float padding = 25f;

    // Hit detection areas
    private Rectangle nextBtnRect;
    private Rectangle prevBtnRect;
    private Rectangle closeBtnRect;
    private Rectangle hoverButton; // Tracks which button is currently hovered

    private Matrix4 uiMatrix;

    private GlyphLayout titleLayout;
    private GlyphLayout contentLayout;

    // Animation properties
    private float alpha = 0f;
    private boolean animatingIn = false;
    private boolean animatingOut = false;

    public TutorialUI() {
        this(250, 200, 800, 400); // Default size and position
    }

    public TutorialUI(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        pages = new ArrayList<>();
        loadedTextures = new HashMap<>();
        currentPageIndex = 0;
        visible = false;

        uiMatrix = new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        initializeFonts();
        this.tutorialManager = new TutorialManager();
        createWhitePixel();

        // Initialize GlyphLayout objects
        titleLayout = new GlyphLayout();
        contentLayout = new GlyphLayout();

        // Set up hit detection areas
        float buttonSize = 60;
        nextBtnRect = new Rectangle(x + width, y + padding, buttonSize, buttonSize);
        prevBtnRect = new Rectangle(x + padding -buttonSize - 25, y + padding, buttonSize, buttonSize);
        closeBtnRect = new Rectangle(x + width - padding - 40, y + height - padding - 40, 40, 40);
    }

    private void createWhitePixel() {
        // Create a 1x1 white pixel for drawing UI elements
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        whitePixel = new Texture(pixmap);
        pixmap.dispose();
    }

    private void initializeFonts() {
        // This assumes you have FreeType extension
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/IMFellEnglishSC-Regular.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter params = new FreeTypeFontGenerator.FreeTypeFontParameter();

        // Title font
        params.size = 24;
        params.color = TITLE_COLOR;
        params.borderWidth = 1;
        params.borderColor = new Color(0, 0, 0, 0.3f);
        titleFont = generator.generateFont(params);

        // Content font
        params.size = 18;
        params.color = TEXT_COLOR;
        params.borderWidth = 0;
        contentFont = generator.generateFont(params);

        // Button font
        params.size = 22;
        params.color = Color.WHITE;
        buttonFont = generator.generateFont(params);

        generator.dispose();
    }

    public void render(SpriteBatch batch) {
        if (!visible && !animatingIn && !animatingOut) return;
        if (pages.isEmpty()) return;

        // Update animation
        if (animatingIn) {
            alpha += Gdx.graphics.getDeltaTime() * 4;
            if (alpha >= 1f) {
                alpha = 1f;
                animatingIn = false;
            }
        } else if (animatingOut) {
            alpha -= Gdx.graphics.getDeltaTime() * 4;
            if (alpha <= 0f) {
                alpha = 0f;
                animatingOut = false;
                visible = false;
                return;
            }
        }

        batch.setProjectionMatrix(uiMatrix);
        batch.begin();
        TutorialPage currentPage = pages.get(currentPageIndex);

        // Draw panel background
        Color oldColor = batch.getColor();
        Color bgColor = new Color(BACKGROUND_COLOR);
        bgColor.a *= alpha;
        batch.setColor(bgColor);

        if (background != null) {
            batch.draw(background, x, y, width, height);
        } else {
            // Draw a rounded rectangle background
            drawRoundedRect(batch, x, y, width, height);
        }

        // Draw decorative header bar
        batch.setColor(TITLE_COLOR.r, TITLE_COLOR.g, TITLE_COLOR.b, alpha * 0.8f);
        batch.draw(whitePixel, x, y + height - padding - 50, width, 4);

        batch.setColor(oldColor.r, oldColor.g, oldColor.b, alpha);

        // Draw title
        titleLayout.setText(titleFont, currentPage.title, TITLE_COLOR, width - padding * 2, Align.center, false);
        titleFont.setColor(TITLE_COLOR.r, TITLE_COLOR.g, TITLE_COLOR.b, alpha);
        titleFont.draw(batch, titleLayout, x + padding, y + height - padding);

        // Draw image with fixed size
        drawImageFixed(batch, currentPage);

        // Draw content text
        contentFont.setColor(TEXT_COLOR.r, TEXT_COLOR.g, TEXT_COLOR.b, alpha);
        contentLayout.setText(contentFont, currentPage.content,
                TEXT_COLOR, width - padding * 2, Align.left, true);
        float textY = y + height - padding * 2 - titleLayout.height - FIXED_IMAGE_HEIGHT - padding;
        contentFont.draw(batch, contentLayout, x + padding, textY);

        // Draw navigation controls
        drawEnhancedNavigationControls(batch);

        batch.end();

        // Update hover state for buttons
        updateButtonHover(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY());
    }

    private void drawRoundedRect(SpriteBatch batch, float x, float y, float width, float height) {
        // For simplicity, use the white pixel texture to draw a rectangle
        batch.draw(whitePixel, x, y, width, height);
    }

    private void drawImageFixed(SpriteBatch batch, TutorialPage currentPage) {
        if (currentPage.image == null) return;

        // Calculate the position to center the image
        float imageX = x + (width - FIXED_IMAGE_WIDTH) / 2;
        float imageY = y + height - padding - titleLayout.height - FIXED_IMAGE_HEIGHT - padding -20;

        // Draw image border
        batch.setColor(TITLE_COLOR.r, TITLE_COLOR.g, TITLE_COLOR.b, alpha * 0.5f);
        batch.draw(whitePixel, imageX - 3, imageY - 3, FIXED_IMAGE_WIDTH + 6, FIXED_IMAGE_HEIGHT + 6);

        // Draw actual image
        batch.setColor(Color.WHITE.r, Color.WHITE.g, Color.WHITE.b, alpha);
        batch.draw(
                currentPage.image,
                imageX, imageY,
                FIXED_IMAGE_WIDTH, FIXED_IMAGE_HEIGHT
        );
    }

    private void drawEnhancedNavigationControls(SpriteBatch batch) {
        buttonFont.setColor(Color.WHITE.r, Color.WHITE.g, Color.WHITE.b, alpha);

        // Draw next button if not on last page
        if (currentPageIndex < pages.size() - 1) {
            Color btnColor = nextBtnRect == hoverButton ? BUTTON_HOVER_COLOR : BUTTON_COLOR;
            batch.setColor(btnColor.r, btnColor.g, btnColor.b, alpha);

            if (nextButton != null) {
                batch.draw(nextButton, nextBtnRect.x, nextBtnRect.y, nextBtnRect.width, nextBtnRect.height);
            } else {
                // Draw styled "Next" button
                batch.draw(whitePixel, nextBtnRect.x, nextBtnRect.y, nextBtnRect.width, nextBtnRect.height);
                buttonFont.draw(batch, ">", nextBtnRect.x + nextBtnRect.width/2 - 6,
                        nextBtnRect.y + nextBtnRect.height/2 + 8);
            }
        }

        // Draw previous button if not on first page
        if (currentPageIndex > 0) {
            Color btnColor = prevBtnRect == hoverButton ? BUTTON_HOVER_COLOR : BUTTON_COLOR;
            batch.setColor(btnColor.r, btnColor.g, btnColor.b, alpha);

            if (prevButton != null) {
                batch.draw(prevButton, prevBtnRect.x, prevBtnRect.y, prevBtnRect.width, prevBtnRect.height);
            } else {
                // Draw styled "Prev" button
                batch.draw(whitePixel, prevBtnRect.x, prevBtnRect.y, prevBtnRect.width, prevBtnRect.height);
                buttonFont.draw(batch, "<", prevBtnRect.x + prevBtnRect.width/2 - 6,
                        prevBtnRect.y + prevBtnRect.height/2 + 8);
            }
        }

        // Draw page counter
        batch.setColor(TEXT_COLOR.r, TEXT_COLOR.g, TEXT_COLOR.b, alpha);
        String pageText = (currentPageIndex + 1) + "/" + pages.size();
        GlyphLayout pageLayout = new GlyphLayout(buttonFont, pageText);
        buttonFont.draw(batch, pageText, x + width/2 - pageLayout.width/2, y + 40);

        // Draw close button
        Color closeBtnColor = closeBtnRect == hoverButton ? BUTTON_HOVER_COLOR : BUTTON_COLOR;
        batch.setColor(closeBtnColor.r, closeBtnColor.g, closeBtnColor.b, alpha);
        batch.draw(whitePixel, closeBtnRect.x, closeBtnRect.y, closeBtnRect.width, closeBtnRect.height);
        buttonFont.draw(batch, "X", closeBtnRect.x + closeBtnRect.width/2 - 8,
                closeBtnRect.y + closeBtnRect.height/2 + 8);
    }

    private void updateButtonHover(float touchX, float touchY) {
        if (!visible) {
            hoverButton = null;
            return;
        }

        if (nextBtnRect.contains(touchX, touchY) && currentPageIndex < pages.size() - 1) {
            hoverButton = nextBtnRect;
        } else if (prevBtnRect.contains(touchX, touchY) && currentPageIndex > 0) {
            hoverButton = prevBtnRect;
        } else if (closeBtnRect.contains(touchX, touchY)) {
            hoverButton = closeBtnRect;
        } else {
            hoverButton = null;
        }
    }

    public boolean handleClick(float touchX, float touchY) {
        touchY = Gdx.graphics.getHeight() - touchY; // Invert Y coordinate for touch input
        if (!visible) return false;

        if (nextBtnRect.contains(touchX, touchY) && currentPageIndex < pages.size() - 1) {
            currentPageIndex++;
            return true;
        }

        if (prevBtnRect.contains(touchX, touchY) && currentPageIndex > 0) {
            currentPageIndex--;
            return true;
        }

        if (closeBtnRect.contains(touchX, touchY)) {
            markCurrentTutorialCompleted();
            animatingOut = true;
            return true;
        }

        return false;
    }

    public void show() {
        visible = true;
        currentPageIndex = 0;
        animatingIn = true;
        alpha = 0f;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void show(String tutorialType) {
        loadTutorialsFromManager(tutorialType);
        visible = true;
        currentPageIndex = 0;
        animatingIn = true;
        alpha = 0f;
    }

    public void hide() {
        animatingOut = true;
    }

    /**
     * Marks the current tutorial as completed so it won't show again
     */
    private void markCurrentTutorialCompleted() {
        if (currentTutorialType != null && tutorialManager != null) {
//            tutorialManager.markTutorialCompleted(currentTutorialType);
        }
    }

    /**
     * Loads tutorial content from the TutorialManager
     */
    private void loadTutorialsFromManager(String tutorialType) {
        currentTutorialType = tutorialType;
        pages.clear();

        if (tutorialManager == null) return;

        List<Tutorial> tutorialList = tutorialManager.getTutorialsByType(tutorialType);
        if (tutorialList == null || tutorialList.isEmpty()) return;

        // Load tutorial content from the Tutorial objects
        for (Tutorial tutorial : tutorialList) {
            Texture image = null;
            if (tutorial.getImage() != null && !tutorial.getImage().isEmpty()) {
                // Load or reuse texture
                image = loadedTextures.computeIfAbsent(
                        tutorial.getImage(),
                        path -> new Texture(Gdx.files.internal("tutorials/"+path))
                );
            }
            pages.add(new TutorialPage(tutorial.getTitle(), tutorial.getText(), image));
        }
    }

    @Override
    public void dispose() {
        if (titleFont != null) titleFont.dispose();
        if (contentFont != null) contentFont.dispose();
        if (buttonFont != null) buttonFont.dispose();
        if (whitePixel != null) whitePixel.dispose();

        // Dispose all loaded textures
        for (Texture texture : loadedTextures.values()) {
            if (texture != null) {
                texture.dispose();
            }
        }
        loadedTextures.clear();
    }

    /**
     * Add a page to the tutorial manually
     */
    public void addPage(String title, String content, Texture image) {
        pages.add(new TutorialPage(title, content, image));
    }
}