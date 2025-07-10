package ctu.game.isometric.controller.gameplay;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
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
import ctu.game.isometric.model.entity.Gender;
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
        titleFont = generateVietNameseFont("Tektur-Bold.ttf", 20);
        regularFont = generateVietNameseFont("Tektur-Bold.ttf", 14);
        layout = new GlyphLayout();
        viewport = new FitViewport(1280, 720);
        createWhiteTexture();
        loadUITextures();
        createSpecialCellTextures();
        createMainActionButtons();
        createCloseButton(1280, 720);
    }


    // Gọi khi có hành động (ví dụ: player tấn công)
// Camera animation properties
    private boolean isCameraZooming = false;
    private float cameraZoomTarget = 1.0f;
    private float cameraZoomOriginal = 1.0f;
    private float cameraZoomCurrent = 1.0f;
    private float cameraZoomDuration = 0.5f;
    private float cameraZoomTimer = 0f;
    private float cameraShakeIntensity = 0f;
    private Vector3 cameraOriginalPosition = new Vector3();
    private Vector3 cameraTargetPosition = new Vector3();

    public void playerAttack(String word, int dmg, Runnable onComplete) {
        // Save original camera position and start zoom animation
        cameraOriginalPosition.set(viewport.getCamera().position);
        cameraZoomOriginal = ((OrthographicCamera) viewport.getCamera()).zoom;
        cameraZoomCurrent = cameraZoomOriginal;

        // Set zoom target (closer to enemy)
        cameraZoomTarget = 0.7f; // Closer zoom
        cameraTargetPosition.set(600, 550, 0); // Position near enemy

        // Start zoom animation
        isCameraZooming = true;
        cameraZoomTimer = 0f;
        cameraShakeIntensity = 0.1f; // Small shake for dramatic effect

        // Create attack card as before
        AttackCard card = new AttackCard(
                AttackCard.CardType.ATTACK,
                word,
                dmg,
                830, 600, 830, 600, 830, 470
        );

        // Extend onComplete to reset camera when animation finishes
        Runnable extendedComplete = () -> {
            // Reset camera zoom
            isCameraZooming = false;
            cameraZoomTimer = 0f;
            ((OrthographicCamera) viewport.getCamera()).zoom = cameraZoomOriginal;
            viewport.getCamera().position.set(cameraOriginalPosition);

            // Call the original onComplete
            if (onComplete != null) onComplete.run();
        };

        card.setSFXCallback(() -> effectManager.playClickSound());
        card.setOnComplete(extendedComplete);
        cardAnimationManager.addCard(card);
    }

    public void playerHealing(int heal, Runnable onComplete) {
        AttackCard card = new AttackCard(
                AttackCard.CardType.HEALING,
                "",
                heal,
                326, 266, 326, 276, 326, 266
        );
        card.setSFXCallback(() -> effectManager.playClickSound());
        card.setOnComplete(onComplete);
        cardAnimationManager.addCard(card);
    }

    public void playerHealingMana(int mana, Runnable onComplete) {
        AttackCard card = new AttackCard(
                AttackCard.CardType.MANA,
                "",
                mana,
                326, 266, 326, 276, 326, 266
        );
        card.setSFXCallback(() -> effectManager.playClickSound());
        card.setOnComplete(onComplete);
        cardAnimationManager.addCard(card);
    }


    public void playerBuff(int buff, Runnable onComplete) {
        AttackCard card = new AttackCard(
                AttackCard.CardType.SPECIAL,
                "",
                buff,
                326, 266, 326, 276, 326, 266
        );
        card.setSFXCallback(() -> effectManager.playClickSound());
        card.setOnComplete(onComplete);
        cardAnimationManager.addCard(card);
    }

    public void enemyAttack(int dmg, int action, int heal, Runnable onComplete) {
        AttackCard card;
        if (dmg == 0) {
            card = new AttackCard(
                    AttackCard.CardType.HEALING,
                    "",
                    heal,
                    830, 450, 460, 830, 450, 150
            );
            card.setSFXCallback(() -> effectManager.playClickSound());
        } else if (dmg < 0) {
            card = new AttackCard(
                    AttackCard.CardType.ATTACK,
                    "MISS",
                    0,
                    600, 580, 250, 400, 250, 400
            );
            card.setSFXCallback(() -> effectManager.playClickSound());
        } else {
            if (action < 8 && action > 4)
                card = new AttackCard(
                        AttackCard.CardType.STRONG,
                        "",
                        dmg,
                        316, 416, 316, 416, 316, 286
                );
            else
                card = new AttackCard(
                        AttackCard.CardType.ATTACK,
                        "",
                        dmg,
                        316, 416, 316, 416, 316, 286
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
        updateCardAnimation(delta);

        if (timerAction > 0) {
            timerAction -= delta;
            if (timerAction <= 0) {
                timerAction = 0;
            }
        }
//
//        // Update camera zoom animation
//        if (isCameraZooming) {
//            cameraZoomTimer += delta;
//            float progress = Math.min(cameraZoomTimer / cameraZoomDuration, 1.0f);
//
//            // Smooth interpolation
//            float smoothProgress = (float) (1 - Math.pow(1 - progress, 3));
//
//            // Update camera zoom
//            OrthographicCamera camera = (OrthographicCamera) viewport.getCamera();
//            camera.zoom = cameraZoomOriginal + (cameraZoomTarget - cameraZoomOriginal) * smoothProgress;
//
//            // Move camera position
//            camera.position.x = cameraOriginalPosition.x +
//                    (cameraTargetPosition.x - cameraOriginalPosition.x) * smoothProgress;
//            camera.position.y = cameraOriginalPosition.y +
//                    (cameraTargetPosition.y - cameraOriginalPosition.y) * smoothProgress;
//
//            // Add camera shake if needed
//            if (cameraShakeIntensity > 0) {
//                camera.position.x += (Math.random() - 0.5) * cameraShakeIntensity * 2;
//                camera.position.y += (Math.random() - 0.5) * cameraShakeIntensity * 2;
//                cameraShakeIntensity *= 0.95f; // Decay shake intensity
//            }
//
//            camera.update();
//        }

        if (isCombatMode) updateCombat(delta);
        // Rest of your update method...
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

    public void setGridSize(int size) {
        letterGrid.setGridSize(size);
        cellSize = gridSize / letterGrid.getGridSize();
    }

    private void applyBossEffects() {
        if (isEnemyBoss()) {
            setGridSize(3);
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

    // Thêm method này:
    public boolean handleCombatLogScroll(float scrollAmount) {
        if (!isCombatLogScrollable) return false;

        float lineHeight = regularFont.getLineHeight() + 2;
        float scrollDelta = scrollAmount * lineHeight;

        float oldOffset = combatLogScrollOffset;
        combatLogScrollOffset = Math.max(0, Math.min(maxCombatLogScrollOffset, combatLogScrollOffset + scrollDelta));

        return oldOffset != combatLogScrollOffset;
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

            playerMana = Math.max(0, playerMana - item.getManaCost());
            int newCount = items.get(item.getItemName()) - 1;
            if (newCount <= 0) items.remove(item.getItemName());
            else items.put(item.getItemName(), newCount);
            currentOverlay = OverlayType.NONE;
            timerAction = 5f;
            switch (item.getItemName()) {
                case "Elixir":
                    playerHealing((int) item.getValue(), () -> {
                        addCombatLog("Đã hồi " + item.getValue() + " Sinh Lực!");

                        playerHealth = Math.min(playerMaxHealth, playerHealth + item.getValue());

                        checkCombatEnd();
                        if (isCombatMode && enemyHealth > 0) {
                            isPlayerTurn = false;
                            addCombatLog("---Đến lượt của " + enemyName + "!---");
                        }
                    });
                    break;
                case "Arcane Essence":
                    playerHealingMana((int) item.getValue(), () -> {
                        addCombatLog("Đã hồi " + item.getValue() + " Năng lượng!");

                        playerMana = Math.min(playerMaxMana, playerMana + item.getValue());


                        checkCombatEnd();
                        if (isCombatMode && enemyHealth > 0) {
                            isPlayerTurn = false;
                            addCombatLog("---Đến lượt của " + enemyName + "!---");
                        }
                    });
                    break;
                case "Draught of Fury":
                    playerBuff(1, () -> {
                        addCombatLog("Đã tăng 1 sức mạnh!");
                        wordDamageMultiplier += item.getValue();
                        gameController.getCharacter().upAttack(item.getValue());


                        checkCombatEnd();
                        if (isCombatMode && enemyHealth > 0) {
                            isPlayerTurn = false;
                            addCombatLog("---Đến lượt của " + enemyName + "!---");
                        }
                    });
                    break;
                case "Aegis Brew":
                    playerBuff(1, () -> {
                        addCombatLog("Đã tăng 2 phòng thủ!");
                        gameController.getCharacter().upDefend(item.getValue());


                        checkCombatEnd();
                        if (isCombatMode && enemyHealth > 0) {
                            isPlayerTurn = false;
                            addCombatLog("---Đến lượt của " + enemyName + "!---");
                        }
                    });
                    break;
                case "Toxic Poiton":
                    playerAttack("Toxic", (int) item.getValue(), () -> {
                        addCombatLog("Kẻ địch đã bị trúng độc, mất " + item.getValue() + " máu!");
                        enemyHealth = Math.max(0, enemyHealth - item.getValue());

                        checkCombatEnd();
                        if (isCombatMode && enemyHealth > 0) {
                            isPlayerTurn = false;
                            addCombatLog("---Đến lượt của " + enemyName + "!---");
                        }
                    });
                    break;
            }


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
//        gridX = GRID_COLUMN_X + (GRID_COLUMN_WIDTH - gridSize) / 2 -150;
        gridY = 80; // Margin bottom + button height + spacing
        cellSize = gridSize / letterGrid.getGridSize();
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
            drawCenteredText(batch, regularFont, item.getItemDescription(), viewport.getWorldWidth() / 2, panelY + panelHeight - 50, Color.WHITE);
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
        final float SCREEN_WIDTH = viewport.getWorldWidth();
        final float SCREEN_HEIGHT = viewport.getWorldHeight();

        // Main combat background
        // Pokemon-style layout: battlefield takes most of the screen
        final float BATTLEFIELD_HEIGHT = SCREEN_HEIGHT * 0.7f; // 70% for battlefield
        final float UI_PANEL_HEIGHT = SCREEN_HEIGHT * 0.3f;   // 30% for UI panels
        drawBattlefield(batch, 0, UI_PANEL_HEIGHT, SCREEN_WIDTH, BATTLEFIELD_HEIGHT);

        final float PANEL_Y = 0;
        final float MAIN_PANEL_WIDTH = SCREEN_WIDTH * 0.6f;
        final float STATUS_PANEL_HEIGHT = UI_PANEL_HEIGHT * 0.6f;
        final float OPTION_PANEL_HEIGHT = UI_PANEL_HEIGHT * 0.4f;

        // Player status (top right)
        drawEnemyInfoPanel(batch, 300, 600, 400, 100);
        drawPlayerInfo(batch, 750, PANEL_Y + OPTION_PANEL_HEIGHT + STATUS_PANEL_HEIGHT + 36,
                400, STATUS_PANEL_HEIGHT);

        // Main action panel (left side - like Pokemon's text box)
        drawMainActionPanel(batch, MARGIN, PANEL_Y + MARGIN,
                MAIN_PANEL_WIDTH - MARGIN, UI_PANEL_HEIGHT - 2 * MARGIN);
        // Draw overlays if active
        if (currentOverlay == OverlayType.SPELL) {
            drawLetterGridOverlay(batch, SCREEN_WIDTH, SCREEN_HEIGHT);
        }

        if (currentOverlay == OverlayType.INVENTORY) {
            drawInventoryOverlay(batch, SCREEN_WIDTH, SCREEN_HEIGHT);
        }

        // Draw action buttons (Pokemon-style bottom menu)
        drawActionButtons(batch, SCREEN_WIDTH, SCREEN_HEIGHT);
    }

    private void drawMainActionPanel(SpriteBatch batch, float x, float y, float width, float height) {
        // Pokemon-style main panel with rounded corners
        batch.setColor(0.1f, 0.1f, 0.3f, 0.95f);
        batch.draw(whiteTexture, x, y, width, height);

        // Title
        drawCenteredText(batch, titleFont, "COMBAT LOG", x + width / 2, y + height - 25, Color.CYAN);

        // Combat log content
        String logText = getCombatLogText();
        if (!logText.isEmpty()) {
            drawScrollableText(batch, regularFont, logText, x + 10, y + height - 50, width - 20, height - 80);
        }

        // Timer display (Pokemon-style)
        float timeLeft = COMBAT_TIME_LIMIT - combatTimer;
        String timeText = String.format("TIME: %02d:%02d", (int) (timeLeft / 60), (int) (timeLeft % 60));
        regularFont.setColor(timeLeft < 60 ? Color.RED : Color.WHITE);
        regularFont.draw(batch, timeText, x + width - 120, y + height - 15);

        // Turn indicator
        String turnText = isPlayerTurn ? "YOUR TURN" : "ENEMY TURN";
        Color turnColor = isPlayerTurn ? Color.GREEN : Color.RED;
        drawCenteredText(batch, regularFont, turnText, x + width / 2, y + 15, turnColor);
    }


    private void drawBattlefield(SpriteBatch batch, float x, float y, float width, float height) {
        // Pokemon-style battlefield background
        Texture battlefieldBg = getTexture("ui/battlefield_bg.png");
        if (battlefieldBg != null) {
            batch.setColor(Color.WHITE);
            batch.draw(battlefieldBg, x, y, width, height);
        } else {
            // Gradient background
            batch.setColor(0.3f, 0.5f, 0.8f, 1);
            batch.draw(whiteTexture, x, y, width, height * 0.5f);
            batch.setColor(0.2f, 0.7f, 0.3f, 1);
            batch.draw(whiteTexture, x, y, width, height * 0.5f);
        }

        drawPokemonStyleEnemySection(batch);


        // Draw player (bottom left of battlefield)
        Texture playerTexture;
        if (gender.equalsIgnoreCase("MALE"))
            playerTexture = getTexture("characters/male.png"); // Back sprite like Pokemon
        else
            playerTexture = getTexture("characters/female.png");

        if (playerTexture != null) {
            batch.setColor(Color.WHITE);

            batch.draw(playerTexture, 326, 266, 150, 200);
        }

    }


    private void drawPokemonStyleEnemySection(SpriteBatch batch) {
        Texture enemyTexture = getTexture(this.enemy.getTexturePath());
        if (enemyTexture != null) {
            batch.setColor(Color.WHITE);

            batch.draw(enemyTexture,
                    830,
                    450,
                    150, 200);
        }
    }

    private void drawEnemyInfoPanel(SpriteBatch batch, float x, float y, float width, float height) {
        // Panel background
        batch.setColor(0.2f, 0.3f, 0.5f, 0.9f);
        batch.draw(whiteTexture, x, y, width, height);

        // Border
        batch.setColor(0.8f, 0.8f, 1.0f, 1);
        drawBorder(batch, x, y, width, height, 3);

        // Enemy name and level
        titleFont.setColor(Color.WHITE);
        titleFont.draw(batch, enemyName, x + 10, y + height - 15);

        String levelText = "Lv." + currentLevel;
        layout.setText(regularFont, levelText);
        regularFont.setColor(Color.YELLOW);
        regularFont.draw(batch, levelText, x + width - layout.width - 10, y + height - 15);

        // HP bar (Pokemon style)
        drawPokemonStyleHPBar(batch, enemyHealth, enemyMaxHealth, x + 40, y + 30, width - 60, 20);

        regularFont.setColor(Color.WHITE);
        regularFont.draw(batch, (int) enemyHealth + "/" + (int) enemyMaxHealth, x + 80, y + 43);
    }

    private void drawPlayerInfo(SpriteBatch batch, float x, float y, float width, float height) {
        // Panel background
        batch.setColor(0.2f, 0.3f, 0.5f, 0.9f);
        batch.draw(whiteTexture, x, y, width, height);
        // Border
        batch.setColor(0.8f, 0.8f, 1.0f, 1);
        drawBorder(batch, x, y, width, height, 3);

        // Enemy name and level
        titleFont.setColor(Color.WHITE);
        titleFont.draw(batch, playerName, x + 10, y + height - 15);

        String levelText = "Lv." + currentLevel;
        layout.setText(regularFont, levelText);
        regularFont.setColor(Color.YELLOW);
        regularFont.draw(batch, levelText, x + width - layout.width - 10, y + height - 15);

        // HP bar (Pokemon style)
        regularFont.setColor(Color.WHITE);
        drawPokemonStyleHPBar(batch, playerHealth, playerMaxHealth, x + 40, y + 60, width - 60, 20);
        drawPokemonStyleMPBar(batch, playerMana, playerMaxMana, x + 40, y + 30, width - 60, 20);
        regularFont.draw(batch, (int) playerHealth + "/" + (int) playerMaxHealth, x + 80, y + 73);
        regularFont.draw(batch, (int) playerMana + "/" + (int) playerMaxMana, x + 80, y + 43);
    }

    Rectangle spellButton = new Rectangle();
    Rectangle itemButton = new Rectangle();

    public void createMainActionButtons() {
        float buttonWidth = 150;
        float buttonHeight = 50;
        float buttonY = 20;
        float spacing = 20;

        // Center the buttons
        float totalWidth = (buttonWidth * 2) + spacing;
        float startX = (viewport.getWorldWidth() - totalWidth) - spacing * 4;

        // Spell button (opens letter grid)
        spellButton.set(startX, buttonY, buttonWidth, buttonHeight);

        // Item button (opens inventory)
        itemButton.set(startX + buttonWidth + spacing, buttonY, buttonWidth, buttonHeight);
    }

    private void drawActionButtons(SpriteBatch batch, float screenWidth, float screenHeight) {

        drawPokemonStyleButton(batch, spellButton, "SPELL", Color.CYAN);
        drawPokemonStyleButton(batch, itemButton, "ITEM", Color.ORANGE);

    }


    private void drawLetterGridOverlay(SpriteBatch batch, float screenWidth, float screenHeight) {
        // Semi-transparent background
        batch.setColor(0, 0, 0, 0.7f);
        batch.draw(whiteTexture, 0, 0, screenWidth, screenHeight);

        // Main overlay panel
        float panelWidth = screenWidth * 0.8f;
        float panelHeight = screenHeight * 0.8f;
        float panelX = (screenWidth - panelWidth) / 2;
        float panelY = (screenHeight - panelHeight) / 2;

        drawCenteredText(batch, titleFont, "🔮 SPELL CASTING", panelX + panelWidth / 2, panelY + panelHeight - 30, Color.CYAN);

        // Current word display (Pokemon style)
        String currentWord = letterGrid.getCurrentWord();
        drawCurrentWordCells(batch, currentWord, panelX + 50, panelY + panelHeight - 100, panelWidth - 100);

        // Letter grid (centered)
        float gridSize = Math.min(panelWidth * 0.6f, panelHeight * 0.6f);
        float gridX = panelX + (panelWidth - gridSize) / 2;
        float gridY = panelY + 120;

        drawLetterGrid(batch, gridX, gridY, gridSize);

        // Action buttons
        float buttonWidth = 120;
        float buttonHeight = 40;
        float buttonY = panelY + 40;

        submitButtonRect = new Rectangle(panelX + panelWidth / 2 - buttonWidth - 10, buttonY, buttonWidth, buttonHeight);
        clearButtonRect = new Rectangle(panelX + panelWidth / 2 + 10, buttonY, buttonWidth, buttonHeight);

        drawPokemonStyleButton(batch, submitButtonRect, "CAST", Color.GREEN);
        drawPokemonStyleButton(batch, clearButtonRect, "CLEAR", Color.RED);

        // Close button

        drawPokemonStyleButton(batch, closeInventoryButton, "x", Color.GRAY);

    }

    Rectangle closeInventoryButton = new Rectangle();

    public void createCloseButton(float screenWidth, float screenHeight) {
        float panelWidth = screenWidth * 0.7f;
        float panelHeight = screenHeight * 0.7f;
        float panelX = (screenWidth - panelWidth) / 2;
        float panelY = (screenHeight - panelHeight) / 2;
        closeInventoryButton = new Rectangle(panelX + panelWidth - 60, panelY + panelHeight - 50, 50, 40);

    }

    private void drawInventoryOverlay(SpriteBatch batch, float screenWidth, float screenHeight) {
        // Semi-transparent background
        batch.setColor(0, 0, 0, 0.7f);
        batch.draw(whiteTexture, 0, 0, screenWidth, screenHeight);

        // Main overlay panel
        float panelWidth = screenWidth * 0.7f;
        float panelHeight = screenHeight * 0.7f;
        float panelX = (screenWidth - panelWidth) / 2;
        float panelY = (screenHeight - panelHeight) / 2;

        // Panel background
        batch.setColor(0.3f, 0.2f, 0.1f, 0.95f);
        batch.draw(whiteTexture, panelX, panelY, panelWidth, panelHeight);

        // Border
        batch.setColor(0.8f, 0.6f, 0.4f, 1);
        drawBorder(batch, panelX, panelY, panelWidth, panelHeight, 4);

        // Title
        drawCenteredText(batch, titleFont, "🎒 INVENTORY", panelX + panelWidth / 2, panelY + panelHeight - 30, Color.ORANGE);

        // Item grid
        drawBeautifulItemGrid(batch, panelX + 20, panelY + 60, panelWidth - 40, panelHeight - 120);

        // Close button
        drawPokemonStyleButton(batch, closeInventoryButton, "x", Color.GRAY);


    }

    private void drawBeautifulItemGrid(SpriteBatch batch, float x, float y, float width, float height) {
        itemRectMap.clear();
        Map<String, Integer> characterItems = gameController.getCharacter().getBuffItems();

        if (characterItems == null || characterItems.isEmpty()) {
            drawCenteredText(batch, regularFont, "No items available", x + width / 2, y + height / 2, Color.GRAY);
            return;
        }

        // Grid layout
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

            // Item slot background
            batch.setColor(isHovered ? new Color(0.4f, 0.5f, 0.6f, 0.9f) : new Color(0.2f, 0.3f, 0.4f, 0.8f));
            batch.draw(whiteTexture, itemRect.x, itemRect.y, itemRect.width, itemRect.height);

            // Border
            batch.setColor(isHovered ? Color.YELLOW : Color.WHITE);
            drawBorder(batch, itemRect.x, itemRect.y, itemRect.width, itemRect.height, 2);

            // Item icon
            Texture itemIcon = getTexture(item.getTexturePath());
            if (itemIcon != null) {
                batch.setColor(Color.WHITE);
                batch.draw(itemIcon, itemX + 5, itemY + itemSlotHeight - 50, 40, 40);
            }

            // Item name and count
            regularFont.setColor(Color.WHITE);
            regularFont.draw(batch, item.getItemName(), itemX + 50, itemY + itemSlotHeight - 15);
            regularFont.draw(batch, "x" + entry.getValue(), itemX + itemSlotSize - 40, itemY + itemSlotHeight - 15);

            // Mana cost
            regularFont.setColor(Color.CYAN);
            regularFont.draw(batch, "MP: " + item.getManaCost(), itemX + 50, itemY + itemSlotHeight - 35);

            index++;
        }
    }

    private void drawPokemonStyleButton(SpriteBatch batch, Rectangle buttonRect, String text, Color color) {
        Vector3 mousePos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(mousePos);
        boolean isHovered = buttonRect.contains(mousePos.x, mousePos.y);

        // Button background
        batch.setColor(isHovered ? color.cpy().mul(1.2f) : color);
        batch.draw(whiteTexture, buttonRect.x, buttonRect.y, buttonRect.width, buttonRect.height);

        // Border
        batch.setColor(Color.WHITE);
        drawBorder(batch, buttonRect.x, buttonRect.y, buttonRect.width, buttonRect.height, 2);

        // Text
        layout.setText(regularFont, text);
        regularFont.setColor(Color.BLACK);
        regularFont.draw(batch, text,
                buttonRect.x + (buttonRect.width - layout.width) / 2,
                buttonRect.y + (buttonRect.height + layout.height) / 2);
    }

    private void drawPokemonStyleHPBar(SpriteBatch batch, float current, float max, float x, float y, float width, float height) {
        if (max <= 0) return; // Tránh chia cho 0

        float percentage = Math.max(0, current / max); // Clamp về 0 nếu âm

        // 1. Vẽ nền tối (viền ngoài)
        batch.setColor(0.1f, 0.1f, 0.1f, 1);
        batch.draw(whiteTexture, x - 2, y - 2, width + 4, height + 4);

        // 2. Vẽ nền trong (background thanh máu)
        batch.setColor(0.3f, 0.3f, 0.3f, 1);
        batch.draw(whiteTexture, x, y, width, height);

        // 3. Chọn màu máu theo phần trăm
        Color hpColor = percentage > 0.5f ? Color.valueOf("4CAF50") : percentage > 0.2f ? Color.valueOf("FFEB3B") : Color.valueOf("F44336");

        // 4. Vẽ phần thanh máu chính
        batch.setColor(hpColor);
        batch.draw(whiteTexture, x, y, width * percentage, height);

        // 5. Vẽ viền đen mỏng
        batch.setColor(Color.BLACK);
        drawBorder(batch, x, y, width, height, 1);

        // 6. Text "HP"
        regularFont.setColor(Color.WHITE);
        regularFont.draw(batch, "HP", x - 30, y + height - 2);

    }


    private void drawPokemonStyleMPBar(SpriteBatch batch, float current, float max, float x, float y, float width, float height) {
        if (max <= 0) return; // Tránh chia cho 0

        float percentage = Math.max(0, current / max); // Clamp về 0 nếu âm

        // 1. Viền ngoài tối
        batch.setColor(0.1f, 0.1f, 0.1f, 1);
        batch.draw(whiteTexture, x - 2, y - 2, width + 4, height + 4);

        // 2. Nền trong
        batch.setColor(0.3f, 0.3f, 0.3f, 1);
        batch.draw(whiteTexture, x, y, width, height);

        // 3. Thanh MP màu xanh lam (có thể thay bằng gradient texture nếu muốn)
        Color mpColor = new Color(0.2f, 0.4f, 0.95f, 1); // xanh biển đậm
        batch.setColor(mpColor);
        batch.draw(whiteTexture, x, y, width * percentage, height);

        // 4. Viền đen mỏng
        batch.setColor(Color.BLACK);
        drawBorder(batch, x, y, width, height, 1);

        // 5. Text "MP"
        regularFont.setColor(Color.WHITE);
        regularFont.draw(batch, "MP", x - 30, y + height - 2);

        // 6. Hiển thị số MP hiện tại / tối đa
    }


    private void drawBorder(SpriteBatch batch, float x, float y, float width, float height, float thickness) {
        // Top
        batch.draw(whiteTexture, x, y + height - thickness, width, thickness);
        // Bottom
        batch.draw(whiteTexture, x, y, width, thickness);
        // Left
        batch.draw(whiteTexture, x, y, thickness, height);
        // Right
        batch.draw(whiteTexture, x + width - thickness, y, thickness, height);
    }

    enum OverlayType {
        NONE,
        SPELL,
        INVENTORY
    }

    OverlayType currentOverlay = OverlayType.NONE;

    // Update the click handler to work with overlays
    public boolean handleCombatClick(float x, float screenY) {
        if (timerAction > 0 || !isPlayerTurn) return false;
        float y = Gdx.graphics.getHeight() - screenY;

        if (spellButton.contains(x, y)) {
            currentOverlay = (currentOverlay == OverlayType.SPELL) ? OverlayType.NONE : OverlayType.SPELL;
        } else if (itemButton.contains(x, y)) {
            currentOverlay = (currentOverlay == OverlayType.INVENTORY) ? OverlayType.NONE : OverlayType.INVENTORY;
        }


        // Handle overlay clicks first
        if (currentOverlay == OverlayType.SPELL) {
            return handleGridOverlayClick(x, screenY);
        } else if (currentOverlay == OverlayType.INVENTORY) {
            return handleInventoryOverlayClick(x, y);
        }
        return false;
    }


    private boolean handleGridOverlayClick(float x, float y) {
        final float SCREEN_WIDTH = viewport.getWorldWidth();
        final float SCREEN_HEIGHT = viewport.getWorldHeight();

        float screenY = Gdx.graphics.getHeight() - y;

        float panelWidth = SCREEN_WIDTH * 0.8f;
        float panelHeight = SCREEN_HEIGHT * 0.8f;
        float panelX = (SCREEN_WIDTH - panelWidth) / 2;
        float panelY = (SCREEN_HEIGHT - panelHeight) / 2;


        if (closeInventoryButton.contains(x, screenY)) {
            currentOverlay = OverlayType.NONE;
            return true;
        }

        // Submit button
        if (submitButtonRect != null && submitButtonRect.contains(x, screenY)) {
            submitWord();
            currentOverlay = OverlayType.NONE;
            return true;
        }

        // Clear button
        if (clearButtonRect != null && clearButtonRect.contains(x, screenY)) {
            clearSelection();
            return true;
        }

        // Letter grid clicks
        float gridSize = Math.min(panelWidth * 0.6f, panelHeight * 0.6f);
        float gridX = panelX + (panelWidth - gridSize) / 2;
        float gridY = panelY + 120;

        return handleLetterGridClick(x, y, gridX, gridY, gridSize);
    }

    private boolean handleInventoryOverlayClick(float x, float y) {
        // Close button
        if (closeInventoryButton.contains(x, y)) {
            currentOverlay = OverlayType.NONE;
            return true;
        }

        // Item clicks
        return handleItemBoxClick(x, y);
    }


    private boolean handleLetterGridClick(float x, float y, float gridX, float gridY, float gridSize) {
        int gridSizeValue = letterGrid.getGridSize();
        float cellSize = gridSize / gridSizeValue;

        // Check if click is within grid bounds
        if (x < gridX || x > gridX + gridSize || y < gridY || y > gridY + gridSize) {
            return false;
        }

        // Calculate grid coordinates
        int gridCol = (int) ((x - gridX) / cellSize);
        int gridRow = (int) ((y - gridY) / cellSize);

        // Validate coordinates
        if (gridCol < 0 || gridCol >= gridSizeValue || gridRow < 0 || gridRow >= gridSizeValue) {
            return false;
        }

        // Check if cell is disabled (for boss fights)
        if (isCellDisabled(gridCol, gridRow)) {
            return false;
        }

        // Handle cell selection
        boolean wasSelected = letterGrid.getSelectedCells()[gridRow][gridCol];
        if (wasSelected) {
            // If cell is already selected, try to deselect it
            return letterGrid.deselectCell(gridCol, gridRow);
        } else {
            // Try to select the cell
            return selectCell(gridCol, gridRow);
        }
    }

    // Helper method for rotated text
    private void drawRotatedText(SpriteBatch batch, BitmapFont font, String text, float x, float y, float rotation, Color color) {
        layout.setText(font, text);

        // Save current state
        batch.end();
        batch.begin();

        font.setColor(color);
        font.draw(batch, text,
                x - layout.width / 2,
                y + layout.height / 2);
    }

    String difficultyText = "";

    private void drawWrappedText(SpriteBatch batch, BitmapFont font, String text, float x, float y, float width) {
        layout.setText(font, text, Color.WHITE, width, 1, true);
        font.draw(batch, layout, x, y);
    }

    private void drawWrappedBlackText(SpriteBatch batch, BitmapFont font, String text, float x, float y, float width) {
        layout.setText(font, text, Color.BLACK, width, 1, true);
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
        drawCenteredText(batch, regularFont, timeText, x + width / 2 + 140, y + height - 20, timeLeft < 60 ? Color.RED : Color.WHITE);
        regularFont.draw(batch, "Độ khó: " + difficultyText, x + width - 220, y + height - 15);


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

    String gender;

    private void drawLetterGrid(SpriteBatch batch, float gridX, float gridY, float gridSize) {
        if (timerAction > 0) return;
        int gridSizeValue = letterGrid.getGridSize();
        float cellSize = gridSize / gridSizeValue;
        char[][] grid = letterGrid.getGrid();
        boolean[][] selected = letterGrid.getSelectedCells();

        for (int y = 0; y < gridSizeValue; y++) {
            for (int x = 0; x < gridSizeValue; x++) {
                float screenX = gridX + x * cellSize;
                float screenY = gridY + (gridSizeValue - 1 - y) * cellSize;

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
            drawCenteredText(batch, regularFont, "Chọn các chữ cái để tạo từ", columnX + columnWidth / 2, y + 22, Color.GRAY);
            return;
        } else {
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
        float heal = 0;

        if (action < 4) {
            damage = (random.nextInt(10) + 1) + currentLevel + enemy.getAttackPower();
            damage = Math.max(0, damage - gameController.getCharacter().getDefend());
            addCombatLog(enemyName + " tấn công gây " + (int) damage + " sát thương!");
        } else if (action < 8) {
            damage = (random.nextInt(10) + 1) + (currentLevel * enemy.getAttackPower());
            damage = Math.max(10, damage - gameController.getCharacter().getDefend());
            addCombatLog(enemyName + " tấn công mạnh gây " + (int) damage + " sát thương!");
        } else if (action == 8) {
            heal = enemyMaxHealth * 0.2f;
            enemyHealth = Math.min(enemyMaxHealth, enemyHealth + heal);
            addCombatLog(enemyName + " hồi phục " + (int) heal + " máu!");
        } else {
            damage = -1;
            addCombatLog(enemyName + " đã trượt đòn tấn công!");
        }
        if (damage >= 0) {
            playerHealth = Math.max(0, playerHealth - damage);
        }
        isPlayerTurn = true;

        enemyAttack((int) damage, action, (int) heal, () -> {
            checkCombatEnd();
            if (isCombatMode) {
                letterGrid.regenerateGrid();
                addCombatLog("---Đến lượt của bạn!---");
                if (isEnemyBoss()) applyBossEffects();
            }
        });


    }

    // Add these enhanced animation fields to your class
    // Enhanced animation fields with Yu-Gi-Oh style


    float timerAction = 0;


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
                    damage -= enemy.getDefensePower() * 0.7f;
                    addCombatLog("Từ '" + word + "' gây " + (int) damage + " sát thương!");
                }

                addCombatLog("+" + points + " điểm!");

                // Replace the playerAttack call with this:
                if (isCombatMode && isPlayerTurn) {
                    letterGrid.clearWord();

                    if (damage > enemyHealth) {
                        startCardAttackAnimation(word, (int) damage, () -> {
                            checkCombatEnd();
                            if (isCombatMode && enemyHealth > 0) {
                                isPlayerTurn = false;
                                addCombatLog("---Đến lượt của " + enemyName + "!---");

                            }
                        });
                        enemyHealth = Math.max(0, enemyHealth - damage);

                    } else if (damage > 0) {
                        playerAttack(word, (int) damage, () -> {
                            checkCombatEnd();
                            if (isCombatMode && enemyHealth > 0) {
                                isPlayerTurn = false;
                                addCombatLog("---Đến lượt của " + enemyName + "!---");

                            }
                        });
                        enemyHealth = Math.max(0, enemyHealth - damage);
                    } else {
                        checkCombatEnd();
                        if (isCombatMode && enemyHealth > 0) {
                            isPlayerTurn = false;
                            addCombatLog("---Đến lượt của " + enemyName + "!---");

                        }
                    }
                }
            }
            achievementManager.updateProgress(Achievement.AchievementType.WORD_COUNT, 1);
            timerAction = 5f;
            Timer.schedule(new Timer.Task() {
                @Override
                public void run() {
                    isDrawingWordMeaning = false;
                }
            }, 2.4f);

            return true;
        } else {
            timerAction = 5f;
            addCombatLog("Từ '" + word + "' không hợp lệ!");
            gameController.getCharacter().updateWrongWordCount();
            if (isCombatMode && enemyHealth > 0) {
                isPlayerTurn = false;
                addCombatLog("---Đến lượt của " + enemyName + "!---");
            }
            return false;
        }
    }

    private static final float MARGIN = 20;
    private static final float SCREEN_WIDTH = 1280;
    private static final float SCREEN_HEIGHT = 720;
    private static final float ENEMY_SECTION_HEIGHT = 120;
    private boolean isCardAnimating = false;
    private float cardAnimX, cardAnimY, cardAnimScale, cardAnimRotation;
    private float cardAnimStartX, cardAnimStartY;
    private float cardAnimTargetX, cardAnimTargetY;
    private float cardAnimDuration = 2.0f; // Longer for dramatic Yu-Gi-Oh effect
    private float cardAnimTimer = 0;
    private String animatingWord = "";
    private Runnable cardAnimCallback;

    // Yu-Gi-Oh style effects
    private float screenShakeTimer = 0;
    private boolean isSpellActivating = false;
    private Color cardTintColor = Color.WHITE.cpy();

    private void startCardAttackAnimation(String word, int damage, Runnable onComplete) {
        animatingWord = word;
        cardAnimCallback = onComplete;

        // Yu-Gi-Oh style starting position (card rises from player deck)
        cardAnimStartX = MARGIN + 140; // Center of player column
        cardAnimStartY = MARGIN + 100;

        // Target position (center screen for spell activation)
        cardAnimTargetX = SCREEN_WIDTH / 2 - 100;
        cardAnimTargetY = SCREEN_HEIGHT / 2 - 100;

        // Reset animation state
        cardAnimX = cardAnimStartX;
        cardAnimY = cardAnimStartY;
        cardAnimScale = 0.8f; // Start smaller
        cardAnimRotation = 0;
        cardAnimTimer = 0;
        isCardAnimating = true;
        isSpellActivating = false;

        cardTintColor.set(Color.WHITE);

        // Yu-Gi-Oh activation sound
        effectManager.playClickSound();
    }

    private void updateCardAnimation(float delta) {
        if (!isCardAnimating) return;

        cardAnimTimer += delta;
        float progress = Math.min(cardAnimTimer / cardAnimDuration, 1.0f);

        // Phase 1: Card Rise & Flip (0-0.25)
        if (progress < 0.25f) {
            float phaseProgress = progress / 0.25f;

            // Card giữ nguyên scale và rotation
            cardAnimScale = 1.0f;
            cardAnimRotation = 0f;

            // Move to center position
            cardAnimX = cardAnimStartX + (cardAnimTargetX - cardAnimStartX) * phaseProgress;
            cardAnimY = cardAnimStartY + (cardAnimTargetY - cardAnimStartY) * phaseProgress;

            // Building golden glow
            cardTintColor.set(1, 1, 0.5f + phaseProgress * 0.5f, 1); // Golden tint
        }
        // Phase 2: Spell Activation Preparation (0.25-0.5)
        else if (progress < 0.5f) {
            float phaseProgress = (progress - 0.25f) / 0.25f;

            // Card giữ nguyên tại center, không scale, không spin
            cardAnimScale = 1.0f;
            cardAnimRotation = 0f;
            cardAnimX = cardAnimTargetX;
            cardAnimY = cardAnimTargetY;

        }
        // Phase 3: SPELL ACTIVATION! (0.5-0.75)
        else if (progress < 0.75f) {
            float phaseProgress = (progress - 0.5f) / 0.25f;

            if (!isSpellActivating) {
                isSpellActivating = true;

                screenShakeTimer = 0.5f;
            }

            // Không còn scale và rotation cho card
            cardAnimScale = 1.0f;
            cardAnimRotation = 0f;


            // Intense white/golden flash
            float flashIntensity = (float) Math.sin(phaseProgress * Math.PI * 20);
            cardTintColor.set(2 - flashIntensity, 2 - flashIntensity, 1, 1);

        }
        // Phase 4: Spell Launch (0.75-1.0)
        else {
            float phaseProgress = (progress - 0.75f) / 0.25f;

            // Card launches toward enemy
            float launchProgress = (float) (1 - Math.pow(1 - phaseProgress, 3));
            cardAnimX = cardAnimTargetX + (580 - cardAnimTargetX) * launchProgress;
            cardAnimY = cardAnimTargetY + (SCREEN_HEIGHT - ENEMY_SECTION_HEIGHT - MARGIN - cardAnimTargetY) * launchProgress;

            // Card grows then shrinks on impact
            if (phaseProgress < 0.3f) {
                cardAnimScale = 1.2f + (phaseProgress / 0.3f) * 0.8f; // Grow to 2.0
            } else {
                cardAnimScale = 2.0f * (1 - ((phaseProgress - 0.3f) / 0.7f)); // Shrink to 0
            }


            if (phaseProgress > 0.8f && screenShakeTimer <= 0) {
                screenShakeTimer = 0.3f;
            }
        }

        // Animation complete
        if (progress >= 1.0f) {
            isCardAnimating = false;
            isSpellActivating = false;
            if (cardAnimCallback != null) {
                cardAnimCallback.run();
            }
        }
    }


    private void checkCombatEnd() {
        if (playerHealth <= 0 || combatTimeUp) {
            String message = combatTimeUp ? "Hết thời gian! Bạn đã thua!" : "Bạn bị đánh bại!";
            addCombatLog(message);
            playerHealth = 0;
            endCombat(false);
            isGameOver = gameController.getCharacter().gameOver();
            gameController.getCharacter().resetWinStreak();
            timerAction = 0;
        } else if (enemyHealth <= 0) {
            addCombatLog("Bạn đã hạ gục " + enemyName + "!");
            enemyHealth = 0;
            endCombat(true);
            timerAction = 0;
            gameController.getCharacter().incrementWinStreak();
            if (currentEvent != null && currentEvent.isOneTime()) {
                gameController.getEventManager().recordDefeatedEnemy(this.enemy.getEnemyID());
                gameController.getEventManager().completeEvent(currentEvent.getId());
                gameController.setCompletedEvent();
            }

            achievementManager.updateProgress(Achievement.AchievementType.COMBAT_WIN, 1);
            this.newLevel = gameController.getCharacter().addExperience(this.experienceGain);
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
        this.enemyMaxHealth = enemy.getHealth() + currentLevel * 5;
        this.enemyHealth = enemy.getHealth() + currentLevel * 5;
        this.gender = gameController.getCharacter().getGender().toString();

        addCombatLog("Bắt đầu chiến đấu với " + enemyName + "!");
        letterGrid.regenerateGrid();

        float difficultyScore = (enemyHealth * 0.5f + enemy.getAttackPower() * 1.5f) / (currentLevel + 1);

        if (enemyName.contains("Lord")) {
            difficultyScore *= 1.5f;
        } else if (enemyName.contains("Boss")) {
            difficultyScore *= 2.0f;
        }
        if (difficultyScore < 10) {
            difficultyText = "Dễ";
        } else if (difficultyScore < 20) {
            difficultyText = "Vừa";
        } else if (difficultyScore < 35) {
            difficultyText = "Khó";
        } else {
            difficultyText = "Rất Khó";
        }


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
                Texture texture = new Texture(Gdx.files.internal(path));
                texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                textureCache.put(path, texture);
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