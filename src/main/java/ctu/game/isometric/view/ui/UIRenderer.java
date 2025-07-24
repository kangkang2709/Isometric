package ctu.game.isometric.view.ui;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

public abstract class UIRenderer {
    protected SpriteBatch batch;
    protected BitmapFont font;
    protected BitmapFont titleFont;
    protected BitmapFont inputFont;
    protected ShapeRenderer shapeRenderer;

    protected final float SCREEN_WIDTH = 1280;
    protected final float SCREEN_HEIGHT = 720;

    public UIRenderer(SpriteBatch batch, BitmapFont font, BitmapFont titleFont,
                      BitmapFont inputFont, ShapeRenderer shapeRenderer) {
        this.batch = batch;
        this.font = font;
        this.titleFont = titleFont;
        this.inputFont = inputFont;
        this.shapeRenderer = shapeRenderer;
    }

    public abstract void render();
}