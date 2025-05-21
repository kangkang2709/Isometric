package ctu.game.isometric.controller;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import ctu.game.isometric.controller.quiz.QuizController;
import ctu.game.isometric.model.game.GameState;
import ctu.game.isometric.view.ui.DialogUI;
import ctu.game.isometric.view.renderer.MapRenderer;

public class InputController extends InputAdapter {
    private final GameController gameController;
    private DialogUI dialogUI;
    private float moveCooldown = 0;
    private final float MOVE_DELAY = 0.42f; // seconds
    private MapRenderer mapRenderer;
    private boolean debugLog = true;


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

        // Handle dialog input first
        if (gameController.getDialogController().isDialogActive() && state == GameState.DIALOG) {
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
            case QUIZZES:
                return handleQuizInput(keycode);
            case CHARACTER_CREATION:
                return gameController.getCharacterCreationController().handleTextInput(keycode);
            default:
                return false; // Explicitly return false for unhandled states
        }
    }


    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        // Only process left clicks during EXPLORING state
        GameState state = gameController.getCurrentState();

        if (state == GameState.EXPLORING) {
            if (moveCooldown > 0) return false;

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
                // Only move if the target tile is walkable
                moveCharacter(dx, dy);
                moveCooldown = MOVE_DELAY;
                return true;
            }
        }

        if (state == GameState.MENU) {
            return gameController.getMenuController().handleMouseClick(screenX, screenY);
        }
        if (state == GameState.LOAD_GAME) {
            return gameController.getLoadGameController().handleMouseClick(screenX, screenY);
        }

        if (state == GameState.GAMEPLAY) {
            return gameController.getGameplayController().handleCombatClick(screenX, screenY);
        }

        if (state == GameState.DICTIONARY) {
            gameController.getDictionaryView().handleMouseClick(screenX, screenY);
            return true;
        }

        return false;
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
                case Keys.ENTER, Keys.SPACE -> {
                    gameController.handleEventProperties(gameController.getProperties(), gameController.getCurrentEventType());
                }
                default -> {
                }
            }
        }

        switch (keycode) {
            case Keys.P -> gameController.setState(GameState.DICTIONARY);
            case Keys.ESCAPE -> gameController.setState(GameState.MENU);
            case Keys.TAB -> gameController.getExploringUI().toggleUI();
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
            case Keys.Q -> { // Diagonal Up-Left
                moveCharacter(1, -1);
                moved = true;
            }
            case Keys.E -> { // Diagonal Up-Right
                moveCharacter(1, 1);
                moved = true;
            }
            case Keys.Z -> { // Diagonal Down-Left
                moveCharacter(-1, -1);
                moved = true;
            }
            case Keys.C -> { // Diagonal Down-Right
                moveCharacter(-1, 1);
                moved = true;
            }
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
            case Keys.ESCAPE -> gameController.setState(GameState.MENU);
            default -> {
            }
        }
        return false;
    }

    private boolean handleMenuInput(int keycode) {
        switch (keycode) {
            case Keys.ESCAPE -> gameController.returnToPreviousState();
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
                if(gameController.getPreviousState() == GameState.MAIN_MENU) {
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
                    dialogUI.completeTextAnimation();
                } else {
                    if (gameController.getDialogController().hasChoices()) {
                        gameController.getDialogController().selectChoice(
                                gameController.getDialogController().getSelectedChoiceIndex());
                    } else if (!gameController.getDialogController().nextDialog()) {
                        gameController.getDialogController().endDialog();
                    }
                }
                return true;
            }
            case Keys.UP -> {
                gameController.getDialogController().selectPreviousChoice();
                return true;
            }
            case Keys.DOWN -> {
                gameController.getDialogController().selectNextChoice();
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
        }
        else if (gameController.getCurrentState() == GameState.DICTIONARY) {
            return gameController.getDictionaryView().handleKeyTyped(character);
        }
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        GameState state = gameController.getCurrentState();

        if (state == GameState.DICTIONARY) {
            gameController.getDictionaryView().handleMouseScroll(amountX, -amountY, Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY());
            return true;
        }

        if (state == GameState.EXPLORING) {
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
        return false;
    }

    public void updateCooldown(float delta) {
        if (moveCooldown > 0) {
            moveCooldown -= delta;
        }
    }

    public MapRenderer getMapRenderer() {
        return mapRenderer;
    }

    public void setMapRenderer(MapRenderer mapRenderer) {
        this.mapRenderer = mapRenderer;
    }
}
