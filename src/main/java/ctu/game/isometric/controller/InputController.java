package ctu.game.isometric.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.utils.TimeUtils;
import ctu.game.isometric.model.game.GameState;
import ctu.game.isometric.view.renderer.DialogUI;

public class InputController extends InputAdapter {
    private GameController gameController;
    private DialogUI dialogUI;
    private float moveCooldown = 0;
    private final float MOVE_DELAY = 0.6f; // Delay between moves in seconds
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
        switch (keycode) {
            case Keys.ESCAPE:
                if (gameController.getCurrentState() == GameState.MENU) {
                    // Return to the previous state if we're already in menu
                    gameController.returnToPreviousState();
                    System.out.println("Returning to previous state: " +
                            gameController.getCurrentState().toString());
                } else {
                    // Go to menu, previous state will be saved
                    gameController.setState(GameState.MENU);
                    System.out.println("Game state changed to MENU");
                }
                return true;
        }

        // Handle dialog input first
        if (gameController.getDialogController().isDialogActive()) {
            switch (keycode) {
                case Keys.ENTER:
                    // Only advance dialogue if text is fully displayed
                    if (dialogUI != null && dialogUI.isTextFullyDisplayed()) {
                        if (gameController.getDialogController().hasChoices()) {
                            gameController.getDialogController().selectChoice(
                                    gameController.getDialogController().getSelectedChoiceIndex()
                            );
                        } else {
                            if (!gameController.getDialogController().nextDialog()) {
                                gameController.getDialogController().endDialog();
                            }
                        }
                    }
                    return true;

                case Keys.SPACE:
                    // If text is still animating, complete it
                    if (dialogUI != null && !dialogUI.isTextFullyDisplayed()) {
                        dialogUI.completeTextAnimation();
                    }
                    else if (dialogUI != null && dialogUI.isTextFullyDisplayed()) {
                        if (gameController.getDialogController().hasChoices()) {
                            gameController.getDialogController().selectChoice(
                                    gameController.getDialogController().getSelectedChoiceIndex()
                            );
                        } else {
                            if (!gameController.getDialogController().nextDialog()) {
                                gameController.getDialogController().endDialog();
                            }
                        }
                    }
                    return true;

                case Keys.UP:
                    gameController.getDialogController().selectPreviousChoice();
                    return true;

                case Keys.DOWN:
                    gameController.getDialogController().selectNextChoice();
                    return true;
            }
            return false;
        }

        // Handle character movement when no dialog is active
        if (TimeUtils.timeSinceMillis(lastInputTime) < INPUT_DELAY) {
            return false;
        }

        lastInputTime = TimeUtils.millis();
        return false;
    }
}