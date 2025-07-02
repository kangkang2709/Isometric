package ctu.game.isometric.view.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
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
import com.badlogic.gdx.math.Vector3;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import ctu.game.isometric.controller.EventManager;
import ctu.game.isometric.model.entity.Character;
import ctu.game.isometric.model.game.Dice;
import ctu.game.isometric.model.game.Items;
import ctu.game.isometric.model.puzzle.PressurePlatePuzzle;
import ctu.game.isometric.model.world.IsometricMap;
import ctu.game.isometric.model.world.MapEvent;
import ctu.game.isometric.util.AnimationManager;
import ctu.game.isometric.util.AssetManager;
import ctu.game.isometric.util.ItemLoader;

import java.util.*;

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

        animationManager.loadDiceAnimations("textures/dice_static.png", "textures/dice_rolling.png");

        animationManager.loadDungeonEnemyAnimations("enemies/dungeons/up.png", "up");
        animationManager.loadDungeonEnemyAnimations("enemies/dungeons/down.png", "down");
        animationManager.loadDungeonEnemyAnimations("enemies/dungeons/left.png", "left");
        animationManager.loadDungeonEnemyAnimations("enemies/dungeons/right.png", "right");


        setSlowMotion(true);
        setSpeedMultiplier(0.01f);
        smoothSlowMotion(true);
        setWeather("snow", 0.4f); // Set default weather to foggy with medium intensity


    }


    private Dice diceRenderer;

    public Dice getDiceRenderer() {
        return diceRenderer;
    }

    public void setDice(Dice diceRenderer) {
        this.diceRenderer = diceRenderer;
    }

    /**
     * Rolls a 20-sided die and returns the face value.
     *
     * @return The face value of the rolled die (1-20).
     */
    boolean isRolling = false;
    int currentFaceValue = 1; // Store the current face value
    int prviousFaceValue = 1; // Store the previous face value

    public int rollingDice() {
        prviousFaceValue = diceRenderer.getCurrentFaceValue();
        currentFaceValue = diceRenderer.rollDice();
        return currentFaceValue;
    }


    public void renderDice(SpriteBatch batch) {
        diceRenderer.render(batch);
    }

    public boolean handleRollingClick(int screenX, int screenY) {
        Vector3 worldCoords = new Vector3(screenX, screenY, 0);
        camera.unproject(worldCoords);

        if (diceRenderer.handleClick(worldCoords.x, worldCoords.y)) {
            rollingDice();
            return true;
        }
        return false;
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


    PerspectiveCamera camera_3d = new PerspectiveCamera();

    public void render(SpriteBatch batch) {
        // Draw background for the entire screen
//        float bgX = camera.position.x - (Gdx.graphics.getWidth() / 2f);
//        float bgY = camera.position.y - (Gdx.graphics.getHeight() / 2f);
//        batch.draw(backgroundTexture, bgX, bgY, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // Update camera position based on character position
        float[] isoPos = toIsometric(character.getGridX(), character.getGridY());

        camera.position.set(600, 0, 0);
        camera.update();
        if (!map.getMapName().equals("board")) {
            camera.position.set(isoPos[0], isoPos[1], 0);
            camera.update();
        }


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

            if (map.getMapName().equals("board")) {
                renderBoard(batch);
                renderDice(batch);
            }


            weatherRenderer.render(batch);
        }
    }

    Map<String, Texture> textures = new HashMap<>();

    float boardOffsetY = 21f; // Offset for board Y position

    public void renderBoard(SpriteBatch batch) {
        if (!eventManager.getMapName().equals("board")) return;

        for (MapEvent event : eventManager.getEvents().values()) {
            float[] isoPos = toIsometric(event.getGridX(), event.getGridY());
            isoPos[1] = isoPos[1] + boardOffsetY; // Adjust Y position for board offset

            // Draw highlight ground tile

            // Skip completed one-time events
            if (event.isOneTime() && event.isCompleted()) continue;

            String type = event.getEventType();

            switch (type) {
                case "treasure":
                    drawTexture(batch, "item_hightlight", isoPos[0], isoPos[1], 64, 34);
                    drawItemTexture(batch, event.getProperties().get("itemName", String.class), isoPos[0] + 16, isoPos[1] + 8, 32, 32);
                    break;
                case "word_scramble":
                    drawTexture(batch, "item_hightlight", isoPos[0], isoPos[1], 64, 34);
                    break;
                case "new_run_event":
                    drawTexture(batch, "new_run", isoPos[0], isoPos[1], 64, 34);
                    break;
                case "quiz":
                    drawTexture(batch, "quiz_hightlight", isoPos[0], isoPos[1], 64, 34);
                    break;
                case "mulquiz":
                    drawTexture(batch, "quiz_hightlight", isoPos[0], isoPos[1], 64, 34);
                    break;
                case "battle":
                    drawTexture(batch, "enemy_hightlight", isoPos[0], isoPos[1], 64, 34);
                    drawEnemySpinCard(batch, isoPos[0] + 14, isoPos[1] + 12, 30, 40);
                    break;
            }
        }
    }

    private float cardRotation = 0f;
    private static final float SPIN_SPEED = 90f; // degrees per second
    private float speedMultiplier = 1.0f; // Default normal speed
    private static final float SLOW_MOTION_FACTOR = 0.2f; // 30% of normal speed

    private void drawEnemySpinCard(SpriteBatch batch, float x, float y, float width, float height) {
        Texture cardTexture = null;
        Texture cardBackTexture = null;

        // Get the front texture
        if (textures.containsKey("enemy_card")) {
            cardTexture = textures.get("enemy_card");
        }

        // Get the back texture (optional)
        if (textures.containsKey("enemy_card_back")) {
            cardBackTexture = textures.get("enemy_card_back");
        } else {
            cardBackTexture = cardTexture; // Use same texture if back not available
        }

        if (cardTexture != null) {
            // Update rotation with speed multiplier for slow motion effect
            cardRotation = (cardRotation + SPIN_SPEED * speedMultiplier * Gdx.graphics.getDeltaTime()) % 360;

            // Calculate center points
            float centerX = x + width / 2;

            // Calculate scale factor based on rotation to simulate X-axis rotation
            float scaleX = (float) Math.abs(Math.cos(Math.toRadians(cardRotation)));

            // Choose texture based on rotation angle (front or back)
            Texture currentTexture = (cardRotation > 90 && cardRotation < 270) ?
                    cardBackTexture : cardTexture;

            // Draw with rotation effect (removed the floating effect)
            batch.draw(
                    currentTexture,
                    centerX - (width * scaleX) / 2, // X position adjusted for scale
                    y, // Fixed Y position without floating effect
                    width * scaleX, // Width scaled to simulate perspective
                    height, // Height
                    0, // Source X
                    0, // Source Y
                    currentTexture.getWidth(), // Source width
                    currentTexture.getHeight(), // Source height
                    (cardRotation > 90 && cardRotation < 270), // Flip horizontally when showing back
                    false // Don't flip vertically
            );

        }
    }

    // Methods to control animation speed
    public void setSlowMotion(boolean enabled) {
        this.speedMultiplier = enabled ? SLOW_MOTION_FACTOR : 1.0f;
    }

    public void setSpeedMultiplier(float multiplier) {
        this.speedMultiplier = Math.max(0.01f, multiplier); // Prevent extremely slow or negative speeds
    }

    // Optional: Smooth transition to slow motion
    private float targetSpeedMultiplier = 0.5f;
    private static final float SPEED_TRANSITION_RATE = 1.0f; // Units per second

    public void smoothSlowMotion(boolean enabled) {
        this.targetSpeedMultiplier = enabled ? SLOW_MOTION_FACTOR : 1.0f;
    }

    public void updateCardAnimation(float deltaTime) {
        // Gradually change speed toward target
        if (speedMultiplier != targetSpeedMultiplier) {
            if (speedMultiplier < targetSpeedMultiplier) {
                speedMultiplier = Math.min(speedMultiplier + SPEED_TRANSITION_RATE * deltaTime, targetSpeedMultiplier);
            } else {
                speedMultiplier = Math.max(speedMultiplier - SPEED_TRANSITION_RATE * deltaTime, targetSpeedMultiplier);
            }
        }
    }


    private void drawTexture(SpriteBatch batch, String key, float x, float y, float width, float height) {
        Texture texture = textures.get(key);
        if (texture != null) {
            batch.draw(texture, x, y, width, height);
        }
    }

    private void drawItemTexture(SpriteBatch batch, String itemKey, float x, float y, float width, float height) {
        if (itemKey != null && textures.containsKey(itemKey)) {
            Texture texture = textures.get(itemKey);
            if (texture != null) {
                batch.draw(texture, x, y, width, height);
            }
        }
    }


    private float diceRollingTime = 0f;
    private static final float DICE_ROLL_DURATION = 1.6f;
    private static final float DICE_OFFSET_X = 640f; // Dice position X
    private static final float DICE_OFFSET_Y = -160f; // Dice position Y

    private void updateDiceRolling(float delta) {
        if (isRolling) {
            diceRollingTime += delta;
            if (diceRollingTime >= DICE_ROLL_DURATION) {
                isRolling = false;
                diceRollingTime = 0f;
            }
        }
    }

    public void update(float delta) {
        weatherRenderer.update(delta);
        diceRenderer.update(delta);
        updateCardAnimation(delta);

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
                buttonText = "Battle";
                drawEnemyInfoCard(batch, event);
                break;
            case "dialog":
                buttonText = "Talk";
                break;
            case "cutscene":
                buttonText = "Cutscene";
                break;
            case "quiz":
                buttonText = "Mini-game";
                break;
            case "mulquiz":
                buttonText = "Mini-game";
                break;
            case "teleport":
                buttonText = "Teleport";
                break;
            case "treasure":
                buttonText = "Pick up";
                break;
        }

        if (buttonTexture != null && !eventType.equals("battle")) {
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


    int centerX = 660;
    int centerY = 0;

    private void drawEnemyInfoCard(SpriteBatch batch, MapEvent event) {
        // Get enemy information from event properties
        if (!map.getMapName().equals("board")) return;

        String enemyName = event.getProperties().get("enemyName", String.class);
        if (enemyName == null) enemyName = "Unknown Enemy";

        // Get difficulty level if available
        String difficulty = event.getProperties().get("difficulty", String.class);
        if (difficulty == null) difficulty = "Normal";

        // Get card texture
        Texture cardTexture = textures.get("enemy_card_large");
        if (cardTexture == null) {
            // Fallback to regular enemy card
            cardTexture = textures.get("enemy_card");
        }

        if (cardTexture != null) {
            // Calculate card position at camera center
            float cardWidth = 200;
            float cardHeight = 300;

            ;

            // Draw card centered on camera
            batch.draw(
                    cardTexture,
                    centerX - cardWidth / 2,
                    centerY - cardHeight / 2,
                    cardWidth,
                    cardHeight
            );

            // Draw enemy information
            if (font != null) {
                // Store original font properties
                Color originalColor = font.getColor();
                float originalScale = font.getData().scaleX;

                // Set larger scale for the card text
                font.getData().setScale(1.5f);
                font.setColor(Color.WHITE);

                // Draw enemy name
                GlyphLayout nameLayout = new GlyphLayout(font, enemyName);
                font.draw(batch, enemyName,
                        centerX - nameLayout.width / 2,
                        centerY + cardHeight / 4);

                // Draw difficulty
                font.getData().setScale(1.2f);
                GlyphLayout diffLayout = new GlyphLayout(font, "Difficulty: " + difficulty);
                font.draw(batch, "Difficulty: " + difficulty,
                        centerX - diffLayout.width / 2,
                        centerY - cardHeight / 8);

                // Restore original font properties
                font.setColor(originalColor);
                font.getData().setScale(originalScale);
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

    public AssetManager getAssetManager() {
        return assetManager;
    }

    public void setAssetManager(AssetManager assetManager) {
        this.assetManager = assetManager;
    }


}