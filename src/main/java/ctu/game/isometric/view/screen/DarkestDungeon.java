package ctu.game.isometric.view.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Timer;
import ctu.game.isometric.IsometricGame;
import ctu.game.isometric.controller.DungeonInputProcessor;
import ctu.game.isometric.controller.GameController;
import ctu.game.isometric.model.entity.Character;
import ctu.game.isometric.model.entity.Enemy;
import ctu.game.isometric.model.game.GameState;
import ctu.game.isometric.model.game.Items;
import ctu.game.isometric.model.game.Reward;
import ctu.game.isometric.util.AssetManager;
import ctu.game.isometric.util.ItemLoader;
import ctu.game.isometric.util.RewardLoader;
import ctu.game.isometric.util.WordNetValidator;
import ctu.game.isometric.view.scene.Character2DRenderer;
import ctu.game.isometric.view.scene.CombatEnvironment3D;
import ctu.game.isometric.view.ui.DefeatRenderer;
import ctu.game.isometric.view.ui.RewardRenderer;
import ctu.game.isometric.view.ui.TutorialRenderer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static ctu.game.isometric.util.FontGenerator.generateVietNameseFont;

public class DarkestDungeon implements Screen {
    private SpriteBatch batch;
    private BitmapFont font;
    private ShapeRenderer shapeRenderer;
    private OrthographicCamera camera;

    // Combat state
    public enum CombatState {
        PLAYER_TURN, ENEMY_TURN, COMBAT_END, ANIMATING
    }

    // Animation state for skill animations
    private enum AnimationState {
        IDLE, MOVE_TO_CENTER, SKILL_EFFECT, MOVE_BACK
    }

    private CombatState combatState = CombatState.PLAYER_TURN;
    private AnimationState animState = AnimationState.IDLE;
    private float animationTimer = 0;
    private final float MOVE_DURATION = 0.8f;
    private final float SKILL_EFFECT_DURATION = 1.2f;
    private final float IDLE_ANIMATION_SPEED = 2.0f;
    private String combatLog = "";

    // Pause menu
    private boolean isPaused = false;

    // Character stats - Enhanced with ATK and DEF
    private int playerHP = 2, playerMaxHP = 60;
    private int playerMana = 25, playerMaxMana = 50;
    private int playerATK = 15, playerDEF = 8;
    private int enemyHP = 20, enemyMaxHP = 40;
    private int enemyMana = 20, enemyMaxMana = 20;
    private int enemyATK = 12, enemyDEF = 5;
    private String playerName = "Plague Doctor";
    private String enemyName = "Cactoid Vertephile";

    // Screen dimensions - Split into two 360px halves
    public final float SCREEN_WIDTH = 1280;
    public final float SCREEN_HEIGHT = 720;
    private final float TOP_HALF_HEIGHT = 360;
    private final float BOTTOM_HALF_HEIGHT = 360;

    // Combat area (top half)
    private final float COMBAT_AREA_Y = BOTTOM_HALF_HEIGHT;
    private final float COMBAT_CENTER_Y = COMBAT_AREA_Y + TOP_HALF_HEIGHT / 2;
    private float playerScale = 1.0f;
    private float enemyScale = 1.0f;
    private final float MAX_SCALE = 1.4f;
    // Bottom UI layout
    private final float STATUS_PANEL_WIDTH = 300;
    private final float STATUS_PANEL_HEIGHT = 200;
    private final float PLAYER_STATUS_X = 50;
    private final float ENEMY_STATUS_X = SCREEN_WIDTH - STATUS_PANEL_WIDTH - 50;
    // Skill bar layout
    private final float SKILL_BAR_WIDTH = 400;
    private final float SKILL_BAR_X = (SCREEN_WIDTH - SKILL_BAR_WIDTH) / 2;

    // Skills configuration
    private String[] skillNames = {"Attack", "Word", "TypeW", "Heal", "Defend"};
    private int[] skillManaCost = {0, 5, 5, 10, 0};
    private boolean[] skillEnabled = {true, true, true, true, true};
    // Textures
    private Texture[] playerIdleTextures = new Texture[2]; // For idle animation
    private Texture[] playerSkillTextures = new Texture[5];
    private Texture[] enemyIdleTextures = new Texture[3]; // For idle animation
    private Texture[] enemySkillTextures = new Texture[3];
    private Texture[] skillButtonTextures = new Texture[5];
    private Texture[] effectTextures = new Texture[3];
    private Texture backgroundTexture;

    // Current states
    private Texture currentPlayerTexture;
    private Texture currentEnemyTexture;
    private Texture currentEffectTexture;
    private boolean showEffect = false;
    private boolean effectOnPlayer = false;
    private int currentSkill = -1;
    private int enemyAction = -1;
    int rewardId = 0;

    // Animation control
    private boolean isAnimating = false;
    private boolean isPlayerAction = false;
    private boolean enemyTurnTriggered = false;
    private float idleAnimationTimer = 0;

    IsometricGame game;
    GameController gameController;
    Character player;
    AssetManager assetManager;

    WordNetValidator wordNetValidator;

    private CombatEnvironment3D environment3D;
    private Character2DRenderer character2DRenderer;

    private Vector3 playerWorldPos = new Vector3(-3f, 1f, 2f);
    private Vector3 enemyWorldPos = new Vector3(5f, 1f, -2f);

    // Camera movement
    private Vector3 cameraTargetPos = new Vector3(0f, 4f, 12f);
    private Vector3 cameraCurrentPos = new Vector3(0f, 4f, 12f);
    private float cameraLerpSpeed = 2f;


    public void resetPosition() {
        playerWorldPos = new Vector3(-3f, 1f, 2f);
        enemyWorldPos = new Vector3(5f, 1f, -2f);
    }

    public DarkestDungeon(IsometricGame game, GameController gameController) {
        this.gameController = gameController;
        this.game = game;
        this.assetManager = game.getAssetManager();
        this.wordNetValidator = gameController.getWordNetValidator();

        environment3D = new CombatEnvironment3D();
        character2DRenderer = new Character2DRenderer(environment3D.getCamera());


        loadTextures();
        this.actionAnimations = game.getAssetManager().getAnimationManager().getActionAnimations();
    }

    private RewardRenderer rewardRenderer;
    private DefeatRenderer defeatRenderer;
    private TutorialRenderer tutorialRenderer;


    public void startCombat(Enemy enemy) {
        this.enemyName = enemy.getEnemyName();
        System.out.println("Starting combat with enemy: " + enemyName);
        this.enemyMaxHP = (int) enemy.getHealth();
        this.enemyHP = enemyMaxHP;


        this.enemyATK = enemy.getAttackPower();
        this.enemyDEF = enemy.getDefensePower();

        this.rewardId = enemy.getRewardID();

        this.playerName = gameController.getCharacter().getName();
        this.playerMaxHP = (int) gameController.getCharacter().getMaxHealth();
        this.playerHP = (int) gameController.getCharacter().getHealth();
        this.playerMana = (int) gameController.getCharacter().getMana();
        this.playerMaxMana = (int) gameController.getCharacter().getMaxMana();
        this.playerATK = (int) gameController.getCharacter().getDamage();
        this.playerDEF = (int) gameController.getCharacter().getDamage();

        this.isAnimating = false;
        this.isPlayerAction = true;
        this.enemyTurnTriggered = false;
        this.combatState = CombatState.PLAYER_TURN;
        this.animState = AnimationState.IDLE;
        this.animationTimer = 0;
        this.idleAnimationTimer = 0;

        this.currentEffectTexture = null;
        this.showEffect = false;
        this.effectOnPlayer = false;
        this.currentSkill = -1;
        this.enemyAction = -1;


        this.combatLog = "Chiến đấu bắt đầu! Chọn hành động.";

        this.victory = false;
        isPaused = false;
        defeated = false;
        isEnded = false;


        currentLevel = gameController.getCharacter().getLevel();
        newLevel = currentLevel;

        item = null;
        reward = null;
        inputWord = "";

        resetPosition();
        enemyIdleTextures = getEnemyIdleTextures(enemy.getTexturePath());
        enemySkillTextures = getEnemySkillTextures(enemy.getTexturePath());
    }

    int currentLevel = 0;
    int newLevel = 0;

    BitmapFont titleFont;

    private OrthographicCamera combatCamera;
    private float cameraZoom = 1f;


    private Texture loadLinearTexture(String path) {
        Texture texture = new Texture(path);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return texture;
    }

    private void loadTextures() {


// Load idle textures
        playerIdleTextures[0] = loadLinearTexture("dungeon/Idle_0001.png");
        playerIdleTextures[1] = loadLinearTexture("dungeon/Idle_0002.png");

// Load skill textures
        playerSkillTextures[0] = loadLinearTexture("dungeon/player_attack.png");
        playerSkillTextures[1] = loadLinearTexture("dungeon/player_flame.png");
        playerSkillTextures[2] = loadLinearTexture("dungeon/player_lightning.png");
        playerSkillTextures[3] = loadLinearTexture("dungeon/player_heal.png");
        playerSkillTextures[4] = loadLinearTexture("dungeon/player_defend.png");

//        enemyIdleTextures = getEnemyIdleTextures("demon");
//        enemySkillTextures = getEnemySkillTextures("demon");

        // Skill button textures
        skillButtonTextures[0] = new Texture("dungeon/skill_attack.png");
        skillButtonTextures[1] = new Texture("dungeon/skill_flame.png");
        skillButtonTextures[2] = new Texture("dungeon/skill_lightning.png");
        skillButtonTextures[3] = new Texture("dungeon/skill_heal.png");
        skillButtonTextures[4] = new Texture("dungeon/skill_defend.png");

        // Effect textures
        effectTextures[0] = new Texture("dungeon/damage_effect.png");
        effectTextures[1] = new Texture("dungeon/heal_effect.png");
        effectTextures[2] = new Texture("dungeon/defense_effect.png");

        backgroundTexture = new Texture("backgrounds/dungeon.png");
        backgroundBlurTexture = new Texture("backgrounds/dungeon_blur.png");
    }

    @Override
    public void show() {


        batch = new SpriteBatch();
        font = generateVietNameseFont("GrenzeGotisch.ttf", 20);
        titleFont = generateVietNameseFont("GrenzeGotisch.ttf", 26);
        inputFont = generateVietNameseFont("Roboto-Black.ttf", 18);

        shapeRenderer = new ShapeRenderer();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, SCREEN_WIDTH, SCREEN_HEIGHT);

        combatCamera = new OrthographicCamera();
        combatCamera.setToOrtho(false, SCREEN_WIDTH, SCREEN_HEIGHT);

        combatLog = "Chiến đấu bắt đầu! Chọn hành động.";


        currentPlayerTexture = playerIdleTextures[0];
        currentEnemyTexture = enemyIdleTextures[0];

        this.player = gameController.getCharacter();

        rewardRenderer = new RewardRenderer(batch, font, titleFont, inputFont, shapeRenderer, assetManager);
        defeatRenderer = new DefeatRenderer(batch, font, titleFont, inputFont, shapeRenderer);
        tutorialRenderer = new TutorialRenderer(batch, font, titleFont, inputFont, shapeRenderer);

        Gdx.input.setInputProcessor(new com.badlogic.gdx.InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    if (showTutorial) {
                        showTutorial = false;
                        currentTutorialPage = 0;
                    } else {
                        isPaused = !isPaused;
                    }
                    return true;
                }

                if (isPaused) {
                    if (keycode == Input.Keys.Q) {
                        Gdx.app.exit();
                        return true;
                    }
                    if (keycode == Input.Keys.T) {
                        showTutorial = !showTutorial;
                        currentTutorialPage = 0;
                        return true;
                    }
                    return true; // Ignore other input when paused
                }

                return false;
            }


            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (!Gdx.input.justTouched()) return false;

                screenY = 720 - screenY; // Invert Y coordinateif

                if (showTutorial) {
                    boolean continueShowing = tutorialRenderer.handleClick(screenX, screenY);
                    if (!continueShowing) {
                        showTutorial = false;
                        currentTutorialPage = 0;
                    }
                    currentTutorialPage = tutorialRenderer.getCurrentPage();
                    return true;
                }

                if (victory) {
                    if (continueButtonBounds != null && continueButtonBounds.contains(screenX, screenY)) {
                        gameController.getCharacter().addItem(item, reward.getAmount());
                        gameController.getCharacter().setHealth(playerHP);
                        gameController.getCharacter().setMana(playerMana);
                        Timer.schedule(new Timer.Task() {
                            @Override
                            public void run() {
//                                gameController.setCompletedEvent();
                                gameController.setState(GameState.EXPLORING);
                                gameController.getCharacter().setHealth(playerHP);
                                gameController.getCharacter().setMana(playerMana);
                                gameController.getMapRenderer().setZoomed(false);
                                gameController.setRenderCharacter(true);
                                game.changeScreen("GAME");

                                if (newLevel > currentLevel) gameController.showLevelUpNotification();
                            }
                        }, 0.5f);
                        if (enemyName.equalsIgnoreCase("Demon"))
                            gameController.completedDungeon2();
                        else if (enemyName.equalsIgnoreCase("Frost Guardian"))
                            gameController.defeatedFrostGuardian();
                    }

                    return true;

                } else if (defeated) {

                    if (continueButtonBounds != null && continueButtonBounds.contains(screenX, screenY)) {
                        if (!isEnded) {
                            game.changeScreen("GAME");
                            gameController.setState(GameState.EXPLORING);
                            gameController.setPreviousState(GameState.EXPLORING);
                            gameController.returnToTower(enemyName);
                            gameController.setRenderCharacter(true);
                            gameController.getCharacter().setDirection("knocked_down");
                            return true;
                        } else {
                            game.changeScreen("GAME_OVER");
                            return true;
                        }
                    }

                    return true; // Ngăn xử lý các input khác khi đã thua
                } else {
                    if (isPaused && !showTutorial) {
                        float menuWidth = 400;
                        float menuHeight = 350; // Increased height for tutorial button
                        float menuX = (SCREEN_WIDTH - menuWidth) / 2;
                        float menuY = (SCREEN_HEIGHT - menuHeight) / 2;

                        float buttonWidth = 240;
                        float buttonHeight = 40;

                        float continueButtonX = menuX + (menuWidth - buttonWidth) / 2;
                        float continueButtonY = menuY + 210;
                        float tutorialButtonY = menuY + 160;
                        float menuButtonY = menuY + 110;
                        float quitButtonY = menuY + 60;

                        // Continue button
                        if (screenX >= continueButtonX && screenX <= continueButtonX + buttonWidth &&
                                screenY >= continueButtonY && screenY <= continueButtonY + buttonHeight) {
                            isPaused = false;
                            return true;
                        }

                        // Tutorial button
                        if (screenX >= continueButtonX && screenX <= continueButtonX + buttonWidth &&
                                screenY >= tutorialButtonY && screenY <= tutorialButtonY + buttonHeight) {
                            showTutorial = true;
                            currentTutorialPage = 0;
                            return true;
                        }

                        // Main menu button
                        if (screenX >= continueButtonX && screenX <= continueButtonX + buttonWidth &&
                                screenY >= menuButtonY && screenY <= menuButtonY + buttonHeight) {
                            gameController.setState(GameState.MAIN_MENU);
                            game.changeScreen("GAME");
                            return true;
                        }

                        // Quit button
                        if (screenX >= continueButtonX && screenX <= continueButtonX + buttonWidth &&
                                screenY >= quitButtonY && screenY <= quitButtonY + buttonHeight) {
                            Gdx.app.exit();
                            return true;
                        }

                        return true; // Ngăn xử lý các input khác khi đang pause
                    }


                    if (combatState == CombatState.PLAYER_TURN && !isAnimating && !isPaused &&
                            screenX >= menuX + 10 && screenX <= menuX + menuWidth - 10) {

                        // Check skill buttons
                        for (int i = 0; i < 5; i++) {
                            float skillY = buttonY - i * (buttonHeight + buttonSpacing);
                            if (screenY >= skillY - buttonHeight && screenY <= skillY) {
                                if (skillEnabled[i]) {
                                    handlePlayerAction(i);
                                    return true;
                                }
                            }
                        }
                    } else if (combatState == CombatState.COMBAT_END) {
                        game.setScreen(new DarkestDungeon(game, gameController));
                        return true;
                    }
                }


                return false;
            }

            @Override
            public boolean keyTyped(char character) {
                if (waitingForInput && showInputField) {
                    if (character == '\r' || character == '\n') { // Enter key
                        if (!inputWord.trim().isEmpty()) {
                            processWordInput(inputWord.trim());
                        }
                    } else if (character == '\b') { // Backspace
                        if (inputWord.length() > 0) {
                            inputWord = inputWord.substring(0, inputWord.length() - 1);
                        }
                    } else if (java.lang.Character.isLetter(character)) {
                        inputWord += java.lang.Character.toUpperCase(character);
                    }
                    return true;
                }
                return false;
            }

            @Override
            public boolean mouseMoved(int screenX, int screenY) {
                mouseX = screenX;
                mouseY = 720 - screenY; // Invert Y coordinate
                updateTooltip();
                return false;
            }
        });
// In the show() method of DarkestDungeon class
//        DungeonInputProcessor inputProcessor = new DungeonInputProcessor(this);
//
//        Gdx.input.setInputProcessor(inputProcessor);
    }


    float menuWidth = 320;
    float menuHeight = 240;
    float menuX = 20;
    float menuY = 20;

    float buttonHeight = 40;
    float buttonSpacing = 8;
    float buttonY = menuY + menuHeight;


    public void updateTooltip() {
        hoveredSkill = -1;
        showTooltip = false;

        if (combatState == CombatState.PLAYER_TURN && !isAnimating && !isPaused) {
            for (int i = 0; i < 5; i++) {
                float skillY = buttonY - i * (buttonHeight + buttonSpacing);

                // Check if mouse is over skill button
                if (mouseX >= menuX + 10 && mouseX <= menuX + menuWidth - 10 &&
                        mouseY >= skillY - buttonHeight && mouseY <= skillY) {
                    hoveredSkill = i;
                    showTooltip = true;
                    break;
                }
            }
        }
    }

    private GlyphLayout layout = new GlyphLayout();

    private void drawTooltip() {
        if (!showTooltip || hoveredSkill == -1) return;

        String description = skillDescriptions[hoveredSkill];
        String manaCost = skillManaCost[hoveredSkill] > 0 ?
                "Năng lượng: " + skillManaCost[hoveredSkill] : "Không tốn năng lượng";

        // Calculate tooltip size
        layout.setText(font, description);
        float tooltipWidth = Math.max(layout.width + 20, 200);
        float tooltipHeight = layout.height + 40;

        // Position tooltip to the right of command menu
        float menuWidth = 320;
        float menuX = 20;
        float tooltipX = menuX + menuWidth + 10;
        float tooltipY = mouseY;

        // Keep tooltip on screen
        tooltipX = Math.max(10, Math.min(tooltipX, SCREEN_WIDTH - tooltipWidth - 10));
        tooltipY = Math.max(10, Math.min(tooltipY, SCREEN_HEIGHT - tooltipHeight - 10));

        // Draw tooltip background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.1f, 0.1f, 0.2f, 0.95f);
        shapeRenderer.rect(tooltipX, tooltipY, tooltipWidth, tooltipHeight);

        // Draw border
        shapeRenderer.setColor(0.4f, 0.4f, 0.6f, 1f);
        shapeRenderer.rect(tooltipX, tooltipY, tooltipWidth, 2);
        shapeRenderer.rect(tooltipX, tooltipY + tooltipHeight - 2, tooltipWidth, 2);
        shapeRenderer.rect(tooltipX, tooltipY, 2, tooltipHeight);
        shapeRenderer.rect(tooltipX + tooltipWidth - 2, tooltipY, 2, tooltipHeight);
        shapeRenderer.end();

        // Draw tooltip text
        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, skillNames[hoveredSkill], tooltipX + 10, tooltipY + tooltipHeight - 10);

        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, description, tooltipX + 10, tooltipY + tooltipHeight - 30);

        font.setColor(skillEnabled[hoveredSkill] ? Color.CYAN : Color.RED);
        font.draw(batch, manaCost, tooltipX + 90, tooltipY + tooltipHeight - 10);
        batch.end();
    }


    Map<String, Texture[]> enemyIdleTexturesCache = new HashMap<>();
    Map<String, Texture[]> enemySkillTexturesCache = new HashMap<>();

    public void loadEnemyTextures(String enemyName) {
        if (enemyIdleTexturesCache.containsKey(enemyName) && enemySkillTexturesCache.containsKey(enemyName)) return;

        Texture[] idleTextures = new Texture[4];
        for (int i = 0; i < 4; i++) {
            String texturePath = "dungeon/" + enemyName.toLowerCase() + (i + 1) + "_idle.png";
            Texture texture = new Texture(texturePath);
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            idleTextures[i] = texture;

        }
        enemyIdleTexturesCache.put(enemyName, idleTextures);

        Texture[] skillTextures = new Texture[3];
        for (int i = 0; i < 3; i++) {
            String texturePath = "dungeon/" + enemyName.toLowerCase() + (i + 1) + "_skill.png";
            Texture texture = new Texture(texturePath);
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            skillTextures[i] = texture;

        }
        enemySkillTexturesCache.put(enemyName, skillTextures);
    }

    public Texture[] getEnemyIdleTextures(String enemyName) {
        if (!enemyIdleTexturesCache.containsKey(enemyName)) {
            loadEnemyTextures(enemyName);
        }
        return enemyIdleTexturesCache.get(enemyName);
    }

    public Texture[] getEnemySkillTextures(String enemyName) {
        if (!enemySkillTexturesCache.containsKey(enemyName)) {
            loadEnemyTextures(enemyName);
        }
        return enemySkillTexturesCache.get(enemyName);
    }


    boolean defeated = false;

    @Override
    public void render(float delta) {

        if (victory) {
            renderReward(batch);
        } else if (defeated && !victory) {
            renderDefeat(batch);
        } else {
            if (!isPaused) {
                updateDamageNumbers(delta);
                updateCombat(delta);
                updateCameraMovement(delta);
                updateCharacterPositions(delta);
                updateIdleAnimations(delta);
            }
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
            Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);

            // Render 3D environment
            environment3D.render();

            // Disable depth test cho 2D rendering
            Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);

            // Render 2D characters
            renderCharacters2D();
            renderDamageNumbers();

            // Draw word display effects
            if (showWordDisplay && !isPaused) {
                drawWordDisplay();
            }
            batch.begin();
            renderActionAnimation(batch);
            batch.end();

            drawBottomUI();
            if (showTooltip && !isPaused) {
                drawTooltip();
            }
            if (showInputField && !isPaused) {
                drawInputField();
            }
            if (isPaused) {
                drawPauseMenu();
            }
            if (showTutorial) {
                drawTutorial();
            }
        }
    }


    private void renderCharacters2D() {
        // Render player
        character2DRenderer.renderCharacter(
                currentPlayerTexture,
                playerWorldPos,
                environment3D.getCamera(),
                playerScale
        );

        // Render enemy
        if (currentEnemyTexture!= null) character2DRenderer.renderCharacter(
                currentEnemyTexture,
                enemyWorldPos,
                environment3D.getCamera(),
                enemyScale
        );

        // Render effects nếu có
        if (showEffect && currentEffectTexture != null) {
            Vector3 effectPos = effectOnPlayer ? playerWorldPos : enemyWorldPos;
            effectPos.y += 0.0f; // Hiệu ứng ở phía trên nhân vật
            character2DRenderer.renderCharacter(
                    currentEffectTexture,
                    effectPos,
                    environment3D.getCamera(),
                    1.2f
            );
        }
    }

    // Thêm method để tạo camera effects
    private void addCameraMovement(Vector3 targetPos, float duration) {
        cameraTargetPos.set(targetPos);
        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                cameraTargetPos.set(0f, 8f, 12f); // Reset về vị trí mặc định
            }
        }, duration);
    }

    private void updateCameraMovement(float delta) {
        PerspectiveCamera cam = environment3D.getCamera();

        // Apply camera positioning based on combat state - gentler movements
        if (isAnimating) {
            switch (animState) {
                case MOVE_TO_CENTER:
                    // Different camera behavior based on skill type
                    if (isPlayerAction) {
                        // For heal/defend, use more subtle camera movement
                        if (currentSkill == 3 || currentSkill == 4) { // Heal or Defend
                            float prepProgress = Math.min(1.0f, animationTimer / (MOVE_DURATION * 0.7f));
                            cameraTargetPos.set(
                                    Interpolation.smooth.apply(1f, 1.5f, prepProgress),
                                    Interpolation.smooth.apply(4f, 4.5f, prepProgress),
                                    Interpolation.smooth.apply(12f, 10f, prepProgress)
                            );
                            // No camera shake
                        } else { // Attack skills - still dramatic but less intense
                            float prepProgress = Math.min(1.0f, animationTimer / (MOVE_DURATION * 0.7f));
                            cameraTargetPos.set(
                                    Interpolation.smooth.apply(-1.5f, -2f, prepProgress),
                                    Interpolation.smooth.apply(4f, 5f, prepProgress),
                                    Interpolation.smooth.apply(12f, 9.5f, prepProgress)
                            );
                        }
                    } else {
                        // Enemy attack - gentler camera angle
                        float prepProgress = Math.min(1.0f, animationTimer / (MOVE_DURATION * 0.7f));
                        cameraTargetPos.set(
                                Interpolation.smooth.apply(1.5f, 2.5f, prepProgress),
                                Interpolation.smooth.apply(4f, 5f, prepProgress),
                                Interpolation.smooth.apply(12f, 10f, prepProgress)
                        );
                    }
                    break;

                case SKILL_EFFECT:
                    // More subtle camera effects during skills
                    if (currentSkill == 0 || enemyAction == 0) { // Basic attack
                        // No shake, just subtle FOV change
                        cam.fieldOfView = MathUtils.lerp(
                                67f,
                                64f,
                                Math.max(0, 1.0f - Math.abs(animationTimer - SKILL_EFFECT_DURATION / 2) * 2)
                        );
                    } else if (currentSkill == 1 || enemyAction == 1) { // Word skill
                        // Subtle camera movement for special moves
                        float angle = animationTimer * 10f; // Reduced rotation speed
                        float radius = 13f;
                        float height = 4.5f + (float) Math.sin(animationTimer * 2) * 0.2f; // Less vertical movement

                        cameraTargetPos.set(
                                (float) Math.cos(Math.toRadians(angle)) * radius,
                                height,
                                (float) Math.sin(Math.toRadians(angle)) * radius
                        );
                        cam.lookAt(0f, 2f, 0f);
                    } else if (currentSkill == 2) { // TypeW - much gentler camera when waiting for input
                        if (waitingForInput) {
//                            // Very subtle movement while waiting for player input
//                            float angle = animationTimer * 10f; // Much slower rotation
//                            float radius = 12f;
//
//                            cameraTargetPos.set(
//                                    (float)Math.cos(Math.toRadians(angle)) * radius * 0.2f,
//                                    4.5f,
//                                    11f + (float)Math.sin(Math.toRadians(angle)) * radius * 0.2f
//                            );
                        } else {
                            // Gentle movement during skill execution
                            startActionAnimation(3);
                            float angle = animationTimer * 20f;
                            float radius = 12f;

                            cameraTargetPos.set(
                                    (float) Math.cos(Math.toRadians(angle)) * radius * 0.5f,
                                    4.5f + (float) Math.sin(animationTimer * 2) * 0.2f,
                                    11f + (float) Math.sin(Math.toRadians(angle)) * radius * 0.5f
                            );
                        }
                        cam.lookAt(0f, 2f, 0f);
                    } else if (currentSkill == 3 || currentSkill == 4 || enemyAction == 2) { // Heal/Defend
//                        // Zoom in gently on the character
//                        Vector3 targetPos = isPlayerAction ?
//                                new Vector3(1.5f, 3.8f, 8f) :
//                                new Vector3(3f, 3.8f, 8f);
//
//                        cameraTargetPos.lerp(targetPos, delta * 2f);
//                        cam.lookAt(isPlayerAction ? playerWorldPos : enemyWorldPos);
                    }
                    break;

                case MOVE_BACK:
                    // Smooth transition back to default with reduced speed
                    float returnProgress = Math.min(1.0f, animationTimer / (MOVE_DURATION * 0.8f));
                    cameraTargetPos.set(
                            Interpolation.smooth.apply(cam.position.x, 0f, returnProgress),
                            Interpolation.smooth.apply(cam.position.y, 4f, returnProgress),
                            Interpolation.smooth.apply(cam.position.z, 12f, returnProgress)
                    );
                    // Gradually restore default FOV
                    cam.fieldOfView = MathUtils.lerp(cam.fieldOfView, 67f, delta * 2.5f);
                    break;
            }
        } else {
            // Very subtle ambient camera movement during idle states
            float idleTime = idleAnimationTimer * 0.3f; // Reduced speed
            cameraTargetPos.set(
                    (float) Math.sin(idleTime) * 0.2f,
                    4f + (float) Math.sin(idleTime * 0.5f) * 0.15f,
                    12f + (float) Math.cos(idleTime * 0.4f) * 0.25f
            );

            // Restore default FOV smoothly
            cam.fieldOfView = MathUtils.lerp(cam.fieldOfView, 67f, delta * 2f);
        }

        // Smoother camera position interpolation with reduced speed
        float lerpSpeed = isAnimating ?
                (currentSkill == 3 || currentSkill == 4 || enemyAction == 2) ? 1.5f : 2.0f
                : 1.2f;
        cameraCurrentPos.lerp(cameraTargetPos, delta * lerpSpeed);

        // Apply very subtle tilt based on turn state
        if (combatState == CombatState.PLAYER_TURN && !isAnimating) {
            cam.up.set(0, 1, 0.03f * (float) Math.sin(idleAnimationTimer * 0.6f));
        } else if (combatState == CombatState.ENEMY_TURN && !isAnimating) {
            cam.up.set(0, 1, -0.03f * (float) Math.sin(idleAnimationTimer * 0.6f));
        } else {
            cam.up.lerp(new Vector3(0, 1, 0), delta * 2f);
        }

        // Apply position without camera shake
        cam.position.set(cameraCurrentPos);

        cam.lookAt(0f, 2f, 0f);
        cam.update();
    }

    private void updateCharacterPositions(float delta) {
        if (isAnimating) {
            // Cập nhật vị trí 3D của nhân vật trong animation
//            updateCharacterAnimation3D(delta);
        }
    }

    private void updateCharacterAnimation3D(float delta) {
        float progress = animationTimer;

        switch (animState) {
            case MOVE_TO_CENTER:
                if (isPlayerAction) {
                    // Di chuyển trong không gian 3D
                    Vector3 targetPos = new Vector3(0f, 1f, 1f);
                    playerWorldPos.lerp(targetPos, progress);

                    // Thay đổi góc camera để tập trung vào action
                    cameraTargetPos.set(-2f, 6f, 8f);
                } else {
                    Vector3 targetPos = new Vector3(2f, 1f, -1f);
                    enemyWorldPos.lerp(targetPos, progress);
                    cameraTargetPos.set(2f, 6f, 8f);
                }
                break;

            case MOVE_BACK:
                // Reset positions
                if (isPlayerAction) {
                    playerWorldPos.lerp(new Vector3(-3f, 1f, 2f), progress);
                } else {
                    enemyWorldPos.lerp(new Vector3(5f, 1f, -2f), progress);
                }
                cameraTargetPos.set(0f, 4f, 12f);
                break;
        }
    }

    private void drawInputField() {
        if (!showInputField) return;

        float fieldWidth = 300;
        float fieldHeight = 40;
        float fieldX = (SCREEN_WIDTH - fieldWidth) / 2 + 35;
        float fieldY = COMBAT_CENTER_Y - 50;


        // Draw input field background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.2f, 0.2f, 0.3f, 0.9f);
        shapeRenderer.rect(fieldX, fieldY, fieldWidth, fieldHeight);
        shapeRenderer.setColor(0.6f, 0.6f, 0.8f, 1f);
        shapeRenderer.rect(fieldX, fieldY, fieldWidth, 2);
        shapeRenderer.rect(fieldX, fieldY + fieldHeight - 2, fieldWidth, 2);
        shapeRenderer.rect(fieldX, fieldY, 2, fieldHeight);
        shapeRenderer.rect(fieldX + fieldWidth - 2, fieldY, 2, fieldHeight);
        shapeRenderer.end();

        // Draw input text
        batch.begin();
        font.setColor(Color.WHITE);
        inputFont.draw(batch, "Nhấn ENTER để hoàn thành từ", fieldX, fieldY + 70);
        inputFont.draw(batch, inputWord + "_", fieldX + 30, fieldY + 25);
        batch.end();
    }


    boolean victory = false;

    private Rectangle continueButtonBounds;
    Items item = null;
    Reward reward = null;

    private void renderReward(SpriteBatch batch) {
        rewardRenderer.setReward(item, reward);
        rewardRenderer.render();
        continueButtonBounds = rewardRenderer.getContinueButtonBounds();
    }

    private void renderDefeat(SpriteBatch batch) {
        defeatRenderer.setEnded(isEnded);
        defeatRenderer.render();
        continueButtonBounds = defeatRenderer.getContinueButtonBounds();
    }

    private void updateIdleAnimations(float delta) {
        if (animState == AnimationState.IDLE) {
            idleAnimationTimer += delta;

            // Switch between idle frames
            int frame = (int) (idleAnimationTimer * IDLE_ANIMATION_SPEED) % 2;
            currentPlayerTexture = playerIdleTextures[frame];
            currentEnemyTexture = enemyIdleTextures[frame];
        }
    }

    private float combatEndDelayTimer = 0f;
    private boolean pendingCombatEnd = false;
    private float timeScale = 1f;
    private float slowMotionTimer = 0f;

    private void triggerSlowMotion(float duration, float scale) {
        timeScale = scale;
        slowMotionTimer = duration;
    }


    private boolean isEnded = false;

    private void updateCombat(float delta) {
        float scaledDelta = delta * timeScale;

        // Restore normal time
        if (slowMotionTimer > 0) {
            slowMotionTimer -= delta;
            if (slowMotionTimer <= 0) {
                timeScale = 1f;
            }
        }

        updateWordDisplay(scaledDelta);
        for (int i = 0; i < skillManaCost.length; i++) {
            skillEnabled[i] = (playerMana >= skillManaCost[i]);
        }

        // Animation update
        if (isAnimating) {
            animationTimer += delta;
            updateAnimation();
            updateAnimations(delta);

        }

        // Delay combat end if flagged
        if (pendingCombatEnd) {
            combatEndDelayTimer += delta;
            if (combatEndDelayTimer >= 2.0f) { // Delay 1.5 seconds
                pendingCombatEnd = false;
                combatEndDelayTimer = 0f;
                if (playerHP <= 0) {
//                    gameController.getMusicController().playMusic("defeat");
                    combatState = CombatState.COMBAT_END;
                    combatLog = "THẤT BẠI! Bạn đã thua trong chiến đấu.";
                    victory = false;
                    defeated = true;

                    isEnded = gameController.getCharacter().gameOver();


                } else if (enemyHP <= 0) {
                    combatState = CombatState.COMBAT_END;
                    combatLog = "CHIẾN THẮNG! Kẻ địch đã bị hạ gục!";
//                    gameController.getMusicController().playMusic("victory");
                    victory = true;
                    defeated = false;
                    isEnded = false;
                    reward = RewardLoader.getRewardById(this.rewardId);
                    item = ItemLoader.getItemById(reward.getItemID());
                }

                isAnimating = false;
            }
            return; // Wait before continuing update
        }

        // Trigger delayed end if HP hits 0
        if (!pendingCombatEnd) {
            if (playerHP <= 0 || enemyHP <= 0) {
                pendingCombatEnd = true;
                combatEndDelayTimer = 0f;
            }
        }

        // Auto enemy turn trigger
        if (combatState == CombatState.ENEMY_TURN && !isAnimating && !enemyTurnTriggered) {
            enemyTurnTriggered = true;
            startEnemyTurn();

        }
    }

    // Animation fields
    private Animation<TextureRegion>[] actionAnimations; // def, heal, attack, skill
    private float stateTime = 0;
    private boolean isPlayingAnimation = false;
    private int currentAnimationIndex = -1;
    private float animationScale = 1.5f; // Scale for rendering animations


    private void startActionAnimation(int animationIndex) {
        currentAnimationIndex = animationIndex;
        isPlayingAnimation = true;
        stateTime = 0;
    }

    private boolean isAuraAnimationActive = false;

    private void updateAnimations(float delta) {
        // Update regular animations
        if (isPlayingAnimation && currentAnimationIndex >= 0 && currentAnimationIndex < actionAnimations.length) {
            stateTime += delta;
            if (actionAnimations[currentAnimationIndex].isAnimationFinished(stateTime)) {
                isPlayingAnimation = false;
            }
        }

        // Handle aura animation separately with smoother transitions
        if (currentSkill == 2) {
            if (waitingForInput && !isAuraAnimationActive) {
                // Start aura animation
                isAuraAnimationActive = true;
                stateTime = 0;
            } else if (!waitingForInput && isAuraAnimationActive) {
                // Stop aura animation
                isAuraAnimationActive = false;
            }
        } else if (isAuraAnimationActive) {
            // If skill changed, stop aura animation
            isAuraAnimationActive = false;
        }

        // Always update aura animation time while active for smooth looping
        if (isAuraAnimationActive) {
            stateTime += delta;
        }
    }

    private void renderActionAnimation(SpriteBatch batch) {
        // Special case for skill 2 (TypeW) - show looping aura when waiting for input
        if (isAuraAnimationActive) {
            // Use aura animation (index 4) and make it loop
            TextureRegion currentFrame = actionAnimations[4].getKeyFrame(stateTime, true); // true = looping

            // Show on player character's position
            Vector3 position = playerWorldPos;

            // Convert 3D world position to screen coordinates
            Vector3 screenPos = environment3D.getCamera().project(new Vector3(position));

            // Center the animation on the character
            float x = screenPos.x - (192 * animationScale / 2) - 40;
            float y = screenPos.y - (192 * animationScale / 2) - 20;

            // Draw the looping aura animation
            batch.draw(currentFrame, x, y, 192 * animationScale, 192 * animationScale);
        }

        // Original animation rendering logic
        if (!isPlayingAnimation || currentAnimationIndex < 0 || currentAnimationIndex >= actionAnimations.length) {
            return;
        }

        // Get current frame
        TextureRegion currentFrame = actionAnimations[currentAnimationIndex].getKeyFrame(stateTime, false);

        Vector3 position;

        // For attack and skill animations, show them on the target instead of the actor
        if (currentAnimationIndex == 2 || currentAnimationIndex == 3) { // Attack or Skill
            // Show on opponent's position (reverse of isPlayerAction)
            position = isPlayerAction ? enemyWorldPos : playerWorldPos;
        } else { // Defense and Heal animations
            // Show on the acting character's position
            position = isPlayerAction ? playerWorldPos : enemyWorldPos;
        }

        // Convert 3D world position to screen coordinates
        Vector3 screenPos = environment3D.getCamera().project(new Vector3(position));

        // Center the animation on the character
        float x = screenPos.x - (192 * animationScale / 2);
        float y = screenPos.y - (192 * animationScale / 2);

        // Draw the animation frame
        batch.draw(currentFrame, x, y, 192 * animationScale, 192 * animationScale);
    }

    private void updateAnimation() {
        float progress = animationTimer;

        switch (animState) {
            case MOVE_TO_CENTER:
                if (progress >= MOVE_DURATION) {
                    animState = AnimationState.SKILL_EFFECT;
                    animationTimer = 0;
                    applySkillEffects();
                    showEffect = true;
                } else {
                    updateMoveToCenter(progress / MOVE_DURATION);
                }
                break;

            case SKILL_EFFECT:
                if (currentSkill == 2 && waitingForInput) {
                    return;
                }

                if (progress >= SKILL_EFFECT_DURATION) {
                    animState = AnimationState.MOVE_BACK;
                    animationTimer = 0;
                    showEffect = false;
                } else {
                }
                break;

            case MOVE_BACK:
                if (progress >= MOVE_DURATION) {
                    animState = AnimationState.IDLE;
                    animationTimer = 0;
                    isAnimating = false;
                    resetCharacterPositions();
                    finishTurn();
                } else {
                    updateMoveBack(progress / MOVE_DURATION);
                }
                break;
        }
    }

    float offsetX = 0f;

    // Replace updateMoveToCenter with this cinematic version:
    private void updateMoveToCenter(float progress) {
        // Use different easing for more dramatic effect
        float anticipationProgress = progress < 0.3f ?
                Interpolation.pow3In.apply(progress / 0.3f) * 0.1f :
                0.1f + Interpolation.bounceOut.apply((progress - 0.3f) / 0.7f) * 0.9f;

        boolean shouldMove = isPlayerAction ?
                (currentSkill == 0 || currentSkill == 1 || currentSkill == 2) :
                (enemyAction == 0 || enemyAction == 1);

        if (shouldMove) {
            if (isPlayerAction) {
                // Dramatic zoom in during player action
                cameraZoom = MathUtils.lerp(1f, 0.8f, anticipationProgress);

                // Character moves with anticipation
                float moveX = anticipationProgress < 0.2f ? -30 * (anticipationProgress / 0.2f) : // Pull back
                        MathUtils.lerp(-30, 150, (anticipationProgress - 0.2f) / 0.8f); // Rush forward

                if (currentSkill == 2 || waitingForInput)
                    offsetX = 50;

                else offsetX = 250;


                // Dynamic scaling with overshoot
                playerScale = MathUtils.lerp(1.0f, MAX_SCALE * 1.2f, anticipationProgress);

                // Enemy reacts
                enemyScale = MathUtils.lerp(1.0f, 0.9f, anticipationProgress); // Slightly shrink
            } else {
                // Dramatic zoom in during enemy action
                cameraZoom = MathUtils.lerp(1f, 0.8f, anticipationProgress);

                // Character moves with anticipation
                float moveX = anticipationProgress < 0.2f ? 30 * (anticipationProgress / 0.2f) : // Pull back
                        MathUtils.lerp(30, -150, (anticipationProgress - 0.2f) / 0.8f); // Rush forward


                // Dynamic scaling with overshoot
                enemyScale = MathUtils.lerp(1.0f, MAX_SCALE * 1.2f, anticipationProgress);

                // Player reacts
                playerScale = MathUtils.lerp(1.0f, 0.9f, anticipationProgress); // Slightly shrink
            }
        }
    }


    private void updateMoveBack(float progress) {
        // Only move back for attack skills, not heal/defend
        boolean shouldMove = isPlayerAction ?
                (currentSkill == 0 || currentSkill == 1 || currentSkill == 2) :
                (enemyAction == 0 || enemyAction == 1);

        if (shouldMove) {
            // Bỏ qua hiệu ứng di chuyển từ từ, set về gốc luôn
            playerScale = 1.0f;

            enemyScale = 1.0f;
        }
    }


    private void resetCharacterPositions() {
        playerScale = 1.0f;

        enemyScale = 1.0f;

        currentPlayerTexture = playerIdleTextures[0];
        currentEnemyTexture = enemyIdleTextures[0];
    }

    private void finishTurn() {
        // Switch turns properly
        if (isPlayerAction) {
            combatState = CombatState.ENEMY_TURN;
            enemyTurnTriggered = false; // Reset for next enemy turn
        } else {
            combatState = CombatState.PLAYER_TURN;
        }
        isPlayerAction = false;
    }

    private void applySkillEffects() {
        if (isPlayerAction) {
            applyPlayerSkillEffects();
        } else {
            applyEnemySkillEffects();
        }
    }

    // Add these fields to the DarkestDungeon class
    private String inputWord = "";
    private boolean showInputField = false;
    private boolean waitingForInput = false;
    private Rectangle inputFieldBounds;
    private BitmapFont inputFont;
    // Add these fields to the DarkestDungeon class
    private String displayWord = "";
    private boolean showWordDisplay = false;
    private float wordDisplayTimer = 0f;
    private final float WORD_DISPLAY_DURATION = 2f;

    private void applyPlayerSkillEffects() {
        switch (currentSkill) {
            case 0: // Attack
                startActionAnimation(2);
                int damage = MathUtils.random(playerATK - 2, playerATK + 2) - enemyDEF;
                damage = Math.max(1, damage);
                enemyHP = Math.max(0, enemyHP - damage);
                combatLog = "Bạn tấn công gây " + damage + " sát thương!";
                showDamageNumber(damage, true, false);
                break;
            case 1: // Word (Random from learned words)
                startActionAnimation(3);
                playerMana -= 5;
                Set<String> learnedWords = gameController.getCharacter().getLearnedWords();
                if (learnedWords != null && !learnedWords.isEmpty()) {
                    String[] wordsArray = learnedWords.toArray(new String[0]);
                    String randomWord = wordsArray[MathUtils.random(wordsArray.length - 1)];

                    int wordScore = wordNetValidator.getTotalScore(randomWord);
                    int wordDamage = Math.max(1, wordScore + playerATK - enemyDEF);
                    enemyHP = Math.max(0, enemyHP - wordDamage);
                    combatLog = "Từ '" + randomWord + "' gây " + wordDamage + " sát thương!";

                    // Show word display effect
                    displayWord = randomWord;
                    showWordDisplay = true;
                    wordDisplayTimer = 0f;
                    showDamageNumber(wordDamage, true, false);
                } else {
                    int basicDamage = MathUtils.random(playerATK - 2, playerATK + 2) - enemyDEF;
                    basicDamage = Math.max(1, basicDamage);
                    enemyHP = Math.max(0, enemyHP - basicDamage);
                    combatLog = "Không có từ nào! Tấn công thường " + basicDamage + " sát thương!";
                }
                break;
            case 2: // TypeW (Input word)
                startActionAnimation(4);
                playerMana -= 5;
                showInputField = true;
                waitingForInput = true;
                inputWord = "";
                combatLog = "Nhập một từ và nhấn ENTER\n Từ không hợp lệ sẽ gây sát thương (Phản sát thương) cho bạn!";
                return;
            case 3: // Heal
                startActionAnimation(1);
                playerMana -= 10;
                int heal = (int) (playerMaxHP * 0.2f);
                playerHP = Math.min(playerMaxHP, playerHP + heal);
                combatLog = "Bạn hồi phục" + heal + " sinh lực!";
                showDamageNumber(heal, false, true);
                break;
            case 4: // Defend
                startActionAnimation(0);
                playerMana = Math.min(playerMaxMana, playerMana + 5);
                playerDEF += 3;
                combatLog = "Bạn phòng thủ và hồi phục 5 mana! Phòng thủ tăng lên!";
                break;
        }
    }

    private void updateWordDisplay(float delta) {
        if (showWordDisplay) {
            wordDisplayTimer += delta;
            if (wordDisplayTimer >= WORD_DISPLAY_DURATION) {
                showWordDisplay = false;
            }
        }
    }

    private void drawWordDisplay() {
        if (!showWordDisplay) return;

        batch.begin();

        // Calculate floating animation
        float floatOffset = (float) Math.sin(wordDisplayTimer * 4) * 5;
        float fadeAlpha = Math.max(0, 1 - (wordDisplayTimer / WORD_DISPLAY_DURATION));

        // Position above combat area
        float wordX = SCREEN_WIDTH / 2;
        float wordY = COMBAT_CENTER_Y + 100 + floatOffset;

        // Draw word with glow effect
        titleFont.setColor(1f, 1f, 0.3f, fadeAlpha); // Yellow with fade

        // Draw outline for better visibility
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                if (x != 0 || y != 0) {
                    titleFont.setColor(0f, 0f, 0f, fadeAlpha * 0.8f);
                    layout.setText(titleFont, displayWord);
                    titleFont.draw(batch, layout, wordX - layout.width / 2 + x, wordY + y);
                }
            }
        }

        // Draw main text
        titleFont.setColor(1f, 1f, 0.3f, fadeAlpha);
        layout.setText(titleFont, displayWord);
        titleFont.draw(batch, layout, wordX - layout.width / 2, wordY - 100);

        batch.end();

    }


    public void processWordInput(String word) {
        displayWord = word;
        showWordDisplay = true;
        wordDisplayTimer = 0f;

        if (wordNetValidator.isValidWord(word)) {
            int wordScore = wordNetValidator.getTotalScore(word);
            int wordDamage = Math.max(1, wordScore + playerATK - enemyDEF);
            enemyHP = Math.max(0, enemyHP - wordDamage);
            combatLog = "'" + word + "' hợp lệ! Gây " + wordDamage + " sát thương!";

            currentEffectTexture = effectTextures[0];
            effectOnPlayer = false;
            showDamageNumber(wordScore, true, false);
            if (gameController.getCharacter().updateDict(word))
                gameController.getDictionaryView().addNewWord(word);
        } else {
            int selfDamage = MathUtils.random(3, 8);
            playerHP = Math.max(0, playerHP - selfDamage);
            combatLog = "'" + word + "' không hợp lệ! Bạn nhận" + selfDamage + " sát thương!";

            currentEffectTexture = effectTextures[0];
            effectOnPlayer = true;
            showDamageNumber(selfDamage, false, false);
        }

        showInputField = false;
        waitingForInput = false;
        inputWord = "";
    }

    private void applyEnemySkillEffects() {
        switch (enemyAction) {
            case 0: // Attack
                startActionAnimation(2);
                int damage = MathUtils.random(enemyATK - 2, enemyATK + 2) - playerDEF;
                damage = Math.max(1, damage);
                playerHP = Math.max(0, playerHP - damage);
                combatLog = enemyName + " tấn công gây " + damage + " sát thương!";
                showDamageNumber(damage, false, false);
                break;
            case 1: // Special
                startActionAnimation(3);
                if (enemyMana >= 8) {
                    enemyMana -= 8;
                    int specialDamage = MathUtils.random(enemyATK + 5, enemyATK + 10) - playerDEF;
                    specialDamage = Math.max(1, specialDamage);
                    playerHP = Math.max(0, playerHP - specialDamage);
                    combatLog = enemyName + " sử dụng kỹ năng gây " + specialDamage + " sát thương!";
                    showDamageNumber(specialDamage, false, false);
                } else {
                    damage = MathUtils.random(enemyATK - 2, enemyATK + 2) - playerDEF;
                    damage = Math.max(1, damage);
                    playerHP = Math.max(0, playerHP - damage);
                    combatLog = enemyName + " tấn công gây " + damage + " sát thương!";
                }
                break;
            case 2: // Heal
                startActionAnimation(1);
                if (enemyMana >= 7) {
                    enemyMana -= 7;
                    int healAmount = enemyMaxHP / 4;
                    enemyHP = Math.min(enemyMaxHP, enemyHP + healAmount);
                    combatLog = enemyName + " hồi phục được " + healAmount + " sinh lực!";
                    showDamageNumber(healAmount, true, true);
                } else {
                    damage = MathUtils.random(enemyATK - 2, enemyATK + 2) - playerDEF;
                    damage = Math.max(1, damage);
                    playerHP = Math.max(0, playerHP - damage);
                    combatLog = enemyName + " tấn công gây " + damage + " sát thương!";
                    showDamageNumber(damage, false, false);
                }
                break;
        }
    }

    Texture backgroundBlurTexture;

    // Add these fields to the class
    private boolean showTutorial = false;
    private int currentTutorialPage = 0;

    private void drawTutorial() {
        tutorialRenderer.setCurrentPage(currentTutorialPage);
        tutorialRenderer.render();
    }

    // Add these fields to the class
    private float mouseX = 0, mouseY = 0;
    private int hoveredSkill = -1;
    private boolean showTooltip = false;

    // Tooltip data
    private String[] skillDescriptions = {
            "Đòn tấn công cơ bản gây sát thương vật lý",
            "Niệm một từ từ từ điển của bạn\nGây sát thương dựa trên điểm số của từ",
            "Tự gõ một từ thủ công\nTừ không hợp lệ sẽ gây sát thương cho bạn!",
            "Hồi phục điểm máu\nTốn 10 năng lượng",
            "Tăng phòng thủ và hồi năng lượng\nKhông tốn mana"
    };

    private void drawBottomUI() {

        // Draw FF7R style UI components
        drawCommandMenu();        // Bottom left
        drawCharacterPanel();     // Bottom right
        drawEnemyStatusBar();     // Top middle (boss style)
        drawTurnIndicator();      // Top left
        drawRebirthStyleCombatLog(); // Keep combat log
    }

    private void drawCommandMenu() {
        float buttonY = menuY + menuHeight - 50;
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Draw subtle background with glow effect
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.setColor(0.05f, 0.1f, 0.2f, 0.3f);
        shapeRenderer.rect(menuX, menuY, menuWidth, menuHeight);

        // Top highlight line
        shapeRenderer.setColor(0.2f, 0.5f, 0.9f, 0.6f);
        shapeRenderer.rectLine(menuX, menuY + menuHeight, menuX + menuWidth, menuY + menuHeight, 1);


        for (int i = 0; i < 5; i++) {
            boolean canUse = skillEnabled[i] && combatState == CombatState.PLAYER_TURN && !isAnimating;
            float alpha = canUse ? 0.8f : 0.3f;

            // Button highlight
            if (hoveredSkill == i) {
                shapeRenderer.setColor(0.2f, 0.6f, 0.9f, 0.2f);
                shapeRenderer.rect(menuX + 10, buttonY - i * (buttonHeight + buttonSpacing), menuWidth - 20, buttonHeight);
            }

            // Button line
            shapeRenderer.setColor(0.4f, 0.6f, 0.9f, alpha);
            shapeRenderer.rectLine(
                    menuX + 10,
                    buttonY - i * (buttonHeight + buttonSpacing),
                    menuX + 30,
                    buttonY - i * (buttonHeight + buttonSpacing),
                    1);
        }

        shapeRenderer.end();

        // Draw command text
        batch.begin();
        titleFont.setColor(0.9f, 0.95f, 1f, 0.9f);
        titleFont.draw(batch, "COMMANDS", menuX + 180, menuY + menuHeight - 5);

        for (int i = 0; i < 5; i++) {
            boolean canUse = skillEnabled[i] && combatState == CombatState.PLAYER_TURN && !isAnimating;
            float alpha = canUse ? 1.0f : 0.5f;

            if (skillButtonTextures[i] != null) {
                batch.setColor(canUse ? Color.WHITE : new Color(0.5f, 0.5f, 0.6f, 0.5f));
                batch.draw(skillButtonTextures[i], menuX + 15, buttonY - i * (buttonHeight + buttonSpacing) + 4, 32, 32);
                batch.setColor(Color.WHITE);
            }

            // Skill name
            font.setColor(canUse ? new Color(0.9f, 0.9f, 1f, 1f) : new Color(0.6f, 0.6f, 0.7f, 0.5f));
            font.draw(batch, skillNames[i], menuX + 55, buttonY - i * (buttonHeight + buttonSpacing) + 25);

            // Mana cost
            if (skillManaCost[i] > 0) {
                font.setColor(canUse ? Color.CYAN : new Color(0.3f, 0.5f, 0.7f, 0.5f));
                font.draw(batch, "MP: " + skillManaCost[i], menuX + menuWidth - 70, buttonY - i * (buttonHeight + buttonSpacing) + 25);
            }
        }
        batch.end();
    }

    private void drawCharacterPanel() {
        float panelWidth = 310;
        float panelHeight = 180;
        float panelX = SCREEN_WIDTH - panelWidth - 20;
        float panelY = 20;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Panel background glow
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.setColor(0.05f, 0.1f, 0.15f, 0.3f);
        shapeRenderer.rect(panelX, panelY, panelWidth, panelHeight);

        // Blue accent lines
        shapeRenderer.setColor(0.2f, 0.5f, 0.9f, 0.6f);
        shapeRenderer.rectLine(panelX, panelY + panelHeight, panelX + panelWidth, panelY + panelHeight, 1);
        shapeRenderer.rectLine(panelX, panelY, panelX, panelY + panelHeight, 1);

        // Status bars
        float barY = panelY + panelHeight - 60;
        float manaBarY = panelY + panelHeight - 90;

        // HP bar with segments
        drawRebirthBar(panelX + 60, barY, 220, 16,
                (float) playerHP / playerMaxHP,
                new Color(0.05f, 0.15f, 0.1f, 0.8f),
                new Color(0.1f, 0.8f, 0.3f, 0.9f));

        // MP bar with segments
        drawRebirthBar(panelX + 60, manaBarY, 220, 12,
                (float) playerMana / playerMaxMana,
                new Color(0.05f, 0.1f, 0.2f, 0.8f),
                new Color(0.2f, 0.4f, 0.9f, 0.9f));

        shapeRenderer.end();

        // Draw character info text
        batch.begin();
        titleFont.setColor(0.9f, 0.95f, 1f, 0.9f);
        layout.setText(titleFont, playerName);
        titleFont.draw(batch, playerName,
                panelX + (panelWidth - layout.width) / 2,
                panelY + panelHeight - 15);

        font.setColor(0.8f, 0.9f, 1f, 0.9f);
        font.draw(batch, "HP", panelX + 25, barY + 18);
        font.draw(batch, playerHP + "/" + playerMaxHP, panelX + panelWidth - 40, barY + 11);

        font.draw(batch, "MP", panelX + 25, manaBarY + 16);
        font.draw(batch, playerMana + "/" + playerMaxMana, panelX + panelWidth - 40, manaBarY + 8);

        // Stats with icons
        font.draw(batch, "ATK: " + playerATK, panelX + 20, panelY + 50);
        font.draw(batch, "DEF: " + playerDEF, panelX + 140, panelY + 50);

        batch.end();
    }

    private void drawEnemyStatusBar() {
        float barWidth = 600;
        float barHeight = 60;
        float barX = (SCREEN_WIDTH - barWidth) / 2;
        float barY = SCREEN_HEIGHT - barHeight - 10;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Boss-style wide bar with subtle background
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.setColor(0.05f, 0.1f, 0.15f, 0.7f);
        shapeRenderer.rect(barX, barY, barWidth, barHeight);

        // Red accent line

        layout.setText(titleFont, enemyName);
        float gapWidth = layout.width + 30;
        float halfGap = gapWidth / 2;
        float centerX = barX + barWidth / 2;
        shapeRenderer.setColor(0.8f, 0.2f, 0.2f, 0.6f);
        shapeRenderer.rectLine(barX, barY, centerX - halfGap, barY, 1);
        shapeRenderer.rectLine(centerX + halfGap, barY, barX + barWidth, barY, 1);


        // HP bar
        drawRebirthBar(barX + 100, barY + 25, barWidth - 120, 15,
                (float) enemyHP / enemyMaxHP,
                new Color(0.15f, 0.05f, 0.05f, 0.8f),
                new Color(0.8f, 0.2f, 0.2f, 0.9f));

        shapeRenderer.end();

        // Draw enemy info
        batch.begin();
        titleFont.setColor(0.9f, 0.7f, 0.7f, 0.9f);
        titleFont.draw(batch, enemyName,
                barX + (barWidth - layout.width) / 2,
                barY + barHeight - 50);


        font.setColor(0.8f, 0.7f, 0.7f, 0.8f);
        font.draw(batch, "HP", barX + 70, barY + 52);
        font.draw(batch, enemyHP + "/" + enemyMaxHP, barX + barWidth - 60, barY + 38);
        batch.end();
    }

    private void drawTurnIndicator() {
        float indicatorWidth = 180;
        float indicatorHeight = 40;
        float indicatorX = 10;
        float indicatorY = SCREEN_HEIGHT - indicatorHeight - 10;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Semi-transparent background
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.setColor(0.05f, 0.1f, 0.15f, 0.7f);
        shapeRenderer.rect(indicatorX, indicatorY, indicatorWidth, indicatorHeight);

        // Accent line based on turn
        if (combatState == CombatState.PLAYER_TURN) {
            shapeRenderer.setColor(0.2f, 0.6f, 0.9f, 0.8f);
        } else {
            shapeRenderer.setColor(0.8f, 0.3f, 0.3f, 0.8f);
        }
        shapeRenderer.rectLine(indicatorX, indicatorY, indicatorX + indicatorWidth, indicatorY, 1);

        shapeRenderer.end();

        // Draw turn text
        batch.begin();
        font.setColor(0.9f, 0.9f, 1.0f, 0.9f);
        font.draw(batch, getTurnText(), indicatorX + 15, indicatorY + 25);
        batch.end();
    }

    private void drawRebirthBar(float x, float y, float width, float height, float fillPercent,
                                Color bgColor, Color fillColor) {
        int segments = 20; // More segments for smoother look
        float segmentWidth = width / segments;
        float segmentSpacing = 1; // Smaller spacing for smoother look
        float filledWidth = width * fillPercent;

        // Background with subtle gradient
        shapeRenderer.setColor(bgColor);
        shapeRenderer.rect(x, y, width, height);

        // Filled segments
        for (int i = 0; i < segments; i++) {
            float segX = x + i * segmentWidth;
            if (segX < x + filledWidth) {
                float segW = Math.min(segmentWidth - segmentSpacing, x + filledWidth - segX);

                // Add subtle gradient effect to segments
                float brightness = 0.8f + 0.2f * (float) Math.sin(i * 0.3f);
                shapeRenderer.setColor(
                        fillColor.r * brightness,
                        fillColor.g * brightness,
                        fillColor.b * brightness,
                        fillColor.a
                );

                shapeRenderer.rect(segX, y + 1, segW, height - 2);
            }
        }

        // Top highlight
        shapeRenderer.setColor(1f, 1f, 1f, 0.4f);
        shapeRenderer.rectLine(x, y + height, x + width, y + height, 1);
    }

    float logWidth = 580;
    float logHeight = 40;
    float logX = (SCREEN_WIDTH - logWidth) / 2 + 10;
    float logY = 5;

    private void drawRebirthStyleCombatLog() {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.05f, 0.1f, 0.15f, 0.4f);
        shapeRenderer.rect(logX, logY, logWidth, logHeight);

        // FF7-style border
        shapeRenderer.setColor(0.3f, 0.5f, 0.9f, 0.6f);
        shapeRenderer.rectLine(logX + 10, logY, logX + logWidth - 10, logY, 1);
        shapeRenderer.rectLine(logX + 10, logY + logHeight, logX + logWidth - 10, logY + logHeight, 1);
        shapeRenderer.rectLine(logX, logY + 5, logX, logY + logHeight - 5, 1);
        shapeRenderer.rectLine(logX + logWidth, logY + 5, logX + logWidth, logY + logHeight - 5, 1);

        // Corner connectors
        shapeRenderer.rectLine(logX, logY + 5, logX + 10, logY, 1);
        shapeRenderer.rectLine(logX + logWidth - 10, logY, logX + logWidth, logY + 5, 1);
        shapeRenderer.rectLine(logX, logY + logHeight - 5, logX + 10, logY + logHeight, 1);
        shapeRenderer.rectLine(logX + logWidth - 10, logY + logHeight, logX + logWidth, logY + logHeight - 5, 1);
        shapeRenderer.end();

        // Draw text
        batch.begin();
        font.setColor(Color.WHITE);
        layout.setText(font, combatLog);
        font.draw(batch, combatLog, logX + (logWidth - layout.width) / 2, logY + 25);
        batch.end();
    }

    // Add this class to represent damage/healing popup numbers
    private class DamageNumber {
        private final String text;
        private final float x, startY;
        private float y;
        private float alpha;
        private final Color color;
        private float lifeTime;
        private final float MAX_LIFETIME = 1.5f;

        public DamageNumber(String text, float x, float y, Color color) {
            this.text = text;
            this.x = x;
            this.startY = y;
            this.y = y;
            this.alpha = 1.0f;
            this.color = new Color(color);
            this.lifeTime = 0f;
        }

        public boolean update(float delta) {
            lifeTime += delta;
            if (lifeTime > MAX_LIFETIME) return true;

            // Float upward
            y = startY + 80 * (lifeTime / MAX_LIFETIME);

            // Fade out
            if (lifeTime > MAX_LIFETIME * 0.5f) {
                alpha = 1.0f - (lifeTime - (MAX_LIFETIME * 0.5f)) / (MAX_LIFETIME * 0.5f);
            }

            return false;
        }

        public void render(SpriteBatch batch, BitmapFont font) {
            Color prevColor = new Color(font.getColor());
            font.setColor(color.r, color.g, color.b, alpha);
            layout.setText(font, text);
            font.draw(batch, text, x - layout.width / 2, y);
            font.setColor(prevColor);
        }
    }

    // Add this to class fields
    // Add this to class fields
    private Array<DamageNumber> damageNumbers = new Array<>();

    // Updated method to display damage numbers using world positions
    private void showDamageNumber(int amount, boolean onEnemy, boolean isHealing) {
        // Get the world position of the target
        Vector3 worldPos = onEnemy ? enemyWorldPos.cpy() : playerWorldPos.cpy();

        // Offset Y position to appear above character
        worldPos.y += 2.0f;

        // Convert world position to screen position
        Vector3 screenPos = environment3D.getCamera().project(new Vector3(worldPos));

        // Choose color based on damage type
        Color color;
        if (isHealing) {
            color = new Color(0.2f, 0.9f, 0.4f, 1.0f);  // Green for healing
            amount = Math.abs(amount);  // Make sure healing is positive
        } else {
            color = new Color(0.95f, 0.2f, 0.2f, 1.0f);  // Red for damage
        }

        String text = (isHealing ? "+" : "") + amount;
        damageNumbers.add(new DamageNumber(text, screenPos.x, screenPos.y, color));
    }

    // Update method to update damage numbers
    private void updateDamageNumbers(float delta) {
        for (int i = damageNumbers.size - 1; i >= 0; i--) {
            if (damageNumbers.get(i).update(delta)) {
                damageNumbers.removeIndex(i);
            }
        }
    }

    // Render method to draw damage numbers
    private void renderDamageNumbers() {
        batch.begin();
        for (DamageNumber number : damageNumbers) {
            number.render(batch, titleFont);
        }
        batch.end();
    }

    private void drawPauseMenu() {
        // Updated constants
        float menuWidth = 400;
        float menuHeight = 350; // Increased for tutorial button
        float menuX = (SCREEN_WIDTH - menuWidth) / 2;
        float menuY = (SCREEN_HEIGHT - menuHeight) / 2;

        float buttonWidth = 240;
        float buttonHeight = 40;

        float continueButtonX = menuX + (menuWidth - buttonWidth) / 2;
        float continueButtonY = menuY + 210;
        float tutorialButtonY = menuY + 160;
        float menuButtonY = menuY + 110;
        float quitButtonY = menuY + 60;

        // Draw overlay and menu background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 0, 0, 0.7f);
        shapeRenderer.rect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        shapeRenderer.setColor(0.1f, 0.1f, 0.15f, 0.95f);
        shapeRenderer.rect(menuX, menuY, menuWidth, menuHeight);

        // Draw border
        shapeRenderer.setColor(0.3f, 0.3f, 0.4f, 1);
        shapeRenderer.rect(menuX, menuY, menuWidth, 3);
        shapeRenderer.rect(menuX, menuY + menuHeight - 3, menuWidth, 3);
        shapeRenderer.rect(menuX, menuY, 3, menuHeight);
        shapeRenderer.rect(menuX + menuWidth - 3, menuY, 3, menuHeight);

        // Draw button backgrounds
        shapeRenderer.setColor(0.2f, 0.2f, 0.25f, 1f);
        shapeRenderer.rect(continueButtonX, continueButtonY, buttonWidth, buttonHeight);
        shapeRenderer.rect(continueButtonX, tutorialButtonY, buttonWidth, buttonHeight);
        shapeRenderer.rect(continueButtonX, menuButtonY, buttonWidth, buttonHeight);
        shapeRenderer.rect(continueButtonX, quitButtonY, buttonWidth, buttonHeight);
        shapeRenderer.end();

        // Draw text
        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "Tạm Dừng", menuX + 130, menuY + 300);

        font.draw(batch, "Tiếp tục - ESC", continueButtonX + 60, continueButtonY + 28);
        font.draw(batch, "Hướng dẫn - T", continueButtonX + 60, tutorialButtonY + 28);
        font.draw(batch, "Main Menu - M", continueButtonX + 60, menuButtonY + 28);
        font.draw(batch, "Thoát - Q", continueButtonX + 60, quitButtonY + 28);
        batch.end();
    }


    private String getTurnText() {
        switch (combatState) {
            case PLAYER_TURN:
                return isAnimating ? "Thực thi hành động..." : "Lượt của Người Chơi";
            case ENEMY_TURN:
                return isAnimating ? "Kẻ địch hành động..." : "Lượt của Kẻ Địch";
            case COMBAT_END:
                return "Chiến đấu Kết Thúc";
            default:
                return "";
        }
    }


    public void handlePlayerAction(int skillIdx) {
        // Play click sound
        if (gameController.getEffectManager() != null) {
            gameController.getEffectManager().playClickSound();
        }

        // Reset word effects
        showWordDisplay = false;
        isAnimating = true;
        isPlayerAction = true;
        animState = AnimationState.MOVE_TO_CENTER;
        animationTimer = 0;
        currentSkill = skillIdx;

        // Set textures and effects
        currentPlayerTexture = playerSkillTextures[skillIdx];

        if (skillIdx == 3) { // Heal
            currentEffectTexture = effectTextures[1];
            effectOnPlayer = true;
        } else if (skillIdx == 4) { // Defend
            currentEffectTexture = effectTextures[2];
            effectOnPlayer = true;
        } else { // Attack skills
            currentEffectTexture = effectTextures[0];
            effectOnPlayer = false;
        }
    }

    private void startEnemyTurn() {


        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                if (gameController.getEffectManager() != null) {
                    gameController.getEffectManager().playClickSound();
                }

                isAnimating = true;
                isPlayerAction = false;
                animState = AnimationState.MOVE_TO_CENTER;
                animationTimer = 0;

                // Enemy AI
                enemyAction = chooseEnemyAction();
                currentEnemyTexture = enemySkillTextures[enemyAction];

                if (enemyAction == 2) { // Heal
                    currentEffectTexture = effectTextures[1];
                    effectOnPlayer = false;
                } else { // Attack
                    currentEffectTexture = effectTextures[0];
                    effectOnPlayer = true;
                }
            }
        }, 1.0f);

    }

    private int chooseEnemyAction() {
        //randomly choose an action based on enemy mana and HP
        if (enemyMana >= 12 && enemyHP < enemyMaxHP * 0.5f) {
            return 2; // Heal
        } else if (enemyMana >= 8 && enemyHP < enemyMaxHP * 0.3f) {
            return MathUtils.random(0, 1); // Attack or Special
        } else {
            return MathUtils.random(0, 1); // Attack or Special
        }
    }

    @Override
    public void resize(int width, int height) {
        camera.setToOrtho(false, width, height);
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
        batch.dispose();
        font.dispose();
        shapeRenderer.dispose();

        if (actionAnimations != null) {
            for (Animation<TextureRegion> animation : actionAnimations) {
                if (animation != null && animation.getKeyFrame(0) != null) {
                    animation.getKeyFrame(0).getTexture().dispose();
                }
            }
        }
        for (Texture texture : playerIdleTextures) {
            if (texture != null) texture.dispose();
        }
        for (Texture texture : playerSkillTextures) {
            if (texture != null) texture.dispose();
        }
        for (Texture texture : enemyIdleTextures) {
            if (texture != null) texture.dispose();
        }
        for (Texture texture : enemySkillTextures) {
            if (texture != null) texture.dispose();
        }
        for (Texture texture : effectTextures) {
            if (texture != null) texture.dispose();
        }
        if (backgroundTexture != null) backgroundTexture.dispose();

        if (backgroundBlurTexture != null) backgroundBlurTexture.dispose();
        if (titleFont != null) titleFont.dispose();
        if (inputFont != null) inputFont.dispose();

        // Dispose all cached enemy textures and clear the caches
        for (Texture[] textures : enemyIdleTexturesCache.values()) {
            for (Texture texture : textures) {
                if (texture != null) texture.dispose();
            }
        }
        for (Texture[] textures : enemySkillTexturesCache.values()) {
            for (Texture texture : textures) {
                if (texture != null) texture.dispose();
            }
        }


        // Clear the cache maps
        enemyIdleTexturesCache.clear();
        enemySkillTexturesCache.clear();
        environment3D.dispose();
    }

    // Getter and setter methods for DarkestDungeon class
    public GameController getGameController() {
        return gameController;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void setPaused(boolean paused) {
        this.isPaused = paused;
    }

    public boolean isShowTutorial() {
        return showTutorial;
    }

    public void setShowTutorial(boolean showTutorial) {
        this.showTutorial = showTutorial;
    }

    public int getCurrentTutorialPage() {
        return currentTutorialPage;
    }

    public void setCurrentTutorialPage(int page) {
        this.currentTutorialPage = page;
    }

    public boolean isVictory() {
        return victory;
    }

    public boolean isDefeated() {
        return defeated;
    }

    public boolean isEnded() {
        return isEnded;
    }

    public boolean isWaitingForInput() {
        return waitingForInput;
    }

    public boolean isShowInputField() {
        return showInputField;
    }

    public String getInputWord() {
        return inputWord;
    }

    public void setInputWord(String word) {
        this.inputWord = word;
    }

    public TutorialRenderer getTutorialRenderer() {
        return tutorialRenderer;
    }

    public Rectangle getContinueButtonBounds() {
        return continueButtonBounds;
    }

    public Items getItem() {
        return item;
    }

    public Reward getReward() {
        return reward;
    }

    public int getPlayerHP() {
        return playerHP;
    }

    public int getPlayerMana() {
        return playerMana;
    }

    public String getEnemyName() {
        return enemyName;
    }

    public int getNewLevel() {
        return newLevel;
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    public IsometricGame getGame() {
        return game;
    }

    public DarkestDungeon.CombatState getCombatState() {
        return combatState;
    }

    public boolean isAnimating() {
        return isAnimating;
    }

    public float getMenuX() {
        return menuX;
    }

    public float getMenuWidth() {
        return menuWidth;
    }

    public float getButtonY() {
        return buttonY;
    }

    public float getButtonHeight() {
        return buttonHeight;
    }

    public float getButtonSpacing() {
        return buttonSpacing;
    }

    public boolean isSkillEnabled(int index) {
        return skillEnabled[index];
    }

    public void setMousePosition(float x, float y) {
        this.mouseX = x;
        this.mouseY = y;
    }
}