package ctu.game.isometric.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import ctu.game.isometric.model.game.GameState;

public class MainMenuController {
    private GameController gameController;
    private Texture backgroundImage;
    private BitmapFont font;

    // Screen dimensions
    private final int screenWidth = 1280;
    private final int screenHeight = 720;

    // Menu options
    private String[] menuOptions = {"Start New Game", "Settings", "Exit"};
    private int selectedOption = 0;

    // Input cooldown to prevent rapid selection
    private float inputCooldown = 0;
    private final float INPUT_DELAY = 0.2f;

    public MainMenuController(GameController gameController) {
        this.gameController = gameController;

        // Load background image
        backgroundImage = new Texture(Gdx.files.internal("backgrounds/main_menu_bg.png"));

        // Initialize font
        font = new BitmapFont();
        font.getData().setScale(2);
    }

    public void update(float delta) {
        // Update input cooldown
        if (inputCooldown > 0) {
            inputCooldown -= delta;
        }

        // Handle keyboard input
        handleInput();
    }

    public void render(SpriteBatch batch) {
        // Store original projection matrix
        Matrix4 originalMatrix = new Matrix4(batch.getProjectionMatrix());

        // Set projection matrix for UI rendering
        batch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0,
                Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));

        // Draw full-screen background
        batch.draw(backgroundImage, 0, 0, screenWidth, screenHeight);

        // Draw menu options
        int menuX = screenWidth-250;
        int startY = screenHeight - 400;
        int spacing = 50;

        for (int i = 0; i < menuOptions.length; i++) {
            if (i == selectedOption) {
                font.setColor(1, 1, 0, 1); // Yellow for selected option
            } else {
                font.setColor(1, 1, 1, 1); // White for other options
            }

            font.draw(batch, menuOptions[i], menuX, startY - (i * spacing));
        }

        // Restore original projection matrix
        batch.setProjectionMatrix(originalMatrix);
    }

    public void handleInput() {
        // Only process input if cooldown is complete
        if (inputCooldown <= 0) {
            // Handle keyboard navigation
            if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
                selectedOption = (selectedOption - 1 + menuOptions.length) % menuOptions.length;
                inputCooldown = INPUT_DELAY;
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
                selectedOption = (selectedOption + 1) % menuOptions.length;
                inputCooldown = INPUT_DELAY;
            }

            // Selection with Enter or Space
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) ||
                    Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                selectOption(selectedOption);
                inputCooldown = INPUT_DELAY;
            }
        }

        // Keep mouse/touch input for backwards compatibility
        if (Gdx.input.justTouched()) {
            int touchY = Gdx.graphics.getHeight() - Gdx.input.getY();
            int touchX = Gdx.input.getX();

            // Check if touch is within menu area
            int menuX = 100;
            int startY = screenHeight - 200;
            int spacing = 50;
            int menuWidth = 200;

            if (touchX >= menuX && touchX <= menuX + menuWidth) {
                for (int i = 0; i < menuOptions.length; i++) {
                    int optionY = startY - (i * spacing);
                    if (touchY >= optionY - spacing && touchY <= optionY) {
                        selectOption(i);
                        break;
                    }
                }
            }
        }
    }

    private void selectOption(int option) {
        switch (option) {
            case 0: // Start Game
                gameController.setCurrentState(GameState.CHARACTER_CREATION);
                gameController.getMusicController().playMusicForState(GameState.EXPLORING);
                gameController.setPreviousState(GameState.MAIN_MENU);
                break;
            case 1: // Settings
                gameController.setState(GameState.SETTINGS);
                break;
            case 2: // Exit
                Gdx.app.exit();
                break;
        }
    }

    public void dispose() {
        backgroundImage.dispose();
        font.dispose();
    }
}