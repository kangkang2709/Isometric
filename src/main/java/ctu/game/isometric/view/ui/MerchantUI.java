package ctu.game.isometric.view.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.TimeUtils;
import ctu.game.isometric.controller.GameController;
import ctu.game.isometric.model.entity.Character;
import ctu.game.isometric.model.game.Items;
import ctu.game.isometric.util.ItemLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ctu.game.isometric.util.FontGenerator.generateVietNameseFont;

public class MerchantUI {
    private GameController gameController;
    private BitmapFont titleFont;
    private BitmapFont font;
    private BitmapFont smallFont;
    private GlyphLayout layout;
    private boolean visible = false;

    private ShapeRenderer shapeRenderer;
    private Texture backgroundTexture;
    private Texture buttonTexture;
    private Texture slotTexture;
    private Texture highlightTexture;
    private Texture borderTexture;

    private int selectedItemIndex = -1;
    private List<Items> merchantItems = new ArrayList<>();
    private Map<Items, Integer> playerItems = new HashMap<>();
    private List<Items> displayItems = new ArrayList<>();

    private Rectangle merchantBounds;
    private Rectangle[] itemSlots;
    private Rectangle[] playerItemSlots;
    private Rectangle buyButton;
    private Rectangle sellButton;
    private Rectangle closeButton;
    private Rectangle tabMerchantButton;
    private Rectangle tabPlayerButton;

    private boolean showingMerchantItems = true;
    private float animTime = 0;
    private float hoverAlpha = 0;

    private boolean hoverBuyButton = false;
    private boolean hoverSellButton = false;
    private boolean hoverMerchantTab = false;
    private boolean hoverPlayerTab = false;
    private boolean hoverCloseButton = false;

    private static final int SLOTS_PER_ROW = 5;
    private static final int MAX_SLOTS = 15;
    private static final int SLOT_SIZE = 64;
    private static final int PADDING = 12;
    private static final float CORNER_RADIUS = 10f;

    private Map<String, Texture> itemTextures = new HashMap<>();
    private Matrix4 uiMatrix;

    // Enhanced UI colors
    private final Color activeTabColor = new Color(0.3f, 0.4f, 0.8f, 1f);
    private final Color inactiveTabColor = new Color(0.22f, 0.22f, 0.28f, 1f);
    private final Color closeButtonColor = new Color(0.7f, 0.3f, 0.3f, 1f);
    private final Color closeButtonHoverColor = new Color(0.8f, 0.4f, 0.4f, 1f);

    private final Color unaffordableColor = new Color(0.9f, 0.4f, 0.4f, 0.7f);

    private final Color titleColor = new Color(0.9f, 0.9f, 1f, 1f);

    // Animation system
    private Map<String, Float> animatedValues;
    private long lastAnimationTime;

    // Add these new fields to MerchantUI class
    private ShaderProgram blurShader;
    private ShaderProgram glowShader;
    private FrameBuffer frameBuffer;
    private float animationSpeed = 0.3f;
    private float openProgress = 0;
    private boolean isOpening = false;
    private boolean isClosing = false;

    // New colors for FF7 Remake style
    private final Color bgColor = new Color(0.1f, 0.12f, 0.2f, 0.92f);
    private final Color accentColor = new Color(0.4f, 0.8f, 0.9f, 0.8f);
    private final Color glowColor = new Color(0.4f, 0.8f, 0.9f, 0.3f);
    private final Color highlightColor = new Color(0.5f, 0.9f, 1f, 1f);
    private final Color buttonColor = new Color(0.15f, 0.2f, 0.3f, 1f);
    private final Color buttonHoverColor = new Color(0.2f, 0.3f, 0.5f, 1f);

    private void initializeShaders() {
        try {
            // Basic vertex shader (common for both effects)
            String vertexShader = "attribute vec4 a_position;\n" +
                    "attribute vec4 a_color;\n" +
                    "attribute vec2 a_texCoord0;\n" +
                    "uniform mat4 u_projTrans;\n" +
                    "varying vec4 v_color;\n" +
                    "varying vec2 v_texCoords;\n" +
                    "void main() {\n" +
                    "    v_color = a_color;\n" +
                    "    v_texCoords = a_texCoord0;\n" +
                    "    gl_Position = u_projTrans * a_position;\n" +
                    "}";

            // Glow shader for highlights
            String glowFragmentShader =
                    "varying vec2 v_texCoords;\n" +
                            "varying vec4 v_color;\n" +
                            "uniform sampler2D u_texture;\n" +
                            "uniform float u_intensity;\n" +
                            "void main() {\n" +
                            "    vec4 color = texture2D(u_texture, v_texCoords) * v_color;\n" +
                            "    vec3 glow = color.rgb * u_intensity;\n" +
                            "    gl_FragColor = vec4(color.rgb + glow, color.a);\n" +
                            "}";

            ShaderProgram.pedantic = false;
            glowShader = new ShaderProgram(vertexShader, glowFragmentShader);

            // Check if shader compiled successfully
            if (!glowShader.isCompiled()) {
                Gdx.app.error("MerchantUI", "Glow shader failed to compile: " + glowShader.getLog());
                glowShader = null;
            }
        } catch (Exception e) {
            Gdx.app.error("MerchantUI", "Failed to initialize shaders: " + e.getMessage());
            glowShader = null;
        }
    }

    Character character;

    public MerchantUI(GameController gameController) {
        this.gameController = gameController;
        this.layout = new GlyphLayout();
        this.shapeRenderer = new ShapeRenderer();
        this.character = gameController.getCharacter();
        // Initialize essential components
        initializeShaders();

        this.font = gameController.getCommonFont();
        this.titleFont = gameController.getBigCommonFont();
        this.smallFont = gameController.getRegularFont();

        initializeAnimationValues();
        loadTextures();
        setupLayout();
        loadMerchantItems();

        // Create projection matrix for UI rendering
        uiMatrix = new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private void initializeAnimationValues() {
        // Animation state tracking
        animatedValues = new HashMap<>();
        animatedValues.put("hoverAlpha", 0f);
        animatedValues.put("selectedPulse", 0f);
        animatedValues.put("tabTransition", 1.0f);
        animatedValues.put("hoverMerchantTab", 0f);
        animatedValues.put("hoverPlayerTab", 0f);
        lastAnimationTime = TimeUtils.millis();
    }


    Texture buttonTexture2;

    private void loadTextures() {
        try {
            // Create or load basic UI textures
            Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);

            // White pixel for basic shapes
            pixmap.setColor(Color.WHITE);
            pixmap.fill();
            buttonTexture = new Texture(pixmap);

            // Create slot texture
            Pixmap slotPixmap = new Pixmap(SLOT_SIZE, SLOT_SIZE, Pixmap.Format.RGBA8888);
            slotPixmap.setColor(0.2f, 0.2f, 0.3f, 0.8f);
            slotPixmap.fill();
            slotPixmap.setColor(0.5f, 0.5f, 0.6f, 1f);
            for (int i = 0; i < 3; i++) {
                slotPixmap.drawRectangle(i, i, SLOT_SIZE - i * 2, SLOT_SIZE - i * 2);
            }
            slotTexture = new Texture(slotPixmap);
            slotPixmap.dispose();

            // Create highlight texture
            Pixmap highlightPixmap = new Pixmap(SLOT_SIZE, SLOT_SIZE, Pixmap.Format.RGBA8888);
            highlightPixmap.setColor(0.6f, 0.9f, 1f, 0.7f);
            highlightPixmap.fill();
            highlightPixmap.setColor(0.7f, 1f, 1f, 1f);
            for (int i = 0; i < 2; i++) {
                highlightPixmap.drawRectangle(i, i, SLOT_SIZE - i * 2, SLOT_SIZE - i * 2);
            }
            highlightTexture = new Texture(highlightPixmap);
            highlightPixmap.dispose();

            // Create border texture (white pixel)
            borderTexture = new Texture(Gdx.files.internal("ui/panel-header-2.png"));
            buttonTexture2 = new Texture(Gdx.files.internal("ui/button.png"));
            backgroundTexture = new Texture(Gdx.files.internal("ui/panel-1.png"));

            // Load item textures
            preloadCommonTextures();

        } catch (Exception e) {
            Gdx.app.error("MerchantUI", "Failed to load textures", e);
        }
    }

    private void preloadCommonTextures() {
        List<Items> commonItems = ItemLoader.getAllItems();
        Map<String, Texture> textureMap = gameController.getAssetManager().loadAllItems(commonItems);
        this.itemTextures.putAll(textureMap);
    }

    private Texture getItemTexture(String name, String texturePath) {
        if (!itemTextures.containsKey(name)) {
            itemTextures.put(name, new Texture(Gdx.files.internal(texturePath)));
        }
        return itemTextures.get(name);
    }


    private void setupLayout() {
        // Calculate the main panel size and position
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        float panelWidth = Math.min(900f, screenWidth * 0.8f);
        float panelHeight = screenHeight * 0.75f;
        float panelX = (screenWidth - panelWidth) / 2;
        float panelY = (screenHeight - panelHeight) / 2;

        merchantBounds = new Rectangle(panelX, panelY, panelWidth, panelHeight);

        // Create item slots
        createSlots();

        // Setup buttons
        float buttonWidth = 150;
        float buttonHeight = 50;
        float buttonX = merchantBounds.x + merchantBounds.width - buttonWidth - PADDING * 2;
        float buttonY = merchantBounds.y + PADDING * 2 + 20;

        buyButton = new Rectangle(buttonX, buttonY, buttonWidth, buttonHeight);
        sellButton = new Rectangle(buttonX, buttonY, buttonWidth, buttonHeight);

        // Close button
        float closeSize = 30;
        closeButton = new Rectangle(
                merchantBounds.x + merchantBounds.width - closeSize - 10,
                merchantBounds.y + merchantBounds.height - closeSize - 10,
                closeSize, closeSize);

        // Tab buttons
        float tabWidth = 120;
        float tabHeight = 40;
        float tabY = merchantBounds.y + merchantBounds.height - tabHeight - 48;

        tabMerchantButton = new Rectangle(merchantBounds.x + PADDING * 3, tabY, tabWidth, tabHeight);
        tabPlayerButton = new Rectangle(merchantBounds.x + PADDING * 3 + tabWidth + PADDING, tabY, tabWidth, tabHeight);
    }

    private void createSlots() {
        itemSlots = new Rectangle[MAX_SLOTS];
        for (int i = 0; i < MAX_SLOTS; i++) {
            int row = i / SLOTS_PER_ROW;
            int col = i % SLOTS_PER_ROW;

            itemSlots[i] = new Rectangle(
                    merchantBounds.x + PADDING * 2 + (col * (SLOT_SIZE + PADDING)) + 50,
                    merchantBounds.y + merchantBounds.height - PADDING * 2 - 130 - SLOT_SIZE - (row * (SLOT_SIZE + PADDING + 16)),
                    SLOT_SIZE,
                    SLOT_SIZE
            );
        }

        playerItemSlots = new Rectangle[MAX_SLOTS];
        for (int i = 0; i < MAX_SLOTS; i++) {
            playerItemSlots[i] = new Rectangle(itemSlots[i].x, itemSlots[i].y, SLOT_SIZE, SLOT_SIZE);
        }
    }

    private void loadMerchantItems() {
        try {
            merchantItems = ItemLoader.getAllItemsWithoutNA();
            updateDisplayItems();
        } catch (Exception e) {
            Gdx.app.error("MerchantUI", "Failed to load merchant items", e);
        }
    }

    private void updateDisplayItems() {
        // Convert player's map of items to a list for display
        displayItems.clear();
    }

    public void render(SpriteBatch batch) {
        if (!visible && !isOpening && !isClosing) return;

        // Update animations
        updateAnimations();

        // Store original batch state
        Matrix4 prevMatrix = batch.getProjectionMatrix().cpy();
        boolean batchWasDrawing = batch.isDrawing();
        if (batchWasDrawing) batch.end();

        // Render background blur effect
        renderBlurredBackground(batch);

        // Apply animation transforms based on open/close state
        float scale = calculateAnimationScale();
        float offsetY = (1.0f - scale) * 120;

        // Render the main UI components
        renderMainPanel(offsetY, scale);

        if (scale > 0.3f) { // Only render content when sufficiently visible
            renderHeader(batch);
            renderTabs(batch);
            renderItemGrid(batch);
            renderItemDetails(batch);
            renderActionButtons(batch);
            renderPriceInfo(batch);
            renderCloseButton(batch);
        }
        // Restore batch state
        if (batchWasDrawing) {
            batch.setProjectionMatrix(prevMatrix);
            batch.begin();
        }
    }

    private void updateAnimations() {
        float deltaTime = Gdx.graphics.getDeltaTime();
        float targetPulse = (float) Math.sin(animTime * 2.5f) * 0.5f + 0.5f;

        // Update animation time
        animTime += deltaTime;

        // Smoothly animate hover effects
        animatedValues.put("hoverMerchantTab", interpolateValue(
                animatedValues.get("hoverMerchantTab"),
                hoverMerchantTab ? 1f : 0f,
                deltaTime * 8f));

        animatedValues.put("hoverPlayerTab", interpolateValue(
                animatedValues.get("hoverPlayerTab"),
                hoverPlayerTab ? 1f : 0f,
                deltaTime * 8f));

        animatedValues.put("selectedPulse", interpolateValue(
                animatedValues.get("selectedPulse"),
                targetPulse,
                deltaTime * 5f));

        // Handle opening/closing animations
        if (isOpening) {
            openProgress += deltaTime / animationSpeed;
            if (openProgress >= 1.0f) {
                openProgress = 1.0f;
                isOpening = false;
            }
        } else if (isClosing) {
            openProgress -= deltaTime / animationSpeed;
            if (openProgress <= 0) {
                openProgress = 0;
                isClosing = false;
                visible = false;
            }
        }
    }

    private float interpolateValue(float current, float target, float factor) {
        float diff = target - current;
        if (Math.abs(diff) < 0.01f) return target;
        return current + diff * Math.min(1.0f, factor);
    }

    private float calculateAnimationScale() {
        // Use smooth interpolation for the scaling animation
        return Interpolation.smoother.apply(openProgress);
    }

    private void renderBlurredBackground(SpriteBatch batch) {
        if (frameBuffer == null || blurShader == null) return;

        try {
            // Capture the game background
            frameBuffer.begin();
            Gdx.gl.glClearColor(0, 0, 0, 0);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

            // Here we would draw whatever is behind the merchant UI
            // For a full screen blur effect
            batch.begin();
            batch.setShader(null);
            batch.setColor(Color.WHITE);
            batch.draw(backgroundTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            batch.end();

            frameBuffer.end();

            // Apply blur effect with modern stylized blur
            batch.begin();
            batch.setShader(blurShader);
            blurShader.setUniformf("u_radius", 3.5f);
            blurShader.setUniformf("u_resolution", Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

            // Add subtle color tint to the blur for atmospheric effect
            batch.setColor(0.8f, 0.9f, 1f, 0.8f);

            // Draw the blurred background only behind our UI
            TextureRegion blurRegion = new TextureRegion(frameBuffer.getColorBufferTexture());
            blurRegion.flip(false, true);

            float padding = 20f;
            batch.draw(blurRegion,
                    merchantBounds.x - padding, merchantBounds.y - padding,
                    merchantBounds.width + padding * 2, merchantBounds.height + padding * 2);

            batch.setShader(null);
            batch.end();
        } catch (Exception e) {
            Gdx.app.error("MerchantUI", "Error applying blur effect", e);
        }
    }

    private void renderMainPanel(float offsetY, float scale) {
        float panelX = merchantBounds.x;
        float panelY = merchantBounds.y + offsetY;
        float panelWidth = merchantBounds.width * scale;
        float panelHeight = merchantBounds.height * scale;

        // Draw outer glow effect
        Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
        shapeRenderer.setProjectionMatrix(uiMatrix);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Outer glow with pulsating effect
        float pulseIntensity = 0.7f + 0.3f * (float) Math.sin(animTime * 2f);
        Color glow = new Color(glowColor);
        glow.a *= pulseIntensity;
        shapeRenderer.setColor(glow);
        drawRoundedRect(panelX - 5, panelY - 5, panelWidth + 10, panelHeight + 10, CORNER_RADIUS + 2);

        // Main background with subtle gradient
        shapeRenderer.setColor(bgColor);
        drawRoundedRect(panelX, panelY, panelWidth, panelHeight, CORNER_RADIUS);

        // Top accent bar
        shapeRenderer.setColor(accentColor);
        drawRoundedRect(panelX, panelY + panelHeight - 48, panelWidth, 48,
                new float[]{CORNER_RADIUS, CORNER_RADIUS, 0, 0});

        // Bottom accent trim
        shapeRenderer.setColor(accentColor);
        shapeRenderer.rectLine(panelX + 20, panelY + 25, panelX + panelWidth - 20, panelY + 25, 2);

        shapeRenderer.end();
        Gdx.gl.glDisable(Gdx.gl.GL_BLEND);
    }

    // Helper method to draw rounded rectangles
    private void drawRoundedRect(float x, float y, float width, float height, float radius) {
        drawRoundedRect(x, y, width, height, new float[]{radius, radius, radius, radius});
    }

    private void drawRoundedRect(float x, float y, float width, float height, float[] radii) {
        // Top-left corner
        if (radii[0] > 0)
            shapeRenderer.arc(x + radii[0], y + height - radii[0], radii[0], 90, 90);

        // Top-right corner
        if (radii[1] > 0)
            shapeRenderer.arc(x + width - radii[1], y + height - radii[1], radii[1], 0, 90);

        // Bottom-right corner
        if (radii[2] > 0)
            shapeRenderer.arc(x + width - radii[2], y + radii[2], radii[2], 270, 90);

        // Bottom-left corner
        if (radii[3] > 0)
            shapeRenderer.arc(x + radii[3], y + radii[3], radii[3], 180, 90);

        // Rectangles to connect the corners
        shapeRenderer.rect(x + radii[0], y + height - radii[0], width - radii[0] - radii[1], radii[0]); // Top
        shapeRenderer.rect(x + radii[3], y, width - radii[3] - radii[2], radii[3]); // Bottom
        shapeRenderer.rect(x, y + radii[3], radii[3], height - radii[3] - radii[0]); // Left
        shapeRenderer.rect(x + width - radii[1], y + radii[2], radii[1], height - radii[1] - radii[2]); // Right

        // Central rectangle
        shapeRenderer.rect(x + radii[3], y + radii[3], width - radii[3] - radii[2], height - radii[0] - radii[3]);
    }

    private void renderHeader(SpriteBatch batch) {
        batch.begin();
        batch.setProjectionMatrix(uiMatrix);

        // Title
        titleFont.setColor(titleColor);
        String title = "Merchant";
        layout.setText(titleFont, title);
        float titleX = merchantBounds.x + 30;
        float titleY = merchantBounds.y + merchantBounds.height - 20;

        // Draw title with shadow
        titleFont.setColor(0, 0, 0, 0.5f);
        titleFont.draw(batch, title, titleX + 2, titleY - 2);
        titleFont.setColor(titleColor);
        titleFont.draw(batch, title, titleX, titleY);

        // Draw player's gold
        Character character = gameController.getCharacter();
        String goldText = "Your Gold: " + character.getScore();
        layout.setText(titleFont, goldText);
        float goldX = merchantBounds.x + merchantBounds.width - layout.width - 70;


        titleFont.setColor(Color.GOLD);
        titleFont.draw(batch, goldText, goldX, titleY - 50);

        batch.end();
    }

    private void renderTabs(SpriteBatch batch) {
        batch.begin();
        batch.setProjectionMatrix(uiMatrix);

        // Animation for tab transition

        // Tab positions
        float tabWidth = tabMerchantButton.width;
        float tabHeight = tabMerchantButton.height;
        float merchTabX = tabMerchantButton.x;
        float playerTabX = tabPlayerButton.x;
        float tabY = tabMerchantButton.y;

        // Draw background panels for tabs
        batch.setColor(showingMerchantItems ? activeTabColor : inactiveTabColor);
        batch.draw(buttonTexture, merchTabX, tabY, tabWidth, tabHeight);

        batch.setColor(showingMerchantItems ? inactiveTabColor : activeTabColor);
        batch.draw(buttonTexture, playerTabX, tabY, tabWidth, tabHeight);

        // Draw tab text
        titleFont.setColor(showingMerchantItems ? Color.WHITE : Color.LIGHT_GRAY);
        layout.setText(titleFont, "Buy");
        titleFont.draw(batch, "Buy",
                merchTabX + (tabWidth - layout.width) / 2,
                tabY + (tabHeight + layout.height) / 2);

        titleFont.setColor(showingMerchantItems ? Color.LIGHT_GRAY : Color.WHITE);
        layout.setText(titleFont, "Sell");
        titleFont.draw(batch, "Sell",
                playerTabX + (tabWidth - layout.width) / 2,
                tabY + (tabHeight + layout.height) / 2);

        // Draw indicator for active tab
        float indicatorWidth = 80;
        float indicatorHeight = 2;
        float indicatorX = showingMerchantItems ?
                merchTabX + (tabWidth - indicatorWidth) / 2 :
                playerTabX + (tabWidth - indicatorWidth) / 2;

        batch.setColor(highlightColor);
        batch.draw(buttonTexture, indicatorX, tabY, indicatorWidth, indicatorHeight);

        batch.end();
    }

    private void renderItemGrid(SpriteBatch batch) {
        List<Items> currentItems = showingMerchantItems ? merchantItems : displayItems;
        Rectangle[] currentSlots = showingMerchantItems ? itemSlots : playerItemSlots;

        batch.begin();
        batch.setProjectionMatrix(uiMatrix);

        // Calculate pulse animation for selected items
        float pulse = animatedValues.get("selectedPulse");

        for (int i = 0; i < MAX_SLOTS; i++) {
            Rectangle slot = currentSlots[i];
            boolean hasItem = i < currentItems.size();
            boolean isSelected = i == selectedItemIndex;

            if (isSelected) {
                // Draw selection glow with animated pulse
                batch.setShader(glowShader);
                glowShader.setUniformf("u_intensity", 0.6f + 0.4f * pulse);
                batch.setColor(highlightColor);

                // Draw expanding selection border
                float expansion = 4f * pulse;
                batch.draw(highlightTexture,
                        slot.x - expansion, slot.y - expansion,
                        slot.width + expansion * 2, slot.height + expansion * 2);

                batch.setShader(null);
            }

            // Draw slot with different appearance based on state
            batch.setColor(hasItem ? Color.WHITE : new Color(0.5f, 0.5f, 0.6f, 0.4f));
            batch.draw(slotTexture, slot.x, slot.y, slot.width, slot.height);

            if (hasItem) {
                Items item = currentItems.get(i);
                renderItemInSlot(batch, item, slot, i, isSelected, pulse);
            }
        }

        batch.end();
    }

    private void renderItemInSlot(SpriteBatch batch, Items item, Rectangle slot, int index, boolean isSelected, float pulse) {
        // Get item texture or use default
        Texture itemTexture = getItemTexture(item.getItemName(), item.getTexturePath());

        // Draw item in slot with slight scaling for selected items
        float scale = isSelected ? 1.0f + 0.1f * pulse : 1.0f;
        float size = SLOT_SIZE * 0.7f * scale;
        float x = slot.x + (SLOT_SIZE - size) / 2;
        float y = slot.y + (SLOT_SIZE - size) / 2;

        batch.setColor(Color.WHITE);
        batch.draw(itemTexture, x, y, size, size);

        // Draw item count if stacked
        if (!showingMerchantItems) {
            int count = playerItems.getOrDefault(item, 1);
            if (count > 1) {
                smallFont.setColor(Color.WHITE);
                smallFont.draw(batch, "x" + count, slot.x + SLOT_SIZE - 20, slot.y + 15);
            }
        }
    }

    private void renderItemDetails(SpriteBatch batch) {
        if (selectedItemIndex < 0) return;

        List<Items> currentItems = showingMerchantItems ? merchantItems : displayItems;
        if (selectedItemIndex >= currentItems.size()) return;

        Items item = currentItems.get(selectedItemIndex);

        batch.begin();
        batch.setProjectionMatrix(uiMatrix);


        // Detail panel positioning
        float detailX = merchantBounds.x + merchantBounds.width * 0.58f;
        float detailY = merchantBounds.y + merchantBounds.height * 0.6f + 45;
        float detailWidth = merchantBounds.width * 0.38f;

        // Semi-transparent panel background with glow
        batch.draw(borderTexture,
                detailX - 10, merchantBounds.y + 130,
                detailWidth + 20, merchantBounds.height * 0.5f);

        // Stylish header with item name
        titleFont.setColor(highlightColor);
        layout.setText(titleFont, item.getItemName());
        titleFont.draw(batch, item.getItemName(), detailX+10, detailY);


        // Render item properties with style
        float textY = detailY - 30;
        font.setColor(new Color(0.9f, 0.9f, 0.7f, 1f));
        font.draw(batch, "Effect:", detailX, textY);
        font.draw(batch, item.getItemEffect().toUpperCase(), detailX + 60, textY);

        // Description with word wrapping and styled text


        // Item effects with custom styling
        textY -= 30;
        font.setColor(Color.WHITE);
        font.draw(batch, item.getItemDescription(),
                detailX, textY,
                detailWidth, Align.left, true);

        font.setColor(new Color(0.7f, 1f, 0.8f, 1f));

        batch.end();
    }

    private void renderActionButtons(SpriteBatch batch) {
        List<Items> currentItems = showingMerchantItems ? merchantItems : displayItems;
        if (selectedItemIndex < 0 || selectedItemIndex >= currentItems.size()) return;

        // Button positioning
        Rectangle actionButton = showingMerchantItems ? buyButton : sellButton;
        boolean isHovered = showingMerchantItems ? hoverBuyButton : hoverSellButton;
        String buttonText = showingMerchantItems ? "BUY" : "SELL";

        // Get affordable status for coloring
        Character character = gameController.getCharacter();
        Items selectedItem = currentItems.get(selectedItemIndex);
        boolean canAfford = true;

        if (showingMerchantItems) {
            try {
                int price = selectedItem.getItemPrice();
                canAfford = character.getScore() >= price;
            } catch (NumberFormatException e) {
                // Default to affordable if price parsing fails
            }
        }

        // Start rendering
        batch.begin();
        batch.setProjectionMatrix(uiMatrix);

        // Draw button background with glow effect for hover
        batch.setShader(glowShader);
        float glowIntensity = isHovered ? 0.7f : 0.3f;
        glowShader.setUniformf("u_intensity", glowIntensity);

        // Button color based on affordability
        Color buttonBaseColor = showingMerchantItems && !canAfford ?
                unaffordableColor : buttonColor;

        // Apply hover effect
        Color finalColor = isHovered ?
                new Color(buttonBaseColor).lerp(highlightColor, 0.3f) : buttonBaseColor;

        batch.setColor(finalColor);
        batch.draw(buttonTexture2, actionButton.x, actionButton.y,
                actionButton.width, actionButton.height);

        batch.setShader(null);

        // Draw button text with shadow for depth
        titleFont.setColor(0, 0, 0, 0.5f);
        layout.setText(titleFont, buttonText);
        float textX = actionButton.x + (actionButton.width - layout.width) / 2;
        float textY = actionButton.y + (actionButton.height + layout.height) / 2;

        titleFont.draw(batch, buttonText, textX + 2, textY - 2);

        titleFont.setColor(Color.WHITE);
        titleFont.draw(batch, buttonText, textX, textY);

        // Render close button

        batch.end();
    }

    private void renderCloseButton(SpriteBatch batch) {
        batch.begin();

        // Draw close button background
        batch.setColor(hoverCloseButton ? closeButtonHoverColor : closeButtonColor);
        batch.draw(buttonTexture2, closeButton.x, closeButton.y, closeButton.width, closeButton.height);
        float textX = closeButton.x + (closeButton.width - layout.width) / 2f;
        float textY = closeButton.y + (closeButton.height + layout.height) / 2f;
        font.setColor(Color.BLACK); // Change to desired font color
        font.draw(batch, "X", textX + 10, textY);

        batch.end();
    }


    private void renderPriceInfo(SpriteBatch batch) {
        if (selectedItemIndex < 0) return;

        List<Items> currentItems = showingMerchantItems ? merchantItems : displayItems;
        if (selectedItemIndex >= currentItems.size()) return;

        Items item = currentItems.get(selectedItemIndex);
        int price = item.getItemPrice();

        batch.begin();
        batch.setProjectionMatrix(uiMatrix);

        // Draw price
        String priceText = showingMerchantItems ?
                "Price: " + price + " Gold" :
                "Value: " + (price / 2) + " Gold";

        float priceX = merchantBounds.x + 80;
        float priceY = merchantBounds.y + 60;

        font.setColor(Color.GOLD);
        font.draw(batch, priceText, priceX, priceY);

        batch.end();
    }

    // Public methods for interaction

    public void show() {
        if (!visible && !isOpening) {
            visible = true;
            isOpening = true;
            isClosing = false;
            openProgress = 0;

            // Reset selection state
            selectedItemIndex = -1;

            // Update player items
            updatePlayerItems();
        }
    }

    public void hide() {
        if (visible && !isClosing) {
            isClosing = true;
            isOpening = false;
        }
    }

    private void updatePlayerItems() {
        Character character = gameController.getCharacter();


        playerItems.clear();
        displayItems.clear();

        Map<String, Integer> characterItems = character.getItems();
        for (Map.Entry<String, Integer> entry : characterItems.entrySet()) {
            Items item = ItemLoader.getItemByName(entry.getKey());
            if (item.getItemEffect().equals("N/A")) {
                continue; // Skip items with "N/A" effect
            }
            if (item != null) {
                playerItems.put(item, entry.getValue());
                displayItems.add(item);
            }
        }
    }

    public boolean handleClick(float x, float y) {
        if (!visible || isClosing) return false;

        // Convert y coordinate if needed
        y = Gdx.graphics.getHeight() - y;

        // Check if close button was clicked
        if (closeButton.contains(x, y)) {
            hide();
            return true;
        }

        // Check tab clicks
        if (tabMerchantButton.contains(x, y) && !showingMerchantItems) {
            showingMerchantItems = true;
            selectedItemIndex = -1;
            return true;
        } else if (tabPlayerButton.contains(x, y) && showingMerchantItems) {
            showingMerchantItems = false;
            selectedItemIndex = -1;
            return true;
        }

        // Check item slots
        Rectangle[] currentSlots = showingMerchantItems ? itemSlots : playerItemSlots;
        List<Items> currentItems = showingMerchantItems ? merchantItems : displayItems;

        for (int i = 0; i < currentSlots.length; i++) {
            if (i < currentItems.size() && currentSlots[i].contains(x, y)) {
                if (selectedItemIndex != i) {
                    selectedItemIndex = i;
                }
                return true;
            }
        }

        // Check action button click
        if (selectedItemIndex >= 0) {
            Rectangle actionButton = showingMerchantItems ? buyButton : sellButton;
            if (actionButton.contains(x, y)) {
                if (showingMerchantItems) {
                    buySelectedItem();
                } else {
                    sellSelectedItem();
                }
                return true;
            }
        }

        return false;
    }

    public boolean handleHover(float x, float y) {
        // Convert y coordinate
        y = Gdx.graphics.getHeight() - y;

        // Reset hover states
        boolean hoverChanged = false;

        // Check close button hover
        boolean wasHoveringClose = hoverCloseButton;
        hoverCloseButton = closeButton.contains(x, y);
        if (wasHoveringClose != hoverCloseButton) hoverChanged = true;

        // Check tab hovers
        boolean wasHoveringMerchant = hoverMerchantTab;
        hoverMerchantTab = tabMerchantButton.contains(x, y);
        if (wasHoveringMerchant != hoverMerchantTab) hoverChanged = true;

        boolean wasHoveringPlayer = hoverPlayerTab;
        hoverPlayerTab = tabPlayerButton.contains(x, y);
        if (wasHoveringPlayer != hoverPlayerTab) hoverChanged = true;

        // Check action button hover
        boolean wasHoveringBuy = hoverBuyButton;
        hoverBuyButton = showingMerchantItems && selectedItemIndex >= 0 && buyButton.contains(x, y);
        if (wasHoveringBuy != hoverBuyButton) hoverChanged = true;

        boolean wasHoveringSell = hoverSellButton;
        hoverSellButton = !showingMerchantItems && selectedItemIndex >= 0 && sellButton.contains(x, y);
        if (wasHoveringSell != hoverSellButton) hoverChanged = true;


        return hoverChanged;
    }

    private void buySelectedItem() {
        if (selectedItemIndex < 0 || selectedItemIndex >= merchantItems.size()) return;

        Items item = merchantItems.get(selectedItemIndex);
        // Check if player can afford the item
        if (character.getScore() >= item.getItemPrice()) {
            // Deduct gold
            character.setScore(character.getScore() - item.getItemPrice());

            // Add item to inventory
            character.addItem(item, 1);

            // Update player items display
            updatePlayerItems();

            // Play purchase sound
        }
    }

    private void sellSelectedItem() {
        if (selectedItemIndex < 0 || selectedItemIndex >= displayItems.size()) return;
        Items item = displayItems.get(selectedItemIndex);

        // Remove one instance of the item
        character.removeItem(item.getItemName(), 1);

        // Add half the item's value to player's gold
        int sellValue = item.getItemPrice() / 2;
        character.setScore(character.getScore() + sellValue);

        // Update player items
        updatePlayerItems();

        // Reset selection if no more of this item
        if (selectedItemIndex >= displayItems.size()) {
            selectedItemIndex = -1;
        }

        // Play sell sound
    }

    private void playSound(Sound sound) {
        if (sound != null) {
            sound.play(0.5f);
        }
    }

    public boolean isVisible() {
        return visible || isOpening;
    }

    public void dispose() {

        if (backgroundTexture != null) backgroundTexture.dispose();
        if (buttonTexture != null) buttonTexture.dispose();
        if (buttonTexture2 != null) buttonTexture2.dispose();
        if (slotTexture != null) slotTexture.dispose();
        if (highlightTexture != null) highlightTexture.dispose();
        if (borderTexture != null && borderTexture != buttonTexture) borderTexture.dispose();

        if (shapeRenderer != null) shapeRenderer.dispose();
        if (frameBuffer != null) frameBuffer.dispose();
        if (blurShader != null) blurShader.dispose();
        if (glowShader != null) glowShader.dispose();

    }
}