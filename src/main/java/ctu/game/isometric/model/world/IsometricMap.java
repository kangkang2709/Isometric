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

    public boolean isWalkable(int x, int y) {
        // First check if coordinates are within map bounds
        if (x < 0 || x >= mapWidth || y < 0 || y >= mapHeight) {
            return false;
        }

        // Check if the tile exists at this position
        TiledMapTileLayer.Cell cell = baseLayer.getCell(x, y);
        if (cell == null) {
            return false;
        }

        // If you have a "walkable" property in your tiles, you can check it:
        // TiledMapTile tile = cell.getTile();
        // if (tile != null && tile.getProperties().containsKey("walkable")) {
        //     return tile.getProperties().get("walkable", Boolean.class);
        // }

        // If there's no specific property, assume tiles with ID > 0 are walkable
        return getTileId(x, y) > 0;
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

    public TiledMapTileLayer getBaseLayer() {
        return baseLayer;
    }

    public void setBaseLayer(TiledMapTileLayer baseLayer) {
        this.baseLayer = baseLayer;
    }
}