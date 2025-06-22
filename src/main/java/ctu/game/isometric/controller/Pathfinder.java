package ctu.game.isometric.controller;

import com.badlogic.gdx.utils.Array;
import ctu.game.isometric.model.world.IsometricMap;

import java.util.*;

public class Pathfinder {
    private static class Node implements Comparable<Node> {
        int x, y;
        Node parent;
        float g; // cost from start
        float h; // heuristic (estimate to goal)
        float f; // total cost (g + h)

        Node(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Node node = (Node) o;
            return x == node.x && y == node.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }

        @Override
        public int compareTo(Node other) {
            return Float.compare(this.f, other.f);
        }
    }

    private  IsometricMap map;

    private int[][] npcPositions;

    // Directions for movement: right, down, left, up and diagonals
    private static final int[][] DIRECTIONS = {
            {0, 1},   // right
            {1, 0},   // down
            {0, -1},  // left
            {-1, 0},  // up
            {1, 1},   // down-right
            {1, -1},  // down-left
            {-1, -1}, // up-left
            {-1, 1}   // up-right
    };

    // Precalculated move costs
    private static final float DIAGONAL_COST = 1.414f;
    private static final float STRAIGHT_COST = 1.0f;
    private static final float[] MOVE_COSTS = {
            STRAIGHT_COST, STRAIGHT_COST, STRAIGHT_COST, STRAIGHT_COST,
            DIAGONAL_COST, DIAGONAL_COST, DIAGONAL_COST, DIAGONAL_COST
    };

    public Pathfinder(IsometricMap map) {
        this.map = map;
    }



    public void setNpcPositions(int[][] npcPositions) {
        this.npcPositions = npcPositions;
    }

    private boolean isNPCHere(int x, int y) {
        if (npcPositions == null) {
            return false;
        }
        return Arrays.stream(npcPositions)
                .anyMatch(pos -> pos[0] == x && pos[1] == y);
    }

    public Array<int[]> findPath(int startX, int startY, int goalX, int goalY, int maxLength) {

        // If the target is not walkable, find the closest walkable tile
        if (!map.isWalkable(goalX, goalY) || isNPCHere(goalX, goalY)) {
            int[] closestWalkable = findClosestWalkable(goalX, goalY, startX, startY);
            if (closestWalkable == null) {
                Array<int[]> fallbackPath = new Array<>();
                fallbackPath.add(new int[]{startX, startY}); // Return starting position as fallback
                return fallbackPath;
            }
            goalX = closestWalkable[0];
            goalY = closestWalkable[1];
        }

        PriorityQueue<Node> openSet = new PriorityQueue<>();
        Map<Long, Node> openMap = new HashMap<>();  // Efficient lookup by coordinates
        Set<Long> closedSet = new HashSet<>();

        Node startNode = new Node(startX, startY);
        startNode.g = 0;
        startNode.h = heuristic(startX, startY, goalX, goalY);
        startNode.f = startNode.g + startNode.h;

        openSet.add(startNode);
        openMap.put(packCoords(startX, startY), startNode);

        final int MAX_NODE_EXPANSION = 30; // Limit the number of nodes explored
        int expandedCount = 0;

        // Determine how many directions to check based on map name
        int directionsToCheck = "board".equals(map.getMapName()) ? 4 : DIRECTIONS.length;

        while (!openSet.isEmpty()) {
            if (expandedCount++ > MAX_NODE_EXPANSION) {
                Array<int[]> fallbackPath = new Array<>();
                fallbackPath.add(new int[]{startX, startY});
                System.out.println("MAX NODE");// Return starting position as fallback
                return fallbackPath;
            }
            Node current = openSet.poll();
            long currentKey = packCoords(current.x, current.y);
            openMap.remove(currentKey);

            // Check if we reached the goal
            if (current.x == goalX && current.y == goalY) {
                return reconstructPath(current, maxLength);
            }

            closedSet.add(currentKey);

            // Check neighbors - only 4 directions for "board" map, all 8 for others
            for (int i = 0; i < directionsToCheck; i++) {
                int nx = current.x + DIRECTIONS[i][0];
                int ny = current.y + DIRECTIONS[i][1];

                // Skip if outside map, unwalkable, or has NPC
                if (!map.isWalkable(nx, ny) || isNPCHere(nx, ny)) continue;

                long neighborKey = packCoords(nx, ny);
                if (closedSet.contains(neighborKey)) continue;

                float tentativeG = current.g + MOVE_COSTS[i];
                Node neighborNode = openMap.get(neighborKey);

                if (neighborNode == null) {
                    // New node discovery
                    neighborNode = new Node(nx, ny);
                    neighborNode.parent = current;
                    neighborNode.g = tentativeG;
                    neighborNode.h = heuristic(nx, ny, goalX, goalY);
                    neighborNode.f = neighborNode.g + neighborNode.h;

                    openSet.add(neighborNode);
                    openMap.put(neighborKey, neighborNode);
                } else if (tentativeG < neighborNode.g) {
                    // Better path found to existing node
                    openSet.remove(neighborNode);
                    neighborNode.parent = current;
                    neighborNode.g = tentativeG;
                    neighborNode.f = tentativeG + neighborNode.h;
                    openSet.add(neighborNode);
                }
            }
        }

        // No path found
        Array<int[]> fallbackPath = new Array<>();
        fallbackPath.add(new int[]{startX, startY}); // Return starting position as fallback
        return fallbackPath;
    }

    private int[] findClosestWalkable(int x, int y, int startX, int startY) {
        // Check if the current tile is walkable
//        if (map.isWalkable(x, y) && !isNPCHere(x, y)) {
//            return new int[]{x, y};
//        }

        // Check immediate neighbors (1-tile radius)
        int[] closestTile = null;
        double closestDistance = Double.MAX_VALUE;
        System.out.println(map.getMapName());
        // Determine how many directions to check based on map name
        int directionsToCheck = "board".equals(map.getMapName()) ? 4 : DIRECTIONS.length;

        for (int i = 0; i < directionsToCheck; i++) {
            int[] direction = DIRECTIONS[i];
            int nx = x + direction[0];
            int ny = y + direction[1];

            if (map.isWalkable(nx, ny) && !isNPCHere(nx, ny)) {
                double distance = Math.sqrt(Math.pow(nx - startX, 2) + Math.pow(ny - startY, 2));
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestTile = new int[]{nx, ny};
                }
            }
        }

        // Return closest tile found or null if none was found
        return closestTile;
    }

    // Pack coordinates into a single long for efficient hashing
    private long packCoords(int x, int y) {
        return ((long) x << 32) | (y & 0xFFFFFFFFL);
    }

    // Octile Distance heuristic for 8-directional movement
    private float heuristic(int x1, int y1, int x2, int y2) {
        float dx = Math.abs(x1 - x2);
        float dy = Math.abs(y1 - y2);
        return (dx + dy) + (0.414f * Math.min(dx, dy));
    }

    private Array<int[]> reconstructPath(Node endNode, int maxLength) {
        Array<int[]> path = new Array<>(Math.min(maxLength, 20)); // Preallocate with reasonable size
        Node current = endNode;

        while (current != null && path.size < maxLength) {
            path.insert(0, new int[]{current.x, current.y});
            current = current.parent;
        }

        return path;
    }

    public IsometricMap getMap() {
        return map;
    }
    public void setMap(IsometricMap map) {
        this.map = map;
    }
}