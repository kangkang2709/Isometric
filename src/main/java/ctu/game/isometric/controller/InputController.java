package ctu.game.isometric.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.TimeUtils;
import ctu.game.isometric.model.game.GameState;
import ctu.game.isometric.model.world.AStarPathfinder;
import ctu.game.isometric.model.world.GridPoint;
import ctu.game.isometric.view.renderer.DialogUI;
import ctu.game.isometric.view.renderer.MapRenderer;

import java.util.ArrayList;
import java.util.List;

import static com.badlogic.gdx.math.Rectangle.tmp;

public class InputController extends InputAdapter {
    private final GameController gameController;
    private DialogUI dialogUI;
    private float moveCooldown = 0;
    private final float MOVE_DELAY = 0.42f; // seconds
    private static final long INPUT_DELAY = 200; // milliseconds
    private long lastInputTime = 0;
    private MapRenderer mapRenderer;
    private boolean debugLog = true;
    private AStarPathfinder pathfinder;

    private int chunkSize = 16; // Default chunk size, adjust as needed
    private boolean isChangingChunk = false;

    private int[] toIsometricGrid(float worldX, float worldY) {
        // Get map properties
        float tileWidth = mapRenderer.getMap().getTileWidth();
        float tileHeight = mapRenderer.getMap().getTileHeight();

        // These formulas were swapped - fix the inverse isometric transformation
        float gridX = (worldX / (tileWidth/2) - worldY / (tileHeight/2)) / 2;
        float gridY = (worldX / (tileWidth/2) + worldY / (tileHeight/2)) / 2;

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


    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        // Only process left clicks during EXPLORING state
        if (button != Input.Buttons.LEFT || gameController.getCurrentState() != GameState.EXPLORING || mapRenderer == null) {
            return false;
        }

        // Convert screen coordinates to world coordinates
        Vector3 worldCoords = new Vector3(screenX, screenY, 0);
        gameController.getCamera().unproject(worldCoords);

        // Convert world coordinates to grid coordinates
        int[] gridPos = toIsometricGrid(worldCoords.x, worldCoords.y);
        int targetX = gridPos[0];
        int targetY = gridPos[1]-1;

        // Get character's current position
        int characterX = (int) Math.floor(gameController.getCharacter().getGridX());
        int characterY = (int) Math.floor(gameController.getCharacter().getGridY());

        // Debug output
        if (debugLog) {
            Gdx.app.log("Mouse", "Click at grid: " + targetX + "," + targetY);
            Gdx.app.log("Mouse", "Character at: " + characterX + "," + characterY);
        }

        // Check if target is walkable
        if (targetX >= 0 && targetX < gameController.getMap().getMapWidth() &&
                targetY >= 0 && targetY < gameController.getMap().getMapHeight() &&
                gameController.getMap().isWalkable(targetX, targetY)) {

            // Use moveTo direct method when target is close
            if (Math.abs(targetX - characterX) <= 1 && Math.abs(targetY - characterY) <= 1) {
                gameController.getCharacter().moveToward(targetX, targetY);
                moveCooldown = MOVE_DELAY;
                lastInputTime = TimeUtils.millis();
                return true;
            } else {
                // Otherwise, find path and start following it
                findAndFollowPath(characterX, characterY, targetX, targetY);
                return true;
            }
        }

        return false;
    }

    private void findAndFollowPath(int startX, int startY, int targetX, int targetY) {
        if (pathfinder == null) {
            pathfinder = new AStarPathfinder(gameController.getMap());
        }

        List<GridPoint> path = pathfinder.findPath(startX, startY, targetX, targetY);

        if (path != null && !path.isEmpty()) {
            // Remove the first point as it's the current position
            if (path.size() > 1) {
                path.remove(0);
            }

            // Start moving to the first point in the path
            if (!path.isEmpty()) {
                GridPoint nextPoint = path.get(0);
                gameController.getCharacter().moveToward(nextPoint.x, nextPoint.y);

                // Store the path for continued movement
                gameController.setCharacterPath(path);
            }
        }
    }

    // Add this method to update path following logic
    public void updatePathFollowing() {
        // If character is not moving and there's a path to follow
        if (!gameController.getCharacter().isMoving() &&
                gameController.hasCharacterPath() &&
                !gameController.getCharacterPath().isEmpty()) {

            // Get the next point in the path
            GridPoint nextPoint = gameController.getCharacterPath().get(0);
            gameController.getCharacterPath().remove(0);

            // Move character to the next point
            gameController.getCharacter().moveToward(nextPoint.x, nextPoint.y);
        }
    }

    public void update(float delta) {
        updateCooldown(delta);
        updatePathFollowing();
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
            case Keys.Q -> { // Diagonal Up-Left
                moveCharacter(1, -1);
                moved = true;
                if (debugLog) Gdx.app.log("Input", "Up-Left pressed");
            }
            case Keys.E -> { // Diagonal Up-Right
                moveCharacter(1, 1);
                moved = true;
                if (debugLog) Gdx.app.log("Input", "Up-Right pressed");
            }
            case Keys.Z -> { // Diagonal Down-Left
                moveCharacter(-1, -1);
                moved = true;
                if (debugLog) Gdx.app.log("Input", "Down-Left pressed");
            }
            case Keys.C -> { // Diagonal Down-Right
                moveCharacter(-1, 1);
                moved = true;
                if (debugLog) Gdx.app.log("Input", "Down-Right pressed");
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

    public MapRenderer getMapRenderer() {
        return mapRenderer;
    }

    public void setMapRenderer(MapRenderer mapRenderer) {
        this.mapRenderer = mapRenderer;
    }
}
