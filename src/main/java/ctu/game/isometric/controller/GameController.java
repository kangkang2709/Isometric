package ctu.game.isometric.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.MapRenderer;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Rectangle;
import ctu.game.isometric.IsometricGame;
import ctu.game.isometric.controller.cutscene.CutsceneController;
import ctu.game.isometric.controller.gameplay.GameplayController;
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
    private GameplayController gameplayController;
    private CharacterCreationController characterCreationController;
    private GameState currentState = GameState.MAIN_MENU;
    private GameState previousState = GameState.MAIN_MENU;
    private CutsceneController cutsceneController;

    boolean isCreated = false;

    public GameController(IsometricGame game) {
        this.game = game;
        this.map = new IsometricMap();
        this.character = new Character(0, 0);
        this.inputController = new InputController(this);
        this.dialogController = new DialogController(this);
        this.musicController = new MusicController();
        this.gameplayController = new GameplayController(this);
        characterCreationController = new CharacterCreationController(this);
        this.menuController = new MenuController(this);
        this.settingsMenuController = new SettingsMenuController(this);
        this.mainMenuController = new MainMenuController(this);
        this.transitionController = new TransitionController();
        this.cutsceneController = new CutsceneController(this);

        this.musicController.initialize();
        this.musicController.playMusicForState(GameState.MAIN_MENU);
    }

    public void update(float delta) {


        switch (currentState) {
            case EXPLORING:
                inputController.updateCooldown(delta);
                character.update(delta);
                break;
            case CHARACTER_CREATION:
                characterCreationController.update(delta);
                break;
            case DIALOG:
                // Only update dialog controller when in dialog state
                // dialogController.update(delta);
                break;
            case GAMEPLAY:
                gameplayController.update(delta);
                break;
            case MENU:
                menuController.update(delta);
                break;
            case MAIN_MENU:
                mainMenuController.update(delta);
                break;

            case SETTINGS:
                settingsMenuController.update(delta);
                break;
            case CUTSCENE:
                if (character.getFlags() != null && !character.getFlags().isEmpty()) {
                    String flags = character.getFlags().get(0);
                    if (flags != null && flags == "intro" && getTransitionController().isTransitioning() == false) {
                        startCutscene(flags);
                        character.getFlags().remove(0);
                    }
                }
                cutsceneController.update(delta);
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

        transitionController.startLoadingScreen(() -> {
            // This code executes after the fade out, during loading
            currentState = newState;

            // Reset controllers when entering specific states
            if (currentState == GameState.CHARACTER_CREATION) {
                characterCreationController.reset();
            }

            // Update music for the new state
            musicController.playMusicForState(newState);
        });

    }

    private void onStateChanged(GameState oldState, GameState newState) {
        // Notify relevant subsystems about state change
    }

    public void startCutscene(String cutsceneName) {
        setPreviousState(currentState);
        setState(GameState.CUTSCENE);
        cutsceneController.loadCutscene(cutsceneName);
    }

    public CutsceneController getCutsceneController() {
        return cutsceneController;
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

    public boolean isCreated() {
        return isCreated;
    }

    public void setCreated(boolean created) {
        this.isCreated = created;
        if (created && characterCreationController != null && currentState != GameState.MAIN_MENU) {
            setState(GameState.CUTSCENE);
        }
    }
    // Add to GameController.java
    // In GameController.java, enhance resetGame method
    // In GameController.java - update the resetGame method
    public void resetGame() {

        currentState = GameState.MAIN_MENU;
        previousState = GameState.MAIN_MENU;
        // Reset character with a new instance
        character = new Character(0, 0);

        // Reset map with a new instance
        this.map = new IsometricMap();
        // Force recreation of renderers by setting isCreated flag
        isCreated = false;

        // Reset controllers to initial state - make sure to reset character creation controller
        if (characterCreationController != null) {
            characterCreationController.dispose();
            characterCreationController = new CharacterCreationController(this);
        }

        if (cutsceneController != null) {
            cutsceneController.dispose();
            cutsceneController = new CutsceneController(this);
        }

        if (dialogController != null) {
            dialogController = new DialogController(this);
        }

        if (gameplayController != null) {
//            getCutsceneController().dispose();
            gameplayController = new GameplayController(this);
        }

        // Reset to main menu state


        // Reset music
        musicController.playMusicForState(GameState.MAIN_MENU);
    }



    private String currentEventType;
    private int currentEventX;
    private int currentEventY;
    private boolean hasActiveEvent = false;
    private MapProperties properties;
    private void checkPositionEvents(float x, float y) {
        int gridX = (int) x;
        int gridY = (int) y;

        hasActiveEvent = false; // Reset event flag
        // Check for object-based events (NPCs, triggers, etc.)
        MapLayer objectLayer = map.getTiledMap().getLayers().get("object");
        if (objectLayer != null) {
            for (MapObject object : objectLayer.getObjects()) {
                if (object instanceof RectangleMapObject) {
                    Rectangle rect = ((RectangleMapObject) object).getRectangle();
                    int objGridX = (int) (rect.x / map.getTileWidth())+2;
                    int objGridY = (int) (rect.y / map.getTileHeight())-2;
                    System.out.println("Object position: " + objGridX + "," + objGridY);
                    if (objGridX == gridX && objGridY == gridY) {
                        properties = object.getProperties();
                         if (properties.containsKey("event")){
                            currentEventType = properties.get("event", String.class);
                            currentEventX = gridX;
                            currentEventY = gridY;
                            hasActiveEvent = true;
                        }
                        else properties = null;
//                        handleEventProperties(object.getProperties(), gridX, gridY);
                    }
                }
            }
        }
    }

    public MapProperties getProperties() {
        return properties;
    }

    public void handleEventProperties(MapProperties properties, String event) {
            switch (event) {
                case "battle":
                    String enemyId = (properties != null && properties.containsKey("enemy")) ?
                            properties.get("enemy", String.class) : "enemy_01";
                    int health;
                    Object healthObj = properties.get("health");
                    if (healthObj instanceof String) {
                        health = Integer.parseInt((String) healthObj);
                    } else {
                        health = properties.get("health", Integer.class);
                    }

                    setState(GameState.GAMEPLAY);
                    gameplayController.activate();
                    gameplayController.startCombat(enemyId, health);
                    break;

                case "cutscene":
                    if (properties.containsKey("cutsceneId")) {
                        String cutsceneId = properties.get("cutsceneId", String.class);
                        startCutscene(cutsceneId);
                    }
                    break;


            }

    }


    public boolean hasActiveEvent() {
        return hasActiveEvent && currentState == GameState.EXPLORING;
    }

    public String getCurrentEventType() {
        return currentEventType;
    }

    public int getCurrentEventX() {
        return currentEventX;
    }

    public int getCurrentEventY() {
        return currentEventY;
    }

//    public boolean[][] getWalkableTiles() {
//        // First check if map is valid
//        if (map == null || map.getMapData() == null || map.getMapData().length == 0) {
//            return new boolean[0][0];
//        }
//
//        int height = map.getMapData().length;
//        int width = map.getMapData()[0].length;
//        boolean[][] walkable = new boolean[height][width];
//
//        // Initialize all tiles as not walkable
//        for (int y = 0; y < height; y++) {
//            for (int x = 0; x < width; x++) {
//                walkable[y][x] = false;
//            }
//        }
//
//        // Mark only adjacent walkable tiles
//        int charX = (int) character.getGridX();
//        int charY = (int) character.getGridY();
//
//        // Check adjacent tiles (up, down, left, right)
//        int[][] directions = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
//        for (int[] dir : directions) {
//            int newX = charX + dir[0];
//            int newY = charY + dir[1];
//
//            // First validate that this position is inside the map
//            if (newX < 0 || newY < 0 || newX >= width || newY >= height) {
//                continue; // Skip this direction if it's outside the map
//            }
//
//            // Then check if we can move there
//            if (canMove(dir[0], dir[1])) {
//                walkable[newY][newX] = true;
//            }
//        }
//
//        return walkable;
//    }
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

    public GameplayController getGameplayController() {
        return gameplayController;
    }

    public void setGameplayController(GameplayController gameplayController) {
        this.gameplayController = gameplayController;
    }

    public CharacterCreationController getCharacterCreationController() {
        return characterCreationController;
    }

    public void setCharacterCreationController(CharacterCreationController characterCreationController) {
        this.characterCreationController = characterCreationController;
    }

    public void setCharacter(Character character) {
        this.character = character;
    }
}