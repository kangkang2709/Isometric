package ctu.game.isometric.model.world;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MazeGenerationResult {
    public Map<Integer, int[][]> layers = new HashMap<>(); // 0: đường chính, 1: end giả, 6: rương
    public int[][] minimapMask;
    public List<int[]> enemySpawns;
    public int pathLength;
    public int startX, startY, endX, endY;
}
