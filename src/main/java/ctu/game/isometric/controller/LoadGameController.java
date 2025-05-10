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

import java.util.Set;

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


    // Add these fields at the top of the class
    private Texture deleteButtonTexture;
    private Rectangle[] deleteButtonRects;

    private boolean isConfirmationDialogActive = false;
    private String fileToDelete = null;
    private Rectangle confirmYesButtonRect;
    private Rectangle confirmNoButtonRect;
    String title = "Load Game";

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
// Load delete button texture
        deleteButtonTexture = new Texture(Gdx.files.internal("ui/button_delete.png")); // Use an appropriate delete icon
        // Back button position
        backButtonRect = new Rectangle(
                screenWidth / 2 - BACK_BUTTON_WIDTH / 2,
                50,
                BACK_BUTTON_WIDTH,
                BACK_BUTTON_HEIGHT);
        confirmYesButtonRect = new Rectangle(screenWidth / 2 - 150, screenHeight / 2 - 50, 100, 50);
        confirmNoButtonRect = new Rectangle(screenWidth / 2 + 50, screenHeight / 2 - 50, 100, 50);
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
        deleteButtonRects = new Rectangle[saveFiles.length]; // Create delete buttons
        int startY = screenHeight - 200;
        int spacing = BUTTON_HEIGHT + 20;

        for (int i = 0; i < saveFiles.length; i++) {
            fileButtonRects[i] = new Rectangle(
                    screenWidth / 2 - BUTTON_WIDTH / 2,
                    startY - (i * spacing),
                    BUTTON_WIDTH,
                    BUTTON_HEIGHT);

            // Create delete button to the right of each save file button
            deleteButtonRects[i] = new Rectangle(
                    screenWidth / 2 + BUTTON_WIDTH / 2 + 10, // 10px gap after the file button
                    startY - (i * spacing),
                    BUTTON_HEIGHT, // Square button
                    BUTTON_HEIGHT);
        }
    }

    public void update(float delta) {

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
        GlyphLayout titleLayout = new GlyphLayout(titleFont, title);
        titleFont.draw(batch, title,
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

                batch.draw(deleteButtonTexture,
                        deleteButtonRects[i].x,
                        deleteButtonRects[i].y,
                        deleteButtonRects[i].width,
                        deleteButtonRects[i].height);

                // You can add an X or trash icon inside the button if needed
                GlyphLayout deleteLayout = new GlyphLayout(font, "X");
                font.draw(batch, "X",
                        deleteButtonRects[i].x + (deleteButtonRects[i].width - deleteLayout.width) / 2,
                        deleteButtonRects[i].y + deleteButtonRects[i].height -
                                (deleteButtonRects[i].height - deleteLayout.height) / 2);
            }
        }

        // Draw back button
        batch.draw(backButtonTexture, backButtonRect.x, backButtonRect.y,
                backButtonRect.width, backButtonRect.height);

        GlyphLayout backLayout = new GlyphLayout(font, "Back");
        font.draw(batch, "Back",
                backButtonRect.x + (backButtonRect.width - backLayout.width) / 2,
                backButtonRect.y + backButtonRect.height - (backButtonRect.height - backLayout.height) / 2);

        if (isConfirmationDialogActive) {
            // Draw dialog background
            batch.draw(buttonNormal, screenWidth / 2 - 200, screenHeight / 2 - 100, 400, 200);

            // Draw dialog text
            GlyphLayout dialogText = new GlyphLayout(font, "Delete save file?");
            font.draw(batch, dialogText, screenWidth / 2 - dialogText.width / 2, screenHeight / 2 + 50);

            // Draw Yes button
            batch.draw(buttonNormal, confirmYesButtonRect.x, confirmYesButtonRect.y, confirmYesButtonRect.width, confirmYesButtonRect.height);
            GlyphLayout yesText = new GlyphLayout(font, "Yes");
            font.draw(batch, "Yes", confirmYesButtonRect.x + (confirmYesButtonRect.width - yesText.width) / 2,
                    confirmYesButtonRect.y + confirmYesButtonRect.height - (confirmYesButtonRect.height - yesText.height) / 2);

            // Draw No button
            batch.draw(buttonNormal, confirmNoButtonRect.x, confirmNoButtonRect.y, confirmNoButtonRect.width, confirmNoButtonRect.height);
            GlyphLayout noText = new GlyphLayout(font, "No");
            font.draw(batch, "No", confirmNoButtonRect.x + (confirmNoButtonRect.width - noText.width) / 2,
                    confirmNoButtonRect.y + confirmNoButtonRect.height - (confirmNoButtonRect.height - noText.height) / 2);
        }



        // Restore original matrix
        batch.setProjectionMatrix(originalMatrix);
    }

    public boolean handleInput(int keycode) {
        switch (keycode) {
            case Input.Keys.UP:
            case Input.Keys.W:
                if (saveFiles.length > 0) {
                    selectedFileIndex = (selectedFileIndex - 1 + saveFiles.length) % saveFiles.length;
                }
                break;

            case Input.Keys.DOWN:
            case Input.Keys.S:
                if (saveFiles.length > 0) {
                    selectedFileIndex = (selectedFileIndex + 1) % saveFiles.length;
                }
                break;

            case Input.Keys.ENTER:
            case Input.Keys.SPACE:
                if (selectedFileIndex >= 0 && selectedFileIndex < saveFiles.length) {
                    loadSelectedSave();
                }
                break;

            case Input.Keys.ESCAPE:
            case Input.Keys.BACKSPACE:
                gameController.setState(GameState.MAIN_MENU);
                break;
        }

        return true;
    }

    public boolean handleMouseMove(int x, int y) {
        // Check if a save file button or delete button was clicked
        int mouseX = Gdx.input.getX();
        int mouseY = screenHeight - Gdx.input.getY(); // Invert Y coordinate

        // Check save file hover
        for (int i = 0; i < fileButtonRects.length; i++) {
            if (fileButtonRects[i].contains(mouseX, mouseY)) {
                selectedFileIndex = i;
                break;
            }
        }
        return false;
    }

    public boolean handleMouseClick(int x, int y) {
        int mouseX = Gdx.input.getX();
        int mouseY = screenHeight - Gdx.input.getY(); // Invert Y coordinate

        if (isConfirmationDialogActive) {
            if (confirmYesButtonRect.contains(mouseX, mouseY)) {
                deleteSaveFile(fileToDelete);
                isConfirmationDialogActive = false;
                fileToDelete = null;
                return true;
            } else if (confirmNoButtonRect.contains(mouseX, mouseY)) {
                isConfirmationDialogActive = false;
                fileToDelete = null;
                return true;
            }
            return false;
        }

        if (backButtonRect.contains(mouseX, mouseY)) {
            gameController.setState(GameState.MAIN_MENU);
            return true;
        }

        for (int i = 0; i < fileButtonRects.length; i++) {
            if (fileButtonRects[i].contains(mouseX, mouseY)) {
                loadSelectedSave();
                return true;
            }
            if (deleteButtonRects[i].contains(mouseX, mouseY)) {
                fileToDelete = saveFiles[i];
                isConfirmationDialogActive = true;
                return true;
            }
        }

        return false;
    }


    private void deleteSaveFile(String fileName) {
        if (fileName != null) {
            boolean success = saveService.deleteSave(fileName);

            if (success) {
                System.out.println("Deleted save file: " + fileName);
                refreshSaveFiles();
            } else {
                System.out.println("Failed to delete save file: " + fileName);
            }
        }
    }

    private void loadSelectedSave() {
        try {
            if (selectedFileIndex >= 0 && selectedFileIndex < saveFiles.length) {
                String fileName = saveFiles[selectedFileIndex];
                GameSave save = saveService.loadGame(fileName);

                if (save != null) {
                    Set<String> words = saveService.loadLearnedWords(save.getCharacter(), save.getWordFilePath());

                    // Set character data in game controller
                    gameController.loadCharacter(save.getCharacter());
                    gameController.getCharacter().setLearnedWords(words);

                    // THIS IS CRUCIAL: Mark as created to initialize renderers
                    gameController.setCreated(true);

                    // Start the game
                    gameController.setState(GameState.EXPLORING);

                    System.out.println("Game loaded: " + fileName);
                } else {
                    System.out.println("Failed to load save: " + fileName);
                }
            }
        } catch (Exception e) {
                this.title = "Error loading save file";
                System.out.println("Error loading save file: " + e.getMessage());
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