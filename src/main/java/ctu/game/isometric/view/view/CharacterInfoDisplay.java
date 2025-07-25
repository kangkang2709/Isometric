package ctu.game.isometric.view.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.TimeUtils;
import ctu.game.isometric.model.entity.Character;

import java.util.HashMap;
import java.util.Map;

import static ctu.game.isometric.util.FontGenerator.generateVietNameseFont;

public class CharacterInfoDisplay {
    // FF7R-inspired color scheme
    private static final Color BACKGROUND = new Color(0.05f, 0.08f, 0.12f, 0.85f);
    private static final Color PANEL_BACKGROUND = new Color(0.08f, 0.1f, 0.15f, 0.92f);
    private static final Color PRIMARY_BLUE = new Color(0.12f, 0.65f, 0.89f, 1f);
    private static final Color SECONDARY_BLUE = new Color(0.2f, 0.4f, 0.8f, 1f);
    private static final Color HIGHLIGHT = new Color(0.12f, 0.9f, 0.9f, 1f);
    private static final Color HP_COLOR = new Color(0.18f, 0.8f, 0.44f, 1f);
    private static final Color MP_COLOR = new Color(0.25f, 0.6f, 0.9f, 1f);
    private static final Color EXP_COLOR = new Color(0.9f, 0.78f, 0.25f, 1f);

    private static final int TITLE_FONT_SIZE = 28;
    private static final int TAB_FONT_SIZE = 22;
    private static final int STAT_FONT_SIZE = 20;
    private static final float GLOW_INTENSITY = 0.8f;
    private static final float ANIMATION_SPEED = 0.6f;

    // UI Components
    private Character character;
    private BitmapFont titleFont;
    private BitmapFont tabFont;
    private BitmapFont statFont;
    private GlyphLayout layout;
    private ShapeRenderer shapeRenderer;
    private FrameBuffer blurBuffer;
    private ShaderProgram glowShader;
    private ShaderProgram blurShader;

    // Textures and visual assets
    private Texture avatarTexture;
    private Texture tabIcons;
    private Texture borderTexture;
    private Texture glowTexture;
    private Texture backgroundTexture;

    // UI Layout
    private Rectangle mainPanel;
    private Rectangle headerSection;
    private Rectangle tabSection;
    private Rectangle contentSection;
    private Rectangle portraitSection;
    private Rectangle[] tabRects;

    // Animation state
    private Map<String, Float> animatedValues = new HashMap<>();
    private long lastUpdateTime;
    private int activeTab = 0;
    private float tabTransition = 1.0f;
    private boolean isAnimating = false;

    // Audio
    private Sound selectSound;
    private Sound tabSound;
    private Sound hoverSound;

    // Tab content components
    private StatsTabContent statsTab;

    private boolean initialized = false;

    public CharacterInfoDisplay(Character character) {
        this.character = character;
        lastUpdateTime = TimeUtils.millis();
        initialize();
    }

    private void initialize() {
        if (!initialized) {
            try {
                // Initialize fonts
                this.titleFont = generateVietNameseFont("NovaSquare-Regular.ttf", TITLE_FONT_SIZE);
                this.tabFont = generateVietNameseFont("NovaSquare-Regular.ttf", TAB_FONT_SIZE);
                this.statFont = generateVietNameseFont("NovaSquare-Regular.ttf", STAT_FONT_SIZE);
                this.layout = new GlyphLayout();
                this.shapeRenderer = new ShapeRenderer();

                // Initialize shaders
                initializeShaders();

                // Load textures and assets
                loadTextures();
                loadSounds();

                // Initialize animation values
                initializeAnimationValues();

                // Setup UI layout
                setupLayout();

                // Initialize tab contents
                initializeTabs();

                initialized = true;
            } catch (Exception e) {
                Gdx.app.error("CharacterInfoDisplay", "Failed to initialize: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void initializeShaders() {
        // Glow shader for borders and highlights
        String vertexShader = Gdx.files.internal("shaders/default.vert").readString();
        String glowFragmentShader = Gdx.files.internal("shaders/glow.frag").readString();
        glowShader = new ShaderProgram(vertexShader, glowFragmentShader);

        // Blur shader for background effects
        String blurFragmentShader = Gdx.files.internal("shaders/blur.frag").readString();
        blurShader = new ShaderProgram(vertexShader, blurFragmentShader);

        // Create framebuffer for blur effect
        blurBuffer = new FrameBuffer(Pixmap.Format.RGBA8888,
                Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);
    }

    private void loadTextures() {
        // Load avatar based on character gender
        avatarTexture = character.getGender().name().equals("MALE") ?
                new Texture(Gdx.files.internal("characters/male2.png")) :
                new Texture(Gdx.files.internal("characters/female2.png"));

        // Load tab icons
        tabIcons = new Texture(Gdx.files.internal("ui/button.png"));

        // Create decorative textures
        createDecoTextures();

        // Load background texture
        backgroundTexture = new Texture(Gdx.files.internal("ui/button.png"));
    }

    private void loadSounds() {
        selectSound = Gdx.audio.newSound(Gdx.files.internal("audio/effects/click.ogg"));
        tabSound = Gdx.audio.newSound(Gdx.files.internal("audio/effects/click.ogg"));
        hoverSound = Gdx.audio.newSound(Gdx.files.internal("audio/effects/click.ogg"));
    }

    private void createDecoTextures() {
        // Create glow texture
        Pixmap glowPixmap = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        glowPixmap.setColor(1, 1, 1, 1);
        glowPixmap.fillCircle(32, 32, 30);
        glowTexture = new Texture(glowPixmap);
        glowPixmap.dispose();

        // Create border texture
        Pixmap borderPixmap = new Pixmap(3, 3, Pixmap.Format.RGBA8888);
        borderPixmap.setColor(1, 1, 1, 1);
        borderPixmap.fill();
        borderTexture = new Texture(borderPixmap);
        borderPixmap.dispose();
    }

    private void initializeAnimationValues() {
        // Initialize with current values to avoid jumps
        animatedValues.put("health", (float) character.getHealth());
        animatedValues.put("mana", (float) character.getMana());
        animatedValues.put("exp", (float) character.getExp());
    }

    private void setupLayout() {
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        float panelWidth = Math.min(1100f, screenWidth * 0.85f);
        float panelHeight = screenHeight * 0.85f;
        float panelX = (screenWidth - panelWidth) / 2;
        float panelY = (screenHeight - panelHeight) / 2;

        // Main panel
        mainPanel = new Rectangle(panelX, panelY, panelWidth, panelHeight);

        // Header section (title + portrait)
        headerSection = new Rectangle(
                mainPanel.x + 20,
                mainPanel.y + mainPanel.height - 100,
                mainPanel.width - 40,
                80
        );

        // Portrait section
        portraitSection = new Rectangle(
                mainPanel.x + 40,
                mainPanel.y + mainPanel.height - 345,
                180,
                180
        );

        // Tab navigation section
        tabSection = new Rectangle(
                mainPanel.x + 20,
                mainPanel.y + mainPanel.height - 140,
                mainPanel.width - 40,
                40
        );

        // Content area for tab content
        contentSection = new Rectangle(
                mainPanel.x + 240,
                mainPanel.y + 40,
                mainPanel.width - 280,
                mainPanel.height - 200
        );

        // Tab button rectangles
        tabRects = new Rectangle[3];
        float tabWidth = tabSection.width / 3;
        for (int i = 0; i < 3; i++) {
            tabRects[i] = new Rectangle(
                    tabSection.x + i * tabWidth,
                    tabSection.y,
                    tabWidth,
                    tabSection.height
            );
        }
    }

    // Tab content components
    private SkillsTabContent skillsTab;
    private BiographyTabContent bioTab;

    private void initializeTabs() {
        statsTab = new StatsTabContent(character, statFont);
        skillsTab = new SkillsTabContent(character, statFont);
        bioTab = new BiographyTabContent(character, statFont, getMainObjectiveDescriptions());
    }

    // Helper method to get objectives from GameController
    private Map<String, String> getMainObjectiveDescriptions() {
        Map<String, String> objectives = new HashMap<>();
        objectives.put("intro", "Mình cần tìm đường rời khỏi khu rừng này trước tiên.");
        objectives.put("forest_done", "Có vẻ như mình đã đến một ngôi làng nhỏ, mình nên khám phá xung quanh.");
        objectives.put("god_intro", "Cleric Klein có thể giúp mình hiểu rõ hơn về thế giới này.");
        objectives.put("klein_meet", "Nói chuyện với Cleric Klein");
        objectives.put("dungeon_call", "Tiến đến hầm ngục thông qua cổng dịch chuyển theo lời chỉ dẫn của Cleric Klein.");
        objectives.put("dungeon_entry", "Vượt qua hầm ngục và tìm hiểu bí mật của thế giới này.\nMục tiêu: tìm kiếm 3 viên ngọc và sống sót đến tầng cuối.");
        return objectives;
    }

    Matrix4 uiProjectionMatrix = new Matrix4().setToOrtho2D(0, 0,
            Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

    public void render(SpriteBatch batch) {
        update();

        Matrix4 originalMatrix = batch.getProjectionMatrix().cpy();
        boolean batchWasDrawing = batch.isDrawing();

        try {
            if (batchWasDrawing) {
                batch.end();
            }

            // Render blur effect for background
            renderBlurredBackground(batch);

            // Draw UI elements
            renderPanels();

            batch.begin();
            batch.setProjectionMatrix(uiProjectionMatrix);

            renderHeader(batch);
            renderPortrait(batch);
            renderTabs(batch);
            renderActiveTabContent(batch);
            renderStatusBars(batch);

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

    private void update() {
        long currentTime = TimeUtils.millis();
        float deltaTime = Math.min((currentTime - lastUpdateTime) / 1000f, 0.1f);
        lastUpdateTime = currentTime;

        // Update animated values with smooth transitions
        updateAnimatedValues(deltaTime);

        // Update tab transition animation
        if (isAnimating) {
            tabTransition -= deltaTime * 5f;
            if (tabTransition <= 0) {
                tabTransition = 0;
                isAnimating = false;
            }
        }
    }

    private void updateAnimatedValues(float deltaTime) {
        // Add null checks and default values to prevent NullPointerException

        // Smoothly animate health value
        float targetHealth = character.getHealth();
        float currentHealth = animatedValues.getOrDefault("health", targetHealth);
        animatedValues.put("health", interpolateValue(currentHealth, targetHealth, deltaTime));

        // Smoothly animate mana value
        float targetMana = character.getMana();
        float currentMana = animatedValues.getOrDefault("mana", targetMana);
        animatedValues.put("mana", interpolateValue(currentMana, targetMana, deltaTime));

        // Smoothly animate exp value
        float targetExp = character.getExp();
        float currentExp = animatedValues.getOrDefault("exp", targetExp);
        animatedValues.put("exp", interpolateValue(currentExp, targetExp, deltaTime));
    }

    private float interpolateValue(float current, float target, float deltaTime) {
        float diff = target - current;
        if (Math.abs(diff) < 0.1f) return target;
        return current + diff * Math.min(1.0f, deltaTime * ANIMATION_SPEED * 5);
    }

    private void renderBlurredBackground(SpriteBatch batch) {
        // Use frame buffer to create blur effect for background
        blurBuffer.begin();
        Gdx.gl.glClearColor(0, 0, 0, 0);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        batch.setShader(null);
        batch.draw(backgroundTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.end();

        blurBuffer.end();

        // Draw the blurred background
        batch.begin();
        batch.setShader(blurShader);
        blurShader.setUniformf("resolution", Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        blurShader.setUniformf("radius", 2.5f);

        TextureRegion blurRegion = new TextureRegion(blurBuffer.getColorBufferTexture());
        blurRegion.flip(false, true);
        batch.draw(blurRegion, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        batch.setShader(null);
        batch.end();
    }

    private void renderPanels() {
        shapeRenderer.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0,
                Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));

        // Draw panel backgrounds
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Main panel background
        shapeRenderer.setColor(BACKGROUND);
        shapeRenderer.rect(mainPanel.x, mainPanel.y, mainPanel.width, mainPanel.height);

        // Content panel background
        shapeRenderer.setColor(PANEL_BACKGROUND);
        shapeRenderer.rect(contentSection.x, contentSection.y, contentSection.width, contentSection.height);

        shapeRenderer.end();

        // Draw glowing borders
        renderGlowingBorders();
    }

    private void renderGlowingBorders() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

        // Outer border glow effect
        float pulseIntensity = 0.7f + 0.3f * (float) Math.sin(TimeUtils.millis() / 1000.0 * 2);
        Color glowColor = new Color(PRIMARY_BLUE);
        glowColor.a = GLOW_INTENSITY * pulseIntensity;

        shapeRenderer.setColor(glowColor);

        // Main panel border
        drawBorderLines(mainPanel, 2f);

        // Content panel border
        drawBorderLines(contentSection, 1.5f);

        // Tab section border
        drawBorderLines(tabSection, 1.5f);

        shapeRenderer.end();

        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void drawBorderLines(Rectangle rect, float thickness) {
        shapeRenderer.rectLine(rect.x, rect.y, rect.x + rect.width, rect.y, thickness);
        shapeRenderer.rectLine(rect.x + rect.width, rect.y, rect.x + rect.width, rect.y + rect.height, thickness);
        shapeRenderer.rectLine(rect.x + rect.width, rect.y + rect.height, rect.x, rect.y + rect.height, thickness);
        shapeRenderer.rectLine(rect.x, rect.y + rect.height, rect.x, rect.y, thickness);
    }

    private void renderHeader(SpriteBatch batch) {
        // Draw character name and level
        titleFont.setColor(PRIMARY_BLUE);
        String headerText = character.getName() + " - Lv. " + character.getLevel();
        layout.setText(titleFont, headerText);

        float textX = headerSection.x + 20;
        float textY = headerSection.y + headerSection.height - 20;

        // Shadow effect
        titleFont.setColor(0, 0, 0, 0.5f);
        titleFont.draw(batch, headerText, textX + 2, textY - 2);

        // Main text
        titleFont.setColor(PRIMARY_BLUE);
        titleFont.draw(batch, headerText, textX, textY);

        titleFont.setColor(Color.YELLOW);
        titleFont.draw(batch, "GOLD: " + character.getScore(), mainPanel.x + mainPanel.width - 200, textY);

        // Character class/job
        statFont.setColor(HIGHLIGHT);
        String classText = "Adventurer";  // Replace with actual character class if available
        layout.setText(statFont, classText);
        statFont.draw(batch, classText, textX, textY - 30);
    }

    private void renderPortrait(SpriteBatch batch) {
        // Draw portrait background with glow
        batch.setColor(PRIMARY_BLUE);
        batch.setShader(glowShader);
        glowShader.setUniformf("u_intensity", GLOW_INTENSITY * 0.8f);

        batch.draw(glowTexture,
                portraitSection.x - 10,
                portraitSection.y - 10,
                portraitSection.width + 20,
                portraitSection.height + 20);

        batch.setShader(null);

        // Draw portrait frame
        batch.setColor(SECONDARY_BLUE);
        batch.draw(borderTexture,
                portraitSection.x - 2,
                portraitSection.y - 2,
                portraitSection.width + 4,
                portraitSection.height + 4);

        // Draw character portrait
        batch.setColor(Color.WHITE);
        batch.draw(avatarTexture,
                portraitSection.x,
                portraitSection.y,
                portraitSection.width,
                portraitSection.height);

        // Draw status effect icons if applicable
    }

    private void renderTabs(SpriteBatch batch) {
        String[] tabNames = {"Stats", "Skills", "Bio"};

        for (int i = 0; i < tabRects.length; i++) {
            Rectangle tabRect = tabRects[i];
            boolean isActive = i == activeTab;

            // Tab background
            Color tabColor = isActive ? PRIMARY_BLUE : PANEL_BACKGROUND;
            batch.setColor(tabColor);
            batch.draw(borderTexture, tabRect.x, tabRect.y, tabRect.width, tabRect.height);

            // Tab text
            tabFont.setColor(isActive ? Color.WHITE : SECONDARY_BLUE);
            layout.setText(tabFont, tabNames[i]);
            float textX = tabRect.x + (tabRect.width - layout.width) / 2;
            float textY = tabRect.y + (tabRect.height + layout.height) / 2;

            tabFont.draw(batch, tabNames[i], textX, textY);

            // Tab highlight for active tab
            if (isActive) {
                batch.setColor(HIGHLIGHT);
                batch.draw(borderTexture,
                        tabRect.x, tabRect.y + tabRect.height - 3,
                        tabRect.width, 3);
            }
        }
    }

    private void renderActiveTabContent(SpriteBatch batch) {
        // Apply tab transition animation if active
        if (isAnimating) {
            batch.setColor(1, 1, 1, 1 - tabTransition);
        } else {
            batch.setColor(1, 1, 1, 1);
        }

        // Render the appropriate tab content based on activeTab
        switch (activeTab) {
            case 0:
                if (statsTab != null) statsTab.render(batch, contentSection);
                break;
            case 1:
                if (skillsTab != null) skillsTab.render(batch, contentSection);
                break;
            case 2:
                if (bioTab != null) bioTab.render(batch, contentSection);
                break;
        }

        // Reset color
        batch.setColor(1, 1, 1, 1);
    }

    private void renderStatusBars(SpriteBatch batch) {
        float barWidth = 180;
        float barHeight = 18;
        float barX = portraitSection.x;
        float barY = portraitSection.y - 60;
        float spacing = barHeight + 10;

        batch.setColor(Color.WHITE);

        // HP bar
        renderBar(batch, barX, barY, barWidth, barHeight,
                animatedValues.get("health"), character.getMaxHealth(),
                "HP", HP_COLOR);

        // MP bar
        renderBar(batch, barX, barY - spacing, barWidth, barHeight,
                animatedValues.get("mana"), character.getMaxMana(),
                "MP", MP_COLOR);

        // EXP bar
        int expRequired = character.getLevel() * 50;
        renderBar(batch, barX, barY - spacing * 2, barWidth, barHeight,
                animatedValues.get("exp"), expRequired,
                "EXP", EXP_COLOR);
    }

    private void renderBar(SpriteBatch batch, float x, float y, float width, float height,
                           float current, float max, String label, Color barColor) {
        float percentage = Math.min(current / max, 1.0f);

        // Draw bar background
        batch.draw(borderTexture, x, y, width, height);

        // Draw bar fill with glow shader
        batch.setShader(glowShader);
        glowShader.setUniformf("u_intensity", 0.4f);

        batch.setColor(barColor);
        batch.draw(borderTexture, x, y, width * percentage, height);
        batch.setShader(null);

        // Draw bar border
        batch.setColor(PRIMARY_BLUE);
        batch.draw(borderTexture, x, y, width, 1);
        batch.draw(borderTexture, x, y + height - 1, width, 1);
        batch.draw(borderTexture, x, y, 1, height);
        batch.draw(borderTexture, x + width - 1, y, 1, height);

        // Draw text
        statFont.setColor(Color.WHITE);
        String text = label + ": " + (int) current + "/" + (int) max;
        layout.setText(statFont, text);

        float textX = x + 10;
        float textY = y + (height + layout.height) / 2;

        // Text shadow
        statFont.setColor(0, 0, 0, 0.7f);
        statFont.draw(batch, text, textX + 1, textY - 1);

        statFont.setColor(Color.WHITE);
        statFont.draw(batch, text, textX, textY);
    }

    public boolean handleClick(float screenX, float screenY) {
        if (!initialized) return false;

        float y = Gdx.graphics.getHeight() - screenY; // Convert to OpenGL coordinates

        // Check tab clicks
        System.out.println("Handling click at: " + screenX + ", " + y);

        for (int i = 0; i < tabRects.length; i++) {
            if (tabRects[i] != null && tabRects[i].contains(screenX, y)) {
                if (activeTab != i) {
                    setActiveTab(i);
                    return true;
                }
                return true; // Return true even if already active
            }
        }

        // Handle other interactive elements based on active tab
        switch (activeTab) {
            case 0:
                return statsTab != null && statsTab.handleClick(screenX, y, contentSection);
            case 1:
                return skillsTab != null && skillsTab.handleClick(screenX, y, contentSection);
            case 2:
                return bioTab != null && bioTab.handleClick(screenX, y, contentSection);
        }

        return false;
    }

    public boolean handleHover(float screenX, float screenY) {
        float y = Gdx.graphics.getHeight() - screenY; // Convert to OpenGL coordinates

        // Check tab hovers for visual feedback
        for (int i = 0; i < tabRects.length; i++) {
            if (tabRects[i].contains(screenX, y)) {
                if (i != activeTab) {
                    hoverSound.play(0.2f);
                }
                return true;
            }
        }

        // Check for hoverable elements in skill tab
        if (activeTab == 1) {
            return skillsTab.handleHover(screenX, y, contentSection);
        }

        return false;
    }

    private void setActiveTab(int tabIndex) {
        // Fix check to match 3 tabs instead of 4
        if (tabIndex != activeTab && tabIndex >= 0 && tabIndex < 3) {
            isAnimating = true;
            tabTransition = 0f;
            activeTab = tabIndex;

            // Play sound when changing tabs
            if (tabSound != null) {
                tabSound.play(0.3f);
            }
        }
    }

    public boolean handleScroll(float amountY) {

        return bioTab.handleScroll(amountY);
    }

    public void dispose() {
        if (titleFont != null) titleFont.dispose();
        if (tabFont != null) tabFont.dispose();
        if (statFont != null) statFont.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (blurBuffer != null) blurBuffer.dispose();
        if (glowShader != null) glowShader.dispose();
        if (blurShader != null) blurShader.dispose();
        if (avatarTexture != null) avatarTexture.dispose();
        if (borderTexture != null) borderTexture.dispose();
        if (glowTexture != null) glowTexture.dispose();
        if (backgroundTexture != null) backgroundTexture.dispose();
        if (tabIcons != null) tabIcons.dispose();
        if (selectSound != null) selectSound.dispose();
        if (tabSound != null) tabSound.dispose();
        if (hoverSound != null) hoverSound.dispose();

        // Dispose tab content resources
        if (statsTab != null) statsTab.dispose();
        if (skillsTab != null) skillsTab.dispose();
        if (bioTab != null) bioTab.dispose();

        initialized = false;
    }
}