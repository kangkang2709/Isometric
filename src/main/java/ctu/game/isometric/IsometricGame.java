package ctu.game.isometric;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import ctu.game.isometric.controller.GameController;
import ctu.game.isometric.util.AssetManager;
import ctu.game.isometric.view.screen.*;

public class IsometricGame extends Game {
    private AssetManager assetManager;
    private GameController gameController;
    private GameScreen gameScreen;

    private SplashScreen splashScreen;
    private LinearCaveScreen dungeonScreen;

    @Override
    public void create() {
        assetManager = new AssetManager();
        assetManager.loadAssets();
        Gdx.graphics.setVSync(true);
        gameController = new GameController(this);

        splashScreen = new SplashScreen(this, gameController);
        gameScreen = new GameScreen(this, gameController);
        dungeonScreen = new LinearCaveScreen(this, gameController);

        setScreen(splashScreen);
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

    public void changeScreen(String name) {
        switch (name) {
            case "SPLASH":
                setScreen(splashScreen);
                break;
            case "GAME":
                setScreen(gameScreen);
                break;
            case "DUNGEON":
                setScreen(dungeonScreen);
                break;
            default:
                break;
        }
    }

    @Override
    public void dispose() {
        assetManager.dispose();
        super.dispose();

    }

    public GameScreen getGameScreen() {
        return gameScreen;
    }

    public void setGameScreen(GameScreen gameScreen) {
        this.gameScreen = gameScreen;
    }
}