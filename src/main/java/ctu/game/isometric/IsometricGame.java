package ctu.game.isometric;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import ctu.game.isometric.controller.GameController;
import ctu.game.isometric.model.game.GameState;
import ctu.game.isometric.util.AssetManager;
import ctu.game.isometric.view.screen.*;

public class IsometricGame extends Game {
    private AssetManager assetManager;
    private static GameController gameController;
    private GameScreen gameScreen;

    private SplashScreen splashScreen;
    private LinearCaveScreen dungeonScreen;
    private EndScreen gameOverScreen;
    private DarkestDungeon darkestDungeonScreen;
    private CreditsScreen creditsScreen;

    @Override
    public void create() {
        assetManager = new AssetManager();
        assetManager.loadAssets();
        Gdx.graphics.setVSync(true);
        gameController = new GameController(this);


//        splashScreen = new SplashScreen(this);
        gameScreen = new GameScreen(this, gameController);
        creditsScreen = new CreditsScreen(() -> {
            gameController.setState(GameState.MAIN_MENU);
            gameController.resetGame();
            changeScreen("GAME");
        },gameController.getCommonFont());
        gameOverScreen = new EndScreen(() -> {
            gameController.setState(GameState.MAIN_MENU);
            gameController.resetGame();
            changeScreen("GAME");
            gameOverScreen.dispose();
        }, gameController.getTitleFont(), gameController.getCommonFont());
//        dungeonScreen = new LinearCaveScreen(this, gameController);

        darkestDungeonScreen = new DarkestDungeon(this, gameController);

        setScreen(gameScreen);
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
            case "DARK_DUNGEON":
                setScreen(darkestDungeonScreen);
                break;
            case "DUNGEON":
                dungeonScreen.setGameStarted(false);
                setScreen(dungeonScreen);
                break;
            case "GAME_OVER":
                setScreen(gameOverScreen);
                break;
            case "CREDITS":
                setScreen(creditsScreen);
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

    public static GameController getGameController() {
        return gameController;
    }

    public GameScreen getGameScreen() {
        return gameScreen;
    }

    public DarkestDungeon getDarkestDungeonScreen() {
        return darkestDungeonScreen;
    }

    public void setGameScreen(GameScreen gameScreen) {
        this.gameScreen = gameScreen;
    }
}