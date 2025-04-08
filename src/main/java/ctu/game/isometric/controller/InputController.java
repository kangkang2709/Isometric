package ctu.game.isometric.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Input.Keys;

public class InputController extends InputAdapter {
    private GameController gameController;
    private float moveCooldown = 0;
    private final float MOVE_DELAY = 0.15f; // Delay between moves in seconds

    // Add debugging
    private boolean debugLog = true;

    public InputController(GameController gameController) {
        this.gameController = gameController;
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
                moveCharacter(0, -1);
                moved = true;
                if (debugLog) Gdx.app.log("Input", "Up pressed");
            } else if (Gdx.input.isKeyPressed(Keys.S) || Gdx.input.isKeyPressed(Keys.DOWN)) {
                moveCharacter(0, 1);
                moved = true;
                if (debugLog) Gdx.app.log("Input", "Down pressed");
            } else if (Gdx.input.isKeyPressed(Keys.A) || Gdx.input.isKeyPressed(Keys.LEFT)) {
                moveCharacter(-1, 0);
                moved = true;
                if (debugLog) Gdx.app.log("Input", "Left pressed");
            } else if (Gdx.input.isKeyPressed(Keys.D) || Gdx.input.isKeyPressed(Keys.RIGHT)) {
                moveCharacter(1, 0);
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
        // Handle dialog progression
        if (keycode == Keys.SPACE) {
            if (gameController.getDialogModel().isActive()) {
                gameController.getDialogController().hideDialog();
                return true;
            }
        }

        // Add debug messages for key presses
        if (debugLog) {
            Gdx.app.log("Input", "Key pressed: " + keycode);
        }

        return false;
    }
}