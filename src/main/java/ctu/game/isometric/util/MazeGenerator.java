package ctu.game.isometric.util;

import ctu.game.isometric.model.world.MazeGenerationResult;

import java.util.*;

public class MazeGenerator {
    private static final int WALL = 0;
    private static final int FLOOR = 1;
    private static final int TILE_BLOCK = 2;
    private static final int[][] DIRS = {
            {0, -2}, {0, 2}, {-2, 0}, {2, 0}
    };
    public static MazeGenerationResult generateMaze(int width, int height, Random random, int difficulty) {
        difficulty = Math.max(0, Math.min(5, difficulty)); // clamp difficulty to [0, 5]
        int maxAttempts = 10; // Prevent infinite loop
        int attempts = 0;

        while (attempts < maxAttempts) {
            attempts++;
            int[][] baseLayer = new int[height][width];
            int[][] terrainLayer = new int[height][width];

            for (int y = 0; y < height; y++) Arrays.fill(baseLayer[y], WALL);

            int startX = randomOdd(width, random);
            int startY = randomOdd(height, random);
            baseLayer[startY][startX] = FLOOR;

            generateMazeBase(baseLayer, startX, startY, width, height, random);
            applyTerrainLayer(baseLayer, terrainLayer, width, height);

            List<int[]> floorTiles = collectTiles(baseLayer, FLOOR);

            int[] start, end;
            do {
                start = floorTiles.get(random.nextInt(floorTiles.size()));
                end = selectDeadEndPoint(baseLayer, floorTiles, start, width, height, random);
            } while (manhattan(start, end) < 10);

            List<int[]> path = aStarPath(baseLayer, start, end, width, height);
            if (path.size() < 10) continue;

            Set<String> pathSet = toSet(path);

            int fakeCount = 2 + random.nextInt(2) + difficulty / 2;
            int chestCount = Math.max(1, (6 + random.nextInt(3) - difficulty / 2));
            int enemyCount = 5 + random.nextInt(3) + difficulty / 2;

            List<int[]> fakeEndpoints = generateFakeEndpoints(floorTiles, baseLayer, pathSet, start, end, fakeCount, width, height, random);
            Set<String> fakeSet = toSet(fakeEndpoints);

            List<int[]> chests = generateChests(floorTiles, pathSet, fakeSet, start, baseLayer, chestCount, width, height, random);
            Set<String> chestSet = toSet(chests);

            List<int[]> enemies = generateEnemySpawnsRevised(floorTiles, path, pathSet, fakeSet, chestSet, start, end, baseLayer, enemyCount, width, height, random);

            int[][] minimapMask = generateMinimap(baseLayer, path, width, height);

            MazeGenerationResult result = new MazeGenerationResult();
            result.layers.put("base", baseLayer);
            result.layers.put("terrain", terrainLayer);
            result.layers.put("path", toArray(path));
            result.layers.put("fake", toArray(fakeEndpoints));
            result.layers.put("chest", toArray(chests));
            result.layers.put("enemy", toArray(enemies));
            result.minimapMask = minimapMask;
            result.pathLength = path.size();
            result.startX = start[0];
            result.startY = start[1];
            result.endX = end[0];
            result.endY = end[1];
            result.enemySpawns = enemies;

            return result;
        }

        // If max attempts reached, create a simple default maze
        return createFallbackMaze(width, height, random, difficulty);
    }


    private static int[] selectDeadEndPoint(int[][] baseLayer, List<int[]> floorTiles, int[] start, int width, int height, Random random) {
        List<int[]> deadEnds = new ArrayList<>();

        for (int[] tile : floorTiles) {
            int floorNeighbors = 0;
            for (int[] dir : new int[][]{{0,1},{1,0},{-1,0},{0,-1}}) {
                int nx = tile[0] + dir[0];
                int ny = tile[1] + dir[1];
                if (inBounds(nx, ny, width, height) && baseLayer[ny][nx] == FLOOR) {
                    floorNeighbors++;
                }
            }

            if (floorNeighbors == 1 && !Arrays.equals(tile, start)) {
                deadEnds.add(tile);
            }
        }

        if (deadEnds.isEmpty()) {
            return findCornerPoint(floorTiles, baseLayer, start, width, height, random);
        }

        int[] farthest = null;
        int maxDist = -1;

        for (int[] deadEnd : deadEnds) {
            int dist = manhattan(deadEnd, start);
            if (dist > maxDist) {
                maxDist = dist;
                farthest = deadEnd;
            }
        }

        return farthest;
    }

    private static int[] findCornerPoint(List<int[]> floorTiles, int[][] baseLayer, int[] avoid, int width, int height, Random random) {
        List<int[]> cornerTiles = new ArrayList<>();

        for (int[] tile : floorTiles) {
            if (!Arrays.equals(tile, avoid)) {
                int wallCount = 0;
                for (int[] dir : new int[][]{{0,1},{1,0},{-1,0},{0,-1}}) {
                    int nx = tile[0] + dir[0];
                    int ny = tile[1] + dir[1];
                    if (nx < 0 || ny < 0 || nx >= width || ny >= height || baseLayer[ny][nx] != FLOOR) {
                        wallCount++;
                    }
                }

                if (wallCount >= 2) {
                    cornerTiles.add(tile);
                }
            }
        }

        cornerTiles.sort((a, b) -> Integer.compare(
                manhattan(b, avoid), manhattan(a, avoid)));

        return cornerTiles.isEmpty() ?
                floorTiles.get(random.nextInt(floorTiles.size())) :
                cornerTiles.get(0);
    }
    private static List<int[]> generateEnemySpawnsRevised(List<int[]> tiles, List<int[]> path, Set<String> pathSet, Set<String> fakeSet,
                                                          Set<String> chestSet, int[] start, int[] end,
                                                          int[][] base, int count, int width, int height, Random random) {
        List<int[]> enemies = new ArrayList<>();
        int minEnemyDistance = 5; // Minimum distance between enemies to make them sparse

        // Place 60-70% of enemies on the correct path, avoiding start and end areas
        int pathEnemies = (int)(count * (0.6 + random.nextDouble() * 0.1));
        int offPathEnemies = count - pathEnemies;

        // Filter path to avoid start and end areas
        List<int[]> validPathTiles = new ArrayList<>();
        for (int[] p : path) {
            if (manhattan(p, start) >= 5 && manhattan(p, end) >= 5) {
                validPathTiles.add(p);
            }
        }

        // Ensure we have enough valid path tiles
        if (validPathTiles.size() < pathEnemies) {
            pathEnemies = validPathTiles.size();
            offPathEnemies = count - pathEnemies;
        }

        // Place enemies on path with spacing
        while (enemies.size() < pathEnemies && !validPathTiles.isEmpty()) {
            int idx = random.nextInt(validPathTiles.size());
            int[] p = validPathTiles.remove(idx);

            // Check minimum distance from other enemies
            boolean tooClose = false;
            for (int[] enemy : enemies) {
                if (manhattan(p, enemy) < minEnemyDistance) {
                    tooClose = true;
                    break;
                }
            }

            if (!tooClose && !chestSet.contains(key(p))) {
                enemies.add(p);
            }
        }

        // Place remaining enemies off the path with spacing
        int attempts = 0;
        int maxAttempts = 200;
        while (enemies.size() < count && attempts < maxAttempts) {
            attempts++;
            int[] p = tiles.get(random.nextInt(tiles.size()));
            String k = key(p);

            if (!pathSet.contains(k) && !fakeSet.contains(k) && !chestSet.contains(k) && !contains(enemies, p)) {
                if (manhattan(p, start) >= 4 && manhattan(p, end) >= 4 &&
                        isReachable(base, start, p, width, height)) {

                    // Check minimum distance from other enemies
                    boolean tooClose = false;
                    for (int[] enemy : enemies) {
                        if (manhattan(p, enemy) < minEnemyDistance) {
                            tooClose = true;
                            break;
                        }
                    }

                    if (!tooClose) {
                        enemies.add(p);
                    }
                }
            }
        }

        return enemies;
    }

    private static MazeGenerationResult createFallbackMaze(int width, int height, Random random, int difficulty) {
        int[][] baseLayer = new int[height][width];
        int[][] terrainLayer = new int[height][width];
        for (int y = 0; y < height; y++) {
            Arrays.fill(baseLayer[y], WALL);
            Arrays.fill(terrainLayer[y], TILE_BLOCK);
        }

        List<int[]> path = new ArrayList<>();
        int type = random.nextInt(3); // 0: LINE, 1: ZIGZAG, 2: U_SHAPE

        switch (type) {
            case 0: { // LINE
                int py = height / 2;
                for (int x = 1; x < width - 1; x++) {
                    baseLayer[py][x] = FLOOR;
                    terrainLayer[py][x] = 0;
                    path.add(new int[]{x, py});
                }
                break;
            }
            case 1: { // ZIGZAG
                int x = 1, y = 1;
                boolean down = true;
                while (x < width - 1 && y < height - 1) {
                    baseLayer[y][x] = FLOOR;
                    terrainLayer[y][x] = 0;
                    path.add(new int[]{x, y});
                    if (down) {
                        if (y + 1 >= height - 1) { x++; down = false; }
                        else y++;
                    } else {
                        if (y - 1 <= 0) { x++; down = true; }
                        else y--;
                    }
                }
                break;
            }
            case 2: { // U_SHAPE
                int topY = 1;
                int bottomY = height - 2;
                for (int x = 1; x < width - 1; x++) {
                    int y = (x < width / 2) ? topY : bottomY;
                    baseLayer[y][x] = FLOOR;
                    terrainLayer[y][x] = 0;
                    path.add(new int[]{x, y});
                }
                // nối đoạn dọc
                for (int y = topY; y <= bottomY; y++) {
                    baseLayer[y][width / 2] = FLOOR;
                    terrainLayer[y][width / 2] = 0;
                    path.add(new int[]{width / 2, y});
                }
                break;
            }
        }

        int[] start = path.get(0);
        int[] end = path.get(path.size() - 1);

        int[][] minimapMask = new int[height][width];
        for (int[] p : path) minimapMask[p[1]][p[0]] = 1;

        MazeGenerationResult result = new MazeGenerationResult();
        result.layers.put("base", baseLayer);
        result.layers.put("terrain", terrainLayer);
        result.layers.put("path", toArray(path));
        result.minimapMask = minimapMask;
        result.pathLength = path.size();
        result.startX = start[0];
        result.startY = start[1];
        result.endX = end[0];
        result.endY = end[1];

        return result;
    }


    private static void generateMazeBase(int[][] base, int startX, int startY, int width, int height, Random random) {
        List<int[]> frontier = new ArrayList<>();
        for (int[] dir : DIRS) {
            int nx = startX + dir[0], ny = startY + dir[1];
            if (inBounds(nx, ny, width, height)) frontier.add(new int[]{nx, ny, startX, startY});
        }

        while (!frontier.isEmpty()) {
            int[] f = frontier.remove(random.nextInt(frontier.size()));
            int x = f[0], y = f[1], fx = f[2], fy = f[3];
            if (base[y][x] == WALL) {
                base[y][x] = FLOOR;
                base[(y + fy) / 2][(x + fx) / 2] = FLOOR;
                for (int[] dir : DIRS) {
                    int nx = x + dir[0], ny = y + dir[1];
                    if (inBounds(nx, ny, width, height) && base[ny][nx] == WALL)
                        frontier.add(new int[]{nx, ny, x, y});
                }
            }
        }
    }

    private static void applyTerrainLayer(int[][] base, int[][] terrain, int width, int height) {
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                terrain[y][x] = (base[y][x] == FLOOR) ? 0 : TILE_BLOCK;
    }

    private static List<int[]> generateFakeEndpoints(List<int[]> floorTiles, int[][] baseLayer,
                                                     Set<String> pathSet, int[] start, int[] end,
                                                     int count, int width, int height, Random random) {
        List<int[]> result = new ArrayList<>();
        List<int[]> deadEnds = new ArrayList<>();

        // Find all dead-end floor tiles
        for (int[] tile : floorTiles) {
            String key = key(tile);
            if (!pathSet.contains(key) && !Arrays.equals(tile, start) && !Arrays.equals(tile, end)) {
                int floorNeighbors = 0;
                for (int[] dir : new int[][]{{0,1},{1,0},{-1,0},{0,-1}}) {
                    int nx = tile[0] + dir[0];
                    int ny = tile[1] + dir[1];
                    if (inBounds(nx, ny, width, height) && baseLayer[ny][nx] == FLOOR) {
                        floorNeighbors++;
                    }
                }

                if (floorNeighbors == 1) {
                    deadEnds.add(tile);
                }
            }
        }

        // Sort dead ends by distance from both start and real end
        deadEnds.sort((a, b) -> {
            int distA = manhattan(a, start) + manhattan(a, end);
            int distB = manhattan(b, start) + manhattan(b, end);
            return Integer.compare(distB, distA); // Higher distance first
        });

        // Add dead ends to result, ensuring minimum distance between points
        int minDistance = 8; // Minimum distance between fake endpoints

        for (int[] candidate : deadEnds) {
            if (result.size() >= count) break;

            // Check distance from real end
            if (manhattan(candidate, end) < minDistance) continue;

            // Check distance from other fake ends
            boolean tooClose = false;
            for (int[] existing : result) {
                if (manhattan(candidate, existing) < minDistance) {
                    tooClose = true;
                    break;
                }
            }

            if (!tooClose) {
                result.add(candidate);
            }
        }

        // If we couldn't find enough dead ends, fall back to corners
        if (result.size() < count) {
            List<int[]> cornerTiles = findCornerTiles(floorTiles, baseLayer, pathSet, result, start, end, width, height);

            for (int[] corner : cornerTiles) {
                if (result.size() >= count) break;

                boolean tooClose = false;
                for (int[] existing : result) {
                    if (manhattan(corner, existing) < minDistance) {
                        tooClose = true;
                        break;
                    }
                }

                if (!tooClose && manhattan(corner, end) >= minDistance) {
                    result.add(corner);
                }
            }
        }

        // Last resort - use original method
        while (result.size() < count) {
            int[] p = floorTiles.get(random.nextInt(floorTiles.size()));
            if (!contains(result, p) && !Arrays.equals(p, start) && !Arrays.equals(p, end)) {
                result.add(p);
            }
        }

        return result;
    }
    private static List<int[]> findCornerTiles(List<int[]> floorTiles, int[][] baseLayer, Set<String> pathSet, List<int[]> result,
                                               int[] start, int[] end, int width, int height) {
        List<int[]> cornerTiles = new ArrayList<>();
        for (int[] tile : floorTiles) {
            String key = key(tile);
            if (!pathSet.contains(key) && !contains(result, tile) && !Arrays.equals(tile, start) && !Arrays.equals(tile, end)) {
                int wallCount = 0;
                for (int[] dir : new int[][]{{0,1},{1,0},{-1,0},{0,-1}}) {
                    int nx = tile[0] + dir[0];
                    int ny = tile[1] + dir[1];
                    if (!inBounds(nx, ny, width, height) || baseLayer[ny][nx] != FLOOR) {
                        wallCount++;
                    }
                }
                if (wallCount >= 2) {
                    cornerTiles.add(tile);
                }
            }
        }
        return cornerTiles;
    }
    private static List<int[]> generateChests(List<int[]> tiles, Set<String> pathSet, Set<String> fakeSet,
                                              int[] start, int[][] base, int count, int width, int height, Random random) {
        List<int[]> chests = new ArrayList<>();
        while (chests.size() < count) {
            int[] p = tiles.get(random.nextInt(tiles.size()));
            String key = key(p);
            if (!pathSet.contains(key) && !fakeSet.contains(key) && !contains(chests, p)
                    && isReachable(base, start, p, width, height)) {

                boolean nearPath = false;
                for (int[] pathTile : pathSetToList(pathSet)) {
                    if (manhattan(p, pathTile) <=3) {
                        nearPath = true;
                        break;
                    }
                }
                if (nearPath) {
                    chests.add(p);
                }
            }
        }
        return chests;
    }



    private static int[][] generateMinimap(int[][] base, List<int[]> path, int width, int height) {
        int[][] map = new int[height][width];
        for (int y = 0; y < height; y++) Arrays.fill(map[y], -1);
        for (int[] p : path) map[p[1]][p[0]] = 1;
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                if (base[y][x] == WALL) map[y][x] = 0;
        return map;
    }

    private static boolean isReachable(int[][] maze, int[] from, int[] to, int width, int height) {
        boolean[][] visited = new boolean[height][width];
        Queue<int[]> queue = new LinkedList<>();
        queue.add(from);
        visited[from[1]][from[0]] = true;

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            if (Arrays.equals(curr, to)) return true;
            for (int[] d : new int[][]{{0,1},{1,0},{-1,0},{0,-1}}) {
                int nx = curr[0] + d[0], ny = curr[1] + d[1];
                if (inBounds(nx, ny, width, height) && !visited[ny][nx] && maze[ny][nx] == FLOOR) {
                    visited[ny][nx] = true;
                    queue.add(new int[]{nx, ny});
                }
            }
        }
        return false;
    }

    private static List<int[]> collectTiles(int[][] layer, int value) {
        List<int[]> list = new ArrayList<>();
        for (int y = 0; y < layer.length; y++)
            for (int x = 0; x < layer[0].length; x++)
                if (layer[y][x] == value)
                    list.add(new int[]{x, y});
        return list;
    }

    private static List<int[]> pathSetToList(Set<String> set) {
        List<int[]> list = new ArrayList<>();
        for (String s : set) {
            String[] parts = s.split(",");
            list.add(new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])});
        }
        return list;
    }

    private static List<int[]> aStarPath(int[][] maze, int[] start, int[] end, int width, int height) {
        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingInt(n -> n.f));
        Map<String, Node> allNodes = new HashMap<>();

        Node startNode = new Node(start[0], start[1], null, 0, manhattan(start, end));
        open.add(startNode);
        allNodes.put(startNode.key(), startNode);

        while (!open.isEmpty()) {
            Node current = open.poll();
            if (current.x == end[0] && current.y == end[1]) {
                List<int[]> path = new ArrayList<>();
                while (current != null) {
                    path.add(0, new int[]{current.x, current.y});
                    current = current.parent;
                }
                return path;
            }

            for (int[] d : new int[][]{{0,1},{1,0},{-1,0},{0,-1}}) {
                int nx = current.x + d[0], ny = current.y + d[1];
                if (inBounds(nx, ny, width, height) && maze[ny][nx] == FLOOR) {
                    int g = current.g + 1;
                    String key = nx + "," + ny;
                    Node neighbor = allNodes.getOrDefault(key, new Node(nx, ny, null, Integer.MAX_VALUE, manhattan(new int[]{nx, ny}, end)));
                    if (g < neighbor.g) {
                        neighbor.g = g;
                        neighbor.f = g + neighbor.h;
                        neighbor.parent = current;
                        allNodes.put(key, neighbor);
                        open.add(neighbor);
                    }
                }
            }
        }
        return new ArrayList<>();
    }

    private static boolean inBounds(int x, int y, int width, int height) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    private static int manhattan(int[] a, int[] b) {
        return Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]);
    }

    private static int randomOdd(int max, Random rand) {
        int r = rand.nextInt(max / 2) * 2 + 1;
        return Math.min(r, max - 1);
    }

    private static int[][] toArray(List<int[]> list) {
        int[][] arr = new int[list.size()][2];
        for (int i = 0; i < list.size(); i++) {
            arr[i][0] = list.get(i)[0];
            arr[i][1] = list.get(i)[1];
        }
        return arr;
    }

    private static Set<String> toSet(List<int[]> list) {
        Set<String> set = new HashSet<>();
        for (int[] p : list) set.add(key(p));
        return set;
    }

    private static boolean contains(List<int[]> list, int[] point) {
        for (int[] p : list)
            if (Arrays.equals(p, point)) return true;
        return false;
    }

    private static String key(int[] p) {
        return p[0] + "," + p[1];
    }

    private static class Node {
        int x, y, g, h, f;
        Node parent;

        Node(int x, int y, Node parent, int g, int h) {
            this.x = x;
            this.y = y;
            this.parent = parent;
            this.g = g;
            this.h = h;
            this.f = g + h;
        }

        String key() {
            return x + "," + y;
        }
    }
}
