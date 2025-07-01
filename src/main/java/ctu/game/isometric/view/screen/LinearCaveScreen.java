package ctu.game.isometric.view.screen;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.*;
import com.badlogic.gdx.graphics.g3d.decals.CameraGroupStrategy;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch;
import com.badlogic.gdx.graphics.g3d.environment.*;
import com.badlogic.gdx.graphics.g3d.utils.*;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.math.collision.BoundingBox;
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
    private Vector2Int finalCell;

    // For typing UI
    private StringBuilder currentTypedWord = new StringBuilder();
    private float alertTimer = 0f;
    private String alertText = null;
    private Color alertColor = Color.WHITE;
    private final float ALERT_DURATION = 2.0f;

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

    private DecalBatch decalBatch;
    private TextureRegion[] wordTextures;
    private BitmapFont wordFont;
    private SpriteBatch tempBatch;
    private FrameBuffer frameBuffer;

    private Array<Enemy> enemies = new Array<>();
    private int currentEnemyIdx = 0;
    private final float ENEMY_TIME_LIMIT = 5f;
    private SceneAsset enemySceneAsset;
    private Model enemyModel;
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

    boolean isTyped = false;


    public void setTyped() {
        isTyped = true;
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(new InputMultiplexer(this.dungeonInputController, new InputAdapter() {
            @Override
            public boolean keyTyped(char character) {
                if (!isTyped)
                    return false;

                if (currentEnemyIdx < enemies.size && enemies.get(currentEnemyIdx).active &&
                        !enemies.get(currentEnemyIdx).destroyed) {
                    if (character == '\b' && currentTypedWord.length() > 0) {
                        currentTypedWord.deleteCharAt(currentTypedWord.length() - 1);
                    } else if (Character.isLetter(character)) {
                        currentTypedWord.append(character);
                    } else if (character == '\r' || character == '\n') {
                        checkWordInput(currentTypedWord.toString());
                        currentTypedWord.setLength(0);
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
        spawnEnemiesOnPath(Math.min(7, pathCells.size - 2));
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


        decalBatch = new DecalBatch(new CameraGroupStrategy(camera));
        tempBatch = new SpriteBatch();
        frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, 256, 64, false);
        wordTextures = new TextureRegion[enemies.size];
        createWordTextures();
    }


    private void createWordTextures() {
        System.out.println("Creating word textures for " + enemies.size + " enemies");

        for (int i = 0; i < enemies.size; i++) {
            Enemy enemy = enemies.get(i);
            System.out.println("Creating texture for word: " + enemy.word);

            FrameBuffer wordBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, 256, 64, false);

            wordBuffer.begin();
            Gdx.gl.glClearColor(0, 0, 0, 0);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

            tempBatch.getProjectionMatrix().setToOrtho2D(0, 0, 256, 64);
            tempBatch.begin();

            GlyphLayout layout = new GlyphLayout(wordFont, enemy.word);
            float textX = (256 - layout.width) / 2;
            float textY = (64 + layout.height) / 2;

            wordFont.setColor(1, 1, 1, 1); // Use full white (1,1,1,1)
            wordFont.draw(tempBatch, enemy.word, textX, textY);
            tempBatch.end();
            wordBuffer.end();

            // Simpler approach - use the framebuffer texture directly
            Texture texture = wordBuffer.getColorBufferTexture();
            wordTextures[i] = new TextureRegion(texture);
            wordTextures[i].flip(false, true);

            System.out.println("Created texture: " + (wordTextures[i] != null));

        }
    }

    private void renderEnemyWords(Camera camera) {
        if (decalBatch == null) return;

        for (int i = 0; i < enemies.size; i++) {
            Enemy enemy = enemies.get(i);
            if (!enemy.destroyed && enemy.active) {
                TextureRegion region = wordTextures[i];
                if (region != null) {
                    Decal decal = Decal.newDecal(
                            2.0f, 0.5f,
                            region,
                            true
                    );
                    decal.setPosition(enemy.position.x, 0.25f, enemy.position.y);
                    decal.lookAt(camera.position, camera.up);
                    decalBatch.add(decal);

                    // Draw progress bar if timer started
                    if (enemy.timerStarted) {
                        float progress = 1f - (enemy.timer / ENEMY_TIME_LIMIT);

//                         Draw progress background
//                        Decal progressBg = Decal.newDecal(
//                                0.5f, 0.03f,
//                                new TextureRegion(whiteTexture),
//                                true
//                        );
//                        progressBg.setColor(0.2f, 0.2f, 0.2f, 0.8f);
//                        progressBg.setPosition(enemy.position.x, 0.9f, enemy.position.y);
//                        progressBg.lookAt(camera.position, camera.up);

                        // Draw progress fill
                        Decal progressFill = Decal.newDecal(
                                0.5f * progress, 0.03f,
                                new TextureRegion(whiteTexture),
                                true
                        );
                        progressFill.setColor(1f, 0.3f, 0.3f, 0.8f);
                        progressFill.setPosition(enemy.position.x - 0.25f * (1 - progress), 0.5f, enemy.position.y);
                        progressFill.lookAt(camera.position, camera.up);

                        decalBatch.add(progressFill);
                    }
                }
            }
        }

        decalBatch.flush();
    }

    private void drawTextInputUI(SpriteBatch batch) {
        if (currentEnemyIdx < enemies.size && enemies.get(currentEnemyIdx).active &&
                enemies.get(currentEnemyIdx).timerStarted) {

            batch.begin();

            // Draw typing box
            float boxWidth = 400;
            float boxHeight = 50;
            float x = (Gdx.graphics.getWidth() - boxWidth) / 2;
            float y = 50;

            // Background
            batch.setColor(0.2f, 0.2f, 0.3f, 0.9f);
            batch.draw(whiteTexture, x, y, boxWidth, boxHeight);

            // Border
            batch.setColor(0.5f, 0.5f, 0.6f, 1f);
            float borderThickness = 2;
            batch.draw(whiteTexture, x, y, boxWidth, borderThickness); // Bottom
            batch.draw(whiteTexture, x, y + boxHeight - borderThickness, boxWidth, borderThickness); // Top
            batch.draw(whiteTexture, x, y, borderThickness, boxHeight); // Left
            batch.draw(whiteTexture, x + boxWidth - borderThickness, y, borderThickness, boxHeight); // Right

            // Text
            regularFont.setColor(Color.WHITE);

            Enemy currentEnemy = enemies.get(currentEnemyIdx);
            String targetWord = currentEnemy.word;
            String typedText = currentTypedWord.toString();

            // Draw target word above the input box
            regularFont.draw(batch, "Type: " + targetWord, x + 10, y + boxHeight + 20);

            // Draw typed text
            GlyphLayout layout = new GlyphLayout(regularFont, typedText);
            regularFont.draw(batch, typedText, x + 10, y + boxHeight / 2 + layout.height / 2);

            batch.end();
        }
    }

    private void drawAlertUI(SpriteBatch batch) {
        if (alertText != null && alertTimer > 0) {
            batch.begin();

            float boxWidth = 400;
            float boxHeight = 60;
            float x = (Gdx.graphics.getWidth() - boxWidth) / 2;
            float y = 150;

            // Background with fade effect
            float alpha = Math.min(1.0f, alertTimer / (ALERT_DURATION * 0.5f));
            if (alertTimer < ALERT_DURATION * 0.5f) {
                alpha = alertTimer / (ALERT_DURATION * 0.5f);
            }

            batch.setColor(0.2f, 0.2f, 0.3f, 0.8f * alpha);
            batch.draw(whiteTexture, x, y, boxWidth, boxHeight);

            // Text with fade effect
            Color textColor = new Color(alertColor);
            textColor.a = alpha;
            regularFont.setColor(textColor);

            GlyphLayout layout = new GlyphLayout(regularFont, alertText);
            regularFont.draw(batch, alertText,
                    x + (boxWidth - layout.width) / 2,
                    y + (boxHeight + layout.height) / 2);

            batch.end();
        }
    }

    private void updateAlerts(float delta) {
        if (alertTimer > 0) {
            alertTimer -= delta;
            if (alertTimer <= 0) {
                alertText = null;
            }
        }
    }

    private void showAlert(String text, Color color) {
        alertText = text;
        alertColor = color;
        alertTimer = ALERT_DURATION;
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
        Scene enemyScene = new Scene(enemySceneAsset.scene);
        sceneManager.addScene(enemyScene);
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
                    for (int[] dir : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
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
            for (int[] dir : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int nx = curr.x + dir[0], ny = curr.y + dir[1];
                if (nx >= 0 && ny >= 0 && nx < MAP_WIDTH && ny < MAP_HEIGHT && map[nx][ny] == 1 && !visited[nx][ny]) {
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
            enemy.modelInstance.transform.setToTranslation(enemy.position.x, -0.25f, enemy.position.y);
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
            if (enemy.active && !enemy.destroyed) {
                if (input.equalsIgnoreCase(enemy.word)) {
                    enemy.destroyed = true;
                    enemy.active = false;
                    showAlert("Correct! Enemy defeated!", Color.GREEN);
                    currentEnemyIdx++;
                    if (currentEnemyIdx < enemies.size) {
                        enemies.get(currentEnemyIdx).active = true;
                        enemies.get(currentEnemyIdx).timerStarted = false;
                        enemies.get(currentEnemyIdx).timer = 0f;
                        isTyped = false; // Reset typing state
                    } else {
                        showAlert("All enemies defeated!", Color.GOLD);
                    }
                } else {
                    showAlert("Wrong word! Try again!", Color.RED);
                }
            }
        }
    }

    private void updateEnemyTimers(float delta) {
        if (currentEnemyIdx < enemies.size) {
            Enemy enemy = enemies.get(currentEnemyIdx);
            if (!enemy.destroyed) {
                // Check if player is adjacent to enemy (one cell away)
                int playerIdx = -1, enemyIdx = -1;
                for (int i = 0; i < pathCells.size; i++) {
                    if (pathCells.get(i).equals(gridPosition)) playerIdx = i;
                    if (pathCells.get(i).equals(enemy.position)) enemyIdx = i;
                }
                if (playerIdx != -1 && enemyIdx != -1 && Math.abs(playerIdx - enemyIdx) <= 1 && isEnemyVisibleToCamera(enemy)) {
                    if (!enemy.timerStarted) {
                        enemy.timerStarted = true;
                        enemy.timer = 0f;
                        setTyped();
                        showAlert("Start typing \"" + enemy.word + "\"!", Color.CYAN);
                    }
                }

                // Handle timer and completion logic
                if (enemy.timerStarted && !enemy.destroyed) {
                    enemy.timer += delta;
                    if (enemy.timer >= ENEMY_TIME_LIMIT) {
                        enemy.destroyed = true;
                        enemy.active = false;
                        playerHealth -= 15;
                        showAlert("Time's up! Enemy exploded! -15 HP", Color.ORANGE);
                        currentTypedWord.setLength(0); // Clear the input
                        currentEnemyIdx++;
                        if (currentEnemyIdx < enemies.size) {
                            enemies.get(currentEnemyIdx).active = true;
                            enemies.get(currentEnemyIdx).timerStarted = false;
                            enemies.get(currentEnemyIdx).timer = 0;
                            isTyped = false; // Reset typing state
                        }
                    }
                }
            }
        }
    }


    private boolean isEnemyVisibleToCamera(Enemy enemy) {
        // Define the size of the word decal
        float decalWidth = 2.0f;
        float decalHeight = 0.5f;
        float centerY = 0.25f;

        // Calculate the 8 corners of the bounding box for the word decal
        Vector3[] corners = new Vector3[8];
        float minX = enemy.position.x - decalWidth / 2f;
        float maxX = enemy.position.x + decalWidth / 2f;
        float minY = centerY - decalHeight / 2f;
        float maxY = centerY + decalHeight / 2f;
        float minZ = enemy.position.y - 0.01f;
        float maxZ = enemy.position.y + 0.01f;

        int idx = 0;
        for (float x : new float[]{minX, maxX}) {
            for (float y : new float[]{minY, maxY}) {
                for (float z : new float[]{minZ, maxZ}) {
                    corners[idx++] = new Vector3(x, y, z);
                }
            }
        }

        // All corners must be inside the frustum
        for (Vector3 corner : corners) {
            if (!camera.frustum.pointInFrustum(corner)) {
                return false;
            }
        }

        // Line of sight check (as before)
        Vector3 playerPos = new Vector3(playerPosition.x, 0.2f, playerPosition.z);
        Vector3 enemyPos = new Vector3(enemy.position.x, centerY, enemy.position.y);
        Vector3 direction = new Vector3(enemyPos).sub(playerPos).nor();
        float dotProduct = playerDirection.dot(direction);

        if (dotProduct > 0.7f) {
            int playerX = gridPosition.x;
            int playerY = gridPosition.y;
            int enemyX = enemy.position.x;
            int enemyY = enemy.position.y;
            boolean clearPath = true;
            int dx = Math.abs(enemyX - playerX);
            int dy = Math.abs(enemyY - playerY);
            int sx = playerX < enemyX ? 1 : -1;
            int sy = playerY < enemyY ? 1 : -1;
            int err = dx - dy;
            int x = playerX;
            int y = playerY;

            while (x != enemyX || y != enemyY) {
                int e2 = 2 * err;
                if (e2 > -dy) {
                    err -= dy;
                    x += sx;
                }
                if (e2 < dx) {
                    err += dx;
                    y += sy;
                }
                if (x == playerX && y == playerY) continue;
                if (x >= 0 && y >= 0 && x < MAP_WIDTH && y < MAP_HEIGHT && map[x][y] == 0) {
                    clearPath = false;
                    break;
                }
            }
            return clearPath;
        }
        return false;
    }

    public void activateCurrentEnemy() {
        if (currentEnemyIdx < enemies.size && !enemies.get(currentEnemyIdx).destroyed) {
            Enemy enemy = enemies.get(currentEnemyIdx);
            enemy.timerStarted = true;
            enemy.timer = 0f;
            setTyped();
            showAlert("Start typing \"" + enemy.word + "\"!", Color.CYAN);
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


    public boolean keyPressed(int keycode) {
        if (!isMoving && !isTyped) {
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
        }
        return true;
    }

    private void handleGridMovement(float delta) {
        if (!isMoving && !isTyped) {
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
        camera.position.set(basePos.x , basePos.y + eyeHeight, basePos.z);
        camera.lookAt(
                basePos.x + playerDirection.x - 0.2f,
                basePos.y + eyeHeight -0.1f,
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
        updateAlerts(delta);

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
//        modelBatch.render(scene.modelInstance, environment);
        renderEnemies(modelBatch, environment);
        modelBatch.end();
        renderEnemyWords(camera);

        drawMiniMap();
        drawPlayerHUD(hudBatch);
        drawTextInputUI(hudBatch);
        drawAlertUI(hudBatch);
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

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

        if (decalBatch != null) decalBatch.dispose();
        if (tempBatch != null) tempBatch.dispose();
        if (frameBuffer != null) frameBuffer.dispose();
    }

    public static class Vector2Int {
        int x, y;

        Vector2Int(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public void set(int xx, int yy) {
            x = xx;
            y = yy;
        }

        public Vector2Int cpy() {
            return new Vector2Int(x, y);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Vector2Int)) return false;
            Vector2Int other = (Vector2Int) o;
            return this.x == other.x && this.y == other.y;
        }

        @Override
        public int hashCode() {
            return x * 31 + y;
        }
    }

    public IsometricGame getGame() {
        return game;
    }

    public void setGame(IsometricGame game) {
        this.game = game;
    }

    public float getPlayerHealth() {
        return playerHealth;
    }

    public void setPlayerHealth(float playerHealth) {
        this.playerHealth = playerHealth;
    }

    public float getPlayerMana() {
        return playerMana;
    }

    public void setPlayerMana(float playerMana) {
        this.playerMana = playerMana;
    }
}