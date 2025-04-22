package ctu.game.isometric.model.world;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class IsometricMap {
    private TiledMap tiledMap;
    private int tileWidth = 64;
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
        baseLayer = (TiledMapTileLayer) tiledMap.getLayers().get("ground_layer");
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
                data[y][x] = getTileId(x, y);
            }
        }
        return data;
    }
    public Vector2 orthogonalToIsometric(float x, float y) {
        float isoX = (x - y);
        float isoY = (x + y) / 2;
        return new Vector2(isoX, isoY);
    }

    public boolean isWalkable(int x, int y) {
        // 1. Kiểm tra tọa độ hợp lệ
        if (x < 0 || x >= mapWidth || y < 0 || y >= mapHeight) {
            return false;
        }

        // 2. Lấy tile tại vị trí (x, y)
        TiledMapTileLayer.Cell cell = baseLayer.getCell(x, y);
        if (cell == null || cell.getTile() == null || cell.getTile().getId() <= 0) {
            return false;
        }

        // 3. Chuyển đổi tile grid (x, y) sang tọa độ pixel thực (orthogonal)
        float tilePixelX = x * tileWidth;
        float tilePixelY = y * tileHeight;

        // 4. Chuyển sang tọa độ isometric
        Vector2 isoPos = orthogonalToIsometric(tilePixelX, tilePixelY);
        float worldX = isoPos.x;
        float worldY = isoPos.y;

        System.out.println("Isometric coordinates: isoX=" + worldX + ", isoY=" + worldY);

        // 5. Kiểm tra va chạm với object trong layer collision
        MapLayer collisionLayer = tiledMap.getLayers().get("collision_layer");
        if (collisionLayer != null) {
            for (MapObject object : collisionLayer.getObjects()) {
                if (object instanceof PolygonMapObject) {
                    Polygon polygon = ((PolygonMapObject) object).getPolygon();
                    if (polygon.contains(worldX, worldY)) {

                        return false;
                    }
                }
            }
        }

        System.out.println("No collision detected. Position is walkable.");
        return true;
    }





    /**
     * Check if a point is inside a polygon defined by an array of vertices.
     * @param x X coordinate of the point
     * @param y Y coordinate of the point
     * @param vertices Array of vertices (x1,y1,x2,y2,...)
     * @return true if the point is inside the polygon
     */
    private boolean isPointInPolygon(float x, float y, float[] vertices) {
        boolean inside = false;
        int j = vertices.length - 2;

        for (int i = 0; i < vertices.length; i += 2) {
            float xi = vertices[i];
            float yi = vertices[i + 1];
            float xj = vertices[j];
            float yj = vertices[j + 1];

            boolean intersect = ((yi > y) != (yj > y)) &&
                    (x < (xj - xi) * (y - yi) / (yj - yi) + xi);

            if (intersect) {
                inside = !inside;
            }

            j = i;
        }

        return inside;
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