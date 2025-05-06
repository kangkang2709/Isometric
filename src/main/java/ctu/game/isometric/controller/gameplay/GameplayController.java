package ctu.game.isometric.controller.gameplay;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import ctu.game.isometric.controller.GameController;
import ctu.game.isometric.model.game.GameState;
import ctu.game.isometric.model.word.LetterGrid;
import ctu.game.isometric.util.WordScorer;
import ctu.game.isometric.util.WordValidator;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class GameplayController {
    // Core components
    private final GameController gameController;
    private final LetterGrid letterGrid;
    private final WordValidator wordValidator;
    private final Random random = new Random();

    // Game state
    private int currentScore;
    private int highScore;
    private boolean active;
    private String currentMessage = "";
    private float messageTimer = 0;

    // UI components
    private BitmapFont titleFont, regularFont, bigFont;
    private GlyphLayout layout;
    private Viewport viewport;
    private Texture whiteTexture;
    private Map<String, Texture> textureCache = new HashMap<>();

    // Button areas
    private Rectangle submitButtonRect, clearButtonRect, exitButtonRect;

    // Combat state
    private boolean isCombatMode = false;
    private boolean isPlayerTurn = true;
    private int playerHealth = 100;
    private int enemyHealth = 100;
    private int enemyMaxHealth = 100;
    private String enemyName = "Enemy";
    private float enemyActionTimer = 0;
    private static final float ENEMY_TURN_DELAY = 2.5f;
    private String combatLog = "";
    private int enemyDamageMultiplier = 1;
    private int wordDamageMultiplier = 1;
    private boolean autoStartCombat = false;
    private String playerName;

    public GameplayController(GameController gameController) {
        this.gameController = gameController;
        this.letterGrid = new LetterGrid();
        this.wordValidator = new WordValidator();
        this.playerName = gameController.getCharacter().getName();
        initializeUI();
    }

    private void initializeUI() {
        // Initialize fonts
        titleFont = new BitmapFont();
        titleFont.getData().setScale(2.0f);
        regularFont = new BitmapFont();
        bigFont = new BitmapFont();
        bigFont.getData().setScale(1.5f);

        layout = new GlyphLayout();
        viewport = new FitViewport(1280, 720);

        // Create white texture for drawing colored rectangles
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        whiteTexture = new Texture(pixmap);
        pixmap.dispose();

        // Initialize button rectangles
        submitButtonRect = new Rectangle(900, 350, 200, 50);
        clearButtonRect = new Rectangle(900, 280, 200, 50);
    }

    public void update(float delta) {
        if (!active) return;

        // Update message timer
        if (messageTimer > 0) {
            messageTimer -= delta;
            if (messageTimer <= 0) currentMessage = "";
        }

        // Update appropriate mode
        if (isCombatMode) updateCombat(delta);
        else updateWordPuzzle(delta);
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

        // Handle player input
        if (Gdx.input.justTouched()) {
            Vector3 touchPos = getTouchPosition();

            if (submitButtonRect.contains(touchPos.x, touchPos.y)) {
                submitWord();
            } else if (clearButtonRect.contains(touchPos.x, touchPos.y)) {
                clearSelection();
            } else {
                checkGridClick(touchPos.x, touchPos.y);
            }
        }
    }

    private void updateWordPuzzle(float delta) {
        if (Gdx.input.justTouched()) {
            Vector3 touchPos = getTouchPosition();

            if (submitButtonRect.contains(touchPos.x, touchPos.y)) {
                submitWord();
            } else if (clearButtonRect.contains(touchPos.x, touchPos.y)) {
                clearSelection();
            } else {
                checkGridClick(touchPos.x, touchPos.y);
            }
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
        float gridY = isCombatMode ? 150 : 110;
        float cellSize = gridSize / 5;

        if (x >= gridX && x < gridX + gridSize && y >= gridY && y < gridY + gridSize) {
            int cellX = (int)((x - gridX) / cellSize);
            int cellY = 4 - (int)((y - gridY) / cellSize);
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

        if (isCombatMode) renderCombatUI(batch);
        else renderWordPuzzleUI(batch);
    }

    private void renderWordPuzzleUI(SpriteBatch batch) {
        // Draw title
        drawCenteredText(batch, titleFont, "WORD PUZZLE", viewport.getWorldWidth()/2, 650, Color.WHITE);

        // Draw letter grid
        drawLetterGrid(batch);

        // Draw current word and score
        String currentWord = letterGrid.getCurrentWord();
        bigFont.setColor(Color.WHITE);
        bigFont.draw(batch, currentWord, 900, 500);

        regularFont.setColor(Color.WHITE);
        regularFont.draw(batch, "Score: " + currentScore + "   High Score: " + highScore, 900, 450);

        // Draw message
        if (!currentMessage.isEmpty()) {
            regularFont.setColor(Color.YELLOW);
            regularFont.draw(batch, currentMessage, 900, 420);
        }

        // Draw buttons
        drawButton(batch, submitButtonRect, "Submit Word");
        drawButton(batch, clearButtonRect, "Clear Selection");
    }

    private void renderCombatUI(SpriteBatch batch) {
        // Draw battle background
        batch.setColor(0.15f, 0.15f, 0.3f, 1);
        batch.draw(whiteTexture, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());

        // Draw player and enemy
        drawCombatCharacter(batch, playerName, playerHealth, 100, 50, 600, true);
        drawCombatCharacter(batch, enemyName, enemyHealth, enemyMaxHealth, viewport.getWorldWidth() - 300, 600, false);

        // Draw combat log
        drawDialogBox(batch, 50, 50, 300, 150);
        regularFont.setColor(Color.WHITE);
        regularFont.draw(batch, combatLog, 80, 170);
        drawCenteredText(batch, regularFont, (isPlayerTurn ? "Player" : enemyName) + " TURN", viewport.getWorldWidth()/2, 700, Color.WHITE);

        // Update and draw buttons
        float buttonX = viewport.getWorldWidth() - 250;
        float buttonY = 50;
        float buttonWidth = 200;
        float buttonHeight = 50;
        float buttonSpacing = 60;

        submitButtonRect = new Rectangle(buttonX, buttonY + buttonSpacing*2, buttonWidth, buttonHeight);
        clearButtonRect = new Rectangle(buttonX, buttonY + buttonSpacing, buttonWidth, buttonHeight);

        drawPokemonButton(batch, submitButtonRect, "CAST WORD", isPlayerTurn);
        drawPokemonButton(batch, clearButtonRect, "CLEAR", isPlayerTurn);

        // Draw compact letter grid and word info during player turn
        if (isPlayerTurn) {
            drawCompactLetterGrid(batch);

            String currentWord = letterGrid.getCurrentWord();
            if (currentWord.length() > 0) {
                drawCenteredText(batch, regularFont, "Spell: " + currentWord, viewport.getWorldWidth()/2, 600, Color.WHITE);

                int potentialScore = WordScorer.getTotalScore(currentWord);
                if(wordValidator.isValidWord(currentWord)) {
                    drawCenteredText(batch, regularFont, "Power: " + (potentialScore * wordDamageMultiplier),
                            viewport.getWorldWidth()/2, 570, Color.WHITE);
                }
            }
        }
    }

    private void drawCenteredText(SpriteBatch batch, BitmapFont font, String text, float x, float y, Color color) {
        layout.setText(font, text);
        font.setColor(color);
        font.draw(batch, text, x - layout.width/2, y);
    }

    private void drawCombatCharacter(SpriteBatch batch, String name, int currentHealth,
                                     int maxHealth, float x, float y, boolean isPlayer) {
        // Draw character image
        batch.setColor(1, 1, 1, 1);
        Texture characterTexture = getCharacterTexture(isPlayer ? "characters/player.png" : "enemy/" + name + ".png");
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
        float gridY = 150;

        char[][] grid = letterGrid.getGrid();
        boolean[][] selected = letterGrid.getSelectedCells();

        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                float screenX = gridX + x * cellSize;
                float screenY = gridY + (4-y) * cellSize;

                // Cell background
                batch.setColor(selected[y][x] ? new Color(0.2f, 0.6f, 1f, 1) : new Color(0.9f, 0.9f, 0.8f, 1));
                batch.draw(whiteTexture, screenX, screenY, cellSize, cellSize);

                // Cell border
                batch.setColor(0.3f, 0.3f, 0.3f, 1);
                batch.draw(whiteTexture, screenX, screenY, cellSize, 1); // Bottom
                batch.draw(whiteTexture, screenX, screenY, 1, cellSize); // Left
                batch.draw(whiteTexture, screenX + cellSize - 1, screenY, 1, cellSize); // Right
                batch.draw(whiteTexture, screenX, screenY + cellSize - 1, cellSize, 1); // Top

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

    private void drawPokemonHealthBar(SpriteBatch batch, String name, int current, int max, float x, float y) {
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
        float healthPercentage = (float)current / max;
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

    private void drawDialogBox(SpriteBatch batch, float x, float y, float width, float height) {
        // Background
        batch.setColor(0.1f, 0.1f, 0.1f, 0.85f);
        batch.draw(whiteTexture, x, y, width, height);

        // Border
        batch.setColor(0.7f, 0.7f, 0.7f, 1);
        drawRect(batch, x, y, width, height, 2);

        // Inner shadow
        batch.setColor(0.3f, 0.3f, 0.3f, 0.5f);
        batch.draw(whiteTexture, x + 4, y + 4, width - 8, height - 8);
    }

    private void drawPokemonButton(SpriteBatch batch, Rectangle buttonRect, String text, boolean enabled) {
        // Background
        batch.setColor(enabled ? new Color(0.4f, 0.4f, 0.8f, 0.9f) : new Color(0.3f, 0.3f, 0.4f, 0.6f));
        batch.draw(whiteTexture, buttonRect.x, buttonRect.y, buttonRect.width, buttonRect.height);

        // Border
        batch.setColor(0.7f, 0.7f, 0.9f, 1);
        drawRect(batch, buttonRect.x, buttonRect.y, buttonRect.width, buttonRect.height, 2);

        // Text
        layout.setText(regularFont, text);
        regularFont.setColor(enabled ? Color.WHITE : Color.GRAY);
        regularFont.draw(batch, text,
                buttonRect.x + (buttonRect.width - layout.width) / 2,
                buttonRect.y + (buttonRect.height + layout.height) / 2);
    }

    private void drawLetterGrid(SpriteBatch batch) {
        float gridX = 250;
        float gridY = 110;
        float gridSize = 500;
        float cellSize = gridSize / 5;
        float padding = 4;

        // Grid background
        batch.setColor(0.3f, 0.3f, 0.4f, 0.7f);
        batch.draw(whiteTexture, gridX - padding, gridY - padding, gridSize + padding*2, gridSize + padding*2);

        char[][] grid = letterGrid.getGrid();
        boolean[][] selected = letterGrid.getSelectedCells();

        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                float screenX = gridX + x * cellSize + padding/2;
                float screenY = gridY + (4-y) * cellSize + padding/2;
                float actualCellSize = cellSize - padding;

                // Cell background
                if (selected[y][x]) {
                    batch.setColor(0.3f, 0.7f, 1f, 0.5f);
                    batch.draw(whiteTexture, screenX - 3, screenY - 3, actualCellSize + 6, actualCellSize + 6);
                    batch.setColor(0.2f, 0.6f, 1f, 1);
                } else {
                    batch.setColor(0.85f, 0.85f, 0.7f, 1);
                }
                batch.draw(whiteTexture, screenX, screenY, actualCellSize, actualCellSize);

                // Letter
                String letter = String.valueOf(grid[y][x]);
                layout.setText(regularFont, letter);
                regularFont.setColor(Color.BLACK);
                regularFont.draw(batch, letter,
                        screenX + (actualCellSize - layout.width) / 2,
                        screenY + actualCellSize - (actualCellSize - layout.height) / 2);
            }
        }
        batch.setColor(Color.WHITE);
    }

    private void drawButton(SpriteBatch batch, Rectangle buttonRect, String text) {
        batch.setColor(0.4f, 0.4f, 0.8f, 1);
        batch.draw(whiteTexture, buttonRect.x, buttonRect.y, buttonRect.width, buttonRect.height);

        batch.setColor(0.2f, 0.2f, 0.6f, 1);
        batch.draw(whiteTexture, buttonRect.x, buttonRect.y, buttonRect.width, 2);
        batch.draw(whiteTexture, buttonRect.x, buttonRect.y, 2, buttonRect.height);

        layout.setText(regularFont, text);
        regularFont.setColor(Color.WHITE);
        regularFont.draw(batch, text,
                buttonRect.x + (buttonRect.width - layout.width) / 2,
                buttonRect.y + (buttonRect.height + layout.height) / 2);

        batch.setColor(Color.WHITE);
    }

    private void performEnemyAction() {
        int damage = (random.nextInt(8) + 3) * enemyDamageMultiplier;
        playerHealth -= damage;

        int action = random.nextInt(10);
        if (action < 7) { // 70% normal attack
            combatLog = enemyName + " attacks for " + damage + " damage!";
        } else if (action < 9) { // 20% power attack
            int extraDamage = random.nextInt(5) + 1;
            playerHealth -= extraDamage;
            combatLog = enemyName + " performs a power attack for " + (damage + extraDamage) + " damage!";
        } else { // 10% heal
            int heal = random.nextInt(8) + 3;
            enemyHealth = Math.min(enemyMaxHealth, enemyHealth + heal);
            combatLog = enemyName + " recovers " + heal + " health!";
        }

        checkCombatEnd();
        if (isCombatMode) {
            isPlayerTurn = true;
            letterGrid.regenerateGrid();
        }
    }

    private void checkCombatEnd() {
        if (playerHealth <= 0) {
            combatLog = "You were defeated by " + enemyName + "!";
            playerHealth = 0;
            endCombat(false);
        } else if (enemyHealth <= 0) {
            combatLog = "You defeated " + enemyName + "!";
            enemyHealth = 0;
            endCombat(true);
        }
    }

    private void endCombat(boolean victory) {
        if (victory) {
            int rewardPoints = random.nextInt(20) + 10;
            currentScore += rewardPoints;
            highScore = Math.max(currentScore, highScore);
            showMessage("Victory! +" + rewardPoints + " points!");
        } else {
            showMessage("Defeat! Better luck next time.");
        }

        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                isCombatMode = false;
                gameController.getCharacter().setHealth(playerHealth);
                gameController.setState(GameState.EXPLORING);
                dispose();
            }
        }, 3.0f);
    }

    public boolean submitWord() {
        if (!active) return false;

        String word = letterGrid.getCurrentWord();
        if (word.length() < 3) {
            showMessage("Words must be at least 3 letters long!");
            return false;
        }

        if (wordValidator.isValidWord(word)) {
            int points = WordScorer.getTotalScore(word);
            currentScore += points;
            highScore = Math.max(currentScore, highScore);

            if (isCombatMode && isPlayerTurn) {
                int damage = points * wordDamageMultiplier;
                enemyHealth -= damage;
                combatLog = "Your word '" + word + "' deals " + damage + " damage!";
                showMessage("+" + points + " points! " + damage + " damage!");

                checkCombatEnd();
                if (isCombatMode) {
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

    // Public methods for combat difficulty
    public void startCombatWithDifficulty(int difficulty) {
        switch(difficulty) {
            case 1: startEasyCombat(); break;
            case 2: startMediumCombat(); break;
            case 3: startHardCombat(); break;
            default: startEasyCombat(); break;
        }
    }

    public void startEasyCombat() {
        startCombat("Word Goblin", 50, 1, 1);
    }

    public void startMediumCombat() {
        startCombat("Word Troll", 100, 1, 2);
    }

    public void startHardCombat() {
        startCombat("Word Dragon", 200, 1, 3);
    }

    public void startCombat(String enemyName, int enemyHealth) {
        startCombat(enemyName, enemyHealth, wordDamageMultiplier, enemyDamageMultiplier);
    }

    public void startCombat(String enemyName, int enemyHealth, int wordMult, int enemyMult) {
        this.enemyName = enemyName;
        this.enemyMaxHealth = enemyHealth;
        this.enemyHealth = enemyHealth;
        this.wordDamageMultiplier = wordMult;
        this.enemyDamageMultiplier = enemyMult;
        this.playerHealth = gameController.getCharacter().getHealth();
        this.isCombatMode = true;
        this.isPlayerTurn = true;
        this.combatLog = "Combat with " + enemyName + " has begun!";
        letterGrid.regenerateGrid();
    }

    // Core game functions
    public void activate() {
        this.active = true;
        letterGrid.regenerateGrid();
        currentScore = 0;
        currentMessage = "";
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        if (autoStartCombat) {
            startCombat("enemy_01", 100);
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
    }

    // Getters
    public boolean isInCombatMode() { return isCombatMode; }
    public int getPlayerHealth() { return playerHealth; }
    public int getEnemyHealth() { return enemyHealth; }
    public LetterGrid getLetterGrid() { return letterGrid; }
    public int getCurrentScore() { return currentScore; }
    public int getHighScore() { return highScore; }
    public String getCurrentWord() { return letterGrid.getCurrentWord(); }
    public boolean isActive() { return active; }
}