package ctu.game.isometric.controller;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.math.Rectangle;
import ctu.game.isometric.IsometricGame;
import ctu.game.isometric.model.entity.Character;
import ctu.game.isometric.model.game.GameState;
import ctu.game.isometric.model.world.IsometricMap;

public class GameController {


    private IsometricGame game;
    private Character character;
    private IsometricMap map;
    private OrthographicCamera camera;
    private InputController inputController;
    private DialogController dialogController; // New field
    private MusicController musicController;
    private MenuController menuController;
    private SettingsMenuController settingsMenuController;
    private MainMenuController mainMenuController;
    private TransitionController transitionController;

    private GameState currentState = GameState.MAIN_MENU;
    private GameState previousState = GameState.MAIN_MENU;

    public GameController(IsometricGame game) {
        this.game = game;
        this.map = new IsometricMap();
        this.character = new Character(0, 0);
        this.inputController = new InputController(this);
        this.dialogController = new DialogController(this);
        this.musicController = new MusicController();

        this.menuController = new MenuController(this);
        this.settingsMenuController = new SettingsMenuController(this);
        this.mainMenuController = new MainMenuController(this);
        this.transitionController = new TransitionController();


        this.musicController.initialize();
        this.musicController.playMusicForState(GameState.MAIN_MENU);
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
                menuController.update(delta);
                break;
            case MAIN_MENU:
                mainMenuController.update(delta);
                break;

            case SETTINGS:
//                settingsMenuController.update(delta);
                break;
            case CUTSCENE:
                character.update(delta); // Keep character animations running
                break;

        }

    }

    public TransitionController getTransitionController() {
        return transitionController;
    }

    public void setTransitionController(TransitionController transitionController) {
        this.transitionController = transitionController;
    }

    public GameState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(GameState currentState) {
        this.currentState = currentState;
    }

    public void setState(GameState newState) {
        if (currentState == newState) return;

        final GameState oldState = currentState;

        if(newState != GameState.SETTINGS){
            previousState = oldState;
        }

        transitionController.startFadeOut(() -> {
            // This code runs when fade out is complete
            currentState = newState;  // Only set state at completion of transition
            musicController.playMusicForState(newState);
            onStateChanged(oldState, newState);
        });
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

    public float[] toIsometric(float x, float y) {
        float isoX = (x - y) * (map.getTileWidth() / 2.0f);
        float isoY = (y + x) * (map.getTileHeight() / 2.0f);
        return new float[]{isoX, isoY};
    }


    private void checkPositionEvents(float x, float y) {
        // Convert player's position from grid to isometric pixel coordinates
        float[] playerIsoPos = toIsometric(x, y);
        float playerIsoX = playerIsoPos[0];
        float playerIsoY = playerIsoPos[1];

        // Log the player's isometric position
        System.out.println("Player Isometric Position: (" + playerIsoX + ", " + playerIsoY + ")");

        // Get the object layer from the map
        MapLayer objectLayer = map.getTiledMap().getLayers().get("object_layer");
        if (objectLayer == null) {
            System.out.println("No object layer found.");
            return; // No object layer found
        }

        // Iterate through all objects in the layer
        for (MapObject object : objectLayer.getObjects()) {
            if (object instanceof RectangleMapObject) {
                RectangleMapObject rectObject = (RectangleMapObject) object;
                Rectangle rect = rectObject.getRectangle();

                // Convert object's position to isometric coordinates
                float[] objectIsoPos = toIsometric(rect.x / map.getTileWidth(), rect.y / map.getTileHeight());
                float objectIsoX = objectIsoPos[0];
                float objectIsoY = objectIsoPos[1];

                // Adjust object's width and height to isometric space
                float isoWidth = rect.width * (map.getTileWidth()) / map.getTileWidth();
                float isoHeight = rect.height * (map.getTileHeight()) / map.getTileHeight();

                // Log the object's isometric position and dimensions
                System.out.println("Object Isometric Position: (" + objectIsoX + ", " + objectIsoY + "), Width: " + isoWidth + ", Height: " + isoHeight);

                // Check if the player's isometric position is within the object's rectangle
                if (playerIsoX >= objectIsoX && playerIsoX <= objectIsoX + isoWidth &&
                        playerIsoY >= objectIsoY && playerIsoY <= objectIsoY + isoHeight) {
                    System.out.println("Player intersects with object!");
                    // Trigger events based on object properties
                    String type = object.getProperties().get("type", String.class);
                    if ("story".equals(type)) {
                        setState(GameState.DIALOG);
                        dialogController.startDialog("chapter_01", "scene_01");
                    }
                } else {
                    System.out.println("Player does not intersect with object.");
                }
            }
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

    public MenuController getMenuController() {
        return menuController;
    }

    public void setPreviousState(GameState previousState) {
        this.previousState = previousState;
    }

    public void setMenuController(MenuController menuController) {
        this.menuController = menuController;
    }
    public SettingsMenuController getSettingsMenuController() {
        return settingsMenuController;
    }
    public MainMenuController getMainMenuController() {
        return mainMenuController;
    }
    public void cycleTransitionType() {
        TransitionController.TransitionType[] types = TransitionController.TransitionType.values();
        int nextIndex = (transitionController.getCurrentType().ordinal() + 1) % types.length;
        transitionController.setTransitionType(types[nextIndex]);
        System.out.println("Changed transition to: " + types[nextIndex]);
    }
    public void dispose() {
        transitionController.dispose();
        musicController.dispose();
        menuController.dispose();
        settingsMenuController.dispose();
        mainMenuController.dispose();
    }

    public OrthographicCamera getCamera() {
        return camera;
    }

    public void setCamera(OrthographicCamera camera) {
        this.camera = camera;
    }
}