package ctu.game.isometric.view.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import ctu.game.isometric.IsometricGame;
import ctu.game.isometric.controller.GameController;
import ctu.game.isometric.model.entity.Character;
import ctu.game.isometric.view.renderer.CharacterRenderer;
import ctu.game.isometric.view.renderer.DialogRenderer;
import ctu.game.isometric.view.renderer.MapRenderer;

public class GameScreen implements Screen {
    private final IsometricGame game;
    private GameController gameController;
    private OrthographicCamera camera;
    private Viewport viewport;
    private SpriteBatch batch;


    // Renderers
    private MapRenderer mapRenderer;
    private CharacterRenderer characterRenderer;
    private DialogRenderer dialogRenderer;

    public GameScreen(IsometricGame game, GameController gameController) {
        this.game = game;
        this.gameController = gameController;
        // Setup camera and viewport
        camera = new OrthographicCamera();
        viewport = new FitViewport(1280, 720, camera);
        camera.position.set(viewport.getWorldWidth() / 2, viewport.getWorldHeight() / 2, 0);

        batch = new SpriteBatch();

        // Initialize renderers
        mapRenderer = new MapRenderer(gameController.getMap(), game.getAssetManager(), gameController.getCharacter());
        characterRenderer = new CharacterRenderer(gameController.getCharacter(), game.getAssetManager());
        dialogRenderer = new DialogRenderer(gameController.getDialogModel(), game.getAssetManager());
        // Set input processor
        Gdx.input.setInputProcessor(gameController.getInputController());
    }

    @Override
    public void render(float delta) {
        // Update game state
        gameController.update(delta);


        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        mapRenderer.render(batch);

        mapRenderer.renderWalkableTileHighlights(batch, gameController.getWalkableTiles(), gameController.getCharacter().getAnimationTime());
        characterRenderer.render(batch, mapRenderer.getOffsetX(), mapRenderer.getOffsetY());
        dialogRenderer.render(batch);
        batch.end();
    }



    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    @Override
    public void show() {
        camera.position.set(viewport.getWorldWidth() / 2, viewport.getWorldHeight() / 2, 0);
        camera.update();


    }    @Override
    public void pause() {}
    @Override
    public void resume() {}
    @Override
    public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        dialogRenderer.dispose();
    }
}