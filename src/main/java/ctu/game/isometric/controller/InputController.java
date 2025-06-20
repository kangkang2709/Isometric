package ctu.game.isometric.controller;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import ctu.game.isometric.controller.quiz.QuizController;
import ctu.game.isometric.model.game.GameState;
import ctu.game.isometric.view.renderer.WeatherRenderer;
import ctu.game.isometric.view.ui.DialogUI;
import ctu.game.isometric.view.renderer.MapRenderer;
import ctu.game.isometric.view.ui.MerchantUI;

public class InputController extends InputAdapter {
    private final GameController gameController;
    private DialogUI dialogUI;
    private float moveCooldown = 0;
    private final float MOVE_DELAY = 0.42f; // seconds
    private MapRenderer mapRenderer;
    private boolean debugLog = true;
    private EffectManager effectManager;


    private int targetX = -1;
    private int targetY = -1;
    private boolean showTargetIndicator = false;
    private float indicatorTimer = 0;
    private final float INDICATOR_DURATION = 1.0f; // Duration in seconds

    public void setEffectManager(EffectManager effectManager) {
        this.effectManager = effectManager;
    }

    public void showTargetIndicator(int x, int y) {
        this.targetX = x;
        this.targetY = y + 1;
        this.showTargetIndicator = true;
        this.indicatorTimer = INDICATOR_DURATION;
    }

    public void updateTargetIndicator(float delta) {
        if (showTargetIndicator) {
            indicatorTimer -= delta;
            if (indicatorTimer <= 0) {
                showTargetIndicator = false;
            }
        }
    }

    private int[] toIsometricGrid(float worldX, float worldY) {
        // Get map properties
        float tileWidth = mapRenderer.getMap().getTileWidth();
        float tileHeight = mapRenderer.getMap().getTileHeight();

        // These formulas were swapped - fix the inverse isometric transformation
        float gridX = (worldX / (tileWidth / 2) - worldY / (tileHeight / 2)) / 2;
        float gridY = (worldX / (tileWidth / 2) + worldY / (tileHeight / 2)) / 2;

//        if (debugLog) {
//            Gdx.app.log("Conversion", "World: " + worldX + "," + worldY +
//                    " -> Grid: " + gridX + "," + gridY);
//        }

        return new int[]{Math.round(gridX), Math.round(gridY)};
    }

    public InputController(GameController gameController) {
        this.gameController = gameController;
    }

    public void setDialogUI(DialogUI dialogUI) {
        this.dialogUI = dialogUI;
    }

    private void moveCharacter(int dx, int dy) {
        if (gameController.canMove(dx, dy)) {
            gameController.moveCharacter(dx, dy);
        }
    }

    @Override
    public boolean keyDown(int keycode) {
        GameState state = gameController.getCurrentState();

        if (gameController.getTutorialUI().isVisible()) {
            return gameController.getTutorialUI().handleKeyPress(keycode);
        }

        // Handle dialog input first
        if (gameController.getDialogController().isDialogActive() && state == GameState.EXPLORING) {
            return handleDialogInput(keycode);
        }

        // Handle different game states
        switch (state) {
            case DICTIONARY:
                return handleDictionaryInput(keycode);
            case MENU:
                return handleMenuInput(keycode);
            case SETTINGS:
                return handleSettingsInput(keycode);
            case CUTSCENE:
                return handleCutSceneInput(keycode);
            case EXPLORING:
                if (gameController.getDialogController().isDialogActive())
                    return handleDialogInput(keycode);
                else
                    return handleExploringInput(keycode);
            case LOAD_GAME:
                return gameController.getLoadGameController().handleInput(keycode);
            case GAMEPLAY:
                return handleGamePlayInput(keycode);
            case INFORMATION: // Handle information screen inputs
                if (keycode == Keys.ESCAPE || keycode == Keys.F1) {
                    gameController.returnToPreviousState();
                    return true;
                }
                return false; // No other keys handled in INFORMATION state
            case QUIZZES:
                return handleQuizInput(keycode);
            case BOUNTY_BOARD:
                return handleBountyBoardInput(keycode);
            case QUEST_TRACKER:
                return handleQuestTrackerInput(keycode);
            case CHARACTER_CREATION:
                return gameController.getCharacterCreationController().handleTextInput(keycode);
            default:
                return false; // Explicitly return false for unhandled states
        }
    }


    private static Texture circleTexture;
    private static TextureRegion circleRegion;

    private void ensureCircleTextureExists() {
        if (circleTexture == null) {
            // Create a pixmap for drawing the circle
            int size = 64;
            Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
            pixmap.setColor(Color.WHITE);
            pixmap.fillCircle(size / 2, size / 2, size / 2 - 1);
            circleTexture = new Texture(pixmap);
            circleRegion = new TextureRegion(circleTexture);
            pixmap.dispose();
        }
    }

    public void renderTargetIndicator(SpriteBatch batch) {
        if (!showTargetIndicator) return;

        ensureCircleTextureExists();

        // Save all batch state we'll modify
        Color oldColor = batch.getColor().cpy(); // Make a copy to be safe
        float oldPackedColor = batch.getPackedColor();

        // Convert to isometric coordinates
        float[] screenPos = mapRenderer.toIsometric(targetX, targetY);
        float alpha = Math.min(1.0f, indicatorTimer / (INDICATOR_DURATION / 2));

        // Calculate size with pulsation
        float baseSize = 30;
        float pulseSize = baseSize * (0.8f + 0.2f * (float) Math.sin(indicatorTimer * 5));

        // Draw a pulsing circle with color
        batch.setColor(0.9f, 0.3f, 0.1f, alpha); // Reddish-orange color
        batch.draw(circleRegion,
                screenPos[0] - pulseSize / 2,
                screenPos[1] - pulseSize / 4,
                pulseSize, pulseSize / 2);

        // Completely restore original batch state
        batch.setColor(oldColor);
        batch.setPackedColor(oldPackedColor);
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        // Handle tutorial UI separately since it overrides other interactions
        if (gameController.getTutorialUI().isVisible()) {
            // Special case for tutorial showing in MENU state
            if (gameController.getCurrentState() == GameState.MENU && !gameController.getMenuController().isTutorialShowing()) {
                return gameController.getMenuController().handleMouseClick(screenX, screenY);
            } else if (gameController.getTutorialUI().handleClick(screenX, screenY)) {
                effectManager.playClickSound();
            }
            return true; // Click was handled by tutorial
        }

        // Process state-specific interactions using switch-case
        switch (gameController.getCurrentState()) {
            case EXPLORING:
                return handleExploringStateClick(screenX, screenY);

            case MENU:
                return gameController.getMenuController().handleMouseClick(screenX, screenY);

            case LOAD_GAME:
                return gameController.getLoadGameController().handleMouseClick(screenX, screenY);

            case GAMEPLAY:
                return gameController.getGameplayController().handleCombatClick(screenX, screenY);

            case DICTIONARY:
                gameController.getDictionaryView().handleMouseClick(screenX, screenY);
                return true;

            case MULTIPLE_CHOICE_QUIZZES:
                return gameController.getMultipleChoiceQuizController().handleClick(screenX, screenY);

            case BOUNTY_BOARD:
                return gameController.getBountyBoardView().handleClick(screenX, screenY);

            case QUEST_TRACKER:
                return gameController.getQuestTrackerView().handleClick(screenX, screenY);

            case QUIZZES:
                return true;

            default:
                return false;
        }
    }

    /**
     * Handles click interactions when in the EXPLORING state
     */
    private boolean handleExploringStateClick(int screenX, int screenY) {
        // Don't process clicks during dialog or movement cooldown
        if (gameController.getDialogController().isDialogActive() || moveCooldown > 0) {
            return false;
        }

        // Check for UI element clicks first
        if (gameController.getInventoryUI().isVisible()) {
            gameController.getInventoryUI().handleClick(screenX, screenY);
            return true;
        }

        if (gameController.getMerchantUI().isVisible()) {
            gameController.getMerchantUI().handleClick(screenX, screenY);
            return true;
        }

        if (gameController.getAchievementUI().isActive()) {
            gameController.getAchievementUI().handleInput(screenX, screenY);
            return true;
        }

        // Handle character movement
        return handleCharacterMovement(screenX, screenY);
    }

    /**
     * Processes character movement based on click coordinates
     */
    private boolean handleCharacterMovement(int screenX, int screenY) {
        // Convert screen coordinates to world coordinates
        Vector3 worldCoords = new Vector3(screenX, screenY, 0);
        gameController.getCamera().unproject(worldCoords);

        // Convert world coordinates to grid coordinates
        int[] gridPos = toIsometricGrid(worldCoords.x, worldCoords.y);
        int targetX = gridPos[0];
        int targetY = gridPos[1] - 1;

        // Get character's current position
        int characterX = (int) Math.floor(gameController.getCharacter().getGridX());
        int characterY = (int) Math.floor(gameController.getCharacter().getGridY());

        // Calculate the movement delta
        int dx = targetX - characterX;
        int dy = targetY - characterY;

        // Debug output
        if (debugLog) {
            Gdx.app.log("Mouse", "Click at grid: " + targetX + "," + targetY);
            Gdx.app.log("Mouse", "Character at: " + characterX + "," + characterY);
            Gdx.app.log("Mouse", "Delta: " + dx + "," + dy);
        }

        // Only allow movement to adjacent tiles (including diagonals)
        if (Math.abs(dx) <= 1 && Math.abs(dy) <= 1 && (dx != 0 || dy != 0)) {
            moveCharacter(dx, dy);
        } else {
            // For non-adjacent tiles, use pathfinding
            gameController.moveCharacterAlongPath(targetX, targetY);
        }

        moveCooldown = MOVE_DELAY;
        return true;
    }


    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        GameState state = gameController.getCurrentState();

        if (state == GameState.DICTIONARY) {
            Vector3 touchPos = new Vector3(screenX, screenY, 0);
            gameController.getCamera().unproject(touchPos);
            gameController.getDictionaryView().handleMouseDrag(touchPos.x, touchPos.y);
            return true;
        }

        // Your existing touchDragged code
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        GameState state = gameController.getCurrentState();

        if (state == GameState.DICTIONARY) {
            gameController.getDictionaryView().handleMouseRelease();
            return true;
        }

        // Your existing touchUp code
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        GameState state = gameController.getCurrentState();

        if (state == GameState.MENU) {
            return gameController.getMenuController().handleMouseMove(screenX, screenY);
        }
        if (state == GameState.LOAD_GAME) {
            return gameController.getLoadGameController().handleMouseMove(screenX, screenY);
        }
        if (state == GameState.EXPLORING) {
            if (gameController.getMerchantUI().isVisible()) {
                gameController.getMerchantUI().handleMouseMove(screenX, screenY);
                return true;
            }
            if (gameController.getInventoryUI().isVisible()) {
                gameController.getInventoryUI().handleMouseMove(screenX, screenY);
                return true;
            }
            return true;
        }
        // Handle other states...
        return false;
    }

    private boolean handleExploringInput(int keycode) {
        if (moveCooldown > 0) {
            return false;
        }

        boolean moved = false;


        if (gameController.hasActiveEvent()) {
            switch (keycode) {
                case Keys.E, Keys.SPACE -> {
                    effectManager.playClickSound();
                    gameController.handleEventProperties(gameController.getProperties(), gameController.getCurrentEventType());
                }
                default -> {
                }
            }
        }
        switch (keycode) {
            case Keys.F12 -> {
                if (mapRenderer.getCurrentWeather() == WeatherRenderer.WeatherType.SNOW) {
                    mapRenderer.setWeather("rain", 2f);
                } else {
                    mapRenderer.setWeather("snow", 0.4f);
                }

            }
            case Keys.F5 -> {
                gameController.getQuestTrackerView().toggleVisibility();
                gameController.setState(GameState.QUEST_TRACKER);
            }
            case Keys.F -> {
                gameController.interactWithNPC();
            }
            case Keys.G -> {
                gameController.showNPCBackStory();
            }
            case Keys.V -> {
                if (gameController.getInventoryUI().isVisible()) {
                    gameController.getInventoryUI().hide();
                }
                gameController.setState(GameState.DICTIONARY);
            }
            case Keys.F1 -> gameController.setState(GameState.INFORMATION);
            case Keys.F2 -> {
                if (gameController.getMerchantUI().isVisible())
                    gameController.getMerchantUI().hide();
                else
                    gameController.getMerchantUI().show();

            }
            case Keys.F3 -> {
                if (gameController.getAchievementUI().isActive())
                    gameController.getAchievementUI().hide();
                else
                    gameController.showAchievementUI();
            }
            case Keys.ESCAPE -> {
                if (gameController.getAchievementUI().isActive())
                    gameController.getAchievementUI().hide();
                else
                    gameController.setState(GameState.MENU);
            }
            case Keys.TAB -> gameController.getExploringUI().toggleUI();
            case Keys.I -> { // Toggle inventory
                if (gameController.getInventoryUI() != null) {
                    if (gameController.getInventoryUI().isVisible()) {
                        gameController.getInventoryUI().hide();
                    } else {
                        gameController.getInventoryUI().show();
                    }
                }
            }
            case Keys.W, Keys.UP -> {
                moveCharacter(1, 0);
                moved = true;
            }
            case Keys.S, Keys.DOWN -> {
                moveCharacter(-1, 0);
                moved = true;
            }
            case Keys.A, Keys.LEFT -> {
                moveCharacter(0, -1);
                moved = true;
            }
            case Keys.D, Keys.RIGHT -> {
                moveCharacter(0, 1);
                moved = true;
            }
//            case Keys.Q -> { // Diagonal Up-Left
//                moveCharacter(1, -1);
//                moved = true;
//            }
//            case Keys.E -> { // Diagonal Up-Right
//                moveCharacter(1, 1);
//                moved = true;
//            }
//            case Keys.Z -> { // Diagonal Down-Left
//                moveCharacter(-1, -1);
//                moved = true;
//            }
//            case Keys.C -> { // Diagonal Down-Right
//                moveCharacter(-1, 1);
//                moved = true;
//            }
            default -> {
            }
        }

        if (moved) {
            moveCooldown = MOVE_DELAY;
        }

        return moved;
    }

    private boolean handleQuizInput(int keycode) {
        // Handle result screen inputs
        QuizController quizController = gameController.getQuizController();

        if (keycode == Input.Keys.ENTER) {
            if (quizController.isShowingResults()) {
                quizController.handleNextQuiz();
            } else {
                quizController.submitAnswer();
            }
            return true;
        } else if (keycode == Input.Keys.BACKSPACE) {
            quizController.backspace();
            return true;
        } else if (keycode == Input.Keys.ESCAPE) {
            quizController.exitQuiz();
            return true;
        }
        return false;
    }


    public boolean handleBountyBoardInput(int keycode) {
        switch (keycode) {
            case Keys.ESCAPE -> gameController.setState(GameState.EXPLORING);
            case Keys.UP -> gameController.getBountyBoardView().scrollUp();
            case Keys.DOWN -> gameController.getBountyBoardView().scrollDown();
            default -> {
            }

        }
        return true;
    }

    public boolean handleQuestTrackerInput(int keycode) {
        switch (keycode) {
            case Keys.ESCAPE -> {
                gameController.setState(GameState.EXPLORING);
                gameController.getQuestTrackerView().toggleVisibility();
            }
            case Keys.UP -> gameController.getQuestTrackerView().scrollUp();
            case Keys.DOWN -> gameController.getQuestTrackerView().scrollDown();
            default -> {
            }
        }
        return true;
    }

    private boolean handleDictionaryInput(int keycode) {
        switch (keycode) {
            case Keys.ESCAPE -> gameController.setCurrentState(GameState.EXPLORING);
            case Keys.UP -> {
                gameController.getDictionaryView().selectPreviousWord();
                return true;
            }
            case Keys.DOWN -> {
                gameController.getDictionaryView().selectNextWord();
                return true;
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    private boolean handleCutSceneInput(int keycode) {
//        if (keycode == Keys.ENTER || keycode == Keys.SPACE) {
//            gameController.getCutsceneController().nextPage();
//            return true;
//        }
        switch (keycode) {
            case Keys.ESCAPE -> gameController.setState(GameState.MENU);
            default -> {
            }
        }
        return false;
    }

    private boolean handleGamePlayInput(int keycode) {
//        if (keycode == Keys.ENTER || keycode == Keys.SPACE) {
//            gameController.getCutsceneController().nextPage();
//            return true;
//        }
        switch (keycode) {
            case Keys.ESCAPE -> {
                gameController.setState(GameState.MENU);
                effectManager.playClickSound();
            }
            default -> {
            }
        }
        return false;
    }

    private boolean handleMenuInput(int keycode) {
        switch (keycode) {
            case Keys.ESCAPE -> {
                if (gameController.getMenuController().isTutorialShowing()) {
                    gameController.getMenuController().showTutorialMenu();
                } else 
                    gameController.returnToPreviousState();


            }
            case Keys.UP -> gameController.getMenuController().selectPreviousItem();
            case Keys.DOWN -> gameController.getMenuController().selectNextItem();
            case Keys.ENTER, Keys.SPACE -> gameController.getMenuController().activateSelectedItem();
            default -> {
            }
        }
        return true;
    }

    private boolean handleSettingsInput(int keycode) {
        switch (keycode) {
            case Keys.ESCAPE -> {
                System.out.println(gameController.getCurrentState() + " " + gameController.getPreviousState());
                if (gameController.getPreviousState() == GameState.MAIN_MENU) {
                    gameController.setState(GameState.MAIN_MENU);
                    gameController.setPreviousState(GameState.MAIN_MENU);
                    return true;
                } else {
                    gameController.setCurrentState(GameState.MENU);
                    return true;
                }

            }
            case Keys.UP -> gameController.getSettingsMenuController().selectPreviousItem();
            case Keys.DOWN -> gameController.getSettingsMenuController().selectNextItem();
            case Keys.LEFT -> gameController.getSettingsMenuController().adjustSelectedOption(false);
            case Keys.RIGHT -> gameController.getSettingsMenuController().adjustSelectedOption(true);
            case Keys.ENTER, Keys.SPACE -> gameController.getSettingsMenuController().activateSelectedItem();
            default -> {
            }
        }
        return true;
    }

//    @Override
//    public boolean touchDragged(int screenX, int screenY, int pointer) {
//        GameState state = gameController.getCurrentState();
//        if (state == GameState.SETTINGS) {
//            return gameController.getSettingsMenuController().handleMouseDrag(screenX, screenY);
//        }
//
//        // Rest of your existing touchDragged code
//        return false;
//    }
//    @Override
//    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
//        GameState state = gameController.getCurrentState();
//        if (state == GameState.SETTINGS) {
//            gameController.getSettingsMenuController().handleMouseUp();
//        }
//
//        // Return true to indicate we've handled this touch event
//        return state == GameState.SETTINGS;
//    }

    private boolean handleDialogInput(int keycode) {
        if (dialogUI == null) return false;

        switch (keycode) {
            case Keys.ESCAPE -> {
                gameController.setState(GameState.MENU);
                return true;
            }
            case Keys.ENTER, Keys.SPACE -> {
                if (!dialogUI.isTextFullyDisplayed()) {
                    effectManager.playClickSound();
                    dialogUI.completeTextAnimation();
                } else {
                    if (gameController.getDialogController().hasChoices()) {
                        gameController.getDialogController().selectChoice(
                                gameController.getDialogController().getSelectedChoiceIndex());
                        effectManager.playClickSound();
                    } else if (!gameController.getDialogController().nextDialog()) {
                        gameController.getDialogController().endDialog();
                    }
                }
                return true;
            }
            case Keys.UP -> {
                gameController.getDialogController().selectPreviousChoice();
                effectManager.playClickSound();

                return true;
            }
            case Keys.DOWN -> {
                gameController.getDialogController().selectNextChoice();
                effectManager.playClickSound();
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @Override
    public boolean keyTyped(char character) {
        if (gameController.getCurrentState() == GameState.QUIZZES) {
            QuizController quizController = gameController.getQuizController();

            // Only process alphanumeric characters when answering questions
            if (!quizController.isShowingResults() && Character.isLetterOrDigit(character)) {
                quizController.processInput(character);
                return true;
            }
        } else if (gameController.getCurrentState() == GameState.DICTIONARY) {
            return gameController.getDictionaryView().handleKeyTyped(character);
        }
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        GameState state = gameController.getCurrentState();

        switch (state) {
            case DICTIONARY -> {
                gameController.getDictionaryView().handleMouseScroll(amountX, -amountY, Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY());
                return true;
            }
            case GAMEPLAY -> {
                gameController.getGameplayController().handleCombatLogScroll(amountY);
            }

            case EXPLORING -> {
                float defaultZoom = 1.0f;
                float minZoom = 0.5f;
                float zoomStep = 0.1f;

                if (amountY < 0) {
                    gameController.getCamera().zoom -= zoomStep;
                } else if (amountY > 0 && gameController.getCamera().zoom < defaultZoom) {
                    gameController.getCamera().zoom += zoomStep;
                }

                gameController.getCamera().zoom = MathUtils.clamp(gameController.getCamera().zoom, minZoom, defaultZoom);
                gameController.getCamera().update();

                return true;
            }


            case BOUNTY_BOARD -> {
                if (amountY > 0) {
                    gameController.getBountyBoardView().scrollDown();
                } else if (amountY < 0) {
                    gameController.getBountyBoardView().scrollUp();
                }
                return true;
            }
            case QUEST_TRACKER -> {
                if (amountY > 0) {
                    gameController.getQuestTrackerView().scrollDown();
                } else if (amountY < 0) {
                    gameController.getQuestTrackerView().scrollUp();
                }
                return true;
            }
        }
        return false;
    }

    public void updateCooldown(float delta) {
        if (moveCooldown > 0) {
            moveCooldown -= delta;
        }
        updateTargetIndicator(delta);
    }

    public MapRenderer getMapRenderer() {
        return mapRenderer;
    }

    public void setMapRenderer(MapRenderer mapRenderer) {
        this.mapRenderer = mapRenderer;
    }
}
