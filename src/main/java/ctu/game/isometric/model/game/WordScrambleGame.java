package ctu.game.isometric.model.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.MathUtils;
import ctu.game.isometric.controller.GameController;

import java.util.*;

import static ctu.game.isometric.util.FontGenerator.generateVietNameseFont;

public class WordScrambleGame {
    private GameController gameController;
    private String originalWord;
    private String scrambledWord;
    private boolean isActive = false;
    private int attemptsLeft = 3;
    private boolean isSuccessful = false;
    private List<String> wordList;
    private String currentGuess = "";
    private GlyphLayout layout = new GlyphLayout();

    // UI elements
    private Rectangle submitButton;
    private Rectangle giveUpButton;
    private Rectangle gamePanel;
    private Rectangle inputField;
    private float uiX, uiY;
    private float width = 480;
    private float height = 320;
    private boolean showingResult = false;
    private String resultMessage = "";
    private float resultTimer = 0;
    private static final float RESULT_DURATION = 3.0f;

    // Enhanced UI
    private BitmapFont font;
    private BitmapFont titleFont;
    private BitmapFont subtitleFont;
    private BitmapFont buttonFont;
    private ShapeRenderer shapeRenderer;
    private Matrix4 uiMatrix;
    private Matrix4 originalProjectionMatrix;

    // Animation and visual effects
    private float animationTimer = 0;
    private float pulseTimer = 0;
    private float sparkleTimer = 0;
    private boolean showCursor = true;
    private float blinkTimer = 0;
    private float buttonHoverTimer = 0;
    private boolean submitHovered = false;
    private boolean giveUpHovered = false;

    // Color scheme
    private static final Color BACKGROUND_COLOR = new Color(0.08f, 0.08f, 0.15f, 0.95f);
    private static final Color PANEL_BORDER = new Color(0.4f, 0.6f, 1.0f, 1.0f);
    private static final Color ACCENT_COLOR = new Color(1.0f, 0.8f, 0.2f, 1.0f);
    private static final Color SUCCESS_COLOR = new Color(0.2f, 0.8f, 0.3f, 1.0f);
    private static final Color ERROR_COLOR = new Color(0.9f, 0.3f, 0.3f, 1.0f);
    private static final Color INPUT_BG = new Color(0.15f, 0.15f, 0.25f, 0.9f);
    private static final Color BUTTON_SUBMIT = new Color(0.2f, 0.7f, 0.4f, 0.9f);
    private static final Color BUTTON_CANCEL = new Color(0.7f, 0.3f, 0.3f, 0.9f);

    // Input validation
    private static final int MAX_GUESS_LENGTH = 20;

    // Words to use if player hasn't learned many yet
    private final String[] DEFAULT_WORDS = {"HELLO", "WORLD", "GAME", "DICE", "BOARD", "PATH", "MAGIC", "QUEST", "SWORD", "SHIELD"};

    public WordScrambleGame(GameController gameController) {
        this.gameController = gameController;
        this.wordList = new ArrayList<>();
        this.font = generateVietNameseFont("GrenzeGotisch.ttf", 22);
        this.titleFont = generateVietNameseFont("GrenzeGotisch.ttf", 27);
        this.subtitleFont = generateVietNameseFont("GrenzeGotisch.ttf", 18);
        this.buttonFont = generateVietNameseFont("GrenzeGotisch.ttf", 18);
        this.shapeRenderer = new ShapeRenderer();

        // Configure fonts with better styling
        this.font.setColor(Color.WHITE);

        this.titleFont.setColor(ACCENT_COLOR);

        this.subtitleFont.setColor(new Color(0.8f, 0.8f, 0.9f, 1.0f));

        this.buttonFont.getData().setScale(1.1f);
        this.buttonFont.setColor(Color.WHITE);
    }

    public void startGame() {
        isActive = true;
        attemptsLeft = 3;
        isSuccessful = false;
        currentGuess = "";
        showingResult = false;
        resultTimer = 0;
        animationTimer = 0;

        // Position the UI in the center of the screen
        uiX = Gdx.graphics.getWidth() / 2 - width / 2;
        uiY = Gdx.graphics.getHeight() / 2 + height / 2;

        // Create UI elements with better proportions
        gamePanel = new Rectangle(uiX, uiY - height, width, height);
        inputField = new Rectangle(uiX + 40, uiY - height + 80, width - 80, 40);
        submitButton = new Rectangle(uiX + 60, uiY - height + 25, 120, 40);
        giveUpButton = new Rectangle(uiX + width - 180, uiY - height + 25, 120, 40);

        // Select a word - either from learned words or defaults
        Set<String> learnedWords = gameController.getCharacter().getLearnedWords();
        if (learnedWords.size() > 5) {
            wordList = new ArrayList<>(learnedWords);
        } else {
            wordList = Arrays.asList(DEFAULT_WORDS);
        }

        // Pick random word and scramble it
        originalWord = wordList.get(new Random().nextInt(wordList.size()));
        scrambledWord = scrambleWord(originalWord);
    }

    private String scrambleWord(String word) {
        List<Character> characters = new ArrayList<>();
        for (char c : word.toCharArray()) {
            characters.add(c);
        }

        String scrambled;
        int attempts = 0;
        do {
            Collections.shuffle(characters);
            StringBuilder builder = new StringBuilder();
            for (char c : characters) {
                builder.append(c);
            }
            scrambled = builder.toString();
            attempts++;
        } while (scrambled.equals(word) && attempts < 10);

        return scrambled;
    }

    public boolean checkGuess(String guess) {
        if (!isActive || guess.trim().isEmpty()) return false;

        String cleanGuess = guess.trim().toUpperCase();
        if (cleanGuess.equals(originalWord.toUpperCase())) {
            isSuccessful = true;
            isActive = false;
            showResult("EXCELLENT! You earned 50 points!", SUCCESS_COLOR);
            giveReward();
            return true;
        } else {
            attemptsLeft--;
            if (attemptsLeft <= 0) {
                isActive = false;
                showResult("Game Over! The word was: " + originalWord, ERROR_COLOR);
            } else {
                showResult("Try again! " + attemptsLeft + " attempts left", new Color(1.0f, 0.6f, 0.2f, 1.0f));
            }
            return false;
        }
    }

    private void showResult(String message, Color color) {
        showingResult = true;
        resultMessage = message;
        resultTimer = 0;
        font.setColor(color);
    }

    public void giveReward() {
        if (isSuccessful && gameController.getCharacter() != null) {
            gameController.getCharacter().addScore(50);
        }
    }

    public void update(float delta) {
        if (!isActive && !showingResult) return;

        // Update timers for animations
        animationTimer += delta;
        blinkTimer += delta;
        pulseTimer += delta;
        sparkleTimer += delta;
        buttonHoverTimer += delta;

        // Update cursor blink
        if (blinkTimer >= 0.5f) {
            showCursor = !showCursor;
            blinkTimer = 0;
        }

        // Update result timer
        if (showingResult) {
            resultTimer += delta;
            if (resultTimer >= RESULT_DURATION) {
                showingResult = false;
                if (!isActive) {
                    // Game ended
                }
            }
        }

        // Handle mouse input and hover detection
        handleMouseInput();
    }

    private void handleMouseInput() {
        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();

        // Check button hover states
        submitHovered = submitButton.contains(mouseX, mouseY);
        giveUpHovered = giveUpButton.contains(mouseX, mouseY);

        if (Gdx.input.justTouched()) {
            if (submitHovered && !currentGuess.trim().isEmpty()) {
                checkGuess(currentGuess);
                currentGuess = "";
            }

            if (giveUpHovered) {
                giveUp();
            }
        }
    }

    private void giveUp() {
        showResult("😔 Better luck next time! The word was: " + originalWord, new Color(0.6f, 0.6f, 0.6f, 1.0f));
        isActive = false;
    }

    private void drawRoundedRect(ShapeRenderer renderer, float x, float y, float width, float height, float radius) {
        // Draw rounded rectangle using multiple shapes
        renderer.rect(x + radius, y, width - 2 * radius, height);
        renderer.rect(x, y + radius, width, height - 2 * radius);
        renderer.circle(x + radius, y + radius, radius);
        renderer.circle(x + width - radius, y + radius, radius);
        renderer.circle(x + radius, y + height - radius, radius);
        renderer.circle(x + width - radius, y + height - radius, radius);
    }

    private void drawGradientBackground(ShapeRenderer renderer, Rectangle rect) {
        // Simulate gradient with multiple rectangles
        float steps = 20;
        for (int i = 0; i < steps; i++) {
            float alpha = BACKGROUND_COLOR.a * (1.0f - (i / steps) * 0.3f);
            renderer.setColor(BACKGROUND_COLOR.r, BACKGROUND_COLOR.g, BACKGROUND_COLOR.b, alpha);
            renderer.rect(rect.x, rect.y + (rect.height / steps) * i, rect.width, rect.height / steps);
        }
    }

    public void render(SpriteBatch batch, float x, float y) {
        if (!isActive && !showingResult) return;

        // Store original state
        Color originalColor = font.getColor().cpy();
        originalProjectionMatrix = batch.getProjectionMatrix().cpy();

        // Switch to UI projection
        batch.end();
        uiMatrix = new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.setProjectionMatrix(uiMatrix);

        // Draw shapes
        shapeRenderer.setProjectionMatrix(uiMatrix);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Draw gradient background
        drawGradientBackground(shapeRenderer, gamePanel);

        // Draw input field with glow effect
        if (isActive) {
            // Glow effect
            float glowIntensity = 0.3f + 0.2f * MathUtils.sin(pulseTimer * 3);
            shapeRenderer.setColor(PANEL_BORDER.r, PANEL_BORDER.g, PANEL_BORDER.b, glowIntensity * 0.5f);
            shapeRenderer.rect(inputField.x - 4, inputField.y +26, inputField.width + 8, inputField.height + 8);
        }

        // Input field background
        shapeRenderer.setColor(INPUT_BG);
        shapeRenderer.rect(inputField.x, inputField.y+30, inputField.width, inputField.height);

        // Draw buttons with hover effects
        Color submitColor = submitHovered ?
                new Color(BUTTON_SUBMIT.r * 1.2f, BUTTON_SUBMIT.g * 1.2f, BUTTON_SUBMIT.b * 1.2f, BUTTON_SUBMIT.a) :
                BUTTON_SUBMIT;
        Color giveUpColor = giveUpHovered ?
                new Color(BUTTON_CANCEL.r * 1.2f, BUTTON_CANCEL.g * 1.2f, BUTTON_CANCEL.b * 1.2f, BUTTON_CANCEL.a) :
                BUTTON_CANCEL;

        shapeRenderer.setColor(submitColor);
        shapeRenderer.rect(submitButton.x, submitButton.y, submitButton.width, submitButton.height);

        shapeRenderer.setColor(giveUpColor);
        shapeRenderer.rect(giveUpButton.x, giveUpButton.y, giveUpButton.width, giveUpButton.height);

        shapeRenderer.end();

        // Draw borders and outlines
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

        // Main panel border with animation
        float borderIntensity = 0.8f + 0.2f * MathUtils.sin(animationTimer * 2);
        shapeRenderer.setColor(PANEL_BORDER.r, PANEL_BORDER.g, PANEL_BORDER.b, borderIntensity);
        Gdx.gl.glLineWidth(3);
        shapeRenderer.rect(gamePanel.x, gamePanel.y, gamePanel.width, gamePanel.height);

        // Input field border
        shapeRenderer.setColor(PANEL_BORDER);
        Gdx.gl.glLineWidth(2);
        shapeRenderer.rect(inputField.x, inputField.y + 30, inputField.width, inputField.height);

        // Button borders
        shapeRenderer.setColor(Color.WHITE);
        Gdx.gl.glLineWidth(1);
        shapeRenderer.rect(submitButton.x, submitButton.y, submitButton.width, submitButton.height);
        shapeRenderer.rect(giveUpButton.x, giveUpButton.y, giveUpButton.width, giveUpButton.height);

        Gdx.gl.glLineWidth(1); // Reset line width
        shapeRenderer.end();

        // Draw decorative elements
        if (isSuccessful && showingResult) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            // Draw sparkles
            for (int i = 0; i < 8; i++) {
                float sparkleX = gamePanel.x + gamePanel.width * 0.2f + (i % 4) * gamePanel.width * 0.2f;
                float sparkleY = gamePanel.y + gamePanel.height * 0.3f + (i / 4) * gamePanel.height * 0.4f;
                float sparkleSize = 3 + 2 * MathUtils.sin(sparkleTimer * 4 + i);
                shapeRenderer.setColor(ACCENT_COLOR.r, ACCENT_COLOR.g, ACCENT_COLOR.b,
                        0.8f * MathUtils.sin(sparkleTimer * 3 + i * 0.5f));
                shapeRenderer.circle(sparkleX, sparkleY, sparkleSize);
            }
            shapeRenderer.end();
        }

        batch.begin();

        float panelX = gamePanel.x + 20;
        float panelY = gamePanel.y;

        // Draw title with shadow effect
        titleFont.setColor(0.2f, 0.2f, 0.4f, 0.8f); // Shadow
        titleFont.draw(batch, "🎯 WORD SCRAMBLE", panelX + 2, panelY + height - 22);
        titleFont.setColor(ACCENT_COLOR); // Main text
        titleFont.draw(batch, "🎯 WORD SCRAMBLE", panelX, panelY + height - 20);

        // Draw subtitle
        subtitleFont.setColor(new Color(0.8f, 0.8f, 0.9f, 1.0f));
        subtitleFont.draw(batch, "Unscramble the letters to form a word", panelX, panelY + height - 50);

        // Draw scrambled word with special styling
        font.setColor(new Color(0.6f, 0.8f, 1.0f, 1.0f));
        font.draw(batch, "Letters:", panelX, panelY + height - 80);

        // Draw each letter of scrambled word separately with animation
        float letterSpacing = 25;
        float startX = panelX + 80;
        titleFont.setColor(Color.WHITE);
        for (int i = 0; i < scrambledWord.length(); i++) {
            float yOffset = 3 * MathUtils.sin(animationTimer * 2 + i * 0.5f);
            titleFont.draw(batch, String.valueOf(scrambledWord.charAt(i)),
                    startX + i * letterSpacing, panelY + height - 75 + yOffset);
        }

        // Draw attempt counter with visual indicators
        float heartX = panelX;
        font.setColor(Color.WHITE);
        font.draw(batch, "Lives: " + attemptsLeft, heartX, panelY + height - 110);

        // Draw input field content
        subtitleFont.setColor(new Color(0.7f, 0.7f, 0.8f, 1.0f));
        subtitleFont.draw(batch, "Your Answer:", panelX, inputField.y + inputField.height + 53);

        // Draw input text with cursor
        font.setColor(Color.WHITE);
        String displayGuess = currentGuess + (showCursor && isActive ? "|" : "");
        font.draw(batch, displayGuess, inputField.x + 10, inputField.y + inputField.height / 2 + 35);

        // Draw input hint
        subtitleFont.setColor(new Color(0.5f, 0.5f, 0.6f, 1.0f));
        subtitleFont.draw(batch, "Type your answer and press ENTER or click SUBMIT", panelX + 20, panelY + 97);

        // Draw buttons with better text
        buttonFont.setColor(Color.WHITE);
        layout.setText(buttonFont, "SUBMIT");
        buttonFont.draw(batch, "SUBMIT",
                submitButton.x + (submitButton.width - layout.width) / 2,
                submitButton.y + (submitButton.height + layout.height) / 2);

        layout.setText(buttonFont, "GIVE UP");
        buttonFont.draw(batch, "GIVE UP",
                giveUpButton.x + (giveUpButton.width - layout.width) / 2,
                giveUpButton.y + (giveUpButton.height + layout.height) / 2);

        // Draw result message with enhanced styling
        if (showingResult) {
            // Animated overlay
            batch.end();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            float overlayAlpha = 0.7f * MathUtils.sin(resultTimer * 3);
            shapeRenderer.setColor(0, 0, 0, overlayAlpha);
            shapeRenderer.rect(gamePanel.x, gamePanel.y, gamePanel.width, gamePanel.height);
            shapeRenderer.end();
            batch.begin();

            // Result message with pulsing effect
            layout.setText(titleFont, resultMessage);

            // Draw message shadow
            titleFont.setColor(0, 0, 0, 0.8f);
            titleFont.draw(batch, resultMessage,
                    gamePanel.x + (gamePanel.width - layout.width) / 2 + 2,
                    gamePanel.y + gamePanel.height / 2 - 2);

            // Draw main message
            if (isSuccessful) {
                titleFont.setColor(SUCCESS_COLOR);
            } else {
                titleFont.setColor(font.getColor());
            }
            titleFont.draw(batch, resultMessage,
                    gamePanel.x + (gamePanel.width - layout.width) / 2,
                    gamePanel.y + gamePanel.height / 2);

            // Reset font scale
        }

        // Restore original state
        font.setColor(originalColor);
        batch.end();
        batch.setProjectionMatrix(originalProjectionMatrix);
        batch.begin();
    }

    public boolean handleInput(int keycode) {
        if (!isActive) return false;

        switch (keycode) {
            case Input.Keys.ENTER:
                if (!currentGuess.trim().isEmpty()) {
                    checkGuess(currentGuess);
                    currentGuess = "";
                }
                break;

            case Input.Keys.BACKSPACE:
                if (currentGuess.length() > 0) {
                    currentGuess = currentGuess.substring(0, currentGuess.length() - 1);
                }
                break;

            case Input.Keys.ESCAPE:
                giveUp();
                break;

            case Input.Keys.SPACE:
                if (currentGuess.length() < MAX_GUESS_LENGTH) {
                    currentGuess += " ";
                }
                break;
        }
        return false;
    }

    public boolean handleTyped(char c) {
        if (!isActive) return false;

        if (currentGuess.length() < MAX_GUESS_LENGTH) {
            currentGuess += Character.toUpperCase(c);
        }
        return true;
    }

    // Getters
    public String getScrambledWord() {
        return scrambledWord;
    }

    public String getOriginalWord() {
        return originalWord;
    }

    public boolean isActive() {
        return isActive;
    }

    public int getAttemptsLeft() {
        return attemptsLeft;
    }

    public boolean isSuccessful() {
        return isSuccessful;
    }

    public String getCurrentGuess() {
        return currentGuess;
    }

    // Cleanup
    public void dispose() {
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
        if (font != null) {
            font.dispose();
        }
        if (titleFont != null) {
            titleFont.dispose();
        }
        if (subtitleFont != null) {
            subtitleFont.dispose();
        }
        if (buttonFont != null) {
            buttonFont.dispose();
        }
    }
}