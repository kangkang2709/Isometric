package ctu.game.isometric.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import ctu.game.isometric.model.entity.Character;
import ctu.game.isometric.model.entity.Gender;
import ctu.game.isometric.model.game.GameState;
import ctu.game.isometric.util.AssetManager;

public class CharacterCreationController {
    private GameController gameController;

    private BitmapFont font;
    private ShapeRenderer shapeRenderer;
    private GlyphLayout layout;

    private String playerName = "";
    private String playerGender = "MALE"; // Default

    // Replace animation with static textures
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

        font = new BitmapFont();
        font.getData().setScale(1.5f);
        shapeRenderer = new ShapeRenderer();
        layout = new GlyphLayout();

        // Define UI element positions
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

        nameInputBox = new Rectangle(screenWidth / 2 - 150, screenHeight / 2 + 50, 300, 40);
        maleButton = new Rectangle(screenWidth / 2 - 160, screenHeight / 2 - 20, 150, 40);
        femaleButton = new Rectangle(screenWidth / 2 + 10, screenHeight / 2 - 20, 150, 40);
        confirmButton = new Rectangle(screenWidth / 2 - 100, screenHeight / 2 - 100, 200, 40);

        // Load static avatars instead of animations
        maleAvatar = new Texture(Gdx.files.internal("characters/male_avatar.png"));
        femaleAvatar = new Texture(Gdx.files.internal("characters/female_avatar.png"));
        currentAvatar = maleAvatar;
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
        if (isNameFieldActive) {
            for (int i = 0; i < Input.Keys.MAX_KEYCODE; i++) {
                if (Gdx.input.isKeyJustPressed(i)) {
                    if (i == Input.Keys.BACKSPACE && playerName.length() > 0) {
                        playerName = playerName.substring(0, playerName.length() - 1);
                    } else if (i == Input.Keys.ENTER) {
                        isNameFieldActive = false;
                    } else {
                        char c = Input.Keys.toString(i).charAt(0);
                        if (java.lang.Character.isLetterOrDigit(c) || c == ' ') {
                            playerName += c;
                        }
                    }
                }
            }
        }
    }

    public void render(SpriteBatch batch) {
        batch.end(); // End SpriteBatch to use ShapeRenderer

        // Draw UI boxes
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Draw name input box
        if (isNameFieldActive) {
            shapeRenderer.setColor(0.9f, 0.9f, 1f, 1);
        } else {
            shapeRenderer.setColor(0.8f, 0.8f, 0.8f, 1);
        }
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

        batch.begin(); // Resume SpriteBatch

        // Draw text elements
        float titleY = Gdx.graphics.getHeight() - 100;
        layout.setText(font, "Character Creation");
        font.setColor(Color.WHITE);
        font.draw(batch, "Character Creation", Gdx.graphics.getWidth() / 2 - layout.width / 2, titleY);

        font.draw(batch, "Name:", nameInputBox.x, nameInputBox.y + nameInputBox.height + 20);

        // Draw input text
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
    }

    private void confirmCharacter() {
        // Update character with name and gender
        Character character = gameController.getCharacter();
        character.setName(playerName);
        character.setGender(Gender.valueOf(playerGender));
        // Signal that character has been created
        // Change game state to start playing
        gameController.setCreated(true);


    }

    public void dispose() {
        maleAvatar.dispose();
        femaleAvatar.dispose();
    }
}