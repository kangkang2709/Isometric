package ctu.game.isometric;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Timer;
import ctu.game.isometric.animation.DissolveShaderManager;
import ctu.game.isometric.controller.GameController;
import ctu.game.isometric.model.game.GameState;
import ctu.game.isometric.model.perform.EnhancedMemoryMonitor;
import ctu.game.isometric.model.perform.RealTimePerformanceMonitor;
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

    private RealTimePerformanceMonitor perfMonitor;
    private BitmapFont debugFont;

    @Override
    public void create() {
        assetManager = new AssetManager();
        assetManager.loadAssets();
        Gdx.graphics.setVSync(true);
        gameController = new GameController(this);
        DissolveShaderManager.initialize();


        splashScreen = new SplashScreen(this);
//        gameScreen = new GameScreen(this, gameController);
        creditsScreen = new CreditsScreen(() -> {
            gameController.setState(GameState.MAIN_MENU);
            gameController.resetGame();
            changeScreen("GAME");
        }, gameController.getCommonFont());
        gameOverScreen = new EndScreen(() -> {
            gameController.setState(GameState.MAIN_MENU);
            gameController.resetGame();
            changeScreen("GAME");
            gameOverScreen.dispose();
        }, gameController.getTitleFont(), gameController.getCommonFont());
//        dungeonScreen = new LinearCaveScreen(this, gameController);

        darkestDungeonScreen = new DarkestDungeon(this, gameController);

        setScreen(splashScreen);

// Trong IsometricGame.create() hoặc method khác
//        IsometricPerformanceTestRunner testRunner = new IsometricPerformanceTestRunner(this);
//        testRunner.runPerformanceTests();

        perfMonitor = RealTimePerformanceMonitor.getInstance();
        debugFont = new BitmapFont();
        perfMonitor.toggleDebugOverlay();

        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                perfMonitor.startMonitoring();
                System.out.println("🎯 Real-time monitoring started!");
            }
        }, 2.0f); // Delay 2 giây

    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);


        super.render(); // Render current screen
        if (spriteBatch == null)
            spriteBatch = new SpriteBatch();
        // Render debug overlay cuối cùng
        perfMonitor.updateFrame();
        perfMonitor.renderDebugOverlay(spriteBatch, debugFont);


    }

    SpriteBatch spriteBatch;

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
        if (spriteBatch != null) {
            spriteBatch.dispose();
        }
        perfMonitor.stopMonitoring();
        DissolveShaderManager.dispose();
        debugFont.dispose();

    }

    public static GameController getGameController() {
        return gameController;
    }

    public RealTimePerformanceMonitor getPerfMonitor() {
        return perfMonitor;
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