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
import ctu.game.isometric.animation.AttackCard;
import ctu.game.isometric.animation.CardAnimationManager;
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
    // Core constants
    private static final float ENEMY_TURN_DELAY = 1.2f;
    private static final float COMBAT_TIME_LIMIT = 300f;
    private static final String VOWELS = "AEIOU";
    private static final int MAX_COMBAT_LOG_LINES = 20;

    // Core components
    private final GameController gameController;
    private final LetterGrid letterGrid;
    private final Random random = new Random();
    private EffectManager effectManager;
    private WordNetValidator wordValidator;
    private AchievementManager achievementManager;

    // Game state
    private boolean active;
    private boolean isGameOver = false;
    private int currentLevel = 1;
    private int newLevel = 1;
    private float experienceGain = 0;

    // Combat state
    private boolean isCombatMode = false;
    private boolean isPlayerTurn = true;
    private float combatTimer = 0;
    private boolean combatTimeUp = false;
    private float enemyActionTimer = 0;
    private List<String> combatLogLines = new ArrayList<>();
    private boolean isVictory = false;
    private boolean isDrawingWordMeaning = false;
    private String lastSubmittedWord = "";

    // UI components
    private BitmapFont titleFont, regularFont;
    private GlyphLayout layout;
    private Viewport viewport;
    private Texture whiteTexture;
    private Map<String, Texture> textureCache = new HashMap<>();

    // UI textures
    private Texture buttonTexture, buttonSelectedTexture, cellTexture, vowelCellTexture, wordCellTexture, disabledCellTexture;

    // Boss mechanics
    private Set<Integer> disabledCells = new HashSet<>();

    // Player/Enemy stats
    private float playerMaxHealth = 100, playerHealth = 100, playerMana = 100, playerMaxMana = 100;
    private String playerName = "Player";
    private float enemyHealth = 100, enemyMaxHealth = 100;
    private String enemyName = "Enemy";
    private Enemy enemy;

    // Button areas
    private Rectangle submitButtonRect, clearButtonRect;

    // Combat mechanics
    private float wordDamageMultiplier = 1f;
    private Map<Rectangle, Items> itemRectMap = new HashMap<>();
    private Items hoveredItem = null;
    private MapEvent currentEvent;

    // Grid constants
    private float gridX, gridY, gridSize, cellSize;
    private CardAnimationManager cardAnimationManager;

    public GameplayController(GameController gameController) {
        this.gameController = gameController;
        this.letterGrid = new LetterGrid();
        this.effectManager = gameController.getEffectManager();
        this.wordValidator = gameController.getWordNetValidator();
        this.achievementManager = gameController.getAchievementManager();

        cardAnimationManager = new CardAnimationManager();
        initializeUI();

        initializeGridConstants();
    }

    private void initializeUI() {
        titleFont = generateVietNameseFont("Tektur-Bold.ttf", 18);
        regularFont = generateVietNameseFont("Tektur-Bold.ttf", 13);
        layout = new GlyphLayout();
        viewport = new FitViewport(1280, 720);
        createWhiteTexture();
        loadUITextures();
        createSpecialCellTextures();
    }

    // Gọi khi có hành động (ví dụ: player tấn công)

    public void playerAttack(String word, int dmg, Runnable onComplete) {
        AttackCard card = new AttackCard(
                AttackCard.CardType.ATTACK,
                word,
                dmg,
                600, 280, 600, 380, 600, 550
        );

        card.setSFXCallback(() -> effectManager.playClickSound());
        card.setOnComplete(onComplete);
        cardAnimationManager.addCard(card);
    }

    public void playerHealing(int heal, Runnable onComplete){
        AttackCard card = new AttackCard(
                AttackCard.CardType.HEALING,
                "HEAL",
                heal,
                620, 550, 620, 550, 620, 550
        );
        card.setSFXCallback(() -> effectManager.playClickSound());
        card.setOnComplete(onComplete);
        cardAnimationManager.addCard(card);
    }
    public void playerHealingMana(int mana, Runnable onComplete) {
        AttackCard card = new AttackCard(
                AttackCard.CardType.HEALING,
                "MANA",
                mana,
                620, 550, 620, 550, 620, 550
        );
        card.setSFXCallback(() -> effectManager.playClickSound());
        card.setOnComplete(onComplete);
        cardAnimationManager.addCard(card);
    }

    public void playerDefend(int defend, Runnable onComplete) {
        AttackCard card = new AttackCard(
                AttackCard.CardType.SHIELD,
                "SHIELD",
                defend,
                600, 580, 400, 450, 90, 270
        );
        card.setSFXCallback(() -> effectManager.playClickSound());
        card.setOnComplete(onComplete);
        cardAnimationManager.addCard(card);
    }

    public void playerBuff(int buff, Runnable onComplete) {
        AttackCard card = new AttackCard(
                AttackCard.CardType.SPECIAL,
                "BUFF",
                buff,
                600, 580, 400, 450, 90, 270
        );
        card.setSFXCallback(() -> effectManager.playClickSound());
        card.setOnComplete(onComplete);
        cardAnimationManager.addCard(card);
    }

    public void enemyAttack(int dmg, Runnable onComplete) {
        AttackCard card;
        if (dmg == 0) {
            card = new AttackCard(
                    AttackCard.CardType.HEALING,
                    "",
                    dmg,
                    620, 550, 620, 550, 620, 550
            );

        } else {
            card = new AttackCard(
                    AttackCard.CardType.ATTACK,
                    "",
                    dmg,
                    600, 580, 400, 450, 90, 270
            );
            card.setSFXCallback(() -> effectManager.playClickSound());
        }
        card.setOnComplete(onComplete);
        cardAnimationManager.addCard(card);
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
            buttonTexture = new Texture(Gdx.files.internal("ui/button.png"));
            buttonSelectedTexture = new Texture(Gdx.files.internal("ui/button_selected.png"));
            wordCellTexture = new Texture(Gdx.files.internal("ui/cell.png"));
        } catch (Exception e) {
            buttonTexture = createFallbackTexture(100, 40, Color.GRAY);
            buttonSelectedTexture = createFallbackTexture(100, 40, Color.LIGHT_GRAY);
            wordCellTexture = createFallbackTexture(64, 64, Color.WHITE);
        }
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
        cardAnimationManager.update(delta);
        if (isCombatMode) updateCombat(delta);
    }

    private void updateCombat(float delta) {
        combatTimer += delta;
        if (combatTimer >= COMBAT_TIME_LIMIT && !combatTimeUp) {
            combatTimeUp = true;
            addCombatLog("Thời gian hết! Trận đấu kết thúc!");
            endCombat(false);
            return;
        }

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
        handleCombatLogScroll(1); // Thêm dòng này
        if (combatLogLines.size() > MAX_COMBAT_LOG_LINES) {
            combatLogLines.remove(0);
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
            disabledCells.clear();
            int cellsToDisable = random.nextInt(2) + 1;
            for (int i = 0; i < cellsToDisable; i++) {
                int cellIndex;
                do {
                    cellIndex = random.nextInt(25);
                } while (disabledCells.contains(cellIndex));
                disabledCells.add(cellIndex);
            }
        }
    }

    private boolean isCellDisabled(int x, int y) {
        return disabledCells.contains(y * 5 + x);
    }

    private float combatLogScrollOffset = 0;
    private float maxCombatLogScrollOffset = 0;
    private boolean isCombatLogScrollable = false;
    private static final float SCROLL_SPEED = 20f;

    // Thêm method này:
    public boolean handleCombatLogScroll(float scrollAmount) {
        if (!isCombatLogScrollable) return false;

        float lineHeight = regularFont.getLineHeight() + 2;
        float scrollDelta = scrollAmount * lineHeight;

        float oldOffset = combatLogScrollOffset;
        combatLogScrollOffset = Math.max(0, Math.min(maxCombatLogScrollOffset, combatLogScrollOffset + scrollDelta));

        return oldOffset != combatLogScrollOffset;
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
        if (item == null || !isPlayerTurn || !isCombatMode) return;

        if (playerMana < item.getManaCost()) {
            addCombatLog("Không đủ mana để sử dụng " + item.getItemName() + "!");
            return;
        }

        Map<String, Integer> items = gameController.getCharacter().getItems();
        if (items.containsKey(item.getItemName()) && items.get(item.getItemName()) > 0) {
            switch (item.getItemName()) {
                case "Elixir":
                    playerHealth = Math.min(playerMaxHealth, playerHealth + item.getValue());
                    addCombatLog("Đã hồi " + item.getValue() + " Sinh Lực!");
                    break;
                case "Arcane Essence":
                    playerMana = Math.min(playerMaxMana, playerMana + item.getValue());
                    addCombatLog("Đã hồi " + item.getValue() + " Năng lượng!");
                    break;
                case "Draught of Fury":
                    wordDamageMultiplier += item.getValue();
                    gameController.getCharacter().upAttack(item.getValue());
                    addCombatLog("Đã tăng 1 sức mạnh!");
                    break;
                case "Aegis Brew":
                    gameController.getCharacter().upDefend(item.getValue());
                    addCombatLog("Đã tăng 2 phòng thủ!");
                    break;
                case "Toxic Poiton":
                    enemyHealth = Math.max(0, enemyHealth - item.getValue());
                    addCombatLog("Kẻ địch đã bị trúng độc, mất " + item.getValue() + " máu!");
                    break;
            }

            playerMana = Math.max(0, playerMana - item.getManaCost());
            int newCount = items.get(item.getItemName()) - 1;
            if (newCount <= 0) items.remove(item.getItemName());
            else items.put(item.getItemName(), newCount);

            isPlayerTurn = false;
        }
    }

    private void initializeGridConstants() {
        gridSize = 275;

        // Tính toán grid position dựa trên layout columns
        final float MARGIN = 20;
        final float PLAYER_COLUMN_WIDTH = 280;
        final float GRID_COLUMN_WIDTH = 1280 - 280 - 220 - 4 * MARGIN; // Screen width - player width - item width - margins
        final float GRID_COLUMN_X = MARGIN + PLAYER_COLUMN_WIDTH + MARGIN;

        // Center grid trong grid column
        gridX = GRID_COLUMN_X + (GRID_COLUMN_WIDTH - gridSize) / 2;
        gridY = 80; // Margin bottom + button height + spacing
        cellSize = gridSize / 5;
    }

    private void checkGridClick(float x, float y) {
        if (x < gridX || x >= gridX + gridSize || y < gridY || y >= gridY + gridSize) return;

        int cellX = (int) ((x - gridX) / cellSize);
        int cellY = 4 - (int) ((y - gridY) / cellSize);

        if (cellX >= 0 && cellX < 5 && cellY >= 0 && cellY < 5 && !isCellDisabled(cellX, cellY)) {
            selectCell(cellX, cellY);
        }
    }


    public void render(SpriteBatch batch) {
        if (!active) return;

        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);

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
        cardAnimationManager.render(batch);

    }

    public void renderGameOver(SpriteBatch batch) {
        if (!isGameOver) {
            gameController.setState(GameState.EXPLORING);
        } else {
            drawCenteredText(batch, regularFont, "Game Over!", viewport.getWorldWidth() / 2, viewport.getWorldHeight() / 2, Color.RED);
        }
    }

    private void renderReward(SpriteBatch batch) {
        float panelWidth = 600, panelHeight = 400;
        float panelX = (viewport.getWorldWidth() - panelWidth) / 2;
        float panelY = (viewport.getWorldHeight() - panelHeight) / 2;

        batch.setColor(0.2f, 0.2f, 0.4f, 0.9f);
        batch.draw(whiteTexture, panelX, panelY, panelWidth, panelHeight);

        drawCenteredText(batch, titleFont, "CHIẾN THẮNG!", viewport.getWorldWidth() / 2, panelY + panelHeight - 50, Color.GOLD);

        Reward reward = RewardLoader.getRewardById(this.enemy.getRewardID());
        Items item = ItemLoader.getItemById(reward.getItemID());

        if (item != null) {
            Texture itemTexture = getTexture(item.getTexturePath());
            if (itemTexture != null) {
                batch.setColor(Color.WHITE);
                batch.draw(itemTexture, panelX + 100, panelY + panelHeight / 2 - 32, 64, 64);
            }
            regularFont.setColor(Color.YELLOW);
            regularFont.draw(batch, item.getItemName() + " x" + reward.getAmount(), panelX + 180, panelY + panelHeight / 2 + 30);
            regularFont.draw(batch, reward.getDescription(), panelX + 180, panelY + panelHeight / 2);
        }

        Rectangle continueButton = new Rectangle(viewport.getWorldWidth() / 2 - 100, panelY + 50, 200, 50);
        drawButton(batch, continueButton, "Tiếp tục");

        if (Gdx.input.justTouched()) {
            Vector3 touchPos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            viewport.unproject(touchPos);
            if (continueButton.contains(touchPos.x, touchPos.y)) {
                gameController.getCharacter().addItem(item, reward.getAmount());
                gameController.getCharacter().setHealth(playerHealth);
                gameController.getCharacter().setMana(playerMana);
                Timer.schedule(new Timer.Task() {
                    @Override
                    public void run() {
                        gameController.setState(GameState.EXPLORING);
                        if (newLevel > currentLevel) gameController.showLevelUpNotification();
                        cleanupCombatState();
                    }
                }, 0.5f);
            }
        }
    }

    private void renderCombatUI(SpriteBatch batch) {
        final float MARGIN = 20;
        final float SCREEN_WIDTH = viewport.getWorldWidth();
        final float SCREEN_HEIGHT = viewport.getWorldHeight();

        // Enemy status section (top)
        final float ENEMY_SECTION_HEIGHT = 120;
        final float ENEMY_SECTION_Y = SCREEN_HEIGHT - ENEMY_SECTION_HEIGHT - MARGIN;
        drawEnemyStatusSection(batch, MARGIN, ENEMY_SECTION_Y, SCREEN_WIDTH - 2 * MARGIN, ENEMY_SECTION_HEIGHT);

        // Combat log section (middle)
        final float LOG_SECTION_HEIGHT = 100;
        final float LOG_SECTION_Y = ENEMY_SECTION_Y - LOG_SECTION_HEIGHT - MARGIN;
        drawCombatLogSection(batch, MARGIN, LOG_SECTION_Y, SCREEN_WIDTH - 2 * MARGIN, LOG_SECTION_HEIGHT);

        // Bottom section - Three columns
        final float BOTTOM_SECTION_Y = MARGIN;
        final float BOTTOM_SECTION_HEIGHT = LOG_SECTION_Y - 2 * MARGIN;

        // Tính toán lại column widths để cân đối hơn
        final float PLAYER_COLUMN_WIDTH = 280;  // Tăng từ 250
        final float ITEM_COLUMN_WIDTH = 220;    // Tăng từ 200
        final float GRID_COLUMN_WIDTH = SCREEN_WIDTH - PLAYER_COLUMN_WIDTH - ITEM_COLUMN_WIDTH - 4 * MARGIN;

        // Column positions - điều chỉnh để không bị overlap
        final float PLAYER_COLUMN_X = MARGIN;
        final float GRID_COLUMN_X = PLAYER_COLUMN_X + PLAYER_COLUMN_WIDTH + MARGIN;
        final float ITEM_COLUMN_X = GRID_COLUMN_X + GRID_COLUMN_WIDTH + MARGIN;

        // Draw three columns với kích thước đã điều chỉnh
        drawPlayerStatusColumn(batch, PLAYER_COLUMN_X, BOTTOM_SECTION_Y, PLAYER_COLUMN_WIDTH, BOTTOM_SECTION_HEIGHT);
        drawLetterGridColumn(batch, GRID_COLUMN_X, BOTTOM_SECTION_Y, GRID_COLUMN_WIDTH, BOTTOM_SECTION_HEIGHT);
        drawItemColumn(batch, ITEM_COLUMN_X, BOTTOM_SECTION_Y, ITEM_COLUMN_WIDTH, BOTTOM_SECTION_HEIGHT);
    }

    private void drawEnemyStatusSection(SpriteBatch batch, float x, float y, float width, float height) {
        batch.setColor(0.2f, 0.2f, 0.4f, 0.9f);
        batch.draw(whiteTexture, x, y, width, height);

        String titleText = "KẺ ĐỊCH";
        if (isEnemyBoss()) titleText += " (BOSS)";
        else if (isEnemyLord()) titleText += " (LORD)";

        drawCenteredText(batch, titleFont, titleText, x + 156, y + height - 20, Color.RED);

        Texture enemyTexture = getTexture(this.enemy.getTexturePath());
        if (enemyTexture != null) {
            batch.setColor(Color.WHITE);
//            x +20
            batch.draw(enemyTexture, 600, y + 20, 80, 80);
        }

        regularFont.setColor(Color.WHITE);
        regularFont.draw(batch, "Tên: " + enemyName, x + 120, y + height - 50);
        regularFont.draw(batch, "HP: " + (int) enemyHealth + "/" + (int) enemyMaxHealth, x + 120, y + height - 75);

        // Health bar
        float healthPercentage = enemyHealth / enemyMaxHealth;
        batch.setColor(0.3f, 0.3f, 0.3f, 1);
        batch.draw(whiteTexture, x + 220, y + height - 85, 200, 15);
        batch.setColor(getHealthColor(healthPercentage));
        batch.draw(whiteTexture, x + 220, y + height - 85, 200 * healthPercentage, 15);

        //draw description
        String description = "Mô tả:\n" + this.enemy.getEnemyDescription();
        drawWrappedText(batch, regularFont, description, 830, y + height - 30, 400);

    }

    private void drawWrappedText(SpriteBatch batch, BitmapFont font, String text, float x, float y, float width) {
        layout.setText(font, text, Color.WHITE, width, 1, true);
        font.draw(batch, layout, x, y);
    }

    private Color getHealthColor(float percentage) {
        if (percentage > 0.5f) return Color.GREEN;
        else if (percentage > 0.2f) return Color.YELLOW;
        else return Color.RED;
    }

    private void drawCombatLogSection(SpriteBatch batch, float x, float y, float width, float height) {
        batch.setColor(0.1f, 0.1f, 0.2f, 0.9f);
        batch.draw(whiteTexture, x, y, width, height);

        drawCenteredText(batch, titleFont, "NHẬT KÝ GIAO CHIẾN", x + width / 2, y + height - 15, Color.CYAN);

        // Improved scrollable text rendering
        String logText = getCombatLogText();
        if (!logText.isEmpty()) {
            drawScrollableText(batch, regularFont, logText, x + 10, y + height - 40, width - 300, height - 50);
        }

        // Timer and turn indicator
        float timeLeft = COMBAT_TIME_LIMIT - combatTimer;
        String timeText = String.format("⏰ %02d:%02d", (int) (timeLeft / 60), (int) (timeLeft % 60));
        drawCenteredText(batch, regularFont, timeText, x + width / 2  + 20, y + height - 25, timeLeft < 60 ? Color.RED : Color.WHITE);

        String turnText = "Lượt: " + (isPlayerTurn ? "Người Chơi" : enemyName);
        regularFont.setColor(isPlayerTurn ? Color.GREEN : Color.RED);
        regularFont.draw(batch, turnText, x + width - 220, y + height - 50);
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

    // Thêm các utility methods cho scroll:
    public void scrollToBottom() {
        combatLogScrollOffset = maxCombatLogScrollOffset;
    }

    public void scrollToTop() {
        combatLogScrollOffset = 0;
    }

    private void drawPlayerStatusColumn(SpriteBatch batch, float x, float y, float width, float height) {
        batch.setColor(0.2f, 0.4f, 0.2f, 0.9f);
        batch.draw(whiteTexture, x, y, width, height);

        drawCenteredText(batch, regularFont, "NGƯỜI CHƠI", x + width / 2, y + height - 15, Color.GREEN);

        Texture playerTexture = getTexture("characters/player.png");
        if (playerTexture != null) {
            batch.setColor(Color.WHITE);
            batch.draw(playerTexture, x + (width - 100) / 2, y + height - 160, 100, 100);
        }

        regularFont.setColor(Color.WHITE);
        regularFont.draw(batch, "Tên: " + playerName, x + 15, y + height - 180);
        regularFont.draw(batch, "HP: " + (int) playerHealth + "/" + (int) playerMaxHealth, x + 15, y + height - 200);
        regularFont.draw(batch, "MP: " + (int) playerMana + "/" + (int) playerMaxMana, x + 15, y + height - 220);

        // Health and mana bars
        drawHealthBar(batch, playerHealth, playerMaxHealth, x + 15, y + height - 245, width - 50, 12);
        drawManaBar(batch, playerMana, playerMaxMana, x + 15, y + height - 265, width - 50, 12);
    }

    private void drawHealthBar(SpriteBatch batch, float current, float max, float x, float y, float width, float height) {
        batch.setColor(0.3f, 0.3f, 0.3f, 1);
        batch.draw(whiteTexture, x, y, width, height);
        batch.setColor(getHealthColor(current / max));
        batch.draw(whiteTexture, x, y, width * (current / max), height);
    }

    private void drawManaBar(SpriteBatch batch, float current, float max, float x, float y, float width, float height) {
        batch.setColor(0.3f, 0.3f, 0.3f, 1);
        batch.draw(whiteTexture, x, y, width, height);
        batch.setColor(0.2f, 0.4f, 0.9f, 1);
        batch.draw(whiteTexture, x, y, width * (current / max), height);
    }

    private void drawItemColumn(SpriteBatch batch, float x, float y, float width, float height) {
        batch.setColor(0.3f, 0.2f, 0.4f, 0.9f);
        batch.draw(whiteTexture, x, y, width, height);

        drawCenteredText(batch, regularFont, "🎒 ITEM", x + width / 2, y + height - 15, Color.MAGENTA);

        itemRectMap.clear();
        Map<String, Integer> characterItems = gameController.getCharacter().getBuffItems();

        if (characterItems == null || characterItems.isEmpty()) {
            regularFont.setColor(Color.GRAY);
            regularFont.draw(batch, "Không có vật phẩm!", x + 10, y + height - 30);
            return;
        }

        Vector3 mousePos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(mousePos);

        float itemY = y + height - 50;
        for (Map.Entry<String, Integer> entry : characterItems.entrySet()) {
            Items item = ItemLoader.getItemByName(entry.getKey());
            if (item == null) continue;

            Rectangle itemRect = new Rectangle(x, itemY - 25, width, 30);
            itemRectMap.put(itemRect, item);

            boolean isHovered = itemRect.contains(mousePos.x, mousePos.y);
            if (isHovered) {
                batch.setColor(0.5f, 0.5f, 0.7f, 0.8f);
                batch.draw(whiteTexture, itemRect.x, itemRect.y, itemRect.width, itemRect.height);
            }

            Texture itemIcon = getTexture(item.getTexturePath());
            if (itemIcon != null) {
                batch.setColor(Color.WHITE);
                batch.draw(itemIcon, x + 5, itemY - 20, 20, 20);
            }

            regularFont.setColor(isHovered ? Color.YELLOW : Color.WHITE);
            regularFont.draw(batch, item.getItemName(), x + 30, itemY);
            regularFont.draw(batch, "x" + entry.getValue(), x + width - 30, itemY);

            itemY -= 35;
        }
    }


    private void drawLetterGridColumn(SpriteBatch batch, float x, float y, float width, float height) {
        batch.setColor(0.25f, 0.25f, 0.35f, 0.9f);
        batch.draw(whiteTexture, x, y, width, height);

        String currentWord = letterGrid.getCurrentWord();
        drawCurrentWordCells(batch, currentWord, x, y + height - 50, width);

        if (isDrawingWordMeaning && !lastSubmittedWord.isEmpty()) {
            String meaning = wordValidator.getWordMeaning(lastSubmittedWord);
            if (meaning != null && !meaning.isEmpty()) {
                regularFont.setColor(Color.CYAN);
                if (meaning.length() > 50) {
                    meaning = meaning.substring(0, 50) + "...";
                }
                regularFont.draw(batch, "Meaning: " + meaning, x + 20, y + height - 30);
            }
        }

        drawLetterGrid(batch, gridX, gridY, gridSize);

        // Buttons
        submitButtonRect = new Rectangle(x + 220, y + 16, 120, 40);
        clearButtonRect = new Rectangle(x + 360, y + 16, 120, 40);

        if (isPlayerTurn && !combatTimeUp) {
            drawButton(batch, submitButtonRect, "CAST SPELL");
            drawButton(batch, clearButtonRect, "CLEAR");
        }
    }

    private void drawLetterGrid(SpriteBatch batch, float gridX, float gridY, float gridSize) {
        float cellSize = gridSize / 5;
        char[][] grid = letterGrid.getGrid();
        boolean[][] selected = letterGrid.getSelectedCells();

        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                float screenX = gridX + x * cellSize;
                float screenY = gridY + (4 - y) * cellSize;

                char letter = grid[y][x];
                boolean isSelected = selected[y][x];
                boolean isVowel = VOWELS.indexOf(Character.toUpperCase(letter)) != -1;
                boolean isDisabled = isCellDisabled(x, y);

                Texture cellTexture = isDisabled ? disabledCellTexture :
                        isSelected ? wordCellTexture :
                                isVowel ? vowelCellTexture : this.cellTexture;

                batch.setColor(Color.WHITE);
                batch.draw(cellTexture, screenX, screenY, cellSize, cellSize);

                Color letterColor = isDisabled ? Color.GRAY :
                        isSelected ? Color.RED :
                                isVowel ? Color.BLUE : Color.BLACK;

                layout.setText(regularFont, String.valueOf(letter));
                regularFont.setColor(letterColor);
                regularFont.draw(batch, String.valueOf(letter),
                        screenX + (cellSize - layout.width) / 2,
                        screenY + cellSize - (cellSize - layout.height) / 2);
            }
        }
    }

    private void drawCurrentWordCells(SpriteBatch batch, String currentWord, float columnX, float y, float columnWidth) {
        if (currentWord.isEmpty() && !isDrawingWordMeaning) {
            regularFont.setColor(Color.GRAY);
            drawCenteredText(batch, regularFont, "Chọn các chữ cái để tạo từ", columnX + columnWidth / 2, y + 7, Color.GRAY);
            return;
        }

        final float CELL_SIZE = 35;
        final float CELL_SPACING = 5;
        float totalWidth = currentWord.length() * (CELL_SIZE + CELL_SPACING) - CELL_SPACING;
        float startX = columnX + (columnWidth - totalWidth) / 2;

        for (int i = 0; i < currentWord.length(); i++) {
            char letter = currentWord.charAt(i);
            float cellX = startX + i * (CELL_SIZE + CELL_SPACING);

            boolean isVowel = VOWELS.indexOf(Character.toUpperCase(letter)) != -1;
            batch.setColor(Color.WHITE);
            batch.draw(isVowel ? vowelCellTexture : wordCellTexture, cellX, y, CELL_SIZE, CELL_SIZE);

            layout.setText(regularFont, String.valueOf(Character.toUpperCase(letter)));
            regularFont.setColor(isVowel ? Color.BLUE : Color.BLACK);
            regularFont.draw(batch, String.valueOf(Character.toUpperCase(letter)),
                    cellX + (CELL_SIZE - layout.width) / 2,
                    y + (CELL_SIZE + layout.height) / 2);
        }
    }

    private void drawButton(SpriteBatch batch, Rectangle buttonRect, String text) {
        Vector3 mousePos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(mousePos);
        boolean isSelected = buttonRect.contains(mousePos.x, mousePos.y);

        batch.setColor(Color.WHITE);
        batch.draw(isSelected ? buttonSelectedTexture : buttonTexture,
                buttonRect.x, buttonRect.y, buttonRect.width, buttonRect.height);

        layout.setText(regularFont, text);
        regularFont.setColor(Color.WHITE);
        regularFont.draw(batch, text,
                buttonRect.x + (buttonRect.width - layout.width) / 2,
                buttonRect.y + (buttonRect.height + layout.height) / 2);
    }

    private void performEnemyAction() {
        int action = random.nextInt(10);
        float damage = 0;

        if (action < 4) {
            damage = (random.nextInt(10) + 1) + currentLevel + enemy.getAttackPower();
            damage = Math.max(0, damage - gameController.getCharacter().getDefend());
            addCombatLog(enemyName + " tấn công gây " + (int) damage + " sát thương!");
        } else if (action < 8) {
            damage = (random.nextInt(10) + 1) + (currentLevel * enemy.getAttackPower());
            damage = Math.max(10, damage - gameController.getCharacter().getDefend());
            addCombatLog(enemyName + " tấn công mạnh gây " + (int) damage + " sát thương!");
        } else if (action == 8) {
            float heal = enemyMaxHealth * 0.2f;
            enemyHealth = Math.min(enemyMaxHealth, enemyHealth + heal);
            addCombatLog(enemyName + " hồi phục " + (int) heal + " máu!");
        } else {
            addCombatLog(enemyName + " đã trượt đòn tấn công!");
        }
        playerHealth = Math.max(0, playerHealth - damage);
        isPlayerTurn = true;

        enemyAttack((int) damage, () -> {
            checkCombatEnd();
            if (isCombatMode) {
                letterGrid.regenerateGrid();
                addCombatLog("---Đến lượt của bạn!---");
                if (isEnemyBoss()) applyBossEffects();
            }
        });



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

                if (isEnemyLord() && damage < 10) {
                    damage = 0;
                    addCombatLog("Lord " + enemyName + " chống chọi được đòn tấn công yếu!");
                } else {
                    damage = Math.min(enemyHealth, damage);
                    enemyHealth -= damage;
                    addCombatLog("Từ '" + word + "' gây " + (int) damage + " sát thương!");
                }

                addCombatLog("+" + points + " điểm!");

                playerAttack(word, 10, () -> {
                    checkCombatEnd();
                    if (isCombatMode && enemyHealth > 0) {
                        isPlayerTurn = false;
                        addCombatLog("---Đến lượt của " + enemyName + "!---");
                    }
                });
            }
            achievementManager.updateProgress(Achievement.AchievementType.WORD_COUNT, 1);

            letterGrid.regenerateGrid();

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
                addCombatLog("---Đến lượt của " + enemyName + "!---");
            }
            return false;
        }
    }

    private void checkCombatEnd() {
        if (playerHealth <= 0 || combatTimeUp) {
            String message = combatTimeUp ? "Hết thời gian! Bạn đã thua!" : "Bạn bị đánh bại!";
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
        this.isCombatMode = true;
        this.isPlayerTurn = true;
        this.playerName = gameController.getCharacter().getName();
        this.currentLevel = gameController.getCharacter().getLevel();
        this.achievementManager = gameController.getAchievementManager();
        this.enemyMaxHealth = enemy.getHealth() + currentLevel * 2;
        this.enemyHealth = enemy.getHealth() + currentLevel * 2;

        addCombatLog("Bắt đầu chiến đấu với " + enemyName + "!");
        letterGrid.regenerateGrid();

        if (isEnemyBoss()) applyBossEffects();
    }

    public void activate() {
        this.active = true;
        letterGrid.regenerateGrid();
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
    }

    public void deactivate() {
        this.active = false;
        clearSelection();
    }

    public boolean selectCell(int x, int y) {
        if (!active) return false;
        if (letterGrid.getCurrentWord().length() > 12) {
            addCombatLog("Từ đã quá dài! Tối đa 12 chữ cái.");
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

    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    public void dispose() {
        active = false;
        for (Texture texture : textureCache.values()) {
            if (texture != null) texture.dispose();
        }
        textureCache.clear();

        if (whiteTexture != null) whiteTexture.dispose();
        if (buttonTexture != null) buttonTexture.dispose();
        if (buttonSelectedTexture != null) buttonSelectedTexture.dispose();
        if (wordCellTexture != null) wordCellTexture.dispose();
        if (cellTexture != null) cellTexture.dispose();
        if (vowelCellTexture != null) vowelCellTexture.dispose();
        if (disabledCellTexture != null) disabledCellTexture.dispose();
    }

    private Texture getTexture(String path) {
        if (!textureCache.containsKey(path)) {
            try {
                textureCache.put(path, new Texture(Gdx.files.internal(path)));
            } catch (Exception e) {
                return whiteTexture;
            }
        }
        return textureCache.get(path);
    }

    private void drawCenteredText(SpriteBatch batch, BitmapFont font, String text, float x, float y, Color color) {
        layout.setText(font, text);
        font.setColor(color);
        font.draw(batch, text, x - layout.width / 2, y);
    }

    // Getters
    public MapEvent getCurrentEvent() {
        return currentEvent;
    }

    public void setCurrentEvent(MapEvent currentEvent) {
        this.currentEvent = currentEvent;
    }
}