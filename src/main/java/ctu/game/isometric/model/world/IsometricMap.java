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

    int startX = 0;
    int startY = 0;

    int endX = 0;
    int endY = 0;

    public IsometricMap(String tmxFilePath) {
        // Load the TMX file
// Extract the map name by removing both "maps/" prefix and ".tmx" suffix

        tiledMap = new TmxMapLoader().load(tmxFilePath);

        String tempName = tmxFilePath.replaceAll("\\.tmx$", ""); // Remove .tmx extension
        this.mapName = tempName.contains("/") ? tempName.substring(tempName.lastIndexOf("/") + 1) : tempName;

        if (this.mapName == null || this.mapName.isEmpty()) {
            this.mapName = "default_map"; // Fallback name
        }

        switch (this.mapName) {
            case "board":
                this.startX = 0;
                this.startY = 0;
                this.endX = 15;
                this.endY = 15;
                break;
            case "main":
                this.startX = 12;
                this.startY = 12;
                break;
            default:
                // Keep the original name
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
        // Initialize data structures
        initializeMapData();
        initializeWalkableCache();


        // Auto-enable chunking for large maps
        if (mapWidth * mapHeight > 10000) {
            enableChunking();
        }
        puzzle = new PressurePlatePuzzle("puzzle1", this, 3);

        loadPlate();
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
        TiledMapTileLayer tiledMapLayer = (TiledMapTileLayer) tiledMap.getLayers().get("terrain_layer");
        if (tiledMapLayer == null) return null; // Ensure the layer exists
        return tiledMapLayer.getCell(x, y); // Delegate to the TiledMapTileLayer
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
        if (x < 0 || x >= mapWidth || y < 0 || y >= mapHeight) {
            throw new IndexOutOfBoundsException("Coordinates out of bounds: " + x + ", " + y);
        }

        if (!chunkingEnabled) {
            walkableCache[y][x] = walkable;
        } else {
            MapChunk chunk = getOrCreateChunk(x, y);
            chunk.setWalkable(x % CHUNK_SIZE, y % CHUNK_SIZE, walkable);
        }
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

    public void setTiledMap(TiledMap tiledMap) {
        this.tiledMap = tiledMap;
    }

    public void setTileWidth(int tileWidth) {
        this.tileWidth = tileWidth;
    }

    public void setTileHeight(int tileHeight) {
        this.tileHeight = tileHeight;
    }

    public void setMapWidth(int mapWidth) {
        this.mapWidth = mapWidth;
    }

    public void setMapHeight(int mapHeight) {
        this.mapHeight = mapHeight;
    }

    public void setBaseLayer(TiledMapTileLayer baseLayer) {
        this.baseLayer = baseLayer;
    }
}