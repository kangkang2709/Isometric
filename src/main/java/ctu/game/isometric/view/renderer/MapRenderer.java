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
    public MapRenderer(IsometricMap map, AssetManager assetManager,EventManager eventManager, Character character, OrthographicCamera camera) {
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


        // Use the provided camera instead of creating a new one
        this.camera = camera;
//        backgroundTexture = new Texture(Gdx.files.internal("maps/background.png"));
        // Create the tiled map renderer
        this.tiledMapRenderer = new IsometricTiledMapRenderer(map.getTiledMap());
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
                    String eventId = eventManager.getStringProperty(object.getProperties(),"id", "");
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
        int mapOffsetY = -1 ; // Adjust as needed for your map

        return new int[]{x + mapOffsetX, y + mapOffsetY};
    }
    //

    public void renderActionButton(SpriteBatch batch, String eventType, MapEvent event, float x, float y) {
        if (eventType == null || event == null) return;


        if(event.isOneTime() && event.isCompleted()) {
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

    /**
     * Renders highlights for walkable tiles around the player's position.
     * @param batch The sprite batch to render with
     * @param viewDistance How many tiles away from the player to check
     * @param stateTime Current animation state time
     */
    /**
     * Renders highlights for walkable tiles in the four cardinal directions from the player.
     *
     * @param batch     The sprite batch to render with
     * @param stateTime Current animation state time
     */
    public void renderWalkableTileHighlights(SpriteBatch batch, float stateTime) {
        // Get character position
        int characterX = (int) Math.floor(character.getGridX());
        int characterY = (int) Math.floor(character.getGridY());

        // Store original color
        Color originalColor = new Color(batch.getColor());

        // Set highlight color (semi-transparent green)
        batch.setColor(0.2f, 1f, 0.2f, 0.5f);

        // Create a list to hold valid adjacent tiles
        java.util.List<int[]> validTiles = new java.util.ArrayList<>();

        // Define the four cardinal directions matching the movement controls
        int[][] directions = {
                {1, 0},   // Up
                {-1, 0},  // Down
                {0, -1},  // Left
                {0, 1},   // Right
                {1, -1},  // Up-Left
                {1, 1},   // Up-Right
                {-1, -1}, // Down-Left
                {-1, 1}   // Down-Right
        };

        // Check only the four cardinal directions
        for (int[] dir : directions) {
            int x = characterX + dir[0];
            int y = characterY + dir[1];

            // Skip if out of bounds
            if (x < 0 || x >= map.getMapWidth() || y < 0 || y >= map.getMapHeight()) {
                continue;
            }

            // Check if walkable using map's chunk system
            if (map.isWalkable(x, y)) {
                validTiles.add(new int[]{x, y});
            }
        }

        // Sort by isometric depth (higher x+y is farther from camera in isometric view)
        validTiles.sort((a, b) -> {
            int sumComparison = Integer.compare(b[0] + b[1], a[0] + a[1]);
            return sumComparison != 0 ? sumComparison : Integer.compare(b[1], a[1]);
        });

        // Render each walkable tile
        for (int[] tile : validTiles) {
            int x = tile[0];
            int y = tile[1];

            TiledMapTileLayer.Cell cell = map.getBaseLayer().getCell(x, y);
            if (cell != null && cell.getTile() != null) {
                TextureRegion tileRegion = cell.getTile().getTextureRegion();
                float[] iso = toIsometric(x, y);

                batch.draw(tileRegion,
                        iso[0],
                        iso[1],
                        map.getTileWidth(),
                        map.getTileHeight());
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
    }
}