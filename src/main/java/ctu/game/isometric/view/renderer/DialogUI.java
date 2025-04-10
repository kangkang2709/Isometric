package ctu.game.isometric.view.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import ctu.game.isometric.controller.DialogController;
import ctu.game.isometric.model.dialog.Dialog;
import ctu.game.isometric.model.dialog.Choice;

import java.util.List;

public class DialogUI {
    private DialogController dialogController;
    private BitmapFont font;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;

    private Texture characterImage;
    private String currentImagePath;

    // UI dimensions
    private static final int DIALOG_BOX_X = 50;
    private static final int DIALOG_BOX_Y = 50;
    private static final int DIALOG_BOX_WIDTH = 1280;
    private static final int DIALOG_BOX_HEIGHT = 200;

    private static final int CHARACTER_IMAGE_SIZE = 100;

    public DialogUI(DialogController dialogController) {
        this.dialogController = dialogController;
        this.font = new BitmapFont();
        font.setColor(Color.WHITE);
        this.batch = new SpriteBatch();
        this.shapeRenderer = new ShapeRenderer();
    }
    private void loadCharacterImage(String imagePath) {
        // Don't reload the same image
        imagePath = "visualnovel/character/" + imagePath;
        if (imagePath != null && !imagePath.equals(currentImagePath)) {
            // Dispose previous image to avoid memory leaks
            if (characterImage != null) {
                characterImage.dispose();
                characterImage = null;
            }

            try {
                characterImage = new Texture(Gdx.files.internal(imagePath));
                currentImagePath = imagePath;
                Gdx.app.log("DialogUI", "Loaded character image: " + imagePath);
            } catch (Exception e) {
                Gdx.app.error("DialogUI", "Failed to load character image: " + imagePath, e);
                characterImage = null;
                currentImagePath = null;
            }
        }
    }
    public void render() {
        if (dialogController == null || !dialogController.isDialogActive()) {
            return;
        }

        Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
        Gdx.gl.glBlendFunc(Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE_MINUS_SRC_ALPHA);

        // Draw dialog background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 0, 0, 0.8f);
        shapeRenderer.rect(DIALOG_BOX_X, DIALOG_BOX_Y, DIALOG_BOX_WIDTH, DIALOG_BOX_HEIGHT);
        shapeRenderer.end();

        Gdx.gl.glDisable(Gdx.gl.GL_BLEND);

        batch.begin();

        // Render current dialog
        Dialog currentDialog = dialogController.getCurrentDialog();

        if (currentDialog != null) {
            // Character name

            if (currentDialog.getCharacterImage() != null && !currentDialog.getCharacterImage().isEmpty()) {
                loadCharacterImage(currentDialog.getCharacterImage());
            }

            // Draw character image
            if (characterImage != null) {
                batch.draw(characterImage,
                        DIALOG_BOX_X + 20,
                        DIALOG_BOX_Y + DIALOG_BOX_HEIGHT - CHARACTER_IMAGE_SIZE - 100,
                        CHARACTER_IMAGE_SIZE,
                        CHARACTER_IMAGE_SIZE);
            }

            font.draw(batch, currentDialog.getCharacterName(), DIALOG_BOX_X + 20, DIALOG_BOX_Y + DIALOG_BOX_HEIGHT - 20);

            // Dialog text
            font.draw(batch, currentDialog.getText(), DIALOG_BOX_X + 150, DIALOG_BOX_Y + DIALOG_BOX_HEIGHT - 50,
                    DIALOG_BOX_WIDTH - 40, -1, true);
        }

        // Render choices if present
        List<Choice> choices = dialogController.getCurrentChoices();
        int selectedIndex = dialogController.getSelectedChoiceIndex();

        if (choices != null && !choices.isEmpty()) {
            for (int i = 0; i < choices.size(); i++) {
                String choiceText = (i == selectedIndex ? "> " : "  ") + choices.get(i).getText();
                font.draw(batch, choiceText, DIALOG_BOX_X + 150, DIALOG_BOX_Y + 80 - (i * 30));
            }
        } else if (currentDialog != null) {
            // Show continue prompt
            font.draw(batch, "Press ENTER to continue...", DIALOG_BOX_X + DIALOG_BOX_WIDTH - 300, DIALOG_BOX_Y + 20);
        }

        batch.end();
    }

    public void dispose() {
        font.dispose();
        batch.dispose();
        shapeRenderer.dispose();
        if (characterImage != null) characterImage.dispose();
    }
}