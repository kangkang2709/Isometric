package ctu.game.isometric.view.screen;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.*;
import com.badlogic.gdx.graphics.g3d.decals.*;
import com.badlogic.gdx.graphics.g3d.environment.*;
import com.badlogic.gdx.graphics.g3d.utils.*;
import com.badlogic.gdx.graphics.glutils.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.StringBuilder;
import ctu.game.isometric.IsometricGame;
import ctu.game.isometric.controller.GameController;
import ctu.game.isometric.model.game.GameState;
import net.mgsx.gltf.loaders.glb.GLBLoader;
import net.mgsx.gltf.loaders.gltf.GLTFLoader;
import net.mgsx.gltf.scene3d.scene.*;
import net.mgsx.gltf.scene3d.shaders.*;

import java.util.*;
import java.util.Queue;

public class LinearCaveScreen implements Screen {

    // Constants
    private static final int MAP_WIDTH = 15;
    private static final int MAP_HEIGHT = 15;
    private static final float PLAYER_SCALE = 0.5f;
    private static final float MOVE_DURATION = 0.20f;
    private static final float ENEMY_TIME_LIMIT = 5f;
    private static final float ALERT_DURATION = 2.0f;
    private static final int MINIMAP_SIZE = 160;
    private static final int MINIMAP_PADDING = 24;
    private static final int MAX_ENEMIES = 7;

    private static final String[] ENGLISH_WORDS = {
            "hero", "dungeon", "isometric", "screen", "linear", "cave",
            "enemy", "magic", "light", "battle", "game", "floor", "ceiling"
    };

    // Core game objects
    private final IsometricGame game;
    private final GameController gameController;

    // Rendering components
    private PerspectiveCamera camera;
    private OrthographicCamera uiCamera;
    private ModelBatch modelBatch;
    private Environment environment;
    private ShapeRenderer shapeRenderer;
    private SpriteBatch hudBatch;
    private SpriteBatch tempBatch;
    private DecalBatch decalBatch;
    private FrameBuffer frameBuffer;

    // Models and scenes
    private Model boxModel;
    private Model ceilingModel;
    private Model enemyModel;
    private Scene scene;
    private SceneManager sceneManager;
    private SceneAsset enemySceneAsset;
    private Array<ModelInstance> instances = new Array<>();

    // Map and pathfinding
    private int[][] map;
    private Array<Vector2Int> pathCells = new Array<>();
    private final Vector2Int startCell = new Vector2Int(1, 1);
    private Vector2Int finalCell;

    // Player state
    private Vector3 playerPosition = new Vector3();
    private Vector3 playerDirection = new Vector3(1, 0, 0);
    private Vector2Int gridPosition = new Vector2Int(1, 1);
    private Vector2Int moveDirection = null;
    private boolean isMoving = false;
    private float moveTimer = 0f;
    private float shakeTime = 0f;
    private Vector3 moveStart = new Vector3();
    private Vector3 moveEnd = new Vector3();

    // Player stats
    private String playerName = "Hero";
    private float playerHealth = 75f;
    private float playerMaxHealth = 100f;
    private float playerMana = 40f;
    private float playerMaxMana = 60f;

    // UI components
    private Texture whiteTexture;
    private BitmapFont regularFont;
    private BitmapFont wordFont;
    private TextureRegion[] wordTextures;

    // Typing system
    private StringBuilder currentTypedWord = new StringBuilder();
    private boolean isTyped = false;

    // Alert system
    private float alertTimer = 0f;
    private String alertText = null;
    private Color alertColor = Color.WHITE;

    // Enemy system
    private Array<Enemy> enemies = new Array<>();
    private int currentEnemyIdx = 0;
    private final Random random = new Random();
    private final Set<String> usedWords = new HashSet<>();

    // Game state
    private boolean gameStarted = false;
    private boolean gameEnded = false;

    public LinearCaveScreen(IsometricGame game, GameController gameController) {
        this.game = game;
        this.gameController = gameController;
    }

    /**
     * Starts or restarts the dungeon with a new map layout
     */
    public void startNewDungeon() {
        startNewDungeon(System.currentTimeMillis());
    }

    /**
     * Starts or restarts the dungeon with a specific seed for reproducible maps
     */
    public void startNewDungeon(long seed) {
        gameStarted = true;
        gameEnded = false;
        random.setSeed(seed);

        // Reset player state
        resetPlayerState();

        // Generate new map and enemies
        generateNewMap();

        // Reset UI state
        resetUIState();

        showAlert("New dungeon generated! Good luck!", Color.CYAN);
    }

    private void resetPlayerState() {
        playerMaxHealth = gameController.getCharacter().getMaxHealth();
        playerHealth = gameController.getCharacter().getHealth();
        playerMaxMana = gameController.getCharacter().getMaxMana();
        playerMana = gameController.getCharacter().getMana();

        gridPosition.set(startCell.x, startCell.y);
        playerDirection.set(1, 0, 0);
        isMoving = false;
        isTyped = false;
        updatePlayerPositionFromGrid();
        updateCameraFromPlayer(0f);
    }

    public void setGameStarted(boolean gameStarted) {
        this.gameStarted = gameStarted;
    }

    public void finishDungeon() {
        gameStarted = false;
        gameEnded = false;

        if (isComplete) {
            gameController.getCharacter().setHealth(playerHealth);
            gameController.getCharacter().setMana(playerMana);
            game.changeScreen("GAME");
        } else {
            gameController.setState(GameState.MAIN_MENU);
            game.changeScreen("GAME_OVER");
        }
    }

    private void generateNewMap() {
        // Clear previous data
        enemies.clear();
        usedWords.clear();
        instances.clear();
        currentEnemyIdx = 0;

        // Generate new map
        map = generateLinearMaze(MAP_WIDTH, MAP_HEIGHT);
        buildMap();

        // Find path and spawn enemies
        findPathBFS(startCell, finalCell);
        spawnEnemiesOnPath(Math.min(MAX_ENEMIES, pathCells.size - 2));

        // Recreate word textures for new enemies
        if (wordTextures != null) {
            for (TextureRegion texture : wordTextures) {
                if (texture != null && texture.getTexture() != null) {
                    texture.getTexture().dispose();
                }
            }
        }
        createWordTextures();
    }

    private void resetUIState() {
        currentTypedWord.setLength(0);
        alertTimer = 0f;
        alertText = null;
    }

    @Override
    public void show() {
        initializeInput();
        initializeRendering();
        initializeAssets();

        // Don't start the game automatically - wait for startNewDungeon() call
        if (!gameStarted) {
            showAlert("Press SPACE to start a new dungeon!", Color.YELLOW);
        }
    }

    private void initializeInput() {
        Gdx.input.setInputProcessor(new InputMultiplexer(
                createTypingInputProcessor(),
                createGameControlInputProcessor()
        ));
    }


    private InputProcessor createTypingInputProcessor() {
        return new InputAdapter() {
            @Override
            public boolean keyTyped(char character) {
                if (!isTyped || !gameStarted || gameEnded) return false;
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
                return true;
            }

            @Override
            public boolean keyDown(int keycode) {
                return keyPressed(keycode);
            }
        };
    }

    private InputProcessor createGameControlInputProcessor() {
        return new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.SPACE && !gameStarted) {
                    startNewDungeon();
                    return true;
                } else if (keycode == Input.Keys.R && (gameEnded || !gameStarted)) {
                    finishDungeon();
                    return true;
                }

                return false;
            }
        };
    }


    private void initializeRendering() {
        // Initialize cameras
        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 0.01f;
        camera.far = 100f;

        uiCamera = new OrthographicCamera();
        uiCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        uiCamera.update();

        // Initialize rendering components
        modelBatch = new ModelBatch();
        shapeRenderer = new ShapeRenderer();
        hudBatch = new SpriteBatch();
        tempBatch = new SpriteBatch();
        decalBatch = new DecalBatch(new CameraGroupStrategy(camera));
        frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, 256, 64, false);

        // Initialize environment
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.8f, 0.8f, 0.8f, 1f));
        environment.add(new DirectionalLight().set(1f, 1f, 1f, -1f, -0.8f, -0.2f));
    }

    private void initializeAssets() {
        createModels();
        createTextures();
        createFonts();
    }

    private void createModels() {
        ModelBuilder modelBuilder = new ModelBuilder();

        // Create basic models
        boxModel = modelBuilder.createBox(1f, 1.4f, 1f,
                new Material(ColorAttribute.createDiffuse(Color.GRAY)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

        ceilingModel = modelBuilder.createBox(1f, 0.1f, 1f,
                new Material(ColorAttribute.createDiffuse(new Color(0.2f, 0.2f, 0.25f, 1f))),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

        // Load player scene
        SceneAsset sceneAsset = new GLTFLoader().load(Gdx.files.internal("3d/Untitled7.gltf"));
        scene = new Scene(sceneAsset.scene);
        scene.modelInstance.transform.setToTranslation(0, 0, 0);

        // Initialize scene manager
        sceneManager = new SceneManager();
        sceneManager.setCamera(camera);
        sceneManager.addScene(scene);

        if (!sceneAsset.animations.isEmpty()) {
            scene.animationController.setAnimation(sceneAsset.animations.first().id, -1);
        }

        // Load enemy assets
        enemySceneAsset = new GLBLoader().load(Gdx.files.internal("3d/slime.glb"));
        enemyModel = enemySceneAsset.scene.model;

        // Configure PBR shader
        PBRShaderConfig config = PBRShaderProvider.createDefaultConfig();
        config.numBones = 60;
        sceneManager.setShaderProvider(new PBRShaderProvider(config));
        sceneManager.environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.3f, 0.3f, 0.3f, 1f));
        sceneManager.environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));
    }

    private void createTextures() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1, 1, 1, 1);
        pixmap.fill();
        whiteTexture = new Texture(pixmap);
        pixmap.dispose();
    }

    private void createFonts() {
        regularFont = new BitmapFont();
        wordFont = new BitmapFont();
        wordFont.getData().setScale(1.2f);
    }

    // Enemy class moved to inner class for better encapsulation
    private static class Enemy {
        Vector2Int position;
        int id;
        String word;
        boolean destroyed = false;
        float timer = 0;
        boolean active = false;
        ModelInstance modelInstance;
        boolean timerStarted = false;

        Enemy(Vector2Int position, int id, String word) {
            this.position = position;
            this.id = id;
            this.word = word;
        }
    }

    private int[][] generateLinearMaze(int width, int height) {
        int[][] newMap = new int[width][height];
        int x = 1, y = 1;
        newMap[x][y] = 1;

        Array<int[]> directions = new Array<>(new int[][]{
                {1, 0}, {-1, 0}, {0, 1}, {0, -1}
        });

        int steps = 0;
        int maxSteps = width * height;
        finalCell = new Vector2Int(x, y);

        while (steps < maxSteps - 1) {
            directions.shuffle();
            boolean moved = false;

            // FIX: Use index-based iteration instead of enhanced for-loop
            for (int i = 0; i < directions.size; i++) {
                int[] dir = directions.get(i);
                int nx = x + dir[0];
                int ny = y + dir[1];

                if (isValidMazeCell(nx, ny, width, height) && newMap[nx][ny] == 0) {
                    int neighbors = countNeighbors(newMap, nx, ny, directions, width, height);
                    if (neighbors <= 1) {
                        x = nx;
                        y = ny;
                        newMap[x][y] = 1;
                        steps++;
                        moved = true;
                        finalCell = new Vector2Int(x, y);
                        break;
                    }
                }
            }
            if (!moved) break;
        }
        return newMap;
    }

    private boolean isValidMazeCell(int x, int y, int width, int height) {
        return x >= 1 && y >= 1 && x < width - 1 && y < height - 1;
    }

    private int countNeighbors(int[][] map, int x, int y, Array<int[]> directions, int width, int height) {
        int neighbors = 0;
        // FIX: Use index-based iteration here too
        for (int i = 0; i < directions.size; i++) {
            int[] d = directions.get(i);
            int ax = x + d[0];
            int ay = y + d[1];
            if (ax >= 0 && ay >= 0 && ax < width && ay < height) {
                neighbors += map[ax][ay];
            }
        }
        return neighbors;
    }

    private void buildMap() {
        instances.clear();
        for (int x = 0; x < MAP_WIDTH; x++) {
            for (int y = 0; y < MAP_HEIGHT; y++) {
                if (map[x][y] == 0 && isAdjacentToFloor(x, y)) {
                    ModelInstance wall = new ModelInstance(boxModel);
                    wall.transform.setToTranslation(x, 0.5f, y);
                    instances.add(wall);
                }
            }
        }
    }

    private boolean isAdjacentToFloor(int x, int y) {
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] dir : directions) {
            int nx = x + dir[0];
            int ny = y + dir[1];
            if (nx >= 0 && ny >= 0 && nx < MAP_WIDTH && ny < MAP_HEIGHT && map[nx][ny] == 1) {
                return true;
            }
        }
        return false;
    }

    private void findPathBFS(Vector2Int start, Vector2Int end) {
        boolean[][] visited = new boolean[MAP_WIDTH][MAP_HEIGHT];
        Vector2Int[][] prev = new Vector2Int[MAP_WIDTH][MAP_HEIGHT];
        Queue<Vector2Int> queue = new LinkedList<>();

        queue.add(start);
        visited[start.x][start.y] = true;
        boolean found = false;

        while (!queue.isEmpty() && !found) {
            Vector2Int curr = queue.poll();
            if (curr.equals(end)) {
                found = true;
                break;
            }

            int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] dir : directions) {
                int nx = curr.x + dir[0];
                int ny = curr.y + dir[1];
                if (isValidPathCell(nx, ny) && !visited[nx][ny]) {
                    visited[nx][ny] = true;
                    prev[nx][ny] = curr;
                    queue.add(new Vector2Int(nx, ny));
                }
            }
        }

        reconstructPath(prev, end, found);
    }

    private boolean isValidPathCell(int x, int y) {
        return x >= 0 && y >= 0 && x < MAP_WIDTH && y < MAP_HEIGHT && map[x][y] == 1;
    }

    private void reconstructPath(Vector2Int[][] prev, Vector2Int end, boolean found) {
        pathCells.clear();
        if (found) {
            Vector2Int curr = end;
            while (curr != null) {
                pathCells.add(curr);
                curr = prev[curr.x][curr.y];
            }
            pathCells.reverse();
            if (pathCells.size > 0) {
                finalCell = pathCells.peek();
            }
        }
    }

    private void spawnEnemiesOnPath(int count) {
        enemies.clear();
        usedWords.clear();

        int availableSpots = Math.max(0, pathCells.size - 4);
        int enemyCount = Math.min(count, Math.min(MAX_ENEMIES, availableSpots));

        if (enemyCount <= 0) return;

        int spacing = availableSpots / enemyCount;
        for (int i = 0; i < enemyCount; i++) {
            int pathIndex = 3 + i * spacing;
            if (pathIndex >= pathCells.size - 1) {
                pathIndex = pathCells.size - 2;
            }

            Vector2Int enemyPos = pathCells.get(pathIndex);
            String word = getRandomEnglishWord();
            Enemy enemy = new Enemy(enemyPos, i + 1, word);

            // Create enemy scene
            Scene enemyScene = new Scene(enemySceneAsset.scene);
            enemyScene.modelInstance.transform.setToTranslation(enemyPos.x, -0.25f, enemyPos.y);

            if (enemySceneAsset.animations.size > 1) {
                enemyScene.animationController.setAnimation(enemySceneAsset.animations.get(1).id, -1);
            }

            sceneManager.addScene(enemyScene);
            enemy.modelInstance = enemyScene.modelInstance;
            enemies.add(enemy);
        }

        // Activate first enemy
        if (enemies.size > 0) {
            enemies.get(0).active = true;
            enemies.get(0).timer = 0;
        }
        currentEnemyIdx = 0;
    }

    private String getRandomEnglishWord() {
        String word;
        int attempts = 0;
        do {
            word = ENGLISH_WORDS[random.nextInt(ENGLISH_WORDS.length)];
            attempts++;
        } while (usedWords.contains(word) && attempts < ENGLISH_WORDS.length * 2);

        usedWords.add(word);
        return word;
    }

    private void createWordTextures() {
        if (enemies.size == 0) return;

        wordTextures = new TextureRegion[enemies.size];

        for (int i = 0; i < enemies.size; i++) {
            Enemy enemy = enemies.get(i);
            wordTextures[i] = createWordTexture(enemy.word);
        }
    }

    private TextureRegion createWordTexture(String word) {
        FrameBuffer wordBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, 256, 64, false);

        wordBuffer.begin();
        Gdx.gl.glClearColor(0, 0, 0, 0);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        tempBatch.getProjectionMatrix().setToOrtho2D(0, 0, 256, 64);
        tempBatch.begin();

        GlyphLayout layout = new GlyphLayout(wordFont, word);
        float textX = (256 - layout.width) / 2;
        float textY = (64 + layout.height) / 2;

        wordFont.setColor(1, 1, 1, 1);
        wordFont.draw(tempBatch, word, textX, textY);
        tempBatch.end();
        wordBuffer.end();

        Texture texture = wordBuffer.getColorBufferTexture();
        TextureRegion region = new TextureRegion(texture);
        region.flip(false, true);

        return region;
    }

    private void checkWordInput(String input) {
        if (currentEnemyIdx >= enemies.size || !gameStarted || gameEnded) return;

        Enemy enemy = enemies.get(currentEnemyIdx);
        if (enemy.active && !enemy.destroyed) {
            if (input.equalsIgnoreCase(enemy.word)) {
                handleCorrectWord(enemy);
            } else {
                showAlert("Wrong word! Try again!", Color.RED);
            }
        }
    }

    private void handleCorrectWord(Enemy enemy) {
        enemy.destroyed = true;
        enemy.active = false;
        showAlert("Correct! Enemy defeated!", Color.GREEN);

        currentEnemyIdx++;
        if (currentEnemyIdx < enemies.size) {
            activateNextEnemy();
        } else {
            showAlert("All enemies defeated!", Color.GOLD);
            isTyped = false;
        }
    }

    private void activateNextEnemy() {
        if (currentEnemyIdx < enemies.size) {
            Enemy nextEnemy = enemies.get(currentEnemyIdx);
            nextEnemy.active = true;
            nextEnemy.timerStarted = false;
            nextEnemy.timer = 0f;
            isTyped = false;
        }
    }

    boolean isComplete = false;

    public void checkEnd() {
        if (gameEnded) return;

        if (playerHealth <= 0) {
            gameEnded = true;
            isComplete = false;
            showAlert("You died! Press R to come back Main Menu!", Color.RED);
        } else if (currentEnemyIdx >= enemies.size && gridPosition.equals(finalCell)) {
            gameEnded = true;
            isComplete = true;
            showAlert("Victory!\n You receive 200 SCORE!\n Press R return main game!", Color.GREEN);
        }
    }

    private void updateEnemyTimers(float delta) {
        if (!gameStarted || gameEnded || currentEnemyIdx >= enemies.size) return;

        Enemy enemy = enemies.get(currentEnemyIdx);
        if (enemy.destroyed) return;

        // Check if player is near enemy and enemy is visible
        if (isPlayerNearEnemy(enemy) && isEnemyVisibleToCamera(enemy)) {
            if (!enemy.timerStarted) {
                startEnemyTimer(enemy);
            }
        }

        // Update timer
        if (enemy.timerStarted && !enemy.destroyed) {
            enemy.timer += delta;
            if (enemy.timer >= ENEMY_TIME_LIMIT) {
                handleEnemyTimeout(enemy);
            }
        }
    }

    private boolean isPlayerNearEnemy(Enemy enemy) {
        int playerIdx = findPlayerPositionOnPath();
        int enemyIdx = findEnemyPositionOnPath(enemy);
        return playerIdx != -1 && enemyIdx != -1 && Math.abs(playerIdx - enemyIdx) <= 1;
    }

    private int findPlayerPositionOnPath() {
        for (int i = 0; i < pathCells.size; i++) {
            if (pathCells.get(i).equals(gridPosition)) {
                return i;
            }
        }
        return -1;
    }

    private int findEnemyPositionOnPath(Enemy enemy) {
        for (int i = 0; i < pathCells.size; i++) {
            if (pathCells.get(i).equals(enemy.position)) {
                return i;
            }
        }
        return -1;
    }

    private void startEnemyTimer(Enemy enemy) {
        enemy.timerStarted = true;
        enemy.timer = 0f;
        setTyped();
        showAlert("Start typing \"" + enemy.word + "\"!", Color.CYAN);
    }

    private void handleEnemyTimeout(Enemy enemy) {
        enemy.destroyed = true;
        enemy.active = false;
        playerHealth = Math.max(0, playerHealth - 20);
        showAlert("Time's up! Enemy exploded! -20 HP", Color.ORANGE);
        currentTypedWord.setLength(0);

        currentEnemyIdx++;
        if (currentEnemyIdx < enemies.size) {
            activateNextEnemy();
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

    // Movement and input handling
    public boolean keyPressed(int keycode) {
        if (!gameStarted || gameEnded || isMoving || isTyped) return false;

        switch (keycode) {
            case Input.Keys.F5:
                activateCurrentEnemy();
                break;

            case Input.Keys.W:
                moveDirection = new Vector2Int((int) playerDirection.x, (int) playerDirection.z);
                break;
            case Input.Keys.S:
                moveDirection = new Vector2Int(-(int) playerDirection.x, -(int) playerDirection.z);
                break;
            case Input.Keys.A:
                playerDirection.rotate(Vector3.Y, 90);
                snapPlayerDirection();
                break;
            case Input.Keys.D:
                playerDirection.rotate(Vector3.Y, -90);
                snapPlayerDirection();
                break;
        }
        return true;
    }

    private void handleGridMovement(float delta) {
        if (!gameStarted || gameEnded) return;

        if (!isMoving && !isTyped && moveDirection != null) {
            tryMove();
        } else if (isMoving) {
            updateMovement(delta);
        }
    }

    private void tryMove() {
        Vector2Int target = new Vector2Int(
                gridPosition.x + moveDirection.x,
                gridPosition.y + moveDirection.y
        );

        if (isWalkable(target) && !isEnemyAt(target)) {
            startMovement(target);
        }
        moveDirection = null;
    }

    private boolean isEnemyAt(Vector2Int position) {
        for (Enemy enemy : enemies) {
            if (!enemy.destroyed && enemy.position.equals(position)) {
                return true;
            }
        }
        return false;
    }

    private void startMovement(Vector2Int target) {
        moveStart.set(gridToWorld(gridPosition));
        moveEnd.set(gridToWorld(target));
        gridPosition = target;
        isMoving = true;
        moveTimer = 0f;
        shakeTime = 0f;
    }

    private void updateMovement(float delta) {
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
        camera.position.set(basePos.x - 0.2f, basePos.y + eyeHeight, basePos.z);
        camera.lookAt(
                basePos.x + playerDirection.x - 0.2f,
                basePos.y,
                basePos.z + playerDirection.z
        );
        camera.up.set(Vector3.Y);
        camera.update();
    }

    // Rendering methods
    @Override
    public void render(float delta) {
        if (gameStarted) {
            handleGridMovement(delta);
            updateEnemyTimers(delta);
            updateCameraFromPlayer(delta);
            checkEnd();
        }
        updateAlerts(delta);

        renderWorld();
        renderUI();
    }

    private void renderWorld() {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        if (!gameStarted) return;

        modelBatch.begin(camera);
        sceneManager.update(Gdx.graphics.getDeltaTime());

        // Render walls
        for (ModelInstance instance : instances) {
            modelBatch.render(instance, environment);
        }

        // Render player
        scene.modelInstance.transform.setToTranslation(playerPosition.x, playerPosition.y - 0.05f, playerPosition.z);
        float angle = (float) Math.atan2(playerDirection.x, playerDirection.z) * MathUtils.radiansToDegrees;
        scene.modelInstance.transform.rotate(Vector3.Y, angle);

        // Render enemies
        renderEnemies(modelBatch, environment);
        modelBatch.end();

        renderEnemyWords(camera);
    }

    private void renderUI() {
        if (gameStarted) {
            drawMiniMap();
            drawPlayerHUD(hudBatch);
            drawTextInputUI(hudBatch);
        }
        drawAlertUI(hudBatch);
    }

    private void renderEnemies(ModelBatch batch, Environment env) {
        for (Enemy enemy : enemies) {
            if (!enemy.destroyed) {
                batch.render(enemy.modelInstance, env);
            }
        }
    }

    private void renderEnemyWords(Camera camera) {
        if (decalBatch == null || wordTextures == null) return;

        for (int i = 0; i < Math.min(enemies.size, wordTextures.length); i++) {
            Enemy enemy = enemies.get(i);
            if (!enemy.destroyed && enemy.active && wordTextures[i] != null) {
                renderEnemyWord(enemy, wordTextures[i], camera);
                if (enemy.timerStarted) {
                    renderEnemyProgressBar(enemy, camera);
                }
            }
        }

        decalBatch.flush();
    }

    private void renderEnemyWord(Enemy enemy, TextureRegion texture, Camera camera) {
        Decal decal = Decal.newDecal(2.0f, 0.5f, texture, true);
        decal.setPosition(enemy.position.x, 0.25f, enemy.position.y);
        decal.lookAt(camera.position, camera.up);
        decalBatch.add(decal);
    }

    private void renderEnemyProgressBar(Enemy enemy, Camera camera) {
        float progress = 1f - (enemy.timer / ENEMY_TIME_LIMIT);
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

    // UI Drawing methods
    private void drawTextInputUI(SpriteBatch batch) {
        if (!isTypingActive()) return;

        batch.begin();

        float boxWidth = 400;
        float boxHeight = 50;
        float x = (Gdx.graphics.getWidth() - boxWidth) / 2;
        float y = 50;

        // Draw background and border
        drawUIBox(batch, x, y, boxWidth, boxHeight);

        // Draw text content
        Enemy currentEnemy = enemies.get(currentEnemyIdx);
        String targetWord = currentEnemy.word;
        String typedText = currentTypedWord.toString();

        regularFont.setColor(Color.WHITE);
        regularFont.draw(batch, "Type: " + targetWord, x + 10, y + boxHeight + 20);

        GlyphLayout layout = new GlyphLayout(regularFont, typedText);
        regularFont.draw(batch, typedText, x + 10, y + boxHeight / 2 + layout.height / 2);

        batch.end();
    }

    private boolean isTypingActive() {
        return currentEnemyIdx < enemies.size &&
                enemies.get(currentEnemyIdx).active &&
                enemies.get(currentEnemyIdx).timerStarted &&
                gameStarted && !gameEnded;
    }

    private void drawUIBox(SpriteBatch batch, float x, float y, float width, float height) {
        // Background
        batch.setColor(0.2f, 0.2f, 0.3f, 0.9f);
        batch.draw(whiteTexture, x, y, width, height);

        // Border
        batch.setColor(0.5f, 0.5f, 0.6f, 1f);
        float borderThickness = 2;
        batch.draw(whiteTexture, x, y, width, borderThickness); // Bottom
        batch.draw(whiteTexture, x, y + height - borderThickness, width, borderThickness); // Top
        batch.draw(whiteTexture, x, y, borderThickness, height); // Left
        batch.draw(whiteTexture, x + width - borderThickness, y, borderThickness, height); // Right
    }

    private void drawAlertUI(SpriteBatch batch) {
        if (alertText == null || alertTimer <= 0) return;

        batch.begin();

        float boxWidth = 400;
        float boxHeight = 60;
        float x = (Gdx.graphics.getWidth() - boxWidth) / 2;
        float y = 150;

        // Calculate fade effect
        float alpha = Math.min(1.0f, alertTimer / (ALERT_DURATION * 0.5f));
        if (alertTimer < ALERT_DURATION * 0.5f) {
            alpha = alertTimer / (ALERT_DURATION * 0.5f);
        }

        // Background with fade
        batch.setColor(0.2f, 0.2f, 0.3f, 0.8f * alpha);
        batch.draw(whiteTexture, x, y, boxWidth, boxHeight);

        // Text with fade
        Color textColor = new Color(alertColor);
        textColor.a = alpha;
        regularFont.setColor(textColor);

        GlyphLayout layout = new GlyphLayout(regularFont, alertText);
        regularFont.draw(batch, alertText,
                x + (boxWidth - layout.width) / 2,
                y + (boxHeight + layout.height) / 2);

        batch.end();
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

        // Background
        shapeRenderer.setColor(0, 0, 0, 0.5f);
        shapeRenderer.rect(originX - 4, originY - 4, MINIMAP_SIZE + 8, MINIMAP_SIZE + 8);

        // Map cells
        for (int x = 0; x < MAP_WIDTH; x++) {
            for (int y = 0; y < MAP_HEIGHT; y++) {
                shapeRenderer.setColor(map[x][y] == 0 ? Color.DARK_GRAY : Color.LIGHT_GRAY);
                shapeRenderer.rect(originX + x * cellSize, originY + y * cellSize, cellSize, cellSize);
            }
        }

        // Enemies
        for (Enemy enemy : enemies) {
            if (!enemy.destroyed) {
                shapeRenderer.setColor(Color.RED);
                shapeRenderer.ellipse(
                        originX + enemy.position.x * cellSize + cellSize / 4,
                        originY + enemy.position.y * cellSize + cellSize / 4,
                        cellSize / 2, cellSize / 2
                );
            }
        }

        // Player
        shapeRenderer.setColor(Color.BLUE);
        shapeRenderer.ellipse(
                originX + gridPosition.x * cellSize + cellSize / 4,
                originY + gridPosition.y * cellSize + cellSize / 4,
                cellSize / 2, cellSize / 2
        );

        shapeRenderer.end();
    }

    // Public setters and getters for external access
    public void setTyped() {
        isTyped = true;
    }

    public void activateCurrentEnemy() {
        if (currentEnemyIdx < enemies.size && !enemies.get(currentEnemyIdx).destroyed) {
            Enemy enemy = enemies.get(currentEnemyIdx);
            startEnemyTimer(enemy);
        }
    }

    public IsometricGame getGame() {
        return game;
    }

    public void setGame(IsometricGame game) { /* readonly */ }

    public float getPlayerHealth() {
        return playerHealth;
    }

    public void setPlayerHealth(float playerHealth) {
        this.playerHealth = MathUtils.clamp(playerHealth, 0, playerMaxHealth);
    }

    public float getPlayerMana() {
        return playerMana;
    }

    public void setPlayerMana(float playerMana) {
        this.playerMana = MathUtils.clamp(playerMana, 0, playerMaxMana);
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public boolean isGameEnded() {
        return gameEnded;
    }

    // Screen lifecycle methods
    @Override
    public void resize(int width, int height) {
        if (uiCamera != null) {
            uiCamera.setToOrtho(false, width, height);
            uiCamera.update();
        }
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
        // Dispose all resources
        if (modelBatch != null) modelBatch.dispose();
        if (boxModel != null) boxModel.dispose();
        if (ceilingModel != null) ceilingModel.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (sceneManager != null) sceneManager.dispose();
        if (enemyModel != null) enemyModel.dispose();
        if (hudBatch != null) hudBatch.dispose();
        if (whiteTexture != null) whiteTexture.dispose();
        if (decalBatch != null) decalBatch.dispose();
        if (tempBatch != null) tempBatch.dispose();
        if (frameBuffer != null) frameBuffer.dispose();
        if (regularFont != null) regularFont.dispose();
        if (wordFont != null) wordFont.dispose();

        // Dispose word textures
        if (wordTextures != null) {
            for (TextureRegion texture : wordTextures) {
                if (texture != null && texture.getTexture() != null) {
                    texture.getTexture().dispose();
                }
            }
        }
    }

    // Utility class for 2D integer vectors
    public static class Vector2Int {
        int x, y;

        Vector2Int(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public void set(int x, int y) {
            this.x = x;
            this.y = y;
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

        @Override
        public String toString() {
            return "Vector2Int(" + x + ", " + y + ")";
        }
    }
}