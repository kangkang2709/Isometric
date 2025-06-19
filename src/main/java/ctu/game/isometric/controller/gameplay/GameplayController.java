package ctu.game.isometric.controller.gameplay;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import ctu.game.isometric.controller.AchievementManager;
import ctu.game.isometric.controller.EffectManager;
import ctu.game.isometric.controller.GameController;
import ctu.game.isometric.model.entity.Enemy;
import ctu.game.isometric.model.game.*;
import ctu.game.isometric.model.world.MapEvent;
import ctu.game.isometric.util.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;

import static ctu.game.isometric.util.FontGenerator.generateVietNameseFont;

public class GameplayController {
    // Constants
    private static final float ENEMY_TURN_DELAY = 1.7f;
    private static final float COMBAT_TIME_LIMIT = 300f; // 5 minutes in seconds
    private static final String VOWELS = "AEIOU";
    private static final int MAX_COMBAT_LOG_LINES = 20; // Limit combat log lines
    private static final int MAX_CACHED_TEXTURES = 50; // Limit texture cache
    private static final float CLICK_COOLDOWN = 0.1f; // 100ms cooldown between clicks

    // Core components
    private final GameController gameController;
    private final LetterGrid letterGrid;
    private final Random random = new Random();
    private EffectManager effectManager;
    private WordNetValidator wordValidator;
    private AchievementManager achievementManager;

    // Game state
    private int currentScore;
    private boolean active;
    private String currentMessage = "";
    private float messageTimer = 0;
    private boolean isGameOver = false;
    private int currentLevel = 1;
    private int newLevel = 1;
    private float experienceGain = 0;
    private float lastClickTime = 0;

    // Combat timer
    private float combatTimer = 0;
    private boolean combatTimeUp = false;

    // UI components
    private BitmapFont titleFont, regularFont, bigFont;
    private GlyphLayout layout;
    private Viewport viewport;
    private Texture whiteTexture;
    private Map<String, Texture> textureCache = new HashMap<>();

    // UI textures
    private Texture buttonTexture;
    private Texture buttonSelectedTexture;
    private Texture cellTexture;
    private Texture vowelCellTexture;
    private Texture wordCellTexture;
    private Texture disabledCellTexture;

    // Boss mechanics
    private Set<Integer> disabledCells = new HashSet<>();

    // Player stats
    private float playerMaxHealth = 100;
    private float playerHealth = 100;
    private float playerMana = 100;
    private float playerMaxMana = 100;
    private String playerName = "Player";

    // Enemy stats
    private float enemyHealth = 100;
    private float enemyMaxHealth = 100;
    private String enemyName = "Enemy";
    private Enemy enemy;

    // Button areas
    private Rectangle submitButtonRect, clearButtonRect, exitButtonRect;

    // Combat state
    private boolean isCombatMode = false;
    private boolean isPlayerTurn = true;
    private float enemyActionTimer = 0;
    private List<String> combatLogLines = new ArrayList<>(); // Changed to list for better management
    private float wordDamageMultiplier = 1f;
    private boolean isVictory = false;
    private boolean isDrawingWordMeaning = false;
    private String lastSubmittedWord = "";

    // Item handling
    private Map<Rectangle, Items> itemRectMap = new HashMap<>();
    private Items hoveredItem = null;

    // Current event
    private MapEvent currentEvent;

    public GameplayController(GameController gameController) {
        this.gameController = gameController;
        this.letterGrid = new LetterGrid();
        this.effectManager = gameController.getEffectManager();
        this.wordValidator = gameController.getWordNetValidator();
        this.achievementManager = gameController.getAchievementManager();

        initializeUI();
        initializeGridConstants();
    }

    private void initializeUI() {
        // Initialize fonts
        titleFont = generateVietNameseFont("Tektur-Bold.ttf", 18);
        regularFont = generateVietNameseFont("Tektur-Bold.ttf", 13);
        bigFont = regularFont;

        layout = new GlyphLayout();
        viewport = new FitViewport(1280, 720);

        // Create white texture for drawing colored rectangles
        createWhiteTexture();

        // Load UI textures with error handling
        loadUITextures();

        // Create special cell textures
        createSpecialCellTextures();
    }

    private void createWhiteTexture() {
        if (whiteTexture != null) {
            whiteTexture.dispose();
        }
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        whiteTexture = new Texture(pixmap);
        pixmap.dispose();
    }

    private void loadUITextures() {
        try {
            buttonTexture = new Texture(Gdx.files.internal("ui/button.png"));
            buttonSelectedTexture = new Texture(Gdx.files.internal("ui/button_selected.png"));
            wordCellTexture = new Texture(Gdx.files.internal("ui/cell.png"));
        } catch (Exception e) {
            Gdx.app.error("GameplayController", "Error loading UI textures", e);
            // Create fallback textures
            buttonTexture = createFallbackTexture(100, 40, Color.GRAY);
            buttonSelectedTexture = createFallbackTexture(100, 40, Color.LIGHT_GRAY);
            wordCellTexture = createFallbackTexture(64, 64, Color.WHITE);
        }
    }

    private Texture createFallbackTexture(int width, int height, Color color) {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        pixmap.setColor(Color.BLACK);
        pixmap.drawRectangle(0, 0, width, height);
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    private void createSpecialCellTextures() {
        // Dispose existing textures first
        if (cellTexture != null) cellTexture.dispose();
        if (vowelCellTexture != null) vowelCellTexture.dispose();
        if (disabledCellTexture != null) disabledCellTexture.dispose();

        // Create new textures
        cellTexture = createTintedTexture(new Color(1.0f, 1.0f, 1.0f, 1.0f));
        vowelCellTexture = createTintedTexture(new Color(0.8f, 0.9f, 1.0f, 1.0f));
        disabledCellTexture = createTintedTexture(new Color(0.5f, 0.5f, 0.5f, 0.8f));
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

    public void update(float delta) {
        if (!active) return;

        // Update particle effects
        effectManager.update(delta);

        // Update message timer
        if (messageTimer > 0) {
            messageTimer -= delta;
            if (messageTimer <= 0) currentMessage = "";
        }

        // Update combat mode if active
        if (isCombatMode) updateCombat(delta);

        // Clean up texture cache periodically
        if (textureCache.size() > MAX_CACHED_TEXTURES) {
            cleanupTextureCache();
        }
    }

    private void cleanupTextureCache() {
        // Remove some old textures to prevent memory overflow
        int toRemove = textureCache.size() - MAX_CACHED_TEXTURES + 10;
        List<String> keysToRemove = new ArrayList<>();

        for (String key : textureCache.keySet()) {
            if (keysToRemove.size() >= toRemove) break;
            keysToRemove.add(key);
        }

        for (String key : keysToRemove) {
            Texture texture = textureCache.remove(key);
            if (texture != null) {
                texture.dispose();
            }
        }

        Gdx.app.log("GameplayController", "Cleaned up " + keysToRemove.size() + " textures from cache");
    }

    private void updateCombat(float delta) {
        // Update combat timer
        combatTimer += delta;
        if (combatTimer >= COMBAT_TIME_LIMIT && !combatTimeUp) {
            combatTimeUp = true;
            addCombatLog("Thời gian hết! Trận đấu kết thúc!");
            endCombat(false);
            return;
        }

        // Process enemy turn
        if (!isPlayerTurn) {
            enemyActionTimer += delta;
            if (enemyActionTimer >= ENEMY_TURN_DELAY) {
                performEnemyAction();
                enemyActionTimer = 0;
            }
        }
    }

    private void addCombatLog(String message) {
        combatLogLines.add(message);
        handleCombatLogScroll(1);
        if (combatLogLines.size() > MAX_COMBAT_LOG_LINES) {
            combatLogLines.remove(0); // Remove oldest line if limit exceeded
        }
    }

    private String getCombatLogText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < combatLogLines.size(); i++) {
            if (i > 0) sb.append("\n");
            sb.append(combatLogLines.get(i));
        }
        return sb.toString();
    }

    private boolean isEnemyBoss() {
        return enemyName.toLowerCase().contains("boss");
    }

    private boolean isEnemyLord() {
        return enemyName.toLowerCase().contains("lord");
    }


    private void applyBossEffects() {
        if (isEnemyBoss()) {
            // Disable 1-2 random cells
            disabledCells.clear();
            int cellsToDisable = random.nextInt(2) + 1; // 1 or 2

            for (int i = 0; i < cellsToDisable; i++) {
                int cellIndex;
                do {
                    cellIndex = random.nextInt(25); // 5x5 grid = 25 cells
                } while (disabledCells.contains(cellIndex));

                disabledCells.add(cellIndex);
            }
        }
    }

    private boolean isCellDisabled(int x, int y) {
        int cellIndex = y * 5 + x;
        return disabledCells.contains(cellIndex);
    }

    public boolean handleCombatClick(float x, float screenY) {
        float y = Gdx.graphics.getHeight() - screenY;

        if (submitButtonRect != null && submitButtonRect.contains(x, y)) {
            submitWord();
            return true;
        } else if (clearButtonRect != null && clearButtonRect.contains(x, y)) {
            clearSelection();
            return true;
        } else {
            checkGridClick(x, y);
            handleItemBoxClick(x, y);
            return true;
        }
    }

    private Vector3 getTouchPosition() {
        Vector3 touchPos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(touchPos);
        return touchPos;
    }

    private boolean handleItemBoxClick(float x, float y) {
        for (Map.Entry<Rectangle, Items> entry : itemRectMap.entrySet()) {
            if (entry.getKey().contains(x, y)) {
                useItem(entry.getValue());
                return true;
            }
        }
        return false;
    }


    private void useItem(Items item) {
        if (item == null) return;

        // Only allow item usage during player's turn in combat
        if (!isPlayerTurn || !isCombatMode) return;

        // Check if player has enough mana
        if (playerMana < item.getManaCost()) {
            addCombatLog("Nhân vật không đủ mana để sử dụng " + item.getItemName() + "!");
            return;
        }

        // Remove one of this item from inventory
        Map<String, Integer> items = gameController.getCharacter().getItems();
        if (items.containsKey(item.getItemName()) && items.get(item.getItemName()) > 0) {
            // Apply effect based on item type
            switch (item.getItemEffect()) {
                case "heal":
                    playerHealth = Math.min(playerMaxHealth, playerHealth + item.getValue());
                    addCombatLog("Đã dùng " + item.getItemName() + "! HỒI " + item.getValue() + " Sinh Lực!");
                    break;
                case "buff":
                    wordDamageMultiplier += item.getValue();
                    addCombatLog("Đã dùng " + item.getItemName() + "! MẠNH MẼ!");
                    break;
                default:
                    addCombatLog("Đã dùng " + item.getItemName() + "!");
                    break;
            }

            // Reduce player mana
            this.playerMana = Math.max(0, this.playerMana - item.getManaCost());

            // Reduce item count
            int newCount = items.get(item.getItemName()) - 1;
            if (newCount <= 0) {
                items.remove(item.getItemName());
            } else {
                items.put(item.getItemName(), newCount);
            }

            // End player's turn after using an item
            addCombatLog("Bạn đã dùng " + item.getItemName() + ".Tới Lượt Của Kẻ Địch!");
            isPlayerTurn = false;
        }
    }

    // Add these class fields to store pre-calculated values
    private float gridX, gridY, gridSize, cellSize;
    private float gridXMax, gridYMax;
    private static final float CLICK_PADDING = 2f;

    // Call this method when viewport changes or during initialization
    private void initializeGridConstants() {
        final float SCREEN_WIDTH = 1280;
        final float MARGIN = 20;
        final float PLAYER_COLUMN_WIDTH = 250;
        final float ITEM_COLUMN_WIDTH = 200;
        final float GRID_COLUMN_WIDTH = SCREEN_WIDTH - PLAYER_COLUMN_WIDTH - ITEM_COLUMN_WIDTH - 4 * MARGIN;
        final float GRID_COLUMN_X = MARGIN + PLAYER_COLUMN_WIDTH + MARGIN;

        gridSize = 275;
        gridX = GRID_COLUMN_X + (GRID_COLUMN_WIDTH - gridSize) / 2;
        gridY = 70;
        cellSize = gridSize / 5;

        // Pre-calculate grid boundaries with padding
        gridXMax = gridX + gridSize - CLICK_PADDING;
        gridYMax = gridY + gridSize - CLICK_PADDING;
    }

    private void checkGridClick(float x, float y) {

        // Fast bounds check using pre-calculated boundaries
        if (x < gridX + CLICK_PADDING || x >= gridXMax ||
                y < gridY + CLICK_PADDING || y >= gridYMax) {
            return; // Outside grid bounds
        }

        // Calculate cell coordinates using fast approach
        int cellX = (int) ((x - gridX) * 0.018181f); // 1/55 = 0.018181 (cellSize is 55)
        int cellY = 4 - (int) ((y - gridY) * 0.018181f);

        // Final bounds check and selection
        if (cellX >= 0 && cellX < 5 && cellY >= 0 && cellY < 5 && !isCellDisabled(cellX, cellY)) {
            selectCell(cellX, cellY);
        }
    }

    public void render(SpriteBatch batch) {
        if (!active) return;

        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);

        // Draw background
        batch.setColor(0.1f, 0.1f, 0.2f, 1);
        batch.draw(whiteTexture, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
        batch.setColor(Color.WHITE);

        if (isCombatMode) {
            renderCombatUI(batch);
        } else if (isVictory) {
            renderReward(batch);
        } else {
            renderGameOver(batch);
        }

        effectManager.render(batch);
    }

    public void renderGameOver(SpriteBatch batch) {
        if (!isGameOver) {
            gameController.setState(GameState.EXPLORING);
        } else {
            drawCenteredText(batch, bigFont, "Game Over!", viewport.getWorldWidth() / 2, viewport.getWorldHeight() / 2, Color.RED);
        }
    }

    private void renderReward(SpriteBatch batch) {
        // Draw background
        batch.setColor(0.1f, 0.1f, 0.2f, 1);
        batch.draw(whiteTexture, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());

        // Draw victory panel
        float panelWidth = 600;
        float panelHeight = 400;
        float panelX = (viewport.getWorldWidth() - panelWidth) / 2;
        float panelY = (viewport.getWorldHeight() - panelHeight) / 2;

        // Panel background
        batch.setColor(0.2f, 0.2f, 0.4f, 0.9f);
        batch.draw(whiteTexture, panelX, panelY, panelWidth, panelHeight);

        // Panel border
        batch.setColor(0.8f, 0.7f, 0.2f, 1);
        drawRect(batch, panelX, panelY, panelWidth, panelHeight, 3);

        // Get reward information
        Reward reward = RewardLoader.getRewardById(this.enemy.getRewardID());
        Items item = ItemLoader.getItemById(reward.getItemID());

        // Title
        drawCenteredText(batch, titleFont, "CHIẾN THĂNG!", viewport.getWorldWidth() / 2, panelY + panelHeight - 50, new Color(1, 0.9f, 0.3f, 1));

        // Enemy defeated message
        drawCenteredText(batch, regularFont, "Bạn đã thắng " + enemyName + "!",
                viewport.getWorldWidth() / 2, panelY + panelHeight - 100, Color.WHITE);

        // Draw reward item
        if (item != null) {
            try {
                Texture itemTexture = getTexture(item.getTexturePath());
                float iconSize = 64;
                float iconX = panelX + 100;
                float iconY = panelY + panelHeight / 2 - iconSize / 2;
                batch.setColor(Color.WHITE);
                batch.draw(itemTexture, iconX, iconY, iconSize, iconSize);
            } catch (Exception e) {
                Gdx.app.error("GameplayController", "Could not load item texture: " + item.getTexturePath());
            }

            // Item details
            float textX = panelX + 180;
            float textY = panelY + panelHeight / 2 + 30;

            regularFont.setColor(new Color(0.9f, 0.9f, 0.3f, 1));
            regularFont.draw(batch, item.getItemName() + " x" + reward.getAmount(), textX, textY);

            regularFont.setColor(Color.WHITE);
            drawWrappedText(batch, regularFont, reward.getDescription(), textX, textY - 25, panelWidth - 200);
        }

        // Continue button
        float buttonWidth = 200;
        float buttonHeight = 50;
        float buttonX = viewport.getWorldWidth() / 2 - buttonWidth / 2;
        float buttonY = panelY + 50;

        Rectangle continueButton = new Rectangle(buttonX, buttonY, buttonWidth, buttonHeight);

        drawButton(batch, continueButton, "Tiếp tục");

        // Handle button click
        if (Gdx.input.justTouched()) {
            Vector3 touchPos = getTouchPosition();
            if (continueButton.contains(touchPos.x, touchPos.y)) {
                gameController.getCharacter().addItem(item, reward.getAmount());
                gameController.getInventoryUI().notifyItemsChanged();

                gameController.getCharacter().setHealth(playerHealth);
                gameController.getCharacter().setMana(playerMana);

                Timer.schedule(new Timer.Task() {
                    @Override
                    public void run() {
                        gameController.setState(GameState.EXPLORING);
                        if (newLevel > currentLevel) {
                            gameController.showLevelUpNotification();

                        }
                        // Don't dispose here - just clean up combat state
                        cleanupCombatState();
                    }
                }, 0.5f);
            }
        }

        batch.setColor(Color.WHITE);
    }

    private void drawButton(SpriteBatch batch, Rectangle buttonRect, String text) {
        batch.setColor(Color.WHITE);

        // Check if mouse is hovering over the button
        Vector3 mousePos = getTouchPosition();
        boolean isSelected = buttonRect.contains(mousePos.x, mousePos.y);

        // Draw appropriate button texture based on selection state
        batch.draw(isSelected ? buttonSelectedTexture : buttonTexture,
                buttonRect.x, buttonRect.y, buttonRect.width, buttonRect.height);

        layout.setText(regularFont, text);
        regularFont.setColor(Color.WHITE);
        regularFont.draw(batch, text,
                buttonRect.x + (buttonRect.width - layout.width) / 2,
                buttonRect.y + (buttonRect.height + layout.height) / 2);
    }

    private void renderCombatUI(SpriteBatch batch) {
        // Draw battle background
        batch.setColor(0.15f, 0.15f, 0.3f, 1);
        batch.draw(whiteTexture, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
        batch.setColor(Color.WHITE);

        // Layout constants
        final float SCREEN_WIDTH = viewport.getWorldWidth();
        final float SCREEN_HEIGHT = viewport.getWorldHeight();
        final float MARGIN = 20;

        // Top section - Enemy Status (full width)
        final float ENEMY_SECTION_HEIGHT = 120;
        final float ENEMY_SECTION_Y = SCREEN_HEIGHT - ENEMY_SECTION_HEIGHT - MARGIN;
        drawEnemyStatusSection(batch, MARGIN, ENEMY_SECTION_Y, SCREEN_WIDTH - 2 * MARGIN, ENEMY_SECTION_HEIGHT);

        // Middle section - Combat Log (full width)
        final float LOG_SECTION_HEIGHT = 100;
        final float LOG_SECTION_Y = ENEMY_SECTION_Y - LOG_SECTION_HEIGHT - MARGIN;

        drawCombatLogSection(batch, MARGIN, LOG_SECTION_Y, SCREEN_WIDTH - 2 * MARGIN, LOG_SECTION_HEIGHT);

        // Bottom section - Three columns
        final float BOTTOM_SECTION_Y = MARGIN;
        final float BOTTOM_SECTION_HEIGHT = LOG_SECTION_Y - 2 * MARGIN;

        // Column widths
        final float PLAYER_COLUMN_WIDTH = 250;
        final float ITEM_COLUMN_WIDTH = 200;
        final float GRID_COLUMN_WIDTH = SCREEN_WIDTH - PLAYER_COLUMN_WIDTH - ITEM_COLUMN_WIDTH - 4 * MARGIN;

        // Column positions
        final float PLAYER_COLUMN_X = MARGIN;
        final float GRID_COLUMN_X = PLAYER_COLUMN_X + PLAYER_COLUMN_WIDTH + MARGIN;
        final float ITEM_COLUMN_X = GRID_COLUMN_X + GRID_COLUMN_WIDTH + MARGIN;

        // Draw three columns
        drawPlayerStatusColumn(batch, PLAYER_COLUMN_X, BOTTOM_SECTION_Y, PLAYER_COLUMN_WIDTH, BOTTOM_SECTION_HEIGHT);
        drawLetterGridColumn(batch, GRID_COLUMN_X, BOTTOM_SECTION_Y, GRID_COLUMN_WIDTH, BOTTOM_SECTION_HEIGHT);
        drawItemColumn(batch, ITEM_COLUMN_X, BOTTOM_SECTION_Y, ITEM_COLUMN_WIDTH, BOTTOM_SECTION_HEIGHT);
    }

    private void drawEnemyStatusSection(SpriteBatch batch, float x, float y, float width, float height) {
        // Draw section background
        batch.setColor(0.2f, 0.2f, 0.4f, 0.9f);
        batch.draw(whiteTexture, x, y, width, height);

        // Draw border
        batch.setColor(0.8f, 0.2f, 0.2f, 1);
        drawRect(batch, x, y, width, height, 2);

        // Section title
        batch.setColor(Color.WHITE);
        String titleText = "TRẠNG THÁI KẺ ĐỊCH";
        if (isEnemyBoss()) {
            titleText += " (BOSS)";
        } else if (isEnemyLord()) {
            titleText += " (LORD)";
        }
        drawCenteredText(batch, titleFont, titleText, x + width / 2, y + height - 20, Color.RED);

        // Enemy image
        final float ENEMY_IMG_SIZE = 80;
        final float ENEMY_IMG_X = x + 20;
        final float ENEMY_IMG_Y = y + 20;

        Texture enemyTexture = getTexture(this.enemy.getTexturePath());
        if (enemyTexture != null) {
            batch.setColor(Color.WHITE);
            batch.draw(enemyTexture, ENEMY_IMG_X, ENEMY_IMG_Y, ENEMY_IMG_SIZE, ENEMY_IMG_SIZE);
        }

        // Enemy info text area
        final float TEXT_START_X = ENEMY_IMG_X + ENEMY_IMG_SIZE + 20;
        final float TEXT_Y = y + height - 50;

        regularFont.setColor(Color.WHITE);
        regularFont.draw(batch, "Tên: " + enemyName, TEXT_START_X, TEXT_Y);

        // Health bar
        final float HP_BAR_WIDTH = 200;
        final float HP_BAR_HEIGHT = 15;
        final float HP_BAR_X = TEXT_START_X + 100;
        final float HP_BAR_Y = TEXT_Y - 25;

        regularFont.draw(batch, "HP: " + enemyHealth + "/" + enemyMaxHealth, TEXT_START_X, HP_BAR_Y);

        // Health bar background
        batch.setColor(0.3f, 0.3f, 0.3f, 1);
        batch.draw(whiteTexture, HP_BAR_X - 100, HP_BAR_Y - 30, HP_BAR_WIDTH, HP_BAR_HEIGHT);

        // Health bar fill
        float healthPercentage = (float) enemyHealth / enemyMaxHealth;
        batch.setColor(getHealthColor(healthPercentage));
        batch.draw(whiteTexture, HP_BAR_X - 100, HP_BAR_Y - 30, HP_BAR_WIDTH * healthPercentage, HP_BAR_HEIGHT);

        // Status effects
        regularFont.setColor(Color.ORANGE);
        regularFont.draw(batch, "Mô tả:" + this.enemy.getEnemyDescription(), TEXT_START_X + 350, TEXT_Y);

        // Special abilities
        if (isEnemyBoss()) {
            regularFont.setColor(Color.RED);
            regularFont.draw(batch, "Khả năng: Vô hiệu hóa ô chữ", TEXT_START_X + 350, TEXT_Y - 20);
        } else if (isEnemyLord()) {
            regularFont.setColor(Color.PURPLE);
            regularFont.draw(batch, "Khả năng: Giảm sát thương nhỏ", TEXT_START_X + 350, TEXT_Y - 20);
        }
    }

    private Color getHealthColor(float percentage) {
        if (percentage > 0.5f) return new Color(0.3f, 0.9f, 0.3f, 1);
        else if (percentage > 0.2f) return new Color(0.9f, 0.9f, 0.2f, 1);
        else return new Color(0.9f, 0.2f, 0.2f, 1);
    }

    private void drawCombatLogSection(SpriteBatch batch, float x, float y, float width, float height) {
        // Draw section background
        batch.setColor(0.1f, 0.1f, 0.2f, 0.9f);
        batch.draw(whiteTexture, x, y, width, height);

        // Draw border
        batch.setColor(0.6f, 0.6f, 0.8f, 1);
        drawRect(batch, x, y, width, height, 2);

        // Section title
        batch.setColor(Color.WHITE);
        drawCenteredText(batch, titleFont, "NHẬT KÝ GIAO CHIẾN / THÔNG BÁO", (x + width / 2) + 50, y + height - 15, Color.CYAN);

        // Combat log text area
        final float LOG_TEXT_X = x + 10;
        final float LOG_TEXT_Y = y + height - 40;
        final float LOG_WIDTH = width - 300;
        final float MAX_TEXT_HEIGHT = height - 50;

        regularFont.setColor(Color.WHITE);
        drawScrollableTextFixed(batch, regularFont, getCombatLogText(), LOG_TEXT_X + 20, LOG_TEXT_Y + 20, LOG_WIDTH, MAX_TEXT_HEIGHT);

        // Combat timer
        float timeLeft = COMBAT_TIME_LIMIT - combatTimer;
        int minutes = (int) (timeLeft / 60);
        int seconds = (int) (timeLeft % 60);
        String timeText = String.format("⏰ %02d:%02d", minutes, seconds);
        Color timeColor = timeLeft < 60 ? Color.RED : Color.WHITE;

        drawCenteredText(batch, regularFont, timeText, x + width - 100, y + height - 25, timeColor);

        // Current turn indicator
        String turnText = "► " + "Lượt của: " + (isPlayerTurn ? "✦ Người Chơi ✦" : "✦ " + enemyName + " ✦");
        Color turnColor = isPlayerTurn ? Color.GREEN : Color.RED;
        regularFont.setColor(turnColor);
        regularFont.draw(batch, turnText, x + width - 250, y + height - 50);
    }

    private void drawScrollableTextFixed(SpriteBatch batch, BitmapFont font, String text, float x, float y, float width, float maxHeight) {
        if (text == null || text.isEmpty()) return;

        String[] lines = text.split("\n");
        float lineHeight = font.getLineHeight() + 2; // Small spacing between lines
        float totalHeight = lines.length * lineHeight;

        // Calculate how many lines can fit
        int maxVisibleLines = (int) (maxHeight / lineHeight);

        // Update scroll state
        isCombatLogScrollable = lines.length > maxVisibleLines;

        if (!isCombatLogScrollable) {
            // If content fits, reset scroll and show all lines
            combatLogScrollOffset = 0;
            maxCombatLogScrollOffset = 0;

            // Draw all lines
            for (int i = 0; i < lines.length; i++) {
                float lineY = y - (i * lineHeight);
                drawColoredLine(batch, font, lines[i], x, lineY);
            }
        } else {
            // Content needs scrolling
            maxCombatLogScrollOffset = (lines.length - maxVisibleLines) * lineHeight;

            // Clamp scroll offset
            combatLogScrollOffset = Math.max(0, Math.min(maxCombatLogScrollOffset, combatLogScrollOffset));

            // Calculate which lines to show
            int startLine = (int) (combatLogScrollOffset / lineHeight);
            int endLine = Math.min(lines.length, startLine + maxVisibleLines + 1);

            // Draw visible lines
            for (int i = startLine; i < endLine; i++) {
                float lineY = y - ((i - startLine) * lineHeight);

                // Make sure line is within visible area
                if (lineY <= y && lineY >= y - maxHeight) {
                    drawColoredLine(batch, font, lines[i], x, lineY);
                }
            }

            // Draw scroll indicators
            font.setColor(0.7f, 0.7f, 0.9f, 0.8f);
            if (combatLogScrollOffset > 0) {
                font.draw(batch, "<", x + width - 30, y);
            }
            if (combatLogScrollOffset < maxCombatLogScrollOffset) {
                font.draw(batch, ">", x + width - 30, y - maxHeight + 15);
            }
        }
    }

    // Helper method to draw lines with color coding
    private void drawColoredLine(SpriteBatch batch, BitmapFont font, String line, float x, float y) {
        // Set color based on content
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

    private float combatLogScrollOffset = 0; // Current scroll offset
    private float maxCombatLogScrollOffset = 0; // Maximum possible scroll offset
    private boolean isCombatLogScrollable = false; // Whether there's enough content to scroll
    private static final float SCROLL_SPEED = 20f; // Increased for better responsiveness

    public boolean handleCombatLogScroll(float scrollAmount) {
        if (!isCombatLogScrollable) return false;

        // Convert scroll amount to line-based scrolling
        float lineHeight = regularFont.getLineHeight() + 2;
        float scrollDelta = scrollAmount * lineHeight; // Move by full lines

        float oldOffset = combatLogScrollOffset;
        combatLogScrollOffset = Math.max(0, Math.min(maxCombatLogScrollOffset, combatLogScrollOffset + scrollDelta));

        // Return true if scroll position actually changed
        return oldOffset != combatLogScrollOffset;
    }


    // Helper method to handle text wrapping
    private void drawWrappedText(SpriteBatch batch, BitmapFont font, String text, float x, float y, float width, float lineHeight) {
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        float currentY = y;

        for (String word : words) {
            String testLine = currentLine.length() > 0 ? currentLine + " " + word : word;
            GlyphLayout testLayout = new GlyphLayout(font, testLine);

            if (testLayout.width <= width) {
                currentLine.append(currentLine.length() > 0 ? " " + word : word);
            } else {
                // Draw current line and start new one
                if (currentLine.length() > 0) {
                    font.draw(batch, currentLine.toString(), x, currentY);
                    currentY -= lineHeight;
                    currentLine = new StringBuilder(word);
                } else {
                    // Single word is too long, draw it anyway
                    font.draw(batch, word, x, currentY);
                    currentY -= lineHeight;
                }
            }
        }

        // Draw the last line
        if (currentLine.length() > 0) {
            font.draw(batch, currentLine.toString(), x, currentY);
        }
    }

    // Optional: Method to auto-scroll to bottom when new content is added
    public void scrollToBottom() {
        combatLogScrollOffset = maxCombatLogScrollOffset;
    }

    // Optional: Method to scroll to top
    public void scrollToTop() {
        combatLogScrollOffset = 0;
    }

    private void drawWrappedLine(SpriteBatch batch, BitmapFont font, String text, float x, float y, float width) {
        String[] words = text.split("\\s+");
        StringBuilder currentLine = new StringBuilder();
        float lineHeight = font.getLineHeight();
        float currentY = y;

        for (String word : words) {
            // Test if adding this word exceeds the width
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            GlyphLayout layout = new GlyphLayout(font, testLine);

            if (layout.width > width) {
                // Draw current line and start a new one
                if (currentLine.length() > 0) {
                    font.draw(batch, currentLine.toString(), x, currentY);
                    currentY -= lineHeight;
                    currentLine = new StringBuilder(word);
                } else {
                    // Word is too long on its own, just draw it
                    font.draw(batch, word, x, currentY);
                    currentY -= lineHeight;
                    currentLine = new StringBuilder();
                }
            } else {
                currentLine = new StringBuilder(testLine);
            }
        }

        // Draw the last line if any
        if (currentLine.length() > 0) {
            font.draw(batch, currentLine.toString(), x, currentY);
        }
    }

    public MapEvent getCurrentEvent() {
        return currentEvent;
    }

    public void setCurrentEvent(MapEvent currentEvent) {
        this.currentEvent = currentEvent;
    }

    private void drawPlayerStatusColumn(SpriteBatch batch, float x, float y, float width, float height) {
        // Draw column background
        batch.setColor(0.2f, 0.4f, 0.2f, 0.9f);
        batch.draw(whiteTexture, x, y, width, height);

        // Draw border
        batch.setColor(0.2f, 0.8f, 0.2f, 1);
        drawRect(batch, x, y, width, height, 2);

        // Column title
        batch.setColor(Color.WHITE);
        drawCenteredText(batch, regularFont, "TRẠNG THÁI", x + width / 2, y + height - 15, Color.GREEN);
        drawCenteredText(batch, regularFont, "NGƯỜI CHƠI", x + width / 2, y + height - 35, Color.GREEN);

        // Player image
        final float PLAYER_IMG_SIZE = 100;
        final float PLAYER_IMG_X = x + (width - PLAYER_IMG_SIZE) / 2;
        final float PLAYER_IMG_Y = y + height - 160;

        Texture playerTexture = getTexture("characters/player.png");
        if (playerTexture != null) {
            batch.setColor(Color.WHITE);
            batch.draw(playerTexture, PLAYER_IMG_X, PLAYER_IMG_Y, PLAYER_IMG_SIZE, PLAYER_IMG_SIZE);
        }

        // Player stats
        final float STATS_X = x + 15;
        final float STATS_Y = PLAYER_IMG_Y - 20;

        regularFont.setColor(Color.WHITE);
        regularFont.draw(batch, "Tên: " + playerName, STATS_X, STATS_Y);

        // Player health bar
        regularFont.draw(batch, "HP: " + playerHealth + "/" + playerMaxHealth, STATS_X, STATS_Y - 20);
        drawHealthBar(batch, playerHealth, playerMaxHealth, STATS_X, STATS_Y - 45, width - 50, 12);

        // Player mana bar
        regularFont.draw(batch, "MP: " + playerMana + "/" + playerMaxMana, STATS_X, STATS_Y - 60);
        drawManaBar(batch, playerMana, playerMaxMana, STATS_X, STATS_Y - 85, width - 50, 12);
    }

    private void drawHealthBar(SpriteBatch batch, float current, float max, float x, float y, float width, float height) {
        // Background
        batch.setColor(0.3f, 0.3f, 0.3f, 1);
        batch.draw(whiteTexture, x, y, width, height);

        // Fill
        float percentage = current / max;
        batch.setColor(getHealthColor(percentage));
        batch.draw(whiteTexture, x, y, width * percentage, height);
    }

    private void drawManaBar(SpriteBatch batch, float current, float max, float x, float y, float width, float height) {
        // Background
        batch.setColor(0.3f, 0.3f, 0.3f, 1);
        batch.draw(whiteTexture, x, y, width, height);

        // Fill
        float percentage = current / max;
        batch.setColor(0.2f, 0.4f, 0.9f, 1);
        batch.draw(whiteTexture, x, y, width * percentage, height);
    }

    private void drawItemColumn(SpriteBatch batch, float x, float y, float width, float height) {
        // Draw column background
        batch.setColor(0.3f, 0.2f, 0.4f, 0.9f);
        batch.draw(whiteTexture, x, y, width, height);

        // Draw border
        batch.setColor(0.8f, 0.4f, 0.8f, 1);
        drawRect(batch, x, y, width, height, 2);

        // Column title
        batch.setColor(Color.WHITE);
        drawCenteredText(batch, regularFont, "🎒 ITEM", x + width / 2, y + height - 15, Color.MAGENTA);

        // Draw items
        drawItemInventory(batch, x + 10, y + 10, width - 20, height - 40);
    }

    private void drawLetterGridColumn(SpriteBatch batch, float x, float y, float width, float height) {
        // Draw column background
        batch.setColor(0.25f, 0.25f, 0.35f, 0.9f);
        batch.draw(whiteTexture, x, y, width, height);

        // Draw border
        batch.setColor(0.7f, 0.7f, 0.9f, 1);
        drawRect(batch, x, y, width, height, 2);

        // Current word display - each character in its own cell
        String currentWord = letterGrid.getCurrentWord();
        drawCurrentWordCells(batch, currentWord, x, y + height - 50, width);

        // Word meaning display
        if (isDrawingWordMeaning && !lastSubmittedWord.isEmpty()) {
            String meaning = wordValidator.getWordMeaning(lastSubmittedWord);
            if (meaning != null && !meaning.isEmpty()) {
                drawWordMeaning(batch, meaning, x + 10, y + height - 5, width - 20);
            }
        }

        // Letter grid (adjusted position)
        final float GRID_SIZE = 275;
        final float GRID_X = x + (width - GRID_SIZE) / 2;
        final float GRID_Y = y + 80;

        drawLetterGrid(batch, GRID_X, GRID_Y - 10, GRID_SIZE);

        // Action buttons
        final float BUTTON_WIDTH = 120;
        final float BUTTON_HEIGHT = 40;
        final float BUTTON_SPACING = 20;
        final float BUTTONS_START_X = x + (width - (2 * BUTTON_WIDTH + BUTTON_SPACING)) / 2;
        final float BUTTONS_Y = y + 20;

        submitButtonRect = new Rectangle(BUTTONS_START_X, BUTTONS_Y, BUTTON_WIDTH, BUTTON_HEIGHT);
        clearButtonRect = new Rectangle(BUTTONS_START_X + BUTTON_WIDTH + BUTTON_SPACING, BUTTONS_Y, BUTTON_WIDTH, BUTTON_HEIGHT);

        if (isPlayerTurn && !combatTimeUp) {
            drawButton(batch, submitButtonRect, "CAST SPELL");
            drawButton(batch, clearButtonRect, "CLEAR WORD");
        }
    }

    private void drawLetterGrid(SpriteBatch batch, float gridX, float gridY, float gridSize) {
        float cellSize = gridSize / 5;

        char[][] grid = letterGrid.getGrid();
        boolean[][] selected = letterGrid.getSelectedCells();

        // Reuse these objects instead of creating new ones per cell
        GlyphLayout reusableLayout = new GlyphLayout();
        Color letterColor = new Color();

        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                float screenX = gridX + x * cellSize;
                float screenY = gridY + (4 - y) * cellSize;

                char letter = grid[y][x];
                boolean isSelected = selected[y][x];
                boolean isVowel = VOWELS.indexOf(Character.toUpperCase(letter)) != -1;
                boolean isDisabled = isCellDisabled(x, y);

                // Choose appropriate cell texture
                Texture cellTexture;
                if (isDisabled) {
                    cellTexture = disabledCellTexture;
                } else if (isSelected) {
                    cellTexture = wordCellTexture;
                } else if (isVowel) {
                    cellTexture = vowelCellTexture;
                } else {
                    cellTexture = this.cellTexture;
                }

                // Draw cell background
                batch.setColor(Color.WHITE);
                batch.draw(cellTexture, screenX, screenY, cellSize, cellSize);

                // Draw additional highlight for selected cells
                if (isSelected && !isDisabled) {
                    batch.setColor(1.0f, 0.8f, 0.2f, 0.7f); // Golden glow
                    drawRect(batch, screenX + 2, screenY + 2, cellSize - 4, cellSize - 4, 3);
                    batch.setColor(Color.WHITE);
                }

                // Set the appropriate letter color (reusing color object)
                if (isDisabled) {
                    letterColor.set(0.5f, 0.5f, 0.5f, 1.0f); // Gray
                } else if (isSelected) {
                    letterColor.set(0.8f, 0.2f, 0.2f, 1.0f); // Bright red for selected letters
                } else if (isVowel) {
                    letterColor.set(0.2f, 0.2f, 0.8f, 1.0f); // Dark blue for vowels
                } else {
                    letterColor.set(0, 0, 0, 1); // Black
                }

                // Draw letter with appropriate color
                String letterStr = String.valueOf(letter);
                reusableLayout.setText(regularFont, letterStr);
                regularFont.setColor(letterColor);
                regularFont.draw(batch, letterStr,
                        screenX + (cellSize - reusableLayout.width) / 2,
                        screenY + cellSize - (cellSize - reusableLayout.height) / 2);
            }
        }
    }

    private void drawWordMeaning(SpriteBatch batch, String meaning, float x, float y, float maxWidth) {
        // Draw background for word meaning
        float meaningHeight = 40;
        batch.setColor(0.2f, 0.3f, 0.5f, 0.9f);
        batch.draw(whiteTexture, x, y - meaningHeight, maxWidth, meaningHeight);

        // Draw border
        batch.setColor(Color.CYAN);
        drawRect(batch, x, y - meaningHeight, maxWidth, meaningHeight, 1);

        // Draw text with wrapping
        regularFont.setColor(Color.CYAN);
        drawWrappedText(batch, regularFont, "✨ " + meaning, x + 5, y - 10, maxWidth - 10);
    }

    private void drawItemInventory(SpriteBatch batch, float x, float y, float width, float height) {
        // Clear previous item rectangles
        itemRectMap.clear();

        // Get character items
        Map<String, Integer> characterItems = gameController.getCharacter().getBuffItems();

        if (characterItems == null || characterItems.isEmpty()) {
            regularFont.setColor(Color.GRAY);
            regularFont.draw(batch, "Không có\nvật phẩm!", x + 10, y + height - 30);
            return;
        }

        // Display items
        final float ITEM_HEIGHT = 35;
        final int MAX_ITEMS_TO_SHOW = (int) ((height - 20) / ITEM_HEIGHT);
        int itemsShown = 0;
        float itemY = y + height - 30;

        // Get mouse position
        Vector3 mousePos = getTouchPosition();

        for (Map.Entry<String, Integer> entry : characterItems.entrySet()) {
            if (itemsShown >= MAX_ITEMS_TO_SHOW) break;

            String itemName = entry.getKey();
            int amount = entry.getValue();
            Items item = ItemLoader.getItemByName(itemName);

            if (item == null) continue;

            // Create item rectangle
            Rectangle itemRect = new Rectangle(x, itemY - 25, width, 30);
            itemRectMap.put(itemRect, item);

            // Check if hovered
            boolean isHovered = itemRect.contains(mousePos.x, mousePos.y);

            // Draw item background
            if (isHovered) {
                batch.setColor(0.5f, 0.5f, 0.7f, 0.8f);
                batch.draw(whiteTexture, itemRect.x, itemRect.y, itemRect.width, itemRect.height);
                hoveredItem = item;
            }

            // Draw item icon (small)
            Texture itemIcon = getTexture(item.getTexturePath());
            if (itemIcon != null) {
                batch.setColor(Color.WHITE);
                batch.draw(itemIcon, x + 5, itemY - 20, 20, 20);
            }

            // Draw item text
            regularFont.setColor(isHovered ? Color.YELLOW : Color.WHITE);
            String itemText = itemName.length() > 12 ? itemName.substring(0, 9) + "..." : itemName;
            regularFont.draw(batch, itemText, x + 30, itemY);
            regularFont.draw(batch, "x" + amount, x + width - 30, itemY);

            if (isHovered && isPlayerTurn) {
                regularFont.setColor(Color.GREEN);
                regularFont.draw(batch, "[USE]", x + width - 60, itemY - 15);
            }

            itemY -= ITEM_HEIGHT;
            itemsShown++;
        }

        // Show item tooltip
        if (hoveredItem != null) {
            drawItemTooltip(batch, mousePos.x, mousePos.y, hoveredItem);
            hoveredItem = null;
        }

        // Show scroll indicator if needed
        int remainingItems = characterItems.size() - MAX_ITEMS_TO_SHOW;
        if (remainingItems > 0) {
            regularFont.setColor(Color.GRAY);
            regularFont.draw(batch, "+" + remainingItems + " more", x + 10, y + 15);
        }
    }

    private void drawItemTooltip(SpriteBatch batch, float x, float y, Items item) {
        if (item == null) return;

        String effect = item.getItemEffect();
        float value = item.getManaCost();

        String tooltip = item.getItemName() + "\n" +
                "Hiệu quả: " + effect + "\n" +
                "Mana: " + value;

        float tooltipWidth = 200;
        float tooltipHeight = 80;
        float tooltipX = Math.min(x, viewport.getWorldWidth() - tooltipWidth - 10);
        float tooltipY = Math.max(y, tooltipHeight + 10);

        // Draw tooltip background
        batch.setColor(0.2f, 0.2f, 0.4f, 0.9f);
        batch.draw(whiteTexture, tooltipX, tooltipY - tooltipHeight, tooltipWidth, tooltipHeight);

        // Draw tooltip border
        batch.setColor(0.8f, 0.7f, 0.2f, 1);
        drawRect(batch, tooltipX, tooltipY - tooltipHeight, tooltipWidth, tooltipHeight, 1);

        // Draw tooltip text
        regularFont.setColor(Color.WHITE);
        drawWrappedText(batch, regularFont, tooltip, tooltipX + 10, tooltipY - 10, tooltipWidth - 20);
    }

    private void performEnemyAction() {
        int action = random.nextInt(10); // 0–9
        float damage = 0;

        if (action < 4) { // 40% normal attack
            damage = (random.nextInt(10) + 1) + currentLevel + enemy.getAttackPower();
            damage = damage - gameController.getCharacter().getDefend(); // Apply player's defense
            addCombatLog(enemyName + " tấn công gây " + damage + " sát thương!");
        } else if (action < 10) { // 40% power attack
            damage = (random.nextInt(5) + 1) + (currentLevel * enemy.getAttackPower());
            damage = damage - gameController.getCharacter().getDefend(); // Apply player's defense
            addCombatLog(enemyName + " tấn công mạnh gây " + damage + " sát thương!");
        } else if (action == 10) { // 10% chance to heal
            float heal = enemyHealth * 0.2f; // Heal 20% of current health
            enemyHealth = Math.min(enemyMaxHealth, enemyHealth + heal);
            addCombatLog(enemyName + " hồi phục " + heal + " máu!");
            damage = 0;
        } else { // action == 9: 10% chance to miss
            addCombatLog(enemyName + " đã trượt đòn tấn công!");
            damage = 0;
        }


        playerHealth = Math.max(0, playerHealth - damage);

        effectManager.spawnEffect("Starlight", 180, 470);
        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                checkCombatEnd();
            }
        }, 0.8f);

        if (isCombatMode) {
            isPlayerTurn = true;
            letterGrid.regenerateGrid();

            // Add turn change notification to combat log
            addCombatLog("---Đến lượt của bạn!---");

            // Apply boss effects when regenerating grid
            if (isEnemyBoss()) {
                applyBossEffects();
            }
        }
    }

    public boolean submitWord() {
        if (!active) return false;

        String word = letterGrid.getCurrentWord();
        if (word.isEmpty()) {
            addCombatLog("Từ phải có ít nhất 1 chữ cái!");
            return false;
        }

        if (gameController.getCharacter().getLearnedWords().contains(word.toUpperCase()) || wordValidator.isValidWord(word)) {
            int points = wordValidator.getTotalScore(word.trim());
            this.experienceGain += points;

            isDrawingWordMeaning = true;
            lastSubmittedWord = word;

            if (gameController.getCharacter().updateDict(word))
                gameController.getDictionaryView().addNewWord(word);

            if (isCombatMode && isPlayerTurn) {
                float damage = points + wordDamageMultiplier;

                // Apply Lord enemy effect
                if (isEnemyLord() && damage < 10) {
                    damage = 0;
                    addCombatLog("Lord " + enemyName + " chống chọi được đòn tấn công yếu!");
                } else {
                    damage = Math.min(enemyHealth, damage);
                    enemyHealth -= damage;
                    addCombatLog("Từ '" + word + "' gây " + damage + " sát thương!");
                }

                addCombatLog("+" + points + " điểm! " + damage + " sát thương!");
                effectManager.spawnEffect("Starlight", viewport.getWorldWidth() - 180, 440);

                achievementManager.updateProgress(Achievement.AchievementType.WORD_COUNT, 1);
                Timer.schedule(new Timer.Task() {
                    @Override
                    public void run() {
                        checkCombatEnd();
                    }
                }, 1.3f);

                if (isCombatMode && enemyHealth > 0) {
                    isPlayerTurn = false;
                    // Add turn change notification to combat log
                    addCombatLog("---Đến lượt của " + enemyName + "!---");
                }
            } else {
                addCombatLog("+" + points + " điểm!");
            }

            letterGrid.regenerateGrid();

            // Schedule meaning display to clear after a few seconds
            Timer.schedule(new Timer.Task() {
                @Override
                public void run() {
                    isDrawingWordMeaning = false;
                }
            }, 3.0f);

            return true;
        } else {
            addCombatLog("Từ '" + word + "' không hợp lệ!");
            gameController.getCharacter().updateWrongWordCount();
            if (isCombatMode && enemyHealth > 0) {
                isPlayerTurn = false;
                // Add turn change notification for invalid words too
                addCombatLog("---Đến lượt của " + enemyName + "!---");
            }
            return false;
        }
    }

    private void checkCombatEnd() {
        if (playerHealth <= 0 || combatTimeUp) {
            String message = combatTimeUp ? "Hết thời gian! Bạn đã thua!" : "Bạn bị đánh bại bởi " + enemyName + "!";
            addCombatLog(message);
            playerHealth = 0;
            endCombat(false);
            isGameOver = gameController.getCharacter().gameOver();
        } else if (enemyHealth <= 0) {
            addCombatLog("Bạn đã hạ gục " + enemyName + "!");
            enemyHealth = 0;
            endCombat(true);

            if (currentEvent != null && currentEvent.isOneTime()) {
                gameController.getEventManager().recordDefeatedEnemy(this.enemy.getEnemyID());
                gameController.getEventManager().completeEvent(currentEvent.getId());
                gameController.setEndEvent();
            }

            achievementManager.updateProgress(Achievement.AchievementType.COMBAT_WIN, 1);
            this.newLevel = gameController.getCharacter().expToLevelUp(this.experienceGain);
        }
    }

    private void cleanupCombatState() {
        isCombatMode = false;
        combatTimer = 0;
        combatTimeUp = false;
        disabledCells.clear();
        combatLogLines.clear();
        isDrawingWordMeaning = false;
        lastSubmittedWord = "";
    }

    private void endCombat(boolean victory) {
        isVictory = victory;
        cleanupCombatState();
    }

    public void startCombat(Enemy enemy) {
        // Clean up previous combat state
        cleanupCombatState();

        this.enemy = enemy;
        this.enemyName = enemy.getEnemyName();
        this.wordDamageMultiplier = gameController.getCharacter().getDamage();
        this.isVictory = false;
        this.experienceGain = 0;
        this.playerHealth = gameController.getCharacter().getHealth();
        this.playerMana = gameController.getCharacter().getMana();
        this.playerMaxMana = gameController.getCharacter().getMaxMana();
        this.playerMaxHealth = gameController.getCharacter().getMaxHealth();
        this.achievementManager = gameController.getAchievementManager();
        this.isCombatMode = true;
        this.isPlayerTurn = true;
        this.playerName = gameController.getCharacter().getName();
        this.currentLevel = gameController.getCharacter().getLevel();

        this.enemyMaxHealth = enemy.getHealth() + currentLevel*2;
        this.enemyHealth = enemy.getHealth() + currentLevel*2;

        addCombatLog("Bắt đầu chiến đấu với " + enemyName + "!");

        letterGrid.regenerateGrid();

        // Apply initial boss effects
        if (isEnemyBoss()) {
            applyBossEffects();
        }
    }

    // Core game functions
    public void activate() {
        this.active = true;
        letterGrid.regenerateGrid();
        currentScore = 0;
        currentMessage = "";
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
    }

    public void deactivate() {
        this.active = false;
        clearSelection();
    }

    // Letter grid interaction
    public boolean selectCell(int x, int y) {
        if (!active) return false;
        if (letterGrid.getCurrentWord().length()>12) {
            addCombatLog("Từ đã quá dài! Tối đa 10 chữ cái.");
            return false;
        }
        try {
            if (letterGrid.canSelect(x, y) && !isCellDisabled(x, y)) {
                letterGrid.selectCell(x, y);
                return true;
            }
            return false;
        } catch (IndexOutOfBoundsException e) {
            return false;
        }
    }

    public boolean deselectLastCell() {
        if (!active) return false;
        letterGrid.deselectLastCell();
        return true;
    }

    public void clearSelection() {
        letterGrid.clearSelection();
    }

    // Utility methods
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    public void dispose() {
        // Only dispose when completely shutting down
        active = false;
        disposeAllTextures();
    }

    private void disposeAllTextures() {
        // Dispose texture cache
        for (Texture texture : textureCache.values()) {
            if (texture != null) {
                texture.dispose();
            }
        }
        textureCache.clear();

        // Dispose UI textures
        if (whiteTexture != null) {
            whiteTexture.dispose();
            whiteTexture = null;
        }
        if (buttonTexture != null) {
            buttonTexture.dispose();
            buttonTexture = null;
        }
        if (buttonSelectedTexture != null) {
            buttonSelectedTexture.dispose();
            buttonSelectedTexture = null;
        }
        if (wordCellTexture != null) {
            wordCellTexture.dispose();
            wordCellTexture = null;
        }
        if (cellTexture != null) {
            cellTexture.dispose();
            cellTexture = null;
        }
        if (vowelCellTexture != null) {
            vowelCellTexture.dispose();
            vowelCellTexture = null;
        }
        if (disabledCellTexture != null) {
            disabledCellTexture.dispose();
            disabledCellTexture = null;
        }
    }

    private Texture getTexture(String path) {
        if (!textureCache.containsKey(path)) {
            try {
                textureCache.put(path, new Texture(Gdx.files.internal(path)));
            } catch (Exception e) {
                Gdx.app.error("GameplayController", "Failed to load texture: " + path, e);
                return whiteTexture; // Return fallback texture
            }
        }
        return textureCache.get(path);
    }

    // Helper methods
    private void drawCurrentWordCells(SpriteBatch batch, String currentWord, float columnX, float y, float columnWidth) {
        if (currentWord.isEmpty() && !isDrawingWordMeaning) {
            regularFont.setColor(Color.GRAY);
            drawCenteredText(batch, regularFont, "Chọn các chữ cái để tạo từ", columnX + columnWidth / 2, y + 7, Color.GRAY);
            return;
        }

        // Calculate cell dimensions
        final float CELL_SIZE = 35;
        final float CELL_SPACING = 5;
        final int MAX_CHARS_PER_ROW = Math.max(1, (int) ((columnWidth - 20) / (CELL_SIZE + CELL_SPACING)));

        int wordLength = currentWord.length();
        int rows = (int) Math.ceil((double) wordLength / MAX_CHARS_PER_ROW);
        float startY = y;

        for (int row = 0; row < rows; row++) {
            int startIndex = row * MAX_CHARS_PER_ROW;
            int endIndex = Math.min(startIndex + MAX_CHARS_PER_ROW, wordLength);
            int charsInThisRow = endIndex - startIndex;

            float totalRowWidth = (charsInThisRow * CELL_SIZE) + ((charsInThisRow - 1) * CELL_SPACING);
            float rowStartX = columnX + (columnWidth - totalRowWidth) / 2;

            for (int i = startIndex; i < endIndex; i++) {
                char letter = currentWord.charAt(i);
                float cellX = rowStartX + ((i - startIndex) * (CELL_SIZE + CELL_SPACING));
                float cellY = startY - (row * (CELL_SIZE + CELL_SPACING));

                drawCharacterCell(batch, letter, cellX, cellY, CELL_SIZE, i);
            }
        }
    }

    private void drawCharacterCell(SpriteBatch batch, char letter, float x, float y, float cellSize, int index) {
        boolean isVowel = VOWELS.indexOf(Character.toUpperCase(letter)) != -1;

        Texture cellTexture;
        Color letterColor;
        Color borderColor;

        if (isVowel) {
            cellTexture = vowelCellTexture;
            letterColor = new Color(0.2f, 0.2f, 0.8f, 1.0f);
            borderColor = new Color(0.4f, 0.4f, 1.0f, 1.0f);
        } else {
            cellTexture = wordCellTexture;
            letterColor = new Color(0.8f, 0.4f, 0.1f, 1.0f);
            borderColor = new Color(1.0f, 0.8f, 0.2f, 1.0f);
        }

        batch.setColor(Color.WHITE);
        batch.draw(cellTexture, x, y, cellSize, cellSize);

        float time = System.currentTimeMillis() * 0.003f;
        float pulseAlpha = 0.7f + 0.3f * (float) Math.sin(time + index * 0.5f);
        batch.setColor(borderColor.r, borderColor.g, borderColor.b, pulseAlpha);
        drawRect(batch, x, y, cellSize, cellSize, 2);

        String letterStr = String.valueOf(Character.toUpperCase(letter));
        layout.setText(regularFont, letterStr);
        regularFont.setColor(letterColor);
        regularFont.draw(batch, letterStr,
                x + (cellSize - layout.width) / 2,
                y + (cellSize + layout.height) / 2);

        batch.setColor(Color.WHITE);
    }

    private void drawRect(SpriteBatch batch, float x, float y, float width, float height, float thickness) {
        batch.draw(whiteTexture, x, y, width, thickness);
        batch.draw(whiteTexture, x, y, thickness, height);
        batch.draw(whiteTexture, x + width - thickness, y, thickness, height);
        batch.draw(whiteTexture, x, y + height - thickness, width, thickness);
    }

    private void drawCenteredText(SpriteBatch batch, BitmapFont font, String text, float x, float y, Color color) {
        layout.setText(font, text);
        font.setColor(color);
        font.draw(batch, text, x - layout.width / 2, y);
    }

    private void drawWrappedText(SpriteBatch batch, BitmapFont font, String text, float x, float y, float maxWidth) {
        String[] lines = text.split("\n");
        float lineHeight = font.getLineHeight();
        float currentY = y;

        for (String line : lines) {
            String[] words = line.split(" ");
            StringBuilder wrappedLine = new StringBuilder();

            for (String word : words) {
                String testLine = wrappedLine.length() == 0 ? word : wrappedLine + " " + word;
                GlyphLayout layout = new GlyphLayout(font, testLine);

                if (layout.width > maxWidth) {
                    font.draw(batch, wrappedLine.toString(), x, currentY);
                    currentY -= lineHeight + 5;
                    wrappedLine = new StringBuilder(word);
                } else {
                    wrappedLine = new StringBuilder(testLine);
                }
            }

            if (wrappedLine.length() > 0) {
                font.draw(batch, wrappedLine.toString(), x, currentY);
                currentY -= lineHeight + 5;
            }
        }
    }
}