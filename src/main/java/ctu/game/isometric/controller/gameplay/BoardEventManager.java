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
    private final int START_X = 10;
    private final int START_Y = 0;
    private Random random = new Random();
    private Set<String> usedPositions = new HashSet<>();

    // Board configuration - Exactly 40 events required
    private final int BOARD_SIZE = 21;
    private final int TOTAL_WALKABLE_TILES = 80;
    private final int EXACT_TOTAL_EVENTS = 40; // Must be exactly 40
    private int totalEventsPlaced = 0;

    // Your specific board path
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

    // Set of valid walkable positions for quick lookup
    private Set<String> walkablePositions = new HashSet<>();

    int currentRun = 0;
    private WordScrambleGame wordScrambleGame;

    public Map<Integer, int[][]> defaultEventsForRun = Map.of(
            0, new int[][]{{11, 4}},
            2, new int[][]{{11, 0}, {11, 1}},
            4, new int[][]{{11, 1}, {11, 2}},
            6, new int[][]{{11, 2}, {11, 3}},
            8, new int[][]{{11, 3}, {11, 4}},
            10, new int[][]{{11, 4}, {11, 5}}
    );

    public BoardEventManager(GameController gameController) {
        try {
            this.gameController = gameController;

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

            this.map = gameController.getMapList().get("board");
            if (this.map == null) {
                throw new IllegalStateException("Map 'board' not found");
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
        walkablePositions.clear();
        for (int[] pos : boardPath) {
            walkablePositions.add(pos[0] + "_" + pos[1]);
        }
        System.out.println("Initialized " + walkablePositions.size() + " walkable positions");
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
                if (x == START_X && y == START_Y) {
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
        try {
            if (defaultEventsForRun.containsKey(currentRun)) {
                int[][] positions = defaultEventsForRun.get(currentRun);
                for (int[] position : positions) {
                    int x = position[0];
                    int y = position[1];
                    String positionKey = x + "_" + y;

                    if (walkablePositions.contains(positionKey) && !usedPositions.contains(positionKey)) {
                        usedPositions.add(positionKey);
                        totalEventsPlaced++;

                        // Create default event for this run
                        MapEvent defaultEvent = new MapEvent("default_run_" + currentRun + "_" + positionKey,
                                "default_event", x, y, "Default Event Run " + currentRun, "0");
                        eventManager.addEvent(defaultEvent);

                        System.out.println("Placed default event for run " + currentRun + " at (" + x + ", " + y + ")");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error placing default events for run: " + e.getMessage());
        }
    }

    private void distributeRemainingEvents(int remainingEvents) {
        try {
            // Define distribution percentages for remaining events
            int itemEvents = (int) Math.round(remainingEvents * 0.3);      // 40% items
            int enemyEvents = (int) Math.round(remainingEvents * 0.4);     // 30% enemies
            int wordEvents = (int) Math.round(remainingEvents * 0.2);      // 20% word scramble
            int quizEvents = 0;
            int multiQuizEvents = 0;

            // Add quiz events if run >= 3
            if (currentRun >= 0) {
                quizEvents = Math.min(3, remainingEvents / 10);           // Small number of quiz events
                multiQuizEvents = Math.min(2, remainingEvents / 15);      // Small number of multi-quiz events

                // Adjust other events to accommodate quiz events
                itemEvents = Math.max(0, itemEvents - quizEvents - multiQuizEvents);
            }

            // Ensure we don't exceed remaining events
            int totalDistributed = itemEvents + enemyEvents + wordEvents + quizEvents + multiQuizEvents;
            if (totalDistributed > remainingEvents) {
                // Reduce item events if we're over
                itemEvents = Math.max(0, itemEvents - (totalDistributed - remainingEvents));
            } else if (totalDistributed < remainingEvents) {
                // Add remaining to items
                itemEvents += (remainingEvents - totalDistributed);
            }

            System.out.println("Distributing " + remainingEvents + " events: Items=" + itemEvents +
                    ", Enemies=" + enemyEvents + ", Words=" + wordEvents +
                    ", Quiz=" + quizEvents + ", MultiQuiz=" + multiQuizEvents);

            // Place events in order
            placeSpecificEvents("treasure", itemEvents, "item");
            placeSpecificEvents("enemy", enemyEvents, "enemy");
            placeSpecificEvents("word_scramble", wordEvents, "word");

            if (currentRun >= 0) {
                placeSpecificEvents("quiz", quizEvents, "quiz");
                placeSpecificEvents("mulquiz", multiQuizEvents, "multiquiz");
            }


            // Fill any remaining spots if we're still under 40
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
                        System.out.println("Placing quiz event at " + positionKey);
                        eventManager.addEvent(quizEvent);
                        break;
                    case "mulquiz":
                        MapEvent multiQuizEvent = new MapEvent(positionKey, "mulquiz", pos[0], pos[1], "Multiple Quiz Challenge", "0");
                        eventManager.addEvent(multiQuizEvent);
                        break;
                    case "plate":
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
            int x = 9, y = 0;
            String positionKey = x + "_" + y;

            if (!usedPositions.contains(positionKey)) {
                usedPositions.add(positionKey);
                totalEventsPlaced++;

                MapEvent defaultEvent = new MapEvent("new_run_event", "new_run_event", x, y, "new_run_event", "0");
                eventManager.addEvent(defaultEvent);
            }
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