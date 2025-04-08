package ctu.game.isometric.view.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import ctu.game.isometric.model.dialog.DialogModel;
import ctu.game.isometric.util.AssetManager;

public class DialogRenderer {
    private DialogModel dialogModel;
    private AssetManager assetManager;
    private BitmapFont font;

    public DialogRenderer(DialogModel dialogModel, AssetManager assetManager) {
        this.dialogModel = dialogModel;
        this.assetManager = assetManager;
        this.font = new BitmapFont();
        this.font.setColor(Color.BLACK);
    }

    public void render(SpriteBatch batch) {
        if (!dialogModel.isActive()) return;

        Texture dialogBox = assetManager.getTexture("ui/dialog_box.png");

        // Draw dialog box at bottom of screen
        float x = 100;
        float y = 50;
        float width = Gdx.graphics.getWidth() - 200;
        float height = 200;

        batch.draw(dialogBox, x, y, width, height);
        font.draw(batch, dialogModel.getCurrentText(), x + 20, y + height - 30, width - 40, -1, true);
    }

    public void dispose() {
        font.dispose();
    }
}