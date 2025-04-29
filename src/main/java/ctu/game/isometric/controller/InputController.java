package ctu.game.isometric.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.TimeUtils;
import ctu.game.isometric.model.game.GameState;
import ctu.game.isometric.view.renderer.DialogUI;

public class InputController extends InputAdapter {
    private final GameController gameController;
    private DialogUI dialogUI;
    private float moveCooldown = 0;
    private final float MOVE_DELAY = 0.42f; // seconds
    private static final long INPUT_DELAY = 200; // milliseconds
    private long lastInputTime = 0;

    private boolean debugLog = true;

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

        if (state == GameState.MAIN_MENU) {
            if (keycode == Keys.ESCAPE) {
                return true;
            }
        }

        if (keycode == Keys.ESCAPE) {
            if (state == GameState.MENU) {
                gameController.returnToPreviousState();
            } else {
                gameController.setState(GameState.MENU);
                System.out.println("Game state changed to MENU");
            }
            return true;
        }

        // Xử lý Dialog trước
        if (gameController.getDialogController().isDialogActive() && state == GameState.DIALOG) {
            return handleDialogInput(keycode);
        }

        // Xử lý các menu
        switch (state) {
            case MENU:
                return handleMenuInput(keycode);
            case SETTINGS:
                return handleSettingsInput(keycode);
            case CUTSCENE:
                return handleCutSceneInput(keycode);
            case EXPLORING:
                return handleExploringInput(keycode);
            default:
                break;
        }

        // Delay chung
        if (TimeUtils.timeSinceMillis(lastInputTime) < INPUT_DELAY) {
            return false;
        }
        lastInputTime = TimeUtils.millis();

        return false;
    }

    private boolean handleExploringInput(int keycode) {
        if (moveCooldown > 0) {
            return false;
        }

        boolean moved = false;

        switch (keycode) {
            case Keys.W, Keys.UP -> {
                moveCharacter(1, 0);
                moved = true;
                if (debugLog) Gdx.app.log("Input", "Up pressed");
            }
            case Keys.S, Keys.DOWN -> {
                moveCharacter(-1, 0);
                moved = true;
                if (debugLog) Gdx.app.log("Input", "Down pressed");
            }
            case Keys.A, Keys.LEFT -> {
                moveCharacter(0, -1);
                moved = true;
                if (debugLog) Gdx.app.log("Input", "Left pressed");
            }
            case Keys.D, Keys.RIGHT -> {
                moveCharacter(0, 1);
                moved = true;
                if (debugLog) Gdx.app.log("Input", "Right pressed");
            }
            default -> {}
        }

        if (moved) {
            moveCooldown = MOVE_DELAY;
            lastInputTime = TimeUtils.millis();
        }

        return moved;
    }

    private boolean handleCutSceneInput(int keycode) {
        if (keycode == Keys.ENTER || keycode == Keys.SPACE) {
            gameController.getCutsceneController().nextPage();
            return true;
        }
        return false;
    }

    private boolean handleMenuInput(int keycode) {
        switch (keycode) {
            case Keys.UP -> gameController.getMenuController().selectPreviousItem();
            case Keys.DOWN -> gameController.getMenuController().selectNextItem();
            case Keys.ENTER, Keys.SPACE -> gameController.getMenuController().activateSelectedItem();
            default -> {}
        }
        return true;
    }

    private boolean handleSettingsInput(int keycode) {
        switch (keycode) {
            case Keys.ESCAPE -> gameController.setCurrentState(GameState.MAIN_MENU);
            case Keys.UP -> gameController.getSettingsMenuController().selectPreviousItem();
            case Keys.DOWN -> gameController.getSettingsMenuController().selectNextItem();
            case Keys.LEFT -> gameController.getSettingsMenuController().adjustSelectedOption(false);
            case Keys.RIGHT -> gameController.getSettingsMenuController().adjustSelectedOption(true);
            case Keys.ENTER, Keys.SPACE -> gameController.getSettingsMenuController().activateSelectedItem();
            default -> {}
        }
        return true;
    }

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
    public boolean scrolled(float amountX, float amountY) {
        GameState state = gameController.getCurrentState();
        if (state != GameState.EXPLORING) {
            return true;
        }

        float defaultZoom = 1.0f;
        float minZoom = 0.1f;
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

    public void updateCooldown(float delta) {
        if (moveCooldown > 0) {
            moveCooldown -= delta;
        }
    }
}
