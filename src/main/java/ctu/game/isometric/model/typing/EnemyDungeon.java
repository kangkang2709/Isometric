package ctu.game.isometric.model.typing;

import ctu.game.isometric.model.entity.Character;

import java.util.PriorityQueue;

public class EnemyDungeon {
    int id;
    int x, y; // grid position
    float posX, posY; // smooth position for rendering
    int targetX, targetY; // next grid cell to move to
    String word = "";
    boolean isActive = false;
    float speed = 3.0f; // tiles per second
    public float stateTime = 0f;

    private boolean moving = false;
    String direction = "down"; // Default direction

    public EnemyDungeon(int y, int x, int id) {
        this.y = y;
        this.x = x;
        this.posX = x;
        this.posY = y;
        this.targetX = x;
        this.targetY = y;
        this.id = id;
    }

    private void updateDirection(float dx, float dy) {
        if (Math.abs(dx) > Math.abs(dy)) {
            direction = dx > 0 ? "right" : "left";
        } else if (Math.abs(dy) > 0) {
            direction = dy > 0 ? "down" : "up";
        }
    }

    public void moveToCharacterAI(Character character, boolean[][] walkableTiles, float delta) {
            // Find path to character
            int[] dx = {0, 1, 0, -1};
            int[] dy = {-1, 0, 1, 0};
            int width = walkableTiles[0].length;
            int height = walkableTiles.length;

            class Node implements Comparable<Node> {
                int x, y, g, f;
                Node parent;
                Node(int y, int x, int g, int f, Node parent) {
                    this.x = x;
                    this.y = y;
                    this.g = g;
                    this.f = f;
                    this.parent = parent;
                }
                public int compareTo(Node o) {
                    return Integer.compare(this.f, o.f);
                }
            }

            boolean[][] visited = new boolean[height][width];
            PriorityQueue<Node> open = new PriorityQueue<>();
            int targetGX = (int) character.getGridX();
            int targetGY = (int) character.getGridY();

            Node start = new Node(y, x, 0, Math.abs(x - targetGX) + Math.abs(y - targetGY), null);
            open.add(start);

            Node end = null;
            while (!open.isEmpty()) {
                Node curr = open.poll();
                if (curr.x == targetGX && curr.y == targetGY) {
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
                        int h = Math.abs(nx - targetGX) + Math.abs(ny - targetGY);
                        open.add(new Node(ny, nx, g, g + h, curr));
                    }
                }
            }
            if (end != null && end.parent != null) {
                Node step = end;
                while (step.parent != null && step.parent.parent != null) {
                    step = step.parent;
                }
                targetX = step.x;
                targetY = step.y;
                moving = true;
                updateDirection(targetX - x, targetY - y);
            }


        if (moving) {
            float dxx = targetX - posX;
            float dxy = targetY - posY;
            float dist = (float) Math.sqrt(dxx * dxx + dxy * dxy);
            float moveDist = speed * delta;
            if (dist <= moveDist) {
                posX = targetX;
                posY = targetY;
                x = targetX;
                y = targetY;
                moving = false;
            } else {
                posX += (dxx / dist) * moveDist;
                posY += (dxy / dist) * moveDist;
            }
        }
    }

    public boolean isTouchCharacter(Character character) {
        return character.getGridX() == x && character.getGridY() == y;
    }

    public float getRenderX() {
        return posX;
    }

    public float getRenderY() {
        return posY;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }
}