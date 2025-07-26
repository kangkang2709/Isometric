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

    // Constants for minimum distance between events
    private static final int MIN_DISTANCE_BETWEEN_EVENTS = 2;

    // Event limits per type based on run level
    private static final int MIN_QUIZ_EVENTS = 3;
    private static final int MAX_QUIZ_EVENTS = 5;
    private static final int MIN_WORD_EVENTS = 3;
    private static final int MAX_WORD_EVENTS = 5;
    private static final int MIN_MULQUIZ_EVENTS = 2;
    private static final int MAX_MULQUIZ_EVENTS = 4;
    private static final int MIN_BATTLE_EVENTS = 5;
    private static final int MAX_BATTLE_EVENTS = 8;

    private int totalEventsPlaced = 0;
    private int targetEventCount = 0;
    private int currentRunLevel = 1;

    // Cache for walkable positions
    private List<int[]> walkablePositions = new ArrayList<>();
    private List<int[]> shuffledPositions = new ArrayList<>();

    private WordScrambleGame wordScrambleGame;

    public BoardEventManager(GameController gameController) {
        try {
            this.gameController = gameController;
            this.map = gameController.getMapList().get("board");

            if (this.map == null) {
                throw new IllegalStateException("Map 'board' not found");
            }

            this.eventManager = gameController.getEventManagerMap().get("board");
            if (this.eventManager == null) {
                throw new IllegalStateException("Event manager for 'board' not found");
            }

            this.enemies = new ArrayList<>();
            this.items = new ArrayList<>();
            this.wordScrambleGame = new WordScrambleGame(gameController);

            // Initialize walkable positions cache
            initializeWalkablePositions();

            loadItems();
            loadEnemies();

            randomBoardEveryRun();

        } catch (Exception e) {
            System.err.println("Lỗi khởi tạo BoardEventManager: " + e.getMessage());
            e.printStackTrace();
            initializeSafeDefaults();
        }
    }

    private void initializeSafeDefaults() {
        this.enemies = new ArrayList<>();
        this.items = new ArrayList<>();
        this.usedPositions = new HashSet<>();
        this.totalEventsPlaced = 0;
        this.walkablePositions = new ArrayList<>();
        this.shuffledPositions = new ArrayList<>();
    }

    /**
     * Thu thập tất cả vị trí có thể đi được trên bản đồ
     */
    private void initializeWalkablePositions() {
        walkablePositions.clear();

        if (map == null) {
            System.err.println("Map is null, không thể khởi tạo vị trí walkable");
            return;
        }

        boolean[][] walkableCache = map.getWalkableCache();
        if (walkableCache == null) {
            System.err.println("Walkable cache is null");
            return;
        }

        for (int y = 0; y < walkableCache.length; y++) {
            for (int x = 0; x < walkableCache[y].length; x++) {
                if (walkableCache[y][x]) {
                    // Loại trừ ô bắt đầu và kết thúc
                    if (!isStartOrEndPosition(x, y)) {
                        walkablePositions.add(new int[]{x, y});
                    }
                }
            }
        }

        System.out.println("Đã khởi tạo " + walkablePositions.size() + " vị trí walkable");
    }

    /**
     * Kiểm tra xem vị trí có phải là ô bắt đầu hoặc kết thúc không
     */
    private boolean isStartOrEndPosition(int x, int y) {
        return (x == map.getStartX() && y == map.getStartY()) ||
                (x == map.getEndX() && y == map.getEndY());
    }

    /**
     * Xáo trộn và chuẩn bị vị trí để đặt event
     */
    private void prepareShuffledPositions() {
        shuffledPositions.clear();
        shuffledPositions.addAll(walkablePositions);
        Collections.shuffle(shuffledPositions, random);
    }

    /**
     * Tìm vị trí hợp lệ tiếp theo với khoảng cách tối thiểu
     */
    private int[] findNextValidPosition() {
        for (Iterator<int[]> iterator = shuffledPositions.iterator(); iterator.hasNext(); ) {
            int[] pos = iterator.next();
            String posKey = pos[0] + "_" + pos[1];

            if (!usedPositions.contains(posKey) && isValidDistanceFromOtherEvents(pos[0], pos[1])) {
                iterator.remove(); // Xóa khỏi danh sách để không dùng lại
                return new int[]{pos[0], pos[1]};
            }
        }
        return null;
    }

    /**
     * Kiểm tra khoảng cách tối thiểu với các event khác
     */
    private boolean isValidDistanceFromOtherEvents(int x, int y) {
        for (String usedPos : usedPositions) {
            String[] coords = usedPos.split("_");
            int usedX = Integer.parseInt(coords[0]);
            int usedY = Integer.parseInt(coords[1]);

            double distance = Math.sqrt(Math.pow(x - usedX, 2) + Math.pow(y - usedY, 2));
            if (distance < MIN_DISTANCE_BETWEEN_EVENTS) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tính toán số lượng sự kiện dựa trên run level
     */
    private RunBasedEventDistribution calculateRunBasedEventDistribution() {
        // Lấy run level từ character
        this.currentRunLevel = Math.max(1, gameController.getCharacter().getRun());

        RunBasedEventDistribution distribution = new RunBasedEventDistribution();

        // Tính toán dựa trên run level
        double runMultiplier = 1.0 + (currentRunLevel - 1) * 0.15; // Tăng 15% mỗi run

        // Quiz events: 3-5
        distribution.quizEvents = Math.min(MAX_QUIZ_EVENTS,
                MIN_QUIZ_EVENTS + (currentRunLevel - 1) / 2);

        // Word scramble events: 3-5
        distribution.wordEvents = Math.min(MAX_WORD_EVENTS,
                MIN_WORD_EVENTS + (currentRunLevel - 1) / 2);

        // Multiple choice events: 2-4
        distribution.multiQuizEvents = Math.min(MAX_MULQUIZ_EVENTS,
                MIN_MULQUIZ_EVENTS + (currentRunLevel - 1) / 3);

        // Battle events: 5-8 (tăng mạnh theo run)
        distribution.battleEvents = Math.min(MAX_BATTLE_EVENTS,
                MIN_BATTLE_EVENTS + (currentRunLevel - 1) / 2);

        // Treasure và trap events (cố định hoặc tăng nhẹ)
        distribution.treasureEvents = Math.min(4, 2 + currentRunLevel / 3);
        distribution.trapEvents = Math.min(3, 1 + currentRunLevel / 4);

        // Tính tổng target events
        distribution.totalEvents = distribution.quizEvents + distribution.wordEvents +
                distribution.multiQuizEvents + distribution.battleEvents +
                distribution.treasureEvents + distribution.trapEvents;

        return distribution;
    }

    /**
     * Tạo lại toàn bộ board với sự kiện mới dựa trên run level
     */
    public void randomBoardEveryRun() {
        try {
            System.out.println("=== Bắt đầu tạo board mới cho Run " + currentRunLevel + " ===");

            // Bước 1: Xóa tất cả sự kiện cũ
            resetBoard();

            // Bước 2: Thu thập lại vị trí walkable và xáo trộn
            initializeWalkablePositions();

            prepareShuffledPositions();

            if (walkablePositions.isEmpty()) {
                System.err.println("Không có vị trí walkable nào!");
                return;
            }

            // Bước 3: Tính toán phân phối sự kiện dựa trên run
            RunBasedEventDistribution distribution = calculateRunBasedEventDistribution();
            this.targetEventCount = distribution.totalEvents;

            System.out.println("Phân phối sự kiện cho Run " + currentRunLevel + ":");
            System.out.println("- Quiz: " + distribution.quizEvents);
            System.out.println("- Word Scramble: " + distribution.wordEvents);
            System.out.println("- Multiple Choice: " + distribution.multiQuizEvents);
            System.out.println("- Battle: " + distribution.battleEvents);
            System.out.println("- Treasure: " + distribution.treasureEvents);
            System.out.println("- Trap: " + distribution.trapEvents);
            System.out.println("- Tổng: " + distribution.totalEvents);

            // Bước 4: Đặt sự kiện bắt buộc
            placeMandatoryEvents();

            // Bước 5: Đặt sự kiện theo phân phối run-based
            distributeRunBasedEvents(distribution);

            System.out.println("=== Hoàn thành tạo board: " + totalEventsPlaced + "/" + targetEventCount + " sự kiện ===");

        } catch (Exception e) {
            System.err.println("Lỗi khi tạo board: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Phân phối sự kiện theo run-based distribution
     */
    private void distributeRunBasedEvents(RunBasedEventDistribution distribution) {
        // Đặt từng loại sự kiện với số lượng cố định
        placeEventsByTypeWithLimit("quiz", distribution.quizEvents);
        placeEventsByTypeWithLimit("word_scramble", distribution.wordEvents);
        placeEventsByTypeWithLimit("mulquiz", distribution.multiQuizEvents);
        placeEventsByTypeWithLimit("enemy", distribution.battleEvents);
        placeEventsByTypeWithLimit("treasure", distribution.treasureEvents);
        placeEventsByTypeWithLimit("trap", distribution.trapEvents);
    }

    /**
     * Đặt sự kiện theo loại với giới hạn cụ thể
     */
    private void placeEventsByTypeWithLimit(String eventType, int limit) {
        int placed = 0;

        while (placed < limit) {
            int[] pos = findNextValidPosition();
            if (pos == null) {
                System.out.println("Không còn vị trí hợp lệ cho " + eventType + " (đã đặt " + placed + "/" + limit + ")");
                break;
            }

            boolean success = placeEventAtPosition(eventType, pos[0], pos[1]);
            if (success) {
                placed++;
            }
        }

        System.out.println("Đã đặt " + placed + "/" + limit + " " + eventType + " events");
    }

    /**
     * Đặt sự kiện tại vị trí cụ thể
     */
    private boolean placeEventAtPosition(String eventType, int x, int y) {
        try {
            switch (eventType) {
                case "quiz":
                    placeQuizEvent(x, y);
                    return true;
                case "mulquiz":
                    placeMultiQuizEvent(x, y);
                    return true;
                case "word_scramble":
                    placeWordScrambleEvent(x, y);
                    return true;
                case "trap":
                    placeTrapEvent(x, y);
                    return true;
                case "treasure":
                    placeTreasureEvent(x, y);
                    return true;
                case "enemy":
                    placeEnemyEvent(x, y);
                    return true;
                default:
                    return false;
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi đặt " + eventType + " tại (" + x + ", " + y + "): " + e.getMessage());
            return false;
        }
    }

    /**
     * Xóa tất cả sự kiện cũ
     */
    public void resetBoard() {
        try {
            // Xóa puzzle plates
            this.map.getPuzzle().clear();

            // Reset event manager
            this.eventManager.resetEvents(this.map);

            // Clear local tracking
            this.usedPositions.clear();
            this.totalEventsPlaced = 0;

            System.out.println("Board đã được reset");

        } catch (Exception e) {
            System.err.println("Lỗi khi reset board: " + e.getMessage());
        }
    }

    /**
     * Đặt các sự kiện bắt buộc (new_run_event, chest, enemy cố định)
     */
    private void placeMandatoryEvents() {
        try {
            // Đặt sự kiện kết thúc level
            placeNewRunEvent();

            // Đặt sự kiện từ maze layers nếu có
            placeMazeLayerEvents();

        } catch (Exception e) {
            System.err.println("Lỗi khi đặt sự kiện bắt buộc: " + e.getMessage());
        }
    }

    /**
     * Đặt sự kiện new_run_event ở cuối map
     */
    private void placeNewRunEvent() {
        try {
            int x = this.map.getEndX();
            int y = this.map.getEndY();
            String positionKey = x + "_" + y;

            MapEvent newRunEvent = new MapEvent("new_run_event", "new_run_event", x, y, "Kết thúc tầng", "0");
            eventManager.addEvent(newRunEvent);
            usedPositions.add(positionKey);
            totalEventsPlaced++;

            System.out.println("Đặt new_run_event tại: " + positionKey);

        } catch (Exception e) {
            System.err.println("Lỗi khi đặt new_run_event: " + e.getMessage());
        }
    }

    /**
     * Đặt sự kiện từ maze layers
     */
    private void placeMazeLayerEvents() {
        try {
            if (map.getMaze() == null || map.getMaze().layers == null) {
                return;
            }

            // Đặt fake events (dungeon)
            if (map.getMaze().layers.containsKey("fake")) {
                for (int[] pos : map.getMaze().layers.get("fake")) {
                    if (isValidPosition(pos[0], pos[1])) {
                        String posKey = pos[0] + "_" + pos[1];
                        MapEvent fakeEvent = new MapEvent(posKey, "dungeon", pos[0], pos[1], "Cổng nguy hiểm", "0");
                        eventManager.addEvent(fakeEvent);
                        usedPositions.add(posKey);
                        // Không tính vào totalEventsPlaced vì đây là sự kiện đặc biệt
                    }
                }
            }

            if (map.getMaze().layers.containsKey("chest")) {
                for (int[] pos : map.getMaze().layers.get("chest")) {
                    if (isValidPosition(pos[0], pos[1]) && totalEventsPlaced < targetEventCount) {
                        placeTreasureEvent(pos[0], pos[1]);
                    }
                }
            }

            if (map.getMaze().layers.containsKey("enemy")) {
                for (int[] pos : map.getMaze().layers.get("enemy")) {
                    if (isValidPosition(pos[0], pos[1]) && totalEventsPlaced < targetEventCount) {
                        placeEnemyEvent(pos[0], pos[1]);
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Lỗi khi đặt maze layer events: " + e.getMessage());
        }
    }

    /**
     * Kiểm tra vị trí có hợp lệ không
     */
    private boolean isValidPosition(int x, int y) {
        return map.isWalkable(x, y) && !isStartOrEndPosition(x, y);
    }

    /**
     * Chọn enemy phù hợp với run level
     */
    private Enemy selectEnemyByRunLevel() {
        if (enemies.isEmpty()) {
            throw new IllegalStateException("Không có enemy nào để chọn");
        }

        // Có thể thêm logic chọn enemy dựa trên run level ở đây
        // Hiện tại trả về enemy ngẫu nhiên
        return enemies.get(random.nextInt(enemies.size()));
    }

    // Event placement methods (giữ nguyên các method cũ)
    private void placeQuizEvent(int x, int y) {
        String posKey = x + "_" + y;
        MapEvent event = new MapEvent(posKey, "quiz", x, y, "Thử thách từ vựng", "0");
        eventManager.addEvent(event);
        usedPositions.add(posKey);
        totalEventsPlaced++;
    }

    private void placeMultiQuizEvent(int x, int y) {
        String posKey = x + "_" + y;
        MapEvent event = new MapEvent(posKey, "mulquiz", x, y, "Câu hỏi trắc nghiệm", "0");
        eventManager.addEvent(event);
        usedPositions.add(posKey);
        totalEventsPlaced++;
    }

    private void placeWordScrambleEvent(int x, int y) {
        String posKey = x + "_" + y;
        MapEvent event = new MapEvent(posKey, "word_scramble", x, y, "Xếp từ", "0");
        eventManager.addEvent(event);
        usedPositions.add(posKey);
        totalEventsPlaced++;
    }

    private void placeTrapEvent(int x, int y) {
        String posKey = x + "_" + y;
        addPlates(x, y, "trap", x, y);
        usedPositions.add(posKey);
        totalEventsPlaced++;
    }

    private void placeTreasureEvent(int x, int y) {
        if (items.isEmpty()) return;

        String posKey = x + "_" + y;
        Items randomItem = items.get(random.nextInt(items.size()));
        MapEvent event = new MapEvent(posKey, "treasure", x, y, randomItem.getItemName(), String.valueOf(randomItem.getItemID()));
        eventManager.addEvent(event);
        usedPositions.add(posKey);
        totalEventsPlaced++;
    }

    private void placeEnemyEvent(int x, int y) {
        if (enemies.isEmpty()) return;

        String posKey = x + "_" + y;
        Enemy selectedEnemy = selectEnemyByRunLevel();
        eventManager.addEnemyEvent(posKey, x, y, selectedEnemy);
        usedPositions.add(posKey);
        totalEventsPlaced++;
    }

    // Utility methods (giữ nguyên)
    public void loadItems() {
        try {
            this.items = ItemLoader.getAllItemsWithout("N/A", "quest");
            if (this.items == null) {
                this.items = new ArrayList<>();
            }
            System.out.println("Đã tải " + items.size() + " items");
        } catch (Exception e) {
            System.err.println("Lỗi khi tải items: " + e.getMessage());
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
            System.out.println("Đã tải " + enemies.size() + " enemies");
        } catch (Exception e) {
            System.err.println("Lỗi khi tải enemies: " + e.getMessage());
            this.enemies = new ArrayList<>();
        }
    }

    public void addPlates(int x, int y, String effectType, int targetX, int targetY) {
        try {
            this.map.getPuzzle().addPlate(x, y, effectType, targetX, targetY);
        } catch (Exception e) {
            System.err.println("Lỗi khi thêm plate: " + e.getMessage());
        }
    }

    // Getters
    public WordScrambleGame getWordScrambleGame() {
        return wordScrambleGame;
    }

    public double getEventCoveragePercentage() {
        return walkablePositions.isEmpty() ? 0 : (totalEventsPlaced * 100.0) / walkablePositions.size();
    }

    public int getTotalEventsPlaced() {
        return totalEventsPlaced;
    }

    public int getTargetEventCount() {
        return targetEventCount;
    }

    public int getCurrentRunLevel() {
        return currentRunLevel;
    }

    public void checkBoardPlayerPosition(int playerX, int playerY) {
        // Implementation for player position checking if needed
    }

    public void setMap(IsometricMap map) {
        this.map = map;
        initializeWalkablePositions(); // Refresh walkable positions when map changes
    }

    /**
     * Helper class để lưu trữ phân phối sự kiện dựa trên run level
     */
    private static class RunBasedEventDistribution {
        int quizEvents = 0;
        int multiQuizEvents = 0;
        int wordEvents = 0;
        int battleEvents = 0;
        int treasureEvents = 0;
        int trapEvents = 0;
        int totalEvents = 0;
    }
}