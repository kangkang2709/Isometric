package ctu.game.isometric.controller.gameplay;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.Viewport;
import ctu.game.isometric.animation.CardAnimationManager;
import ctu.game.isometric.model.entity.Enemy;
import ctu.game.isometric.model.game.GameState;
import ctu.game.isometric.model.game.Items;
import ctu.game.isometric.model.game.LetterGrid;
import ctu.game.isometric.model.game.Reward;
import ctu.game.isometric.util.ItemLoader;
import ctu.game.isometric.util.RewardLoader;
import ctu.game.isometric.view.scene.FloatingText;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static ctu.game.isometric.util.FontGenerator.generateVietNameseFont;

public class GameplayRenderer {
    // Constants
    private static final String VOWELS = "AEIOU";
    private static final float COMBAT_TIME_LIMIT = 300f;
    private static final float MARGIN = 20;

    // Core components
    private final GameplayController controller;
    private final LetterGrid letterGrid;
    private final Viewport viewport;

    // UI components
    private BitmapFont titleFont, regularFont;
    private GlyphLayout layout;
    private Texture whiteTexture;
    private Map<String, Texture> textureCache = new HashMap<>();

    // UI textures
    private Texture buttonTexture, buttonSelectedTexture, cellTexture, vowelCellTexture,
            wordCellTexture, disabledCellTexture;

    // Combat log scroll
    private float combatLogScrollOffset = 0;
    private float maxCombatLogScrollOffset = 0;
    private boolean isCombatLogScrollable = false;

    // Effect animation
    private Map<String, Float> effectAlphaPhase = new HashMap<>();


    private float hpBarAnimationTime = 0f;
    private float mpBarAnimationTime = 0f;
    private float turnTransitionTime = 0f;
    private boolean previousTurnWasPlayer = true;
    private Texture gradientTexture;
    private Texture buttonTextureFancy;
    private Color hpColor = new Color(1f, 0.2f, 0.2f, 1f);
    private Color mpColor = new Color(0.2f, 0.5f, 1f, 1f);

    private Animation<TextureRegion>[] actionAnimations; // def, heal, attack, skill
    private float stateTime = 0;
    private boolean isPlayingAnimation = false;
    private int currentAnimationIndex = -1;
    private float animationScale = 1.5f; // Scale for rendering animations

    public GameplayRenderer(GameplayController controller, LetterGrid letterGrid, Viewport viewport, Animation<TextureRegion>[] actionAnimations) {
        this.controller = controller;
        this.letterGrid = letterGrid;
        this.viewport = viewport;
        initializeUI();
        this.actionAnimations = actionAnimations;
    }


    boolean isPlayer = false;

    public void startActionAnimation(int animationIndex, boolean isPlayer) {
        this.isPlayer = isPlayer;
        currentAnimationIndex = animationIndex;
        isPlayingAnimation = true;
        stateTime = 0;
    }

    public void updateAnimations(float delta) {
        if (isPlayingAnimation && currentAnimationIndex >= 0 && currentAnimationIndex < actionAnimations.length) {
            stateTime += delta;

            // Check if animation is complete
            if (actionAnimations[currentAnimationIndex].isAnimationFinished(stateTime)) {
                isPlayingAnimation = false;
            }
        }
    }


    public void renderActionAnimation(SpriteBatch batch) {
        if (!isPlayingAnimation || currentAnimationIndex < 0 || currentAnimationIndex >= actionAnimations.length) {
            return;
        }

        // Get current frame
        TextureRegion currentFrame = actionAnimations[currentAnimationIndex].getKeyFrame(stateTime, false);
        float x, y;

        if (isPlayer) {
            x = 239;
            y = 200;
            batch.draw(currentFrame, x, y, 150 * animationScale, 150 * animationScale);

        } else {
            x = 770;
            y = 350;
            batch.draw(currentFrame, x, y, 150 * animationScale, 150 * animationScale);

        }
        // Draw the animation frame
    }

    private void initializeUI() {
        titleFont = generateVietNameseFont("Tektur-Bold.ttf", 20);
        regularFont = generateVietNameseFont("Tektur-Bold.ttf", 14);
        layout = new GlyphLayout();
        createWhiteTexture();
        loadUITextures();
        createSpecialCellTextures();

        createGradientTexture();
        if (buttonTextureFancy == null) buttonTextureFancy = createFancyButtonTexture();
    }

    private Texture createFancyButtonTexture() {
        Pixmap pixmap = new Pixmap(200, 50, Pixmap.Format.RGBA8888);
        // Base color
        pixmap.setColor(0.2f, 0.2f, 0.4f, 1f);
        pixmap.fill();

        // Top gradient
        for (int y = 0; y < 15; y++) {
            float alpha = 0.8f * (1 - y / 15f);
            pixmap.setColor(1f, 1f, 1f, alpha);
            pixmap.drawLine(0, y, 199, y);
        }

        // Bottom shadow
        pixmap.setColor(0.1f, 0.1f, 0.2f, 1f);
        pixmap.fillRectangle(0, 45, 200, 5);

        // Border
        pixmap.setColor(0.5f, 0.5f, 0.8f, 1f);
        pixmap.drawRectangle(0, 0, 200, 50);

        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    private Texture createPanelDecorationTexture() {
        Pixmap pixmap = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
        pixmap.setColor(0.7f, 0.7f, 1.0f, 0.8f);
        // Draw a corner decoration
        for (int i = 0; i < 15; i++) {
            pixmap.drawLine(0, i, i, 0);
        }
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    private void createGradientTexture() {
        Pixmap pixmap = new Pixmap(256, 1, Pixmap.Format.RGBA8888);
        for (int x = 0; x < 256; x++) {
            float factor = x / 255f;
            pixmap.setColor(factor, factor * 0.7f, factor * 0.3f, 1f);
            pixmap.drawPixel(x, 0);
        }
        gradientTexture = new Texture(pixmap);
        pixmap.dispose();
    }

    private void createWhiteTexture() {
        if (whiteTexture != null) whiteTexture.dispose();
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        whiteTexture = new Texture(pixmap);
        pixmap.dispose();
    }

    private void loadUITextures() {
        try {
            if (buttonTexture == null) buttonTexture = createFallbackTexture(200, 50, Color.DARK_GRAY);
            if (buttonSelectedTexture == null) buttonSelectedTexture = createFallbackTexture(200, 50, Color.GRAY);
        } catch (Exception e) {
            buttonTexture = createFallbackTexture(200, 50, Color.DARK_GRAY);
            buttonSelectedTexture = createFallbackTexture(200, 50, Color.GRAY);
        }
    }

    private void renderFloatingTexts(SpriteBatch batch) {
        List<FloatingText> floatingTexts = controller.getFloatingTexts();
        if (floatingTexts != null && !floatingTexts.isEmpty()) {
            for (FloatingText floatingText : floatingTexts) {
                Color originalColor = titleFont.getColor().cpy();
                Color textColor = floatingText.getColor().cpy();
                textColor.a = floatingText.getAlpha();

                titleFont.setColor(textColor);
                titleFont.draw(batch, floatingText.getText(),
                        floatingText.getX(), floatingText.getY());

                // Restore original font color
                titleFont.setColor(originalColor);
            }
        }
    }

    public void loadPlayerTexture(String gender) {
        if (gender.equalsIgnoreCase("MALE"))
            playerTexture = getTexture("characters/male.png");
        else
            playerTexture = getTexture("characters/female.png");
    }

    private Texture createFallbackTexture(int width, int height, Color color) {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    private void createSpecialCellTextures() {
        if (cellTexture != null) cellTexture.dispose();
        if (vowelCellTexture != null) vowelCellTexture.dispose();
        if (disabledCellTexture != null) disabledCellTexture.dispose();

        cellTexture = createTintedTexture(Color.WHITE);
        vowelCellTexture = createTintedTexture(new Color(0.8f, 0.9f, 1.0f, 1.0f));
        disabledCellTexture = createTintedTexture(new Color(0.5f, 0.5f, 0.5f, 0.8f));
        wordCellTexture = createTintedTexture(new Color(1.0f, 0.8f, 0.8f, 1.0f));
    }

    private Texture createTintedTexture(Color tint) {
        Pixmap pixmap = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        pixmap.setColor(tint);
        pixmap.fill();
        pixmap.setColor(Color.BLACK);
        pixmap.drawRectangle(0, 0, 64, 64);
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }


    public void render(SpriteBatch batch) {
        batch.setProjectionMatrix(viewport.getCamera().combined);

        // Set background color
        batch.setColor(0.1f, 0.1f, 0.2f, 1);
        batch.draw(whiteTexture, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
        batch.setColor(Color.WHITE);

        if (controller.isCombatMode()) {
            renderCombatUI(batch);
            renderFloatingTexts(batch);
            renderActionAnimation(batch);

            CardAnimationManager cardAnimationManager = controller.getCardAnimationManager();
            if (cardAnimationManager != null) {
                cardAnimationManager.render(batch);
            }
        } else if (controller.isVictory()) {
            renderReward(batch);
        } else {
            renderGameOver(batch);
        }

        renderEnemyTooltip(batch);
    }

    public void renderGameOver(SpriteBatch batch) {
        if (!controller.isGameOver()) {
            controller.getGameController().setState(GameState.EXPLORING);
        } else {
            controller.getGameController().getGame().changeScreen("GAME_OVER");
        }
    }

    private void renderReward(SpriteBatch batch) {
        float panelWidth = 650, panelHeight = 450;
        float panelX = (viewport.getWorldWidth() - panelWidth) / 2;
        float panelY = (viewport.getWorldHeight() - panelHeight) / 2;

        // Enhanced background with gradient effect
        renderRewardBackground(batch, panelX, panelY, panelWidth, panelHeight);

        // Animated title with glow effect
        renderVictoryTitle(batch, panelX, panelY, panelWidth, panelHeight);

        Enemy enemy = controller.getEnemy();
        Reward reward = RewardLoader.getRewardById(enemy.getRewardID());
        Items item = ItemLoader.getItemById(reward.getItemID());

        if (item != null) {
            renderRewardItem(batch, item, reward, panelX, panelY, panelWidth, panelHeight);
        }

        // Enhanced continue button
        Rectangle continueButton = new Rectangle(viewport.getWorldWidth() / 2 - 120, panelY + 40, 240, 60);
        renderFFVIIStyleButton(batch, continueButton, "Tiếp tục", Color.CYAN);

        handleRewardInput(continueButton, item, reward);
    }

    private void renderRewardBackground(SpriteBatch batch, float panelX, float panelY, float panelWidth, float panelHeight) {
        // Semi-transparent dark background overlay
        batch.setColor(0, 0, 0, 0.7f);
        batch.draw(whiteTexture, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());

        // Main panel with gradient effect
        batch.setColor(0.15f, 0.2f, 0.35f, 0.95f);
        batch.draw(whiteTexture, panelX, panelY, panelWidth, panelHeight);

        // Subtle gradient overlay
        if (gradientTexture != null) {
            batch.setColor(1f, 1f, 1f, 0.1f);
            batch.draw(gradientTexture, panelX, panelY, panelWidth, panelHeight);
        }

        // Glowing border with pulsing effect
        float pulseIntensity = 0.6f + 0.4f * MathUtils.sin(hpBarAnimationTime * 2f);
        batch.setColor(0.3f, 0.7f, 1f, pulseIntensity);
        drawEnhancedBorder(batch, panelX, panelY, panelWidth, panelHeight, 4);

        // Corner decorations
        renderCornerDecorations(batch, panelX, panelY, panelWidth, panelHeight);
    }

    private void renderVictoryTitle(SpriteBatch batch, float panelX, float panelY, float panelWidth, float panelHeight) {
        float titleY = panelY + panelHeight - 60;

        // Title shadow for depth
        titleFont.setColor(0.2f, 0.2f, 0.2f, 0.8f);
        drawCenteredText(batch, titleFont, "CHIẾN THẮNG!", panelX + panelWidth / 2 + 3, titleY - 3, titleFont.getColor());

        // Animated title with golden glow
        float goldIntensity = 0.8f + 0.2f * MathUtils.sin(hpBarAnimationTime * 3f);
        Color goldColor = new Color(1f, 0.8f + 0.2f * goldIntensity, 0.2f, 1f);
        drawCenteredText(batch, titleFont, "CHIẾN THẮNG!", panelX + panelWidth / 2, titleY, goldColor);

        // Subtitle
        regularFont.setColor(0.8f, 0.8f, 1f, 0.9f);
        drawCenteredText(batch, regularFont, "Bạn đã nhận được phần thưởng!",
                panelX + panelWidth / 2, titleY - 35, regularFont.getColor());
    }

    private void renderRewardItem(SpriteBatch batch, Items item, Reward reward,
                                  float panelX, float panelY, float panelWidth, float panelHeight) {
        float contentY = panelY + panelHeight / 2;

        // Item showcase panel
        float itemPanelX = panelX + 40;
        float itemPanelY = contentY - 80;
        float itemPanelWidth = panelWidth - 80;
        float itemPanelHeight = 120;

        // Item panel background
        batch.setColor(0.1f, 0.15f, 0.25f, 0.8f);
        batch.draw(whiteTexture, itemPanelX, itemPanelY, itemPanelWidth, itemPanelHeight);

        // Item panel border
        batch.setColor(0.4f, 0.6f, 0.8f, 0.7f);
        drawBorder(batch, itemPanelX, itemPanelY, itemPanelWidth, itemPanelHeight, 2);

        // Item icon with glow effect
        Texture itemTexture = getTexture(item.getTexturePath());
        if (itemTexture != null) {
            float iconSize = 80;
            float iconX = itemPanelX + 20;
            float iconY = itemPanelY + (itemPanelHeight - iconSize) / 2;
            // Main icon
            batch.setColor(Color.WHITE);
            batch.draw(itemTexture, iconX, iconY, iconSize, iconSize);
        }

        // Item information
        float textX = itemPanelX + 120;
        float textStartY = itemPanelY + itemPanelHeight - 20;
// Item name with rarity coloring and proper positioning
        Color rarityColor = getRarityColor(item);
        titleFont.setColor(rarityColor);
        layout.setText(titleFont, item.getItemName());
        float nameWidth = layout.width;
        titleFont.draw(batch, item.getItemName(), textX + 30, textStartY);

// Amount with emphasis - positioned relative to item name
        regularFont.setColor(Color.YELLOW);
        String amountText = "x" + reward.getAmount();
        layout.setText(regularFont, amountText);
        float amountX = textX + 30 + nameWidth + 20; // 20px spacing after name
        regularFont.draw(batch, amountText, amountX, textStartY);

// Add visual separator if needed
        float separatorX = amountX + layout.width + 10;
        if (separatorX < textX + itemPanelWidth - 50) {
            regularFont.setColor(0.6f, 0.6f, 0.8f, 0.7f);
            regularFont.draw(batch, "•", separatorX, textStartY);
        }

// Item description with proper wrapping
        regularFont.setColor(0.9f, 0.9f, 1f, 1f);
        String description = item.getItemDescription();
        layout.setText(regularFont, description, regularFont.getColor(), itemPanelWidth - 160, -1, true);
        float descriptionY = textStartY - 35;
        regularFont.draw(batch, layout, textX + 30, descriptionY);
    }

    private void renderFFVIIStyleButton(SpriteBatch batch, Rectangle buttonRect, String text, Color accentColor) {
        Vector3 mousePos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(mousePos);
        boolean isHovered = buttonRect.contains(mousePos.x, mousePos.y);

        // Button base
        batch.setColor(0.2f, 0.25f, 0.4f, 0.9f);
        batch.draw(whiteTexture, buttonRect.x, buttonRect.y, buttonRect.width, buttonRect.height);

        // Hover effect
        if (isHovered) {
            batch.setColor(accentColor.r, accentColor.g, accentColor.b, 0.3f);
            batch.draw(whiteTexture, buttonRect.x, buttonRect.y, buttonRect.width, buttonRect.height);
        }

        // Border with glow
        float borderIntensity = isHovered ? 1f : 0.7f;
        batch.setColor(accentColor.r, accentColor.g, accentColor.b, borderIntensity);
        drawEnhancedBorder(batch, buttonRect.x, buttonRect.y, buttonRect.width, buttonRect.height, 3);

        // Button text with shadow
        layout.setText(titleFont, text);
        float textX = buttonRect.x + (buttonRect.width - layout.width) / 2;
        float textY = buttonRect.y + (buttonRect.height + layout.height) / 2;

        // Text shadow
        titleFont.setColor(0.1f, 0.1f, 0.1f, 0.8f);
        titleFont.draw(batch, text, textX + 2, textY - 2);

        // Main text
        titleFont.setColor(isHovered ? Color.WHITE : accentColor);
        titleFont.draw(batch, text, textX, textY);

        // Corner highlights for FF7 style
        if (isHovered) {
            batch.setColor(Color.WHITE);
            float cornerSize = 8;
            // Top-left corner
            batch.draw(whiteTexture, buttonRect.x, buttonRect.y + buttonRect.height - cornerSize, cornerSize, cornerSize);
            // Bottom-right corner
            batch.draw(whiteTexture, buttonRect.x + buttonRect.width - cornerSize, buttonRect.y, cornerSize, cornerSize);
        }
    }

    private void renderCornerDecorations(SpriteBatch batch, float panelX, float panelY, float panelWidth, float panelHeight) {
        batch.setColor(0.5f, 0.7f, 1f, 0.6f);
        float decorSize = 20;

        // Corner lines for modern UI feel
        // Top-left
        batch.draw(whiteTexture, panelX + 10, panelY + panelHeight - 10, decorSize, 2);
        batch.draw(whiteTexture, panelX + 10, panelY + panelHeight - decorSize - 10, 2, decorSize);

        // Top-right
        batch.draw(whiteTexture, panelX + panelWidth - decorSize - 10, panelY + panelHeight - 10, decorSize, 2);
        batch.draw(whiteTexture, panelX + panelWidth - 12, panelY + panelHeight - decorSize - 10, 2, decorSize);

        // Bottom corners
        batch.draw(whiteTexture, panelX + 10, panelY + 10, decorSize, 2);
        batch.draw(whiteTexture, panelX + 10, panelY + 10, 2, decorSize);

        batch.draw(whiteTexture, panelX + panelWidth - decorSize - 10, panelY + 10, decorSize, 2);
        batch.draw(whiteTexture, panelX + panelWidth - 12, panelY + 10, 2, decorSize);
    }

    private void drawEnhancedBorder(SpriteBatch batch, float x, float y, float width, float height, float thickness) {
        // Main border
        batch.draw(whiteTexture, x, y + height - thickness, width, thickness); // top
        batch.draw(whiteTexture, x, y, width, thickness); // bottom
        batch.draw(whiteTexture, x, y, thickness, height); // left
        batch.draw(whiteTexture, x + width - thickness, y, thickness, height); // right

        // Inner glow effect
        batch.setColor(batch.getColor().r, batch.getColor().g, batch.getColor().b, batch.getColor().a * 0.3f);
        float innerThickness = thickness - 1;
        if (innerThickness > 0) {
            batch.draw(whiteTexture, x + 1, y + height - innerThickness - 1, width - 2, innerThickness);
            batch.draw(whiteTexture, x + 1, y + 1, width - 2, innerThickness);
            batch.draw(whiteTexture, x + 1, y + 1, innerThickness, height - 2);
            batch.draw(whiteTexture, x + width - innerThickness - 1, y + 1, innerThickness, height - 2);
        }
    }

    private Color getRarityColor(Items item) {
        // You can implement rarity system based on item properties
        // For now, return different colors based on item type or value
        if (item.getItemName().toLowerCase().contains("legendary")) {
            return new Color(1f, 0.5f, 0f, 1f); // Orange
        } else if (item.getItemName().toLowerCase().contains("rare")) {
            return new Color(0.5f, 0.5f, 1f, 1f); // Blue
        } else if (item.getItemName().toLowerCase().contains("epic")) {
            return new Color(0.8f, 0.2f, 0.8f, 1f); // Purple
        }
        return new Color(1f, 1f, 1f, 1f); // White (common)
    }

    private void handleRewardInput(Rectangle continueButton, Items item, Reward reward) {
        if (Gdx.input.justTouched()) {
            Vector3 touchPos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            viewport.unproject(touchPos);

            if (continueButton.contains(touchPos.x, touchPos.y)) {
                // Add reward to player
                controller.getGameController().getCharacter().addItem(item, reward.getAmount());
                controller.getGameController().getCharacter().setHealth(controller.getPlayerHealth());
                controller.getGameController().getCharacter().setMana(controller.getPlayerMana());

                // Transition with delay for better UX
                Timer.schedule(new Timer.Task() {
                    @Override
                    public void run() {
                        handlePostRewardTransition();
                    }
                }, 0.5f);
            }
        }
    }

    private void handlePostRewardTransition() {
        if (controller.isEnemyBoss()) {
            controller.getGameController().returnToTowerAfterBoss(controller.getEnemyName());
        } else if (controller.isEnemyLord()) {
            controller.getGameController().returnToTowerAfterFinalBoss();
        }

        controller.getGameController().setState(GameState.EXPLORING);

        if (controller.getNewLevel() > controller.getCurrentLevel()) {
            controller.getGameController().showLevelUpNotification();
        }

        controller.cleanupCombatState();
    }

    private void renderCombatUI(SpriteBatch batch) {
        final float SCREEN_WIDTH = viewport.getWorldWidth();
        final float SCREEN_HEIGHT = viewport.getWorldHeight();
        final float BATTLEFIELD_HEIGHT = SCREEN_HEIGHT * 0.7f;
        final float UI_PANEL_HEIGHT = SCREEN_HEIGHT * 0.3f;

        drawBattlefield(batch, 0, UI_PANEL_HEIGHT, SCREEN_WIDTH, BATTLEFIELD_HEIGHT);

        final float PANEL_Y = 0;
        final float MAIN_PANEL_WIDTH = SCREEN_WIDTH * 0.6f;
        final float STATUS_PANEL_HEIGHT = UI_PANEL_HEIGHT * 0.6f;
        final float OPTION_PANEL_HEIGHT = UI_PANEL_HEIGHT * 0.4f;

        drawEnemyInfoPanel(batch, 300, 580, 400, 120);
        drawEnemyStatusEffects(batch, 335, 597);
        drawPlayerInfo(batch, 720, PANEL_Y + OPTION_PANEL_HEIGHT + STATUS_PANEL_HEIGHT + 36, 400, STATUS_PANEL_HEIGHT + 20);
//        drawMainActionPanel2(batch, MAIN_PANEL_WIDTH + MARGIN, PANEL_Y + MARGIN + 90, 500 - 2 * MARGIN, UI_PANEL_HEIGHT - 2 * MARGIN - 90);
        drawMainActionPanel(batch, MARGIN, PANEL_Y + MARGIN, MAIN_PANEL_WIDTH - MARGIN, UI_PANEL_HEIGHT - 2 * MARGIN);

        if (controller.getCurrentOverlay() == GameplayController.OverlayType.SPELL) {
            drawLetterGridOverlay(batch, SCREEN_WIDTH, SCREEN_HEIGHT);
        }

        if (controller.getCurrentOverlay() == GameplayController.OverlayType.INVENTORY) {
            drawInventoryOverlay(batch, SCREEN_WIDTH, SCREEN_HEIGHT);
        }

        drawActionButtons(batch, SCREEN_WIDTH, SCREEN_HEIGHT);
    }

    private void drawMainActionPanel(SpriteBatch batch, float x, float y, float width, float height) {
        Texture battlefieldBg = getTexture("ui/panel-1.png");
        if (battlefieldBg != null) {
            batch.setColor(Color.WHITE);
            batch.draw(battlefieldBg, x, y, width, height);
        }

        regularFont.setColor(Color.GRAY);
        regularFont.draw(batch, "Độ khó: " + controller.getDifficultyText(), x + 20, y + height - 15);

        String logText = controller.getCombatLogText();
        if (!logText.isEmpty()) {
            drawScrollableText(batch, regularFont, logText, x + 10, y + height - 50, width - 20, height - 80);
        }

        float timeLeft = COMBAT_TIME_LIMIT - controller.getCombatTimer();
        String timeText = String.format("TIME: %02d:%02d", (int) (timeLeft / 60), (int) (timeLeft % 60));
        regularFont.setColor(timeLeft < 60 ? Color.RED : Color.WHITE);
        regularFont.draw(batch, timeText, x + width - 120, y + height - 15);

        String turnText = controller.isPlayerTurn() ? "LƯỢT NGƯỜI CHƠI" : "LƯỢT KẺ ĐỊCH";
        Color turnColor = controller.isPlayerTurn() ? Color.GREEN : Color.RED;
        drawCenteredText(batch, titleFont, turnText, x + width / 2, y + height - 15, turnColor);
    }

    Texture playerTexture;

    private void drawBattlefield(SpriteBatch batch, float x, float y, float width, float height) {
        Texture battlefieldBg = getTexture("ui/battlefield_bg.png");
        if (battlefieldBg != null) {
            batch.setColor(Color.WHITE);
            batch.draw(battlefieldBg, x, y, width, height);
        } else {
            batch.setColor(0.3f, 0.5f, 0.8f, 1);
            batch.draw(whiteTexture, x, y, width, height * 0.5f);
            batch.setColor(0.2f, 0.7f, 0.3f, 1);
            batch.draw(whiteTexture, x, y, width, height * 0.5f);
        }

        drawPokemonStyleEnemySection(batch);


        if (playerTexture != null) {
            batch.setColor(Color.WHITE);
            batch.draw(playerTexture, 326, 266, 150, 200);
        }
    }

    private void drawPokemonStyleEnemySection(SpriteBatch batch) {
        Enemy enemy = controller.getEnemy();
        Texture enemyTexture = getTexture(enemy.getTexturePath());
        if (enemyTexture != null) {
            batch.setColor(Color.WHITE);
            batch.draw(enemyTexture, 830, 450, 150, 200);
        }
    }

    private void drawEnemyInfoPanel(SpriteBatch batch, float x, float y, float width, float height) {
        // Panel background with gradient
        batch.setColor(0.2f, 0.2f, 0.35f, 0.9f);
        batch.draw(whiteTexture, x, y, width, height);

        // Glowing border based on turn
        float glowIntensity = controller.isPlayerTurn() ? 0.5f : 0.8f + 0.2f * MathUtils.sin(hpBarAnimationTime * 3f);
        Color borderColor = new Color(0.8f, 0.1f, 0.1f, glowIntensity);
        batch.setColor(borderColor);
        drawBorder(batch, x, y, width, height, 3);

        // Enemy name with glowing effect for bosses
        titleFont.setColor(Color.WHITE);
        String enemyName = controller.getEnemyName();
        if (controller.isEnemyBoss()) {
            Color bossColor = new Color(1f, 0.6f + 0.4f * MathUtils.sin(hpBarAnimationTime * 2f), 0.2f, 1f);
            titleFont.setColor(bossColor);
        }
        titleFont.draw(batch, enemyName, x + 10, y + height - 15);

        // Level display
        String levelText = "Lv." + controller.getCurrentLevel();
        layout.setText(regularFont, levelText);
        regularFont.setColor(Color.YELLOW);
        regularFont.draw(batch, levelText, x + width - layout.width - 10, y + height - 15);

        // Enhanced HP bar
        drawModernHPBar(batch, controller.getEnemyHealth(), controller.getEnemyMaxHealth(),
                x + 40, y + 45, width - 60, 20);

        regularFont.setColor(Color.WHITE);
        regularFont.draw(batch, String.format("%.0f", controller.getEnemyHealth()) +
                "/" + (int) controller.getEnemyMaxHealth(), x + 80, y + 58);
    }

    private void renderEnemyTooltip(SpriteBatch batch) {
        if (!controller.isShowEnemyTooltip() || controller.getTooltipText().isEmpty()) return;

        layout.setText(regularFont, controller.getTooltipText());
        float tooltipWidth = layout.width + 20;
        float tooltipHeight = layout.height + 20;

        float finalX = Math.min(controller.getTooltipX(), viewport.getWorldWidth() - tooltipWidth);
        float finalY = Math.min(controller.getTooltipY(), viewport.getWorldHeight() - tooltipHeight);

        batch.setColor(0.1f, 0.1f, 0.1f, 0.9f);
        batch.draw(whiteTexture, finalX, finalY, tooltipWidth, tooltipHeight);

        batch.setColor(0.8f, 0.8f, 0.8f, 1);
        drawBorder(batch, finalX, finalY, tooltipWidth, tooltipHeight, 2);

        regularFont.setColor(Color.WHITE);
        regularFont.draw(batch, controller.getTooltipText(), finalX + 10, finalY + tooltipHeight - 10);
    }

    private void drawPlayerInfo(SpriteBatch batch, float x, float y, float width, float height) {
        // Panel background with gradient
        batch.setColor(0.15f, 0.2f, 0.35f, 0.9f);
        batch.draw(whiteTexture, x, y, width, height);


        // Glowing border based on turn
        float glowIntensity = controller.isPlayerTurn() ? 0.8f + 0.2f * MathUtils.sin(hpBarAnimationTime * 3f) : 0.5f;
        Color borderColor = new Color(0.2f, 0.5f, 0.9f, glowIntensity);
        batch.setColor(borderColor);
        drawBorder(batch, x, y, width, height, 3);

        // Player name
        titleFont.setColor(Color.WHITE);
        titleFont.draw(batch, controller.getPlayerName(), x + 10, y + height - 15);

        // Level display
        String levelText = "Lv." + controller.getCurrentLevel();
        layout.setText(regularFont, levelText);
        regularFont.setColor(Color.YELLOW);
        regularFont.draw(batch, levelText, x + width - layout.width - 10, y + height - 15);

        // Enhanced HP and MP bars
        drawModernHPBar(batch, controller.getPlayerHealth(), controller.getPlayerMaxHealth(),
                x + 40, y + 80, width - 60, 20);
        drawModernMPBar(batch, controller.getPlayerMana(), controller.getPlayerMaxMana(),
                x + 40, y + 50, width - 60, 20);

        // Health and mana values
        regularFont.setColor(Color.WHITE);
        regularFont.draw(batch, String.format("%.0f", controller.getPlayerHealth()) +
                "/" + (int) controller.getPlayerMaxHealth(), x + 80, y + 93);
        regularFont.draw(batch, (int) controller.getPlayerMana() +
                "/" + (int) controller.getPlayerMaxMana(), x + 80, y + 63);

        drawStatusEffects(batch, x + 30, y + height - 133);
    }

    private void drawConsoleStyleButton(SpriteBatch batch, Rectangle rect, String text, Color color) {
        boolean isSelected = false;

        // Check if this button represents the current overlay
        if ((controller.getCurrentOverlay() == GameplayController.OverlayType.SPELL && rect.equals(controller.getSpellButton())) ||
                (controller.getCurrentOverlay() == GameplayController.OverlayType.INVENTORY && rect.equals(controller.getItemButton()))) {
            isSelected = true;
        }

        // Base button texture
        batch.setColor(isSelected ? color : Color.WHITE);
        batch.draw(buttonTextureFancy, rect.x, rect.y, rect.width, rect.height);

        // Add glow effect for selected buttons
        if (isSelected) {
            batch.setColor(color.r, color.g, color.b, 0.5f + 0.3f * MathUtils.sin(hpBarAnimationTime * 4f));
            drawBorder(batch, rect.x, rect.y, rect.width, rect.height, 3);
        }

        // Draw text with shadow for depth
        layout.setText(regularFont, text);
        float textX = rect.x + (rect.width - layout.width) / 2;
        float textY = rect.y + (rect.height + layout.height) / 2;

        // Draw shadow
        regularFont.setColor(0.2f, 0.2f, 0.2f, 0.8f);
        regularFont.draw(batch, text, textX + 2, textY - 2);

        // Draw main text
        regularFont.setColor(isSelected ? Color.WHITE : color);
        regularFont.draw(batch, text, textX, textY);
    }

    private void drawActionButtons(SpriteBatch batch, float screenWidth, float screenHeight) {
        // Panel background for buttons
        Texture battlefieldBg = getTexture("ui/panel-header-2.png");
        if (battlefieldBg != null) {
            batch.setColor(Color.WHITE);
            batch.draw(battlefieldBg, 815, 10, 460, 196);
        }
        // Draw JRPG-style buttons
        drawConsoleStyleButton(batch, controller.getSpellButton(), "Kỹ năng", new Color(0.2f, 0.6f, 1f, 1f));
        drawConsoleStyleButton(batch, controller.getItemButton(), "Vật Phẩm", new Color(1f, 0.6f, 0.2f, 1f));
        drawConsoleStyleButton(batch, controller.getNormalAttackButton(), "Tấn Công", new Color(0.6f, 0.6f, 0.6f, 1f));
        drawConsoleStyleButton(batch, controller.getInfoButton(), "Thông tin", new Color(0.8f, 0.4f, 0.2f, 1f));
    }


    float panelWidth = 1280 * 0.8f;
    float panelHeight = 720 * 0.8f;
    float panelX = (1280 - panelWidth) / 2;
    float panelY = (720 - panelHeight) / 2;
    float buttonWidth = 120;
    float buttonHeight = 40;
    float buttonY = panelY + 40;
    Rectangle submitButtonRect = new Rectangle(panelX + panelWidth / 2 - buttonWidth - 10, buttonY, buttonWidth, buttonHeight);
    Rectangle clearButtonRect = new Rectangle(panelX + panelWidth / 2 + 10, buttonY, buttonWidth, buttonHeight);

    public Rectangle getSubmitButtonRect() {
        return submitButtonRect;
    }


    public Rectangle getClearButtonRect() {
        return clearButtonRect;
    }

    private void drawLetterGridOverlay(SpriteBatch batch, float screenWidth, float screenHeight) {
        batch.setColor(0, 0, 0, 0.7f);
        batch.draw(whiteTexture, 0, 0, screenWidth, screenHeight);


        drawCenteredText(batch, titleFont, "🔮 SPELL CASTING", panelX + panelWidth / 2, panelY + panelHeight - 30, Color.CYAN);

        String currentWord = letterGrid.getCurrentWord();
        drawCurrentWordCells(batch, currentWord, panelX + 50, panelY + panelHeight - 100, panelWidth - 100);

        float gridSize = Math.min(panelWidth * 0.6f, panelHeight * 0.6f);
        float gridX = panelX + (panelWidth - gridSize) / 2;
        float gridY = panelY + 120;

        drawLetterGrid(batch, gridX, gridY, gridSize);

        drawPokemonStyleButton(batch, submitButtonRect, "CAST", Color.GREEN);
        drawPokemonStyleButton(batch, clearButtonRect, "CLEAR", Color.RED);
        drawPokemonStyleButton(batch, controller.getCloseInventoryButton(), "x", Color.GRAY);
    }

    private void drawInventoryOverlay(SpriteBatch batch, float screenWidth, float screenHeight) {
        batch.setColor(0, 0, 0, 0.7f);
        batch.draw(whiteTexture, 0, 0, screenWidth, screenHeight);

        float panelWidth = screenWidth * 0.7f;
        float panelHeight = screenHeight * 0.7f - 80;
        float panelX = (screenWidth - panelWidth) / 2;
        float panelY = (screenHeight - panelHeight) / 2 + 70;

        batch.setColor(0.3f, 0.2f, 0.1f, 0.95f);
        batch.draw(whiteTexture, panelX, panelY, panelWidth, panelHeight);

        batch.setColor(0.8f, 0.6f, 0.4f, 1);
        drawBorder(batch, panelX, panelY, panelWidth, panelHeight, 4);

        drawCenteredText(batch, titleFont, "🎒 INVENTORY", panelX + panelWidth / 2, panelY + panelHeight - 30, Color.ORANGE);

        drawBeautifulItemGrid(batch, panelX + 20, panelY + 60, panelWidth - 40, panelHeight - 120);
        drawPokemonStyleButton(batch, controller.getCloseInventoryButton(), "x", Color.GRAY);
    }

    private void drawBeautifulItemGrid(SpriteBatch batch, float x, float y, float width, float height) {
        Map<Rectangle, Items> itemRectMap = controller.getItemRectMap();
        itemRectMap.clear();
        Map<String, Integer> characterItems = controller.getGameController().getCharacter().getBuffItems2();

        if (characterItems == null || characterItems.isEmpty()) {
            drawCenteredText(batch, regularFont, "No items available", x + width / 2, y + height / 2, Color.GRAY);
            return;
        }

        int columns = 3;
        float itemSlotSize = (width - 40) / columns;
        float itemSlotHeight = 80;

        Vector3 mousePos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(mousePos);

        int index = 0;
        for (Map.Entry<String, Integer> entry : characterItems.entrySet()) {
            Items item = ItemLoader.getItemByName(entry.getKey());
            if (item == null) continue;

            int row = index / columns;
            int col = index % columns;

            float itemX = x + 20 + col * itemSlotSize;
            float itemY = y + height - 40 - (row + 1) * (itemSlotHeight + 10);

            Rectangle itemRect = new Rectangle(itemX, itemY, itemSlotSize - 10, itemSlotHeight);
            itemRectMap.put(itemRect, item);

            boolean isHovered = itemRect.contains(mousePos.x, mousePos.y);

            batch.setColor(isHovered ? new Color(0.4f, 0.5f, 0.6f, 0.9f) : new Color(0.2f, 0.3f, 0.4f, 0.8f));
            batch.draw(whiteTexture, itemRect.x, itemRect.y, itemRect.width, itemRect.height);

            batch.setColor(isHovered ? Color.YELLOW : Color.WHITE);
            drawBorder(batch, itemRect.x, itemRect.y, itemRect.width, itemRect.height, 2);

            Texture itemIcon = getTexture(item.getTexturePath());
            if (itemIcon != null) {
                batch.setColor(Color.WHITE);
                batch.draw(itemIcon, itemX + 5, itemY + itemSlotHeight - 50, 40, 40);
            }

            regularFont.setColor(Color.WHITE);
            regularFont.draw(batch, item.getItemName(), itemX + 50, itemY + itemSlotHeight - 15);
            regularFont.draw(batch, "x" + entry.getValue(), itemX + itemSlotSize - 40, itemY + itemSlotHeight - 15);

            regularFont.setColor(Color.CYAN);
            regularFont.draw(batch, "MP: " + item.getManaCost(), itemX + 50, itemY + itemSlotHeight - 35);

            index++;
        }
    }

    private void drawPokemonStyleButton(SpriteBatch batch, Rectangle buttonRect, String text, Color color) {
        Vector3 mousePos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(mousePos);
        boolean isHovered = buttonRect.contains(mousePos.x, mousePos.y);

        batch.setColor(isHovered ? color.cpy().mul(1.2f) : color);
        batch.draw(whiteTexture, buttonRect.x, buttonRect.y, buttonRect.width, buttonRect.height);

        batch.setColor(Color.WHITE);
        drawBorder(batch, buttonRect.x, buttonRect.y, buttonRect.width, buttonRect.height, 2);

        layout.setText(regularFont, text);
        regularFont.setColor(Color.BLACK);
        regularFont.draw(batch, text, buttonRect.x + (buttonRect.width - layout.width) / 2, buttonRect.y + (buttonRect.height + layout.height) / 2);
    }


    private void drawBorder(SpriteBatch batch, float x, float y, float width, float height, float thickness) {
        batch.draw(whiteTexture, x, y + height - thickness, width, thickness);
        batch.draw(whiteTexture, x, y, width, thickness);
        batch.draw(whiteTexture, x, y, thickness, height);
        batch.draw(whiteTexture, x + width - thickness, y, thickness, height);
    }

    private void drawWrappedText(SpriteBatch batch, BitmapFont font, String text, float x, float y, float width) {
        layout.setText(font, text, Color.WHITE, width, 1, true);
        font.draw(batch, layout, x, y);
    }

    private void drawScrollableText(SpriteBatch batch, BitmapFont font, String text, float x, float y, float width, float maxHeight) {
        if (text == null || text.isEmpty()) return;

        String[] lines = text.split("\n");
        float lineHeight = font.getLineHeight() + 2;
        int maxVisibleLines = (int) (maxHeight / lineHeight);

        isCombatLogScrollable = lines.length > maxVisibleLines;

        if (!isCombatLogScrollable) {
            combatLogScrollOffset = 0;
            maxCombatLogScrollOffset = 0;

            for (int i = 0; i < lines.length; i++) {
                float lineY = y - (i * lineHeight);
                drawColoredLine(batch, font, lines[i], x, lineY);
            }
        } else {
            maxCombatLogScrollOffset = (lines.length - maxVisibleLines) * lineHeight;
            combatLogScrollOffset = Math.max(0, Math.min(maxCombatLogScrollOffset, combatLogScrollOffset));

            int startLine = (int) (combatLogScrollOffset / lineHeight);
            int endLine = Math.min(lines.length, startLine + maxVisibleLines + 1);

            for (int i = startLine; i < endLine; i++) {
                float lineY = y - ((i - startLine) * lineHeight);
                if (lineY <= y && lineY >= y - maxHeight) {
                    drawColoredLine(batch, font, lines[i], x, lineY);
                }
            }

            // Scroll indicators
            font.setColor(0.7f, 0.7f, 0.9f, 0.8f);
            if (combatLogScrollOffset > 0) {
                font.draw(batch, "↑", x + width - 30, y);
            }
            if (combatLogScrollOffset < maxCombatLogScrollOffset) {
                font.draw(batch, "↓", x + width - 30, y - maxHeight + 15);
            }
        }
    }

    private void drawColoredLine(SpriteBatch batch, BitmapFont font, String line, float x, float y) {
        if (line.contains("---")) {
            font.setColor(Color.GREEN);
        } else if (line.contains("tấn công")) {
            font.setColor(Color.ORANGE);
        } else if (line.contains("hồi phục")) {
            font.setColor(Color.LIME);
        } else {
            font.setColor(Color.WHITE);
        }
        font.draw(batch, line, x, y);
    }

    private void drawLetterGrid(SpriteBatch batch, float gridX, float gridY, float gridSize) {
        if (controller.getTimerAction() > 0) return;

        int gridSizeValue = letterGrid.getGridSize();
        float cellSize = gridSize / gridSizeValue;
        char[][] grid = letterGrid.getGrid();
        boolean[][] selected = letterGrid.getSelectedCells();
        Set<Integer> disabledCells = controller.getDisabledCells();

        for (int y = 0; y < gridSizeValue; y++) {
            for (int x = 0; x < gridSizeValue; x++) {
                float screenX = gridX + x * cellSize;
                float screenY = gridY + (gridSizeValue - 1 - y) * cellSize;

                char letter = grid[y][x];
                boolean isSelected = selected[y][x];
                boolean isVowel = VOWELS.indexOf(Character.toUpperCase(letter)) != -1;
                boolean isDisabled = disabledCells.contains(y * 5 + x);

                Texture cellTexture = isDisabled ? disabledCellTexture : isSelected ? wordCellTexture : isVowel ? vowelCellTexture : this.cellTexture;

                batch.setColor(Color.WHITE);
                batch.draw(cellTexture, screenX, screenY, cellSize, cellSize);

                Color letterColor = isDisabled ? Color.GRAY : isSelected ? Color.RED : isVowel ? Color.BLUE : Color.BLACK;

                layout.setText(regularFont, String.valueOf(letter));
                regularFont.setColor(letterColor);
                regularFont.draw(batch, String.valueOf(letter), screenX + (cellSize - layout.width) / 2, screenY + cellSize - (cellSize - layout.height) / 2);
            }
        }
    }

    private void drawCurrentWordCells(SpriteBatch batch, String currentWord, float columnX, float y, float columnWidth) {
        if (currentWord.isEmpty()) {
            regularFont.setColor(Color.GRAY);
            drawCenteredText(batch, regularFont, "Chọn các chữ cái để tạo từ", columnX + columnWidth / 2, y + 22, Color.GRAY);
            return;
        } else {
            final float CELL_SIZE = 35;
            final float CELL_SPACING = 5;
            float totalWidth = currentWord.length() * (CELL_SIZE + CELL_SPACING) - CELL_SPACING;
            float startX = columnX + (columnWidth - totalWidth) / 2;

            for (int i = 0; i < currentWord.length(); i++) {
                char letter = currentWord.charAt(i);
                boolean isVowel = VOWELS.indexOf(Character.toUpperCase(letter)) != -1;

                float cellX = startX + i * (CELL_SIZE + CELL_SPACING);

                batch.setColor(Color.WHITE);
                batch.draw(isVowel ? vowelCellTexture : cellTexture, cellX, y, CELL_SIZE, CELL_SIZE);

                layout.setText(regularFont, String.valueOf(letter));
                regularFont.setColor(isVowel ? Color.BLUE : Color.BLACK);
                regularFont.draw(batch, String.valueOf(letter), cellX + (CELL_SIZE - layout.width) / 2, y + CELL_SIZE - (CELL_SIZE - layout.height) / 2);
            }
        }
    }

    private void drawButton(SpriteBatch batch, Rectangle buttonRect, String text) {
        Vector3 mousePos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(mousePos);
        boolean isSelected = buttonRect.contains(mousePos.x, mousePos.y);

        batch.setColor(Color.WHITE);
        batch.draw(isSelected ? buttonSelectedTexture : buttonTexture, buttonRect.x, buttonRect.y, buttonRect.width, buttonRect.height);

        layout.setText(regularFont, text);
        regularFont.setColor(Color.WHITE);
        regularFont.draw(batch, text, buttonRect.x + (buttonRect.width - layout.width) / 2, buttonRect.y + (buttonRect.height + layout.height) / 2);
    }

    private void drawStatusEffects(SpriteBatch batch, float x, float y) {
        float iconSize = 24;
        float spacing = 30;
        int iconIndex = 0;

        Map<String, Integer> playerStatusDuration = controller.getPlayerStatusDuration();
        if (playerStatusDuration != null && !playerStatusDuration.isEmpty()) {
            for (Map.Entry<String, Integer> status : playerStatusDuration.entrySet()) {
                String statusName = status.getKey();
                int duration = status.getValue();

                Texture statusIcon = getTexture("ui/" + statusName.toLowerCase() + ".png");
                if (statusIcon != null) {
                    batch.setColor(Color.WHITE);
                    batch.draw(statusIcon, x + iconIndex * spacing, y, iconSize, iconSize);
                }

                regularFont.setColor(Color.WHITE);
                regularFont.draw(batch, String.valueOf(duration), x + iconIndex * spacing + iconSize - 10, y);


                // Compute flicker alpha
                float phase = effectAlphaPhase.getOrDefault(statusName, 0f);
                float alpha = 0.5f + 0.5f * MathUtils.sin(phase);
                batch.setColor(1f, 1f, 1f, alpha);

                // Draw effect on enemy
                Texture effectTexture = null;
                if (statusName.equals("BUFF_ATK") || statusName.equals("BUFF_DEF"))
                    effectTexture = getTexture("ui/regen_effect.png");
                else
                    effectTexture = getTexture("ui/" + statusName.toLowerCase() + "_effect.png");

                if (effectTexture != null) {
                    switch (statusName) {
                        case "BURN":
                            batch.setColor(1f, 0.4f, 0f, alpha); // Orange-red glow
                            batch.draw(playerTexture, 326, 266, 150, 200);
                            break;
                        case "TOXIC":
                            batch.draw(effectTexture, 286, 286, 200, 150);
                            break;
                        case "BUFF_ATK":
                        case "BUFF_DEF":
                            batch.draw(effectTexture, 286, 256, 200, 150);
                            break;
                    }
                }

                batch.setColor(Color.WHITE); // Reset after each effect

                iconIndex++;
            }


        }
    }

    private void drawEnemyStatusEffects(SpriteBatch batch, float x, float y) {
        float iconSize = 24;
        float spacing = 30;
        int iconIndex = 0;

        if (controller.getEnemyStatusDuration() != null) {
            for (Map.Entry<String, Integer> status : controller.getEnemyStatusDuration().entrySet()) {
                String statusName = status.getKey();
                int duration = status.getValue();

                // Draw status icon
                Texture statusIcon = getTexture("ui/" + statusName.toLowerCase() + ".png");
                if (statusIcon != null) {
                    batch.setColor(Color.WHITE);
                    batch.draw(statusIcon, x + iconIndex * spacing, y, iconSize, iconSize);
                }

                // Draw duration text
                regularFont.setColor(Color.WHITE);
                regularFont.draw(batch, String.valueOf(duration), x + iconIndex * spacing + iconSize - 10, y);

                // Compute flicker alpha
                float phase = effectAlphaPhase.getOrDefault(statusName, 0f);
                float alpha = 0.5f + 0.5f * MathUtils.sin(phase);
                batch.setColor(1f, 1f, 1f, alpha);

                // Draw effect on enemy
                Texture effectTexture = getTexture("ui/" + statusName.toLowerCase() + "_effect.png");
                Texture enemyTexture = getTexture(controller.getEnemy().getTexturePath());
                if (effectTexture != null) {
                    switch (statusName) {
                        case "REGEN":
                            batch.draw(effectTexture, 805, 430, 200, 150);
                            break;
                        case "FREEZE":
                            batch.draw(effectTexture, 800, 430, 200, 200);
                            break;
                        case "BURN":
                            batch.setColor(1f, 0.4f, 0f, alpha); // Orange-red glow
                            batch.draw(enemyTexture, 830, 450, 150, 200);
                            break;
                        case "TOXIC":
                            batch.draw(effectTexture, 805, 460, 200, 170);
                            break;
                    }
                }

                batch.setColor(Color.WHITE); // Reset after each effect
                iconIndex++;
            }
        }
    }

    private void drawModernHPBar(SpriteBatch batch, float currentHP, float maxHP, float x, float y, float width, float height) {
        // Base background
        batch.setColor(0.2f, 0.2f, 0.3f, 0.8f);
        batch.draw(whiteTexture, x, y, width, height);

        // Calculate fill percentage
        float fillPercentage = Math.max(0, currentHP / maxHP);
        float fillWidth = width * fillPercentage;

        // Calculate pulse effect for low health
        float pulse = 1.0f;
        if (fillPercentage < 0.3f) {
            pulse = 0.7f + 0.3f * MathUtils.sin(hpBarAnimationTime * 5f);
        }

        // Main HP bar with gradient
        Color hpFillColor = new Color(hpColor);
        hpFillColor.r *= pulse;
        batch.setColor(hpFillColor);
        batch.draw(whiteTexture, x, y, fillWidth, height);

        // Gradient overlay
        batch.setColor(1, 1, 1, 0.2f + 0.1f * MathUtils.sin(hpBarAnimationTime));
        batch.draw(gradientTexture, x, y, fillWidth, height);

        // Border
        batch.setColor(0.8f, 0.8f, 1.0f, 1);
        drawBorder(batch, x, y, width, height, 2);

        // HP text
        titleFont.setColor(Color.WHITE);
        titleFont.draw(batch, "HP", x - 30, y + height / 2 + 5);
    }

    // Enhanced MP Bar with FF7 Remake style
    private void drawModernMPBar(SpriteBatch batch, float currentMP, float maxMP, float x, float y, float width, float height) {
        // Base background
        batch.setColor(0.2f, 0.2f, 0.3f, 0.8f);
        batch.draw(whiteTexture, x, y, width, height);

        // Calculate fill percentage
        float fillPercentage = Math.max(0, currentMP / maxMP);
        float fillWidth = width * fillPercentage;

        // Main MP bar with shimmer effect
        Color mpFillColor = new Color(mpColor);
        mpFillColor.b += 0.2f * MathUtils.sin(mpBarAnimationTime * 3f);
        batch.setColor(mpFillColor);
        batch.draw(whiteTexture, x, y, fillWidth, height);

        // Gradient overlay
        batch.setColor(1, 1, 1, 0.3f + 0.1f * MathUtils.sin(mpBarAnimationTime * 2f));
        batch.draw(gradientTexture, x, y, fillWidth, height);

        // Border
        batch.setColor(0.8f, 0.8f, 1.0f, 1);
        drawBorder(batch, x, y, width, height, 2);

        // MP text
        titleFont.setColor(Color.WHITE);
        titleFont.draw(batch, "MP", x - 30, y + height / 2 + 5);
    }

    public void updateFlicker2(float delta) {
        hpBarAnimationTime += delta * 2f;
//        mpBarAnimationTime += delta * 1.5f;

        // Check if turn has changed
        boolean isPlayerTurn = controller.isPlayerTurn();
        if (isPlayerTurn != previousTurnWasPlayer) {
            turnTransitionTime = 1.0f;
            previousTurnWasPlayer = isPlayerTurn;
        }

        if (turnTransitionTime > 0) {
            turnTransitionTime = Math.max(0, turnTransitionTime - delta * 2f);
        }
    }

    public void updateFlicker(float delta) {
        if (controller.getEnemyStatusDuration() != null) {
            for (String statusName : controller.getEnemyStatusDuration().keySet()) {
                float phase = effectAlphaPhase.getOrDefault(statusName, 0f);
                phase += delta * 2f; // tốc độ flicker, bạn có thể điều chỉnh
                if (phase > MathUtils.PI2) phase -= MathUtils.PI2;
                effectAlphaPhase.put(statusName, phase);
            }
        }
        if (controller.getPlayerStatusDuration() != null) {
            for (String statusName : controller.getPlayerStatusDuration().keySet()) {
                float phase = effectAlphaPhase.getOrDefault(statusName, 0f);
                phase += delta * 2f; // tốc độ flicker, bạn có thể điều chỉnh
                if (phase > MathUtils.PI2) phase -= MathUtils.PI2;
                effectAlphaPhase.put(statusName, phase);
            }
        }
    }

    public boolean handleCombatLogScroll(float scrollAmount) {
        if (!isCombatLogScrollable) return false;

        float lineHeight = regularFont.getLineHeight() + 2;
        float scrollDelta = scrollAmount * lineHeight;

        float oldOffset = combatLogScrollOffset;
        combatLogScrollOffset = Math.max(0, Math.min(maxCombatLogScrollOffset, combatLogScrollOffset + scrollDelta));

        return oldOffset != combatLogScrollOffset;
    }

    private void drawCenteredText(SpriteBatch batch, BitmapFont font, String text, float x, float y, Color color) {
        layout.setText(font, text);
        font.setColor(color);
        font.draw(batch, text, x - layout.width / 2, y);
    }

    private static final int MAX_TEXTURE_CACHE_SIZE = 50;

    private Texture getTexture(String path) {
        if (!textureCache.containsKey(path)) {
            if (textureCache.size() >= MAX_TEXTURE_CACHE_SIZE) {
                clearOldTextures();
            }

            try {
                Texture texture = new Texture(Gdx.files.internal(path));
                textureCache.put(path, texture);
            } catch (Exception e) {
                textureCache.put(path, null);
            }
        }
        return textureCache.get(path);
    }

    private void clearOldTextures() {
        int removeCount = textureCache.size() / 2;
        textureCache.entrySet().removeIf(entry -> {
            if (removeCount > 0 && entry.getValue() != null) {
                entry.getValue().dispose();
                return true;
            }
            return false;
        });
    }

    public void clearTextureCache() {
        for (Texture texture : textureCache.values()) {
            if (texture != null) texture.dispose();
        }
        textureCache.clear();
    }

    public void resetForNewCombat() {
        combatLogScrollOffset = 0;
        maxCombatLogScrollOffset = 0;
        isCombatLogScrollable = false;
        effectAlphaPhase.clear();

        // Clear some cached textures to free memory
        clearOldTextures();
    }

    public void dispose() {
        clearTextureCache();

        if (whiteTexture != null) whiteTexture.dispose();
        if (buttonTexture != null) buttonTexture.dispose();
        if (buttonSelectedTexture != null) buttonSelectedTexture.dispose();
        if (wordCellTexture != null) wordCellTexture.dispose();
        if (cellTexture != null) cellTexture.dispose();
        if (vowelCellTexture != null) vowelCellTexture.dispose();
        if (disabledCellTexture != null) disabledCellTexture.dispose();

        if (titleFont != null) titleFont.dispose();
        if (regularFont != null) regularFont.dispose();
    }
}