package ctu.game.isometric.model.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.math.Vector2;
import ctu.game.isometric.model.entity.Enemy;
import ctu.game.isometric.model.puzzle.PressurePlatePuzzle;
import ctu.game.isometric.util.MazeGenerator;

import java.util.*;

public class IsometricMap {
    private TiledMap tiledMap;
    private int tileWidth = 64;
    private int tileHeight;
    private int mapWidth;
    private int mapHeight;
    private TiledMapTileLayer baseLayer;
    private TiledMapTileLayer terrianLayer;
    private boolean[][] walkableCache;
    private int[][] mapData;
    private String mapName; // Default map name

    // Chunking system
    private Map<Long, MapChunk> chunks = new HashMap<>();
    private static final int CHUNK_SIZE = 16;
    private boolean chunkingEnabled = false;
    PressurePlatePuzzle puzzle;

    int startX = 15;
    int startY = 15;

    int endX = 15;
    int endY = 15;

    public IsometricMap(String tmxFilePath) {
        // Load the TMX file
// Extract the map name by removing both "maps/" prefix and ".tmx" suffix

        tiledMap = new TmxMapLoader().load(tmxFilePath);

        String tempName = tmxFilePath.replaceAll("\\.tmx$", ""); // Remove .tmx extension
        this.mapName = tempName.contains("/") ? tempName.substring(tempName.lastIndexOf("/") + 1) : tempName;

        if (this.mapName == null || this.mapName.isEmpty()) {
            this.mapName = "default_map"; // Fallback name
        }

        // Get map properties
        MapProperties props = tiledMap.getProperties();

        tileWidth = props.get("tilewidth", Integer.class);
        tileHeight = props.get("tileheight", Integer.class);
        mapWidth = props.get("width", Integer.class);
        mapHeight = props.get("height", Integer.class);

        // Assume the first layer is the base layer
        baseLayer = (TiledMapTileLayer) tiledMap.getLayers().get("ground_layer");
        terrianLayer = (TiledMapTileLayer) tiledMap.getLayers().get("terrain_layer");

        if (mapName.equals("board")) {
            generateRandomMaze();
        } else {
            initializeMapData();
            initializeWalkableCache();
        }
        // Initialize data structures


        // Auto-enable chunking for large maps
        if (mapWidth * mapHeight > 10000) {
            enableChunking();
        }
        puzzle = new PressurePlatePuzzle("puzzle1", this, 3);

        loadPlate();
    }

    MazeGenerationResult maze;

    public MazeGenerationResult getMaze() {
        return maze;
    }


    public void generateRandomMaze() {
        MazeGenerator generator = new MazeGenerator();
        System.out.println("Generating random maze with size: " + mapWidth + "x" + mapHeight);
        this.maze = generator.generateMaze(); // Generate maze data

        this.startX = maze.startX;
        this.startY = maze.startY;
        this.endX = maze.endX;
        this.endY = maze.endY;

        int[][] base = this.maze.layers.get("base");
        int[][] terrain = this.maze.layers.get("terrain");

        for (int y = 0; y < mapHeight; y++) {
            for (int x = 0; x < mapWidth; x++) {
                boolean isPath = base[y][x] == 1;  // ĐÚNG: [y][x]
                TiledMapTileLayer.Cell groundCell = new TiledMapTileLayer.Cell();
                groundCell.setTile(tiledMap.getTileSets().getTile(isPath ? 1 : 0));
                baseLayer.setCell(x, y, groundCell);
            }
        }

        for (int y = 0; y < mapHeight; y++) {
            for (int x = 0; x < mapWidth; x++) {
                boolean isBlocked = terrain[y][x] == 2;
                TiledMapTileLayer.Cell terrainCell = new TiledMapTileLayer.Cell();
                terrainCell.setTile(tiledMap.getTileSets().getTile(isBlocked ? 2 : 0));
                terrianLayer.setCell(x, y, terrainCell);
            }
        }


        initializeMapData();
        initializeWalkableCache();
        printMatrix(maze.layers.get("fake"));
        System.out.println("Maze generated from (" + startX + "," + startY + ") to (" + endX + "," + endY + ")");
    }

    public static void printMatrix(int[][] matrix) {
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                System.out.print(matrix[i][j] + " ");
            }
            System.out.println(); // xuống dòng sau mỗi hàng
        }
    }

    private int getRandomOdd(int max) {
        Random rand = new Random();
        int r = rand.nextInt((max - 1) / 2) * 2 + 1;
        return Math.min(r, max - 2); // tránh sát rìa
    }

    private int[] findFarthestPathCell(int[][] maze, int sx, int sy) {
        boolean[][] visited = new boolean[mapWidth][mapHeight];
        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[]{sx, sy, 0});

        int maxDist = -1;
        int[] farthest = new int[]{sx, sy};

        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int x = curr[0], y = curr[1], dist = curr[2];
            if (visited[x][y]) continue;
            visited[x][y] = true;

            if (dist > maxDist) {
                maxDist = dist;
                farthest[0] = x;
                farthest[1] = y;
            }

            for (int[] d : dirs) {
                int nx = x + d[0];
                int ny = y + d[1];
                if (nx >= 0 && ny >= 0 && nx < mapWidth && ny < mapHeight && maze[nx][ny] == 1 && !visited[nx][ny]) {
                    queue.add(new int[]{nx, ny, dist + 1});
                }
            }
        }

        return farthest;
    }

    private void generateMazePrim(int[][] maze, int startX, int startY) {
        for (int[] row : maze) {
            Arrays.fill(row, 0); // toàn bộ là tường
        }

        Random rand = new Random();
        List<int[]> walls = new ArrayList<>();

        maze[startX][startY] = 1;

        int[][] directions = {{2, 0}, {-2, 0}, {0, 2}, {0, -2}};

        // Thêm các tường lân cận vào danh sách
        for (int[] dir : directions) {
            int wx = startX + dir[0];
            int wy = startY + dir[1];
            if (wx > 0 && wy > 0 && wx < mapWidth - 1 && wy < mapHeight - 1) {
                walls.add(new int[]{wx, wy, startX, startY});
            }
        }

        while (!walls.isEmpty()) {
            int[] wall = walls.remove(rand.nextInt(walls.size()));
            int wx = wall[0], wy = wall[1];
            int px = wall[2], py = wall[3];

            if (maze[wx][wy] == 0) {
                int betweenX = (wx + px) / 2;
                int betweenY = (wy + py) / 2;

                maze[betweenX][betweenY] = 1;
                maze[wx][wy] = 1;

                for (int[] dir : directions) {
                    int nx = wx + dir[0];
                    int ny = wy + dir[1];
                    if (nx > 0 && ny > 0 && nx < mapWidth - 1 && ny < mapHeight - 1 && maze[nx][ny] == 0) {
                        walls.add(new int[]{nx, ny, wx, wy});
                    }
                }
            }
        }
    }


    public TiledMapTileLayer getTerrianLayer() {
        return terrianLayer;
    }

    public void setTileId(int x, int y, int tileId) {
        if (x < 0 || x >= mapWidth || y < 0 || y >= mapHeight) {
            throw new IndexOutOfBoundsException("Coordinates out of bounds: " + x + ", " + y);
        }

        TiledMapTileLayer.Cell cell = baseLayer.getCell(x, y);
        if (cell == null) {
            cell = new TiledMapTileLayer.Cell();
            baseLayer.setCell(x, y, cell);
        }
        TiledMapTile tile = tiledMap.getTileSets().getTile(tileId);
        cell.setTile(tile);

    }


    public boolean[][] getWalkableCache() {
        return walkableCache;
    }

    public void setMapData(int[][] mapData) {
        this.mapData = mapData;
    }

    public void setWalkableCache(boolean[][] walkableCache) {
        this.walkableCache = walkableCache;
    }

    public int getStartX() {
        return startX;
    }

    public void setStartX(int startX) {
        this.startX = startX;
    }

    public int getStartY() {
        return startY;
    }

    public void setStartY(int startY) {
        this.startY = startY;
    }

    public String getMapName() {
        return mapName;
    }

    public void setMapName(String mapName) {
        this.mapName = mapName;
    }

    public void loadPlate() {

//        puzzle.loadTexturesForType("door", "textures/door_plate_inactive.png", "textures/door_plate_active.png");
//        puzzle.loadTexturesForType("trap", "textures/trap_inactive.png", "textures/trap_active.png");
    }


    public TiledMapTileLayer.Cell getCell(int x, int y) {
        MapLayer layer = tiledMap.getLayers().get("terrain_layer");
        if (layer instanceof TiledMapTileLayer tiledMapLayer) {
            return tiledMapLayer.getCell(x, y);
        }
        return null; // Return null if the layer is not of the expected type
    }

    // For backwards compatibility
    public IsometricMap() {
        this("maps/board.tmx");
        // Default map path
    }


    // Enable chunking for large maps
    public void enableChunking() {
        this.chunkingEnabled = true;
    }

    // Initialize map data efficiently using parallel processing
    public void initializeMapData() {
        mapData = new int[mapHeight][mapWidth];
        Arrays.parallelSetAll(mapData, y -> {
            int[] row = new int[mapWidth];
            for (int x = 0; x < mapWidth; x++) {
                row[x] = getTileIdDirect(x, y);
            }
            return row;
        });
    }

    // Direct access to tile ID without going through chunks
    protected int getTileIdDirect(int x, int y) {
        if (baseLayer == null) {
            return 0; // Empty tile
        }
        TiledMapTileLayer.Cell cell = baseLayer.getCell(x, y);
        if (cell != null && cell.getTile() != null) {
            return cell.getTile().getId();
        }
        return 0; // Empty tile
    }

    // Get the tile ID at a specific position - uses chunking if enabled
    public int getTileId(int x, int y) {
        if (!chunkingEnabled) {
            return getTileIdDirect(x, y);
        } else {
            MapChunk chunk = getOrCreateChunk(x, y);
            return chunk.getTileId(x % CHUNK_SIZE, y % CHUNK_SIZE);
        }
    }


    public void setTileWalkable(int x, int y, boolean walkable) {
        if (walkableCache == null) {
            initializeWalkableCache();
        }
        walkableCache[y][x] = walkable;
    }

    // Get or create a chunk for the given position
    private MapChunk getOrCreateChunk(int x, int y) {
        int chunkX = x / CHUNK_SIZE;
        int chunkY = y / CHUNK_SIZE;
        long key = ((long) chunkX << 32) | (chunkY & 0xFFFFFFFFL);

        return chunks.computeIfAbsent(key, k -> new MapChunk(this, chunkX, chunkY));
    }

    // Initialize walkable cache


    // Made public so chunks can use it
    public boolean calculateWalkable(int x, int y) {
        TiledMapTileLayer.Cell cell = baseLayer.getCell(x, y);
        if (cell == null || cell.getTile() == null || cell.getTile().getId() <= 0) {
            return false;
        }
        TiledMapTileLayer collision = (TiledMapTileLayer) tiledMap.getLayers().get("terrain_layer");
        if (collision != null) {
            TiledMapTileLayer.Cell cell2 = collision.getCell(x, y);
            if (cell2 != null && cell2.getTile() != null) {
                MapProperties properties = cell2.getTile().getProperties();
                return properties.containsKey("walkable") && properties.get("walkable", Boolean.class);
            }
        }
        return true;
    }

    public PressurePlatePuzzle getPuzzle() {
        return puzzle;
    }

    public void setPuzzle(PressurePlatePuzzle puzzle) {
        this.puzzle = puzzle;
    }

    // Check if a tile is walkable - uses chunking if enabled
    public boolean isWalkable(int x, int y) {
        if (x < 0 || x >= mapWidth || y < 0 || y >= mapHeight) {
            return false;
        }

        if (!chunkingEnabled) {
            return walkableCache[y][x];
        } else {
            MapChunk chunk = getOrCreateChunk(x, y);
            return chunk.isWalkable(x % CHUNK_SIZE, y % CHUNK_SIZE);
        }
    }

    public void initializeWalkableCache() {
        walkableCache = new boolean[mapHeight][mapWidth];
        for (int y = 0; y < mapHeight; y++) {
            for (int x = 0; x < mapWidth; x++) {
                walkableCache[y][x] = calculateWalkable(x, y);
            }
        }
    }

    private static final long CHUNK_TIMEOUT_MS = 30000;

    public int cleanupChunks(long maxAgeMs) {
        if (!chunkingEnabled) return 0;

        long currentTime = System.currentTimeMillis();
        int initialSize = chunks.size();

        Iterator<Map.Entry<Long, MapChunk>> iterator = chunks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, MapChunk> entry = iterator.next();
            MapChunk chunk = entry.getValue();

            if (currentTime - chunk.getLastAccessTime() > maxAgeMs) {
                iterator.remove();
            }
        }

        return initialSize - chunks.size();
    }

    public int cleanupChunks() {
        return cleanupChunks(CHUNK_TIMEOUT_MS);
    }

    public int[][] getMapData() {
        return mapData;
    }

    // Standard getters and setters
    public TiledMap getTiledMap() {
        return tiledMap;
    }

    public int getTileWidth() {
        return tileWidth;
    }

    public int getTileHeight() {
        return tileHeight;
    }

    public int getMapWidth() {
        return mapWidth;
    }

    public int getMapHeight() {
        return mapHeight;
    }

    public TiledMapTileLayer getBaseLayer() {
        return baseLayer;
    }

    public int getEndX() {
        return endX;
    }

    public void setEndX(int endX) {
        this.endX = endX;
    }

    public int getEndY() {
        return endY;
    }

    public void setEndY(int endY) {
        this.endY = endY;
    }


}