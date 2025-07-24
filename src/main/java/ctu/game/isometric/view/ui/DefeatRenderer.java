package ctu.game.isometric.view.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

public class DefeatRenderer extends UIRenderer {
    private boolean isEnded;
    private Rectangle continueButtonBounds;

    public DefeatRenderer(SpriteBatch batch, BitmapFont font, BitmapFont titleFont,
                          BitmapFont inputFont, ShapeRenderer shapeRenderer) {
        super(batch, font, titleFont, inputFont, shapeRenderer);
    }

    public void setEnded(boolean isEnded) {
        this.isEnded = isEnded;
    }

    @Override
    public void render() {
        float panelWidth = 600, panelHeight = 400;
        float panelX = (SCREEN_WIDTH - panelWidth) / 2;
        float panelY = (SCREEN_HEIGHT - panelHeight) / 2;

        // Draw defeat panel background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.8f);
        shapeRenderer.rect(panelX, panelY, panelWidth, panelHeight);
        shapeRenderer.end();

        batch.begin();
        titleFont.setColor(Color.RED);
        titleFont.draw(batch, "Bạn đã bị hạ gục!", panelX + 210, panelY + panelHeight - 40);

        // Draw defeat message
        if (!isEnded) {
            font.setColor(Color.WHITE);
            font.draw(batch, "Bạn còn cơ hội, Cleric Klein đã đưa bạn về để chữa trị.", panelX + 100, panelY + panelHeight / 2 + 30);
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
}