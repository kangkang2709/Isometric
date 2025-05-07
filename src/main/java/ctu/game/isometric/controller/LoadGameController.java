package ctu.game.isometric.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import ctu.game.isometric.model.game.GameSave;
import ctu.game.isometric.model.game.GameState;
import ctu.game.isometric.util.GameSaveService;

public class LoadGameController {
    private final GameController gameController;
    private final GameSaveService saveService;
    private Texture backgroundImage;
    private BitmapFont titleFont;
    private BitmapFont font;

    // Button textures
    private Texture buttonNormal;
    private Texture buttonSelected;
    private Texture backButtonTexture;

    // Screen dimensions
    private final int screenWidth = 1280;
    private final int screenHeight = 720;

    // Save files
    private String[] saveFiles;
    private int selectedFileIndex = 0;

    // Button rectangles
    private Rectangle[] fileButtonRects;
    private Rectangle backButtonRect;

    // Button dimensions
    private final int BUTTON_WIDTH = 600;
    private final int BUTTON_HEIGHT = 60;
    private final int BACK_BUTTON_WIDTH = 200;
    private final int BACK_BUTTON_HEIGHT = 50;

    // Input cooldown
    private float inputCooldown = 0;
    private final float INPUT_DELAY = 0.2f;

    public LoadGameController(GameController gameController) {
        this.gameController = gameController;
        this.saveService = new GameSaveService();

        // Initialize fonts
        titleFont = new BitmapFont();
        titleFont.getData().setScale(2.0f);
        titleFont.setColor(Color.WHITE);

        font = new BitmapFont();
        font.getData().setScale(1.5f);

        // Load textures
        backgroundImage = new Texture(Gdx.files.internal("backgrounds/main_menu_bg.png"));
        buttonNormal = new Texture(Gdx.files.internal("ui/button.png"));
        buttonSelected = new Texture(Gdx.files.internal("ui/button_selected.png"));
        backButtonTexture = new Texture(Gdx.files.internal("ui/button.png"));

        // Back button position
        backButtonRect = new Rectangle(
                screenWidth / 2 - BACK_BUTTON_WIDTH / 2,
                50,
                BACK_BUTTON_WIDTH,
                BACK_BUTTON_HEIGHT);

        // Load save files
        refreshSaveFiles();
    }

    public void refreshSaveFiles() {
        saveFiles = saveService.getSaveFiles();

        // Reset selection if needed
        if (saveFiles.length > 0) {
            selectedFileIndex = Math.min(selectedFileIndex, saveFiles.length - 1);
        } else {
            selectedFileIndex = -1;
        }

        // Create button rectangles
        fileButtonRects = new Rectangle[saveFiles.length];
        int startY = screenHeight - 200;
        int spacing = BUTTON_HEIGHT + 20;

        for (int i = 0; i < saveFiles.length; i++) {
            fileButtonRects[i] = new Rectangle(
                    screenWidth / 2 - BUTTON_WIDTH / 2,
                    startY - (i * spacing),
                    BUTTON_WIDTH,
                    BUTTON_HEIGHT);
        }
    }

    public void update(float delta) {
        if (inputCooldown > 0) {
            inputCooldown -= delta;
        }
        handleInput();
    }

    public void render(SpriteBatch batch) {
        // Store original matrix
        Matrix4 originalMatrix = new Matrix4(batch.getProjectionMatrix());

        // Set projection matrix for UI
        batch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0,
                screenWidth, screenHeight));

        // Draw background
        batch.draw(backgroundImage, 0, 0, screenWidth, screenHeight);

        // Draw title
        GlyphLayout titleLayout = new GlyphLayout(titleFont, "Load Game");
        titleFont.draw(batch, "Load Game",
                screenWidth / 2 - titleLayout.width / 2,
                screenHeight - 100);

        // Draw save file buttons
        if (saveFiles.length == 0) {
            GlyphLayout emptyLayout = new GlyphLayout(font, "No save files found");
            font.draw(batch, emptyLayout,
                    screenWidth / 2 - emptyLayout.width / 2,
                    screenHeight / 2);
        } else {
            for (int i = 0; i < saveFiles.length; i++) {
                Rectangle rect = fileButtonRects[i];
                Texture buttonTexture = (i == selectedFileIndex) ? buttonSelected : buttonNormal;

                // Draw button
                batch.draw(buttonTexture, rect.x, rect.y, rect.width, rect.height);

                // Draw save file name
                String displayName = saveFiles[i];
                GlyphLayout layout = new GlyphLayout(font, displayName);
                font.draw(batch, displayName,
                        rect.x + 20,
                        rect.y + rect.height - (rect.height - layout.height) / 2);
            }
        }

        // Draw back button
        batch.draw(backButtonTexture, backButtonRect.x, backButtonRect.y,
                backButtonRect.width, backButtonRect.height);

        GlyphLayout backLayout = new GlyphLayout(font, "Back");
        font.draw(batch, "Back",
                backButtonRect.x + (backButtonRect.width - backLayout.width) / 2,
                backButtonRect.y + backButtonRect.height - (backButtonRect.height - backLayout.height) / 2);

        // Restore original matrix
        batch.setProjectionMatrix(originalMatrix);
    }

    public void handleInput() {
        // Mouse position
        int mouseX = Gdx.input.getX();
        int mouseY = screenHeight - Gdx.input.getY(); // Invert Y coordinate

        // Check save file hover
        for (int i = 0; i < fileButtonRects.length; i++) {
            if (fileButtonRects[i].contains(mouseX, mouseY)) {
                selectedFileIndex = i;
                break;
            }
        }

        if (inputCooldown <= 0) {
            // Keyboard navigation
            if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
                if (saveFiles.length > 0) {
                    selectedFileIndex = (selectedFileIndex - 1 + saveFiles.length) % saveFiles.length;
                }
                inputCooldown = INPUT_DELAY;
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
                if (saveFiles.length > 0) {
                    selectedFileIndex = (selectedFileIndex + 1) % saveFiles.length;
                }
                inputCooldown = INPUT_DELAY;
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                if (selectedFileIndex >= 0 && selectedFileIndex < saveFiles.length) {
                    loadSelectedSave();
                }
                inputCooldown = INPUT_DELAY;
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)) {
                gameController.setState(GameState.MAIN_MENU);
                inputCooldown = INPUT_DELAY;
            }
        }

        // Mouse click
        if (Gdx.input.justTouched()) {
            // Check save file buttons
            for (int i = 0; i < fileButtonRects.length; i++) {
                if (fileButtonRects[i].contains(mouseX, mouseY)) {
                    loadSelectedSave();
                    break;
                }
            }

            // Check back button
            if (backButtonRect.contains(mouseX, mouseY)) {
                gameController.setState(GameState.MAIN_MENU);
            }
        }
    }

    private void loadSelectedSave() {
        if (selectedFileIndex >= 0 && selectedFileIndex < saveFiles.length) {
            String fileName = saveFiles[selectedFileIndex];
            GameSave save = saveService.loadGame(fileName);

            if (save != null) {
                // Set character data in game controller
                gameController.loadCharacter(save.getCharacter());

                // THIS IS CRUCIAL: Mark as created to initialize renderers
                gameController.setCreated(true);

                // Start the game
                gameController.setState(GameState.EXPLORING);

                System.out.println("Game loaded: " + fileName);
            } else {
                System.out.println("Failed to load save: " + fileName);
            }
        }
    }

    public void dispose() {
        backgroundImage.dispose();
        titleFont.dispose();
        font.dispose();
        buttonNormal.dispose();
        buttonSelected.dispose();
        backButtonTexture.dispose();
    }
}