package ctu.game.isometric.model.typing;

import ctu.game.isometric.model.entity.Character;

import java.util.PriorityQueue;

public class EnemyDungeon {
    int id;
    int x, y;
    String word = "";
    boolean isActive = false;
    float speed = 0.1f;

    // Add movement delay fields
    private float moveDelay = 0.5f; // seconds per move
    private float moveTimer = 0f;

    public EnemyDungeon(int y, int x, int id) {
        this.y = y;
        this.x = x;
        this.id = id;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public boolean isTouchCharacter(Character character) {
        return character.getGridX() == x && character.getGridY() == y;
    }

    // Add delta parameter
    public void moveToCharacterAI(Character character, boolean[][] walkableTiles, float delta) {
        moveTimer += delta;
        if (moveTimer < moveDelay) return;
        moveTimer = 0f;

        int[] dx = {0, 1, 0, -1};
        int[] dy = {-1, 0, 1, 0};
        int width = walkableTiles[0].length;
        int height = walkableTiles.length;

        class Node implements Comparable<Node> {
            int x, y, g, f;
            Node parent;
            Node(int y, int x, int g, int f, Node parent) {
                this.x = x; this.y = y; this.g = g; this.f = f; this.parent = parent;
            }
            public int compareTo(Node o) { return Integer.compare(this.f, o.f); }
        }

        boolean[][] visited = new boolean[height][width];
        PriorityQueue<Node> open = new PriorityQueue<>();
        int targetX = (int) character.getGridX();
        int targetY = (int) character.getGridY();

        Node start = new Node(y, x, 0, Math.abs(x - targetX) + Math.abs(y - targetY), null);
        open.add(start);

        Node end = null;
        while (!open.isEmpty()) {
            Node curr = open.poll();
            if (curr.x == targetX && curr.y == targetY) {
                end = curr;
                break;
            }
            if (visited[curr.y][curr.x]) continue;
            visited[curr.y][curr.x] = true;
            for (int d = 0; d < 4; d++) {
                int nx = curr.x + dx[d];
                int ny = curr.y + dy[d];
                if (nx >= 0 && nx < width && ny >= 0 && ny < height && walkableTiles[ny][nx] && !visited[ny][nx]) {
                    int g = curr.g + 1;
                    int h = Math.abs(nx - targetX) + Math.abs(ny - targetY);
                    open.add(new Node(ny, nx, g, g + h, curr));
                }
            }
        }
        if (end != null && end.parent != null) {
            Node step = end;
            while (step.parent != null && step.parent.parent != null) {
                step = step.parent;
            }
            this.x = step.x;
            this.y = step.y;
        }
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }
}