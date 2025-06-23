package ctu.game.isometric.model.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import ctu.game.isometric.controller.GameController;
import ctu.game.isometric.util.AnimationManager;

import java.util.Map;

public class Dice {
    private final AnimationManager animationManager;
    private final float diceX;
    private final float diceY;
    private ParticleEffect rollEffect;

    private int currentFaceValue = 1;
    private boolean isRolling = false;
    private float diceRollingTime = 0f;
    private float bounceTime = 0f;
    private float scale = 1f;
    private float rotation = 0f;

    private static final float DICE_ROLL_DURATION = 1f;
    private static final float BOUNCE_DURATION = 0.5f;
    private static final int DICE_SIZE = 90;
    private static final int MIN_DICE_VALUE = 1;
    private static final int MAX_DICE_VALUE = 6;

    private GameController gameController;
    private int currentPathIndex = 0;

    // Define board path coordinates in clockwise order based on logs
    // Starting from position (10,0)
    private final int[][] boardPath = {
            {10, 0}, {11, 0},
            {11, 1}, {11, 2}, {11, 3}, {11, 4}, {11, 5}, {11, 6}, {11, 7}, {11, 8}, {11, 9},
            {12, 9}, {13, 9}, {14, 9}, {15, 9}, {16, 9}, {17, 9}, {18, 9}, {19, 9}, {20, 9},
            {20, 10}, {20, 11},
            {19, 11}, {18, 11}, {17, 11}, {16, 11}, {15, 11}, {14, 11}, {13, 11}, {12, 11}, {11, 11},
            {11, 12}, {11, 13}, {11, 14}, {11, 15}, {11, 16}, {11, 17}, {11, 18}, {11, 19}, {11, 20},
            {10, 20}, {9, 20},
            {9, 19}, {9, 18}, {9, 17}, {9, 16}, {9, 15}, {9, 14}, {9, 13}, {9, 12}, {9, 11},
            {8, 11}, {7, 11}, {6, 11}, {5, 11}, {4, 11}, {3, 11}, {2, 11}, {1, 11}, {0, 11},
            {0, 10}, {0, 9},
            {1, 9}, {2, 9}, {3, 9}, {4, 9}, {5, 9}, {6, 9}, {7, 9}, {8, 9}, {9, 9},
            {9, 8}, {9, 7}, {9, 6}, {9, 5}, {9, 4}, {9, 3}, {9, 2}, {9, 1}, {9, 0}
    };


    public Dice(AnimationManager animationManager, float x, float y, GameController gameController) {
        this.animationManager = animationManager;
        this.diceX = x;
        this.diceY = y;
        this.gameController = gameController;

        // Initialize particle effect
        rollEffect = new ParticleEffect();
        rollEffect.load(Gdx.files.internal("effects/dice_roll/dice_roll.p"), Gdx.files.internal("effects/dice_roll/"));
        rollEffect.setPosition(diceX + DICE_SIZE / 2, diceY + DICE_SIZE / 2);
    }


    public Map<Integer, int[][]> defaultEventsForRun = Map.of(
            0, new int[][]{{11, 4}, {11, 5}},
            2, new int[][]{{11, 0}, {11, 1}},
            4, new int[][]{{11, 1}, {11, 2}},
            6, new int[][]{{11, 2}, {11, 3}},
            8, new int[][]{{11, 3}, {11, 4}},
            10, new int[][]{{11, 4}, {11, 5}}
    );

    public int rollDice() {
        if (gameController.getCharacter().isMoving()) {
            return 0;
        }
        System.out.println(gameController.getCurrentEvent());
        if (gameController.getCurrentEvent()!= null) {
            gameController.getDialogController().showSimpleMessage("You cannot roll the dice while an event is active.");
            return 0;
        }

        int runIndex = gameController.getCharacter().getRun();
        int gridX = (int) gameController.getCharacter().getGridX();
        int gridY = (int) gameController.getCharacter().getGridY();

        if (gridX == 9 && gridY == 0 && !gameController.isNewRun()) {
            gameController.getDialogController().showSimpleMessage("You cannot roll the dice yet. Please wait for your turn.");
            return 0;
        }

        // Check if there are default events for current run
        if (defaultEventsForRun.containsKey(runIndex)) {
            int[][] eventPositions = defaultEventsForRun.get(runIndex);

            // Find current position in board path
            int currentBoardIndex = -1;
            for (int i = 0; i < boardPath.length; i++) {
                if (boardPath[i][0] == gridX && boardPath[i][1] == gridY) {
                    currentBoardIndex = i;
                    break;
                }
            }

            if (currentBoardIndex == -1) {
                // Character position not found in board path, use random roll
                currentFaceValue = MathUtils.random(MIN_DICE_VALUE, MAX_DICE_VALUE);
            } else {
                // Special handling for position (9,0) - last cell
                // Find index of (9,0) in boardPath
                int endBoardIndex = -1;
                for (int i = 0; i < boardPath.length; i++) {
                    if (boardPath[i][0] == 9 && boardPath[i][1] == 0) {
                        endBoardIndex = i;
                        break;
                    }
                }

                // Find the next event position the character should reach
                int targetBoardIndex = -1;

                for (int i = 0; i < eventPositions.length; i++) {
                    int[] eventPos = eventPositions[i];
                    // Find this event position in the board path
                    for (int j = 0; j < boardPath.length; j++) {
                        if (boardPath[j][0] == eventPos[0] && boardPath[j][1] == eventPos[1]) {
                            // Check if this event position is ahead of current position
                            // but not beyond the end position (9,0)
                            if (j > currentBoardIndex && (endBoardIndex == -1 || j <= endBoardIndex)) {
                                targetBoardIndex = j;
                                break;
                            }
                        }
                    }
                    if (targetBoardIndex != -1) break;
                }

                // If no event found within bounds, check if we should go to end position (9,0)
                if (targetBoardIndex == -1 && endBoardIndex != -1 && endBoardIndex > currentBoardIndex) {
                    // Calculate steps to reach (9,0) - the last cell
                    int stepsToEnd = endBoardIndex - currentBoardIndex;
                    if (stepsToEnd >= MIN_DICE_VALUE && stepsToEnd <= MAX_DICE_VALUE) {
                        currentFaceValue = stepsToEnd;
                        currentPathIndex = currentBoardIndex;
                    } else {
                        // If steps to end are out of range, use random roll
                        currentFaceValue = MathUtils.random(MIN_DICE_VALUE, MAX_DICE_VALUE);
                    }
                } else if (targetBoardIndex != -1) {
                    // Calculate steps needed to reach the target event position
                    int stepsNeeded = targetBoardIndex - currentBoardIndex;

                    // Ensure steps are within valid dice range
                    if (stepsNeeded >= MIN_DICE_VALUE && stepsNeeded <= MAX_DICE_VALUE) {
                        currentFaceValue = stepsNeeded;
                        currentPathIndex = currentBoardIndex;
                    } else {
                        // If steps needed are out of range, use random roll
                        currentFaceValue = MathUtils.random(MIN_DICE_VALUE, MAX_DICE_VALUE);
                    }
                } else {
                    // No more event positions to reach in this run, use random roll
                    currentFaceValue = MathUtils.random(MIN_DICE_VALUE, MAX_DICE_VALUE);
                }
            }
        } else {
            // No default events for this run, use random roll
            currentFaceValue = MathUtils.random(MIN_DICE_VALUE, MAX_DICE_VALUE);
        }

        // Start rolling animation
        isRolling = true;
        diceRollingTime = 0f;
        bounceTime = 0f;
        rollEffect.start();

        return currentFaceValue;
    }

    public void update(float delta) {
        if (isRolling) {
            diceRollingTime += delta;

            // Update scale and rotation during rolling
            scale = 1f + 0.3f * MathUtils.sin(diceRollingTime * 10);
            rotation = diceRollingTime * 360 % 360;

            // Check if rolling animation is complete
            if (diceRollingTime >= DICE_ROLL_DURATION) {
                isRolling = false;
                bounceTime = 0f;
                moveCharacterClockwise();
            }
        } else if (bounceTime < BOUNCE_DURATION) {
            // Apply bounce effect when showing the result
            bounceTime += delta;
            float progress = Math.min(bounceTime / BOUNCE_DURATION, 1.0f);
            scale = 1f + 0.5f * Interpolation.bounceOut.apply(1f - progress);
            rotation = 0f;
        }

        rollEffect.update(delta);
    }

    private void moveCharacterClockwise() {
        if (gameController == null) {
            Gdx.app.error("Dice", "GameController is not initialized");
            return;
        }

        // Calculate new position by moving clockwise around the board
        int steps = currentFaceValue;
        int oldPathIndex = currentPathIndex;
        int newPathIndex = (currentPathIndex + steps) % boardPath.length;

        // Check if we passed through or landed on the start position (index 0)
        boolean passedStart = false;

        if (oldPathIndex == 0) {
            // Starting from start position - only count as "passing start" if we make a full lap
            passedStart = steps >= boardPath.length;
        } else {
            // Not starting from start position
            if (oldPathIndex + steps >= boardPath.length) {
                // We wrapped around the board, so we passed through start (index 0)
                passedStart = true;
            }
        }

        // Auto-adjust target if passed start
        int targetX, targetY;

        if (passedStart) {
            // Auto-adjust to position (9,0) when passing start
            targetX = 9;
            targetY = 0;
            // Find the index for position (9,0) in the board path

            newPathIndex = 0;


        } else {
            // Normal movement - use calculated position
            targetX = boardPath[newPathIndex][0];
            targetY = boardPath[newPathIndex][1];
        }

        // Validate the new position
        if (newPathIndex < 0 || newPathIndex >= boardPath.length) {
            return;
        }


        // Use the game controller to move the character
        gameController.moveCharacterAlongPath(targetX, targetY);

        // Update the current position
        currentPathIndex = newPathIndex;
    }

    public void render(SpriteBatch batch) {
        // Draw particle effect
        rollEffect.draw(batch);

        TextureRegion frame = animationManager.getDiceFrame(isRolling, currentFaceValue, diceRollingTime);

        // Draw dice with scale and rotation
        batch.draw(
                frame,
                diceX - (DICE_SIZE * scale - DICE_SIZE) / 2,
                diceY - (DICE_SIZE * scale - DICE_SIZE) / 2,
                DICE_SIZE / 2,
                DICE_SIZE / 2,
                DICE_SIZE,
                DICE_SIZE,
                scale,
                scale,
                rotation
        );
    }

    public boolean isAnimating() {
        return isRolling || bounceTime < BOUNCE_DURATION;
    }

    public boolean handleClick(float screenX, float screenY) {
        // Check if click is within dice bounds
        return !isAnimating() &&
                screenX >= diceX && screenX <= diceX + DICE_SIZE &&
                screenY >= diceY && screenY <= diceY + DICE_SIZE;
    }

    public void setCurrentPathIndex(int index) {
        if (index >= 0 && index < boardPath.length) {
            currentPathIndex = index;
        }
    }

    public int getCurrentFaceValue() {
        return currentFaceValue;
    }

    public int getCurrentPathIndex() {
        return currentPathIndex;
    }

    public int[][] getBoardPath() {
        return boardPath;
    }

    public void dispose() {
        if (rollEffect != null) {
            rollEffect.dispose();
        }
    }
}