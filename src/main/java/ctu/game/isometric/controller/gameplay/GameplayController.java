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
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import ctu.game.isometric.controller.GameController;
import ctu.game.isometric.model.word.LetterGrid;
import ctu.game.isometric.util.WordScorer;
import ctu.game.isometric.util.WordValidator;

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

    // UI components
    private String currentMessage = "";
    private float messageTimer = 0;

    // Button rectangles for hit detection
    private Rectangle submitButtonRect;
    private Rectangle clearButtonRect;
    private Rectangle exitButtonRect;

    public GameplayController(GameController gameController) {
        this.gameController = gameController;
        this.letterGrid = new LetterGrid();
        this.wordValidator = new WordValidator();
        this.currentScore = 0;
        this.highScore = 0;
        this.active = false;

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
        exitButtonRect = new Rectangle(900, 210, 200, 50);
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

        // Handle input
        if (Gdx.input.justTouched()) {
            Vector3 touchPos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            viewport.unproject(touchPos);

            // Check button clicks
            if (submitButtonRect.contains(touchPos.x, touchPos.y)) {
                submitWord();
            } else if (clearButtonRect.contains(touchPos.x, touchPos.y)) {
                clearSelection();
            } else if (exitButtonRect.contains(touchPos.x, touchPos.y)) {
                deactivate();
                gameController.returnToPreviousState();
            } else {
                // Check grid clicks
                checkGridClick(touchPos.x, touchPos.y);
            }
        }
    }

    private void checkGridClick(float x, float y) {
        // Calculate grid area
        float gridSize = 500;
        float gridX = 250;
        float gridY = 110;
        float cellSize = gridSize / 5; // Changed from 7 to 5

        // Check if click is within grid bounds
        if (x >= gridX && x < gridX + gridSize && y >= gridY && y < gridY + gridSize) {
            // Convert to grid coordinates
            int cellX = (int)((x - gridX) / cellSize);
            int cellY = 4 - (int)((y - gridY) / cellSize); // Changed from 6 to 4

            // Try to select this cell
            selectCell(cellX, cellY);
        }
    }

    public void render(SpriteBatch batch) {
        if (!active) {
            // Debug if we're getting here but not rendering
            System.out.println("GameplayController not active, skipping render");
            return;
        }

        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);

        // Draw background
        batch.setColor(0.1f, 0.1f, 0.2f, 1);
        batch.draw(whiteTexture, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
        batch.setColor(Color.WHITE);

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
        drawButton(batch, exitButtonRect, "Exit Game");
    }

    private void drawLetterGrid(SpriteBatch batch) {
        float gridX = 250;
        float gridY = 110;
        float gridSize = 500;
        float cellSize = gridSize / 5; // Changed from 7 to 5

        char[][] grid = letterGrid.getGrid();
        boolean[][] selected = letterGrid.getSelectedCells();

        // Draw cells
        for (int y = 0; y < 5; y++) { // Changed from 7 to 5
            for (int x = 0; x < 5; x++) { // Changed from 7 to 5
                float screenX = gridX + x * cellSize;
                float screenY = gridY + (4-y) * cellSize; // Changed from 6-y to 4-y

                // Draw cell background
                if (selected[y][x]) {
                    batch.setColor(0.2f, 0.6f, 1f, 1); // Selected cell color
                } else {
                    batch.setColor(0.9f, 0.9f, 0.8f, 1); // Regular cell color
                }

                // Draw with the white texture instead of font region
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
    }

    public void deactivate() {
        this.active = false;
        clearSelection();
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
            showMessage("+" + points + " points!");
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
        titleFont.dispose();
        regularFont.dispose();
        bigFont.dispose();
        whiteTexture.dispose();
    }
}