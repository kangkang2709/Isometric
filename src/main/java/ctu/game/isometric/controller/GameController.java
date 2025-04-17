package ctu.game.isometric.controller;

import ctu.game.isometric.IsometricGame;
import ctu.game.isometric.model.entity.Character;
import ctu.game.isometric.model.game.GameState;
import ctu.game.isometric.model.world.IsometricMap;

public class GameController {


    private IsometricGame game;
    private Character character;
    private IsometricMap map;
    private InputController inputController;
    private DialogController dialogController; // New field
    private MusicController musicController;

    private GameState currentState = GameState.EXPLORING;
    private GameState previousState = GameState.EXPLORING;

    public GameController(IsometricGame game) {
        this.game = game;
        this.map = new IsometricMap();
        this.character = new Character(0, 0);
        this.inputController = new InputController(this);
        this.dialogController = new DialogController(this);
        this.musicController = new MusicController();


        this.musicController.initialize();
        this.musicController.playMusicForState(GameState.EXPLORING);

    }

    public void update(float delta) {
        switch (currentState) {
            case EXPLORING:
                inputController.update(delta);
                character.update(delta);
                break;

            case DIALOG:
                // Only update dialog controller when in dialog state
                // dialogController.update(delta);
                break;

            case MENU:
                // Update menu-specific logic (if needed)
                break;

            case CUTSCENE:
                character.update(delta); // Keep character animations running
                break;
        }
    }

    public GameState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(GameState currentState) {
        this.currentState = currentState;
    }

    public void setState(GameState newState) {
        if (currentState == newState) return;

        GameState oldState = currentState;
        currentState = newState;

        previousState = oldState; // Correctly assign the old state
        // Update music based on the new state
        musicController.playMusicForState(newState);

        // Additional state transition logic if needed
        onStateChanged(oldState, newState);
    }

    private void onStateChanged(GameState oldState, GameState newState) {
        // Notify relevant subsystems about state change
    }

    public void returnToPreviousState() {
        setState(previousState);
    }
    public GameState getPreviousState() {
        return previousState;
    }
    public boolean canMove(int dx, int dy) {
        int newX = (int) (character.getGridX() + dx);
        int newY = (int) (character.getGridY() + dy);

        return map.isWalkable(newX,newY);
    }

    // Add a method to change maps safely
    public void changeMap(IsometricMap newMap, int startX, int startY) {
        this.map = newMap;

        // Ensure character is placed at a valid position on the new map
        if (isValidPosition(startX, startY)) {
            character.setPosition(startX, startY);
        } else {
            // Find a valid starting position if the provided one is invalid
            findValidStartPosition();
        }
    }

    private boolean isValidPosition(int x, int y) {
        if (map == null || map.getMapData() == null) return false;

        int[][] mapData = map.getMapData();
        if (mapData.length == 0) return false;

        if (x < 0 || y < 0 || y >= mapData.length || x >= mapData[0].length) {
            return false;
        }

        return mapData[y][x] != 0;
    }

    private void findValidStartPosition() {
        // Find the first walkable tile on the new map
        int[][] mapData = map.getMapData();
        for (int y = 0; y < mapData.length; y++) {
            for (int x = 0; x < mapData[y].length; x++) {
                if (mapData[y][x] != 0) {
                    character.setPosition(x, y);
                    return;
                }
            }
        }
        // If no walkable tile found, place at (0,0) as a last resort
        character.setPosition(0, 0);
    }


    public void moveCharacter(int dx, int dy) {


        if (!canMove(dx, dy)) {
            return; // Skip this move if it's invalid
        }

        float newX = character.getGridX() + dx;
        float newY = character.getGridY() + dy;

        character.moveToward(newX, newY);
//        character.setMoving(true);

        // Optional: Trigger a dialog when character reaches certain positions
        checkPositionEvents(newX, newY);
    }

    private void checkPositionEvents(float x, float y) {
        // Example: Show dialog when character reaches specific positions
        if (x == 2 && y == 3) {
            setState(GameState.DIALOG);
            dialogController.startDialog("chapter_01", "scene_01");
        }
    }
    public boolean[][] getWalkableTiles() {
        // First check if map is valid
        if (map == null || map.getMapData() == null || map.getMapData().length == 0) {
            return new boolean[0][0];
        }

        int height = map.getMapData().length;
        int width = map.getMapData()[0].length;
        boolean[][] walkable = new boolean[height][width];

        // Initialize all tiles as not walkable
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                walkable[y][x] = false;
            }
        }

        // Mark only adjacent walkable tiles
        int charX = (int) character.getGridX();
        int charY = (int) character.getGridY();

        // Check adjacent tiles (up, down, left, right)
        int[][] directions = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        for (int[] dir : directions) {
            int newX = charX + dir[0];
            int newY = charY + dir[1];

            // First validate that this position is inside the map
            if (newX < 0 || newY < 0 || newX >= width || newY >= height) {
                continue; // Skip this direction if it's outside the map
            }

            // Then check if we can move there
            if (canMove(dir[0], dir[1])) {
                walkable[newY][newX] = true;
            }
        }

        return walkable;
    }
    // Getters
    public Character getCharacter() { return character; }
    public IsometricMap getMap() { return map; }
    public InputController getInputController() { return inputController; }

    public DialogController getDialogController() {
        return dialogController;
    }

    public void setDialogController(DialogController dialogController) {
        this.dialogController = dialogController;
    }
    public MusicController getMusicController() {
        return musicController;
    }
}