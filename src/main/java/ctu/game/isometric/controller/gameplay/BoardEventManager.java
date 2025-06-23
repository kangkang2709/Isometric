package ctu.game.isometric.controller.gameplay;

import ctu.game.isometric.controller.EventManager;
import ctu.game.isometric.controller.GameController;
import ctu.game.isometric.model.entity.Enemy;
import ctu.game.isometric.model.game.Items;
import ctu.game.isometric.model.game.WordScrambleGame;
import ctu.game.isometric.model.world.IsometricMap;
import ctu.game.isometric.model.world.MapEvent;
import ctu.game.isometric.util.EnemyLoader;
import ctu.game.isometric.util.ItemLoader;

import java.util.*;

public class BoardEventManager {
    private GameController gameController;
    private List<Items> items;
    private List<Enemy> enemies;
    private String mapName = "board";
    private EventManager eventManager;
    private IsometricMap map;
    private boolean[][] isBoard;
    private boolean isFirstPosition = true;
    private final int START_X = 10;
    private final int START_Y = 0;
    private Random random = new Random();
    private List<String> listUsedId = new ArrayList<>();

    int currentRun = 0;
    private WordScrambleGame wordScrambleGame;

    public Map<Integer, int[][]> defaultEventsForRun = Map.of(
            0, new int[][]{{11, 4}, {11, 5}},
            2, new int[][]{{11, 0}, {11, 1}},
            4, new int[][]{{11, 1}, {11, 2}},
            6, new int[][]{{11, 2}, {11, 3}},
            8, new int[][]{{11, 3}, {11, 4}},
            10, new int[][]{{11, 4}, {11, 5}}
    );


    public BoardEventManager(GameController gameController) {
        this.gameController = gameController;
        this.eventManager = gameController.getEventManagerMap().get("board");
        this.map = gameController.getMapList().get("board");
        this.isBoard = this.map.getWalkableCache();

        this.enemies = new ArrayList<>();
        this.items = new ArrayList<>();

        this.wordScrambleGame = new WordScrambleGame(gameController);

        loadItems();
        loadEnemies();
        randomBoardEveryRun();

    }
    private void placeWordScrambleEvents() {
        int numWordEvents = random.nextInt(2) + 1; // 1-2 word events
        for (int i = 0; i < numWordEvents; i++) {
            int x, y;
            do {
                x = random.nextInt(21);
                y = random.nextInt(21);
            } while (!isValidPosition(x, y) || (x == START_X && y == START_Y));

            String eventId = "word_scramble_" + x + "_" + y;
            listUsedId.add(eventId);
            MapEvent wordEvent = new MapEvent(eventId, "word_scramble", x, y, "Word Challenge", "0");
            eventManager.addEvent(wordEvent);
        }
    }

    public void randomBoardEveryRun() {
        this.currentRun = gameController.getCharacter().getRun();
        resetBoard();
        placeRandomItems();
        placeRandomEnemies();
        placeRandomPlates();

        placeWordScrambleEvents();

        placedDefaultEvent();
    }


    public void loadItems() {
        this.items = ItemLoader.getAllItemsWithout("N/A", "quest");
    }

    public void loadEnemies() {
        this.enemies = EnemyLoader.getAllEnemies();
        gameController.getAssetManager().loadAllEnemy(this.enemies);
        System.out.println("Enemies loaded: " + enemies.size());
    }

    public void addPlates(int x, int y, String effectType, int targetX, int targetY) {
        this.map.getPuzzle().addPlate(x, y, effectType, targetX, targetY);
    }

    public void resetBoard() {
        this.map.getPuzzle().clear();
        this.eventManager.resetEvents(this.map);
        this.listUsedId.clear(); // Clear used IDs when resetting board
    }

    public void checkBoardPlayerPosition(int playerX, int playerY) {

    }


    public void placedDefaultEvent() {
        String eventId = "new_run_event";
        MapEvent defaultEvent = new MapEvent(eventId, eventId, 9, 0, eventId, "0");

        eventManager.addEvent(defaultEvent);

    }

    private void placeRandomItems() {
        // Place random items on walkable tiles
        int numItems = random.nextInt(5) + 3; // 3-7 items
        for (int i = 0; i < numItems; i++) {
            int x, y;
            do {
                x = random.nextInt(21);
                y = random.nextInt(21);
            } while (!isValidPosition(x, y) || (x == START_X && y == START_Y));

            Items randomItem = items.get(random.nextInt(items.size()));

            String itemId = "item_" + randomItem.getItemName() + "_" + x + "_" + y;

            listUsedId.add(String.valueOf(randomItem.getItemID()));
            MapEvent itemEvent = new MapEvent(itemId, "treasure", x, y, randomItem.getItemName(), String.valueOf(randomItem.getItemID()));
            eventManager.addEvent(itemEvent);
        }
    }

    private void placeRandomEnemies() {
        // Place random enemies on walkable tiles
        int numEnemies = random.nextInt(3) + 2; // 2-4 enemies
        for (int i = 0; i < numEnemies; i++) {
            int x, y;
            do {
                x = random.nextInt(21);
                y = random.nextInt(21);
            } while (!isValidPosition(x, y) || (x == START_X && y == START_Y));

            Enemy randomEnemy = enemies.get(random.nextInt(enemies.size()));
            // Generate unique ID for enemy placement
            String enemyId = "enemy_" + randomEnemy.getEnemyName() + "_" + x + "_" + y;
            listUsedId.add(enemyId);
            eventManager.addEnemyEvent(enemyId, x, y, randomEnemy);
        }
    }

    private void placeRandomPlates() {
//        // Place random pressure plates
//        int numPlates = random.nextInt(3) + 1; // 1-3 plates
//        for (int i = 0; i < numPlates; i++) {
//            int x, y, targetX, targetY;
//            do {
//                x = random.nextInt(21);
//                y = random.nextInt(21);
//            } while (!isValidPosition(x, y) || (x == START_X && y == START_Y));
//
//            do {
//                targetX = random.nextInt(21);
//                targetY = random.nextInt(21);
//            } while (!isValidPosition(targetX, targetY));
//
//            String effectType = "trap";
//            // Generate unique ID for plate placement
//            String plateId = "plate_" + x + "_" + y + "_" + effectType;
//            listUsedId.add(plateId);
//            if (effectType.equals("trap"))
//                AddPlates(x, y, effectType, x, y);
//            else AddPlates(x, y, effectType, targetX, targetY);
//
//        }

        addPlates(11, 0, "trap", 11, 0); // Example plate at (11, 0) with door effect
    }

    public WordScrambleGame getWordScrambleGame() {
        return wordScrambleGame;
    }
    private boolean isValidPosition(int x, int y) {
        // Check if position is within bounds and walkable
        if (!(x >= 0 && x < 21 && y >= 0 && y < 21 && isBoard[y][x])) {
            return false;
        }

        // Check if position is (9,0) - the special default event position
        if (x == 9 && y == 0) {
            return false;
        }
        if (x == 10 && y == 0) {
            return false;
        }

        // Check if position is one of the default events for the current run
        if (defaultEventsForRun.containsKey(currentRun)) {
            int[][] positions = defaultEventsForRun.get(currentRun);
            for (int[] position : positions) {
                if (x == position[0] && y == position[1]) {
                    return false;
                }
            }
        }

        // Also check all default event positions across all runs to be extra safe
        for (int[][] positions : defaultEventsForRun.values()) {
            for (int[] position : positions) {
                if (x == position[0] && y == position[1]) {
                    return false;
                }
            }
        }

        return true;
    }
}