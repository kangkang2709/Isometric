package ctu.game.isometric.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import ctu.game.isometric.model.entity.Enemy;
import ctu.game.isometric.model.game.Items;
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


    public BoardEventManager(GameController gameController) {
        this.gameController = gameController;
        this.eventManager = gameController.getEventManagerMap().get("board");
        this.map = gameController.getMapList().get("board");
        this.isBoard = this.map.getWalkableCache();
        this.enemies = new ArrayList<>();
        this.items = new ArrayList<>();

        loadItems();
        loadEnemies();
        randomBoardEveryRun();

    }


    public void randomBoardEveryRun() {
        resetBoard();
        placeRandomItems();
        placeRandomEnemies();
        placeRandomPlates();

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

        if (playerX == START_X && playerY == START_Y) {

            System.out.println(isFirstPosition);
            // Player has completed a turn and returned to start
            randomBoardEveryRun();
            gameController.getDialogController().showSimpleMessage("You have completed the run. The board has been randomized again.");
            isFirstPosition = true; // Reset flag after completing a turn
        }
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


    private boolean isValidPosition(int x, int y) {
        // Check if the position is within bounds and walkable
        return x >= 0 && x < 21 && y >= 0 && y < 21 && isBoard[y][x];
    }
}