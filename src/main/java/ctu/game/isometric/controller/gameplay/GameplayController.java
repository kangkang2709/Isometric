package ctu.game.isometric.controller.gameplay;

import com.badlogic.gdx.Gdx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import ctu.game.isometric.animation.CardAnimationManager;
import ctu.game.isometric.animation.CardAnimationService;
import ctu.game.isometric.controller.AchievementManager;
import ctu.game.isometric.controller.EffectManager;
import ctu.game.isometric.controller.GameController;
import ctu.game.isometric.model.entity.Enemy;
import ctu.game.isometric.model.game.*;
import ctu.game.isometric.model.world.MapEvent;
import ctu.game.isometric.util.*;
import ctu.game.isometric.view.scene.FloatingText;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;

public class GameplayController {
    // Core constants
    private static final float ENEMY_TURN_DELAY = 2.4f;
    private static final float COMBAT_TIME_LIMIT = 1200f;
    private static final int MAX_COMBAT_LOG_LINES = 40;
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
    String difficultyText = "";
    String gender;
    // Combat state
    private boolean isCombatMode = false;
    private boolean isPlayerTurn = true;
    private float combatTimer = 0;
    private boolean combatTimeUp = false;
    private float enemyActionTimer = 0;
    private List<String> combatLogLines = new ArrayList<>();
    private boolean isVictory = false;
    private String lastSubmittedWord = "";
    // UI components
    private GlyphLayout layout;
    private Viewport viewport;
    // Boss mechanics
    private Set<Integer> disabledCells = new HashSet<>();
    // Player/Enemy stats
    private float playerMaxHealth = 100, playerHealth = 100, playerMana = 100, playerMaxMana = 100;
    private String playerName = "Player";
    private float enemyHealth = 100, enemyMaxHealth = 100;
    private String enemyName = "Enemy";
    private Enemy enemy;
    // Button areas
    // Combat mechanics
    private float wordDamageMultiplier = 1f;
    private Map<Rectangle, Items> itemRectMap = new HashMap<>();
    private MapEvent currentEvent;
    // Grid constants
    private float gridX, gridY, gridSize, cellSize;
    private CardAnimationManager cardAnimationManager;
    private CardAnimationService cardAnimationService;

    public GameplayController(GameController gameController) {
        this.gameController = gameController;
        this.letterGrid = new LetterGrid();
        this.effectManager = gameController.getEffectManager();
        this.wordValidator = gameController.getWordNetValidator();
        this.achievementManager = gameController.getAchievementManager();
        cardAnimationManager = new CardAnimationManager();
        this.cardAnimationService = new CardAnimationService(cardAnimationManager, effectManager);
        initializeUI();

        renderer = new GameplayRenderer(this, letterGrid, viewport, gameController.getAssetManager().getAnimationManager().getActionAnimations());
    }

    private void initializeUI() {
        layout = new GlyphLayout();
        viewport = new FitViewport(1280, 720);
        createMainActionButtons();
        createCloseButton(1280, 720);
    }

    private List<FloatingText> floatingTexts = new ArrayList<>();

    public void addFloatingText(String text, float x, float y, Color color) {
        floatingTexts.add(new FloatingText(text, x, y, color, 2.0f));
    }

    public void playerAttack(String word, int dmg, Runnable onComplete) {
        cardAnimationService.playerAttack(word, dmg, onComplete);
    }

    public void playerMiss(String word, int dmg, Runnable onComplete) {
        cardAnimationService.playerMiss(word, dmg, onComplete);
    }

    public void playerHealing(int heal, Runnable onComplete) {
        addFloatingText("+" + heal + " HP", 320, 460, Color.GREEN);
        cardAnimationService.playerHealing(heal, onComplete);
    }

    public void playerHealingMana(int mana, Runnable onComplete) {
        addFloatingText("+" + mana + " MP", 320, 460, Color.CYAN);
        cardAnimationService.playerHealingMana(mana, onComplete);
    }

    public void playerBuff(int buff, Runnable onComplete) {
        cardAnimationService.playerBuff(buff, onComplete);
    }

    public void playerToxic(int buff, Runnable onComplete) {
        cardAnimationService.playerToxic(buff, onComplete);
    }

    public void enemyToxic(int buff, Runnable onComplete) {
        cardAnimationService.enemyToxic(buff, onComplete);
    }

    public void enemyFire(int buff, Runnable onComplete) {
        cardAnimationService.enemyFire(buff, onComplete);
    }

    public void enemyAttack(int dmg, int action, int heal, Runnable onComplete) {
        if (heal > 0) {
            addFloatingText("+" + heal + " HP", 950, 600, Color.LIME);
        }
        cardAnimationService.enemyAttack(dmg, action, heal, onComplete);
    }

    public void update(float delta) {
        if (!active) return;
        cardAnimationManager.update(delta);


        // Update floating texts
        floatingTexts.removeIf(text -> {
            text.update(delta);
            return text.isFinished();
        });

        if (timerAction > 0) {
            timerAction -= delta;
            if (timerAction <= 0) {
                timerAction = 0;
            }
        }
        if (isCombatMode) {
            updateCombat(delta);
            renderer.updateFlicker(delta);
            renderer.updateFlicker2(delta);
        }
        renderer.updateAnimations(delta);
    }


    // Add getter for floating texts
    public List<FloatingText> getFloatingTexts() {
        return floatingTexts;
    }

    private boolean enemyHasActed = false;

    private void updateCombat(float delta) {
        combatTimer += delta;
        if (combatTimer >= COMBAT_TIME_LIMIT && !combatTimeUp) {
            combatTimeUp = true;
            addCombatLog("Thời gian hết! Trận đấu kết thúc!");
            endCombat(false);
            return;
        }

        if (!isPlayerTurn && !enemyHasActed) {
            enemyActionTimer += delta;
            if (enemyActionTimer >= ENEMY_TURN_DELAY) {
                performEnemyAction();
                enemyActionTimer = 0;
                enemyHasActed = true; // Mark that enemy has acted this turn
            }
        }
    }


    public GameplayRenderer getRenderer() {
        return renderer;
    }

    public void setRenderer(GameplayRenderer renderer) {
        this.renderer = renderer;
    }

    private void addCombatLog(String message) {
        combatLogLines.add(message);
        renderer.handleCombatLogScroll(1); // Thêm dòng này
        if (combatLogLines.size() > MAX_COMBAT_LOG_LINES) {
            combatLogLines.remove(0);
        }
    }

    public String getCombatLogText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < combatLogLines.size(); i++) {
            if (i > 0) sb.append("\n");
            sb.append(combatLogLines.get(i));
        }
        return sb.toString();
    }

    public float getCombatTimer() {
        return combatTimer;
    }

    public boolean isEnemyBoss() {
        return enemyName.toLowerCase().contains("boss");
    }

    public boolean isEnemyLord() {
        return enemyName.contains("Lord") || enemyName.contains("Azrok");
    }

    public void setGridSize(int size) {
        letterGrid.setGridSize(size);
        cellSize = gridSize / letterGrid.getGridSize();
    }

    private void applyBossEffects() {
        switch (enemyName) {
            case "Crystal Serpent Boss": {
                disabledCells.clear();
                int cellsToDisable = random.nextInt(2) + 1;
                for (int i = 0; i < cellsToDisable; i++) {
                    int cellIndex;
                    do {
                        cellIndex = random.nextInt(25);
                    } while (disabledCells.contains(cellIndex));
                    disabledCells.add(cellIndex);
                }
                addCombatLog("Crystal Serpent Boss đã làm vô hiệu hóa " + cellsToDisable + " ô!");
                break;
            }
            case "Emerald Revenant Boss": {
                setGridSize(4);
                addCombatLog("Emerald Revenant Boss làm giảm kích thước lưới!");

                if (random.nextFloat() < 0.1f && !playerStatusDuration.containsKey("TOXIC") && !enemyStatusDuration.containsKey("FREEZE")) {
                    enemyToxic(0, () -> {
                        addCombatLog("Bạn đã bị trúng độc bởi Emerald Revenant Boss!");
                        playerStatusDuration.put("TOXIC", 1);
                    });
                }
                break;
            }
            case "Sapphire Dragon Boss": {
                if (random.nextFloat() < 0.1f && !playerStatusDuration.containsKey("BURN") && !enemyStatusDuration.containsKey("FREEZE")) {
                    enemyFire(0, () -> {
                        addCombatLog("Bạn đã bị đốt cháy bởi Emerald Revenant Boss!");
                        playerStatusDuration.put("BURN", 1);
                    });
                }
                break;
            }
            default: {
                // Log or handle unexpected enemy names
                System.out.println("Unknown boss: " + enemyName);
            }
        }
    }

    private boolean isCellDisabled(int x, int y) {
        return disabledCells.contains(y * 5 + x);
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

        enemyHasActed = false;
        Map<String, Integer> items = gameController.getCharacter().getItems();
        if (items.containsKey(item.getItemName()) && items.get(item.getItemName()) > 0) {

            playerMana = Math.max(0, playerMana - item.getManaCost());
            int newCount = items.get(item.getItemName()) - 1;
            if (newCount <= 0) items.remove(item.getItemName());
            else items.put(item.getItemName(), newCount);
            currentOverlay = OverlayType.NONE;
            timerAction = 5f;
            switch (item.getItemName()) {
                case "Big Elixir":
                case "Elixir":
                    playerHealing((int) item.getValue(), () -> {
                        addCombatLog("Đã hồi " + item.getValue() + " Sinh Lực!");
                        renderer.startActionAnimation(1, true);
                        playerHealth = Math.min(playerMaxHealth, playerHealth + item.getValue());
                        checkCombatEnd();
                        if (isCombatMode && enemyHealth > 0) {
                            isPlayerTurn = false;
                            addCombatLog("---Đến lượt của " + enemyName + "!---");
                        }
                    });
                    break;
                case "Big Arcane Essence":
                case "Arcane Essence":
                    playerHealingMana((int) item.getValue(), () -> {
                        renderer.startActionAnimation(1, true);
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
                        addCombatLog("Đã tăng 8 sức mạnh trong trận!");
                        renderer.startActionAnimation(4, true);
                        gameController.getCharacter().upAttack(item.getValue());
                        playerStatusDuration.put("BUFF_ATK", 2);
                        checkCombatEnd();
                        if (isCombatMode && enemyHealth > 0) {
                            isPlayerTurn = false;
                            addCombatLog("---Đến lượt của " + enemyName + "!---");
                        }
                    });
                    break;
                case "Aegis Brew":
                    playerBuff(1, () -> {
//                        renderer.startActionAnimation(4, true);
                        addCombatLog("Đã tăng 8 phòng thủ trong trận!");
                        gameController.getCharacter().upDefend(item.getValue());
                        playerStatusDuration.put("BUFF_DEF", 5);

                        checkCombatEnd();
                        if (isCombatMode && enemyHealth > 0) {
                            isPlayerTurn = false;
                            addCombatLog("---Đến lượt của " + enemyName + "!---");
                        }
                    });
                    break;
                case "Toxic Potion":
                    playerToxic(20, () -> {
                        gameController.addFlag("quest_012");
                        addCombatLog("Kẻ địch đã bị trúng độc, mất " + item.getValue() + " máu! Bị độc trong 3 lượt tiếp theo!");
                        enemyHealth = Math.max(0, enemyHealth - item.getValue());
                        checkCombatEnd();

                        if (isCombatMode && enemyHealth > 0) {
                            if (enemyName.equals("Sapphire Dragon Boss"))
                                enemyStatusDuration.put("TOXIC", 5);
                            else enemyStatusDuration.put("TOXIC", 3);

                            isPlayerTurn = false;
                            addCombatLog("---Đến lượt của " + enemyName + "!---");
                        }
                    });
                    break;
            }


        }
    }

    GameplayRenderer renderer;

    public void render(SpriteBatch batch) {
        if (!active) return;
        renderer.render(batch);
    }


    private boolean showEnemyTooltip = false;
    private float tooltipX, tooltipY;
    private String tooltipText = "";

    Rectangle enemyHitArea = new Rectangle(830, 450, 150, 200);

    public void handleEnemyHover(int mouseX, int mouseY) {
        // Define enemy hit area (adjust coordinates to match your enemy sprite position)
        mouseY = 720 - mouseY; // Invert Y coordinate for LibGDX

        if (enemyHitArea.contains(mouseX, mouseY)) {
            showEnemyTooltip = true;
            tooltipX = mouseX + 20; // Offset to avoid cursor overlap
            tooltipY = mouseY + 20;

            // Build tooltip text with enemy stats
            StringBuilder tooltip = new StringBuilder();
            tooltip.append(enemy.getEnemyName()).append("\n");
            tooltip.append("Level: ").append(currentLevel).append("\n");
            tooltip.append("HP: ").append((int) enemyHealth).append("/").append((int) enemyMaxHealth).append("\n");
            tooltip.append("Attack: ").append(enemy.getAttackPower()).append("\n");
            tooltip.append("Defense: ").append(enemy.getDefensePower()).append("\n");

            // Add enemy description if available
            if (enemy.getEnemyDescription() != null) {
                tooltip.append("\n").append(enemy.getEnemyDescription());
            }

            tooltipText = tooltip.toString();
        } else {
            showEnemyTooltip = false;
        }
    }

    Rectangle spellButton = new Rectangle();
    Rectangle itemButton = new Rectangle();
    Rectangle normalAttackButton = new Rectangle();
    Rectangle closeInventoryButton = new Rectangle();
    Rectangle infoButton = new Rectangle();

    public void createMainActionButtons() {
        float buttonWidth = 150;
        float buttonHeight = 45;
        float buttonY = 35;
        float spacing = 20;
        float offsetY = 60;

        // Giữ vị trí canh phải như cũ
        float totalWidth = (buttonWidth * 2) + spacing;
        float startX = (viewport.getWorldWidth() - totalWidth) - spacing * 5;
        float centerX = startX + buttonWidth + spacing / 2f;

        // Trên: Normal Attack
        normalAttackButton.set(centerX - buttonWidth / 2f, buttonY + offsetY * 2, buttonWidth, buttonHeight);

        // Trái: Item
        itemButton.set(centerX - 100 - buttonWidth / 2f, buttonY + offsetY, buttonWidth, buttonHeight);

        // Phải: Spell
        spellButton.set(centerX + 100 - buttonWidth / 2f, buttonY + offsetY, buttonWidth, buttonHeight);

        // Dưới: Info
        infoButton.set(centerX - buttonWidth / 2f, buttonY, buttonWidth, buttonHeight);
    }


    public Rectangle getInfoButton() {
        return infoButton;
    }

    public void setInfoButton(Rectangle infoButton) {
        this.infoButton = infoButton;
    }

    public void createCloseButton(float screenWidth, float screenHeight) {
        float panelWidth = screenWidth * 0.7f;
        float panelHeight = screenHeight * 0.7f;
        float panelX = (screenWidth - panelWidth) / 2;
        float panelY = (screenHeight - panelHeight) / 2;
        closeInventoryButton = new Rectangle(panelX + panelWidth - 60, panelY + panelHeight - 50, 50, 40);

    }

    enum OverlayType {
        NONE, SPELL, INVENTORY
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
        } else if (normalAttackButton.contains(x, y)) {
            currentOverlay = OverlayType.NONE;
            return normalAttack();
        } else if (infoButton.contains(x, y)) {
            gameController.setState(GameState.INFORMATION);
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

        Rectangle submitButtonRect = renderer.getSubmitButtonRect();
        Rectangle clearButtonRect = renderer.getClearButtonRect();
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


    private Map<String, Integer> playerStatusDuration = new HashMap<>();
    private Map<String, Integer> enemyStatusDuration = new HashMap<>();


    private int playerDef = 0;
    private int attackBuff = 0;
    private int playerNerf = 0;
    private float atkNerf = 0;

    public Random getRandom() {
        return random;
    }

    public int getCurrentLevel() {
        return currentLevel;
    }


    public GlyphLayout getLayout() {
        return layout;
    }

    public Set<Integer> getDisabledCells() {
        return disabledCells;
    }

    public float getPlayerMaxHealth() {
        return playerMaxHealth;
    }

    public float getPlayerHealth() {
        return playerHealth;
    }

    public float getPlayerMana() {
        return playerMana;
    }

    public float getPlayerMaxMana() {
        return playerMaxMana;
    }

    public String getPlayerName() {
        return playerName;
    }

    public float getEnemyHealth() {
        return enemyHealth;
    }

    public float getEnemyMaxHealth() {
        return enemyMaxHealth;
    }

    public String getEnemyName() {
        return enemyName;
    }

    public float getGridX() {
        return gridX;
    }

    public float getGridY() {
        return gridY;
    }

    public boolean isShowEnemyTooltip() {
        return showEnemyTooltip;
    }

    public float getTooltipX() {
        return tooltipX;
    }

    public float getTooltipY() {
        return tooltipY;
    }

    public String getTooltipText() {
        return tooltipText;
    }

    public Rectangle getEnemyHitArea() {
        return enemyHitArea;
    }

    public Rectangle getSpellButton() {
        return spellButton;
    }

    public Rectangle getItemButton() {
        return itemButton;
    }

    public Rectangle getNormalAttackButton() {
        return normalAttackButton;
    }

    public Rectangle getCloseInventoryButton() {
        return closeInventoryButton;
    }

    public float getTimerAction() {
        return timerAction;
    }

    public int getPlayerDef() {
        return playerDef;
    }

    public float getPlayerDefend() {
        return playerDefend;
    }

    public String getGender() {
        return gender;
    }

    public int getAttackBuff() {
        return attackBuff;
    }

    public int getPlayerNerf() {
        return playerNerf;
    }

    private void checkPlayerStatusEffects() {
        if (playerStatusDuration.containsKey("BURN")) {
            int burnDamage = Math.max(1, (int) (playerMaxHealth * 0.02f));
            playerHealth = Math.max(0, playerHealth - burnDamage);

            addFloatingText("-" + burnDamage + " HP", 320, 450, Color.ORANGE);

            addCombatLog("Bạn mất " + burnDamage + " HP do bỏng! Và giảm 20% sát thương!");
            playerNerf = (int) (enemy.getAttackPower() * 0.2f);
            int remaining = playerStatusDuration.get("BURN") - 1;
            if (remaining <= 0) {
                playerNerf = 0; // Reset nerf
                playerStatusDuration.remove("BURN");
                addCombatLog("Bạn không còn bị bỏng!");
            } else {
                playerStatusDuration.put("BURN", remaining);
            }
        }

        if (playerStatusDuration.containsKey("TOXIC")) {
            int toxicDamage = Math.max(1, (int) (playerMaxHealth * 0.05f));
            playerHealth = Math.max(0, playerHealth - toxicDamage);

            addFloatingText("-" + toxicDamage + " HP", 330, 465, Color.PURPLE);

            addCombatLog("Bạn mất " + toxicDamage + " HP do độc tố!");

            int remaining = playerStatusDuration.get("TOXIC") - 1;
            if (remaining <= 0) {
                playerStatusDuration.remove("TOXIC");
                addCombatLog("Bạn không còn bị độc tố!");
            } else {
                playerStatusDuration.put("TOXIC", remaining);
            }
        }

        if (playerStatusDuration.containsKey("BUFF_ATK")) {
            int buffDuration = playerStatusDuration.get("BUFF_ATK");
            if (buffDuration > 1) {
                attackBuff = 8; // Tăng sát thương tấn công
                addCombatLog("Bạn được tăng 8 sát thương tấn công!");
                playerStatusDuration.put("BUFF_ATK", buffDuration - 1);
            } else {
                playerStatusDuration.remove("BUFF_ATK");
                attackBuff = 0; // Reset buff
                addCombatLog("Hiệu ứng BUFF ATK đã hết hạn!");
            }
        }
        if (playerStatusDuration.containsKey("BUFF_DEF")) {
            int buffDuration = playerStatusDuration.get("BUFF_DEF");
            if (buffDuration > 1) {
                playerDef = 8; // Tăng phòng thủ
                addCombatLog("Bạn được tăng 8 phòng thủ!");
                playerStatusDuration.put("BUFF_DEF", buffDuration - 1);
            } else {
                playerStatusDuration.remove("BUFF_DEF");
                playerDef = 0; // Reset buff
                addCombatLog("Hiệu ứng BUFF DEF đã hết hạn!");
            }
        }

    }

    private void checkStatusEffects() {
        // Enemy status effects
        if (enemyStatusDuration.containsKey("BURN") && enemyName.contains("Sapphire Dragon Boss")) {
            addCombatLog(enemyName + " không thể bị bỏng!");
            enemyStatusDuration.remove("BURN");
        }

        if (enemyStatusDuration.containsKey("TOXIC") && enemyName.contains("Emerald Revenant Boss")) {
            // Sapphire Dragon Boss không bị bỏng
            addCombatLog(enemyName + " không thể bị độc!");
            enemyStatusDuration.remove("TOXIC");
        }


        if (enemyStatusDuration.containsKey("BURN")) {
            int burnDamage = Math.max(1, (int) (enemyMaxHealth * 0.02f));
            enemyHealth = Math.max(0, enemyHealth - burnDamage);

            addFloatingText("-" + burnDamage + " HP", 940, 600, Color.ORANGE);

            atkNerf = enemy.getAttackPower() * 0.2f;
            addCombatLog(enemyName + " mất " + burnDamage + " HP do bỏng và giảm 20% sát thương!");

            int remaining = enemyStatusDuration.get("BURN") - 1;
            if (remaining <= 0) {
                enemyStatusDuration.remove("BURN");
                addCombatLog(enemyName + " không còn bị bỏng!");
            } else {
                enemyStatusDuration.put("BURN", remaining);
            }
        }


        if (enemyStatusDuration.containsKey("TOXIC")) {
            int toxicDamage = Math.max(1, (int) (enemyMaxHealth * 0.05f));

            addFloatingText("-" + toxicDamage + " HP", 960, 610, Color.PURPLE);

            enemyHealth = Math.max(0, enemyHealth - toxicDamage);
            addCombatLog(enemyName + " mất " + toxicDamage + " HP do độc tố!");

            int remaining = enemyStatusDuration.get("TOXIC") - 1;
            if (remaining <= 0) {
                enemyStatusDuration.remove("TOXIC");
                addCombatLog(enemyName + " không còn bị độc tố!");
            } else {
                enemyStatusDuration.put("TOXIC", remaining);
            }
        }

        if (enemyStatusDuration.containsKey("REGEN")) {
            int regenHeal = Math.max(1, (int) (enemyMaxHealth * 0.05f));

            enemyHealth = Math.min(enemyMaxHealth, enemyHealth + regenHeal);
            renderer.startActionAnimation(1, false);
            addFloatingText("+" + regenHeal + " HP", 955, 610, Color.GREEN);
            addCombatLog(enemyName + " hồi phục " + regenHeal + " HP liên tục!");
            int remaining = enemyStatusDuration.get("REGEN") - 1;
            if (remaining <= 0) {
                enemyStatusDuration.remove("REGEN");
                addCombatLog(enemyName + " không còn  khả năng hồi phục!");
            } else {
                enemyStatusDuration.put("REGEN", remaining);
            }
        }

    }

    private void performEnemyAction() {
        // Calculate action probabilities based on enemy health and type
        EnemyActionProbabilities probabilities = calculateActionProbabilities();
        int action = selectWeightedAction(probabilities);

        float damage = 0;
        float heal = 0;
        boolean skipTurn = false;

        // Handle freeze status with more nuanced behavior
        if (enemyStatusDuration.containsKey("FREEZE")) {
            skipTurn = handleFreezeStatus();
            if (skipTurn) return;
        }

        // Apply other status effects
        if (!enemyStatusDuration.isEmpty()) {
            checkStatusEffects();
            checkCombatEnd();
            if (!isCombatMode) return;
        }

        // Execute the selected action
        switch (action) {
            case 0: // Light Attack (40% base chance)
                damage = calculateLightAttack();
                addCombatLog(enemyName + " thực hiện đòn tấn công nhẹ gây " + (int) damage + " sát thương!");
                break;

            case 1: // Heavy Attack (30% base chance)
                damage = calculateHeavyAttack();
                addCombatLog(enemyName + " tung đòn tấn công mạnh gây " + (int) damage + " sát thương!");
                break;

            case 2: // Heal (15% base chance)
                heal = calculateHealAmount();
                enemyHealth = Math.min(enemyMaxHealth, enemyHealth + heal);
                addCombatLog(enemyName + " hồi phục " + (int) heal + " máu!");
                break;

            case 3: // Special Attack (10% base chance)
                damage = performSpecialAttack();
                break;

            case 4: // Defensive Action (3% base chance)
                performDefensiveAction();
                damage = 0;
                break;

            default: // Miss (2% base chance)
                damage = -1;
                addCombatLog(enemyName + " đã trượt đòn tấn công!");
                break;
        }

        // Apply damage to player
        if (damage > 0) {
            float finalDamage = Math.max(0, damage - (playerDefend + playerDef));
            playerHealth = Math.max(0, playerHealth - finalDamage);
            damage = finalDamage;
        }

        int finalDamage = (int) damage;

        // Execute attack animation and end turn
        enemyAttack(finalDamage, action, (int) heal, () -> {
            checkCombatEnd();
            if (isCombatMode) {
                handlePostAttackEffects(finalDamage);
                endEnemyTurn();
            }
        });
    }

    private EnemyActionProbabilities calculateActionProbabilities() {
        float healthPercent = enemyHealth / enemyMaxHealth;
        EnemyActionProbabilities probabilities = new EnemyActionProbabilities();

        // Base probabilities
        probabilities.lightAttack = 0.4f;
        probabilities.heavyAttack = 0.3f;
        probabilities.heal = 0.15f;
        probabilities.special = 0.1f;
        probabilities.defensive = 0.03f;
        probabilities.miss = 0.02f;

        // Adjust based on health
        if (healthPercent < 0.3f) {
            // Low health: more healing and defensive actions
            probabilities.heal += 0.2f;
            probabilities.defensive += 0.05f;
            probabilities.lightAttack -= 0.15f;
            probabilities.heavyAttack -= 0.1f;
        } else if (healthPercent < 0.6f) {
            // Medium health: more aggressive
            probabilities.heavyAttack += 0.1f;
            probabilities.special += 0.05f;
            probabilities.heal -= 0.05f;
            probabilities.lightAttack -= 0.1f;
        }

        // Boss-specific adjustments
        if (isEnemyBoss()) {
            probabilities.special += 0.1f;
            probabilities.defensive += 0.02f;
            probabilities.miss -= 0.01f;
            probabilities.lightAttack -= 0.06f;
            probabilities.heavyAttack -= 0.05f;
        }

        // Lord-specific adjustments
        if (isEnemyLord()) {
            probabilities.heavyAttack += 0.15f;
            probabilities.special += 0.05f;
            probabilities.heal += 0.1f;
            probabilities.lightAttack -= 0.2f;
            probabilities.defensive -= 0.05f;
            probabilities.miss -= 0.05f;
        }

        return probabilities;
    }

    private int selectWeightedAction(EnemyActionProbabilities prob) {
        float rand = random.nextFloat();
        float cumulative = 0;

        cumulative += prob.lightAttack;
        if (rand <= cumulative) return 0;

        cumulative += prob.heavyAttack;
        if (rand <= cumulative) return 1;

        cumulative += prob.heal;
        if (rand <= cumulative) return 2;

        cumulative += prob.special;
        if (rand <= cumulative) return 3;

        cumulative += prob.defensive;
        if (rand <= cumulative) return 4;

        return 5; // Miss
    }

    private boolean handleFreezeStatus() {
        int freezeDuration = enemyStatusDuration.get("FREEZE");

        // Higher level enemies have better chance to break freeze
        float breakChance = Math.min(0.6f, 0.3f + (currentLevel * 0.02f));
        if (isEnemyBoss()) breakChance += 0.2f;
        if (isEnemyLord()) breakChance += 0.3f;

        boolean isBroke = random.nextFloat() < breakChance;

        if (isBroke) {
            enemyStatusDuration.remove("FREEZE");
            addCombatLog(enemyName + " đã phá vỡ hiệu ứng đóng băng và có thể tấn công!");
            return false; // Continue with normal action
        } else {
            if (freezeDuration > 1) {
                addCombatLog(enemyName + " bị đóng băng không thể tấn công và hồi phục nhẹ!");

                float regenAmount = Math.min(10, enemyMaxHealth * 0.02f);
                enemyHealth = Math.min(enemyMaxHealth, enemyHealth + regenAmount);
                addFloatingText("+" + (int) regenAmount + " HP", 960, 605, Color.BLUE);

                enemyStatusDuration.put("FREEZE", freezeDuration - 1);
            } else {
                enemyStatusDuration.remove("FREEZE");
                addCombatLog(enemyName + " đã hết hiệu ứng đóng băng!");
            }

            enemyAttack(-1, 0, 0, () -> {
                checkCombatEnd();
                if (isCombatMode) {
                    endEnemyTurn();
                }
            });
            return true; // Skip turn
        }
    }

    private float calculateLightAttack() {
        float baseDamage = 5 + random.nextInt(6) + enemy.getAttackPower();
        return Math.max(1, baseDamage - atkNerf);
    }

    private float calculateHeavyAttack() {
        float baseDamage = 10 + random.nextInt(8) + (enemy.getAttackPower() - atkNerf) * 1.3f + currentLevel * 0.6f;
        return Math.max(3, baseDamage);
    }

    private float calculateHealAmount() {
        float baseHeal = enemyMaxHealth * (0.15f + random.nextFloat() * 0.1f); // 15-25%
        if (isEnemyBoss()) baseHeal *= 1.2f;
        return baseHeal;
    }

    private float performSpecialAttack() {
        float damage = 0;

        switch (enemyName) {
            case "Crystal Serpent Boss":
                damage = performCrystalSerpenSpecial();
                break;
            case "Sapphire Dragon Boss":
                damage = performSapphireDragonSpecial();
                break;
            case "Emerald Revenant Boss":
                damage = performEmeraldRevenantSpecial();
                break;
            case "Demon Lord Azrok":
                damage = performDemonLordSpecial();
                break;
            default:
                damage = performGenericSpecial();
                break;
        }

        return damage;
    }

    private float performCrystalSerpenSpecial() {
        addCombatLog("Crystal Serpent Boss phóng ra tia crystal xuyên thấu!");
        float damage = enemy.getAttackPower() * 1.5f + currentLevel;

        // Chance to disable additional cells
        if (random.nextFloat() < 0.3f) {
            int cellToDisable = random.nextInt(25);
            if (!disabledCells.contains(cellToDisable)) {
                disabledCells.add(cellToDisable);
                addCombatLog("Thêm một ô bị vô hiệu hóa!");
            }
        }

        return damage;
    }

    private float performSapphireDragonSpecial() {
        addCombatLog("Sapphire Dragon Boss thở ra ngọn lửa rồng xanh!");
        float damage = enemy.getAttackPower() * 1.4f + currentLevel * 0.8f;

        // Chance to apply burn
        if (random.nextFloat() < 0.4f && !playerStatusDuration.containsKey("BURN")) {
            playerStatusDuration.put("BURN", 3);
            addCombatLog("Bạn bị bỏng bởi ngọn lửa rồng!");
        }

        return damage;
    }

    private float performEmeraldRevenantSpecial() {
        addCombatLog("Emerald Revenant Boss triệu hồi năng lượng tối!");
        float damage = enemy.getAttackPower() * 1.3f + currentLevel * 0.7f;

        // Chance to apply toxic and reduce grid size temporarily
        if (random.nextFloat() < 0.3f && !playerStatusDuration.containsKey("TOXIC")) {
            playerStatusDuration.put("TOXIC", 2);
            addCombatLog("Bạn bị nhiễm độc tố tối!");
        }

        return damage;
    }

    private float performDemonLordSpecial() {
        addCombatLog("Demon Lord Azrok phóng ra năng lượng hủy diệt!");
        float damage = enemy.getAttackPower() * 2.0f + currentLevel;

        // Multiple effects for final boss
        if (random.nextFloat() < 0.5f) {
            if (random.nextBoolean() && !playerStatusDuration.containsKey("BURN")) {
                playerStatusDuration.put("BURN", 2);
                addCombatLog("Bạn bị lửa địa ngục thiêu đốt!");
            } else if (!playerStatusDuration.containsKey("TOXIC")) {
                playerStatusDuration.put("TOXIC", 2);
                addCombatLog("Bạn bị nhiễm độc tố quỷ!");
            }
        }

        return damage;
    }

    private float performGenericSpecial() {
        addCombatLog(enemyName + " sử dụng kỹ năng đặc biệt!");
        return enemy.getAttackPower() * 1.4f + currentLevel * 0.5f;
    }

    private void performDefensiveAction() {
        addCombatLog(enemyName + " tập trung phòng thủ và hồi phục sức mạnh!");

        // Small heal and temporary defense boost
        float healAmount = enemyMaxHealth * 0.08f;
        enemyHealth = Math.min(enemyMaxHealth, enemyHealth + healAmount);
        addFloatingText("+" + (int) healAmount + " HP", 955, 610, Color.GREEN);

        // Remove one negative status effect if any
        if (enemyStatusDuration.containsKey("BURN")) {
            enemyStatusDuration.remove("BURN");
            addCombatLog(enemyName + " đã loại bỏ hiệu ứng bỏng!");
        } else if (enemyStatusDuration.containsKey("TOXIC")) {
            enemyStatusDuration.remove("TOXIC");
            addCombatLog(enemyName + " đã loại bỏ hiệu ứng độc!");
        }
    }

    private void handlePostAttackEffects(int finalDamage) {
        if (finalDamage == 0) {
            renderer.startActionAnimation(1, false);
        } else if (finalDamage < 0) {
            // Miss animation handled in renderer
        } else {
            if (isEnemyBoss()) {
                renderer.startActionAnimation(3, true);
            } else {
                renderer.startActionAnimation(2, true);
            }
            addFloatingText("-" + finalDamage + " HP", 327, 480, Color.RED);
        }
    }

    private void endEnemyTurn() {
        isPlayerTurn = true;
        letterGrid.regenerateGrid();
        addCombatLog("---Đến lượt của bạn!---");
        checkPlayerStatusEffects();
        if (isEnemyBoss()) applyBossEffects();
    }

    // Helper class for action probabilities
    private static class EnemyActionProbabilities {
        float lightAttack = 0.4f;
        float heavyAttack = 0.3f;
        float heal = 0.15f;
        float special = 0.1f;
        float defensive = 0.03f;
        float miss = 0.02f;
    }

    float timerAction = 0;

    public boolean normalAttack() {
        enemyHasActed = false;
        if (isCombatMode && isPlayerTurn) {
            float damage = currentLevel + (wordDamageMultiplier - playerNerf + attackBuff) - enemy.getDefensePower() * 0.5f;
            damage = Math.max(1, damage);
            if (isEnemyLord() && damage < 15) {
                if (random.nextFloat() < 0.1f && !playerStatusDuration.containsKey("TOXIC") && !enemyStatusDuration.containsKey("FREEZE")) {
                    playerStatusDuration.put("TOXIC", 2);
                } else if (random.nextFloat() < 0.2f && !playerStatusDuration.containsKey("BURN") && !enemyStatusDuration.containsKey("FREEZE")) {
                    playerStatusDuration.put("BURN", 2);
                }

                addCombatLog("Lord " + enemyName + " chống chọi được đòn tấn công yếu!");
            } else {
                addCombatLog("Tấn công thường gây " + (int) damage + " sát thương!");
            }

            letterGrid.clearWord();

            final float finalDamage = damage;
            if (damage > 0) {

                playerAttack("", (int) damage, () -> {
                    checkCombatEnd();
                    renderer.startActionAnimation(2, false);
                    if (isCombatMode && enemyHealth > 0) {
                        isPlayerTurn = false;
                        addCombatLog("---Đến lượt của " + enemyName + "!---");
                    }
                    addFloatingText("-" + finalDamage, 900, 600, Color.RED);

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

        timerAction = 5f;

        return true;
    }

    public boolean submitWord() {
        if (!active) return false;
        enemyHasActed = false;
        String word = letterGrid.getCurrentWord();
        if (word.isEmpty()) {
            addCombatLog("Từ phải có ít nhất 1 chữ cái!");
            return false;
        }
        if (gameController.getCharacter().getLearnedWords().contains(word.toUpperCase()) || wordValidator.isValidWord(word)) {
            int points = wordValidator.getTotalScore(word.trim());
            this.experienceGain += points;

            lastSubmittedWord = word;
            String effectLog = "";
            String stats = "";
            String wordLower = word.toLowerCase();

            boolean isBurn = wordLower.contains("fire") || wordLower.contains("burn") || wordLower.contains("flame");
            boolean isPoison = wordLower.contains("poison") || wordLower.contains("toxic") || wordLower.contains("venom");
            boolean isFreeze = wordLower.contains("ice") || wordLower.contains("frost") || wordLower.contains("freeze");
            boolean isBuff = wordLower.contains("god") || wordLower.contains("world") || wordLower.contains("return");

            if (isBurn) {
                if (enemyStatusDuration.containsKey("FREEZE")) {
                    stats = "";
                    enemyStatusDuration.remove("FREEZE");
                    enemyStatusDuration.remove("BURN");
                    addCombatLog("Băng làm mất hiệu ứng bỏng!");
                } else {
                    stats = "BURN";
                    effectLog = enemyName + " bị bỏng!";
                }
            } else if (isPoison) {
                stats = "POISON";
                effectLog = enemyName + " bị trúng độc!";
            } else if (isFreeze) {
                if (enemyStatusDuration.containsKey("BURN")) {
                    stats = "";
                    enemyStatusDuration.remove("FREEZE");
                    enemyStatusDuration.remove("BURN");
                    addCombatLog("Bỏng làm mất hiệu ứng đóng băng!");
                } else {
                    stats = "FREEZE";
                    effectLog = enemyName + " bị đóng băng!";
                }
            } else if (isBuff) {
                stats = "BUFF";
                effectLog = "Vị thần sẽ phù hộ bạn!";
            } else {
                stats = "";
            }


            if (gameController.getCharacter().updateDict(word)) gameController.getDictionaryView().addNewWord(word);

            if (isCombatMode && isPlayerTurn) {
                float damage = points + (wordDamageMultiplier - playerNerf) + attackBuff - enemy.getDefensePower() * 0.7f;
                damage = Math.max(1, damage);

                if (isEnemyLord() && damage < 15) {
                    damage = 0;
                    // 20% chance to apply TOXIC or BURN status, but not if enemy is frozen
                    if (random.nextFloat() < 0.2f && !playerStatusDuration.containsKey("TOXIC") && !enemyStatusDuration.containsKey("FREEZE")) {
                        playerStatusDuration.put("TOXIC", 2);
                        addCombatLog("Bạn bị Lord phản đòn, bị trúng độc nhẹ!");
                    } else if (random.nextFloat() < 0.4f && !playerStatusDuration.containsKey("BURN") && !enemyStatusDuration.containsKey("FREEZE")) {
                        playerStatusDuration.put("BURN", 2);
                        addCombatLog("Bạn bị Lord phản đòn, bị bỏng nhẹ!");
                    }
                } else {
                    addCombatLog("Từ '" + word + "' gây " + (int) damage + " sát thương!");
                }

                // Replace the playerAttack call with this:
                if (isCombatMode && isPlayerTurn) {
                    letterGrid.clearWord();

                    if (damage > 0) {
                        playerAttack(word, (int) damage, () -> {
                            renderer.startActionAnimation(3, false);
                            checkCombatEnd();
                            renderer.startActionAnimation(3, false);
                            if (isCombatMode && enemyHealth > 0) {
                                isPlayerTurn = false;
                                addCombatLog("---Đến lượt của " + enemyName + "!---");
                            }
                        });

                        if (!stats.isEmpty() && !stats.equals("BUFF")) {
                            if (!enemyStatusDuration.containsKey(stats)) {
                                enemyStatusDuration.put(stats, 2); // 4 lượt hiệu ứng
                            } else {
                                enemyStatusDuration.put(stats, enemyStatusDuration.get(stats) + 1);
                                addCombatLog(effectLog);
                            }
                            playerMana = Math.max(0, playerMana - 3);
                        } else if (stats.equals("BUFF")) {
                            if (!playerStatusDuration.containsKey("BUFF_ATK") && !playerStatusDuration.containsKey("BUFF_DEF")) {
                                playerStatusDuration.put("BUFF_ATK", 2);
                                playerStatusDuration.put("BUFF_DEF", 2);
                                addCombatLog(effectLog);
                            }
                        }
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

            return true;
        } else {
            timerAction = 5f;
            addCombatLog("Từ '" + word + "' không hợp lệ!");
            gameController.getCharacter().updateWrongWordCount();
            checkCombatEnd();
            playerMiss("", 0, () -> {
                if (isCombatMode && enemyHealth > 0) {
                    isPlayerTurn = false;

                    addCombatLog("---Đến lượt của " + enemyName + "!---");
                }
            });
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
            gameController.getMusicController().playMusic("defeat");

            if (!isGameOver) {
                gameController.getCharacter().setDirection("knocked_down");
                gameController.returnToTower(enemyName);
                String eventId = currentEvent != null ? currentEvent.getId() : gameController.getCurrentEventId();
                gameController.getEventManager().completeEvent(eventId);
                gameController.setCompletedEvent();
                gameController.setRenderCharacter(true);

            }

            gameController.getCharacter().resetWinStreak();
            timerAction = 0;
            achievementManager.updateProgress(Achievement.AchievementType.FALLEN, 1);
            gameController.getMusicController().playMusic("defeat");
            gameController.getMapRenderer().setRenderInfoCard(false);

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
            gameController.getMusicController().playMusic("victory");
            switch (enemyName) {
                case "Crystal Serpent Boss":
                    achievementManager.updateProgress(Achievement.AchievementType.ENEMY_WIN_1, 1);
                    gameController.addFlag("crystal_serpent_boss_defeated");
                    break;
                case "Sapphire Dragon Boss":
                    System.out.println("Defeated Sapphire Dragon Boss");
                    achievementManager.updateProgress(Achievement.AchievementType.ENEMY_WIN_2, 1);
                    gameController.addFlag("sapphire_dragon_boss_defeated");
                    break;
                case "Emerald Revenant Boss":
                    achievementManager.updateProgress(Achievement.AchievementType.ENEMY_WIN_3, 1);
                    gameController.addFlag("emerald_revenant_boss_defeated");
                    gameController.addFlag("boss");
                    gameController.addFlag("quest_024");
                    break;
                case "Demon Lord Azrok":
                    achievementManager.updateProgress(Achievement.AchievementType.ENEMY_WIN, 1);
                    gameController.addFlag("demon_lord_azrok_defeated");
                    gameController.addFlag("return");
                    break;
            }

            gameController.getMapRenderer().setZoomed(false);
            gameController.setRenderCharacter(true);
            achievementManager.updateProgress(Achievement.AchievementType.COMBAT_WIN, 1);

            this.newLevel = gameController.getCharacter().addExperience(calculateCombatRewards());
            gameController.getMapRenderer().setRenderInfoCard(false);
            gameController.getMusicController().playMusic("victory");
        }
    }

    private float calculateCombatRewards() {
        // Base XP from enemy
        float baseXP = this.experienceGain * (1 + currentLevel * 0.1f);

        // Performance bonuses
        float timeBonus = (COMBAT_TIME_LIMIT - combatTimer) / COMBAT_TIME_LIMIT;
        float healthBonus = playerHealth / playerMaxHealth;
        float wordBonus = experienceGain / 10f;

        float totalXP = baseXP * (1 + timeBonus * 0.5f + healthBonus * 0.3f + wordBonus * 0.2f);

        // Gold calculation
        int goldReward = (int) (this.experienceGain * (1 + random.nextFloat() * 0.5f));

        addCombatLog("Nhận được " + (int) totalXP + " XP và " + goldReward + " vàng!");

        gameController.getCharacter().addScore(goldReward);

        return totalXP;
    }

    public int getNewLevel() {
        return newLevel;
    }

    public Map<Rectangle, Items> getItemRectMap() {
        return itemRectMap;
    }

    public void cleanupCombatState() {
        isCombatMode = false;
        combatTimer = 0;
        combatTimeUp = false;
        disabledCells.clear();
        combatLogLines.clear();
        lastSubmittedWord = "";

        playerStatusDuration.clear();
        enemyStatusDuration.clear();
        this.attackBuff = 0;
        this.playerDef = 0;

        // Clear item rect map to free memory
        itemRectMap.clear();

        // Reset overlay state
        currentOverlay = OverlayType.NONE;
    }

    private void endCombat(boolean victory) {
        isVictory = victory;
        cleanupCombatState();
    }


    private float playerDefend = 0;

    public void startCombat(Enemy enemy) {
        cleanupCombatState();
        if (renderer != null) {
            renderer.resetForNewCombat();
        }

        // Initialize basic combat state
        this.enemy = enemy;
        this.enemyName = enemy.getEnemyName();
        this.wordDamageMultiplier = gameController.getCharacter().getDamage();
        this.playerDefend = gameController.getCharacter().getDefend();
        this.isVictory = false;
        this.experienceGain = 0;
        this.isCombatMode = true;
        this.isPlayerTurn = true;
        this.currentLevel = gameController.getCharacter().getLevel();

        // Initialize player stats
        this.playerHealth = gameController.getCharacter().getHealth();
        this.playerMana = gameController.getCharacter().getMana();
        this.playerMaxMana = gameController.getCharacter().getMaxMana();
        this.playerMaxHealth = gameController.getCharacter().getMaxHealth();
        this.playerName = gameController.getCharacter().getName();
        this.gender = gameController.getCharacter().getGender().toString();

        // Reset combat modifiers
        this.playerDef = 0;
        this.attackBuff = 0;
        this.atkNerf = 0;
        this.playerNerf = 0;

        // Calculate scaled enemy stats (no scaling for level 1)
        EnemyStats scaledStats = calculateScaledEnemyStats(enemy, currentLevel);
        this.enemyMaxHealth = scaledStats.health;
        this.enemyHealth = scaledStats.health;

        // Update enemy stats for combat
        enemy.setAttackPower(scaledStats.attackPower);
        enemy.setDefensePower(scaledStats.defensePower);

        // Calculate difficulty after scaling
        float difficultyScore = calculateDifficultyScore(scaledStats, currentLevel);
        this.difficultyText = getDifficultyText(difficultyScore);

        // Initialize combat
        this.achievementManager = gameController.getAchievementManager();

        // Different combat start messages based on level
        if (currentLevel == 1) {
            addCombatLog("🗡️ Trận chiến đầu tiên với " + enemyName + "!");
            addCombatLog("💡 Hãy thử tạo từ để tấn công hoặc dùng Tấn Công Thường!");
        } else {
            addCombatLog("⚔️ Bắt đầu chiến đấu với " + enemyName + " (Độ khó: " + difficultyText + ")!");

            // Show scaled stats info for higher levels
            if (currentLevel > 1) {
                addCombatLog("📊 Enemy được tăng cường cho Level " + currentLevel);
            }
        }

        letterGrid.regenerateGrid();

        // Add difficulty warning (more specific for level 1)
        if (currentLevel == 1) {
        } else if (difficultyScore >= 80) {
            addCombatLog("⚠️ NGUY HIỂM: Trận chiến cực kỳ khó khăn!");
        } else if (difficultyScore >= 60) {
            addCombatLog("⚠️ Cảnh báo: Trận chiến này khá thách thức!");
        }

        // Load renderer assets
        renderer.loadPlayerTexture(gameController.getCharacter().getGender().toString());

        // Apply special enemy effects (reduced for level 1)
        applySpecialEnemyEffects();
    }

    private EnemyStats calculateScaledEnemyStats(Enemy enemy, int playerLevel) {
        // Base enemy stats
        float baseHealth = enemy.getHealth();
        float baseAttack = enemy.getAttackPower();
        float baseDefense = enemy.getDefensePower();

        // No scaling for level 1
        if (playerLevel <= 1) {
            return new EnemyStats((int) baseHealth, (int) baseAttack, (int) baseDefense);
        }

        // Scaling factors based on enemy type (starting from level 2)
        float healthScaling = 1.0f;
        float attackScaling = 1.0f;
        float defenseScaling = 1.0f;

        // Level-based scaling with different rates for different enemy types
        int levelDiff = playerLevel - 1; // Start scaling from level 2

        if (isEnemyLord()) {
            // Lords scale aggressively
            healthScaling = 1.0f + (levelDiff * 0.20f);
            attackScaling = 1.0f + (levelDiff * 0.15f);
            defenseScaling = 1.0f + (levelDiff * 0.10f);
        } else if (isEnemyBoss()) {
            // Bosses have strong scaling
            healthScaling = 1.0f + (levelDiff * 0.15f);
            attackScaling = 1.0f + (levelDiff * 0.12f);
            defenseScaling = 1.0f + (levelDiff * 0.08f);
        } else {
            // Regular enemies have moderate scaling
            healthScaling = 1.0f + (levelDiff * 0.10f);
            attackScaling = 1.0f + (levelDiff * 0.08f);
            defenseScaling = 1.0f + (levelDiff * 0.05f);
        }

        // Apply diminishing returns for high levels
        if (playerLevel > 10) {
            float diminishingFactor = 1.0f - ((playerLevel - 10) * 0.02f);
            diminishingFactor = Math.max(0.7f, diminishingFactor); // Minimum 70% effectiveness

            healthScaling *= diminishingFactor;
            attackScaling *= diminishingFactor;
            defenseScaling *= diminishingFactor;
        }

        // Calculate final stats
        float finalHealth = baseHealth * healthScaling;
        float finalAttack = baseAttack * attackScaling;
        float finalDefense = baseDefense * defenseScaling;

        // Ensure stats don't go below base values
        finalHealth = Math.max(finalHealth, baseHealth);
        finalAttack = Math.max(finalAttack, baseAttack);
        finalDefense = Math.max(finalDefense, baseDefense);

        return new EnemyStats((int) finalHealth, (int) finalAttack, (int) finalDefense);
    }


    private float calculateDifficultyScore(EnemyStats enemyStats, int playerLevel) {
        // Player power estimation based on level and stats
        float playerPower = gameController.getCharacter().getDamage() +
                gameController.getCharacter().getDefend() +
                gameController.getCharacter().getHealth() * 0.1f +
                (playerLevel * 3.0f);

        // Enemy power calculation with weight adjustments
        float enemyPower = (enemyStats.health * 0.4f) +
                (enemyStats.attackPower * 3.0f) +
                (enemyStats.defensePower * 2.0f);

        // Base difficulty ratio
        float difficultyRatio = enemyPower / Math.max(playerPower, 1.0f);

        // Apply enemy type modifiers
        if (isEnemyLord()) {
            difficultyRatio *= 2.2f;
        } else if (isEnemyBoss()) {
            difficultyRatio *= 1.8f;
        }

        // Level 1 adjustment - make it easier
        if (playerLevel == 1) {
            difficultyRatio *= 0.6f;
        } else if (playerLevel <= 3) {
            difficultyRatio *= 0.8f;
        }

        // Convert to 0-100 scale with better distribution
        float finalScore = Math.min(difficultyRatio * 20.0f, 100.0f);

        return Math.max(5.0f, finalScore); // Minimum difficulty of 5
    }

    private String getDifficultyText(float difficultyScore) {
        if (difficultyScore < 15) {
            return "Rất Dễ";
        } else if (difficultyScore < 30) {
            return "Dễ";
        } else if (difficultyScore < 45) {
            return "Vừa";
        } else if (difficultyScore < 60) {
            return "Khó";
        } else if (difficultyScore < 80) {
            return "Rất Khó";
        } else {
            return "Cực Khó";
        }
    }

    private void applySpecialEnemyEffects() {
        if (isEnemyBoss()) {
            // Reduced boss effects for low levels
            if (currentLevel <= 3) {
                addCombatLog("🛡️ Boss effects được giảm nhẹ cho người chơi mới!");
            }

            switch (enemyName) {
                case "Sapphire Dragon Boss":
                    int regenDuration = Math.max(3, 10 - currentLevel);
                    enemyStatusDuration.put("REGEN", regenDuration);
                    addCombatLog("Sapphire Dragon Boss có khả năng hồi phục liên tục!");
                    break;
                case "Crystal Serpent Boss":
                    addCombatLog("Crystal Serpent Boss có thể vô hiệu hóa ô chữ!");
                    break;
                case "Emerald Revenant Boss":
                    addCombatLog("Emerald Revenant Boss sở hữu sức mạnh độc tố!");
                    break;
            }
            applyBossEffects();

        } else if (isEnemyLord()) {
            // Lord effects
            if (!gameController.getCharacter().getFlags().contains("frost_guardian_defeated")) {
                String description = enemy.getEnemyDescription();
                if (description != null) {
                    enemy.setEnemyDescription(description.replace("15", "**"));
                }
            }

            if (currentLevel == 1) {
                addCombatLog("Một Lord mạnh mẽ! Hãy cẩn thận!");
            } else {
                addCombatLog(" Một Lord hùng mạnh với sức mạnh level " + currentLevel + "!");
            }
        }
    }

    // Helper class for enemy stats
    private static class EnemyStats {
        final int health;
        final int attackPower;
        final int defensePower;

        EnemyStats(int health, int attackPower, int defensePower) {
            this.health = health;
            this.attackPower = attackPower;
            this.defensePower = defensePower;
        }
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

    public void clearSelection() {
        letterGrid.clearSelection();
    }

    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    public void dispose() {
        active = false;
        // Only dispose what this class owns
        if (renderer != null) {
            renderer.dispose();
        }
        cleanupCombatState();
    }

    public void setCurrentEvent(MapEvent currentEvent) {
        this.currentEvent = currentEvent;
    }

    public GameController getGameController() {
        return gameController;
    }

    public boolean isGameOver() {
        return isGameOver;
    }

    public Map<String, Integer> getEnemyStatusDuration() {
        return enemyStatusDuration;
    }

    public Map<String, Integer> getPlayerStatusDuration() {
        return playerStatusDuration;
    }

    public boolean isCombatMode() {
        return isCombatMode;
    }

    public boolean isPlayerTurn() {
        return isPlayerTurn;
    }


    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isVictory() {
        return isVictory;
    }

    public void setVictory(boolean victory) {
        isVictory = victory;
    }

    public CardAnimationManager getCardAnimationManager() {
        return cardAnimationManager;
    }


    public Enemy getEnemy() {
        return enemy;
    }

    public OverlayType getCurrentOverlay() {
        return currentOverlay;
    }

    public String getDifficultyText() {
        return difficultyText;
    }


}