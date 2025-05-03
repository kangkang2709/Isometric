package ctu.game.isometric.model.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.math.Vector2;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class IsometricMap {
    private TiledMap tiledMap;
    private int tileWidth = 64;
    private int tileHeight;
    private int mapWidth;
    private int mapHeight;
    private TiledMapTileLayer baseLayer;
    private boolean[][] walkableCache;
    private int[][] mapData;

    // Chunking system
    private Map<Long, MapChunk> chunks = new HashMap<>();
    private static final int CHUNK_SIZE = 16;
    private boolean chunkingEnabled = false;

    public IsometricMap(String tmxFilePath) {
        // Load the TMX file
        tiledMap = new TmxMapLoader().load(tmxFilePath);

        // Get map properties
        MapProperties props = tiledMap.getProperties();
        tileWidth = props.get("tilewidth", Integer.class);
        tileHeight = props.get("tileheight", Integer.class);
        mapWidth = props.get("width", Integer.class);
        mapHeight = props.get("height", Integer.class);

        // Assume the first layer is the base layer
        baseLayer = (TiledMapTileLayer) tiledMap.getLayers().get("ground_layer");

        // Initialize data structures
        initializeMapData();
        initializeWalkableCache();

        // Auto-enable chunking for large maps
        if (mapWidth * mapHeight > 10000) {
            enableChunking();
        }
    }
    public TiledMapTileLayer.Cell getCell(int x, int y) {
        TiledMapTileLayer tiledMapLayer = (TiledMapTileLayer) tiledMap.getLayers().get("terrain_layer");
        if (tiledMapLayer == null) return null; // Ensure the layer exists
        return tiledMapLayer.getCell(x, y); // Delegate to the TiledMapTileLayer
    }
    // For backwards compatibility
    public IsometricMap() {
        this("maps/untitled1.tmx"); // Default map path
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

    // Get or create a chunk for the given position
    private MapChunk getOrCreateChunk(int x, int y) {
        int chunkX = x / CHUNK_SIZE;
        int chunkY = y / CHUNK_SIZE;
        long key = ((long)chunkX << 32) | (chunkY & 0xFFFFFFFFL);

        return chunks.computeIfAbsent(key, k -> new MapChunk(this, chunkX, chunkY));
    }

    // Initialize walkable cache
    public void initializeWalkableCache() {
        walkableCache = new boolean[mapHeight][mapWidth];
        for (int y = 0; y < mapHeight; y++) {
            for (int x = 0; x < mapWidth; x++) {
                walkableCache[y][x] = calculateWalkable(x, y);
            }
        }
    }

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

    // Check if a tile is walkable - uses chunking if enabled
    public boolean isWalkable(int x, int y) {
        if (x < 0 || x >= mapWidth || y < 0 || y >= mapHeight) {
            return false;
        }

        if (!chunkingEnabled) {
            System.out.println("Using walkable cache");
            return walkableCache[y][x];
        } else {
            MapChunk chunk = getOrCreateChunk(x, y);
            return chunk.isWalkable(x % CHUNK_SIZE, y % CHUNK_SIZE);
        }
    }

    // Get map data (cached)
    public int[][] getMapData() {
        return mapData;
    }

    // Get viewport-based data for efficient rendering
    public int[][] getViewportData(int centerX, int centerY, int width, int height) {
        int startX = centerX - width / 2;
        int startY = centerY - height / 2;
        int[][] viewportData = new int[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int mapX = startX + x;
                int mapY = startY + y;
                if (mapX >= 0 && mapX < mapWidth && mapY >= 0 && mapY < mapHeight) {
                    viewportData[y][x] = getTileId(mapX, mapY);
                } else {
                    viewportData[y][x] = 0; // Default value for out-of-bounds
                }
            }
        }
        return viewportData;
    }

    public boolean[][] getViewportWalkable(int centerX, int centerY, int width, int height) {
        int startX = centerX - width / 2;
        int startY = centerY - height / 2;
        boolean[][] viewportWalkable = new boolean[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int mapX = startX + x;
                int mapY = startY + y;
                viewportWalkable[y][x] = isWalkable(mapX, mapY);
            }
        }
        return viewportWalkable;
    }


    // Utility method to convert tile coordinates to world coordinates
    public Vector2 tileToWorld(int x, int y) {
        float worldX = (x - y) * (tileWidth / 2f);
        float worldY = (x + y) * (tileHeight / 2f);
        return new Vector2(worldX, worldY);
    }


    public void cleanupUnusedChunks(long olderThanMillis) {
        long currentTime = System.currentTimeMillis();
        chunks.entrySet().removeIf(entry ->
                entry.getValue().getLastAccessTime() < currentTime - olderThanMillis);
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