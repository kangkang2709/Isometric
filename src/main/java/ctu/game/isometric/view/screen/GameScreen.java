package ctu.game.isometric.view.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import ctu.game.isometric.IsometricGame;
import ctu.game.isometric.controller.GameController;
import ctu.game.isometric.model.dictionary.Dictionary;
import ctu.game.isometric.model.game.Dice;
import ctu.game.isometric.model.game.GameState;
import ctu.game.isometric.view.renderer.CharacterRenderer;
import ctu.game.isometric.view.ui.*;
import ctu.game.isometric.view.renderer.MapRenderer;
import ctu.game.isometric.view.view.DictionaryView;


public class GameScreen implements Screen {
    private final IsometricGame game;
    private GameController gameController;
    private OrthographicCamera camera;
    private Viewport viewport;
    private SpriteBatch batch;
    private DialogUI dialogUI; // Add DialogUI
    private ExploringUI exploringUI;
    // Renderers
    private MapRenderer mapRenderer;
    private CharacterRenderer characterRenderer;
    private boolean isCharacterCreated = false;
    private GameState currentState = GameState.MAIN_MENU;
    private Dice dice;


    public GameScreen(IsometricGame game, GameController gameController) {
        this.game = game;
        this.gameController = gameController;
        gameController.setAssetManager(game.getAssetManager());
        gameController.createBoard();
        // Setup camera and viewport
        camera = new OrthographicCamera();
        viewport = new FitViewport(1280, 720, camera);
        gameController.setCamera(camera);
//        camera.setToOrtho(false, 800, 480);
//        camera.position.set(viewport.getWorldWidth() / 2, viewport.getWorldHeight() / 2, 0);

        batch = new SpriteBatch();
        originalMatrix = batch.getProjectionMatrix().cpy();
        originalColor = batch.getColor().cpy();
        // In GameScreen.java - when initializing MapRenderer
        dialogUI = new DialogUI(gameController.getDialogController(),"MALE");
        gameController.getInputController().setDialogUI(dialogUI);
        // Set input processor

    }


    Matrix4 originalMatrix;
    Color originalColor;

    @Override
    public void render(float delta) {
        // Update game

        gameController.update(delta);

        gameController.getTransitionController().update(delta);


        // Initialize gameController if created
        if (gameController.isCreated()) {
            cleanupForMainMenu();


            mapRenderer = new MapRenderer(
                    gameController.getMap(),
                    game.getAssetManager(),
                    gameController.getEventManager(),
                    gameController.getCharacter(),
                    camera
            );


            this.dice = new Dice(
                    game.getAssetManager().getAnimationManager(),
                    770, 15, gameController
            );
            mapRenderer.setDice(dice);
            gameController.setMapRenderer(mapRenderer);
            gameController.getInputController().setMapRenderer(mapRenderer);

            characterRenderer = new CharacterRenderer(
                    gameController.getCharacter(),
                    game.getAssetManager(),
                    mapRenderer
            );

            InventoryUI inventoryUI = new InventoryUI(gameController);
            gameController.setInventoryUI(inventoryUI);

            exploringUI = new ExploringUI(gameController);

            dialogUI = new DialogUI(gameController.getDialogController(), gameController.getCharacter().getGender().toString());
            dialogUI.setMainCharacterName(gameController.getCharacter().getName());
            gameController.getInputController().setDialogUI(dialogUI);

            if (gameController.getDictionaryView() != null) {
                gameController.getDictionaryView().dispose();
            }

            gameController.resetLearnedWords();
            gameController.setDictionaryView(new DictionaryView(gameController, gameController.getDictionary(), gameController.getWordNetValidator()));
            gameController.setCharacterDisplay();
            gameController.getAchievementUI().hide();
            gameController.setMerchantUI(new MerchantUI(gameController));
            gameController.initializeNPCs(mapRenderer);
            gameController.getBountyBoardController().reset();

            mapRenderer.setDialogController(gameController.getDialogController());
            mapRenderer.loadTextures();
            System.out.println("MapRenderer initialized with textures loaded.");
            gameController.setCreated(false);
        }


        batch.setProjectionMatrix(camera.combined);
        batch.setColor(originalColor);
        batch.begin();


        currentState = gameController.getCurrentState();
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
                case INFORMATION:
                    gameController.getCharacterDisplay().render(batch);
                    break;
                case BOUNTY_BOARD:
                    gameController.getBountyBoardView().render(batch);
                    break;

                case EXPLORING:
                    gameController.getMap().cleanupChunks();
                    gameController.disposeSome();
                    mapRenderer.render(batch);
                    mapRenderer.update(delta);

                    if (gameController.hasActiveEvent()) {
                        mapRenderer.renderActionButton(
                                batch,
                                gameController.getCurrentEventType(),
                                gameController.getCurrentEvent(),
                                gameController.getCurrentEventX(),
                                gameController.getCurrentEventY()
                        );
                    }

                    if (gameController.getNpcRenderer() != null) {
                        gameController.getNpcRenderer().render(batch);
                    }

                    if (characterRenderer != null && gameController.isRenderCharacter())
                        characterRenderer.render(batch);
                    gameController.getBoardEventManager().getWordScrambleGame().render(batch, 490, 435);


                    gameController.getInputController().renderTargetIndicator(batch);

                    batch.setProjectionMatrix(camera.combined);


                    // Render the UI on top
                    if (exploringUI != null) exploringUI.render();

                    if (gameController.getInventoryUI() != null) {
                        gameController.getInventoryUI().render(batch);
                    }
                    if (gameController.getAchievementUI().isActive()) {
                        gameController.getAchievementUI().render(batch);
                    }
                    if (gameController.getMerchantUI().isVisible()) {

                        gameController.getMerchantUI().render(batch);
                    }


                    if (dialogUI != null && gameController.getDialogController().isDialogActive()) {
                        dialogUI.render();
                        gameController.getEffectManager().render(batch);
                    }

                    if (gameController.getLevelUpNotification().isActive()) {
                        gameController.getLevelUpNotification().render(batch);
                    }
                    break;

                case CUTSCENE:
                    gameController.getCutsceneController().render(batch);
                    break;
                case DICTIONARY:
                    gameController.getDictionaryView().render(batch);
                    break;
                case GAMEPLAY:
                    gameController.getGameplayController().render(batch);
                    break;
                case QUIZZES:
                    gameController.getQuizController().render(batch);
                    break;
                case MULTIPLE_CHOICE_QUIZZES:
                    gameController.getMultipleChoiceQuizController().render(batch);
                    break;
                case QUEST_TRACKER:
                    gameController.getQuestTrackerView().render(batch);
                    break;
                case MENU:
                    gameController.getMenuController().render(batch);
                    break;
                case LOAD_GAME:
                    gameController.getLoadGameController().render(batch);
                    break;
                case SETTINGS:
                    gameController.getSettingsMenuController().render(batch);
                    break;
                default:
                    break;
            }
        }

        batch.end();

        // Only render TutorialUI during gameplay states
        if (currentState == GameState.EXPLORING ||
                currentState == GameState.GAMEPLAY ||
                currentState == GameState.DICTIONARY ||
                currentState == GameState.QUIZZES ||
                currentState == GameState.MULTIPLE_CHOICE_QUIZZES ||
                currentState == GameState.QUEST_TRACKER ||
                currentState == GameState.INFORMATION ||
                currentState == GameState.BOUNTY_BOARD ||
                currentState == GameState.MENU
        ) {

            gameController.getTutorialUI().render(batch);
        }


//        batch.setProjectionMatrix(originalMatrix);
    }

    public void cleanupForMainMenu() {
        if (mapRenderer != null) {
            mapRenderer.dispose();
            mapRenderer = null;
        }
        if (exploringUI != null) {
            exploringUI.dispose();
            exploringUI = null;
        }
        if (dialogUI != null) {
            dialogUI.dispose();
            dialogUI = null;
        }
        // Consider clearing any cached data in gameController
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    @Override
    public void show() {
//        camera.zoom = 0.5f;
        camera.update();
        Gdx.input.setInputProcessor(gameController.getInputController());
    }

    @Override
    public void pause() {
        if (gameController.getCurrentState() == GameState.MAIN_MENU || gameController.getCurrentState() == GameState.SETTINGS) {
            return;
        }
        System.out.println("GameScreen paused");
        gameController.setState(GameState.MENU);
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
        if (dialogUI != null) {
            dialogUI.dispose(); // Dispose DialogUI
        }
        gameController.getAchievementUI().dispose();
        gameController.getCharacterDisplay().dispose();
        // Remove incomplete line
        if (exploringUI != null)
            exploringUI.dispose();

        // Dispose any other renderers that might have been created
        if (mapRenderer != null) {
            mapRenderer.dispose();
        }
        if (characterRenderer != null) {
            characterRenderer.dispose();
        }
    }

    public MapRenderer getMapRenderer() {
        return mapRenderer;
    }

    public void setMapRenderer(MapRenderer mapRenderer) {
        this.mapRenderer = mapRenderer;
    }
}