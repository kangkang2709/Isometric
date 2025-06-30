package ctu.game.isometric.view.screen;

import com.badlogic.gdx.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.*;
import com.badlogic.gdx.graphics.g3d.environment.*;
import com.badlogic.gdx.graphics.g3d.model.Animation;
import com.badlogic.gdx.graphics.g3d.utils.*;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;
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

    // Player: half box
    private final float PLAYER_SCALE = 0.5f;

    private Vector3 playerPosition = new Vector3();
    private Vector3 playerDirection = new Vector3(1, 0, 0);
    private Vector2Int gridPosition = new Vector2Int(1, 1);
    private Vector2Int moveDirection = null;
    private boolean isMoving = false;
    private float moveTimer = 0f;
    private final float MOVE_DURATION = 0.20f; // Smooth, a bit slower
    private float shakeTime = 0f;

    // Movement interpolation
    private Vector3 moveStart = new Vector3();
    private Vector3 moveEnd = new Vector3();

    GameController gameController;
    IsometricGame game;
    DungeonInputController dungeonInputController;

    public LinearCaveScreen(IsometricGame game, GameController gameController) {
        this.gameController = gameController;
        this.game = game;
        this.dungeonInputController = new DungeonInputController(this);
    }

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

    // Flow effect
    private ModelInstance flowInstance;
    private Model flowCylinder;
    private boolean showFlowInstance = true; // Toggle for displaying flow

    public void startDugeon() {
        this.playerMaxHealth = gameController.getCharacter().getMaxHealth();
        this.playerMaxMana = gameController.getCharacter().getMaxMana();
        this.playerHealth = gameController.getCharacter().getHealth();
        this.playerMana = gameController.getCharacter().getMana();
    }

    public void endDungeon() {
        gameController.getCharacter().setHealth(playerHealth);
        gameController.getCharacter().setMana(playerMana);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(this.dungeonInputController);
        uiCamera = new OrthographicCamera();
        uiCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        uiCamera.update();

        shapeRenderer = new ShapeRenderer();

        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 0.1f;
        camera.far = 100f;

        modelBatch = new ModelBatch();

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.8f, 0.8f, 0.8f, 1f));
        environment.add(new DirectionalLight().set(1f, 1f, 1f, -1f, -0.8f, -0.2f));

        createModels();
        map = generateLinearMaze(MAP_WIDTH, MAP_HEIGHT);
        buildMap();

        // Ensure starting position is walkable
        if (map[1][1] != 1) {
            OUTER_LOOP:
            for (int x = 1; x < MAP_WIDTH - 1; x++) {
                for (int y = 1; y < MAP_HEIGHT - 1; y++) {
                    if (map[x][y] == 1) {
                        gridPosition = new Vector2Int(x, y);
                        break OUTER_LOOP;
                    }
                }
            }
        }
        updatePlayerPositionFromGrid();
        updateCameraFromPlayer(0f);
        camera.update();

        // Create flow cylinder model/instance (invisible at first, position will be updated to follow player)
        createFlowCylinder();

        // Tạo camera cho UI
        hudBatch = new SpriteBatch();
        // Tạo texture trắng 1x1
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1, 1, 1, 1);
        pixmap.fill();
        whiteTexture = new Texture(pixmap);
        pixmap.dispose();

        regularFont = new BitmapFont(); // Có thể thay font khác nếu muốn

    }

    private void createFlowCylinder() {
        // Tạo cylinder trong suốt phát sáng
        ModelBuilder modelBuilder = new ModelBuilder();
        Material glowMaterial = new Material(
                ColorAttribute.createDiffuse(new Color(0.4f, 0.8f, 1f, 0.5f)), // Màu xanh, alpha 0.5
                ColorAttribute.createEmissive(new Color(0.2f, 0.7f, 1f, 1f)), // Phát sáng nhẹ
                new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, 0.5f)
        );
        flowCylinder = modelBuilder.createCylinder(
                0.2f,   // đường kính đáy < 1 để không trùng hoàn toàn box
                0.2f,     // cao hơn box
                0.2f,   // đường kính đỉnh
                32,
                glowMaterial,
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal
        );
// Offset vị trí lên trên box một chút

        // Sẽ được đặt vị trí tại render để luôn đi theo nhân vật
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

    public void createModels() {
        ModelBuilder modelBuilder = new ModelBuilder();

        // Tạo box model (để so sánh)
        boxModel = modelBuilder.createBox(1f, 1.4f, 1f,
                new Material(ColorAttribute.createDiffuse(Color.GRAY)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

        // Tạo trần nhà
        ceilingModel = modelBuilder.createBox(1f, 0.1f, 1f,
                new Material(ColorAttribute.createDiffuse(new Color(0.2f, 0.2f, 0.25f, 1f))),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

        // Load mô hình GLTF
        SceneAsset sceneAsset = new GLTFLoader().load(Gdx.files.internal("3d/Untitled7.gltf"));

        // Log ra các animation nếu có
        for (Animation entry : sceneAsset.animations) {
            System.out.println("Animation: " + entry.id);
        }

        // Khởi tạo Scene từ GLTF
        scene = new Scene(sceneAsset.scene);
        scene.modelInstance.transform.setToTranslation(0, 0, 0);

        // Tạo SceneManager và gán camera
        sceneManager = new SceneManager();
        sceneManager.setCamera(camera);
        sceneManager.addScene(scene);


        // Cài đặt animation nếu có
        if (!sceneAsset.animations.isEmpty()) {
            scene.animationController.setAnimation(sceneAsset.animations.first().id, -1);
        }


        sceneAsset = null;
        sceneAsset = new GLBLoader().load(Gdx.files.internal("3d/slime.glb"));
        enemyScene = new Scene(sceneAsset.scene);
        enemyScene.modelInstance.transform.setToTranslation(1f, 0.25f, 2f);
        enemySceneManager = new SceneManager();
        enemySceneManager.setCamera(camera);
        enemySceneManager.addScene(enemyScene);

        // Cài đặt animation nếu có
        if (!sceneAsset.animations.isEmpty()) {
            enemyScene.animationController.setAnimation(sceneAsset.animations.first().id, -1);
        }

        // Tạo shader config PBR
        PBRShaderConfig config = PBRShaderProvider.createDefaultConfig();
        config.numBones = 60; // cần nếu có skeleton
        sceneManager.setShaderProvider(new PBRShaderProvider(config));

        // Ánh sáng môi trường và directional light
        sceneManager.environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.3f, 0.3f, 0.3f, 1f));
        sceneManager.environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));
    }

    Scene scene;
    SceneManager sceneManager;

    Scene enemyScene;
    SceneManager enemySceneManager;

    private void buildMap() {
        instances.clear();
        for (int x = 0; x < MAP_WIDTH; x++) {
            for (int y = 0; y < MAP_HEIGHT; y++) {
                if (map[x][y] == 0) {
                    boolean adjacentToFloor = false;
                    for (int dx = -1; dx <= 1 && !adjacentToFloor; dx++) {
                        for (int dy = -1; dy <= 1 && !adjacentToFloor; dy++) {
                            if (Math.abs(dx) + Math.abs(dy) != 1) continue; // Chỉ xét 4 hướng
                            int nx = x + dx;
                            int ny = y + dy;
                            if (nx >= 0 && ny >= 0 && nx < MAP_WIDTH && ny < MAP_HEIGHT && map[nx][ny] == 1) {
                                adjacentToFloor = true;
                            }
                        }
                    }
                    if (!adjacentToFloor) continue; // Bỏ qua nếu không liền kề floor

                    ModelInstance wall = new ModelInstance(boxModel);
                    wall.transform.setToTranslation(x, 0.5f, y);
                    instances.add(wall);
                } else {
                    // ModelInstance ceiling = new ModelInstance(ceilingModel);
                    // ceiling.transform.setToTranslation(x, 1.05f, y);
                    // instances.add(ceiling);
                }
            }
        }
        System.out.println("Map built with " + instances.size + " instances.");
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
                        break;
                    }
                }
            }

            if (!moved) break;
        }

        return map;
    }

    private void drawMiniMap() {
        shapeRenderer.setProjectionMatrix(uiCamera.combined);

        int mapSize = Math.max(MAP_WIDTH, MAP_HEIGHT);
        float cellSize = (float) MINIMAP_SIZE / mapSize;
        float originX = 1280 - MINIMAP_SIZE - MINIMAP_PADDING; // Right side of the screen
        float originY = MINIMAP_PADDING;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Minimap background
        shapeRenderer.setColor(0, 0, 0, 0.5f);
        shapeRenderer.rect(originX - 4, originY - 4, MINIMAP_SIZE + 8, MINIMAP_SIZE + 8);

        // Map cells
        for (int x = 0; x < MAP_WIDTH; x++) {
            for (int y = 0; y < MAP_HEIGHT; y++) {
                if (map[x][y] == 0) {
                    shapeRenderer.setColor(Color.DARK_GRAY);
                } else {
                    shapeRenderer.setColor(Color.LIGHT_GRAY);
                }
                shapeRenderer.rect(
                        originX + x * cellSize,
                        originY + y * cellSize,
                        cellSize,
                        cellSize
                );
            }
        }

        // Player
        shapeRenderer.setColor(Color.BLUE);
        shapeRenderer.ellipse(
                originX + gridPosition.x * cellSize + cellSize / 4,
                originY + gridPosition.y * cellSize + cellSize / 4,
                cellSize / 2,
                cellSize / 2
        );

        shapeRenderer.end();
    }

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
                if (isWalkable(target)) {
                    // Setup smooth movement
                    moveStart.set(gridToWorld(gridPosition));
                    moveEnd.set(gridToWorld(target));
                    gridPosition = target;
                    isMoving = true;
                    moveTimer = 0f;
                    shakeTime = 0f;
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
                updatePlayerPositionFromGrid(); // Snap to grid at the end
            }
        }
    }


    private boolean isWalkable(Vector2Int pos) {
        return pos.x >= 0 && pos.y >= 0 && pos.x < MAP_WIDTH && pos.y < MAP_HEIGHT && map[pos.x][pos.y] == 1;
    }

    private void updatePlayerPositionFromGrid() {
        playerPosition.set(gridToWorld(gridPosition));
    }

    // Helper: converts grid position to world position (centered, on floor)
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
        // Camera position is at player location, slightly higher (eye level)
        float eyeHeight = 0.2f;

        camera.position.set(basePos.x - 0.2f, basePos.y + eyeHeight, basePos.z - 0.15f);

        // Camera looks straight in the player's movement direction
        camera.lookAt(
                basePos.x + playerDirection.x - 0.2f,
                basePos.y + eyeHeight,
                basePos.z + playerDirection.z
        );

        camera.up.set(Vector3.Y);
        camera.update();
    }

    Vector3 vector1 = new Vector3(-1, 0, 0);
    Vector3 vector2 = new Vector3(0, 0, -1);

    @Override
    public void render(float delta) {
        handleGridMovement(delta);
        updateCameraFromPlayer(delta);

        Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        modelBatch.begin(camera);
        sceneManager.update(delta);
//        for (ModelInstance instance : instances) {
//            modelBatch.render(instance, environment);
//        }

        enemySceneManager.update(delta);

        // Player model transform/rotation
        if (playerDirection.equals(vector1)) {
            scene.modelInstance.transform.setToTranslation(playerPosition.x - 0.31f, playerPosition.y - 0.05f, playerPosition.z + 0.2f);
        } else if (playerDirection.equals(vector2)) {
            scene.modelInstance.transform.setToTranslation(playerPosition.x - 0.5f, playerPosition.y - 0.05f, playerPosition.z - 0.3f);
        } else {
            scene.modelInstance.transform.setToTranslation(playerPosition.x, playerPosition.y - 0.05f, playerPosition.z);
        }

        float angle = (float) Math.atan2(playerDirection.x, playerDirection.z) * MathUtils.radiansToDegrees;
        scene.modelInstance.transform.rotate(Vector3.Y, angle);

        modelBatch.render(scene.modelInstance, environment);
        modelBatch.render(enemyScene.modelInstance, environment);

        modelBatch.end();
        drawMiniMap();
        drawPlayerHUD(hudBatch);
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
        if (flowCylinder != null) flowCylinder.dispose();
        if (sceneManager != null) {
            sceneManager.dispose();
        }
        if (gameController != null) {
            gameController.dispose();
        }
        if (game != null) {
            game.dispose();
        }
    }

    static class Vector2Int {
        int x, y;

        Vector2Int(int x, int y) {
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