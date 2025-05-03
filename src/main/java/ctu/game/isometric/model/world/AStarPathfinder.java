package ctu.game.isometric.model.world;
import java.util.*;

public class AStarPathfinder {
    private IsometricMap gameMap;
    private static final int[][] DIRECTIONS = {
            {1, 0}, {0, 1}, {-1, 0}, {0, -1},  // Cardinal
            {1, 1}, {-1, 1}, {-1, -1}, {1, -1}  // Diagonal
    };

    // Path caching
    private final Map<Long, List<GridPoint>> pathCache = new HashMap<>();
    private static final int MAX_CACHE_SIZE = 100;
    private static final long CACHE_EXPIRY_MS = 5000; // 5 seconds

    public AStarPathfinder(IsometricMap gameMap) {
        this.gameMap = gameMap;
    }

    public List<GridPoint> findPath(int startX, int startY, int goalX, int goalY) {
        // Check cache first
        long cacheKey = computePathKey(startX, startY, goalX, goalY);
        List<GridPoint> cachedPath = pathCache.get(cacheKey);
        if (cachedPath != null) {
            return new ArrayList<>(cachedPath); // Return a copy to prevent modification
        }

        // If start and goal are the same, return a single point
        if (startX == goalX && startY == goalY) {
            List<GridPoint> singlePoint = Collections.singletonList(new GridPoint(startX, startY));
            cachePath(cacheKey, singlePoint);
            return singlePoint;
        }

        // Early exit if goal is unwalkable
        if (!gameMap.isWalkable(goalX, goalY)) {
            return null;
        }

        PriorityQueue<Node> openSet = new PriorityQueue<>(32);
        boolean[][] closedSet = new boolean[gameMap.getMapWidth()][gameMap.getMapHeight()];
        Node[][] nodeGrid = new Node[gameMap.getMapWidth()][gameMap.getMapHeight()];

        Node startNode = new Node(startX, startY, null);
        startNode.g = 0;
        startNode.h = heuristic(startX, startY, goalX, goalY);
        startNode.f = startNode.h;

        openSet.add(startNode);
        nodeGrid[startX][startY] = startNode;

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();

            // If already processed this node, skip
            if (closedSet[current.x][current.y]) {
                continue;
            }

            // If we've reached the goal
            if (current.x == goalX && current.y == goalY) {
                List<GridPoint> path = reconstructPath(current);
                path = smoothPath(path);
                cachePath(cacheKey, path);
                return path;
            }

            closedSet[current.x][current.y] = true;

            // Check all neighbors
            for (int[] dir : DIRECTIONS) {
                int newX = current.x + dir[0];
                int newY = current.y + dir[1];

                // Skip if out of bounds or not walkable
                if (newX < 0 || newX >= gameMap.getMapWidth() ||
                        newY < 0 || newY >= gameMap.getMapHeight() ||
                        !gameMap.isWalkable(newX, newY) ||
                        closedSet[newX][newY]) {
                    continue;
                }

                // Calculate cost (diagonal movement costs more)
                float moveCost = (dir[0] != 0 && dir[1] != 0) ? 1.414f : 1.0f;
                float gScore = current.g + moveCost;

                Node neighbor = nodeGrid[newX][newY];
                if (neighbor == null) {
                    neighbor = new Node(newX, newY, current);
                    nodeGrid[newX][newY] = neighbor;
                }

                if (neighbor.f == 0 || gScore < neighbor.g) {
                    neighbor.parent = current;
                    neighbor.g = gScore;
                    neighbor.h = heuristic(newX, newY, goalX, goalY);
                    neighbor.f = neighbor.g + neighbor.h;

                    if (!openSet.contains(neighbor)) {
                        openSet.add(neighbor);
                    }
                }
            }
        }

        // No path found
        return null;
    }

    private float heuristic(int x1, int y1, int x2, int y2) {
        // Octile distance - better for grids with diagonal movement
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        return (dx + dy) + (1.414f - 2) * Math.min(dx, dy);
    }

    private List<GridPoint> reconstructPath(Node endNode) {
        List<GridPoint> path = new ArrayList<>();
        Node current = endNode;

        while (current != null) {
            path.add(0, new GridPoint(current.x, current.y));
            current = current.parent;
        }

        return path;
    }

    private List<GridPoint> smoothPath(List<GridPoint> path) {
        if (path == null || path.size() <= 2) {
            return path;
        }

        List<GridPoint> smoothedPath = new ArrayList<>();
        smoothedPath.add(path.get(0));

        int i = 0;
        while (i < path.size() - 1) {
            int furthestVisible = i;

            for (int j = path.size() - 1; j > i; j--) {
                if (lineOfSight(path.get(i).x, path.get(i).y, path.get(j).x, path.get(j).y)) {
                    furthestVisible = j;
                    break;
                }
            }

            i = furthestVisible;
            smoothedPath.add(path.get(i));
        }

        return smoothedPath;
    }

    private boolean lineOfSight(int x0, int y0, int x1, int y1) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        while (x0 != x1 || y0 != y1) {
            if (!gameMap.isWalkable(x0, y0)) {
                return false;
            }

            int e2 = 2 * err;
            if (e2 > -dy) {
                err = err - dy;
                x0 = x0 + sx;
            }
            if (e2 < dx) {
                err = err + dx;
                y0 = y0 + sy;
            }
        }

        return true;
    }

    private long computePathKey(int startX, int startY, int endX, int endY) {
        return ((long)startX << 48) | ((long)startY << 32) | ((long)endX << 16) | endY;
    }

    private void cachePath(long key, List<GridPoint> path) {
        // Manage cache size
        if (pathCache.size() >= MAX_CACHE_SIZE) {
            // Remove oldest entry
            pathCache.remove(pathCache.keySet().iterator().next());
        }

        pathCache.put(key, new ArrayList<>(path));

        // Schedule cleanup of this entry
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                pathCache.remove(key);
            }
        }, CACHE_EXPIRY_MS);
    }

    private static class Node implements Comparable<Node> {
        int x, y;
        Node parent;
        float g; // Cost from start to current
        float h; // Heuristic (estimated cost from current to goal)
        float f; // Total cost (g + h)

        public Node(int x, int y, Node parent) {
            this.x = x;
            this.y = y;
            this.parent = parent;
        }

        @Override
        public int compareTo(Node other) {
            return Float.compare(this.f, other.f);
        }
    }

    // Clear the cache when needed (e.g., when the map changes)
    public void clearCache() {
        pathCache.clear();
    }
}