package ctu.game.isometric.view.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.renderers.IsometricTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;
import ctu.game.isometric.model.entity.Character;
import ctu.game.isometric.model.world.IsometricMap;
import ctu.game.isometric.util.AnimationManager;
import ctu.game.isometric.util.AssetManager;

public class MapRenderer {
    private IsometricMap map;
    private AssetManager assetManager;
    private float offsetX, offsetY;
    private AnimationManager animationManager;
    private Character character;
    private IsometricTiledMapRenderer tiledMapRenderer;
    private OrthographicCamera camera;
    Texture backgroundTexture;

    private float cameraZoom = 1.0f;
    private static final float MIN_ZOOM = 0.5f;
    private static final float MAX_ZOOM = 2.0f;
    private static final float ZOOM_STEP = 0.1f;


    // In MapRenderer.java - modify constructor to take an existing camera
    public MapRenderer(IsometricMap map, AssetManager assetManager, Character character, OrthographicCamera camera) {
        this.map = map;
        this.assetManager = assetManager;
        this.character = character;
        this.animationManager = assetManager.getAnimationManager();
        this.offsetX = 640;
        this.offsetY = 150;

        // Use the provided camera instead of creating a new one
        this.camera = camera;
        backgroundTexture = new Texture(Gdx.files.internal("maps/background.png"));
        // Create the tiled map renderer
        this.tiledMapRenderer = new IsometricTiledMapRenderer(map.getTiledMap());
    }

    public float[] toIsometric(float x, float y, float zoom) {
        float isoX = (x + y) * (map.getTileWidth() / 2.0f) * zoom;
        float isoY = (y - x) * (map.getTileHeight() / 2.0f) * zoom;
        return new float[]{isoX, isoY};
    }

    // Add overloaded method to maintain compatibility with existing code
    public float[] toIsometric(float x, float y) {
        return toIsometric(x, y, 1.0f);
    }

    public void render(SpriteBatch batch) {
        // Draw background for the entire screen
        float bgX = camera.position.x - (Gdx.graphics.getWidth() / 2f);
        float bgY = camera.position.y - (Gdx.graphics.getHeight() / 2f);
        batch.draw(backgroundTexture, bgX, bgY, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // Update camera position based on character position
        float[] isoPos = toIsometric(character.getGridX(), character.getGridY());
        camera.position.set(isoPos[0], isoPos[1], 0);
        camera.update();

        // End batch if currently drawing to use renderer
        boolean batchWasDrawing = batch.isDrawing();

        if (batchWasDrawing) {
            batch.end();
        }

        // Render tile map
        tiledMapRenderer.setView(camera);
        tiledMapRenderer.render();

        // Resume batch if it was drawing before
        if (batchWasDrawing) {
            batch.begin();
            batch.setProjectionMatrix(camera.combined);

            // Render collision areas
//            renderCollisionAreas(batch);
            renderCollisionDebug(batch);
//            renderObjectLayer(batch, "object_layer");
        }
    }
    private void renderObjectLayer(SpriteBatch batch, String layerName) {
        MapLayer objectLayer = map.getTiledMap().getLayers().get(layerName);
        if (objectLayer != null) {
            for (MapObject object : objectLayer.getObjects()) {

                float x = object.getProperties().get("x", Float.class);
                float y = object.getProperties().get("y", Float.class);

                // Check if this is a tile object
                if (object.getProperties().containsKey("gid")) {
                    int gid = object.getProperties().get("gid", Integer.class);
                    float width = object.getProperties().containsKey("width") ?
                            object.getProperties().get("width", Float.class) : map.getTileWidth();
                    float height = object.getProperties().containsKey("height") ?
                            object.getProperties().get("height", Float.class) : map.getTileHeight();

                    // Find the tile in all map tilesets
                    TiledMapTile tile = null;
                    for (com.badlogic.gdx.maps.tiled.TiledMapTileSet tileset : map.getTiledMap().getTileSets()) {
                        tile = tileset.getTile(gid);
                        if (tile != null) break;
                    }

                    if (tile != null) {
                        // Draw the tile's texture region at the object position
                        TextureRegion region = tile.getTextureRegion();
                        batch.draw(region,
                                x, y - height,  // Adjust for isometric positioning
                                width, height);
                    }
                }
            }
        }
    }

    //


    public void renderCollisionDebug(SpriteBatch batch) {
        // Tạo một đối tượng ShapeRenderer để vẽ các vùng va chạm
        ShapeRenderer shapeRenderer = new ShapeRenderer();
        shapeRenderer.setProjectionMatrix(camera.combined);

        // Lưu lại trạng thái vẽ của batch (nếu batch đang vẽ, ta sẽ kết thúc vẽ trước khi dùng ShapeRenderer)
        boolean wasDrawing = batch.isDrawing();
        if (wasDrawing) {
            batch.end();
        }

        // Bắt đầu vẽ các hình bằng ShapeRenderer
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line); // Dùng vẽ đường viền

        // Lấy lớp va chạm từ bản đồ
        MapLayer collisionLayer = map.getTiledMap().getLayers().get("collision_layer");

        if (collisionLayer != null) {
            // Duyệt qua tất cả các đối tượng trong lớp va chạm
            for (MapObject object : collisionLayer.getObjects()) {
                if (object instanceof RectangleMapObject) {
                    // Nếu đối tượng là hình chữ nhật
                    Rectangle rect = ((RectangleMapObject) object).getRectangle();



                    float[] isoStart = toIsometric(rect.x, rect.y);
                    float isoWidth = rect.width * (map.getTileWidth() / 2f);
                    float isoHeight = rect.height * (map.getTileHeight() / 2f);

                    shapeRenderer.setColor(1f, 0f, 0f, 1f);
                    shapeRenderer.rect(isoStart[0], isoStart[1] - isoHeight, isoWidth, isoHeight);

                } else if (object instanceof PolygonMapObject) {
                    // Nếu đối tượng là đa giác
                    PolygonMapObject polygonObject = (PolygonMapObject) object;

                    // Vẽ các điểm của đa giác
                    shapeRenderer.setColor(0f, 0f, 1f, 1f); // Màu xanh dương
                    float[] vertices = polygonObject.getPolygon().getTransformedVertices();
                    for (int i = 0; i < vertices.length; i += 2) {
                        float x = vertices[i];
                        float y = vertices[i + 1];
                        shapeRenderer.point(x, y, 0);
                    }
                }
            }
        }

        // Kết thúc việc vẽ các hình
        shapeRenderer.end();
        shapeRenderer.dispose();

        // Nếu trước đó batch đang vẽ, ta sẽ bắt đầu vẽ lại batch
        if (wasDrawing) {
            batch.begin();
        }
    }


    public void renderCollisionAreas(SpriteBatch batch) {
        // Store original batch color
        Color originalColor = batch.getColor().cpy();

        // Get the collision layer from the map
        MapLayer collisionLayer = map.getTiledMap().getLayers().get("collision_layer");

        if (collisionLayer != null) {
            ShapeRenderer shapeRenderer = new ShapeRenderer();
            shapeRenderer.setProjectionMatrix(camera.combined);

            boolean wasDrawing = batch.isDrawing();
            if (wasDrawing) {
                batch.end();
            }

            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(1f, 0.2f, 0.2f, 0.4f);

            for (MapObject object : collisionLayer.getObjects()) {
                if (object instanceof RectangleMapObject) {
                    // Handle rectangle objects
                    Rectangle rect = ((RectangleMapObject) object).getRectangle();
                    shapeRenderer.rect(rect.x, rect.y, rect.width, rect.height);
                } else if (object instanceof PolygonMapObject polygonObject && polygonObject.getPolygon() != null) {
                    // Handle polygon objects - either draw as triangles or as bounding rectangles
                    float[] vertices = polygonObject.getPolygon().getTransformedVertices();

                    // Find bounding rectangle
                    float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
                    float maxX = Float.MIN_VALUE, maxY = Float.MIN_VALUE;

                    for (int i = 0; i < vertices.length; i += 2) {
                        float x = vertices[i];
                        float y = vertices[i + 1];

                        minX = Math.min(minX, x);
                        minY = Math.min(minY, y);
                        maxX = Math.max(maxX, x);
                        maxY = Math.max(maxY, y);
                    }

                    // Draw rectangle
                    shapeRenderer.rect(minX, minY, maxX - minX, maxY - minY);
                }
            }

            shapeRenderer.end();
            shapeRenderer.dispose();

            if (wasDrawing) {
                batch.begin();
            }
        }

        // Restore original color
        batch.setColor(originalColor);
    }

    public void renderWalkableTileHighlights(SpriteBatch batch, boolean[][] walkableTiles, float stateTime) {
        // Get character position
        int characterX = (int) Math.floor(character.getGridX());
        int characterY = (int) Math.floor(character.getGridY());

        // Store original color
        Color originalColor = new Color(batch.getColor());

        // Set highlight color (semi-transparent green)
        batch.setColor(0.2f, 1f, 0.2f, 0.5f);

        // Define adjacent tiles in correct drawing order for isometric depth
        int[][] adjacentTiles = {
                {characterX - 1, characterY - 1}, // left_up
                {characterX, characterY - 1},     // up
                {characterX - 1, characterY},     // left
                {characterX, characterY},         // center
                {characterX + 1, characterY - 1}, // right_up
                {characterX - 1, characterY + 1}, // left_down
                {characterX + 1, characterY},     // right
                {characterX, characterY + 1},     // down
                {characterX + 1, characterY + 1}  // right_down
        };

        // Sort by isometric depth (higher x+y is further back, then by y)
        java.util.Arrays.sort(adjacentTiles, (a, b) -> {
            int sumComparison = Integer.compare(a[0] + a[1], b[0] + b[1]);
            if (sumComparison != 0) {
                return sumComparison;
            }
            return Integer.compare(a[1], b[1]); // Compare y-values for tiles with the same sum
        });

        // Get the base layer for highlighting
        TiledMapTileLayer baseLayer = (TiledMapTileLayer) map.getTiledMap().getLayers().get("ground_layer");

        // Ensure walkableTiles array matches map dimensions
        if (walkableTiles != null &&
                walkableTiles.length == map.getMapHeight() &&
                walkableTiles[0].length == map.getMapWidth()) {

            for (int[] tile : adjacentTiles) {
                int x = tile[0];
                int y = tile[1];

                // Check if tile is within bounds and walkable
                if (x >= 0 && x < walkableTiles[0].length &&
                        y >= 0 && y < walkableTiles.length &&
                        walkableTiles[y][x]) {

                    TiledMapTileLayer.Cell cell = baseLayer.getCell(x, y);
                    if (cell != null) {
                        TiledMapTile mapTile = cell.getTile();
                        if (mapTile != null) {
                            TextureRegion tileRegion = mapTile.getTextureRegion();
                            float[] iso = toIsometric(x, y);

                            // Draw at the center position, adjusting for tile dimensions
                            float tileWidth = map.getTileWidth();
                            float tileHeight = map.getTileHeight();
                            batch.draw(tileRegion,
                                    iso[0],
                                    iso[1],
                                    tileWidth,
                                    tileHeight);
                        }
                    }
                }
            }
        }

        // Restore original color
        batch.setColor(originalColor);
    }



    public float getOffsetX() {
        return offsetX;
    }

    public float getOffsetY() {
        return offsetY;
    }

    public IsometricMap getMap() {
        return map;
    }

    public void setMap(IsometricMap map) {
        this.map = map;
    }
}