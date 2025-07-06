package ctu.game.isometric.util;

import ctu.game.isometric.model.world.MazeGenerationResult;

import java.util.*;

public class MazeGenerator {

    private int mapWidth, mapHeight;
    private int startX, startY, endX, endY;

    public MazeGenerator(int mapWidth, int mapHeight) {
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
    }

    public MazeGenerationResult generateMazeData() {
        MazeGenerationResult result = new MazeGenerationResult();

        int[][] maze = new int[mapWidth][mapHeight];
        int[][] pathLayer = new int[mapWidth][mapHeight];
        int[][] fakeEndLayer = new int[mapWidth][mapHeight];
        int[][] chestLayer = new int[mapWidth][mapHeight];

        // B1: Sinh mê cung bằng Prim
        startX = getRandomOdd(mapWidth);
        startY = getRandomOdd(mapHeight);
        generateMazePrim(maze, startX, startY);

        // B2: Tìm endpoint thật
        int[] far = findFarthestPathCell(maze, startX, startY);
        endX = far[0];
        endY = far[1];

        // B3: Tìm đường chính xác
        int[][] exactPath = findShortestPath(maze, startX, startY, endX, endY);
        for (int x = 0; x < mapWidth; x++)
            for (int y = 0; y < mapHeight; y++)
                if (exactPath[x][y] == 1) pathLayer[x][y] = 1;

        // B4: End point giả
        List<int[]> deadEnds = findDeadEnds(maze);
        Collections.shuffle(deadEnds);
        int fakeCount = Math.min(3, deadEnds.size());
        for (int i = 0; i < fakeCount; i++) {
            int[] p = deadEnds.get(i);
            if (exactPath[p[0]][p[1]] != 1)
                fakeEndLayer[p[0]][p[1]] = 2;
        }

        // B5: Đặt rương ở dead ends còn lại
        int chests = 0;
        for (int[] p : deadEnds) {
            int x = p[0], y = p[1];
            if (pathLayer[x][y] == 0 && fakeEndLayer[x][y] == 0 && chests < 5) {
                chestLayer[x][y] = 6;
                chests++;
            }
        }

        // B6: Enemy spawn
        List<int[]> enemySpawns = suggestEnemySpawns(maze, exactPath, 5);

        // B7: Minimap mask
        int[][] minimapMask = generateMinimapMask(exactPath);

        // Kết quả
        result.startX = startX;
        result.startY = startY;
        result.endX = endX;
        result.endY = endY;
        result.layers.put(0, pathLayer);
        result.layers.put(1, fakeEndLayer);
        result.layers.put(6, chestLayer);
        result.pathLength = countPathLength(exactPath);
        result.enemySpawns = enemySpawns;
        result.minimapMask = minimapMask;

        return result;
    }

    // ------------------------ SUPPORT METHODS ------------------------

    private int getRandomOdd(int max) {
        Random rand = new Random();
        int r = rand.nextInt((max - 1) / 2) * 2 + 1;
        return Math.min(r, max - 2);
    }

    private void generateMazePrim(int[][] maze, int sx, int sy) {
        for (int[] row : maze) Arrays.fill(row, 0);
        maze[sx][sy] = 1;

        List<int[]> frontier = new ArrayList<>();
        int[][] dirs = {{2, 0}, {-2, 0}, {0, 2}, {0, -2}};

        for (int[] d : dirs) {
            int nx = sx + d[0], ny = sy + d[1];
            if (inBounds(nx, ny)) frontier.add(new int[]{nx, ny, sx, sy});
        }

        Random rand = new Random();
        while (!frontier.isEmpty()) {
            int[] f = frontier.remove(rand.nextInt(frontier.size()));
            int x = f[0], y = f[1], px = f[2], py = f[3];
            if (maze[x][y] == 0) {
                maze[x][y] = 1;
                maze[(x + px) / 2][(y + py) / 2] = 1;
                for (int[] d : dirs) {
                    int nx = x + d[0], ny = y + d[1];
                    if (inBounds(nx, ny) && maze[nx][ny] == 0) {
                        frontier.add(new int[]{nx, ny, x, y});
                    }
                }
            }
        }
    }

    private int[] findFarthestPathCell(int[][] maze, int sx, int sy) {
        boolean[][] visited = new boolean[mapWidth][mapHeight];
        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[]{sx, sy, 0});
        int maxDist = -1;
        int[] farthest = new int[]{sx, sy};
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int x = curr[0], y = curr[1], dist = curr[2];
            if (visited[x][y]) continue;
            visited[x][y] = true;
            if (dist > maxDist) {
                maxDist = dist;
                farthest = new int[]{x, y};
            }
            for (int[] d : dirs) {
                int nx = x + d[0], ny = y + d[1];
                if (inBounds(nx, ny) && maze[nx][ny] == 1 && !visited[nx][ny]) {
                    queue.add(new int[]{nx, ny, dist + 1});
                }
            }
        }
        return farthest;
    }

    private int[][] findShortestPath(int[][] maze, int sx, int sy, int ex, int ey) {
        int[][] path = new int[mapWidth][mapHeight];
        boolean[][] visited = new boolean[mapWidth][mapHeight];
        int[][] parentX = new int[mapWidth][mapHeight];
        int[][] parentY = new int[mapWidth][mapHeight];
        for (int[] row : parentX) Arrays.fill(row, -1);
        for (int[] row : parentY) Arrays.fill(row, -1);

        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[]{sx, sy});
        visited[sx][sy] = true;

        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int x = curr[0], y = curr[1];
            if (x == ex && y == ey) break;

            for (int[] d : dirs) {
                int nx = x + d[0], ny = y + d[1];
                if (inBounds(nx, ny) && maze[nx][ny] == 1 && !visited[nx][ny]) {
                    queue.add(new int[]{nx, ny});
                    visited[nx][ny] = true;
                    parentX[nx][ny] = x;
                    parentY[nx][ny] = y;
                }
            }
        }

        // reconstruct
        int x = ex, y = ey;
        while (x != -1 && y != -1) {
            path[x][y] = 1;
            int px = parentX[x][y], py = parentY[x][y];
            x = px;
            y = py;
        }

        return path;
    }

    private List<int[]> findDeadEnds(int[][] maze) {
        List<int[]> result = new ArrayList<>();
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int x = 1; x < mapWidth - 1; x++) {
            for (int y = 1; y < mapHeight - 1; y++) {
                if (maze[x][y] == 1) {
                    int count = 0;
                    for (int[] d : dirs) if (maze[x + d[0]][y + d[1]] == 1) count++;
                    if (count == 1) result.add(new int[]{x, y});
                }
            }
        }
        return result;
    }

    private List<int[]> suggestEnemySpawns(int[][] maze, int[][] exactPath, int minDist) {
        List<int[]> spawns = new ArrayList<>();
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        for (int x = 1; x < mapWidth - 1; x++) {
            for (int y = 1; y < mapHeight - 1; y++) {
                if (maze[x][y] == 1 && exactPath[x][y] == 0) {
                    if (Math.abs(x - startX) + Math.abs(y - startY) >= minDist &&
                            Math.abs(x - endX) + Math.abs(y - endY) >= minDist) {
                        int open = 0;
                        for (int[] d : dirs) if (maze[x + d[0]][y + d[1]] == 1) open++;
                        if (open >= 2) spawns.add(new int[]{x, y});
                    }
                }
            }
        }
        Collections.shuffle(spawns);
        return spawns.subList(0, Math.min(spawns.size(), 10));
    }

    private int countPathLength(int[][] path) {
        int count = 0;
        for (int[] row : path)
            for (int cell : row)
                if (cell == 1) count++;
        return count;
    }

    private int[][] generateMinimapMask(int[][] path) {
        int[][] mask = new int[mapWidth][mapHeight];
        for (int x = 0; x < mapWidth; x++)
            for (int y = 0; y < mapHeight; y++)
                mask[x][y] = (path[x][y] == 1) ? 1 : 0;
        return mask;
    }

    private boolean inBounds(int x, int y) {
        return x > 0 && y > 0 && x < mapWidth - 1 && y < mapHeight - 1;
    }
}
