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
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.renderers.IsometricTiledMapRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Timer;
import ctu.game.isometric.controller.DialogController;
import ctu.game.isometric.controller.EventManager;
import ctu.game.isometric.model.entity.Character;
import ctu.game.isometric.model.entity.Enemy;
import ctu.game.isometric.model.game.Dice;
import ctu.game.isometric.model.puzzle.PressurePlatePuzzle;
import ctu.game.isometric.model.world.IsometricMap;
import ctu.game.isometric.model.world.MapEvent;
import ctu.game.isometric.util.AnimationManager;
import ctu.game.isometric.util.AssetManager;
import ctu.game.isometric.util.EnemyLoader;
import ctu.game.isometric.util.ItemLoader;

import java.util.*;

import static ctu.game.isometric.IsometricGame.getGameController;

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

    private DialogController dialogController;


    BitmapFont cardFont;
    BitmapFont titleFont;

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

        this.cardFont = getGameController().getCommonFont();
        this.titleFont = getGameController().getFont();
        this.titleFont.setColor(Color.BLACK);

        // Use the provided camera instead of creating a new one
        this.camera = camera;
//        backgroundTexture = new Texture(Gdx.files.internal("maps/background.png"));
        // Create the tiled map renderer

        this.tiledMapRenderer = new IsometricTiledMapRenderer(map.getTiledMap());

        this.fogTexture = new Texture(Gdx.files.internal("textures/door1.png"));

        this.weatherRenderer = new WeatherRenderer(camera);


        setSlowMotion(true);
        setSpeedMultiplier(0.01f);
        smoothSlowMotion(true);
        setWeather("snow", 0.4f); // Set default weather to foggy with medium intensity

        updateRollTargetValue();
    }

    public boolean isRenderDarknessWithLight() {
        return map.getMapName().equals("board") && !isRenderInfoCard;
    }

    public void render(SpriteBatch batch) {


        if (!isCameraTransitioning) {
            if (isZoomed && map.getMapName().equals("board")) {
                camera.position.set(645, 25, 0);
                camera.zoom = 0.8f;
                camera.update();
            } else if (!isZoomed && map.getMapName().equals("board") || map.getMapName().equals("forest") || map.getMapName().equals("main")) {
                float[] isoPos = toIsometric(character.getGridX(), character.getGridY());
                camera.position.set(isoPos[0], isoPos[1], 0);
                camera.zoom = 0.5f;
                camera.update();
            } else {
                float[] isoPos = toIsometric(character.getGridX(), character.getGridY());
                camera.position.set(isoPos[0], isoPos[1], 0);
                camera.update();
            }
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
            } else {
                weatherRenderer.render(batch);
            }

        }

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
    int previousFaceValue = 1; // Store the previous face value

    public int rollingDice() {
        previousFaceValue = diceRenderer.getCurrentFaceValue();
        currentFaceValue = diceRenderer.rollDice();
        return currentFaceValue;
    }


    boolean isAcceptingRoll = false;

    public void setShouldRenderCharacter(boolean shouldRenderCharacter) {
        this.isAcceptingRoll = shouldRenderCharacter;
        getGameController().setRenderCharacter(true);
    }

    public void rollingDice(int targetValue) {
        previousFaceValue = diceRenderer.getCurrentFaceValue();

        diceRenderer.setCompletionListener((success -> {
            currentFaceValue = diceRenderer.getCurrentFaceValue();

            if (success) {
                dialogController.showMessageWithChoices(
                        "Bạn có muốn bỏ qua trận chiến này", "Đồng Ý [YES]", " Từ chối [NO]",
                        () -> {
                            setShouldRenderCharacter(false);
                            getGameController().setEndEvent();
                        }, () -> {
                            setShouldRenderCharacter(false);
                        }
                );
            } else {
                Timer.schedule(new Timer.Task() {
                    @Override
                    public void run() {
                        if (diceRenderer.getBonusCount() > 0) {
                            dialogController.showMessageWithChoices(
                                    "Bạn tung xúc xắc được " + currentFaceValue + ", không thành công bỏ qua trận chiến.\n Có có muốn thử lại ?", "Đồng Ý [YES]", " Từ chối [NO]",
                                    () -> {
                                        isAcceptingRoll = true;
                                        diceRenderer.activeBonusRoll();
                                    },
                                    () -> {
                                        setShouldRenderCharacter(false);
                                    }
                            );
                        } else {
                            dialogController.showSimpleMessage("Bạn tung xúc xắc được  " + currentFaceValue + ", không thể bỏ qua trận chiến.\n Hãy chiến đấu như 1 anh hùng nào!!!");
                            setShouldRenderCharacter(false);
                        }
                    }
                }, 1.0f);

            }
        }));

        diceRenderer.rollDice(targetValue);
    }

    public void rollingDiceTrap(String trapId) {
        previousFaceValue = diceRenderer.getCurrentFaceValue();

        diceRenderer.setCompletionListener((success -> {
            currentFaceValue = diceRenderer.getCurrentFaceValue();
            if (success) {
                dialogController.showMessageWithChoices(
                        "Bạn muốn mở khóa cái bẫy này ?", "Đồng Ý [YES]", " Từ chối [NO]",
                        () -> {
                            Timer.schedule(new Timer.Task() {
                                @Override
                                public void run() {
                                    setAcceptingRoll(false);
                                    dialogController.showSimpleMessage("Bạn đã phá dỡ được bẫy!!! Giờ bạn có thể mở khoá chiếc rương này!!!\n" +
                                            "Nếu bạn rời khỏi đây, bẫy sẽ được thiết lập lại!!!");
                                    getGameController().unlockTrap(trapId);
                                }
                            }, 1.0f);

                        }, () -> {
                            Timer.schedule(new Timer.Task() {
                                @Override
                                public void run() {
                                    dialogController.showSimpleMessage("Sửa lại cái bẫy!!! Người đến sau sẽ bị mắc lừa!!! HA HA HA");
                                    setAcceptingRoll(false);
                                }
                            }, 1.0f);
                        }
                );
            } else {
                Timer.schedule(new Timer.Task() {
                    @Override
                    public void run() {
                        if (diceRenderer.getBonusCount() > 0) {
                            dialogController.showMessageWithChoices(
                                    "Bạn tung xúc xắc được" + currentFaceValue + ", không đủ để mở khoá cái bẫy này.\n Có có muốn thử lại ?", "Đồng Ý [YES]", " Từ chối [NO]",
                                    () -> {
                                        isAcceptingRoll = true;
                                        diceRenderer.activeBonusRoll();
                                    },
                                    () -> {
                                        dialogController.showSimpleMessage("Từ bỏ việc mở khoá cái bẫy này!!!");
                                        setAcceptingRoll(false);
                                    }
                            );
                        } else {
                            dialogController.showSimpleMessage("Bạn tung xúc xắc được " + currentFaceValue + ", không đủ để mở khoá chiếc rương này.\n Bạn mất " + targetValue + " máu!!!");
                            character.setHealth(Math.max(1, character.getHealth() - targetValue));
                            setAcceptingRoll(false);
                        }
                    }
                }, 1.0f);

            }
        }));

        diceRenderer.rollDice(targetValue);
    }


    public void setAcceptingRoll(boolean acceptingRoll) {
        isAcceptingRoll = acceptingRoll;
    }

    public void renderDice(SpriteBatch batch) {

        if (isAcceptingRoll) {
            batch.draw(cardBackgroundTexture,
                    centerX + 200,
                    centerY - 250,
                    350, 500

            );

            titleFont.draw(batch, "Target: " + targetValue, centerX + 330,
                    centerY - 5);

            cardFont.setColor(Color.BLACK);
            cardFont.draw(batch, "Bonus Roll Left: " + diceRenderer.getBonusCount(),
                    centerX + 300,
                    centerY - 90);

            cardFont.setColor(Color.WHITE);

            diceRenderer.render(batch);

        }
    }

    public void renderDice(SpriteBatch batch, float isoX, float isoY) {

        if (isAcceptingRoll && !character.isMoving()) {
            batch.draw(cardBackgroundTexture,
                    isoX + 200,
                    isoY - 250,
                    350, 500

            );
            cardFont.setColor(Color.BLACK);
            titleFont.draw(batch, "Target: " + targetValue, isoX + 330,
                    isoY - 5);
            cardFont.draw(batch, "Bonus Roll Left: " + diceRenderer.getBonusCount(),
                    isoX + 300,
                    isoY - 90);
            cardFont.setColor(Color.RED);
            cardFont.draw(batch, "PRESS ENTER TO ROLL",
                    isoX + 280,
                    isoY - 130);
            cardFont.setColor(Color.WHITE);

            diceRenderer.render(batch, isoX, isoY);

        }
    }

    public int getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(int targetValue) {
        this.targetValue = targetValue;
    }

    public boolean isAcceptingRoll() {
        return isAcceptingRoll;
    }

    public boolean handleRollingClick(int screenX, int screenY) {
        Vector3 worldCoords = new Vector3(screenX, screenY, 0);
        camera.unproject(worldCoords);

        if (diceRenderer.handleClick(worldCoords.x, worldCoords.y)) {
            rollingDice(targetValue);
            return true;
        }
        return false;
    }


    public void changeTiledMapRenderer(IsometricMap map, EventManager eventManager) {
        if (this.tiledMapRenderer != null) {
            this.tiledMapRenderer.dispose();
        }
        this.tiledMapRenderer = new IsometricTiledMapRenderer(map.getTiledMap());
        this.map = map;
        this.eventManager = eventManager;
        updateRollTargetValue();
    }

    public void changeWeather(String type, float intensity) {
        weatherRenderer.setWeather(type, intensity);
    }


    public WeatherRenderer.WeatherType getCurrentWeather() {
        return weatherRenderer.getCurrentWeather();
    }

    Texture fogTexture;
    Texture cardBlockTexture;
    Texture cardBlockTextureBack;
    Texture cardBackTexture;
    Map<String, TextureRegion> textureRegions = new HashMap<>();

    public void loadTextures() {
        this.textures.clear();
        this.textures.putAll(assetManager.getTextures());

        this.cardTexture = textures.get("enemy_card_large");
        this.cardBackTexture = textures.get("enemy_card_back");

        this.cardBlockTexture = textures.get("block_card");
        this.cardBlockTextureBack = textures.get("block_card_back");

        this.cardBackgroundTexture = textures.get("card-frame");
        Texture inactiveTexture = new Texture(Gdx.files.internal("textures/trap_inactive.png"));
        Texture activeTexture = new Texture(Gdx.files.internal("textures/trap_active.png"));

        textureRegions.put("trap_inactive", new TextureRegion(inactiveTexture));
        textureRegions.put("trap_active", new TextureRegion(activeTexture));
        textureRegions.put("door_inactive", new TextureRegion(new Texture(Gdx.files.internal("textures/pyramid3.png"))));
        textureRegions.put("door_active", new TextureRegion(new Texture(Gdx.files.internal("textures/pyramid3.png"))));

    }


    public void renderPressurePlate(SpriteBatch batch) {
        for (PressurePlatePuzzle.PressurePlate plate : this.map.getPuzzle().getPlates()) {
            TextureRegion texture = textureRegions.get(plate.getEffectType() + "_" + (plate.isActivated() ? "active" : "inactive"));
            float[] isoPos = toIsometric(plate.getGridX(), plate.getGridY());

            if (texture != null) {
                switch (plate.getEffectType()) {
                    case "trap":
                        batch.draw(texture, isoPos[0] + 10, isoPos[1] + 16, 40, 32);
                        break;
                    case "door":
                        batch.draw(texture, isoPos[0] + 16, isoPos[1] + 8, 32, 32);
                        break;
                }
            }
            if (plate.getEffectType().equals("door") && !plate.isActivated()) {
                float[] isoPos2 = toIsometric(plate.getTargetX(), plate.getTargetY());
                batch.draw(fogTexture, isoPos2[0] + 10, isoPos2[1] + 16, 48, 89);
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


    Map<String, Texture> textures = new HashMap<>();

    float boardOffsetY = 21f; // Offset for board Y position

    public void renderBoard(SpriteBatch batch) {
        if (!eventManager.getMapName().equals("board")) return;

        for (MapEvent event : eventManager.getEvents().values()) {
            float[] isoPos = toIsometric(event.getGridX(), event.getGridY());
            isoPos[1] = isoPos[1] + boardOffsetY; // Adjust Y position for board offset
            if (event.isOneTime() && event.isCompleted()) continue;

            String type = event.getEventType();
            switch (type) {
                case "treasure":
                    drawItemTexture(batch, event.getProperties().get("itemName", String.class), isoPos[0] + 16, isoPos[1] + 8, 32, 32);
                    break;
                case "new_run_event":
                case "dungeon":
                    drawTexture(batch, "new_run", isoPos[0], isoPos[1], 32, 64);
                    break;
                case "battle":
                    drawEnemySpinCard(batch, isoPos[0] + 14, isoPos[1] + 12, 20, 30);
                    break;
                case "word_scramble":
                case "mulquiz":
                case "quiz":
                    drawMiniGameSpinCard(batch, isoPos[0] + 14, isoPos[1] + 12, 20, 30);
                    break;
            }
        }
    }


    public void renderHighlight(SpriteBatch batch) {
        if (!eventManager.getMapName().equals("board")) return;
        // Render highlights for events on the board
        for (MapEvent event : eventManager.getEvents().values()) {
            float[] isoPos = toIsometric(event.getGridX(), event.getGridY());
            isoPos[1] = isoPos[1] + boardOffsetY; // Adjust Y position for board offset
            if (event.isOneTime() && event.isCompleted()) continue;

            String type = event.getEventType();

            switch (type) {
                case "treasure":
                    drawTexture(batch, "item_hightlight", isoPos[0], isoPos[1], 64, 34);
                    break;
                case "word_scramble":
                    drawTexture(batch, "item_hightlight", isoPos[0], isoPos[1], 64, 34);
                    break;

                case "quiz":
                    drawTexture(batch, "quiz_hightlight", isoPos[0], isoPos[1], 64, 34);
                    break;
                case "mulquiz":
                    drawTexture(batch, "quiz_hightlight", isoPos[0], isoPos[1], 64, 34);
                    break;
                case "battle":
                    drawTexture(batch, "enemy_hightlight", isoPos[0], isoPos[1], 64, 34);
                    break;
            }
        }
    }

    private float cardRotation = 0f;
    private static final float SPIN_SPEED = 50f; // degrees per second
    private float speedMultiplier = 1.0f; // Default normal speed
    private static final float SLOW_MOTION_FACTOR = 0.1f; // 30% of normal speed

    private void drawEnemySpinCard(SpriteBatch batch, float x, float y, float width, float height) {

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

    private void drawMiniGameSpinCard(SpriteBatch batch, float x, float y, float width, float height) {

        // Get the front texture
        cardBlockTexture = textures.get("block_card");


        // Get the back texture (optional)
        if (textures.containsKey("block_card_back")) {
            cardBlockTextureBack = textures.get("block_card_back");
        } else {
            cardBlockTextureBack = cardBlockTexture; // Use same texture if back not available
        }

        if (cardBlockTexture != null) {
            // Update rotation with speed multiplier for slow motion effect
            cardRotation = (cardRotation + SPIN_SPEED * speedMultiplier * Gdx.graphics.getDeltaTime()) % 360;

            // Calculate center points
            float centerX = x + width / 2;

            // Calculate scale factor based on rotation to simulate X-axis rotation
            float scaleX = (float) Math.abs(Math.cos(Math.toRadians(cardRotation)));

            // Choose texture based on rotation angle (front or back)
            Texture currentTexture = (cardRotation > 90 && cardRotation < 270) ?
                    cardBlockTexture : cardBlockTextureBack;

            // Draw with rotation effect (removed the floating effect)
            batch.draw(
                    currentTexture,
                    centerX - (width * scaleX) / 2, // X position adjusted for scale
                    y, // Fixed Y position without floating effect
                    width * scaleX, // Width scaled to simulate perspective
                    width, // Height
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

    private Vector3 originalCameraPosition;
    private float originalCameraZoom;
    private Vector3 targetCameraPosition;
    private float targetCameraZoom;
    private float cameraTransitionDuration = 2.0f; // Duration to move to target
    private float cameraHoldDuration = 3.0f; // Duration to hold at target
    private float cameraReturnDuration = 2.0f; // Duration to return to original
    private float cameraTransitionTimer = 0f;
    private boolean isCameraTransitioning = false;
    private CameraState cameraState = CameraState.NORMAL;

    private enum CameraState {
        NORMAL,
        MOVING_TO_TARGET,
        HOLDING_AT_TARGET,
        RETURNING_TO_ORIGINAL
    }

    public void moveCameraToTarget(float targetX, float targetY, float targetZoom,
                                   float moveDuration, float holdDuration, float returnDuration) {
        // Store original camera state
        originalCameraPosition = new Vector3(camera.position);
        originalCameraZoom = camera.zoom;

        // Set target state
        targetCameraPosition = new Vector3(targetX, targetY, 0);
        targetCameraZoom = targetZoom;

        // Set durations
        this.cameraTransitionDuration = moveDuration;
        this.cameraHoldDuration = holdDuration;
        this.cameraReturnDuration = returnDuration;

        // Start transition
        cameraState = CameraState.MOVING_TO_TARGET;
        cameraTransitionTimer = 0f;
        isCameraTransitioning = true;
    }

    public void printCurrentPosition() {
        System.out.println("Camera Position: " + camera.position);
        System.out.println("Camera Zoom: " + camera.zoom);
    }

    public void updateCameraTransition(float deltaTime) {
        if (!isCameraTransitioning) return;

        cameraTransitionTimer += deltaTime;

        switch (cameraState) {
            case MOVING_TO_TARGET:
                if (cameraTransitionTimer >= cameraTransitionDuration) {
                    // Transition complete, start holding
                    camera.position.set(targetCameraPosition);
                    camera.zoom = targetCameraZoom;
                    cameraState = CameraState.HOLDING_AT_TARGET;
                    cameraTransitionTimer = 0f;
                } else {
                    // Interpolate to target
                    float progress = cameraTransitionTimer / cameraTransitionDuration;
                    progress = easeInOutQuad(progress); // Smooth easing

                    camera.position.x = lerp(originalCameraPosition.x, targetCameraPosition.x, progress);
                    camera.position.y = lerp(originalCameraPosition.y, targetCameraPosition.y, progress);
                    camera.zoom = lerp(originalCameraZoom, targetCameraZoom, progress);
                }
                break;

            case HOLDING_AT_TARGET:
                if (cameraTransitionTimer >= cameraHoldDuration) {
                    // Hold complete, start returning
                    cameraState = CameraState.RETURNING_TO_ORIGINAL;
                    cameraTransitionTimer = 0f;
                }
                // Camera stays at target position
                break;

            case RETURNING_TO_ORIGINAL:
                if (cameraTransitionTimer >= cameraReturnDuration) {
                    // Return complete
                    camera.position.set(originalCameraPosition);
                    camera.zoom = originalCameraZoom;
                    cameraState = CameraState.NORMAL;
                    isCameraTransitioning = false;
                    cameraTransitionTimer = 0f;
                } else {
                    // Interpolate back to original
                    float progress = cameraTransitionTimer / cameraReturnDuration;
                    progress = easeInOutQuad(progress); // Smooth easing

                    camera.position.x = lerp(targetCameraPosition.x, originalCameraPosition.x, progress);
                    camera.position.y = lerp(targetCameraPosition.y, originalCameraPosition.y, progress);
                    camera.zoom = lerp(targetCameraZoom, originalCameraZoom, progress);
                }
                break;
        }

        camera.update();
    }

    private float lerp(float start, float end, float progress) {
        return start + (end - start) * progress;
    }

    /**
     * Smooth easing function for natural camera movement
     */
    private float easeInOutQuad(float t) {
        return t < 0.5f ? 2 * t * t : -1 + (4 - 2 * t) * t;
    }

    public void cancelCameraTransition() {
        if (isCameraTransitioning) {
            camera.position.set(originalCameraPosition);
            camera.zoom = originalCameraZoom;
            camera.update();
            isCameraTransitioning = false;
            cameraState = CameraState.NORMAL;
        }
    }

    public boolean isCameraTransitioning() {
        return isCameraTransitioning;
    }

    public void update(float delta) {
        weatherRenderer.update(delta);
        diceRenderer.update(delta);
        updateCardAnimation(delta);
        updateCameraTransition(delta); // Add this line
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

    boolean isRenderInfoCard = false;

    public void setRenderInfoCard(boolean renderInfoCard) {
        isRenderInfoCard = renderInfoCard;
    }

    public boolean isRenderInfoCard() {
        return isRenderInfoCard;
    }

    public void toggleRenderInfoCard() {
        isRenderInfoCard = !isRenderInfoCard;

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
                drawEnemyInfoCard(batch, event);
                if ("board".equals(map.getMapName())) {
                    return;
                } else {
                    buttonText = "Combat";
                }
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
            case "tele":
                buttonText = "Go to";
                break;
            case "return":
                buttonText = "Leave";
                break;
            case "trap":
                drawTrapInfoCard(batch, event);
                buttonText = "unlock";
                break;
            case "treasure":
                buttonText = "Pick up";
                break;
            case "message":
                buttonText = "Read";
                break;
            case "new_run_event":
                buttonText = "Next Floor";
                break;
            case "dungeon":
                buttonText = "Floor";
                break;
            default:
                buttonText = "???";
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


    int centerX = 460;
    int centerY = 0;

    Texture cardTexture;
    Texture cardBackgroundTexture;

    int targetValue = 10;

    public boolean isZoomed() {
        return isZoomed;
    }

    public void setZoomed(boolean zoomed) {
        isZoomed = zoomed;
    }

    public void updateRollTargetValue() {
        this.targetValue = character.getLevel() * 4 + character.getRun();
    }

    Map<String, Enemy> enemies = new HashMap<>();


    boolean isZoomed = false;

    private void drawEnemyInfoCard(SpriteBatch batch, MapEvent event) {
        if (!map.getMapName().equals("board") || !isRenderInfoCard) return;


        if (!isAcceptingRoll) {
            centerX = 640;
        } else centerX = 460;

        isZoomed = true;
        String enemyName = event.getProperties().get("enemyName", String.class);
        if (enemyName == null) enemyName = "Unknown Enemy";

        Enemy enemy = enemies.get(enemyName);
        if (enemy == null) {
            enemy = EnemyLoader.getEnemyByName(enemyName);
            enemies.put(enemyName, enemy);
        }

        if (cardTexture == null) {
            cardTexture = textures.get("enemy_card");
        }


        if (cardTexture != null) {
            float cardWidth = 350;
            float cardHeight = 500;
            float cardX = centerX - cardWidth / 2;
            float cardY = centerY - cardHeight / 2;

            // Draw the card background
            batch.draw(cardTexture, cardX, cardY, cardWidth, cardHeight);

            // Draw enemy portrait
            if (textures.containsKey(enemyName))
                batch.draw(textures.get(enemyName),
                        cardX + 70, // inside green area horizontally
                        cardY + cardHeight - 230, // inside green area vertically
                        200, 200
                );


            // HP (top-left red tag)
            cardFont.draw(batch, " " + (int) enemy.getHealth(),
                    cardX + 19,
                    cardY + cardHeight - 150);

            // ATK (bottom-left red tag)
            cardFont.draw(batch, " " + enemy.getAttackPower(),
                    cardX + 22,
                    cardY + cardHeight - 235);

            // DEF (right red tag)
            cardFont.draw(batch, "" + enemy.getDefensePower(),
                    cardX + cardWidth - 37,
                    cardY + cardHeight - 235);

            cardFont.setColor(Color.BLACK);
            cardFont.draw(batch, "Normal Enemy",
                    cardX + cardWidth / 4 + 14,
                    cardY + 210);
            cardFont.setColor(Color.WHITE);


            GlyphLayout layout = new GlyphLayout(titleFont, enemyName);
            float textWidth = layout.width;
            titleFont.draw(batch, layout,
                    cardX + cardWidth / 2 - textWidth / 2,
                    cardY + 130);
            // Optional dice rendering

            renderDice(batch);
        }
    }


    private void drawTrapInfoCard(SpriteBatch batch, MapEvent event) {

        int healthLoss = 0;
        Object hp = event.getProperties().get("hp");
        if (hp instanceof String) {
            healthLoss = Integer.parseInt((String) hp);
        } else if (hp instanceof Integer) {
            healthLoss = (Integer) hp;
        }
        targetValue = healthLoss;
        float[] iso = toIsometric(event.getGridX(), event.getGridY());
        renderDice(batch, iso[0], iso[1]);
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


    public DialogController getDialogController() {
        return dialogController;
    }

    public void setDialogController(DialogController dialogController) {
        this.dialogController = dialogController;
    }

}