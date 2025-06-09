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

    private final IsometricMap map;



    private int[][] npcPositions;

    public int[][] getNpcPositions() {
        return npcPositions;
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

    // Directions for movement: right, down, left, up to support 8 directions
    private final int[][] directions = {
            {0, 1},   // phải
            {1, 0},   // xuống
            {0, -1},  // trái
            {-1, 0},  // lên
            {1, 1},   // xuống phải (chéo)
            {1, -1},  // xuống trái (chéo)
            {-1, -1}, // lên trái (chéo)
            {-1, 1}   // lên phải (chéo)
    };
    // Right, Up, Left, Down

    public Pathfinder(IsometricMap map) {
        this.map = map;
    }

    public Array<int[]> findPath(int startX, int startY, int goalX, int goalY, int maxLength) {
        // If the target is not walkable, find the closest walkable tile
        if (!map.isWalkable(goalX, goalY) || isNPCHere(goalX, goalY)) {
            int[] closestWalkable = findClosestWalkable(goalX, goalY);
            goalX = closestWalkable[0];
            goalY = closestWalkable[1];
        }

        PriorityQueue<Node> openSet = new PriorityQueue<>();
        Set<String> closedSet = new HashSet<>();

        Node startNode = new Node(startX, startY);
        startNode.g = 0;
        startNode.h = heuristic(startX, startY, goalX, goalY);
        startNode.f = startNode.g + startNode.h;

        openSet.add(startNode);

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();

            // Check if we reached the goal
//            Nếu đến đích thì reconstruct lại đường đi.

            if (current.x == goalX && current.y == goalY) {
                return reconstructPath(current, maxLength);
            }
            String key = current.x + "," + current.y;
            closedSet.add(key);

            // Check all neighbors
            // Nếu không thể đi hoặc đã duyệt -> bỏ qua
            for (int[] dir : directions) {
                int nx = current.x + dir[0];
                int ny = current.y + dir[1];

                // Skip if outside map or unwalkable
                if (!map.isWalkable(nx, ny)) continue;

                String neighborKey = nx + "," + ny;
                if (closedSet.contains(neighborKey)) continue;

                // In the neighbor processing loop
                float moveCost = (dir[0] != 0 && dir[1] != 0) ? 1.414f : 1.0f; // Diagonal vs orthogonal
                float tentativeG = current.g + moveCost; // Cost is just distance of 1 per move

                Node neighbor = new Node(nx, ny);
                neighbor.parent = current;
                neighbor.g = tentativeG;
                neighbor.h = heuristic(nx, ny, goalX, goalY);
                neighbor.f = neighbor.g + neighbor.h;

                boolean found = false;
                for (Node node : openSet) {
                    if (node.x == nx && node.y == ny) {
                        found = true;
                        if (tentativeG < node.g) {
                            openSet.remove(node);
                            openSet.add(neighbor);
                        }
                        break;
                    }
                }

                if (!found) {
                    openSet.add(neighbor);
                }
            }
        }

        // No path found
        return new Array<>();
    }

    private int[] findClosestWalkable(int x, int y) {
        int searchRadius = 1;
        int maxSearchRadius = 10;

        while (searchRadius <= maxSearchRadius) {
            boolean found = false;
            for (int offsetY = -searchRadius; offsetY <= searchRadius && !found; offsetY++) {
                for (int offsetX = -searchRadius; offsetX <= searchRadius; offsetX++) {
                    if (Math.abs(offsetX) == searchRadius || Math.abs(offsetY) == searchRadius) {
                        int checkX = x + offsetX;
                        int checkY = y + offsetY;
                        if (map.isWalkable(checkX, checkY) || !isNPCHere(checkX, checkY)) {
                            return new int[]{checkX, checkY};
                        }
                    }
                }
            }
            searchRadius++;
        }

        return new int[]{x, y};
    }

    //    Octile Distance - better for 8-directional movement on a grid
    private float heuristic(int x1, int y1, int x2, int y2) {
        float dx = Math.abs(x1 - x2);
        float dy = Math.abs(y1 - y2);
        // sqrt(2) - 1 ≈ 0.414
        return (dx + dy) + (0.414f * Math.min(dx, dy));
    }


    private Array<int[]> reconstructPath(Node endNode, int maxLength) {
        Array<int[]> path = new Array<>();
        Node current = endNode;

        while (current != null && path.size < maxLength) {
            path.insert(0, new int[]{current.x, current.y});
            current = current.parent;
        }

        return path;
    }
}

