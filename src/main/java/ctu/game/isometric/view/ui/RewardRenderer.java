package ctu.game.isometric.view.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import ctu.game.isometric.model.game.Items;
import ctu.game.isometric.model.game.Reward;
import ctu.game.isometric.util.AssetManager;

public class RewardRenderer extends UIRenderer {
    private Items item;
    private Reward reward;
    private AssetManager assetManager;
    private Rectangle continueButtonBounds;

    public RewardRenderer(SpriteBatch batch, BitmapFont font, BitmapFont titleFont,
                          BitmapFont inputFont, ShapeRenderer shapeRenderer, AssetManager assetManager) {
        super(batch, font, titleFont, inputFont, shapeRenderer);
        this.assetManager = assetManager;
    }

    public void setReward(Items item, Reward reward) {
        this.item = item;
        this.reward = reward;
    }

    @Override
    public void render() {
        float panelWidth = 600, panelHeight = 400;
        float panelX = (SCREEN_WIDTH - panelWidth) / 2;
        float panelY = (SCREEN_HEIGHT - panelHeight) / 2;

        // Draw panel background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.8f);
        shapeRenderer.rect(panelX, panelY, panelWidth, panelHeight);
        shapeRenderer.end();

        batch.begin();
        titleFont.setColor(Color.WHITE);
        titleFont.draw(batch, "CHIẾN THẮNG", panelX + 250, panelY + panelHeight - 40);

        // Display item image and description
        if (item != null) {
            Texture itemTexture = assetManager.loadTexture(item.getItemName(), item.getTexturePath());
            if (itemTexture != null) {
                batch.setColor(Color.WHITE);
                batch.draw(itemTexture, panelX + 100, panelY + panelHeight / 2 - 32, 64, 64);
            }
            font.setColor(Color.YELLOW);
            font.draw(batch, item.getItemName() + " x" + reward.getAmount(), panelX + 180, panelY + panelHeight / 2 + 30);
            font.draw(batch, reward.getDescription(), panelX + 180, panelY + panelHeight / 2);
        }

        // Draw continue button
        float buttonWidth = 200, buttonHeight = 50;
        float buttonX = SCREEN_WIDTH / 2 - buttonWidth / 2;
        float buttonY = panelY + 50;
        continueButtonBounds = new Rectangle(buttonX, buttonY, buttonWidth, buttonHeight);

        batch.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.2f, 0.2f, 0.3f, 1f);
        shapeRenderer.rect(buttonX, buttonY, buttonWidth, buttonHeight);
        shapeRenderer.end();

        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "Tiếp tục", buttonX + 70, buttonY + 33);
        batch.end();
    }

    public Rectangle getContinueButtonBounds() {
        return continueButtonBounds;
    }

    public void dispose() {
        assetManager.dispose();
    }
}