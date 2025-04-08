package ctu.game.isometric.model.world;

public class IsometricMap {
    private int[][] mapData;
    private final int tileWidth = 64;
    private final int tileHeight = 32;
    private String tileTexturePath;

    public IsometricMap() {
        this.tileTexturePath = "tiles/grass.png";

        // Simple map layout (1 = tile, 0 = empty)
        mapData = new int[][] {
                {1, 1, 1, 1, 1},
                {1, 1, 1, 1, 1},
                {1, 1, 1, 1, 1},
                {1, 1, 1, 1, 1},
                {1, 1, 1, 1, 1}
        };
    }

    public int[][] getMapData() { return mapData; }
    public int getTileWidth() { return tileWidth; }
    public int getTileHeight() { return tileHeight; }
    public String getTileTexturePath() { return tileTexturePath; }

    public boolean isWalkable(int x, int y) {
        if (x < 0 || y < 0 || y >= mapData.length || x >= mapData[0].length) {
            return false;
        }
        return mapData[y][x] == 1;
    }
}