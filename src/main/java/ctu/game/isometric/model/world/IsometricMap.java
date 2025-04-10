package ctu.game.isometric.model.world;

import java.util.HashMap;
import java.util.Map;

public class IsometricMap {
    private int[][] mapData;
    private final int tileWidth = 64;
    private final int tileHeight = 32;
    private Map<Integer, String> tileTextureMap;
    private String defaultTileTexturePath;

    public IsometricMap() {
        this.defaultTileTexturePath = "tiles/brick.png";

        // Initialize tile texture mapping
        tileTextureMap = new HashMap<>();
        tileTextureMap.put(1, "tiles/brick.png");
        tileTextureMap.put(2, "tiles/wood.png");

        // Simple map layout (1 = tile, 0 = empty)
        mapData = new int[][] {
                {1, 1, 1, 1, 1},
                {1, 1, 2, 1, 1},
                {1, 1, 1, 2, 1},
                {1, 2, 1, 2, 1},
                {1, 1, 1, 1, 1}
        };
    }


    public int[][] getMapData() { return mapData; }
    public int getTileWidth() { return tileWidth; }
    public int getTileHeight() { return tileHeight; }

    public String getTileTexturePath() {
        return defaultTileTexturePath;
    }

    public String getTileTexturePath(int tileType) {
        return tileTextureMap.getOrDefault(tileType, defaultTileTexturePath);
    }

    public boolean isWalkable(int x, int y) {
        if (x < 0 || y < 0 || y >= mapData.length || x >= mapData[0].length) {
            return false;
        }
        return mapData[y][x] == 1;
    }
}