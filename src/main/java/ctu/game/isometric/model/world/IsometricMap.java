package ctu.game.isometric.model.world;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;

public class IsometricMap {
    private TiledMap tiledMap;
    private int tileWidth;
    private int tileHeight;
    private int mapWidth;
    private int mapHeight;
    private TiledMapTileLayer baseLayer;

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
        baseLayer = (TiledMapTileLayer) tiledMap.getLayers().get(0);
    }

    // For backwards compatibility
    public IsometricMap() {
        this("maps/untitled.tmx"); // Default map path
    }

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

    // Get the tile ID at a specific position
    public int getTileId(int x, int y) {
        TiledMapTileLayer.Cell cell = baseLayer.getCell(x, y);
        if (cell != null && cell.getTile() != null) {
            return cell.getTile().getId();
        }
        return 0; // Empty tile
    }

    // Check if a tile is walkable (can use a property in Tiled called "walkable")
    public boolean isWalkable(int x, int y) {
        if (x < 0 || y < 0 || x >= mapWidth || y >= mapHeight) {
            return false;
        }

        TiledMapTileLayer.Cell cell = baseLayer.getCell(x, y);
        if (cell == null) {
            return false;
        }

        // First check cell properties (higher priority)
        Object walkable = cell.getTile().getProperties().get("walkable");
        if (walkable == null) {
            walkable = cell.getTile().getProperties().get("Walkable");
        }

        // Then check tile properties
        if (walkable == null) {
            TiledMapTile tile = cell.getTile();
            if (tile != null) {
                walkable = tile.getProperties().get("walkable");
                if (walkable == null) {
                    walkable = tile.getProperties().get("Walkable");
                }
            }
        }

        // Try layer properties as fallback
        if (walkable == null && baseLayer.getProperties().containsKey("walkable")) {
            walkable = baseLayer.getProperties().get("walkable");
        }

        // Parse the property value flexibly
        if (walkable != null) {
            if (walkable instanceof Boolean) {
                return (Boolean) walkable;
            } else {
                String value = walkable.toString().toLowerCase();
                return value.equals("true") || value.equals("1") || value.equals("yes");
            }
        }

        // If no property found, determine default behavior
        // You could change this to true if most tiles should be walkable
        return false;
    }

    // For compatibility with existing code
    public int[][] getMapData() {
        int[][] data = new int[mapHeight][mapWidth];
        for (int y = 0; y < mapHeight; y++) {
            for (int x = 0; x < mapWidth; x++) {
                data[y][x] = getTileId(x, y); // Changed from data[x][y]
            }
        }
        return data;
    }

    // For compatibility with existing texture system
    public String getTileTexturePath(int tileType) {
        // In a TMX map, we don't need this since textures are defined in the map
        // Return null or a default texture path if needed
        return "tiles/default.png";
    }

    public String getTileTexturePath() {
        return "tiles/default.png";
    }
}