package ctu.game.isometric.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.TimeUtils;
import ctu.game.isometric.model.game.GameState;
import ctu.game.isometric.view.renderer.DialogUI;

public class InputController extends InputAdapter {
    private GameController gameController;
    private DialogUI dialogUI;
    private float moveCooldown = 0;
    private final float MOVE_DELAY = 0.45f; // Delay between moves in seconds
    private static final long INPUT_DELAY = 200; // milliseconds
    private long lastInputTime = 0;

    // Add debugging
    private boolean debugLog = true;

    public InputController(GameController gameController) {
        this.gameController = gameController;
    }
    public void setDialogUI(DialogUI dialogUI) {
        this.dialogUI = dialogUI;
    }

    public void update(float delta) {
        // Update cooldown timer
        if (moveCooldown > 0) {
            moveCooldown -= delta;
        }

        // Only process movement when cooldown is complete
        if (moveCooldown <= 0) {
            boolean moved = false;

            // Handle continuous movement when keys are held down
            if (Gdx.input.isKeyPressed(Keys.W) || Gdx.input.isKeyPressed(Keys.UP)) {
//                moveCharacter(0, -1);
                moveCharacter(1, 0);
                moved = true;
                if (debugLog) Gdx.app.log("Input", "Up pressed");
            } else if (Gdx.input.isKeyPressed(Keys.S) || Gdx.input.isKeyPressed(Keys.DOWN)) {
//              moveCharacter(0, 1);
                moveCharacter(-1, 0);

                moved = true;
                if (debugLog) Gdx.app.log("Input", "Down pressed");
            } else if (Gdx.input.isKeyPressed(Keys.A) || Gdx.input.isKeyPressed(Keys.LEFT)) {
                moveCharacter(0, -1);
                moved = true;
                if (debugLog) Gdx.app.log("Input", "Left pressed");
            } else if (Gdx.input.isKeyPressed(Keys.D) || Gdx.input.isKeyPressed(Keys.RIGHT)) {
//                moveCharacter(1, 0);
                 moveCharacter(0, 1);

                moved = true;
                if (debugLog) Gdx.app.log("Input", "Right pressed");
            }

            // Only reset cooldown if we actually moved
            if (moved) {
                moveCooldown = MOVE_DELAY;
            }
        }
    }

    private void moveCharacter(int dx, int dy) {
        if (gameController.canMove(dx, dy)) {
            gameController.moveCharacter(dx, dy);
        }
    }

    @Override
    public boolean keyDown(int keycode) {
        GameState state = gameController.getCurrentState();
        if(state == GameState.MAIN_MENU){
            switch (keycode) {
                case Keys.ESCAPE -> {
                    return true;
                }
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

        // Ưu tiên xử lý dialog nếu đang hiện
        if (gameController.getDialogController().isDialogActive() && state == GameState.DIALOG) {
            return handleDialogInput(keycode);
        }

        // Menu / Settings
        switch (state) {
            case MENU:
                return handleMenuInput(keycode);
            case SETTINGS:
                return handleSettingsInput(keycode);
            case CUTSCENE:
                return handleCutSceneInput(keycode);
            default:
                break;
        }

        // ESC dùng để mở/tắt menu


        // Delay đầu vào
        if (TimeUtils.timeSinceMillis(lastInputTime) < INPUT_DELAY) {
            return false;
        }

        lastInputTime = TimeUtils.millis();
        return false;
    }

// ===== Helper methods =====
    private boolean handleCutSceneInput(int keycode) {
        if (keycode == Keys.ENTER || keycode == Keys.SPACE) {
            gameController.getCutsceneController().nextPage();
            return true;
        }
        return false;
    }
    private boolean handleMenuInput(int keycode) {
        switch (keycode) {
            case Keys.UP:
                gameController.getMenuController().selectPreviousItem();
                return true;
            case Keys.DOWN:
                gameController.getMenuController().selectNextItem();
                return true;
            case Keys.ENTER:
            case Keys.SPACE:
                gameController.getMenuController().activateSelectedItem();
                return true;
            default:
                return true;
        }
    }

    private boolean handleSettingsInput(int keycode) {
        switch (keycode) {
            case Keys.ESCAPE:
                gameController.setCurrentState(GameState.MAIN_MENU);
                return true;
            case Keys.UP:
                gameController.getSettingsMenuController().selectPreviousItem();
                return true;
            case Keys.DOWN:
                gameController.getSettingsMenuController().selectNextItem();
                return true;
            case Keys.LEFT:
                gameController.getSettingsMenuController().adjustSelectedOption(false);
                return true;
            case Keys.RIGHT:
                gameController.getSettingsMenuController().adjustSelectedOption(true);
                return true;
            case Keys.ENTER:
            case Keys.SPACE:
                gameController.getSettingsMenuController().activateSelectedItem();
                return true;
            default:
                return true;
        }
    }

    private boolean handleDialogInput(int keycode) {
        if (dialogUI == null) return false;

        switch (keycode) {
            case Keys.ESCAPE:
                gameController.setState(GameState.MENU);
                gameController.setState(GameState.MENU);
            case Keys.ENTER:
            case Keys.SPACE:
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

            case Keys.UP:
                gameController.getDialogController().selectPreviousChoice();
                return true;

            case Keys.DOWN:
                gameController.getDialogController().selectNextChoice();
                return true;
            default:
                return false;
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

        if (amountY < 0) { // Scroll up -> Zoom in (closer)
            gameController.getCamera().zoom -= zoomStep;
        } else if (amountY > 0 && gameController.getCamera().zoom < defaultZoom) {
            // Only zoom out if we're already zoomed in and below default zoom
            gameController.getCamera().zoom += zoomStep;
        }

        // Limit zoom: Don't allow zooming in beyond minZoom or out beyond defaultZoom
        gameController.getCamera().zoom = MathUtils.clamp(gameController.getCamera().zoom, minZoom, defaultZoom);
        gameController.getCamera().update();

        return true;
    }

}