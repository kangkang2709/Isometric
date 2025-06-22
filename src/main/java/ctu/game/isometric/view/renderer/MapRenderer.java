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
import ctu.game.isometric.model.game.Items;
import ctu.game.isometric.model.puzzle.PressurePlatePuzzle;
import ctu.game.isometric.model.world.IsometricMap;
import ctu.game.isometric.model.world.MapEvent;
import ctu.game.isometric.util.AnimationManager;
import ctu.game.isometric.util.AssetManager;
import ctu.game.isometric.util.ItemLoader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private WeatherRenderer weatherRenderer;


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


        // Use the provided camera instead of creating a new one
        this.camera = camera;
//        backgroundTexture = new Texture(Gdx.files.internal("maps/background.png"));
        // Create the tiled map renderer

        this.tiledMapRenderer = new IsometricTiledMapRenderer(map.getTiledMap());

        this.fogTexture = new Texture(Gdx.files.internal("textures/fog.png"));

        this.weatherRenderer = new WeatherRenderer(camera);

        setWeather("snow", 0.4f); // Set default weather to foggy with medium intensity
    }


    public void changeTiledMapRenderer(IsometricMap map) {
        if (this.tiledMapRenderer != null) {
            this.tiledMapRenderer.dispose();
        }
        this.tiledMapRenderer = new IsometricTiledMapRenderer(map.getTiledMap());
        this.map = map;

    }

    public void changeWeather(String type, float intensity) {
        weatherRenderer.setWeather(type, intensity);
    }


    public WeatherRenderer.WeatherType getCurrentWeather() {
        return weatherRenderer.getCurrentWeather();
    }

    Texture fogTexture;


    Map<String, TextureRegion> textureRegions = new HashMap<>();

    public void loadTextures() {

        this.textures.putAll(assetManager.getTextures());


        Texture inactiveTexture = new Texture(Gdx.files.internal("textures/trap_inactive.png"));
        Texture activeTexture = new Texture(Gdx.files.internal("textures/trap_active.png"));

        textureRegions.put("trap_inactive", new TextureRegion(inactiveTexture));
        textureRegions.put("trap_active", new TextureRegion(activeTexture));

    }


    public void renderPressurePlate(SpriteBatch batch) {
        for (PressurePlatePuzzle.PressurePlate plate : this.map.getPuzzle().getPlates()) {
            TextureRegion texture = textureRegions.get(plate.getEffectType() + "_" + (plate.isActivated() ? "active" : "inactive"));
            if (texture != null) {
                float[] isoPos = toIsometric(plate.getGridX(), plate.getGridY());
                batch.draw(texture, isoPos[0], isoPos[1], 64, 32);
            }
//            if (!plate.getEffectType().equals("trap") && !plate.isActivated()) {
//                float[] isoPos = toIsometric(plate.getTargetX(), plate.getTargetY());
//                batch.draw(fogTexture, isoPos[0] - 16, isoPos[1] - 10, 64, 64);
//            }
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

            renderBoard(batch);
            weatherRenderer.render(batch);
        }
    }

    Map<String, Texture> textures = new HashMap<>();

    public void renderBoard(SpriteBatch batch) {
        if (!eventManager.getMapName().equals("board")) {
            return;
        }
        for (MapEvent event : eventManager.getEvents().values()) {

            if (event.getProperties().containsKey("id")) {
                String eventId = eventManager.getStringProperty(event.getProperties(), "id", "");
                if (event.isOneTime() && event.isCompleted()) {
                    continue; // Skip rendering this object
                }
            }

            if (event.getEventType().equals("treasure")) {
                String itemName = event.getProperties().get("itemName", String.class);
                if (textures.containsKey(itemName)) {
                    Texture texture = textures.get(itemName);
                    if (texture != null) {
                        float[] isoPos = toIsometric(event.getGridX(), event.getGridY());
                        batch.draw(texture, isoPos[0], isoPos[1], 64, 32);
                    }
                }
            }
            if (event.getEventType().equals("battle")) {
                String itemName = event.getProperties().get("enemyName", String.class);
                if (textures.containsKey(itemName)) {
                    Texture texture = textures.get(itemName);
                    if (texture != null) {
                        float[] isoPos = toIsometric(event.getGridX(), event.getGridY());
                        batch.draw(texture, isoPos[0], isoPos[1], 64, 32);
                    }
                }
            }
        }

    }

    public void update(float delta) {
        weatherRenderer.update(delta);
    }

    public void setWeather(String type, float intensity) {
        weatherRenderer.setWeather(type, intensity);
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
                                isoPos[0] - width / 2 + 4,      // Center horizontally
                                isoPos[1] - height / 4 + 8,     // Improved alignment for isometric view
                                width, height);
                    }
                }
            }
        }
    }

    // Convert world coordinates to grid coordinates
    private int[] toGrid(float worldX, float worldY) {
        float tileWidth = map.getTileWidth();   // ví dụ: 64
        float tileHeight = map.getTileHeight(); // ví dụ: 32


        float gridX = worldX / tileHeight - 1;
        float gridY = worldY / tileHeight + 1;

        return new int[]{Math.round(gridX), Math.round(gridY)};
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