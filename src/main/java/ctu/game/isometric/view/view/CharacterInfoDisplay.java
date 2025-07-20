package ctu.game.isometric.view.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import ctu.game.isometric.model.entity.Character;

import java.util.Map;

import static ctu.game.isometric.util.FontGenerator.generateVietNameseFont;

public class CharacterInfoDisplay {
    // Constants for Persona-style UI
    private static final Color CYAN_BLUE = new Color(0x00f0ffff);
    private static final Color DARK_BACKGROUND = new Color(0x111111ff);
    private static final Color DARKER_BACKGROUND = new Color(0x0d0d0dff);
    private static final Color STATS_BACKGROUND = new Color(0x222222ff);
    private static final Color WHITE = Color.WHITE;
    private static final Color BLACK = Color.BLACK;

    private static final int TITLE_FONT_SIZE = 32;
    private static final int STAT_FONT_SIZE = 20;
    private static final int SCORE_FONT_SIZE = 24;
    private static final float BORDER_WIDTH = 6f;
    private static final float UI_MARGIN = 40f;
    private static final float AVATAR_SIZE = 150f;

    private Character character;
    private BitmapFont titleFont;
    private BitmapFont statFont;
    private BitmapFont scoreFont;
    private GlyphLayout layout;
    private ShapeRenderer shapeRenderer;

    private Texture maleAvatar;
    private Texture femaleAvatar;
    private Texture currentAvatar;
    private Texture glowTexture;

    private Rectangle mainPanel;
    private Rectangle topSection;
    private Rectangle bottomSection;
    private Rectangle avatarRect;
    private Rectangle statsRect;
    private Rectangle scoreRect;

    private boolean initialized = false;

    public CharacterInfoDisplay(Character character) {
        this.character = character;
        initialize();
    }

    private void initialize() {
        if (!initialized) {
            try {
                // Initialize fonts with different sizes
                this.titleFont = generateVietNameseFont("Roboto-Black.ttf", TITLE_FONT_SIZE);
                this.statFont = generateVietNameseFont("Roboto-Black.ttf", STAT_FONT_SIZE);
                this.scoreFont = generateVietNameseFont("Roboto-Black.ttf", SCORE_FONT_SIZE);
                this.layout = new GlyphLayout();
                this.shapeRenderer = new ShapeRenderer();

                // Load avatars
                loadAvatars();

                // Create glow effect texture
                createGlowTexture();

                // Setup UI layout
                setupLayout();

                initialized = true;
            } catch (Exception e) {
                Gdx.app.error("PersonaStyleCharacterInfoDisplay", "Failed to initialize: " + e.getMessage());
            }
        }
    }

    private void loadAvatars() {
        maleAvatar = new Texture(Gdx.files.internal("characters/male_avatar.png"));
        femaleAvatar = new Texture(Gdx.files.internal("characters/female_avatar.png"));
        currentAvatar = character.getGender().name().equals("MALE") ? maleAvatar : femaleAvatar;
    }

    private void createGlowTexture() {
        // Create a simple glow effect texture
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(CYAN_BLUE);
        pixmap.fill();
        glowTexture = new Texture(pixmap);
        pixmap.dispose();
    }

    private void setupLayout() {
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        float panelWidth = Math.min(1000f, screenWidth - UI_MARGIN * 2);
        float panelHeight = screenHeight - UI_MARGIN * 2;
        float panelX = (screenWidth - panelWidth) / 2;
        float panelY = UI_MARGIN;

        mainPanel = new Rectangle(panelX, panelY, panelWidth, panelHeight);

        // Top section for avatar and stats
        float topHeight = panelHeight * 0.6f;
        topSection = new Rectangle(panelX + 30, panelY + panelHeight - topHeight - 60, panelWidth - 60, topHeight);

        // Avatar rectangle
        avatarRect = new Rectangle(topSection.x + 20, topSection.y + (topSection.height - AVATAR_SIZE) / 2, AVATAR_SIZE, AVATAR_SIZE);

        // Stats rectangle (remaining space in top section)
        statsRect = new Rectangle(avatarRect.x + AVATAR_SIZE + 230, topSection.y + 20,
                topSection.width - AVATAR_SIZE - 70, topSection.height - 40);

        // Bottom section for additional stats
        float bottomHeight = panelHeight * 0.2f;
        bottomSection = new Rectangle(panelX + 30, panelY + 120, panelWidth - 60, bottomHeight);

        // Score section
        scoreRect = new Rectangle(panelX + 30, panelY + 30, panelWidth - 60, 60);
    }

    public void render(SpriteBatch batch) {
        Matrix4 originalMatrix = batch.getProjectionMatrix().cpy();
        boolean batchWasDrawing = batch.isDrawing();

        try {
            if (batchWasDrawing) {
                batch.end();
            }

            // Set up orthographic projection for UI
            Matrix4 uiMatrix = new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

            // Draw shapes first
            renderBackground();

            // Then draw sprites and text
            batch.begin();
            batch.setProjectionMatrix(uiMatrix);

            renderTitle(batch);
            renderAvatar(batch);
            renderStats(batch);
            renderBottomStats(batch);
            renderScore(batch);

        } finally {
            if (batchWasDrawing) {
                batch.end();
                batch.begin();
                batch.setProjectionMatrix(originalMatrix);
            } else {
                batch.end();
            }
        }
    }

    private void renderBackground() {
        shapeRenderer.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));

        // Draw main panel background with glow effect
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Outer glow
        shapeRenderer.setColor(CYAN_BLUE.r, CYAN_BLUE.g, CYAN_BLUE.b, 0.3f);
        shapeRenderer.rect(mainPanel.x - 10, mainPanel.y - 10, mainPanel.width + 20, mainPanel.height + 20);

        // Border
        shapeRenderer.setColor(CYAN_BLUE);
        shapeRenderer.rect(mainPanel.x - BORDER_WIDTH, mainPanel.y - BORDER_WIDTH,
                mainPanel.width + BORDER_WIDTH * 2, mainPanel.height + BORDER_WIDTH * 2);

        // Main background
        shapeRenderer.setColor(DARK_BACKGROUND);
        shapeRenderer.rect(mainPanel.x, mainPanel.y, mainPanel.width, mainPanel.height);

        // Bottom section background (dashed effect simulation)
        shapeRenderer.setColor(STATS_BACKGROUND);
        shapeRenderer.rect(bottomSection.x, bottomSection.y, bottomSection.width, bottomSection.height);

        // Dashed border effect for bottom section
        shapeRenderer.setColor(CYAN_BLUE);
        float dashSize = 10f;
        float gapSize = 5f;
        for (float x = bottomSection.x; x < bottomSection.x + bottomSection.width; x += dashSize + gapSize) {
            shapeRenderer.rect(x, bottomSection.y - 3, Math.min(dashSize, bottomSection.x + bottomSection.width - x), 3);
            shapeRenderer.rect(x, bottomSection.y + bottomSection.height, Math.min(dashSize, bottomSection.x + bottomSection.width - x), 3);
        }

        // Score background
        shapeRenderer.setColor(CYAN_BLUE);
        shapeRenderer.rect(scoreRect.x + scoreRect.width / 2 - 150, scoreRect.y, 300, scoreRect.height);

        shapeRenderer.end();
    }

    private void renderTitle(SpriteBatch batch) {
        titleFont.setColor(CYAN_BLUE);
        String title = "🧠 CHARACTER PROFILE";
        layout.setText(titleFont, title);
        float titleX = mainPanel.x + (mainPanel.width - layout.width) / 2;
        float titleY = mainPanel.y + mainPanel.height - 30;

        // Text shadow effect
        titleFont.setColor(BLACK);
        titleFont.draw(batch, title, titleX + 2, titleY - 2);
        titleFont.setColor(CYAN_BLUE);
        titleFont.draw(batch, title, titleX, titleY);
    }

    private void renderAvatar(SpriteBatch batch) {
        if (currentAvatar != null) {
            // Avatar border
            batch.setColor(CYAN_BLUE);
            batch.draw(glowTexture, avatarRect.x - 4, avatarRect.y - 4, avatarRect.width + 8, avatarRect.height + 8);

            // Avatar
            batch.setColor(WHITE);
            batch.draw(currentAvatar, avatarRect.x, avatarRect.y, avatarRect.width, avatarRect.height);
        }
    }

    private void renderStats(SpriteBatch batch) {
        float currentY = statsRect.y + statsRect.height - 40;
        float lineHeight = 45f;

        // Character name
        renderStatLine(batch, "Tên: " + character.getName(), statsRect.x, currentY, CYAN_BLUE);
        currentY -= lineHeight;

        // Level and EXP
        int expRequired = character.getLevel() * 50;
        renderStatLine(batch, "Cấp: " + character.getLevel() + " | EXP: " + character.getExp() + " / " + expRequired,
                statsRect.x, currentY, WHITE);
        currentY -= lineHeight;

        // Health
        renderStatLine(batch, "HP: " + character.getHealth() + " / " + character.getMaxHealth(),
                statsRect.x, currentY, Color.CORAL);
        currentY -= lineHeight;

        // Mana
        renderStatLine(batch, "MP: " + character.getMana() + " / " + character.getMaxMana(),
                statsRect.x, currentY, Color.CYAN);
        currentY -= lineHeight;

        // Attack and Defense
        renderStatLine(batch, "ATK: " + character.getDamage() + " |  DEF: " + character.getDefend(),
                statsRect.x, currentY, Color.ORANGE);
    }

    private void renderStatLine(SpriteBatch batch, String text, float x, float y, Color color) {
        // Left border line
        batch.setColor(CYAN_BLUE);
        batch.draw(glowTexture, x - 15, y - 12, 6, 25);

        // Text
        statFont.setColor(color);
        statFont.draw(batch, text, x, y);
    }

    private void renderBottomStats(SpriteBatch batch) {
        Map<String, Integer> attemptFlags = character.getEttempFlags();
        if (attemptFlags != null) {
            float sectionWidth = bottomSection.width / 3;
            float textY = bottomSection.y + bottomSection.height / 2 + 5;

            statFont.setColor(WHITE);

            // Quiz attempts
            String quizText = "Lượt quiz 1 hôm nay: " + attemptFlags.getOrDefault("quizAttempts", 0);
            layout.setText(statFont, quizText);
            statFont.draw(batch, quizText, bottomSection.x + sectionWidth * 0.5f - layout.width / 2, textY + 10);
            // Quiz attempts
            String quizText2 = "Lượt quiz 2 hôm nay: " + attemptFlags.getOrDefault("mulQuizAttempts", 0);
            layout.setText(statFont, quizText);
            statFont.draw(batch, quizText2, bottomSection.x + sectionWidth * 0.5f - layout.width / 2, textY - 25);

            // Falls
            String fallText = "Gục ngã: " + attemptFlags.getOrDefault("fallen", 0);
            layout.setText(statFont, fallText);
            statFont.draw(batch, fallText, bottomSection.x + sectionWidth * 1.5f - layout.width / 2, textY);

            // Wrong words
            String wrongText = "Từ sai đã gặp: " + attemptFlags.getOrDefault("wrongWord", 0);
            layout.setText(statFont, wrongText);
            statFont.draw(batch, wrongText, bottomSection.x + sectionWidth * 2.5f - layout.width / 2, textY + 10);

            String wordCount = "Số từ đã dùng: " + character.getLearnedWords().size() + character.getNewlearneWords().size();
            layout.setText(statFont, wrongText);
            statFont.draw(batch, wordCount, bottomSection.x + sectionWidth * 2.5f - layout.width / 2, textY - 25);
        }
    }

    private void renderScore(SpriteBatch batch) {
        String scoreText = "Tổng điểm (GOLD):  " + character.getScore();
        scoreFont.setColor(BLACK);
        layout.setText(scoreFont, scoreText);
        float scoreX = scoreRect.x + scoreRect.width / 2 - layout.width / 2;
        float scoreY = scoreRect.y + scoreRect.height / 2 + layout.height / 2;
        scoreFont.draw(batch, scoreText, scoreX, scoreY);
    }

    public void dispose() {
        if (titleFont != null) {
            titleFont.dispose();
            titleFont = null;
        }
        if (statFont != null) {
            statFont.dispose();
            statFont = null;
        }
        if (scoreFont != null) {
            scoreFont.dispose();
            scoreFont = null;
        }
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
            shapeRenderer = null;
        }
        if (maleAvatar != null) {
            maleAvatar.dispose();
            maleAvatar = null;
        }
        if (femaleAvatar != null) {
            femaleAvatar.dispose();
            femaleAvatar = null;
        }
        if (glowTexture != null) {
            glowTexture.dispose();
            glowTexture = null;
        }
        initialized = false;
    }

    public void updateCharacter(Character newCharacter) {
        this.character = newCharacter;
        if (initialized && currentAvatar != null) {
            currentAvatar = character.getGender().name().equals("MALE") ? maleAvatar : femaleAvatar;
        }
    }

    // Optional: Method to handle screen resize
    public void resize(int width, int height) {
        if (initialized) {
            setupLayout();
        }
    }
}