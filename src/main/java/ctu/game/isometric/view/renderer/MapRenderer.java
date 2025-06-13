package ctu.game.isometric.view.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.maps.tiled.renderers.IsometricTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import ctu.game.isometric.controller.EventManager;
import ctu.game.isometric.model.entity.Character;
import ctu.game.isometric.model.puzzle.PressurePlatePuzzle;
import ctu.game.isometric.model.world.IsometricMap;
import ctu.game.isometric.model.world.MapEvent;
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

    private BitmapFont font;

    private float cameraZoom = 0.5f;
    private static final float MIN_ZOOM = 0.5f;
    private static final float MAX_ZOOM = 2.0f;
    private static final float ZOOM_STEP = 0.1f;
    Texture buttonTexture;

    private EventManager eventManager;

    // In MapRenderer.java - modify constructor to take an existing camera
    public MapRenderer(IsometricMap map, AssetManager assetManager, EventManager eventManager, Character character, OrthographicCamera camera) {
        this.map = map;
        this.assetManager = assetManager;
        this.eventManager = eventManager;
        this.character = character;
        this.animationManager = assetManager.getAnimationManager();
        this.offsetX = 640;
        this.offsetY = 150;

        buttonTexture = new Texture(Gdx.files.internal("ui/action_icon.png"));

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Tektur-Bold.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        // Initialize fonts

        parameter.size = 8; // Increased from 10 to 14
        parameter.color = Color.WHITE; // Changed to white for better visibility
        parameter.borderWidth = 1.5f; // Add outline
        parameter.borderColor = Color.BLACK; // Black outline for contrast
        parameter.shadowOffsetX = 1; // Add shadow for depth
        parameter.shadowOffsetY = 1;
        parameter.shadowColor = new Color(0, 0, 0, 0.5f);
        this.font = generator.generateFont(parameter);
        generator.dispose();

        initLeafParticles();
        // Use the provided camera instead of creating a new one
        this.camera = camera;
//        backgroundTexture = new Texture(Gdx.files.internal("maps/background.png"));
        // Create the tiled map renderer
        this.tiledMapRenderer = new IsometricTiledMapRenderer(map.getTiledMap());
         this.fogTexture= new Texture(Gdx.files.internal("textures/fog.png"));
    }

    Texture fogTexture;

    public void renderPressurePlate(SpriteBatch batch) {
        for (PressurePlatePuzzle.PressurePlate plate : map.getPuzzle().getPlates()) {
            TextureRegion texture = plate.getCurrentTexture();
            if (texture != null) {
                float[] isoPos = toIsometric(plate.getGridX(), plate.getGridY());
                batch.draw(texture, isoPos[0], isoPos[1], 64, 32);
            }
            if (!plate.getEffectType().equals("trap") && !plate.isActivated()) {
                float[] isoPos = toIsometric(plate.getTargetX(), plate.getTargetY());
                batch.draw(fogTexture, isoPos[0]-16, isoPos[1]-10, 64, 64);
            }
        }
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public void setEventManager(EventManager eventManager) {
        this.eventManager = eventManager;
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

    public OrthographicCamera getCamera() {
        return camera;
    }

    public void setCamera(OrthographicCamera camera) {
        this.camera = camera;
    }

    public void render(SpriteBatch batch) {
        // Draw background for the entire screen
        float bgX = camera.position.x - (Gdx.graphics.getWidth() / 2f);
        float bgY = camera.position.y - (Gdx.graphics.getHeight() / 2f);
//        batch.draw(backgroundTexture, bgX, bgY, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

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
            renderObjectLayer(batch, "object");
            renderPressurePlate(batch);
            renderLeaves(batch, Gdx.graphics.getDeltaTime());
        }
    }

    private void renderObjectLayer(SpriteBatch batch, String layerName) {
        MapLayer objectLayer = map.getTiledMap().getLayers().get(layerName);
        if (objectLayer != null) {
            for (MapObject object : objectLayer.getObjects()) {
                Float x = object.getProperties().get("x", Float.class);
                Float y = object.getProperties().get("y", Float.class);

                // Skip completed events
                if (object.getProperties().containsKey("id")) {
                    String eventId = eventManager.getStringProperty(object.getProperties(), "id", "");
                    MapEvent event = eventManager.getEvent(eventId);
                    if (event != null && event.isOneTime() && event.isCompleted()) {
                        continue; // Skip rendering this object
                    }
                }

                if (x != null && y != null && object.getProperties().containsKey("gid")) {
                    int gid = object.getProperties().get("gid", Integer.class);
                    Float width = object.getProperties().get("width", Float.class);
                    Float height = object.getProperties().get("height", Float.class);

                    // Provide default values for width and height if null
                    if (width == null) width = (float) map.getTileWidth(); // Cast to float
                    if (height == null) height = (float) map.getTileHeight(); // Cast to float

                    // Find the tile in all map tilesets
                    TiledMapTile tile = map.getTiledMap().getTileSets().getTile(gid);

                    if (tile != null) {
                        int[] gridPos = toGrid(x, y);

                        // In Tiled, Y is at the bottom of object. Adjust for isometric view.
                        float gridX = gridPos[0];
                        float gridY = gridPos[1];

                        float[] isoPos = toIsometric(gridX, gridY);
                        TextureRegion region = tile.getTextureRegion();
                        batch.draw(region,
                                isoPos[0] - width / 2,      // Center horizontally
                                isoPos[1] - height / 4,     // Improved alignment for isometric view
                                width, height);
                    }
                }
            }
        }
    }

    // Convert world coordinates to grid coordinates
    private int[] toGrid(float worldX, float worldY) {
        // Basic conversion: divide by tile dimensions
        float gridX = worldX / map.getTileWidth();
        float gridY = worldY / map.getTileHeight();

        // For isometric systems, you might need this transformation instead:
        // float gridX = (worldX / (map.getTileWidth() / 2) - worldY / (map.getTileHeight() / 2)) / 2;
        // float gridY = (worldX / (map.getTileWidth() / 2) + worldY / (map.getTileHeight() / 2)) / 2;

        // Round to integers
        int x = Math.round(gridX);
        int y = Math.round(gridY);

        // Ensure positive values by adding an offset if needed
        // If your map can have negative coordinates, add an appropriate offset
        int mapOffsetX = 1; // Adjust as needed for your map
        int mapOffsetY = -1; // Adjust as needed for your map

        return new int[]{x + mapOffsetX, y + mapOffsetY};
    }
    //

    public AnimationManager getAnimationManager() {
        return animationManager;
    }

    public void setAnimationManager(AnimationManager animationManager) {
        this.animationManager = animationManager;
    }

    public void renderActionButton(SpriteBatch batch, String eventType, MapEvent event, float x, float y) {
        if (eventType == null || event == null) return;


        if (event.isOneTime() && event.isCompleted()) {
            return;
        }

        String buttonText = "Action";

        // Set text based on event type
        switch (eventType) {
            case "battle":
                buttonText = "Chiến đấu";
                break;
            case "dialog":
                buttonText = "Nói chuyện";
                break;
            case "cutscene":
                buttonText = "Xem Cắt Cảnh";
                break;
            case "quiz":
                buttonText = "Nói chuyện";
                break;
            case "teleport":
                buttonText = "Dịch Chuyển";
                break;
            case "treasure":
                buttonText = "Mở rương";
                break;
        }

        if (buttonTexture != null) {
            // Convert grid coordinates to isometric screen coordinates
            float[] isoPos = toIsometric(x, y);
            // Position the button above the tile

            float buttonX = isoPos[0] - buttonTexture.getWidth() / 2;
            float buttonY = isoPos[1] + map.getTileHeight() / 2;

            // Draw button with subtle animation (floating effect)
            float offsetY = (float) Math.sin(Gdx.graphics.getDeltaTime() * 3) * 5;
            batch.draw(buttonTexture, buttonX, buttonY + offsetY);

            // Draw the text if font exists
            if (font != null) {
                GlyphLayout layout = new GlyphLayout(font, buttonText);
                font.draw(batch, buttonText,
                        buttonX + buttonTexture.getWidth() / 2 - layout.width / 2,
                        buttonY + buttonTexture.getHeight() + offsetY - 5); // Adjusted Y position
            }
        }
    }
    // For leaf particle system
    private TextureRegion[] leafTextures;
    private static final int LEAF_COUNT = 100;
    private static final float WIND_SPEED = 30f;
    private float[] leafX, leafY, leafRotation, leafScale, leafAlpha;
    private float[] leafSpeedX, leafSpeedY, leafRotationSpeed;
    private boolean[] leafActive;
    private float windDirection = 1.0f;  // 1.0 = right, -1.0 = left
    private float windTimer = 0;
    private float windChangeInterval = 5f; // Change wind direction every 5 seconds

    private void initLeafParticles() {
        // Load leaf textures
        Texture leafAtlas = new Texture(Gdx.files.internal("textures/leaves.png"));
        leafTextures = new TextureRegion[4];
        for (int i = 0; i < 4; i++) {
            leafTextures[i] = new TextureRegion(leafAtlas, i * 32, 0, 32, 32);
        }

        // Initialize leaf arrays
        leafX = new float[LEAF_COUNT];
        leafY = new float[LEAF_COUNT];
        leafRotation = new float[LEAF_COUNT];
        leafScale = new float[LEAF_COUNT];
        leafAlpha = new float[LEAF_COUNT];
        leafSpeedX = new float[LEAF_COUNT];
        leafSpeedY = new float[LEAF_COUNT];
        leafRotationSpeed = new float[LEAF_COUNT];
        leafActive = new boolean[LEAF_COUNT];

        // Do not initialize leaves here - we'll do it in the first render call
        for (int i = 0; i < LEAF_COUNT; i++) {
            leafActive[i] = false;
        }
    }

    private void resetLeaf(int index, boolean randomizeY) {


        if (camera == null) {
            return;
        }

        float viewportWidth = camera.viewportWidth * camera.zoom;
        float viewportHeight = camera.viewportHeight * camera.zoom;

        // Position either at the top or at the side depending on wind direction
        if (windDirection > 0) {
            leafX[index] = camera.position.x - viewportWidth/2 - 50;
        } else {
            leafX[index] = camera.position.x + viewportWidth/2 + 50;
        }

        if (randomizeY) {
            // Spread leaves across entire height
            leafY[index] = camera.position.y - viewportHeight/2 + (float)Math.random() * viewportHeight * 1.5f;
        } else {
            // New leaves appear at the top
            leafY[index] = camera.position.y + viewportHeight/2 + 50;
        }

        // Randomize properties
        leafRotation[index] = (float)(Math.random() * 360);
        leafScale[index] = 0.5f + (float)Math.random() * 0.5f;
        leafAlpha[index] = 0.6f + (float)Math.random() * 0.4f;
        leafSpeedX[index] = windDirection * (WIND_SPEED + (float)Math.random() * 20);
        leafSpeedY[index] = -10f - (float)Math.random() * 20;
        leafRotationSpeed[index] = -2f + (float)(Math.random() * 4);
        leafActive[index] = true;
    }

    private boolean leavesInitialized = false;
    public void renderLeaves(SpriteBatch batch, float deltaTime) {

        // Initialize leaves on first render when camera is available
        if (!leavesInitialized && camera != null) {
            for (int i = 0; i < LEAF_COUNT; i++) {
                resetLeaf(i, true);
            }
            leavesInitialized = true;
        }

        // Skip if camera is not available or leaves not initialized
        if (camera == null || !leavesInitialized) {
            return;
        }

        // Update wind direction periodically
        windTimer += deltaTime;
        if (windTimer >= windChangeInterval) {
            windTimer = 0;
            windDirection = -windDirection;
            windChangeInterval = 3f + (float)Math.random() * 5f; // Random interval
        }

        float viewportWidth = camera.viewportWidth * camera.zoom;
        float viewportHeight = camera.viewportHeight * camera.zoom;

        Color originalColor = batch.getColor().cpy();

        for (int i = 0; i < LEAF_COUNT; i++) {
            if (!leafActive[i]) continue;

            // Update leaf position and rotation
            leafX[i] += leafSpeedX[i] * deltaTime;
            leafY[i] += leafSpeedY[i] * deltaTime;
            leafRotation[i] += leafRotationSpeed[i] * deltaTime;

            // Simulate wind influence with sine wave
            leafX[i] += Math.sin(leafY[i] * 0.01f + windTimer) * deltaTime * 15;

            // Check if leaf is off screen
            if (leafX[i] < camera.position.x - viewportWidth/2 - 100 ||
                    leafX[i] > camera.position.x + viewportWidth/2 + 100 ||
                    leafY[i] < camera.position.y - viewportHeight/2 - 100) {
                resetLeaf(i, false);
            }

            // Draw leaf
            batch.setColor(1, 1, 1, leafAlpha[i]);
            TextureRegion leafTexture = leafTextures[i % leafTextures.length];
            batch.draw(leafTexture,
                    leafX[i] - 16 * leafScale[i],
                    leafY[i] - 16 * leafScale[i],
                    16 * leafScale[i],
                    16 * leafScale[i],
                    32 * leafScale[i],
                    32 * leafScale[i],
                    1, 1,
                    leafRotation[i]);
        }

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


    public void dispose() {
        if (backgroundTexture != null) {
            backgroundTexture.dispose();
        }
        if (buttonTexture != null) {
            buttonTexture.dispose();
        }
        if (font != null) {
            font.dispose();
        }
        if (leafTextures != null && leafTextures.length > 0) {
            leafTextures[0].getTexture().dispose();
        }
    }
}