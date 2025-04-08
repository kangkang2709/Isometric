package ctu.game.isometric;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import ctu.game.isometric.controller.GameController;
import ctu.game.isometric.util.AssetManager;
import ctu.game.isometric.view.screen.GameScreen;

public class IsometricGame extends Game {
    private AssetManager assetManager;
    private GameController gameController;

    @Override
    public void create() {
        assetManager = new AssetManager();
        assetManager.loadAssets();

        gameController = new GameController(this);
        setScreen(new GameScreen(this, gameController));
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        super.render();
    }

    public AssetManager getAssetManager() {
        return assetManager;
    }

    @Override
    public void dispose() {
        assetManager.dispose();
        super.dispose();
    }
}