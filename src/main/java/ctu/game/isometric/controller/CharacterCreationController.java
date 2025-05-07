package ctu.game.isometric.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import ctu.game.isometric.model.entity.Character;
import ctu.game.isometric.model.entity.Gender;
import ctu.game.isometric.model.game.GameState;

public class CharacterCreationController {
    private GameController gameController;
    private boolean initialized = false;

    private BitmapFont font;
    private ShapeRenderer shapeRenderer;
    private GlyphLayout layout;

    private String playerName = "";
    private String playerGender = "MALE"; // Default
    private static final int MAX_NAME_LENGTH = 20;

    // Static textures
    private Texture maleAvatar;
    private Texture femaleAvatar;
    private Texture currentAvatar;

    private Rectangle nameInputBox;
    private Rectangle maleButton;
    private Rectangle femaleButton;
    private Rectangle confirmButton;

    private boolean isNameFieldActive = false;
    private float cursorBlinkTime = 0;
    private boolean showCursor = false;

    public CharacterCreationController(GameController gameController) {
        this.gameController = gameController;
        initResources();
    }

    private void initResources() {
        // Initialize resources needed for rendering
        if (!initialized) {
            font = new BitmapFont();
            font.getData().setScale(1.5f);

            shapeRenderer = new ShapeRenderer();
            layout = new GlyphLayout();

            // Load static avatars
            try {
                maleAvatar = new Texture(Gdx.files.internal("characters/male_avatar.png"));
                femaleAvatar = new Texture(Gdx.files.internal("characters/female_avatar.png"));
                currentAvatar = maleAvatar;
            } catch (Exception e) {
                Gdx.app.error("CharacterCreation", "Failed to load avatars", e);
            }

            initialized = true;
        }

        // Define UI element positions - always update these in case of resize
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

        nameInputBox = new Rectangle(screenWidth / 2 - 150, screenHeight / 2 + 50, 300, 40);
        maleButton = new Rectangle(screenWidth / 2 - 160, screenHeight / 2 - 20, 150, 40);
        femaleButton = new Rectangle(screenWidth / 2 + 10, screenHeight / 2 - 20, 150, 40);
        confirmButton = new Rectangle(screenWidth / 2 - 100, screenHeight / 2 - 100, 200, 40);
    }

    public void reset() {
        // Reset text fields and selection
        playerName = "";
        playerGender = "MALE";
        currentAvatar = maleAvatar;
        isNameFieldActive = false;
        cursorBlinkTime = 0;
        showCursor = false;

        // No need to recreate resources if already initialized
        if (!initialized) {
            initResources();
        }
    }

    public void update(float delta) {
        // Handle cursor blinking
        cursorBlinkTime += delta;
        if (cursorBlinkTime > 0.5f) {
            cursorBlinkTime = 0;
            showCursor = !showCursor;
        }

        // Handle input
        if (Gdx.input.justTouched()) {
            int x = Gdx.input.getX();
            int y = Gdx.graphics.getHeight() - Gdx.input.getY(); // Invert Y

            isNameFieldActive = nameInputBox.contains(x, y);

            if (maleButton.contains(x, y)) {
                playerGender = "MALE";
                currentAvatar = maleAvatar;
            }

            if (femaleButton.contains(x, y)) {
                playerGender = "FEMALE";
                currentAvatar = femaleAvatar;
            }

            if (confirmButton.contains(x, y) && playerName.length() > 0) {
                confirmCharacter();
            }
        }

        // Handle text input
//        handleTextInput();
    }

    public boolean handleTextInput(int keycode) {
        if (!isNameFieldActive) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                gameController.setState(GameState.MAIN_MENU);
            }
            return true;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            isNameFieldActive = false;
            return true;
        }

        // Handle backspace
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE) && playerName.length() > 0) {
            playerName = playerName.substring(0, playerName.length() - 1);
            return true;
        }

        // Handle enter to deactivate field
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            isNameFieldActive = false;
            return true;
        }

        // Handle typed characters
        for (int key = Input.Keys.A; key <= Input.Keys.Z; key++) {
            if (Gdx.input.isKeyJustPressed(key) && playerName.length() < MAX_NAME_LENGTH) {
                char c = (char) (key - Input.Keys.A + 'A');
                playerName += c;
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && playerName.length() < MAX_NAME_LENGTH) {
            playerName += ' ';
        }
        return false;
    }

    public void render(SpriteBatch batch) {
        // Save original projection matrix and batch state
        Matrix4 originalMatrix = batch.getProjectionMatrix().cpy();
        boolean batchWasDrawing = batch.isDrawing();

        if (batchWasDrawing) {
            batch.end();
        }

        // Configure and use ShapeRenderer
        shapeRenderer.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, 1280, 720));
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Draw name input box
        shapeRenderer.setColor(isNameFieldActive ? new Color(0.9f, 0.9f, 1f, 1) : new Color(0.8f, 0.8f, 0.8f, 1));
        shapeRenderer.rect(nameInputBox.x, nameInputBox.y, nameInputBox.width, nameInputBox.height);

        // Draw gender buttons
        shapeRenderer.setColor(playerGender.equals("MALE") ? new Color(0.7f, 0.9f, 1f, 1) : new Color(0.8f, 0.8f, 0.8f, 1));
        shapeRenderer.rect(maleButton.x, maleButton.y, maleButton.width, maleButton.height);

        shapeRenderer.setColor(playerGender.equals("FEMALE") ? new Color(1f, 0.7f, 0.9f, 1) : new Color(0.8f, 0.8f, 0.8f, 1));
        shapeRenderer.rect(femaleButton.x, femaleButton.y, femaleButton.width, femaleButton.height);

        // Draw confirm button
        shapeRenderer.setColor(playerName.length() > 0 ? new Color(0.7f, 1f, 0.7f, 1) : new Color(0.6f, 0.6f, 0.6f, 1));
        shapeRenderer.rect(confirmButton.x, confirmButton.y, confirmButton.width, confirmButton.height);

        shapeRenderer.end();

        // Start batch with original matrix
        batch.begin();
        batch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, 1280, 720));

        // Draw text elements
        float titleY = Gdx.graphics.getHeight() - 100;
        layout.setText(font, "Character Creation");
        font.setColor(Color.WHITE);
        font.draw(batch, "Character Creation", Gdx.graphics.getWidth() / 2 - layout.width / 2, titleY);

        font.draw(batch, "Name:", nameInputBox.x, nameInputBox.y + nameInputBox.height + 20);

        // Draw input text with cursor
        font.setColor(Color.BLACK);
        String displayText = playerName;
        if (isNameFieldActive && showCursor) {
            displayText += "|";
        }
        font.draw(batch, displayText, nameInputBox.x + 10, nameInputBox.y + 28);

        // Draw gender options
        font.setColor(Color.BLACK);
        layout.setText(font, "Male");
        font.draw(batch, "Male", maleButton.x + maleButton.width / 2 - layout.width / 2, maleButton.y + 28);

        layout.setText(font, "Female");
        font.draw(batch, "Female", femaleButton.x + femaleButton.width / 2 - layout.width / 2, femaleButton.y + 28);

        // Draw confirm button
        layout.setText(font, "Confirm");
        font.draw(batch, "Confirm", confirmButton.x + confirmButton.width / 2 - layout.width / 2, confirmButton.y + 28);

        // Draw character avatar
        if (currentAvatar != null) {
            float x = Gdx.graphics.getWidth() / 2f + 200;
            float y = Gdx.graphics.getHeight() / 2f;
            batch.draw(currentAvatar, x, y, 96, 96);
        }

        // If batch wasn't drawing originally, end it
        if (!batchWasDrawing) {
            batch.end();
        }
    }

    private void confirmCharacter() {
        // Update character with name and gender
        Character character = gameController.getCharacter();
        character.setName(playerName);
        character.setGender(Gender.valueOf(playerGender));
        // Signal that character has been created
        gameController.setCreated(true);
    }

    public void dispose() {
        if (font != null) font.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (maleAvatar != null) maleAvatar.dispose();
        if (femaleAvatar != null) femaleAvatar.dispose();
        initialized = false;
    }
}