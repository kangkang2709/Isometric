package ctu.game.isometric.view.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import ctu.game.isometric.controller.DialogController;
import ctu.game.isometric.model.dialog.Arc;
import ctu.game.isometric.model.dialog.Dialog;
import ctu.game.isometric.model.dialog.Choice;
import ctu.game.isometric.model.dialog.Scene;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ctu.game.isometric.util.FontGenerator.generateVietNameseFont;

public class DialogUI {
    private DialogController dialogController;
    private BitmapFont font;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;

    private Texture characterImage;
    private Texture mainCharacterImage;
    private String currentImagePath;

    private Texture backgroundTexture;
    private String currentBackgroundPath;

    // UI dimensions
    private static final int DIALOG_BOX_X = 50;
    private static final int DIALOG_BOX_Y = 50;
    private static final int DIALOG_BOX_WIDTH = 1280;
    private static final int DIALOG_BOX_HEIGHT = 200;

    private static final int CHARACTER_IMAGE_SIZE = 120;

    // Text typing effect variables
    private String currentFullText = "";
    private String displayedText = "";
    private float textTimer = 0;
    private float charactersPerSecond = 30f; // Adjust speed as needed
    private boolean isTextFullyDisplayed = true;
    private Dialog lastDialog = null;

    private BitmapFont dialogFont;
    private BitmapFont nameFont;
    private BitmapFont promptFont;

    private String mainCharacterName = "Main Character";

    public DialogUI(DialogController dialogController, String gender) {
        this.dialogController = dialogController;

        this.dialogFont = generateVietNameseFont("GrenzeGotisch.ttf", 20);
        this.nameFont = generateVietNameseFont("GrenzeGotisch.ttf", 18);
        this.promptFont = generateVietNameseFont("GrenzeGotisch.ttf", 18);
        if (gender.equalsIgnoreCase("MALE")) {
            Texture avatarTexture = new Texture(Gdx.files.internal("characters/male2.png"));
            avatarTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            this.mainCharacterImage = avatarTexture;

        } else {
            Texture avatarTexture = new Texture(Gdx.files.internal("characters/female2.png"));
            avatarTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            this.mainCharacterImage = avatarTexture;
        }
        this.batch = new SpriteBatch();
        this.shapeRenderer = new ShapeRenderer();
    }

    Map<String, Texture> characterImageCache = new HashMap<>();

    private void loadCharacterImage(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) return;

        String fullPath = "characters/" + imagePath;

        if (fullPath.equals(currentImagePath)) return;

        Texture cached = characterImageCache.get(fullPath);
        if (cached != null) {
            characterImage = cached;
            currentImagePath = fullPath;
            Gdx.app.log("DialogUI", "Loaded character image from cache: " + fullPath);
        } else {
            try {
                Texture newTexture = new Texture(Gdx.files.internal(fullPath));
                newTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

                characterImageCache.put(fullPath, newTexture);

                // Dispose old texture if needed
                if (characterImage != null && !characterImageCache.containsValue(characterImage)) {
                    characterImage.dispose();
                }

                characterImage = newTexture;
                currentImagePath = fullPath;
                Gdx.app.log("DialogUI", "Loaded and cached character image: " + fullPath);
            } catch (Exception e) {
                Gdx.app.error("DialogUI", "Failed to load character image: " + fullPath, e);
                characterImage = null;
                currentImagePath = null;
            }
        }
    }


    private void loadBackgroundImage(String imagePath) {
        if (imagePath != null && !imagePath.equals(currentBackgroundPath)) {
            // Dispose previous background to avoid memory leaks
            if (backgroundTexture != null) {
                backgroundTexture.dispose();
                backgroundTexture = null;
            }

            try {
                // Validate imagePath before loading
                String fullPath = "backgrounds/" + imagePath;
                backgroundTexture = new Texture(Gdx.files.internal(fullPath));
                currentBackgroundPath = imagePath;
                Gdx.app.log("DialogUI", "Loaded background image: " + imagePath);
            } catch (Exception e) {
                Gdx.app.error("DialogUI", "Failed to load background image: " + imagePath, e);
                backgroundTexture = null;
                currentBackgroundPath = null;
            }
        }
    }

    public void render() {
        if (dialogController == null || !dialogController.isDialogActive()) {
            return;
        }

        // Start with base rendering setup
        Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
        Gdx.gl.glBlendFunc(Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE_MINUS_SRC_ALPHA);

        // Render background first (bottom layer)
        if (dialogController.shouldRenderBackground()) {
            String currentArcId = dialogController.getCurrentArcId();
            String currentSceneId = dialogController.getCurrentSceneId();

            if (currentArcId != null && currentSceneId != null) {
                Arc arc = dialogController.findArc(currentArcId);
                Scene scene = dialogController.findScene(arc, currentSceneId);

                if (scene != null && scene.getBackground() != null) {
                    loadBackgroundImage(scene.getBackground());

                    batch.begin();
                    if (backgroundTexture != null) {
                        batch.draw(backgroundTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                    }
                    batch.end();
                }
            }
        }

        // Draw dialog background (middle layer)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 0, 0, 0.8f);
        shapeRenderer.rect(DIALOG_BOX_X, DIALOG_BOX_Y, DIALOG_BOX_WIDTH, DIALOG_BOX_HEIGHT);
        shapeRenderer.end();

        Gdx.gl.glDisable(Gdx.gl.GL_BLEND);

        batch.begin();

        // Render current dialog
        Dialog currentDialog = dialogController.getCurrentDialog();

        if (currentDialog != null) {
            // Check if the dialog has changed
            if (currentDialog != lastDialog) {
                startNewTextAnimation(currentDialog.getText());
                lastDialog = currentDialog;
            }

            // Update the text animation
            updateTextAnimation(Gdx.graphics.getDeltaTime());

            // Character name
            if (currentDialog.getCharacterImage() != null && !currentDialog.getCharacterImage().isEmpty()) {
                loadCharacterImage(currentDialog.getCharacterImage());
            }

            // Draw character image
            if (characterImage != null) {
                batch.draw(characterImage,
                        DIALOG_BOX_X + 20,
                        DIALOG_BOX_Y + DIALOG_BOX_HEIGHT - CHARACTER_IMAGE_SIZE - 60,
                        CHARACTER_IMAGE_SIZE,
                        CHARACTER_IMAGE_SIZE);
            }

            nameFont.draw(batch, currentDialog.getCharacterName(), DIALOG_BOX_X + 50, DIALOG_BOX_Y + DIALOG_BOX_HEIGHT - 20);


            dialogFont.draw(batch, displayedText, DIALOG_BOX_X + 150, DIALOG_BOX_Y + DIALOG_BOX_HEIGHT - 50,
                    DIALOG_BOX_WIDTH - 40, -1, true);

            // Show different prompts based on text animation state
            if (!isTextFullyDisplayed) {
                promptFont.draw(batch, "Press SPACE to skip...", DIALOG_BOX_X + DIALOG_BOX_WIDTH - 300, DIALOG_BOX_Y + 20);
            } else {
                promptFont.draw(batch, "Press ENTER to continue...", DIALOG_BOX_X + DIALOG_BOX_WIDTH - 300, DIALOG_BOX_Y + 20);
            }
        }

        // Render choices if present
        List<Choice> choices = dialogController.getCurrentChoices();
        int selectedIndex = dialogController.getSelectedChoiceIndex();

        if (choices != null && !choices.isEmpty()) {
            nameFont.draw(batch, mainCharacterName, DIALOG_BOX_X + 50, DIALOG_BOX_Y + DIALOG_BOX_HEIGHT - 20);
            batch.draw(mainCharacterImage,
                    DIALOG_BOX_X + 20,
                    DIALOG_BOX_Y + DIALOG_BOX_HEIGHT - CHARACTER_IMAGE_SIZE - 60,
                    CHARACTER_IMAGE_SIZE,
                    CHARACTER_IMAGE_SIZE);

            for (int i = 0; i < choices.size(); i++) {
                if (i == selectedIndex) {
                    dialogFont.setColor(Color.YELLOW);
                } else {
                    dialogFont.setColor(Color.WHITE);
                }
                String choiceText = (i == selectedIndex ? "> " : "  ") + choices.get(i).getText();
                dialogFont.draw(batch, choiceText, DIALOG_BOX_X + 150, DIALOG_BOX_Y + 120 - (i * 30));
            }
            dialogFont.setColor(Color.WHITE); // Reset after loop
        }
        batch.end();
    }

    private void startNewTextAnimation(String text) {
        currentFullText = text;
        displayedText = "";
        textTimer = 0;
        isTextFullyDisplayed = false;
    }

    private void updateTextAnimation(float deltaTime) {
        if (!isTextFullyDisplayed) {
            textTimer += deltaTime;
            int charactersToShow = (int) (textTimer * charactersPerSecond);

            if (charactersToShow >= currentFullText.length()) {
                displayedText = currentFullText;
                isTextFullyDisplayed = true;
            } else {
                displayedText = currentFullText.substring(0, charactersToShow);
            }
        }
    }

    // Call this method when the player presses a key to show all text immediately
    public boolean completeTextAnimation() {
        if (!isTextFullyDisplayed) {
            displayedText = currentFullText;
            isTextFullyDisplayed = true;
            return true; // Text was fast-forwarded
        }
        return false; // Text was already complete
    }

    public boolean isTextFullyDisplayed() {
        return isTextFullyDisplayed;
    }

    public String getMainCharacterName() {
        return mainCharacterName;
    }

    public void setMainCharacterName(String mainCharacterName) {
        this.mainCharacterName = mainCharacterName;
    }

    public void dispose() {
        dialogFont.dispose();
        nameFont.dispose();
        promptFont.dispose();
        batch.dispose();
        shapeRenderer.dispose();
        if (characterImage != null) characterImage.dispose();
        if (backgroundTexture != null) backgroundTexture.dispose();
    }
}