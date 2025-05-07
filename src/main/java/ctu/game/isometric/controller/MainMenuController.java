package ctu.game.isometric.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import ctu.game.isometric.model.game.GameState;

public class MainMenuController {
    private GameController gameController;
    private Texture backgroundImage;
    private BitmapFont font;

    // Button textures
    private Texture buttonNormal;
    private Texture buttonSelected;

    // Screen dimensions
    private final int screenWidth = 1280;
    private final int screenHeight = 720;

    // Menu options
    private String[] menuOptions = {"New Game","Load Game","Settings", "Exit"};
    private int selectedOption = 0;

    // Button rectangles
    private Rectangle[] buttonRects;

    // Button dimensions
    private final int BUTTON_WIDTH = 250;
    private final int BUTTON_HEIGHT = 60;

    // Input cooldown to prevent rapid selection
    private float inputCooldown = 0;
    private final float INPUT_DELAY = 0.2f;

    public MainMenuController(GameController gameController) {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Creepster-Regular.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 36;
        parameter.color = com.badlogic.gdx.graphics.Color.WHITE;
        this.font = generator.generateFont(parameter);
        generator.dispose();

        this.gameController = gameController;

        // Load background image
        backgroundImage = new Texture(Gdx.files.internal("backgrounds/main_menu_bg.png"));

        // Load button textures
        buttonNormal = new Texture(Gdx.files.internal("ui/button.png"));
        buttonSelected = new Texture(Gdx.files.internal("ui/button_selected.png"));

        // Initialize button rectangles
        buttonRects = new Rectangle[menuOptions.length];
        int menuX = screenWidth - 350;
        int startY = screenHeight - 400;
        int spacing = 70;

        for (int i = 0; i < menuOptions.length; i++) {
            buttonRects[i] = new Rectangle(menuX, startY - (i * spacing) - BUTTON_HEIGHT, BUTTON_WIDTH, BUTTON_HEIGHT);
        }
    }

    public void update(float delta) {
        if (inputCooldown > 0) {
            inputCooldown -= delta;
        }
        handleInput();
    }

    public void render(SpriteBatch batch) {
        // Store original projection matrix
        Matrix4 originalMatrix = new Matrix4(batch.getProjectionMatrix());

        // Set projection matrix for UI rendering
        batch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0,
                Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));

        // Draw background
        batch.draw(backgroundImage, 0, 0, screenWidth, screenHeight);

        // Draw buttons
        GlyphLayout layout = new GlyphLayout();
        for (int i = 0; i < menuOptions.length; i++) {
            Rectangle buttonRect = buttonRects[i];

            // Choose texture based on selection state
            Texture buttonTexture = (i == selectedOption) ? buttonSelected : buttonNormal;

            // Draw button
            batch.draw(buttonTexture, buttonRect.x, buttonRect.y, buttonRect.width, buttonRect.height);

            // Center text on button
            layout.setText(font, menuOptions[i]);
            float textWidth = layout.width;
            float textX = buttonRect.x + (buttonRect.width - textWidth) / 2;
            float textY = buttonRect.y + buttonRect.height - 15;

            font.draw(batch, menuOptions[i], textX, textY);
        }

        batch.setProjectionMatrix(originalMatrix);
    }

    public void handleInput() {
        // Check for mouse hover
        int mouseX = Gdx.input.getX();
        int mouseY = screenHeight - Gdx.input.getY(); // Invert Y coordinate

        for (int i = 0; i < buttonRects.length; i++) {
            if (buttonRects[i].contains(mouseX, mouseY)) {
                selectedOption = i;
                break;
            }
        }

        if (inputCooldown <= 0) {
            // Keyboard navigation
            if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
                selectedOption = (selectedOption - 1 + menuOptions.length) % menuOptions.length;
                inputCooldown = INPUT_DELAY;
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
                selectedOption = (selectedOption + 1) % menuOptions.length;
                inputCooldown = INPUT_DELAY;
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                selectOption(selectedOption);
                inputCooldown = INPUT_DELAY;
            }
        }

        // Mouse click
        if (Gdx.input.justTouched()) {
            for (int i = 0; i < buttonRects.length; i++) {
                if (buttonRects[i].contains(mouseX, mouseY)) {
                    selectOption(i);
                    break;
                }
            }
        }
    }

    private void selectOption(int option) {
        switch (option) {
            case 0: // Start Game
                gameController.setCurrentState(GameState.CHARACTER_CREATION);
                gameController.setPreviousState(GameState.MAIN_MENU);
                break;
            case 1:  gameController.setState(GameState.LOAD_GAME);
                break;
            case 2: // Settings
                gameController.setState(GameState.SETTINGS);
                break;
            case 3: // Exit
                Gdx.app.exit();
                break;
        }
    }

    public void dispose() {
        backgroundImage.dispose();
        font.dispose();
        buttonNormal.dispose();
        buttonSelected.dispose();
    }
}