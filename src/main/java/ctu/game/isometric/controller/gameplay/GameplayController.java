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
import ctu.game.isometric.controller.EffectManager;
import ctu.game.isometric.controller.GameController;
import ctu.game.isometric.model.entity.Enemy;
import ctu.game.isometric.model.game.GameState;
import ctu.game.isometric.model.game.Items;
import ctu.game.isometric.model.game.Reward;
import ctu.game.isometric.model.game.LetterGrid;
import ctu.game.isometric.model.world.MapEvent;
import ctu.game.isometric.util.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static ctu.game.isometric.util.FontGenerator.generateVietNameseFont;
import static ctu.game.isometric.util.WordNetValidator.calculateScore;
import static ctu.game.isometric.util.WordNetValidator.getTotalScore;


public class GameplayController {
    // Core components
    private final GameController gameController;
    private final LetterGrid letterGrid;
    private final Random random = new Random();

    // Game state
    private int currentScore;
    private boolean active;
    private String currentMessage = "";
    private float messageTimer = 0;

    // UI components
    private BitmapFont titleFont, regularFont, bigFont;
    private GlyphLayout layout;
    private Viewport viewport;
    private Texture whiteTexture;
    private Map<String, Texture> textureCache = new HashMap<>();

    private Texture gridBackgroundTexture;
    private Texture buttonTexture;
    private Texture buttonSelectedTexture;
    private Texture messageBoxTexture;
    private Texture cellTexture;
    private Texture selectedCellTexture;
    private Texture itemCellTexture;
    float playerMaxHealth = 100;
    private MapEvent currentEvent;

    // Button areas
    private Rectangle submitButtonRect, clearButtonRect, exitButtonRect;

    // Combat state
    private boolean isCombatMode = false;
    private boolean isPlayerTurn = true;

    private float playerHealth = 100;

    private float enemyHealth = 100;

    private float enemyMaxHealth = 100;

    private String enemyName = "Enemy";
    private float enemyActionTimer = 0;
    private static final float ENEMY_TURN_DELAY = 2.5f;
    private String combatLog = "";
    private float wordDamageMultiplier = 1f;
    private boolean autoStartCombat = false;
    private String playerName;
    private boolean isVictory = false;
    private EffectManager effectManager;
    private WordNetValidator wordValidator;

    public GameplayController(GameController gameController) {
        this.gameController = gameController;
        this.letterGrid = new LetterGrid();

        this.effectManager = gameController.getEffectManager();
        this.wordValidator = gameController.getWordNetValidator();
        this.playerName = gameController.getCharacter().getName();
        initializeUI();
    }

    private void initializeUI() {
        // Initialize fonts
        titleFont = generateVietNameseFont("Tektur-Bold.ttf", 18);
        regularFont = generateVietNameseFont("Tektur-Bold.ttf", 13);
        bigFont = regularFont;


        layout = new GlyphLayout();
        viewport = new FitViewport(1280, 720);

        // Create white texture for drawing colored rectangles
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        whiteTexture = new Texture(pixmap);
        pixmap.dispose();

        // Load UI textures
        gridBackgroundTexture = new Texture(Gdx.files.internal("ui/grid_bg.png"));
        buttonTexture = new Texture(Gdx.files.internal("ui/button.png"));
        buttonSelectedTexture = new Texture(Gdx.files.internal("ui/button_selected.png"));
        messageBoxTexture = new Texture(Gdx.files.internal("ui/message_box.png"));
        cellTexture = new Texture(Gdx.files.internal("ui/cell.png"));
        itemCellTexture = new Texture(Gdx.files.internal("ui/item_cell.png"));
        selectedCellTexture = new Texture(Gdx.files.internal("ui/selected_cell.png"));

        // Initialize button rectangles
//        submitButtonRect = new Rectangle(900, 350, 200, 50);
//        clearButtonRect = new Rectangle(900, 280, 200, 50);


    }

    private void drawMessageBox(SpriteBatch batch, String message, float x, float y, float width, float height) {
        // Draw background
        batch.setColor(Color.WHITE);
        batch.draw(messageBoxTexture, x - 14, y + 10, width + 60, height);

        // Setup positions
        float textX = x + 20;
        float textY = y + height - 80;
        float maxWidth = width - 40;
        float lineHeight = regularFont.getLineHeight() + 5;

        regularFont.setColor(Color.WHITE);

        // Split message into lines (in case of manual \n)
        String[] paragraphs = message.split("\n");

        for (String paragraph : paragraphs) {
            String[] words = paragraph.split(" ");
            StringBuilder line = new StringBuilder();

            for (String word : words) {
                String testLine = line.length() == 0 ? word : line + " " + word;
                layout.setText(regularFont, testLine);

                if (layout.width > maxWidth) {
                    // Draw current line and reset
                    regularFont.draw(batch, line.toString(), textX, textY);
                    textY -= lineHeight;
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(testLine);
                }
            }

            // Draw last line of paragraph
            if (line.length() > 0) {
                regularFont.draw(batch, line.toString(), textX, textY);
                textY -= lineHeight;
            }
        }
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

        // Update appropriate mode
        if (isCombatMode) updateCombat(delta);
    }

    private void spawnAttackEffect(float x, float y) {
        effectManager.spawnEffect("attack", x, y);
    }

    private void spawnRainEffect(float x, float y) {
        effectManager.spawnEffect("rain", x, y);
    }

    private void updateCombat(float delta) {
        // Process enemy turn
        if (!isPlayerTurn) {
            enemyActionTimer += delta;
            if (enemyActionTimer >= ENEMY_TURN_DELAY) {
                performEnemyAction();
                enemyActionTimer = 0;
            }
            return;
        }
    }

    public boolean handleCombatClick(float x, float ScreenY) {
        float y = Gdx.graphics.getHeight() - ScreenY;

        if (submitButtonRect.contains(x, y)) {
            submitWord();
            return true;
        } else if (clearButtonRect.contains(x, y)) {
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

    private void checkGridClick(float x, float y) {
        float gridSize = isCombatMode ? 350 : 500;
        float gridX = isCombatMode ? (viewport.getWorldWidth() - gridSize) / 2 : 250;
        float gridY = isCombatMode ? 60 : 110;
        float cellSize = gridSize / 5;

        if (x >= gridX && x < gridX + gridSize && y >= gridY && y < gridY + gridSize) {
            int cellX = (int) ((x - gridX) / cellSize);
            int cellY = 4 - (int) ((y - gridY) / cellSize);
            selectCell(cellX, cellY);
        }
    }

    public void render(SpriteBatch batch) {
        if (!active) return;

        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);

        // Draw background FIRST
        batch.setColor(0.1f, 0.1f, 0.2f, 1);
        batch.draw(whiteTexture, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
        batch.setColor(Color.WHITE);

        if (isCombatMode) renderCombatUI(batch);
        else if (isVictory) renderReward(batch);
        else gameController.setState(GameState.EXPLORING);

        effectManager.render(batch);

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
        batch.setColor(0.8f, 0.7f, 0.2f, 1); // Gold border
        drawRect(batch, panelX, panelY, panelWidth, panelHeight, 3);

        // Get reward information
        Reward reward = RewardLoader.getRewardById(this.enemy.getRewardID());
        Items item = reward.getItemID();

        // Title
        drawCenteredText(batch, titleFont, "CHIẾN THĂNG!", viewport.getWorldWidth() / 2, panelY + panelHeight - 50, new Color(1, 0.9f, 0.3f, 1));

        // Enemy defeated message
        drawCenteredText(batch, regularFont, "Bạn đã thua " + enemyName + "!",
                viewport.getWorldWidth() / 2, panelY + panelHeight - 100, Color.WHITE);
        Texture itemTexture = null;
        // Draw reward item
        if (item != null) {
            try {
                itemTexture = getCharacterTexture(item.getTexturePath());
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
            // Wrap long descriptions
            String description = reward.getDescription();
            float wrapWidth = panelWidth - 200;
            float lineHeight = 25;

            int startIndex = 0;
            int lastSpace = 0;
            float lineWidth = 0;

            for (int i = 0; i < description.length(); i++) {
                char c = description.charAt(i);
                layout.setText(regularFont, String.valueOf(c));
                lineWidth += layout.width;

                if (c == ' ') lastSpace = i;

                if (lineWidth > wrapWidth || i == description.length() - 1) {
                    int endIndex = (lineWidth > wrapWidth && lastSpace > startIndex) ? lastSpace : i + 1;
                    String line = description.substring(startIndex, endIndex);
                    regularFont.draw(batch, line, textX, textY - lineHeight);
                    lineHeight += 25;
                    startIndex = endIndex;
                    if (startIndex < description.length() && description.charAt(startIndex) == ' ') startIndex++;
                    lineWidth = 0;
                }
            }
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

                Timer.schedule(new Timer.Task() {
                    @Override
                    public void run() {
                        gameController.setState(GameState.EXPLORING);
                        dispose();
                    }
                }, 1.0f);
            }
        }

        batch.setColor(Color.WHITE); // Reset color
    }

    private void renderCombatUI(SpriteBatch batch) {
        // Draw battle background
        batch.setColor(0.15f, 0.15f, 0.3f, 1);
        batch.draw(whiteTexture, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());

        // Draw player and enemy
        drawCombatCharacter(batch, playerName, playerHealth, playerMaxHealth, 50, 600, true);
        drawCombatCharacter(batch, enemyName, (float) enemyHealth, (float) enemyMaxHealth, viewport.getWorldWidth() - 300, 600, false);

        // Draw combat log


        drawMessageBox(batch, combatLog, 900, 50, 300, 220);

        final float ENEMY_DESC_X = 920;
        final float ENEMY_DESC_Y = 250;
        final float ENEMY_DESC_WIDTH = 260;

// Check for null and draw with text wrapping
        if (this.enemy != null && this.enemy.getEnemyDescription() != null) {
            String description = this.enemy.getEnemyDescription();
            drawWrappedText(batch, bigFont, description, ENEMY_DESC_X, ENEMY_DESC_Y, ENEMY_DESC_WIDTH);
        }

        drawItemBox(batch, 50, 50, 300, 220);


        regularFont.setColor(Color.WHITE);
//        regularFont.draw(batch, combatLog, 80, 170);
        drawCenteredText(batch, regularFont, "Lượt của " + (isPlayerTurn ? "Bạn" : enemyName), viewport.getWorldWidth() / 2, 700, Color.WHITE);

        // Update and draw buttons
        float buttonX = ((viewport.getWorldWidth() - 70) / 2);
        float buttonY = 10;
        float buttonWidth = 130;
        float buttonHeight = 50;
        float buttonSpacing = 60;

        submitButtonRect = new Rectangle(buttonX - 90, buttonY, buttonWidth, buttonHeight);
        clearButtonRect = new Rectangle(buttonX + 32, buttonY, buttonWidth, buttonHeight);


        // Draw compact letter grid and word info during player turn
        if (isPlayerTurn) {
            drawCompactLetterGrid(batch);
            drawButton(batch, submitButtonRect, "CAST WORD");
            drawButton(batch, clearButtonRect, "CLEAR");
            String currentWord = letterGrid.getCurrentWord();
            if (currentWord.length() > 0) {
                drawCenteredText(batch, regularFont, "Spell: " + currentWord, viewport.getWorldWidth() / 2, 600, Color.WHITE);


                if (gameController.getCharacter().getLearnedWords().contains(currentWord.toUpperCase()) || wordValidator.isValidWord(currentWord)) {
                    drawCenteredText(batch, regularFont, wordValidator.getWordMeaning(currentWord),
                            viewport.getWorldWidth() / 2, 570, Color.WHITE);
                }
            }
        }
    }

    // Add these fields to the GameplayController class
    private Map<Rectangle, Items> itemRectMap = new HashMap<>();
    private Items hoveredItem = null;
    private String itemTooltip = "";

    private void drawItemBox(SpriteBatch batch, float x, float y, float width, float height) {
        // Clear previous item rectangles
        itemRectMap.clear();

        // Draw background
        batch.setColor(Color.WHITE);
        batch.draw(messageBoxTexture, x - 14, y + 10, width + 60, height);

        // Setup positions
        float textX = x + 60;
        float textY = y + height - 20;

        // Title
        regularFont.setColor(Color.WHITE);
        textY -= 25;

        // Get character items
        Map<String, Integer> characterItems = gameController.getCharacter().getBuffItems();

        if (characterItems == null || characterItems.isEmpty()) {
            regularFont.draw(batch, "Không có vật phẩm!", textX, textY);
            return;
        }

        // Display items
        final float itemHeight = 40;
        final float itemCellWidth = width - 40;
        final int maxItemsToShow = 5;
        int itemsShown = 0;

        // Get mouse position
        Vector3 mousePos = getTouchPosition();

        for (Map.Entry<String, Integer> entry : characterItems.entrySet()) {
            if (itemsShown >= maxItemsToShow) break;

            String itemName = entry.getKey();
            int amount = entry.getValue();
            Items item = ItemLoader.getItemByName(itemName);

            if (item == null) continue;

            // Create item cell rectangle
            Rectangle itemRect = new Rectangle(x + 10, textY - 35, itemCellWidth, 35);
            itemRectMap.put(itemRect, item);

            // Draw item cell background
            batch.setColor(Color.WHITE);
            boolean isHovered = itemRect.contains(mousePos.x, mousePos.y);
            if (isHovered) {
                batch.setColor(0.9f, 0.9f, 1.0f, 1.0f);
                hoveredItem = item;
            }

            batch.draw(itemCellTexture, itemRect.x, itemRect.y, itemRect.width, itemRect.height);
            batch.setColor(Color.WHITE);

            // Draw item icon
            Texture itemIcon = item != null ? getItemIcon(item.getTexturePath()) : null;
            if (itemIcon != null) {
                batch.draw(itemIcon, x + 20, textY - 30, 32, 32);
            }

            // Draw item name and amount
            regularFont.setColor(isHovered ? Color.YELLOW : Color.WHITE);
            regularFont.draw(batch, itemName + " x" + amount, textX, textY);

            if (isHovered) {
                regularFont.setColor(Color.GREEN);
                regularFont.draw(batch, "[DÙNG]", textX + 120, textY);
            }

            textY -= itemHeight;
            itemsShown++;
        }

        // Show indicator if there are more items
        int remainingItems = characterItems.size() - maxItemsToShow;
        if (remainingItems > 0) {
            regularFont.setColor(Color.WHITE);
            regularFont.draw(batch, "... và " + remainingItems + " thêm", textX, textY);
        }

        // Display tooltip for hovered item
        if (hoveredItem != null) {
            drawItemTooltip(batch, mousePos.x, mousePos.y, hoveredItem);
            hoveredItem = null;
        }
    }

    public Texture getItemIcon(String itemPath) {
        if (!textureCache.containsKey(itemPath)) {
            textureCache.put(itemPath, new Texture(Gdx.files.internal(itemPath)));
        }
        return textureCache.get(itemPath);

    }

    private void drawItemTooltip(SpriteBatch batch, float x, float y, Items item) {
        if (item == null) return;

        String effect = item.getItemEffect();
        float value = item.getValue();

        String tooltip = item.getItemName() + "\n" +
                "Hiệu quả: " + effect + "\n" +
                "Chỉ số: " + value;

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

    // Add this method to handle item usage
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

        // Remove one of this item from inventory
        Map<String, Integer> items = gameController.getCharacter().getItems();
        if (items.containsKey(item.getItemName()) && items.get(item.getItemName()) > 0) {
            // Apply effect based on item type
            switch (item.getItemEffect()) {
                case "heal":
                    playerHealth = Math.min(playerMaxHealth, playerHealth + item.getValue());
                    showMessage("Đã dùng " + item.getItemName() + "! HỒI " + item.getValue() + " Sinh Lực!");
                    break;
                case "buff":
                    wordDamageMultiplier += item.getValue();
                    showMessage("Đã dùng " + item.getItemName() + "! MẠNH MẼ!");
                    break;
                default:
                    showMessage("Đã dùng " + item.getItemName() + "!");
                    break;
            }

            // Reduce item count
            int newCount = items.get(item.getItemName()) - 1;
            if (newCount <= 0) {
                items.remove(item.getItemName());
            } else {
                items.put(item.getItemName(), newCount);
            }

            // End player's turn after using an item
            combatLog += "\nBạn đã dùng " + item.getItemName() + ".\nTới Lượt Của Kẻ Địch!";
            isPlayerTurn = false;
        }
    }


    // Update your touchUp handling in a method like updateCombat to include:


    private void drawCenteredText(SpriteBatch batch, BitmapFont font, String text, float x, float y, Color color) {
        layout.setText(font, text);
        font.setColor(color);
        font.draw(batch, text, x - layout.width / 2, y);
    }

    private void drawCombatCharacter(SpriteBatch batch, String name, float currentHealth,
                                     float maxHealth, float x, float y, boolean isPlayer) {
        // Draw character image
        batch.setColor(1, 1, 1, 1);
        Texture characterTexture = getCharacterTexture(isPlayer ? "characters/player.png" : this.enemy.getTexturePath());
        if (characterTexture != null) {
            float imgSize = 150;
            batch.draw(characterTexture, x + 60, y - imgSize - 100, imgSize, imgSize);
        }

        drawPokemonHealthBar(batch, name, currentHealth, maxHealth, x, y);
    }

    private void drawCompactLetterGrid(SpriteBatch batch) {
        float gridSize = 350;
        float cellSize = gridSize / 5;
        float gridX = (viewport.getWorldWidth() - gridSize) / 2;
        float gridY = 60;

        // Draw grid background
        batch.setColor(Color.WHITE);
        batch.draw(gridBackgroundTexture, gridX - 16, gridY - 48, 380, 450);

        char[][] grid = letterGrid.getGrid();
        boolean[][] selected = letterGrid.getSelectedCells();

        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                float screenX = gridX + x * cellSize;
                float screenY = gridY + (4 - y) * cellSize;

                // Draw cell background
                batch.setColor(Color.WHITE);
                if (selected[y][x]) {
                    batch.draw(selectedCellTexture, screenX, screenY, cellSize, cellSize);
                } else {
                    batch.draw(cellTexture, screenX, screenY, cellSize, cellSize);
                }

                // Draw letter
                String letter = String.valueOf(grid[y][x]);
                layout.setText(regularFont, letter);
                regularFont.setColor(Color.BLACK);
                regularFont.draw(batch, letter,
                        screenX + (cellSize - layout.width) / 2,
                        screenY + cellSize - (cellSize - layout.height) / 2);
            }
        }
        batch.setColor(Color.WHITE);
    }

    private Texture getCharacterTexture(String name) {
        if (!textureCache.containsKey(name)) {
            textureCache.put(name, new Texture(Gdx.files.internal(name)));
        }
        return textureCache.get(name);
    }

    private void drawPokemonHealthBar(SpriteBatch batch, String name, float current, float max, float x, float y) {
        // Name tag background
        batch.setColor(0.2f, 0.2f, 0.2f, 0.8f);
        batch.draw(whiteTexture, x, y, 250, 50);

        // Border
        batch.setColor(0.8f, 0.8f, 0.8f, 1);
        drawRect(batch, x, y, 250, 50, 2);

        // Name and HP label
        regularFont.setColor(Color.WHITE);
        regularFont.draw(batch, name, x + 10, y + 40);
        regularFont.draw(batch, "HP:", x + 8, y + 20);

        // Health bar background
        batch.setColor(0.3f, 0.3f, 0.3f, 1);
        batch.draw(whiteTexture, x + 40, y + 10, 180, 10);

        // Health bar fill
        float healthPercentage = (float) current / max;
        batch.setColor(getHealthColor(healthPercentage));
        batch.draw(whiteTexture, x + 40, y + 10, 180 * healthPercentage, 10);
    }

    private Color getHealthColor(float percentage) {
        if (percentage > 0.5f) return new Color(0.3f, 0.9f, 0.3f, 1);
        else if (percentage > 0.2f) return new Color(0.9f, 0.9f, 0.2f, 1);
        else return new Color(0.9f, 0.2f, 0.2f, 1);
    }

    // Helper to draw rectangle borders
    private void drawRect(SpriteBatch batch, float x, float y, float width, float height, float thickness) {
        batch.draw(whiteTexture, x, y, width, thickness); // Bottom
        batch.draw(whiteTexture, x, y, thickness, height); // Left
        batch.draw(whiteTexture, x + width - thickness, y, thickness, height); // Right
        batch.draw(whiteTexture, x, y + height - thickness, width, thickness); // Top
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

    private void performEnemyAction() {
        float damage = (random.nextInt(8) + 3) * enemy.getAttackPower();
        playerHealth -= damage;

        int action = random.nextInt(10);
        if (action < 7) { // 70% normal attack
            combatLog = enemyName + " attack for " + damage + " damage!\n" + "Your current health is " + this.playerHealth + ".";
        } else if (action < 9) { // 20% power attack
            int extraDamage = random.nextInt(5) + 1;
            playerHealth -= extraDamage;
            combatLog = enemyName + " performs a power attack for " + (damage + extraDamage) + " damage!\n" + "Your current health is " + this.playerHealth + ".";

        } else { // 10% heal
            int heal = random.nextInt(8) + 3;
            enemyHealth = Math.min(enemyMaxHealth, enemyHealth + heal);
            combatLog = enemyName + " recovers " + heal + " health!";
        }
        effectManager.spawnEffect("attack", viewport.getWorldWidth() - 300, 600);
        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                checkCombatEnd();

            }
        }, 0.5f);
        if (isCombatMode) {
            isPlayerTurn = true;
            letterGrid.regenerateGrid();
        }
    }

    private void checkCombatEnd() {
        if (playerHealth <= 0) {
            combatLog = "Bạn bị đánh bại bởi " + enemyName + "!";
            playerHealth = 0;
            endCombat(false);
        } else if (enemyHealth <= 0) {
            combatLog = "Bạn đã hạ gục " + enemyName + "!";
            enemyHealth = 0;
            endCombat(true);
            if (currentEvent.isOneTime()) {
                gameController.getEventManager().recordDefeatedEnemy(this.enemy.getEnemyID());
                gameController.getEventManager().completeEvent(currentEvent.getId());
                gameController.setEndEvent();
            }
        }
    }

    private void endCombat(boolean victory) {
        isCombatMode = false;
        isVictory = victory;
        wordDamageMultiplier = gameController.getCharacter().getDamage();
    }

    public boolean submitWord() {
        if (!active) return false;

        String word = letterGrid.getCurrentWord();
        if (word.length() < 3) {
            showMessage("Words must be at least 3 letters long!");
            return false;
        }

        if (gameController.getCharacter().getLearnedWords().contains(word.toUpperCase()) || wordValidator.isValidWord(word)) {
            int points = getTotalScore(wordValidator.getWordDetails(word));
            System.out.println("Word: " + word + ", Points: " + points);

            if (gameController.getCharacter().addLearnedWord(word))
                gameController.getDictionaryView().addNewWord(word);


            if (isCombatMode && isPlayerTurn) {
                float damage = points * wordDamageMultiplier;
                if (enemyHealth <= 0) {
                    damage = 0;
                }
                if (damage > enemyHealth) {
                    damage = enemyHealth;
                }
                enemyHealth -= damage;
                combatLog = "Your word '" + word + "' deals " + damage + " damage!";
                showMessage("+" + points + " points! " + damage + " damage!");
                // Spawn attack particle effect
                effectManager.spawnEffect("attack", viewport.getWorldWidth() - 300, 600);

                Timer.schedule(new Timer.Task() {
                    @Override
                    public void run() {
                        checkCombatEnd();

                    }
                }, 0.5f);

                if (isCombatMode && enemyHealth > 0) {
                    isPlayerTurn = false;
                }
            } else {
                showMessage("+" + points + " points!");
            }

            letterGrid.regenerateGrid();
            return true;
        } else {
            showMessage("Not a valid word!");
            return false;
        }
    }


    private Enemy enemy;

    public void startCombat(Enemy enemy) {
        this.enemy = enemy;
        this.enemyName = enemy.getEnemyName();
        this.enemyMaxHealth = enemy.getHealth();
        this.enemyHealth = enemy.getHealth();
        this.wordDamageMultiplier = gameController.getCharacter().getDamage();

        this.playerHealth = gameController.getCharacter().getHealth();
        this.playerMaxHealth = gameController.getCharacter().getMaxHealth();

        this.isCombatMode = true;
        this.isPlayerTurn = true;
        this.combatLog = "Bắt đầu cạnh tranh với " + enemyName + "!";
        letterGrid.regenerateGrid();
    }

    public MapEvent getCurrentEvent() {
        return currentEvent;
    }

    public void setCurrentEvent(MapEvent currentEvent) {
        this.currentEvent = currentEvent;
    }

    // Core game functions
    public void activate() {
        this.active = true;
        letterGrid.regenerateGrid();
        currentScore = 0;
        currentMessage = "";
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        if (autoStartCombat) {
            startCombat(new Enemy());
        }
    }

    public void deactivate() {
        this.active = false;
        clearSelection();
    }

    // Letter grid interaction
    public boolean selectCell(int x, int y) {
        if (!active) return false;
        try {
            if (letterGrid.canSelect(x, y)) {
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

    private void showMessage(String message) {
        currentMessage = message;
        messageTimer = 3.0f;
    }

    // Utility methods
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    public void dispose() {
        active = false;
        for (Texture texture : textureCache.values()) {
            texture.dispose();
        }
        textureCache.clear();

//        if (effectManager != null) {
//            effectManager.dispose();
//        }
    }

    // Getters
    public boolean isInCombatMode() {
        return isCombatMode;
    }

    public float getPlayerHealth() {
        return playerHealth;
    }

    public float getEnemyHealth() {
        return enemyHealth;
    }

    public LetterGrid getLetterGrid() {
        return letterGrid;
    }

    public int getCurrentScore() {
        return currentScore;
    }

    public String getCurrentWord() {
        return letterGrid.getCurrentWord();
    }

    public boolean isActive() {
        return active;
    }
}