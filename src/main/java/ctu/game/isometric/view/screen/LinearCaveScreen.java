package ctu.game.isometric.view.screen;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.*;
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch;
import com.badlogic.gdx.graphics.g3d.environment.*;
import com.badlogic.gdx.graphics.g3d.utils.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.StringBuilder;
import ctu.game.isometric.IsometricGame;
import ctu.game.isometric.controller.GameController;
import ctu.game.isometric.controller.dungeon.DungeonInputController;

import net.mgsx.gltf.loaders.glb.GLBLoader;
import net.mgsx.gltf.loaders.gltf.GLTFLoader;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import net.mgsx.gltf.scene3d.scene.SceneManager;
import net.mgsx.gltf.scene3d.shaders.PBRShaderConfig;
import net.mgsx.gltf.scene3d.shaders.PBRShaderProvider;

import java.util.*;
import java.util.Queue;

public class LinearCaveScreen implements Screen {
    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private Environment environment;
    private Array<ModelInstance> instances = new Array<>();
    private Model boxModel;
    private Model ceilingModel;
    private final int MAP_WIDTH = 15;
    private final int MAP_HEIGHT = 15;
    private int[][] map;
    private final float PLAYER_SCALE = 0.5f;
    private Vector3 playerPosition = new Vector3();
    private Vector3 playerDirection = new Vector3(1, 0, 0);
    private Vector2Int gridPosition = new Vector2Int(1, 1);
    private Vector2Int moveDirection = null;
    private boolean isMoving = false;
    private float moveTimer = 0f;
    private final float MOVE_DURATION = 0.20f;
    private float shakeTime = 0f;
    private Vector3 moveStart = new Vector3();
    private Vector3 moveEnd = new Vector3();
    GameController gameController;
    IsometricGame game;
    DungeonInputController dungeonInputController;
    private OrthographicCamera uiCamera;
    private ShapeRenderer shapeRenderer;
    private final int MINIMAP_SIZE = 160;
    private final int MINIMAP_PADDING = 24;
    private SpriteBatch hudBatch;
    private Texture whiteTexture;
    private BitmapFont regularFont;
    private String playerName = "Hero";
    private float playerHealth = 75, playerMaxHealth = 100;
    private float playerMana = 40, playerMaxMana = 60;
    private Scene scene;
    private SceneManager sceneManager;
    private Array<Vector2Int> pathCells = new Array<>();
    private final Vector2Int startCell = new Vector2Int(1, 1);
    private  Vector2Int finalCell;

    // Enemy system
    private static class Enemy {
        Vector2Int position;
        int id;
        String word;
        boolean destroyed = false;
        float timer = 0;
        boolean active = false;
        ModelInstance modelInstance;
        boolean timerStarted = false;
    }
    private Array<Enemy> enemies = new Array<>();
    private int currentEnemyIdx = 0;
    private final float ENEMY_TIME_LIMIT = 5f;
    private SceneAsset enemySceneAsset;
    private Model enemyModel;
    private BitmapFont wordFont;
    private Random random = new Random();
    private Set<String> usedWords = new HashSet<>();
    private static final String[] ENGLISH_WORDS = {
            "hero", "dungeon", "isometric", "screen", "linear", "cave", "enemy", "magic", "light", "battle", "game", "floor", "ceiling"
    };


    public LinearCaveScreen(IsometricGame game, GameController gameController) {
        this.gameController = gameController;
        this.game = game;
        this.dungeonInputController = new DungeonInputController(this);
    }
    @Override
    public void show() {
        Gdx.input.setInputProcessor(new InputMultiplexer(this.dungeonInputController, new InputAdapter() {
            private StringBuilder wordInput = new StringBuilder();
            @Override
            public boolean keyTyped(char character) {
                if (currentEnemyIdx < enemies.size && enemies.get(currentEnemyIdx).active && !enemies.get(currentEnemyIdx).destroyed) {
                    if (character == '\b' && wordInput.length() > 0) {
                        wordInput.deleteCharAt(wordInput.length() - 1);
                    } else if (Character.isLetter(character)) {
                        wordInput.append(character);
                    } else if (character == '\r' || character == '\n') {
                        checkWordInput(wordInput.toString());
                        wordInput.setLength(0);
                    }
                }
                return false;
            }
        }));
        uiCamera = new OrthographicCamera();
        uiCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        uiCamera.update();
        shapeRenderer = new ShapeRenderer();
        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 0.01f;
        camera.far = 100f;

        modelBatch = new ModelBatch();
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.8f, 0.8f, 0.8f, 1f));
        environment.add(new DirectionalLight().set(1f, 1f, 1f, -1f, -0.8f, -0.2f));
        createModels();
        map = generateLinearMaze(MAP_WIDTH, MAP_HEIGHT);
        buildMap();
        findPathBFS(startCell, finalCell);
        spawnEnemiesOnPath(Math.min(7, pathCells.size-2));
        gridPosition.set(startCell.x, startCell.y);
        updatePlayerPositionFromGrid();
        updateCameraFromPlayer(0f);
        camera.update();
        hudBatch = new SpriteBatch();
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1, 1, 1, 1);
        pixmap.fill();
        whiteTexture = new Texture(pixmap);
        pixmap.dispose();
        regularFont = new BitmapFont();
        wordFont = new BitmapFont();
        wordFont.getData().setScale(1.2f);
    }
    private void renderEnemyWordsBillboard(Camera camera, ModelBatch modelBatch, Environment environment) {
        for (Enemy enemy : enemies) {
            if (!enemy.destroyed && enemy.active) {
                Vector3 worldPos = new Vector3();
                enemy.modelInstance.transform.getTranslation(worldPos);
                worldPos.y += 0f; // offset above enemy head

                float dist = camera.position.dst(worldPos);
                float scale = 0.5f ; // scale proportional to camera distance


                // Timer
                float timeLeft = Math.max(0, ENEMY_TIME_LIMIT - enemy.timer);
                String timeStr = String.format("%.1f", timeLeft);
                Vector3 timePos = new Vector3(worldPos.x, worldPos.y - 0.2f, worldPos.z);
                Color timerColor = (timeLeft <= 2.0f ? Color.RED : Color.CYAN);
            }
        }
    }
    private void createModels() {
        ModelBuilder modelBuilder = new ModelBuilder();
        boxModel = modelBuilder.createBox(1f, 1.4f, 1f,
                new Material(ColorAttribute.createDiffuse(Color.GRAY)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        ceilingModel = modelBuilder.createBox(1f, 0.1f, 1f,
                new Material(ColorAttribute.createDiffuse(new Color(0.2f, 0.2f, 0.25f, 1f))),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        SceneAsset sceneAsset = new GLTFLoader().load(Gdx.files.internal("3d/Untitled7.gltf"));
        scene = new Scene(sceneAsset.scene);
        scene.modelInstance.transform.setToTranslation(0, 0, 0);
        sceneManager = new SceneManager();
        sceneManager.setCamera(camera);
        sceneManager.addScene(scene);
        if (!sceneAsset.animations.isEmpty()) {
            scene.animationController.setAnimation(sceneAsset.animations.first().id, -1);
        }
        enemySceneAsset = new GLBLoader().load(Gdx.files.internal("3d/slime.glb"));
        enemyModel = enemySceneAsset.scene.model;
        PBRShaderConfig config = PBRShaderProvider.createDefaultConfig();
        config.numBones = 60;
        sceneManager.setShaderProvider(new PBRShaderProvider(config));
        sceneManager.environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.3f, 0.3f, 0.3f, 1f));
        sceneManager.environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));
    }

    private int[][] generateLinearMaze(int width, int height) {
        int[][] map = new int[width][height];
        int x = 1, y = 1;
        map[x][y] = 1;

        Array<int[]> directions = new Array<>(new int[][]{
                {1, 0}, {-1, 0}, {0, 1}, {0, -1}
        });

        int steps = 0;
        int maxSteps = width * height;

        finalCell = new Vector2Int(x, y);

        while (steps < maxSteps - 1) {
            directions.shuffle();
            boolean moved = false;
            for (int i = 0; i < directions.size; i++) {
                int[] dir = directions.get(i);
                int nx = x + dir[0];
                int ny = y + dir[1];
                if (nx >= 1 && ny >= 1 && nx < width - 1 && ny < height - 1 && map[nx][ny] == 0) {
                    int neighbors = 0;
                    for (int j = 0; j < directions.size; j++) {
                        int[] d = directions.get(j);
                        int ax = nx + d[0];
                        int ay = ny + d[1];
                        if (ax >= 0 && ay >= 0 && ax < width && ay < height) {
                            neighbors += map[ax][ay];
                        }
                    }
                    if (neighbors <= 1) {
                        x = nx;
                        y = ny;
                        map[x][y] = 1;
                        steps++;
                        moved = true;
                        finalCell = new Vector2Int(x, y);
                        break;
                    }
                }
            }
            if (!moved) break;
        }
        return map;
    }

    private void buildMap() {
        instances.clear();
        for (int x = 0; x < MAP_WIDTH; x++) {
            for (int y = 0; y < MAP_HEIGHT; y++) {
                if (map[x][y] == 0) {
                    boolean adjacentToFloor = false;
                    for (int[] dir : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                        int nx = x + dir[0];
                        int ny = y + dir[1];
                        if (nx >= 0 && ny >= 0 && nx < MAP_WIDTH && ny < MAP_HEIGHT && map[nx][ny] == 1) {
                            adjacentToFloor = true;
                            break;
                        }
                    }
                    if (!adjacentToFloor) continue;
                    ModelInstance wall = new ModelInstance(boxModel);
                    wall.transform.setToTranslation(x, 0.5f, y);
                    instances.add(wall);
                }
            }
        }
    }

    private void findPathBFS(Vector2Int start, Vector2Int end) {
        boolean[][] visited = new boolean[MAP_WIDTH][MAP_HEIGHT];
        Vector2Int[][] prev = new Vector2Int[MAP_WIDTH][MAP_HEIGHT];
        Queue<Vector2Int> queue = new LinkedList<>();
        queue.add(start);
        visited[start.x][start.y] = true;
        boolean found = false;
        while (!queue.isEmpty()) {
            Vector2Int curr = queue.poll();
            if (curr.equals(end)) {
                found = true;
                break;
            }
            for (int[] dir : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                int nx = curr.x + dir[0], ny = curr.y + dir[1];
                if (nx>=0 && ny>=0 && nx<MAP_WIDTH && ny<MAP_HEIGHT && map[nx][ny]==1 && !visited[nx][ny]) {
                    visited[nx][ny] = true;
                    prev[nx][ny] = curr;
                    queue.add(new Vector2Int(nx, ny));
                }
            }
        }
        pathCells.clear();
        if (found) {
            Vector2Int curr = end;
            while (curr != null) {
                pathCells.add(curr);
                curr = prev[curr.x][curr.y];
            }
            pathCells.reverse();
            finalCell = pathCells.peek();
        }
    }

    private void spawnEnemiesOnPath(int count) {
        enemies.clear();
        usedWords.clear();
        int n = Math.min(count, Math.min(7, pathCells.size - 2));
        if (n <= 0) return;
        int available = pathCells.size - 4;
        n = Math.min(n, available);
        int distance = available / n;
        for (int i = 0; i < n; i++) {
            int idx = 3 + i * distance;
            if (idx >= pathCells.size - 1) idx = pathCells.size - 2;
            Enemy enemy = new Enemy();
            enemy.position = pathCells.get(idx);
            enemy.id = i + 1;
            enemy.word = getRandomEnglishWord();
            enemy.modelInstance = new ModelInstance(enemyModel);
            enemy.modelInstance.transform.setToTranslation(enemy.position.x, 0.25f, enemy.position.y);
            enemies.add(enemy);
        }
        if (enemies.size > 0) {
            enemies.get(0).active = true;
            enemies.get(0).timer = 0;
        }
        currentEnemyIdx = 0;
    }

    private String getRandomEnglishWord() {
        String word;
        do {
            word = ENGLISH_WORDS[random.nextInt(ENGLISH_WORDS.length)];
        } while (usedWords.contains(word));
        usedWords.add(word);
        return word;
    }

    private void checkWordInput(String input) {
        if (currentEnemyIdx < enemies.size) {
            Enemy enemy = enemies.get(currentEnemyIdx);
            if (enemy.active && !enemy.destroyed && input.equalsIgnoreCase(enemy.word)) {
                enemy.destroyed = true;
                enemy.active = false;
                currentEnemyIdx++;
                if (currentEnemyIdx < enemies.size) {
                    enemies.get(currentEnemyIdx).active = true;
                    enemies.get(currentEnemyIdx).timerStarted = false;
                    enemies.get(currentEnemyIdx).timer = 0f;
                }
            }
        }
    }

    private void updateEnemyTimers(float delta) {
        if (currentEnemyIdx < enemies.size) {
            Enemy enemy = enemies.get(currentEnemyIdx);
            if (!enemy.destroyed) {
                int playerIdx = -1, enemyIdx = -1;
                for (int i = 0; i < pathCells.size; i++) {
                    if (pathCells.get(i).equals(gridPosition)) playerIdx = i;
                    if (pathCells.get(i).equals(enemy.position)) enemyIdx = i;
                }
                if (playerIdx != -1 && enemyIdx != -1 && Math.abs(playerIdx - enemyIdx) == 2) {
                    if (!enemy.timerStarted) {
                        enemy.timerStarted = true;
                        enemy.timer = 0f;
                    }
                }
                if (enemy.timerStarted && !enemy.destroyed) {
                    enemy.timer += delta;
                    if (enemy.timer >= ENEMY_TIME_LIMIT) {
                        enemy.destroyed = true;
                        enemy.active = false;
                        playerHealth -= 15;
                        currentEnemyIdx++;
                        if (currentEnemyIdx < enemies.size) {
                            enemies.get(currentEnemyIdx).active = true;
                            enemies.get(currentEnemyIdx).timerStarted = false;
                            enemies.get(currentEnemyIdx).timer = 0;
                        }
                    }
                }
            }
        }
    }
    private void renderEnemyWords(DecalBatch decalBatch, Camera camera) {
        for (Enemy enemy : enemies) {
            if (!enemy.destroyed && enemy.active) {

            }
        }
    }

    private void renderEnemies(ModelBatch batch, Environment env) {
        for (Enemy enemy : enemies) {
            if (!enemy.destroyed) {
                batch.render(enemy.modelInstance, env);
            }
        }
    }

    private void drawPlayerHUD(SpriteBatch batch) {
        float x = 20, y = 600, width = 220, height = 100;
        batch.begin();
        drawPlayerStatusColumn(batch, x, y, width, height);
        batch.end();
    }

    private void drawPlayerStatusColumn(SpriteBatch batch, float x, float y, float width, float height) {
        batch.setColor(0.2f, 0.4f, 0.2f, 0.9f);
        batch.draw(whiteTexture, x, y, width, height);
        drawHealthBar(batch, playerHealth, playerMaxHealth, x + 15, y + height - 20, width - 30, 12);
        drawManaBar(batch, playerMana, playerMaxMana, x + 15, y + height - 40, width - 30, 12);
        regularFont.setColor(Color.WHITE);
        regularFont.draw(batch, "HP: " + (int) playerHealth + "/" + (int) playerMaxHealth, x + 15, y + height - 50);
        regularFont.draw(batch, "MP: " + (int) playerMana + "/" + (int) playerMaxMana, x + 15, y + height - 70);
    }
    private void drawHealthBar(SpriteBatch batch, float current, float max, float x, float y, float width, float height) {
        batch.setColor(0.3f, 0.3f, 0.3f, 1);
        batch.draw(whiteTexture, x, y, width, height);
        batch.setColor(getHealthColor(current / max));
        batch.draw(whiteTexture, x, y, width * (current / max), height);
    }
    private void drawManaBar(SpriteBatch batch, float current, float max, float x, float y, float width, float height) {
        batch.setColor(0.3f, 0.3f, 0.3f, 1);
        batch.draw(whiteTexture, x, y, width, height);
        batch.setColor(0.2f, 0.4f, 0.9f, 1);
        batch.draw(whiteTexture, x, y, width * (current / max), height);
    }
    private Color getHealthColor(float percent) {
        if (percent > 0.6f) return Color.GREEN;
        if (percent > 0.3f) return Color.ORANGE;
        return Color.RED;
    }

    private void drawMiniMap() {
        shapeRenderer.setProjectionMatrix(uiCamera.combined);
        int mapSize = Math.max(MAP_WIDTH, MAP_HEIGHT);
        float cellSize = (float) MINIMAP_SIZE / mapSize;
        float originX = 1280 - MINIMAP_SIZE - MINIMAP_PADDING;
        float originY = MINIMAP_PADDING;
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 0, 0, 0.5f);
        shapeRenderer.rect(originX - 4, originY - 4, MINIMAP_SIZE + 8, MINIMAP_SIZE + 8);
        for (int x = 0; x < MAP_WIDTH; x++) {
            for (int y = 0; y < MAP_HEIGHT; y++) {
                if (map[x][y] == 0) {
                    shapeRenderer.setColor(Color.DARK_GRAY);
                } else {
                    shapeRenderer.setColor(Color.LIGHT_GRAY);
                }
                shapeRenderer.rect(originX + x * cellSize, originY + y * cellSize, cellSize, cellSize);
            }
        }
        for (Enemy enemy : enemies) {
            if (!enemy.destroyed) {
                shapeRenderer.setColor(Color.RED);
                shapeRenderer.ellipse(
                        originX + enemy.position.x * cellSize + cellSize / 4,
                        originY + enemy.position.y * cellSize + cellSize / 4,
                        cellSize / 2,
                        cellSize / 2
                );
            }
        }
        shapeRenderer.setColor(Color.BLUE);
        shapeRenderer.ellipse(
                originX + gridPosition.x * cellSize + cellSize / 4,
                originY + gridPosition.y * cellSize + cellSize / 4,
                cellSize / 2,
                cellSize / 2
        );
        shapeRenderer.end();
    }

    private int stepCounter = 0;
    private void handleGridMovement(float delta) {
        if (!isMoving) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.W)) {
                moveDirection = new Vector2Int((int) playerDirection.x, (int) playerDirection.z);
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.S)) {
                moveDirection = new Vector2Int(-(int) playerDirection.x, -(int) playerDirection.z);
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.A)) {
                playerDirection.rotate(Vector3.Y, 90);
                snapPlayerDirection();
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.D)) {
                playerDirection.rotate(Vector3.Y, -90);
                snapPlayerDirection();
            }
            if (moveDirection != null) {
                Vector2Int target = new Vector2Int(gridPosition.x + moveDirection.x, gridPosition.y + moveDirection.y);
                boolean blocked = false;
                for (Enemy enemy : enemies) {
                    if (!enemy.destroyed && enemy.position.equals(target)) {
                        blocked = true;
                        break;
                    }
                }
                if (isWalkable(target) && !blocked) {
                    moveStart.set(gridToWorld(gridPosition));
                    moveEnd.set(gridToWorld(target));
                    gridPosition = target;
                    isMoving = true;
                    moveTimer = 0f;
                    shakeTime = 0f;
                    stepCounter++;
                }
                moveDirection = null;
            }
        } else {
            moveTimer += delta;
            shakeTime += delta;
            float alpha = Math.min(moveTimer / MOVE_DURATION, 1f);
            float interp = Interpolation.smooth.apply(0f, 1f, alpha);
            playerPosition.set(moveStart).lerp(moveEnd, interp);
            if (alpha >= 1f) {
                isMoving = false;
                updatePlayerPositionFromGrid();
            }
        }
    }

    private boolean isWalkable(Vector2Int pos) {
        return pos.x >= 0 && pos.y >= 0 && pos.x < MAP_WIDTH && pos.y < MAP_HEIGHT && map[pos.x][pos.y] == 1;
    }

    private void updatePlayerPositionFromGrid() {
        playerPosition.set(gridToWorld(gridPosition));
    }

    private Vector3 gridToWorld(Vector2Int grid) {
        return new Vector3(grid.x, PLAYER_SCALE / 2f, grid.y);
    }

    private void snapPlayerDirection() {
        float x = Math.round(playerDirection.x);
        float z = Math.round(playerDirection.z);
        playerDirection.set(x, 0, z).nor();
    }

    private void updateCameraFromPlayer(float delta) {
        Vector3 basePos = new Vector3(playerPosition);
        float eyeHeight = 0.2f;
        camera.position.set(basePos.x - 0.2f, basePos.y + eyeHeight, basePos.z - 0.15f);
        camera.lookAt(
                basePos.x + playerDirection.x - 0.2f,
                basePos.y + eyeHeight,
                basePos.z + playerDirection.z
        );
        camera.up.set(Vector3.Y);
        camera.update();
    }

    @Override
    public void render(float delta) {
        handleGridMovement(delta);
        updateEnemyTimers(delta);
        updateCameraFromPlayer(delta);

        Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        modelBatch.begin(camera);
        sceneManager.update(delta);
        for (ModelInstance instance : instances) {
            modelBatch.render(instance, environment);
        }
        scene.modelInstance.transform.setToTranslation(playerPosition.x, playerPosition.y - 0.05f, playerPosition.z);
        float angle = (float) Math.atan2(playerDirection.x, playerDirection.z) * MathUtils.radiansToDegrees;
        scene.modelInstance.transform.rotate(Vector3.Y, angle);
        modelBatch.render(scene.modelInstance, environment);
        renderEnemies(modelBatch, environment);
        renderEnemyWordsBillboard(camera, modelBatch, environment);
        modelBatch.end();

        drawMiniMap();
        drawPlayerHUD(hudBatch);

    }

    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override
    public void dispose() {
        modelBatch.dispose();
        boxModel.dispose();
        ceilingModel.dispose();
        shapeRenderer.dispose();
        if (sceneManager != null) sceneManager.dispose();
        if (gameController != null) gameController.dispose();
        if (game != null) game.dispose();
        if (enemyModel != null) enemyModel.dispose();
        if (hudBatch != null) hudBatch.dispose();
        if (whiteTexture != null) whiteTexture.dispose();
    }

    public static class Vector2Int {
        int x, y;
        Vector2Int(int x, int y) { this.x = x; this.y = y; }
        public void set(int xx, int yy) { x = xx; y = yy; }
        public Vector2Int cpy() { return new Vector2Int(x, y); }
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Vector2Int)) return false;
            Vector2Int other = (Vector2Int) o;
            return this.x == other.x && this.y == other.y;
        }
        @Override
        public int hashCode() { return x * 31 + y; }
    }

    public IsometricGame getGame() { return game; }
    public void setGame(IsometricGame game) { this.game = game; }
    public float getPlayerHealth() { return playerHealth; }
    public void setPlayerHealth(float playerHealth) { this.playerHealth = playerHealth; }
    public float getPlayerMana() { return playerMana; }
    public void setPlayerMana(float playerMana) { this.playerMana = playerMana; }
}