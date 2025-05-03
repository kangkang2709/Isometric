package ctu.game.isometric.view.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import ctu.game.isometric.IsometricGame;
import ctu.game.isometric.controller.GameController;
import ctu.game.isometric.model.entity.Character;
import ctu.game.isometric.model.game.GameState;
import ctu.game.isometric.view.renderer.CharacterRenderer;
import ctu.game.isometric.view.renderer.DialogUI;
import ctu.game.isometric.view.renderer.MapRenderer;

public class GameScreen implements Screen {
    private final IsometricGame game;
    private GameController gameController;
    private OrthographicCamera camera;
    private Viewport viewport;
    private SpriteBatch batch;
    private DialogUI dialogUI; // Add DialogUI
    // Renderers
    private MapRenderer mapRenderer;
    private CharacterRenderer characterRenderer;
    private boolean isCharacterCreated = false;


    public GameScreen(IsometricGame game, GameController gameController) {
        this.game = game;
        this.gameController = gameController;

        // Setup camera and viewport
        camera = new OrthographicCamera();

        viewport = new FitViewport(1280, 720, camera);
        gameController.setCamera(camera);
//        camera.setToOrtho(false, 800, 480);
//        camera.position.set(viewport.getWorldWidth() / 2, viewport.getWorldHeight() / 2, 0);

        batch = new SpriteBatch();
        // In GameScreen.java - when initializing MapRenderer
        dialogUI = new DialogUI(gameController.getDialogController());
        gameController.getInputController().setDialogUI(dialogUI);
        // Set input processor
        Gdx.input.setInputProcessor(gameController.getInputController());
    }

    @Override
    public void render(float delta) {
        // Cập nhật game
        gameController.update(delta);
        gameController.getTransitionController().update(delta);

        // Chỉ khởi tạo 1 lần khi gameController vừa tạo xong
        if (gameController.isCreated()) {
            mapRenderer = new MapRenderer(
                    gameController.getMap(),
                    game.getAssetManager(),
                    gameController.getCharacter(),
                    camera
            );
            gameController.getInputController().setMapRenderer(mapRenderer);
            characterRenderer = new CharacterRenderer(
                    gameController.getCharacter(),
                    game.getAssetManager(),
                    mapRenderer
            );

            // Reset dialog UI
            if (dialogUI != null) {
                dialogUI.dispose();
            }
            dialogUI = new DialogUI(gameController.getDialogController());
            gameController.getInputController().setDialogUI(dialogUI);

            // Reset flag
            gameController.setCreated(false);
        }

        batch.setProjectionMatrix(camera.combined);
        batch.begin();


        GameState currentState = gameController.getCurrentState();
        if (gameController.getTransitionController().isTransitioning()) {
            gameController.getTransitionController().render(batch);
        } else {
            switch (currentState) {
                case MAIN_MENU:
                    gameController.getMainMenuController().render(batch);
                    break;
                case CHARACTER_CREATION:
                    gameController.getCharacterCreationController().render(batch);
                    break;
                case EXPLORING:
                    mapRenderer.render(batch);
//                    mapRenderer.renderWalkableTileHighlights(
//                            batch,
//                            gameController.getCharacter().getAnimationTime()
//                    );
                    if (characterRenderer != null) characterRenderer.render(batch);
                    break;
                case DIALOG:
                    dialogUI.render();
                    break;
                case CUTSCENE:
                    gameController.getCutsceneController().render(batch);
                    break;
                case GAMEPLAY:
                    gameController.getGameplayController().render(batch);
                    break;
                case MENU:
                    gameController.getMenuController().render(batch);
                    break;
                case SETTINGS:
                    gameController.getSettingsMenuController().render(batch);
                    break;
                default:
                    break;
            }
        }
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    @Override
    public void show() {
        camera.update();
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        batch.dispose();
        gameController.dispose();
        dialogUI.dispose(); // Dispose DialogUI
    }
}