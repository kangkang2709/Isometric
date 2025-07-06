package ctu.game.isometric.util;

import ctu.game.isometric.model.world.MazeGenerationResult;

import java.util.*;

public class MazeGenerator {
    private static final int WIDTH = 21;
    private static final int HEIGHT = 21;

    private static final int WALL = 0;
    private static final int FLOOR = 1;

    private static final int TILE_BLOCK = 2;

    private static final Random random = new Random();

    private static final int[][] DIRS = {
            {0, -2}, {0, 2}, {-2, 0}, {2, 0}
    };

    public static MazeGenerationResult generateMaze() {
        int[][] baseLayer = new int[HEIGHT][WIDTH];
        int[][] terrainLayer = new int[HEIGHT][WIDTH];

        for (int y = 0; y < HEIGHT; y++)
            Arrays.fill(baseLayer[y], WALL);

        int startX = randomOdd(WIDTH);
        int startY = randomOdd(HEIGHT);
        baseLayer[startY][startX] = FLOOR;

        List<int[]> frontier = new ArrayList<>();
        for (int[] dir : DIRS) {
            int nx = startX + dir[0];
            int ny = startY + dir[1];
            if (inBounds(nx, ny))
                frontier.add(new int[]{nx, ny, startX, startY});
        }

        while (!frontier.isEmpty()) {
            int[] f = frontier.remove(random.nextInt(frontier.size()));
            int x = f[0], y = f[1], fx = f[2], fy = f[3];
            if (baseLayer[y][x] == WALL) {
                baseLayer[y][x] = FLOOR;
                baseLayer[(y + fy) / 2][(x + fx) / 2] = FLOOR;
                for (int[] dir : DIRS) {
                    int nx = x + dir[0];
                    int ny = y + dir[1];
                    if (inBounds(nx, ny) && baseLayer[ny][nx] == WALL) {
                        frontier.add(new int[]{nx, ny, x, y});
                    }
                }
            }
        }

        for (int y = 0; y < HEIGHT; y++)
            for (int x = 0; x < WIDTH; x++)
                terrainLayer[y][x] = (baseLayer[y][x] == FLOOR) ? 0 : TILE_BLOCK;

        List<int[]> floorTiles = collectTiles(baseLayer, FLOOR);
        int[] start, end;
        do {
            start = floorTiles.get(random.nextInt(floorTiles.size()));
            end = floorTiles.get(random.nextInt(floorTiles.size()));
        } while (Arrays.equals(start, end) || manhattan(start, end) < 10);

        List<int[]> path = aStarPath(baseLayer, start, end);

        // Fake endpoints
        List<int[]> fake = new ArrayList<>();
        int fakeCount = 2 + random.nextInt(2);
        int fakesAdded = 0;
        while (fakesAdded < fakeCount) {
            int[] fakeEnd = floorTiles.get(random.nextInt(floorTiles.size()));
            if (!contains(path, fakeEnd) && !contains(fake, fakeEnd)) {
                fake.add(fakeEnd);
                fakesAdded++;
            }
        }

        // Chests
        List<int[]> chest = new ArrayList<>();
        int chestCount = 3 + random.nextInt(3);
        int chestsPlaced = 0;
        while (chestsPlaced < chestCount) {
            int[] c = floorTiles.get(random.nextInt(floorTiles.size()));
            if (!contains(path, c) && !contains(fake, c) && !contains(chest, c)) {
                chest.add(c);
                chestsPlaced++;
            }
        }

        // Minimap
        int[][] minimapMask = new int[HEIGHT][WIDTH];
        for (int y = 0; y < HEIGHT; y++)
            Arrays.fill(minimapMask[y], -1);
        for (int[] p : path)
            minimapMask[p[1]][p[0]] = 1;
        for (int y = 0; y < HEIGHT; y++)
            for (int x = 0; x < WIDTH; x++)
                if (baseLayer[y][x] == WALL)
                    minimapMask[y][x] = 0;

        // Enemy spawns
        List<int[]> enemySpawns = new ArrayList<>();
        for (int i = 1; i < path.size() - 1; i++) {
            if (random.nextFloat() < 0.2f) {
                enemySpawns.add(path.get(i));
            }
        }

        // Output
        MazeGenerationResult result = new MazeGenerationResult();
        result.layers.put("base", baseLayer);
        result.layers.put("terrain", terrainLayer);
        result.layers.put("path", toArray(path));
        result.layers.put("fake", toArray(fake));
        result.layers.put("chest", toArray(chest));
        result.minimapMask = minimapMask;
        result.enemySpawns = enemySpawns;
        result.pathLength = path.size();
        result.startX = start[0];
        result.startY = start[1];
        result.endX = end[0];
        result.endY = end[1];

        return result;
    }

    private static boolean contains(List<int[]> list, int[] point) {
        for (int[] p : list) {
            if (Arrays.equals(p, point)) return true;
        }
        return false;
    }

    private static int[][] toArray(List<int[]> list) {
        int[][] arr = new int[list.size()][2];
        for (int i = 0; i < list.size(); i++) {
            arr[i][0] = list.get(i)[0];
            arr[i][1] = list.get(i)[1];
        }
        return arr;
    }


    private static int randomOdd(int max) {
        int r = random.nextInt(max / 2) * 2 + 1;
        return Math.min(r, max - 1);
    }

    private static boolean inBounds(int x, int y) {
        return x > 0 && y > 0 && x < WIDTH - 1 && y < HEIGHT - 1;
    }

    private static int manhattan(int[] a, int[] b) {
        return Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]);
    }

    private static List<int[]> collectTiles(int[][] layer, int value) {
        List<int[]> list = new ArrayList<>();
        for (int y = 0; y < layer.length; y++)
            for (int x = 0; x < layer[0].length; x++)
                if (layer[y][x] == value)
                    list.add(new int[]{x, y});
        return list;
    }

    private static List<int[]> aStarPath(int[][] maze, int[] start, int[] end) {
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

            for (int[] d : new int[][]{{0, 1}, {1, 0}, {-1, 0}, {0, -1}}) {
                int nx = current.x + d[0], ny = current.y + d[1];
                if (inBounds(nx, ny) && maze[ny][nx] == FLOOR) {
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
