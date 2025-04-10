package ctu.game.isometric.controller;

import ctu.game.isometric.IsometricGame;
import ctu.game.isometric.model.entity.Character;
import ctu.game.isometric.model.world.IsometricMap;

public class GameController {


    private IsometricGame game;
    private Character character;
    private IsometricMap map;
    private InputController inputController;

    public GameController(IsometricGame game) {
        this.game = game;
        this.map = new IsometricMap();
        this.character = new Character("characters/player.png", 2, 2);
        this.inputController = new InputController(this);

    }

    public void update(float delta) {
        // Update input controller
        inputController.update(delta);

        // Update character animation
        character.update(delta);

        // Reset character movement flag
//        character.setMoving(false);
    }

    public boolean canMove(int dx, int dy) {
        int newX = (int) (character.getGridX() + dx);
        int newY = (int) (character.getGridY() + dy);

        // Check map boundaries with null checks
        if (map == null || map.getMapData() == null) return false;

        int[][] mapData = map.getMapData();
        if (mapData.length == 0) return false;

        // Check map boundaries
        if (newX < 0 || newY < 0) return false;
        if (newY >= mapData.length) return false;
        if (newX >= mapData[0].length) return false;

        return mapData[newY][newX] != 0;
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
        if (x == 3 && y == 3) {
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
}