package ctu.game.isometric.model.world;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MazeGenerationResult {
    public Map<String, int[][]> layers = new HashMap<>();
    public int[][] minimapMask;
    public List<int[]> enemySpawns;
    public int pathLength;
    public int startX, startY, endX, endY;
}
