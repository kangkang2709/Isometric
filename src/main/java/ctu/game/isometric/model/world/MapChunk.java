package ctu.game.isometric.model.world;

public class MapChunk {
    private int chunkX, chunkY;
    private int[][] tileData;
    private boolean[][] walkableData;
    private static final int CHUNK_SIZE = 16;
    private long lastAccessTime;

    public MapChunk(IsometricMap map, int chunkX, int chunkY) {
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.lastAccessTime = System.currentTimeMillis();

        int startX = chunkX * CHUNK_SIZE;
        int startY = chunkY * CHUNK_SIZE;
        int endX = Math.min(startX + CHUNK_SIZE, map.getMapWidth());
        int endY = Math.min(startY + CHUNK_SIZE, map.getMapHeight());

        tileData = new int[CHUNK_SIZE][CHUNK_SIZE];
        walkableData = new boolean[CHUNK_SIZE][CHUNK_SIZE];

        for (int y = 0; y < CHUNK_SIZE; y++) {
            for (int x = 0; x < CHUNK_SIZE; x++) {
                int mapX = startX + x;
                int mapY = startY + y;
                if (mapX < endX && mapY < endY) {
                    // Use direct methods to avoid recursion
                    tileData[y][x] = map.getTileIdDirect(mapX, mapY);
                    walkableData[y][x] = map.calculateWalkable(mapX, mapY);
                }
            }
        }
    }

    public int getTileId(int localX, int localY) {
        this.lastAccessTime = System.currentTimeMillis();
        if (localX >= 0 && localX < CHUNK_SIZE && localY >= 0 && localY < CHUNK_SIZE) {
            return tileData[localY][localX];
        }
        return 0;
    }

    public boolean isWalkable(int localX, int localY) {
        this.lastAccessTime = System.currentTimeMillis();
        if (localX >= 0 && localX < CHUNK_SIZE && localY >= 0 && localY < CHUNK_SIZE) {
            return walkableData[localY][localX];
        }
        return false;
    }

    public long getLastAccessTime() {
        return lastAccessTime;
    }
}