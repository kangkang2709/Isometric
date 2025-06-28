package ctu.game.isometric.view.screen;

import com.badlogic.gdx.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.*;
import com.badlogic.gdx.graphics.g3d.environment.*;
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader;
import com.badlogic.gdx.graphics.g3d.utils.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;
import ctu.game.isometric.IsometricGame;
import ctu.game.isometric.controller.GameController;
import ctu.game.isometric.controller.dungeon.DungeonInputController;

public class LinearCaveScreen implements Screen {
    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private Environment environment;
    private Array<ModelInstance> instances = new Array<>();
    private Model boxModel;
    private Model ceilingModel;
    private Model playerModel;
    private ModelInstance playerInstance;

    private final int MAP_WIDTH = 20;
    private final int MAP_HEIGHT = 20;
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
    DungeonInputController dugeonInputController;

    public LinearCaveScreen(IsometricGame game, GameController gameController) {
        this.gameController = gameController;
        this.game = game;
        this.dugeonInputController = new DungeonInputController(this);

    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(this.dugeonInputController);

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
    }

    private void createModels() {
        ModelBuilder modelBuilder = new ModelBuilder();
        boxModel = modelBuilder.createBox(1f, 1f, 1f,
                new Material(ColorAttribute.createDiffuse(Color.GRAY)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

        ceilingModel = modelBuilder.createBox(1f, 0.1f, 1f,
                new Material(ColorAttribute.createDiffuse(new Color(0.2f, 0.2f, 0.25f, 1f))),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);


        // Player: half sized box, blue

        ObjLoader objLoader = new ObjLoader();
        FileHandle fileHandle = Gdx.files.internal("3d/book3.obj");

        playerModel = objLoader.loadModel(fileHandle);
        playerInstance = new ModelInstance(playerModel);


    }

    private void buildMap() {
        instances.clear();
        for (int x = 0; x < MAP_WIDTH; x++) {
            for (int y = 0; y < MAP_HEIGHT; y++) {
                if (map[x][y] == 0) {
                    // Tường
                    ModelInstance wall = new ModelInstance(boxModel);
                    wall.transform.setToTranslation(x, 0.5f, y);
                    instances.add(wall);

                    // Trần nhà phía trên tường
//                    ModelInstance ceiling = new ModelInstance(ceilingModel);
//                    ceiling.transform.setToTranslation(x, 1.05f, y);
//                    instances.add(ceiling);
                } else {
                    // Trần nhà phía trên sàn trống
                    ModelInstance ceiling = new ModelInstance(ceilingModel);
                    ceiling.transform.setToTranslation(x, 1.05f, y);
                    instances.add(ceiling);
                }
            }
        }
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

    private void handleGridMovement(float delta) {
//        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
//            game.changeScreen("GAME");
//            return;
//        }

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
        // Vị trí camera đúng ngay vị trí player, cao hơn một chút (tầm mắt)
        float eyeHeight = 0.2f; // hoặc 0.25f, điều chỉnh cho vừa ý

        camera.position.set(basePos.x - 0.2f, basePos.y + eyeHeight, basePos.z - 0.15f);

        // Camera nhìn thẳng theo hướng di chuyển của player
        camera.lookAt(
                basePos.x + playerDirection.x - 0.2f, // Một chút lệch để nhìn rõ hơn
                basePos.y + eyeHeight,
                basePos.z + playerDirection.z
        );
        camera.up.set(Vector3.Y);
        camera.update();
    }


    Vector3 vector1 = new Vector3(-1, 0, 0);
    Vector3 vector2 = new Vector3(0, 0, -1);
    Vector3 vector3 = new Vector3(1, 0, 0);

    @Override
    public void render(float delta) {
        handleGridMovement(delta);
        updateCameraFromPlayer(delta);

        Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        modelBatch.begin(camera);
        for (ModelInstance instance : instances) {
            modelBatch.render(instance, environment);
        }

        // Player
        if (playerDirection.equals(vector1)) {
            playerInstance.transform.setToTranslation(playerPosition.x - 0.31f, playerPosition.y - 0.05f, playerPosition.z + 0.2f);
        } else if (playerDirection.equals(vector2)) {
            playerInstance.transform.setToTranslation(playerPosition.x - 0.5f, playerPosition.y - 0.05f, playerPosition.z - 0.3f);
        } else playerInstance.transform.setToTranslation(playerPosition.x, playerPosition.y - 0.05f, playerPosition.z);

        float angle = (float) Math.atan2(playerDirection.x, playerDirection.z) * MathUtils.radiansToDegrees;
        playerInstance.transform.rotate(Vector3.Y, angle);
        modelBatch.render(playerInstance, environment);

        modelBatch.end();
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
        playerModel.dispose();
        ceilingModel.dispose();
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
}