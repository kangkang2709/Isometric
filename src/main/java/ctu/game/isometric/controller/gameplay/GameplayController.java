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
    private final GameController gameController;
    private final LetterGrid letterGrid;
    private final WordValidator wordValidator;

    private int currentScore;
    private int highScore;
    private boolean active;

    // Direct drawing components
    private BitmapFont titleFont;
    private BitmapFont regularFont;
    private BitmapFont bigFont;
    private GlyphLayout layout;
    private Viewport viewport;
    private Texture whiteTexture; // White texture for drawing colored rectangles
    private Map<String, Texture> textureCache = new HashMap<>();

    // UI components
    private String currentMessage = "";
    private float messageTimer = 0;

    // Button rectangles for hit detection
    private Rectangle submitButtonRect;
    private Rectangle clearButtonRect;
    private Rectangle exitButtonRect;
    private String playerName;
    // Combat related fields
    private boolean isCombatMode = false;
    private boolean isPlayerTurn = true;
    private int playerHealth = 100;
    private int enemyHealth = 100;
    private String enemyName = "Enemy";
    private float enemyActionTimer = 0;
    private static final float ENEMY_TURN_DELAY = 2.5f;
    private String combatLog = "";
    private Random random = new Random();

    // Combat balance parameters
    private int enemyDamageMultiplier = 1; // Controls enemy damage scaling
    private int wordDamageMultiplier = 1;  // Controls how much damage your words do
    private int enemyMaxHealth = 100;      // Starting enemy health
    private boolean autoStartCombat = false; // If true, starts combat automatically when activating controller

    public GameplayController(GameController gameController) {
        this.gameController = gameController;
        this.letterGrid = new LetterGrid();
        this.wordValidator = new WordValidator();
        this.currentScore = 0;
        this.highScore = 0;
        this.active = false;
        this.playerName = gameController.getCharacter().getName();
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
//        exitButtonRect = new Rectangle(900, 210, 200, 50);
    }

    public void update(float delta) {
        if (!active) return;

        // Update message timer
        if (messageTimer > 0) {
            messageTimer -= delta;
            if (messageTimer <= 0) {
                currentMessage = "";
            }
        }

        if (isCombatMode) {
            updateCombat(delta);
        } else {
            updateWordPuzzle(delta);
        }
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

        // Handle player input during their turn
        if (Gdx.input.justTouched()) {
            Vector3 touchPos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            viewport.unproject(touchPos);

            if (submitButtonRect.contains(touchPos.x, touchPos.y)) {
                submitWord(); // Use the word puzzle system for attacks
            } else if (clearButtonRect.contains(touchPos.x, touchPos.y)) {
                clearSelection();
            } else {
                // Check for letter grid selection
                checkGridClick(touchPos.x, touchPos.y);
            }
        }
    }

    private void updateWordPuzzle(float delta) {
        // Handle input
        if (Gdx.input.justTouched()) {
            Vector3 touchPos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            viewport.unproject(touchPos);

            // Check button clicks
            if (submitButtonRect.contains(touchPos.x, touchPos.y)) {
                submitWord();
            } else if (clearButtonRect.contains(touchPos.x, touchPos.y)) {
                clearSelection();
            }else {
                // Check grid clicks
                checkGridClick(touchPos.x, touchPos.y);
            }
        }
    }

    private void checkGridClick(float x, float y) {
        if (isCombatMode) {
            // Use compact grid dimensions for combat mode
            float gridSize = 350;
            float gridX = (viewport.getWorldWidth() - gridSize) / 2;
            float gridY = 150;
            float cellSize = gridSize / 5;

            // Check if click is within grid bounds
            if (x >= gridX && x < gridX + gridSize && y >= gridY && y < gridY + gridSize) {
                // Convert to grid coordinates
                int cellX = (int)((x - gridX) / cellSize);
                int cellY = 4 - (int)((y - gridY) / cellSize);

                // Try to select this cell
                selectCell(cellX, cellY);
            }
        } else {
            // Original grid dimensions for word puzzle mode
            float gridSize = 500;
            float gridX = 250;
            float gridY = 110;
            float cellSize = gridSize / 5;

            // Check if click is within grid bounds
            if (x >= gridX && x < gridX + gridSize && y >= gridY && y < gridY + gridSize) {
                // Convert to grid coordinates
                int cellX = (int)((x - gridX) / cellSize);
                int cellY = 4 - (int)((y - gridY) / cellSize);

                // Try to select this cell
                selectCell(cellX, cellY);
            }
        }
    }

    public void render(SpriteBatch batch) {
        if (!active) {
            return;
        }

        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);

        // Draw background
        batch.setColor(0.1f, 0.1f, 0.2f, 1);
        batch.draw(whiteTexture, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
        batch.setColor(Color.WHITE);

        if (isCombatMode) {
            renderCombatUI(batch);
        } else {
            renderWordPuzzleUI(batch);
        }
    }

    private void renderWordPuzzleUI(SpriteBatch batch) {
        // Draw title
        layout.setText(titleFont, "WORD PUZZLE");
        titleFont.setColor(Color.WHITE);
        titleFont.draw(batch, "WORD PUZZLE",
                (viewport.getWorldWidth() - layout.width) / 2, 650);

        // Draw letter grid
        drawLetterGrid(batch);

        // Draw current word
        String currentWord = letterGrid.getCurrentWord();
        layout.setText(bigFont, currentWord);
        bigFont.setColor(Color.WHITE);
        bigFont.draw(batch, currentWord, 900, 500);

        // Draw score
        String scoreText = "Score: " + currentScore + "   High Score: " + highScore;
        regularFont.setColor(Color.WHITE);
        regularFont.draw(batch, scoreText, 900, 450);

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

        // Draw player in top-left corner
        drawCombatCharacter(batch, gameController.getCharacter().getName(), playerHealth, 100, 50, 600, true);

        // Draw enemy in top-right corner
        drawCombatCharacter(batch, enemyName, enemyHealth, enemyMaxHealth,
                viewport.getWorldWidth() - 300, 600, false);

        // Draw battle message log in bottom-left
        drawDialogBox(batch, 50, 50, 300, 150);
        regularFont.setColor(Color.WHITE);
        regularFont.draw(batch, combatLog, 80, 170);
        regularFont.draw(batch,  (isPlayerTurn ? "Player" : enemyName) + " TURN",
                (viewport.getWorldWidth()) / 2 - 90,
                700);
        // Draw action buttons in bottom-right
        float buttonX = viewport.getWorldWidth() - 250;
        float buttonY = 50;
        float buttonWidth = 200;
        float buttonHeight = 50;
        float buttonSpacing = 60;

        // Update button rectangles for new positions
        submitButtonRect = new Rectangle(buttonX, buttonY + buttonSpacing*2, buttonWidth, buttonHeight);
        clearButtonRect = new Rectangle(buttonX, buttonY + buttonSpacing, buttonWidth, buttonHeight);
        exitButtonRect = new Rectangle(buttonX, buttonY, buttonWidth, buttonHeight);

        // Draw action buttons in Pokémon style
        drawPokemonButton(batch, submitButtonRect, "CAST WORD", isPlayerTurn);
        drawPokemonButton(batch, clearButtonRect, "CLEAR", isPlayerTurn);
//        drawPokemonButton(batch, exitButtonRect, "RUN", true);

        // Draw compact letter grid in center only during player turn
        if (isPlayerTurn) {
            drawCompactLetterGrid(batch);

            // Show current word in progress above the grid
            String currentWord = letterGrid.getCurrentWord();
            if (currentWord.length() > 0) {
                String wordText = "Spell: " + currentWord;
                layout.setText(regularFont, wordText);
                regularFont.draw(batch, wordText,
                        (viewport.getWorldWidth() - layout.width) / 2,
                        600);

                // Show potential damage
                int potentialScore = WordScorer.getTotalScore(currentWord);
                if(wordValidator.isValidWord(currentWord)){
                    String damageText = "Power: " + (potentialScore * wordDamageMultiplier);
                    layout.setText(regularFont, damageText);
                    regularFont.draw(batch, damageText,
                            (viewport.getWorldWidth() - layout.width) / 2,
                            570);
                }

            }
        }
    }

    private void drawCombatCharacter(SpriteBatch batch, String name, int currentHealth,
                                     int maxHealth, float x, float y, boolean isPlayer) {
        // Draw character image based on name
        batch.setColor(1, 1, 1, 1);
        Texture characterTexture = null;
        if (isPlayer) {
            characterTexture = getCharacterTexture("characters/player.png");
        }
        else{
            characterTexture = getCharacterTexture("enemy/" + name + ".png");
        }
        if (characterTexture != null) {
            float imgSize = 150;
            batch.draw(characterTexture, x + 60, y - imgSize - 100, imgSize, imgSize);
        }

        // Draw character name and health bar
        drawPokemonHealthBar(batch, name, currentHealth, maxHealth, x, y);
    }

    private void drawCompactLetterGrid(SpriteBatch batch) {
        float gridSize = 350; // Smaller than the original 500
        float cellSize = gridSize / 5;
        float gridX = (viewport.getWorldWidth() - gridSize) / 2; // Center horizontally
        float gridY = 150; // Position vertically

        char[][] grid = letterGrid.getGrid();
        boolean[][] selected = letterGrid.getSelectedCells();

        // Draw cells
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                float screenX = gridX + x * cellSize;
                float screenY = gridY + (4-y) * cellSize;

                // Draw cell background
                if (selected[y][x]) {
                    batch.setColor(0.2f, 0.6f, 1f, 1); // Selected cell color
                } else {
                    batch.setColor(0.9f, 0.9f, 0.8f, 1); // Regular cell color
                }

                batch.draw(whiteTexture, screenX, screenY, cellSize, cellSize);

                // Draw cell border
                batch.setColor(0.3f, 0.3f, 0.3f, 1);
                batch.draw(whiteTexture, screenX, screenY, cellSize, 1); // Bottom
                batch.draw(whiteTexture, screenX, screenY, 1, cellSize); // Left
                batch.draw(whiteTexture, screenX + cellSize - 1, screenY, 1, cellSize); // Right
                batch.draw(whiteTexture, screenX, screenY + cellSize - 1, cellSize, 1); // Top

                // Draw letter
                String letter = String.valueOf(grid[y][x]);
                layout.setText(regularFont, letter);
                batch.setColor(Color.BLACK);
                regularFont.draw(batch, letter,
                        screenX + (cellSize - layout.width) / 2,
                        screenY + cellSize - (cellSize - layout.height) / 2);
            }
        }

        batch.setColor(Color.WHITE); // Reset color
    }

    private Texture getCharacterTexture(String name) {
        if (!textureCache.containsKey(name)) {
            textureCache.put(name, new Texture(Gdx.files.internal(name)));
        }
        return textureCache.get(name);
    }
    // Helper method to draw Pokémon-style health bars
    private void drawPokemonHealthBar(SpriteBatch batch, String name, int current, int max, float x, float y) {
        // Draw name tag background
        batch.setColor(0.2f, 0.2f, 0.2f, 0.8f);
        batch.draw(whiteTexture, x, y, 250, 50);

        // Draw border
        batch.setColor(0.8f, 0.8f, 0.8f, 1);
        batch.draw(whiteTexture, x, y, 250, 2); // Top
        batch.draw(whiteTexture, x, y, 2, 50); // Left
        batch.draw(whiteTexture, x + 250, y, 2, 50); // Right
        batch.draw(whiteTexture, x, y + 50, 250, 2); // Bottom

        // Draw name
        regularFont.setColor(Color.WHITE);
        regularFont.draw(batch, name, x + 10, y + 40);

        // Draw "HP" label
        regularFont.draw(batch, "HP:", x + 8, y + 20);

        // Draw health bar background
        batch.setColor(0.3f, 0.3f, 0.3f, 1);
        batch.draw(whiteTexture, x + 40, y + 10, 180, 10);

        // Calculate health percentage
        float healthPercentage = (float)current / max;

        // Determine color based on health percentage
        if (healthPercentage > 0.5f) {
            batch.setColor(0.3f, 0.9f, 0.3f, 1); // Green
        } else if (healthPercentage > 0.2f) {
            batch.setColor(0.9f, 0.9f, 0.2f, 1); // Yellow
        } else {
            batch.setColor(0.9f, 0.2f, 0.2f, 1); // Red
        }

        // Draw health bar
        batch.draw(whiteTexture, x + 40, y + 10, 180 * healthPercentage, 10);
    }

    // Helper method to draw Pokémon-style dialog box
    private void drawDialogBox(SpriteBatch batch, float x, float y, float width, float height) {
        // Draw main dialog background
        batch.setColor(0.1f, 0.1f, 0.1f, 0.85f);
        batch.draw(whiteTexture, x, y, width, height);

        // Draw border
        batch.setColor(0.7f, 0.7f, 0.7f, 1);
        batch.draw(whiteTexture, x, y, width, 2); // Top
        batch.draw(whiteTexture, x, y, 2, height); // Left
        batch.draw(whiteTexture, x + width, y, 2, height); // Right
        batch.draw(whiteTexture, x, y + height, width, 2); // Bottom

        // Inner shadow for depth
        batch.setColor(0.3f, 0.3f, 0.3f, 0.5f);
        batch.draw(whiteTexture, x + 4, y + 4, width - 8, height - 8);
    }

    // Helper method to draw Pokémon-style buttons
    private void drawPokemonButton(SpriteBatch batch, Rectangle buttonRect, String text, boolean enabled) {
        // Background
        if (enabled) {
            batch.setColor(0.4f, 0.4f, 0.8f, 0.9f);
        } else {
            batch.setColor(0.3f, 0.3f, 0.4f, 0.6f); // Disabled appearance
        }
        batch.draw(whiteTexture, buttonRect.x, buttonRect.y, buttonRect.width, buttonRect.height);

        // Border
        batch.setColor(0.7f, 0.7f, 0.9f, 1);
        batch.draw(whiteTexture, buttonRect.x, buttonRect.y, buttonRect.width, 2); // Bottom
        batch.draw(whiteTexture, buttonRect.x, buttonRect.y, 2, buttonRect.height); // Left
        batch.draw(whiteTexture, buttonRect.x + buttonRect.width, buttonRect.y, 2, buttonRect.height); // Right
        batch.draw(whiteTexture, buttonRect.x, buttonRect.y + buttonRect.height, buttonRect.width, 2); // Top

        // Text
        layout.setText(regularFont, text);
        regularFont.setColor(enabled ? Color.WHITE : Color.GRAY);
        regularFont.draw(batch, text,
                buttonRect.x + (buttonRect.width - layout.width) / 2,
                buttonRect.y + (buttonRect.height + layout.height) / 2);
    }

    // Method to draw the health bar
    private void drawHealthBar(SpriteBatch batch, String label, int current, int max,
                               float x, float y, float width, float height, Color color) {
        // Draw label
        regularFont.setColor(Color.WHITE);
        regularFont.draw(batch, label + ": " + current + "/" + max, x, y);

        // Draw background
        batch.setColor(0.2f, 0.2f, 0.2f, 1);
        batch.draw(whiteTexture, x, y - height - 5, width, height);

        // Draw health bar
        float healthRatio = (float)current / max;
        batch.setColor(color);
        batch.draw(whiteTexture, x, y - height - 5, width * healthRatio, height);

        // Draw border
        batch.setColor(0.7f, 0.7f, 0.7f, 1);
        batch.draw(whiteTexture, x, y - height - 5, width, 1); // Top
        batch.draw(whiteTexture, x, y - 5, 1, height); // Left
        batch.draw(whiteTexture, x + width, y - height - 5, 1, height); // Right
        batch.draw(whiteTexture, x, y - 5, width, 1); // Bottom

        batch.setColor(Color.WHITE);
    }

    private void drawLetterGrid(SpriteBatch batch) {
        float gridX = 250;
        float gridY = 110;
        float gridSize = 500;
        float cellSize = gridSize / 5;

        char[][] grid = letterGrid.getGrid();
        boolean[][] selected = letterGrid.getSelectedCells();

        // Draw cells
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                float screenX = gridX + x * cellSize;
                float screenY = gridY + (4-y) * cellSize;

                // Draw cell background
                if (selected[y][x]) {
                    batch.setColor(0.2f, 0.6f, 1f, 1); // Selected cell color
                } else {
                    batch.setColor(0.9f, 0.9f, 0.8f, 1); // Regular cell color
                }

                // Draw with the white texture
                batch.draw(whiteTexture, screenX, screenY, cellSize, cellSize);

                // Draw cell border
                batch.setColor(0.3f, 0.3f, 0.3f, 1);
                batch.draw(whiteTexture, screenX, screenY, cellSize, 1); // Bottom
                batch.draw(whiteTexture, screenX, screenY, 1, cellSize); // Left
                batch.draw(whiteTexture, screenX, screenY + cellSize - 1, cellSize, 1); // Top
                batch.draw(whiteTexture, screenX + cellSize - 1, screenY, 1, cellSize); // Right

                // Draw letter
                String letter = String.valueOf(grid[y][x]);
                layout.setText(regularFont, letter);
                batch.setColor(Color.BLACK);
                regularFont.draw(batch, letter,
                        screenX + (cellSize - layout.width) / 2,
                        screenY + cellSize - (cellSize - layout.height) / 2);
            }
        }

        batch.setColor(Color.WHITE); // Reset color
    }

    private void drawButton(SpriteBatch batch, Rectangle buttonRect, String text) {
        batch.setColor(0.4f, 0.4f, 0.8f, 1);
        batch.draw(whiteTexture, buttonRect.x, buttonRect.y, buttonRect.width, buttonRect.height);

        batch.setColor(0.2f, 0.2f, 0.6f, 1);
        batch.draw(whiteTexture, buttonRect.x, buttonRect.y, buttonRect.width, 2); // Bottom
        batch.draw(whiteTexture, buttonRect.x, buttonRect.y, 2, buttonRect.height); // Left

        layout.setText(regularFont, text);
        regularFont.setColor(Color.WHITE);
        regularFont.draw(batch, text,
                buttonRect.x + (buttonRect.width - layout.width) / 2,
                buttonRect.y + (buttonRect.height + layout.height) / 2);

        batch.setColor(Color.WHITE);
    }

    public void activate() {
        System.out.println("Activating GameplayController");
        this.active = true;
        letterGrid.regenerateGrid();
        currentScore = 0;
        currentMessage = "";
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        // Automatically start combat if enabled
        if (autoStartCombat) {
            startCombat("Word Monster", 100);
        }
    }

    public void deactivate() {
        this.active = false;
        clearSelection();
    }

    public void startCombat(String enemyName, int enemyHealth) {
        this.enemyName = enemyName;
        this.enemyMaxHealth = enemyHealth;
        this.enemyHealth = enemyHealth;
        this.playerHealth = gameController.getCharacter().getHealth();
        this.isCombatMode = true;
        this.isPlayerTurn = true;
        this.combatLog = "Combat with " + enemyName + " has begun!";

        // Generate a new grid of letters for the combat
        letterGrid.regenerateGrid();
    }

    private void performEnemyAction() {
        // Enemy deals damage based on word length and multiplier
        int damage = (random.nextInt(8) + 3) * enemyDamageMultiplier;
        playerHealth -= damage;

        // Sometimes the enemy can perform special actions
        int action = random.nextInt(10);

        if (action < 7) { // 70% chance for normal attack
            combatLog = enemyName + " attacks for " + damage + " damage!";
        } else if (action < 9) { // 20% chance for a power attack
            int extraDamage = random.nextInt(5) + 1;
            playerHealth -= extraDamage;
            combatLog = enemyName + " performs a power attack for " + (damage + extraDamage) + " damage!";
        } else { // 10% chance to heal
            int heal = random.nextInt(8) + 3;
            enemyHealth = Math.min(enemyMaxHealth, enemyHealth + heal);
            combatLog = enemyName + " recovers " + heal + " health!";
        }

        checkCombatEnd();

        // If combat continues, it's player's turn again
        if (isCombatMode) {
            isPlayerTurn = true;
            // Regenerate letter grid for the next player turn
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
            if (currentScore > highScore) {
                highScore = currentScore;
            }
            showMessage("Victory! +" + rewardPoints + " points!");
        } else {
            showMessage("Defeat! Better luck next time.");
        }

        // Return to word puzzle mode after a short delay
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

    // Add these convenience methods for starting combat with different difficulties
    public void startEasyCombat() {
        wordDamageMultiplier = 1;
        enemyDamageMultiplier = 1;
        startCombat("Word Goblin", 50);
    }

    public void startMediumCombat() {
        wordDamageMultiplier = 1;
        enemyDamageMultiplier = 2;
        startCombat("Word Troll", 100);
    }

    public void startHardCombat() {
        wordDamageMultiplier = 1;
        enemyDamageMultiplier = 3;
        startCombat("Word Dragon", 200);
    }

    public void startCombatWithDifficulty(int difficulty) {
        switch(difficulty) {
            case 1: startEasyCombat(); break;
            case 2: startMediumCombat(); break;
            case 3: startHardCombat(); break;
            default: startEasyCombat(); break;
        }
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
            if (currentScore > highScore) {
                highScore = currentScore;
            }

            // If in combat, deal damage based on word score
            if (isCombatMode && isPlayerTurn) {
                int damage = points * wordDamageMultiplier;
                enemyHealth -= damage;
                combatLog = "Your word '" + word + "' deals " + damage + " damage!";
                showMessage("+" + points + " points! " + damage + " damage!");

                // Check if enemy is defeated
                checkCombatEnd();
                if (isCombatMode) {
                    isPlayerTurn = false; // End player's turn
                }
            } else {
                showMessage("+" + points + " points!");
            }

            // Always regenerate the grid after a valid word
            letterGrid.regenerateGrid();
            return true;
        } else {
            showMessage("Not a valid word!");
            return false;
        }
    }

    public boolean selectCell(int x, int y) {
        if (!active) return false;
        try {
            if (letterGrid.canSelect(x, y)) {
                letterGrid.selectCell(x, y);
                return true;
            }
            return false;
        } catch (IndexOutOfBoundsException e) {
            System.err.println("Invalid cell coordinates: " + x + ", " + y);
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
        messageTimer = 3.0f; // Show message for 3 seconds
    }

    // Add getters and other utility methods
    public boolean isInCombatMode() {
        return isCombatMode;
    }

    public int getPlayerHealth() {
        return playerHealth;
    }

    public int getEnemyHealth() {
        return enemyHealth;
    }

    public LetterGrid getLetterGrid() {
        return letterGrid;
    }

    public int getCurrentScore() {
        return currentScore;
    }

    public int getHighScore() {
        return highScore;
    }

    public String getCurrentWord() {
        return letterGrid.getCurrentWord();
    }

    public boolean isActive() {
        return active;
    }

    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    public void dispose() {
        active = false;
//        titleFont.dispose();
//        regularFont.dispose();
//        bigFont.dispose();
//        whiteTexture.dispose();

        // Dispose all cached textures
        for (Texture texture : textureCache.values()) {
            texture.dispose();
        }
        textureCache.clear();
    }
}