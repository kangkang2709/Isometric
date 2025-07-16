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
    private Random random = new Random();
    private Set<String> usedPositions = new HashSet<>();

    private final int TOTAL_WALKABLE_TILES = 80;
    private final int EXACT_TOTAL_EVENTS = 30; // Must be exactly 40
    private int totalEventsPlaced = 0;

    // Your specific board path
    // Set of valid walkable positions for quick lookup
    private Set<String> walkablePositions = new HashSet<>();

    int currentRun = 0;
    private WordScrambleGame wordScrambleGame;


    public BoardEventManager(GameController gameController) {
        try {
            this.gameController = gameController;

            this.map = gameController.getMapList().get("board");
            if (this.map == null) {
                throw new IllegalStateException("Map 'board' not found");
            }

            // Initialize walkable positions from your board path
            initializeWalkablePositions();
            // Null safety checks
            if (gameController == null) {
                throw new IllegalStateException("GameController cannot be null");
            }

            this.eventManager = gameController.getEventManagerMap().get("board");
            if (this.eventManager == null) {
                throw new IllegalStateException("Event manager for 'board' not found");
            }


            this.enemies = new ArrayList<>();
            this.items = new ArrayList<>();

            this.wordScrambleGame = new WordScrambleGame(gameController);

            loadItems();
            loadEnemies();

            randomBoardEveryRun();

        } catch (Exception e) {
            System.err.println("Error initializing BoardEventManager: " + e.getMessage());
            e.printStackTrace();
            initializeSafeDefaults();
        }
    }

    private void initializeWalkablePositions() {
        boolean[][] walkableCache = map.getWalkableCache();
        for (int y = 0; y < walkableCache.length; y++) {
            for (int x = 0; x < walkableCache[y].length; x++) {
                if (walkableCache[y][x]) {
                    walkablePositions.add(x + "_" + y);
                }
            }
        }
    }

    private void initializeSafeDefaults() {
        this.enemies = new ArrayList<>();
        this.items = new ArrayList<>();
        this.usedPositions = new HashSet<>();
        this.totalEventsPlaced = 0;
        initializeWalkablePositions();
    }

    private int[] getRandomWalkablePosition() {
        List<String> availablePositions = new ArrayList<>();
        for (String pos : walkablePositions) {
            if (!usedPositions.contains(pos)) {
                String[] coords = pos.split("_");
                int x = Integer.parseInt(coords[0]);
                int y = Integer.parseInt(coords[1]);

                // Skip start position
                if (x == map.getStartX() && y == map.getStartY()) {
                    continue;
                }
                if (x == map.getEndX() && y == map.getEndY()) {
                    continue;
                }

                availablePositions.add(pos);
            }
        }

        if (availablePositions.isEmpty()) {
            return null;
        }

        String randomPos = availablePositions.get(random.nextInt(availablePositions.size()));
        String[] coords = randomPos.split("_");
        return new int[]{Integer.parseInt(coords[0]), Integer.parseInt(coords[1])};
    }

    public void randomBoardEveryRun() {
        initializeWalkablePositions();

        try {
            this.currentRun = gameController.getCharacter().getRun();
            resetBoard();

            // Step 1: Place default events for this run (these are mandatory)
            placeDefaultEventsForRun();

            // Step 2: Place the new_run_event (mandatory)
            placedDefaultEvent();

            // Step 3: Calculate remaining events needed
            int remainingEvents = EXACT_TOTAL_EVENTS - totalEventsPlaced;

            // Step 4: Distribute remaining events across different types
            distributeRemainingEvents(remainingEvents);

            System.out.println("Total events placed: " + totalEventsPlaced + "/" + EXACT_TOTAL_EVENTS +
                    " (" + String.format("%.1f", (totalEventsPlaced * 100.0 / TOTAL_WALKABLE_TILES)) + "% coverage)");

            // Verify we have exactly 40 events
            if (totalEventsPlaced != EXACT_TOTAL_EVENTS) {
                System.err.println("ERROR: Expected exactly " + EXACT_TOTAL_EVENTS + " events, but placed " + totalEventsPlaced);
            }

        } catch (Exception e) {
            System.err.println("Error in randomBoardEveryRun: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void placeDefaultEventsForRun() {

    }

    private void distributeRemainingEvents(int remainingEvents) {
        try {
            int quizEvents = 0;
            int multiQuizEvents = 0;
            int wordEvents = 0;

            // Tính số lượng quiz/word tối đa có thể, nhưng không quá 3 mỗi loại
            int trio = Math.min(remainingEvents / 3, 3); // mỗi loại 1/3
            quizEvents = trio;
            multiQuizEvents = trio;
            wordEvents = trio;

            int reserved = quizEvents + multiQuizEvents + wordEvents;
            int available = remainingEvents - reserved;

            // Giới hạn item nhỏ hơn hoặc bằng quiz để ưu tiên học tập
            int itemEvents = Math.min((int) Math.round(available * 0.1), quizEvents);
            available -= itemEvents;

            // Chia phần còn lại cho enemy và trap
            int enemyEvents = (int) Math.round(available * 0.6);
            int trapEvents = available - enemyEvents;

            // Kiểm tra tổng, điều chỉnh nếu lệch
            int totalDistributed = itemEvents + enemyEvents + trapEvents + quizEvents + multiQuizEvents + wordEvents;
            int diff = remainingEvents - totalDistributed;

            if (diff > 0) {
                enemyEvents += diff; // đổ thêm vào enemy nếu còn dư
            } else if (diff < 0 && itemEvents > 0) {
                int reduce = Math.min(itemEvents, -diff);
                itemEvents -= reduce;
            }

            System.out.println("Distributing " + remainingEvents + " events: Items=" + itemEvents +
                    ", Enemies=" + enemyEvents + ", Words=" + wordEvents + ", Traps=" + trapEvents +
                    ", Quiz=" + quizEvents + ", MultiQuiz=" + multiQuizEvents);

            // Place events
            placeSpecificEvents("trap", trapEvents, "plate");
            placeSpecificEvents("enemy", enemyEvents, "enemy");
            placeSpecificEvents("word_scramble", wordEvents, "word");
            placeSpecificEvents("quiz", quizEvents, "quiz");
            placeSpecificEvents("mulquiz", multiQuizEvents, "multiquiz");

            fillToExactTotal();

        } catch (Exception e) {
            System.err.println("Error distributing remaining events: " + e.getMessage());
        }
    }


    private void placeSpecificEvents(String eventType, int count, String displayType) {
        try {
            for (int i = 0; i < count && totalEventsPlaced < EXACT_TOTAL_EVENTS; i++) {
                int[] pos = getRandomWalkablePosition();
                if (pos == null) break;

                String positionKey = pos[0] + "_" + pos[1];
                usedPositions.add(positionKey);
                totalEventsPlaced++;

                switch (eventType) {
                    case "treasure":
                        if (!items.isEmpty()) {
                            Items randomItem = items.get(random.nextInt(items.size()));
                            MapEvent itemEvent = new MapEvent(positionKey, "treasure", pos[0], pos[1],
                                    randomItem.getItemName(), String.valueOf(randomItem.getItemID()));
                            eventManager.addEvent(itemEvent);
                        }
                        break;
                    case "enemy":
                        if (!enemies.isEmpty()) {
                            Enemy randomEnemy = enemies.get(random.nextInt(enemies.size()));
                            eventManager.addEnemyEvent(positionKey, pos[0], pos[1], randomEnemy);
                        }
                        break;
                    case "word_scramble":
                        MapEvent wordEvent = new MapEvent(positionKey, "word_scramble", pos[0], pos[1], "Word Challenge", "0");
                        eventManager.addEvent(wordEvent);
                        break;
                    case "quiz":
                        MapEvent quizEvent = new MapEvent(positionKey, "quiz", pos[0], pos[1], "Quiz Challenge", "0");
                        eventManager.addEvent(quizEvent);
                        break;
                    case "mulquiz":
                        MapEvent multiQuizEvent = new MapEvent(positionKey, "mulquiz", pos[0], pos[1], "Multiple Quiz Challenge", "0");
                        eventManager.addEvent(multiQuizEvent);
                        break;
                    case "trap":
                        addPlates(pos[0], pos[1], "trap", pos[0], pos[1]);
                        break;
                }
            }
        } catch (Exception e) {
            System.err.println("Error placing " + displayType + " events: " + e.getMessage());
        }
    }

    private void fillToExactTotal() {
        try {
            // Fill remaining spots with random events until we reach exactly 40
            while (totalEventsPlaced < EXACT_TOTAL_EVENTS) {
                int[] pos = getRandomWalkablePosition();
                if (pos == null) {
                    System.err.println("Warning: No more valid positions available, but still need " +
                            (EXACT_TOTAL_EVENTS - totalEventsPlaced) + " more events");
                    break;
                }

                String positionKey = pos[0] + "_" + pos[1];
                usedPositions.add(positionKey);
                totalEventsPlaced++;

                // Randomly choose event type for remaining spots
                int eventType = random.nextInt(3); // 0=item, 1=enemy, 2=word

                switch (eventType) {
                    case 0: // Item
                        if (!items.isEmpty()) {
                            Items randomItem = items.get(random.nextInt(items.size()));
                            MapEvent itemEvent = new MapEvent(positionKey, "treasure", pos[0], pos[1],
                                    randomItem.getItemName(), String.valueOf(randomItem.getItemID()));
                            eventManager.addEvent(itemEvent);
                        }
                        break;
                    case 1: // Enemy
                        if (!enemies.isEmpty()) {
                            Enemy randomEnemy = enemies.get(random.nextInt(enemies.size()));
                            eventManager.addEnemyEvent(positionKey, pos[0], pos[1], randomEnemy);
                        }
                        break;
                    case 2: // Word scramble
                        MapEvent wordEvent = new MapEvent(positionKey, "word_scramble", pos[0], pos[1], "Word Challenge", "0");
                        eventManager.addEvent(wordEvent);
                        break;
                }
            }
        } catch (Exception e) {
            System.err.println("Error filling to exact total: " + e.getMessage());
        }
    }

    public void loadItems() {
        try {
            this.items = ItemLoader.getAllItemsWithout("N/A", "quest");
            if (this.items == null) {
                this.items = new ArrayList<>();
            }
            System.out.println("Items loaded: " + items.size());
        } catch (Exception e) {
            System.err.println("Error loading items: " + e.getMessage());
            this.items = new ArrayList<>();
        }
    }

    public void loadEnemies() {
        try {
            this.enemies = EnemyLoader.getAllEnemies();
            if (this.enemies == null) {
                this.enemies = new ArrayList<>();
            }
            if (!enemies.isEmpty()) {
                gameController.getAssetManager().loadAllEnemy(this.enemies);
            }
            System.out.println("Enemies loaded: " + enemies.size());
        } catch (Exception e) {
            System.err.println("Error loading enemies: " + e.getMessage());
            this.enemies = new ArrayList<>();
        }
    }

    public void addPlates(int x, int y, String effectType, int targetX, int targetY) {
        try {
            this.map.getPuzzle().addPlate(x, y, effectType, targetX, targetY);
        } catch (Exception e) {
            System.err.println("Error adding plate: " + e.getMessage());
        }
    }

    public void resetBoard() {
        try {
            this.map.getPuzzle().clear();
            this.eventManager.resetEvents(this.map);
            this.usedPositions.clear();
            this.totalEventsPlaced = 0;
        } catch (Exception e) {
            System.err.println("Error resetting board: " + e.getMessage());
        }
    }

    public void placedDefaultEvent() {
        try {
            int x = this.map.getEndX(), y = this.map.getEndY();
            String positionKey = x + "_" + y;

            if (!usedPositions.contains(positionKey)) {
                usedPositions.add(positionKey);
                totalEventsPlaced++;

                MapEvent defaultEvent = new MapEvent("new_run_event", "new_run_event", x, y, "new_run_event", "0");
                eventManager.addEvent(defaultEvent);
            }

            for (int[] pos : map.getMaze().layers.get("fake")) {
                String fakePositionKey = pos[0] + "_" + pos[1];
                if (!usedPositions.contains(fakePositionKey)) {
                    usedPositions.add(fakePositionKey);
//                    totalEventsPlaced++;

                    MapEvent fakeEvent = new MapEvent(fakePositionKey, "dungeon", pos[0], pos[1], "Fake Event", "0");
                    eventManager.addEvent(fakeEvent);
                }

            }

            for (int[] pos : map.getMaze().layers.get("chest")) {
                String itemsPositionKey = pos[0] + "_" + pos[1];
                if (!usedPositions.contains(itemsPositionKey)) {
                    usedPositions.add(itemsPositionKey);
                    totalEventsPlaced++;

                    System.out.println("Placing treasure at: " + itemsPositionKey);
                    Items randomItem = items.get(random.nextInt(items.size()));
                    MapEvent itemEvent = new MapEvent(itemsPositionKey, "treasure", pos[0], pos[1],
                            randomItem.getItemName(), String.valueOf(randomItem.getItemID()));
                    eventManager.addEvent(itemEvent);
                }

            }
            for (int[] pos : map.getMaze().layers.get("enemy")) {
                String enemyPositionKey = pos[0] + "_" + pos[1];
                if (!usedPositions.contains(enemyPositionKey)) {
                    usedPositions.add(enemyPositionKey);
                    totalEventsPlaced++;

                    Enemy randomEnemy = enemies.get(random.nextInt(enemies.size()));
                    eventManager.addEnemyEvent(enemyPositionKey, pos[0], pos[1], randomEnemy);
                }

            }
            System.out.println("Size enemy: " + map.getMaze().layers.get("enemy").length);


        } catch (Exception e) {
            System.err.println("Error placing default event: " + e.getMessage());
        }
    }

    public WordScrambleGame getWordScrambleGame() {
        return wordScrambleGame;
    }

    public double getEventCoveragePercentage() {
        return (totalEventsPlaced * 100.0) / TOTAL_WALKABLE_TILES;
    }

    public int getTotalEventsPlaced() {
        return totalEventsPlaced;
    }

    public int getExactTotalEvents() {
        return EXACT_TOTAL_EVENTS;
    }

    public void checkBoardPlayerPosition(int playerX, int playerY) {

    }
}