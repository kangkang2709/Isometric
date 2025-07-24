package ctu.game.isometric.controller.gameplay;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
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

    public GameplayRenderer(GameplayController controller, LetterGrid letterGrid, Viewport viewport) {
        this.controller = controller;
        this.letterGrid = letterGrid;
        this.viewport = viewport;
        initializeUI();
    }

    private void initializeUI() {
        titleFont = generateVietNameseFont("Tektur-Bold.ttf", 20);
        regularFont = generateVietNameseFont("Tektur-Bold.ttf", 14);
        layout = new GlyphLayout();
        createWhiteTexture();
        loadUITextures();
        createSpecialCellTextures();
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
            buttonTexture = getTexture("ui/button.png");
            buttonSelectedTexture = getTexture("ui/button_selected.png");
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

        batch.setColor(0.1f, 0.1f, 0.2f, 1);
        batch.draw(whiteTexture, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
        batch.setColor(Color.WHITE);

        if (controller.isCombatMode()) {
            renderCombatUI(batch);
            renderFloatingTexts(batch);
        } else if (controller.isVictory()) {
            renderReward(batch);
        } else {
            renderGameOver(batch);
        }

        CardAnimationManager cardAnimationManager = controller.getCardAnimationManager();
        if (cardAnimationManager != null) {
            cardAnimationManager.render(batch);
        }
        renderEnemyTooltip(batch);
    }

    public void renderGameOver(SpriteBatch batch) {
        if (!controller.isGameOver()) {
            controller.getGameController().setState(GameState.EXPLORING);
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

        Enemy enemy = controller.getEnemy();
        Reward reward = RewardLoader.getRewardById(enemy.getRewardID());
        Items item = ItemLoader.getItemById(reward.getItemID());

        if (item != null) {
            Texture itemTexture = getTexture(item.getTexturePath());
            if (itemTexture != null) {
                batch.setColor(Color.WHITE);
                batch.draw(itemTexture, panelX + 90, panelY + panelHeight / 2 - 32, 64, 64);
            }
            regularFont.setColor(Color.YELLOW);
            regularFont.draw(batch, item.getItemName() + " x" + reward.getAmount(), panelX + 180, panelY + panelHeight / 2 + 30);
            drawWrappedText(batch, regularFont, item.getItemDescription(), panelX + 150, panelY + panelHeight / 2, panelWidth - 200);
        }

        Rectangle continueButton = new Rectangle(viewport.getWorldWidth() / 2 - 100, panelY + 50, 200, 50);
        drawButton(batch, continueButton, "Tiếp tục");


        if (Gdx.input.justTouched()) {
            Vector3 touchPos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            viewport.unproject(touchPos);
            if (continueButton.contains(touchPos.x, touchPos.y)) {
                controller.getGameController().getCharacter().addItem(item, reward.getAmount());
                controller.getGameController().getCharacter().setHealth(controller.getPlayerHealth());
                controller.getGameController().getCharacter().setMana(controller.getPlayerMana());
                Timer.schedule(new Timer.Task() {
                    @Override
                    public void run() {
                        if (controller.isEnemyBoss()) {
                            controller.getGameController().returnToTowerAfterBoss(controller.getEnemyName());
                        }
                        controller.getGameController().setState(GameState.EXPLORING);
                        if (controller.getNewLevel() > controller.getCurrentLevel())
                            controller.getGameController().showLevelUpNotification();
                        controller.cleanupCombatState();

                    }
                }, 0.5f);
            }
        }
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
        drawMainActionPanel2(batch, MAIN_PANEL_WIDTH + MARGIN, PANEL_Y + MARGIN + 90, 500 - 2 * MARGIN, UI_PANEL_HEIGHT - 2 * MARGIN - 90);
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

    private void drawMainActionPanel2(SpriteBatch batch, float x, float y, float width, float height) {
        regularFont.setColor(Color.WHITE);
        float attack = controller.getGameController().getCharacter().getDamage() + controller.getAttackBuff() - controller.getPlayerNerf();
        float defend = controller.getPlayerDefend() + controller.getPlayerDef();
        regularFont.draw(batch, "Sức mạnh: " + (int) attack, x + 20, y + height - 15);
        regularFont.draw(batch, "Phòng thủ: " + (int) defend, x + 20, y + height - 35);
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
        batch.setColor(0.8f, 0.8f, 1.0f, 1);
        drawBorder(batch, x, y, width, height, 3);

        titleFont.setColor(Color.WHITE);
        titleFont.draw(batch, controller.getEnemyName(), x + 10, y + height - 15);

        String levelText = "Lv." + controller.getCurrentLevel();
        layout.setText(regularFont, levelText);
        regularFont.setColor(Color.YELLOW);
        regularFont.draw(batch, levelText, x + width - layout.width - 10, y + height - 15);

        drawPokemonStyleHPBar(batch, controller.getEnemyHealth(), controller.getEnemyMaxHealth(), x + 40, y + 45, width - 60, 20);

        regularFont.setColor(Color.WHITE);
        regularFont.draw(batch, String.format("%.2f", controller.getEnemyHealth()) + "/" + (int) controller.getEnemyMaxHealth(), x + 80, y + 58);
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
        batch.setColor(0.2f, 0.3f, 0.5f, 0.9f);
        batch.draw(whiteTexture, x, y, width, height);

        batch.setColor(0.8f, 0.8f, 1.0f, 1);
        drawBorder(batch, x, y, width, height, 3);

        titleFont.setColor(Color.WHITE);
        titleFont.draw(batch, controller.getPlayerName(), x + 10, y + height - 15);

        String levelText = "Lv." + controller.getCurrentLevel();
        layout.setText(regularFont, levelText);
        regularFont.setColor(Color.YELLOW);
        regularFont.draw(batch, levelText, x + width - layout.width - 10, y + height - 15);

        regularFont.setColor(Color.WHITE);
        drawPokemonStyleHPBar(batch, controller.getPlayerHealth(), controller.getPlayerMaxHealth(), x + 40, y + 80, width - 60, 20);
        drawPokemonStyleMPBar(batch, controller.getPlayerMana(), controller.getPlayerMaxMana(), x + 40, y + 50, width - 60, 20);
        regularFont.draw(batch, String.format("%.2f", controller.getPlayerHealth()) + "/" + (int) controller.getPlayerMaxHealth(), x + 80, y + 93);
        regularFont.draw(batch, (int) controller.getPlayerMana() + "/" + (int) controller.getPlayerMaxMana(), x + 80, y + 63);

        drawStatusEffects(batch, x + 30, y + height - 133);
    }

    private void drawActionButtons(SpriteBatch batch, float screenWidth, float screenHeight) {
        batch.setColor(0.1f, 0.1f, 0.3f, 0.95f);
        batch.draw(whiteTexture, 788, 20, 460, 75);

        drawPokemonStyleButton(batch, controller.getSpellButton(), "Tấn công", Color.CYAN);
        drawPokemonStyleButton(batch, controller.getItemButton(), "Vật Phẩm", Color.ORANGE);
        drawPokemonStyleButton(batch, controller.getNormalAttackButton(), "Tấn Công Thường", Color.GRAY);
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

    private void drawPokemonStyleHPBar(SpriteBatch batch, float current, float max, float x, float y, float width, float height) {
        if (max <= 0) return;

        float percentage = Math.max(0, current / max);

        batch.setColor(0.1f, 0.1f, 0.1f, 1);
        batch.draw(whiteTexture, x - 2, y - 2, width + 4, height + 4);

        batch.setColor(0.3f, 0.3f, 0.3f, 1);
        batch.draw(whiteTexture, x, y, width, height);

        Color hpColor = percentage > 0.5f ? Color.valueOf("4CAF50") : percentage > 0.2f ? Color.valueOf("FFEB3B") : Color.valueOf("F44336");

        batch.setColor(hpColor);
        batch.draw(whiteTexture, x, y, width * percentage, height);

        batch.setColor(Color.BLACK);
        drawBorder(batch, x, y, width, height, 1);

        regularFont.setColor(Color.WHITE);
        regularFont.draw(batch, "HP", x - 30, y + height - 2);
    }

    private void drawPokemonStyleMPBar(SpriteBatch batch, float current, float max, float x, float y, float width, float height) {
        if (max <= 0) return;

        float percentage = Math.max(0, current / max);

        batch.setColor(0.1f, 0.1f, 0.1f, 1);
        batch.draw(whiteTexture, x - 2, y - 2, width + 4, height + 4);

        batch.setColor(0.3f, 0.3f, 0.3f, 1);
        batch.draw(whiteTexture, x, y, width, height);

        Color mpColor = new Color(0.2f, 0.4f, 0.95f, 1);
        batch.setColor(mpColor);
        batch.draw(whiteTexture, x, y, width * percentage, height);

        batch.setColor(Color.BLACK);
        drawBorder(batch, x, y, width, height, 1);

        regularFont.setColor(Color.WHITE);
        regularFont.draw(batch, "MP", x - 30, y + height - 2);
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
        if (currentWord.isEmpty() && !controller.isDrawingWordMeaning()) {
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