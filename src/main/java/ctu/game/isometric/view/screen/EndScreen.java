package ctu.game.isometric.view.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class EndScreen implements Screen {
    private final Runnable onEndCallback;
    private float timer = 0f;
    private boolean callbackCalled = false;

    private SpriteBatch batch;
    private BitmapFont font;

    public EndScreen(Runnable onEndCallback) {
        this.onEndCallback = onEndCallback;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        font = new BitmapFont(); // You can load a custom font if needed
    }

    @Override
    public void render(float delta) {
        // Clear screen to black
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Render "Game Over" text
        batch.begin();
        String message = "Game Over";
        float textWidth = font.getRegion().getRegionWidth();
        font.draw(batch, message,
                (Gdx.graphics.getWidth() - textWidth) / 2f,
                Gdx.graphics.getHeight() / 2f);
        batch.end();

        // Check timer or input to trigger callback
        timer += delta;
        boolean shouldEnd = timer >= 5f;

        if (shouldEnd && !callbackCalled || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            callbackCalled = true;
            if (onEndCallback != null) {
                onEndCallback.run();
            }
        }
    }

    @Override
    public void resize(int width, int height) { }

    @Override
    public void pause() { }

    @Override
    public void resume() { }

    @Override
    public void hide() { }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (font != null) font.dispose();
    }
}
