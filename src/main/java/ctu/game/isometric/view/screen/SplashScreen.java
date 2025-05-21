package ctu.game.isometric.view.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import ctu.game.isometric.IsometricGame;
import ctu.game.isometric.controller.GameController;

public class SplashScreen implements Screen {
    private final IsometricGame game;
    private SpriteBatch batch;
    private Texture splashTexture;
    private float timer = 0;
    private final float SPLASH_DURATION = 3.0f; // 1 second duration
    private GameController gameController;

    public SplashScreen(IsometricGame game, GameController gameController) {
        this.game = game;
        this.gameController = gameController;
        batch = new SpriteBatch();
        splashTexture = new Texture(Gdx.files.internal("backgrounds/main_menu_bg.png")); // Add a splash.png to your assets folder
    }

    @Override
    public void render(float delta) {
        // Clear screen
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Update timer
        timer += delta;

        // Render splash image
        batch.begin();
        batch.draw(splashTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.end();

        // After 1 second, switch to game screen
        if (timer >= SPLASH_DURATION) {
            GameScreen gameScreen = new GameScreen(game, gameController);
            game.setGameScreen(gameScreen);
            game.setScreen(gameScreen);
            dispose();
        }
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void show() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        batch.dispose();
        splashTexture.dispose();
    }
}