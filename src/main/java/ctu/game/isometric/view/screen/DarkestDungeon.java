package ctu.game.isometric.view.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Timer;
import ctu.game.isometric.IsometricGame;
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
    private enum CombatState {
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
    private boolean escKeyPressed = false;

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
    private final float SCREEN_WIDTH = 1280;
    private final float SCREEN_HEIGHT = 720;
    private final float TOP_HALF_HEIGHT = 360;
    private final float BOTTOM_HALF_HEIGHT = 360;

    // Combat area (top half)
    private final float COMBAT_AREA_Y = BOTTOM_HALF_HEIGHT;
    private final float COMBAT_CENTER_Y = COMBAT_AREA_Y + TOP_HALF_HEIGHT / 2;

    // Character positions
    private float playerStartX = SCREEN_WIDTH * 0.25f;
    private float playerStartY = COMBAT_CENTER_Y;
    private float enemyStartX = SCREEN_WIDTH * 0.75f;
    private float enemyStartY = COMBAT_CENTER_Y;

    // Current animated positions
    private float playerCurrentX = playerStartX;
    private float playerCurrentY = playerStartY;
    private float enemyCurrentX = enemyStartX;
    private float enemyCurrentY = enemyStartY;

    // Scale for character animation
    private float playerScale = 1.0f;
    private float enemyScale = 1.0f;
    private final float MAX_SCALE = 1.4f;

    private final float CHAR_WIDTH = 200;
    private final float CHAR_HEIGHT = 250;

    // Bottom UI layout
    private final float STATUS_PANEL_WIDTH = 300;
    private final float STATUS_PANEL_HEIGHT = 200;
    private final float PLAYER_STATUS_X = 50;
    private final float ENEMY_STATUS_X = SCREEN_WIDTH - STATUS_PANEL_WIDTH - 50;
    private final float STATUS_PANEL_Y = 50;

    // Skill bar layout
    private final float SKILL_BAR_WIDTH = 400;
    private final float SKILL_BAR_HEIGHT = 80;
    private final float SKILL_BAR_X = (SCREEN_WIDTH - SKILL_BAR_WIDTH) / 2;
    private final float SKILL_BAR_Y = 250;
    private final float SKILL_BUTTON_SIZE = 64;
    private final float SKILL_BUTTON_SPACING = 12;

    // Health/Mana bars
    private final float BAR_WIDTH = 200;
    private final float BAR_HEIGHT = 14;
    private final float MANA_BAR_HEIGHT = 12;

    // Skills configuration
    private String[] skillIcons = {"⚔️", "🔥", "⚡", "💉", "🛡️"};
    private String[] skillNames = {"Attack", "Word", "TypeW", "Heal", "Defend"};
    private int[] skillManaCost = {0, 5, 5, 10, 0};
    private boolean[] skillEnabled = {true, true, true, true, true};

    // Enemy skills
    private String[] enemySkillIcons = {"⚔️", "🌊", "❤️"};
    private String[] enemySkillNames = {"Attack", "Special", "Heal"};
    private int[] enemySkillCost = {0, 8, 12};

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

    public DarkestDungeon(IsometricGame game, GameController gameController) {
        this.gameController = gameController;
        this.game = game;
        this.player = gameController.getCharacter();
        this.assetManager = game.getAssetManager();
        this.wordNetValidator = gameController.getWordNetValidator();
    }

    int rewardId = 0;

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
        this.playerCurrentX = playerStartX;
        this.playerCurrentY = playerStartY;
        this.enemyCurrentX = enemyStartX;
        this.enemyCurrentY = enemyStartY;


        enemyIdleTextures = getEnemyIdleTextures(enemyName);
        enemySkillTextures = getEnemySkillTextures(enemyName);

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
    }

    int currentLevel = 0;
    int newLevel = 0;

    BitmapFont titleFont;

    private OrthographicCamera combatCamera;
    private float cameraShake = 0f;
    private float cameraZoom = 1f;
    private Vector2 cameraOffset = new Vector2();

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

        loadTextures();

        currentPlayerTexture = playerIdleTextures[0];
        currentEnemyTexture = enemyIdleTextures[0];


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
                    // Tutorial navigation
                    float tutorialWidth = 700;
                    float tutorialHeight = 500;
                    float tutorialX = (SCREEN_WIDTH - tutorialWidth) / 2;
                    float tutorialY = (SCREEN_HEIGHT - tutorialHeight) / 2;

                    float buttonWidth = 100;
                    float buttonHeight = 40;
                    float prevButtonX = tutorialX + 50;
                    float nextButtonX = tutorialX + tutorialWidth - 150;
                    float closeButtonX = tutorialX + tutorialWidth - 110;
                    float buttonY = tutorialY + 30;
                    float closeButtonY = tutorialY + tutorialHeight - 50;

                    // Previous page
                    if (currentTutorialPage > 0 &&
                            screenX >= prevButtonX && screenX <= prevButtonX + buttonWidth &&
                            screenY >= buttonY && screenY <= buttonY + buttonHeight) {
                        currentTutorialPage -= 2; // Skip title pages
                        if (currentTutorialPage < 0) currentTutorialPage = 0;
                        return true;
                    }

                    // Next page
                    if (currentTutorialPage < tutorialPages.length - 2 &&
                            screenX >= nextButtonX && screenX <= nextButtonX + buttonWidth &&
                            screenY >= buttonY && screenY <= buttonY + buttonHeight) {
                        currentTutorialPage += 2; // Skip title pages
                        return true;
                    }

                    // Close tutorial
                    if (screenX >= closeButtonX && screenX <= closeButtonX + buttonWidth &&
                            screenY >= closeButtonY && screenY <= closeButtonY + buttonHeight) {
                        showTutorial = false;
                        currentTutorialPage = 0;
                        return true;
                    }

                    return true; // Consume input while tutorial is open
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
                                gameController.getMapRenderer().setZoomed(true);
                                gameController.setRenderCharacter(true);
                                game.changeScreen("GAME");

                                if (newLevel > currentLevel) gameController.showLevelUpNotification();
                            }
                        }, 0.5f);
                        if (enemyName.equalsIgnoreCase("Demon"))
                            gameController.completedDungeon2();
                    }

                    return true;

                } else if (defeated) {

                    if (continueButtonBounds != null && continueButtonBounds.contains(screenX, screenY)) {
                        if (!isEnded) {
                            game.changeScreen("GAME");
                            gameController.setState(GameState.EXPLORING);
                            gameController.setPreviousState(GameState.EXPLORING);
                            gameController.returnToTower();
                            gameController.setRenderCharacter(true);
                            return true;
                        } else {
                            gameController.setState(GameState.MAIN_MENU);
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

                    if (combatState == CombatState.PLAYER_TURN && !isAnimating) {


                        for (int i = 0; i < 5; i++) {
                            float skillX = SKILL_BAR_X + 20 + i * (SKILL_BUTTON_SIZE + SKILL_BUTTON_SPACING);
                            float skillY = SKILL_BAR_Y + 8;

                            if (skillEnabled[i] &&
                                    screenX >= skillX && screenX <= skillX + SKILL_BUTTON_SIZE &&
                                    screenY >= skillY && screenY <= skillY + SKILL_BUTTON_SIZE) {
                                handlePlayerAction(i);
                                return true;
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
// Add this to the InputAdapter in the show() method

    }

    private void addCameraShake(float intensity, float duration) {
        cameraShake = Math.max(cameraShake, intensity);
        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                cameraShake = Math.max(0, cameraShake - intensity / 10);
            }
        }, 0, duration / 10, (int) (duration * 10));
    }

    private void updateTooltip() {
        hoveredSkill = -1;
        showTooltip = false;

        if (combatState == CombatState.PLAYER_TURN && !isAnimating && !isPaused) {
            for (int i = 0; i < 5; i++) {
                float skillX = SKILL_BAR_X + 20 + i * (SKILL_BUTTON_SIZE + SKILL_BUTTON_SPACING);
                float skillY = SKILL_BAR_Y + 8;

                if (mouseX >= skillX && mouseX <= skillX + SKILL_BUTTON_SIZE &&
                        mouseY >= skillY && mouseY <= skillY + SKILL_BUTTON_SIZE) {
                    hoveredSkill = i;
                    showTooltip = true;
                    break;
                }
            }
        }
    }

    private void drawTooltip() {
        if (!showTooltip || hoveredSkill == -1) return;

        String description = skillDescriptions[hoveredSkill];
        String manaCost = skillManaCost[hoveredSkill] > 0 ?
                "Năng lượng: " + skillManaCost[hoveredSkill] : "Không tốn năng lượng";

        // Calculate tooltip size
        layout.setText(font, description);
        float tooltipWidth = Math.max(layout.width + 20, 200);
        float tooltipHeight = layout.height + 40;

        // Position tooltip above skill button
        float tooltipX = mouseX - tooltipWidth / 2;
        float tooltipY = mouseY + 20;

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

    int experience = 0;

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


    private void loadTextures() {
        // Player idle animation textures
        playerIdleTextures[0] = new Texture("dungeon/idle1.png");
        playerIdleTextures[1] = new Texture("dungeon/idle2.png");

        // Player skill textures
        playerSkillTextures[0] = new Texture("dungeon/player_attack.png");
        playerSkillTextures[1] = new Texture("dungeon/player_flame.png");
        playerSkillTextures[2] = new Texture("dungeon/player_lightning.png");
        playerSkillTextures[3] = new Texture("dungeon/player_heal.png");
        playerSkillTextures[4] = new Texture("dungeon/player_defend.png");

        // Enemy idle animation textures
//        enemyIdleTextures[0] = new Texture("dungeon/enemy_1idle.png");
//        enemyIdleTextures[1] = new Texture("dungeon/enemy_2idle.png");
//        enemyIdleTextures[2] = new Texture("dungeon/enemy_3idle.png");
//        enemyIdleTextures[3] = new Texture("dungeon/enemy_3idle.png");
//
//        // Enemy skill textures
//        enemySkillTextures[0] = new Texture("dungeon/enemy_1skill.png");
//        enemySkillTextures[1] = new Texture("dungeon/enemy_2skill.png");
//        enemySkillTextures[2] = new Texture("dungeon/enemy_3skill.png");


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

    boolean defeated = false;

    @Override
    public void render(float delta) {
        if (victory) {
            renderReward(batch);
        } else if (defeated && !victory) {
            renderDefeat(batch);
        } else {
            if (!isPaused) {
                updateCombat(delta);
                updateIdleAnimations(delta);
            }

            Gdx.gl.glClearColor(0.05f, 0.05f, 0.08f, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

            camera.update();
            batch.setProjectionMatrix(camera.combined);
            shapeRenderer.setProjectionMatrix(camera.combined);

            drawBackground();
            drawCharacters();

            // Draw word display effects
            if (showWordDisplay && !isPaused) {
                drawWordDisplay();
            }

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


    private void drawInputField() {
        if (!showInputField) return;

        float fieldWidth = 300;
        float fieldHeight = 40;
        float fieldX = (SCREEN_WIDTH - fieldWidth) / 2 + 35;
        float fieldY = COMBAT_CENTER_Y - 50;

        inputFieldBounds = new Rectangle(fieldX, fieldY, fieldWidth, fieldHeight);

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
        float panelWidth = 600, panelHeight = 400;
        float panelX = (1280 - panelWidth) / 2;
        float panelY = (720 - panelHeight) / 2;

        // 1. Vẽ nền panel
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.8f); // Semi-transparent black
        shapeRenderer.rect(panelX, panelY, panelWidth, panelHeight);
        shapeRenderer.end();


        batch.begin();
        titleFont.setColor(Color.WHITE);
        titleFont.draw(batch, "CHIẾN THẮNG", panelX + 250, panelY + panelHeight - 40);

        // 3. Hiển thị hình ảnh và mô tả vật phẩm
        if (item != null) {
            Texture itemTexture = assetManager.loadTexture(item.getItemName(), item.getTexturePath());
            if (itemTexture != null) {
                batch.setColor(Color.WHITE);
                batch.draw(itemTexture, panelX + 100, panelY + panelHeight / 2 - 32, 64, 64);
            }
            font.setColor(Color.YELLOW);
            font.draw(batch, item.getItemName() + " x" + reward.getAmount(), panelX + 180, panelY + panelHeight / 2 + 30);
            font.draw(batch, reward.getDescription(), panelX + 180, panelY + panelHeight / 2);
        }

        // 4. Vẽ nút "Continue"
        float buttonWidth = 200, buttonHeight = 50;
        float buttonX = 1280 / 2 - buttonWidth / 2;
        float buttonY = panelY + 50;
        continueButtonBounds = new Rectangle(buttonX, buttonY, buttonWidth, buttonHeight);

        batch.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.2f, 0.2f, 0.3f, 1f); // Button background
        shapeRenderer.rect(buttonX, buttonY, buttonWidth, buttonHeight);
        shapeRenderer.end();

        // 5. Vẽ chữ trên nút
        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "Tiếp tục", buttonX + 70, buttonY + 33);
        batch.end();
    }

    public void renderDefeat(SpriteBatch batch) {
        float panelWidth = 600, panelHeight = 400;
        float panelX = (1280 - panelWidth) / 2;
        float panelY = (720 - panelHeight) / 2;

        // Draw defeat panel background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.8f); // Semi-transparent black
        shapeRenderer.rect(panelX, panelY, panelWidth, panelHeight);
        shapeRenderer.end();

        batch.begin();
        titleFont.setColor(Color.RED);
        titleFont.draw(batch, "Bạn đã bị hạ gục!", panelX + 210, panelY + panelHeight - 40);

        // Draw defeat message
        if (!isEnded) {
            font.setColor(Color.WHITE);
            font.draw(batch, "Bạn còn cơ hội, Cleric Klein đã đưa bạn về để chữa trị.", panelX + 100, panelY + panelHeight / 2 + 30);
        }
        // Draw continue button
        float buttonWidth = 200, buttonHeight = 50;
        float buttonX = 1280 / 2 - buttonWidth / 2;
        float buttonY = panelY + 50;
        continueButtonBounds = new Rectangle(buttonX, buttonY, buttonWidth, buttonHeight);

        batch.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.2f, 0.2f, 0.3f, 1f); // Button background
        shapeRenderer.rect(buttonX, buttonY, buttonWidth, buttonHeight);
        shapeRenderer.end();

        // Draw text on the button
        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "Tiếp tục", buttonX + 70, buttonY + 33);
        batch.end();
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
        }

        // Delay combat end if flagged
        if (pendingCombatEnd) {
            combatEndDelayTimer += delta;
            if (combatEndDelayTimer >= 2.0f) { // Delay 1.5 seconds
                pendingCombatEnd = false;
                combatEndDelayTimer = 0f;
                if (playerHP <= 0) {
                    gameController.getMusicController().playMusic("defeat");
                    combatState = CombatState.COMBAT_END;
                    combatLog = "THẤT BẠI! Bạn đã thua trong chiến đấu.";
                    victory = false;
                    defeated = true;

                    isEnded = gameController.getCharacter().gameOver();


                } else if (enemyHP <= 0) {
                    combatState = CombatState.COMBAT_END;
                    combatLog = "CHIẾN THẮNG! Kẻ địch đã bị hạ gục!";
                    gameController.getMusicController().playMusic("victory");
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
                    updateSkillEffect(progress / SKILL_EFFECT_DURATION);
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

                playerCurrentX = playerStartX + moveX + offsetX;
                playerCurrentY = MathUtils.lerp(playerStartY, COMBAT_CENTER_Y - 20, anticipationProgress);

                // Dynamic scaling with overshoot
                playerScale = MathUtils.lerp(1.0f, MAX_SCALE * 1.2f, anticipationProgress);

                // Enemy reacts
                enemyCurrentX = MathUtils.lerp(enemyStartX, SCREEN_WIDTH * 0.65f, anticipationProgress) - 100;
                enemyScale = MathUtils.lerp(1.0f, 0.9f, anticipationProgress); // Slightly shrink
            } else {
                // Dramatic zoom in during enemy action
                cameraZoom = MathUtils.lerp(1f, 0.8f, anticipationProgress);

                // Character moves with anticipation
                float moveX = anticipationProgress < 0.2f ? 30 * (anticipationProgress / 0.2f) : // Pull back
                        MathUtils.lerp(30, -150, (anticipationProgress - 0.2f) / 0.8f); // Rush forward

                enemyCurrentX = enemyStartX + moveX - 250;
                enemyCurrentY = MathUtils.lerp(enemyStartY, COMBAT_CENTER_Y - 20, anticipationProgress);

                // Dynamic scaling with overshoot
                enemyScale = MathUtils.lerp(1.0f, MAX_SCALE * 1.2f, anticipationProgress);

                // Player reacts
                playerCurrentX = MathUtils.lerp(playerStartX, SCREEN_WIDTH * 0.35f, anticipationProgress) + 100;
                playerScale = MathUtils.lerp(1.0f, 0.9f, anticipationProgress); // Slightly shrink
            }
        }
    }


    private void updateSkillEffect(float progress) {
        // Add some shake effect during skill execution
        float shake = (float) Math.sin(progress * 20) * 0.1f;

        if (isPlayerAction) {
            playerCurrentX += shake;
        } else {
            enemyCurrentX += shake;
        }
    }

    //    private void updateMoveBack(float progress) {
//        float easeProgress = Interpolation.pow2In.apply(progress);
//
//        // Only move back for attack skills, not heal/defend
//        boolean shouldMove = isPlayerAction ?
//                (currentSkill == 0 || currentSkill == 1 || currentSkill == 2) :
//                (enemyAction == 0 || enemyAction == 1);
//
//        if (shouldMove) {
//            // Move both characters back to original positions and reset scales
//            playerCurrentX = MathUtils.lerp(SCREEN_WIDTH * 0.4f, playerStartX, easeProgress);
//            playerCurrentY = MathUtils.lerp(COMBAT_CENTER_Y, playerStartY, easeProgress);
//            playerScale = MathUtils.lerp(MAX_SCALE, 1.0f, easeProgress);
//
//            enemyCurrentX = MathUtils.lerp(SCREEN_WIDTH * 0.6f, enemyStartX, easeProgress);
//            enemyCurrentY = MathUtils.lerp(COMBAT_CENTER_Y, enemyStartY, easeProgress);
//            enemyScale = MathUtils.lerp(MAX_SCALE, 1.0f, easeProgress);
//        }
//    }
    private void updateMoveBack(float progress) {
        // Only move back for attack skills, not heal/defend
        boolean shouldMove = isPlayerAction ?
                (currentSkill == 0 || currentSkill == 1 || currentSkill == 2) :
                (enemyAction == 0 || enemyAction == 1);

        if (shouldMove) {
            // Bỏ qua hiệu ứng di chuyển từ từ, set về gốc luôn
            playerCurrentX = playerStartX;
            playerCurrentY = playerStartY;
            playerScale = 1.0f;

            enemyCurrentX = enemyStartX;
            enemyCurrentY = enemyStartY;
            enemyScale = 1.0f;
        }
    }


    private void resetCharacterPositions() {
        playerCurrentX = playerStartX;
        playerCurrentY = playerStartY;
        playerScale = 1.0f;

        enemyCurrentX = enemyStartX;
        enemyCurrentY = enemyStartY;
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
    private boolean showDamageEffect = false;
    private boolean showHealEffect = false;

    private void applyPlayerSkillEffects() {
        switch (currentSkill) {
            case 0: // Attack
                int damage = MathUtils.random(playerATK - 2, playerATK + 2) - enemyDEF;
                damage = Math.max(1, damage);
                enemyHP = Math.max(0, enemyHP - damage);
                combatLog = "Bạn tấn công gây " + damage + " sát thương!";

                break;
            case 1: // Word (Random from learned words)
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
                    showDamageEffect = true;
                } else {
                    int basicDamage = MathUtils.random(playerATK - 2, playerATK + 2) - enemyDEF;
                    basicDamage = Math.max(1, basicDamage);
                    enemyHP = Math.max(0, enemyHP - basicDamage);
                    combatLog = "Không có từ nào! Tấn công thường " + basicDamage + " sát thương!";
                }
                break;
            case 2: // TypeW (Input word)
                playerMana -= 5;
                showInputField = true;
                waitingForInput = true;
                inputWord = "";
                combatLog = "Nhập một từ và nhấn ENTER\n Từ không hợp lệ sẽ gây sát thương (Phản sát thương) cho bạn!";
                return;
            case 3: // Heal
                playerMana -= 10;
                int heal = MathUtils.random(15, 25);
                playerHP = Math.min(playerMaxHP, playerHP + heal);
                combatLog = "Bạn hồi phục" + heal + " sinh lực!";
                showHealEffect = true;
                break;
            case 4: // Defend
                playerMana = Math.min(playerMaxMana, playerMana + 3);
                playerDEF += 2;
                combatLog = "Bạn phòng thủ và hồi phục 3 mana! Phòng thủ tăng lên!";
                break;
        }
    }

    private void updateWordDisplay(float delta) {
        if (showWordDisplay) {
            wordDisplayTimer += delta;
            if (wordDisplayTimer >= WORD_DISPLAY_DURATION) {
                showWordDisplay = false;
                showDamageEffect = false;
                showHealEffect = false;
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
        titleFont.draw(batch, layout, wordX - layout.width / 2, wordY);
        drawWordEffects(batch, wordX, wordY - 20, fadeAlpha);

        batch.end();

        // Draw particle effects

    }

    Texture damageParticleTex = new Texture("dungeon/damage_particle.png");

    private void drawWordEffects(SpriteBatch batch, float centerX, float centerY, float alpha) {

        if (!showDamageEffect && !showHealEffect) return;

        if (showDamageEffect && damageParticleTex != null) {
            float scale = 1.0f + 0.2f * (float) Math.sin(wordDisplayTimer * 8); // nhấp nháy to–nhỏ
            float size = 92 * scale;
            batch.setColor(1f, 0.3f, 0.1f, alpha * 0.8f);
            batch.draw(
                    damageParticleTex,
                    centerX - size / 2f - 25, centerY - size / 2f + 20,
                    size + 50, size
            );
        }


        batch.setColor(Color.WHITE); // Reset lại màu
    }


    private void processWordInput(String word) {
        displayWord = word;
        showWordDisplay = true;
        wordDisplayTimer = 0f;

        if (wordNetValidator.isValidWord(word)) {
            int wordScore = wordNetValidator.getTotalScore(word);
            int wordDamage = Math.max(1, wordScore + playerATK - enemyDEF);
            enemyHP = Math.max(0, enemyHP - wordDamage);
            combatLog = "'" + word + "' hợp lệ! Gây " + wordDamage + " sát thương!";

            showDamageEffect = true;
            currentEffectTexture = effectTextures[0];
            effectOnPlayer = false;

            if (gameController.getCharacter().updateDict(word))
                gameController.getDictionaryView().addNewWord(word);
        } else {
            int selfDamage = MathUtils.random(3, 8);
            playerHP = Math.max(0, playerHP - selfDamage);
            combatLog = "'" + word + "' không hợp lệ! Bạn nhận" + selfDamage + " sát thương!";

            showDamageEffect = true;
            currentEffectTexture = effectTextures[0];
            effectOnPlayer = true;
        }

        showInputField = false;
        waitingForInput = false;
        inputWord = "";
    }

    private void applyEnemySkillEffects() {
        switch (enemyAction) {
            case 0: // Attack
                int damage = MathUtils.random(enemyATK - 2, enemyATK + 2) - playerDEF;
                damage = Math.max(1, damage);
                playerHP = Math.max(0, playerHP - damage);
                combatLog = enemyName + " tấn công gây " + damage + " sát thương!";
                break;
            case 1: // Special
                if (enemyMana >= 8) {
                    enemyMana -= 8;
                    int specialDamage = MathUtils.random(enemyATK + 5, enemyATK + 10) - playerDEF;
                    specialDamage = Math.max(1, specialDamage);
                    playerHP = Math.max(0, playerHP - specialDamage);
                    combatLog = enemyName + " sử dụng kỹ năng gây " + specialDamage + " sát thương!";
                } else {
                    damage = MathUtils.random(enemyATK - 2, enemyATK + 2) - playerDEF;
                    damage = Math.max(1, damage);
                    playerHP = Math.max(0, playerHP - damage);
                    combatLog = enemyName + " tấn công gây " + damage + " sát thương!";
                }
                break;
            case 2: // Heal
                if (enemyMana >= 12) {
                    enemyMana -= 12;
                    int heal = MathUtils.random(10, 20);
                    enemyHP = Math.min(enemyMaxHP, enemyHP + heal);
                    combatLog = enemyName + " hồi phục được " + heal + " sinh lực!";
                } else {
                    damage = MathUtils.random(enemyATK - 2, enemyATK + 2) - playerDEF;
                    damage = Math.max(1, damage);
                    playerHP = Math.max(0, playerHP - damage);
                    combatLog = enemyName + " tấn công gây " + damage + " sát thương!";
                }
                break;
        }
    }

    Texture backgroundBlurTexture;

    private void drawBackground() {
        batch.begin();
        // Top half - combat area
        if (isAnimating)
            batch.draw(backgroundBlurTexture, 0, BOTTOM_HALF_HEIGHT, SCREEN_WIDTH, TOP_HALF_HEIGHT + 100);
        else
            batch.draw(backgroundTexture, 0, BOTTOM_HALF_HEIGHT, SCREEN_WIDTH, TOP_HALF_HEIGHT + 100);
        // Bottom half - UI area
        batch.setColor(0.1f, 0.1f, 0.15f, 1);
//        batch.draw(backgroundTexture, 0, 0, SCREEN_WIDTH, BOTTOM_HALF_HEIGHT);
        batch.setColor(Color.WHITE);
        batch.end();
    }

    private void drawCharacters() {
        batch.begin();

        // Draw player with animation
        float playerWidth = CHAR_WIDTH * playerScale;
        float playerHeight = CHAR_HEIGHT * playerScale;
        batch.draw(currentPlayerTexture,
                playerCurrentX - playerWidth / 2 - 150, playerCurrentY - playerHeight / 2,
                playerWidth, playerHeight);

        // Draw enemy with animation
        float enemyWidth = CHAR_WIDTH * enemyScale;
        float enemyHeight = CHAR_HEIGHT * enemyScale;
        batch.draw(currentEnemyTexture,
                enemyCurrentX - enemyWidth / 2 + 150, enemyCurrentY - enemyHeight / 2,
                enemyWidth, enemyHeight);

        // Draw effects
        if (showEffect && currentEffectTexture != null) {
            float effectX = effectOnPlayer ? playerCurrentX - 150 : enemyCurrentX + 150;
            float effectY = effectOnPlayer ? playerCurrentY : enemyCurrentY;
            float effectScale = effectOnPlayer ? playerScale : enemyScale;
            float effectSize = 120 * effectScale;

            // Flip Y for enemy attack effects
            if (effectOnPlayer && !isPlayerAction && enemyAction == 0) {
                batch.draw(currentEffectTexture,
                        effectX - effectSize / 2 + 40, effectY + effectSize / 2 - 140,
                        effectSize / 2, -effectSize / 2,
                        effectSize, effectSize,
                        1, 1, 0,
                        0, 0, currentEffectTexture.getWidth(), currentEffectTexture.getHeight(),
                        true, false);
            } else {
                batch.draw(currentEffectTexture,
                        effectX - effectSize / 2, effectY - effectSize / 2,
                        effectSize, effectSize);
            }
        }

        batch.end();
    }

    // Add these fields to the class
    private boolean showTutorial = false;
    private String[] tutorialPages = {
            "HƯỚNG DẪN CHIẾN ĐẤU - Trang 1/4",
            "KỸ NĂNG:\n" +
                    "-Attack - Đòn đánh vật lý cơ bản (Không tốn mana)\n" +
                    "-Word - Dùng từ đã học ngẫu nhiên để gây sát thương (5 mana)\n" +
                    "-TypeW - Gõ từ thủ công, nếu sai sẽ tự gây sát thương (5 mana)\n" +
                    "-Heal - Hồi máu (10 mana)\n" +
                    "-Defend - Tăng phòng thủ và hồi mana (Không tốn mana)",

            "HƯỚNG DẪN CHIẾN ĐẤU - Trang 2/4",
            "CƠ CHẾ CHIẾN ĐẤU:\n" +
                    "-Đây là hệ thống chiến đấu theo lượt: bạn và kẻ địch thay phiên nhau\n hành động.\n" +
                    "-Mỗi lượt, bạn chọn một kỹ năng để sử dụng.\n" +
                    "-Mana là năng lượng cần để dùng kỹ năng — khi cạn mana, bạn sẽ không\n thể dùng kỹ năng nữa.\n" +
                    "-Kẻ địch có hành vi khác nhau — có thể tấn công, phòng thủ, hoặc\n hồi máu. Hãy quan sát để chọn chiến thuật phù hợp.",

            "HƯỚNG DẪN CHIẾN ĐẤU - Trang 3/4",
            "CƠ CHẾ CHIẾN ĐẤU:\n" +
                    "-Sát thương gây ra = ATK (tấn công) - DEF (phòng thủ) của địch,tối thiểu là 1.\n" +
                    "-Kỹ năng 1 Word gây sát thương lớn: Word Score + ATK - DEF.\n" +
                    "-Word Score là điểm của từ trong từ điển bạn đã học — từ càng khó thì\n điểm càng cao.\n" +
                    "-Kỹ năng TypeW cho phép bạn gõ bất kỳ từ nào. Nếu đúng, sát thương\n rất mạnh. Nếu sai, bạn tự nhận sát thương.\n",

            "HƯỚNG DẪN CHIẾN ĐẤU - Trang 4/4",
            "MẸO:\n" +
                    "-Học từ mới để tăng sát thương kỹ năng Word.\n" +
                    "-TypeW rất mạnh nếu bạn gõ đúng, nhưng dễ gây hại nếu gõ sai.\n" +
                    "-Dùng Defend để hồi mana và tăng chỉ số phòng thủ.\n" +
                    "-Luôn chú ý lượng máu — Heal kịp lúc để tránh bị hạ gục.\n" +
                    "-Di chuột vào kỹ năng để xem chi tiết mô tả (tooltip).\n" +
                    "-Nhấn ESC để tạm dừng bất cứ lúc nào."
    };


    private int currentTutorialPage = 0;

    private void drawTutorial() {
        if (!showTutorial) return;

        float tutorialWidth = 700;
        float tutorialHeight = 500;
        float tutorialX = (SCREEN_WIDTH - tutorialWidth) / 2;
        float tutorialY = (SCREEN_HEIGHT - tutorialHeight) / 2;

        // Draw tutorial background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 0, 0, 0.8f);
        shapeRenderer.rect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        shapeRenderer.setColor(0.05f, 0.05f, 0.1f, 0.95f);
        shapeRenderer.rect(tutorialX, tutorialY, tutorialWidth, tutorialHeight);

        // Border
        shapeRenderer.setColor(0.4f, 0.4f, 0.6f, 1);
        shapeRenderer.rect(tutorialX, tutorialY, tutorialWidth, 3);
        shapeRenderer.rect(tutorialX, tutorialY + tutorialHeight - 3, tutorialWidth, 3);
        shapeRenderer.rect(tutorialX, tutorialY, 3, tutorialHeight);
        shapeRenderer.rect(tutorialX + tutorialWidth - 3, tutorialY, 3, tutorialHeight);

        // Navigation buttons
        float buttonWidth = 100;
        float buttonHeight = 40;
        float prevButtonX = tutorialX + 50;
        float nextButtonX = tutorialX + tutorialWidth - 150;
        float closeButtonX = tutorialX + tutorialWidth - 110;
        float buttonY = tutorialY + 30;
        float closeButtonY = tutorialY + tutorialHeight - 50;

        // Previous button (if not first page)
        if (currentTutorialPage > 0) {
            shapeRenderer.setColor(0.2f, 0.2f, 0.3f, 1f);
            shapeRenderer.rect(prevButtonX, buttonY, buttonWidth, buttonHeight);
        }

        // Next button (if not last page)
        if (currentTutorialPage < tutorialPages.length - 2) {
            shapeRenderer.setColor(0.2f, 0.2f, 0.3f, 1f);
            shapeRenderer.rect(nextButtonX, buttonY, buttonWidth, buttonHeight);
        }

        // Close button
        shapeRenderer.setColor(0.3f, 0.2f, 0.2f, 1f);
        shapeRenderer.rect(closeButtonX, closeButtonY, buttonWidth, buttonHeight);
        shapeRenderer.end();

        // Draw tutorial content
        batch.begin();

        // Title
        titleFont.setColor(Color.CYAN);
        titleFont.draw(batch, tutorialPages[currentTutorialPage],
                tutorialX + 50, tutorialY + tutorialHeight - 50);

        // Content
        inputFont.setColor(Color.WHITE);
        String content = tutorialPages[currentTutorialPage + 1];
        String[] lines = content.split("\n");

        float lineY = tutorialY + tutorialHeight - 100;
        for (String line : lines) {
            inputFont.draw(batch, line, tutorialX + 50, lineY);
            lineY -= 25;
        }

        // Navigation button text
        font.setColor(Color.WHITE);
        if (currentTutorialPage > 0) {
            font.draw(batch, "Lùi", prevButtonX + 20, buttonY + 25);
        }
        if (currentTutorialPage < tutorialPages.length - 2) {
            font.draw(batch, "Tiếp", nextButtonX + 35, buttonY + 25);
        }
        font.draw(batch, "Đóng", closeButtonX + 30, closeButtonY + 25);

        // Page indicator
        font.setColor(Color.LIGHT_GRAY);
        String pageInfo = "Trang " + ((currentTutorialPage / 2) + 1) + " of " + (tutorialPages.length / 2);
        font.draw(batch, pageInfo, tutorialX + tutorialWidth / 2 - 40, tutorialY + 20);

        batch.end();
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
        // Draw bottom UI background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.02f, 0.02f, 0.05f, 0.95f);
        shapeRenderer.rect(0, 0, SCREEN_WIDTH, BOTTOM_HALF_HEIGHT);
        shapeRenderer.end();

        drawStatusPanels();
        drawSkillBar();
        drawCombatLog();
    }

    private void drawStatusPanels() {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Player status panel
        shapeRenderer.setColor(0.08f, 0.08f, 0.12f, 0.9f);
        shapeRenderer.rect(PLAYER_STATUS_X, STATUS_PANEL_Y, STATUS_PANEL_WIDTH, STATUS_PANEL_HEIGHT);
        shapeRenderer.setColor(0.3f, 0.15f, 0.15f, 0.8f);
        shapeRenderer.rect(PLAYER_STATUS_X, STATUS_PANEL_Y + STATUS_PANEL_HEIGHT - 2, STATUS_PANEL_WIDTH, 2);

        // Enemy status panel
        shapeRenderer.setColor(0.08f, 0.08f, 0.12f, 0.9f);
        shapeRenderer.rect(ENEMY_STATUS_X, STATUS_PANEL_Y, STATUS_PANEL_WIDTH, STATUS_PANEL_HEIGHT);
        shapeRenderer.setColor(0.3f, 0.15f, 0.15f, 0.8f);
        shapeRenderer.rect(ENEMY_STATUS_X, STATUS_PANEL_Y + STATUS_PANEL_HEIGHT - 2, STATUS_PANEL_WIDTH, 2);

        drawStatusBars();

        shapeRenderer.end();

        // Draw status text
        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, playerName, PLAYER_STATUS_X + 20, STATUS_PANEL_Y + STATUS_PANEL_HEIGHT - 20);
        font.draw(batch, enemyName, ENEMY_STATUS_X + 20, STATUS_PANEL_Y + STATUS_PANEL_HEIGHT - 20);

        // Player detailed stats
        font.draw(batch, "HP: " + playerHP + "/" + playerMaxHP, PLAYER_STATUS_X + 20, STATUS_PANEL_Y + 150);
        font.draw(batch, "MP: " + playerMana + "/" + playerMaxMana, PLAYER_STATUS_X + 20, STATUS_PANEL_Y + 115);
        font.draw(batch, "ATK: " + playerATK, PLAYER_STATUS_X + 20, STATUS_PANEL_Y + 80);
        font.draw(batch, "DEF: " + playerDEF, PLAYER_STATUS_X + 20, STATUS_PANEL_Y + 45);

        // Enemy detailed stats
        font.draw(batch, "HP: " + enemyHP + "/" + enemyMaxHP, ENEMY_STATUS_X + 20, STATUS_PANEL_Y + 150);
        font.draw(batch, "MP: " + enemyMana + "/" + enemyMaxMana, ENEMY_STATUS_X + 20, STATUS_PANEL_Y + 115);
        font.draw(batch, "ATK: " + enemyATK, ENEMY_STATUS_X + 20, STATUS_PANEL_Y + 80);
        font.draw(batch, "DEF: " + enemyDEF, ENEMY_STATUS_X + 20, STATUS_PANEL_Y + 45);

        batch.end();
    }

    private void drawStatusBars() {
        // Player bars
        float playerBarX = PLAYER_STATUS_X + 120;
        float playerHPBarY = STATUS_PANEL_Y + 135;
        float playerManaBarY = STATUS_PANEL_Y + 100;

        // Player HP bar
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 1);
        shapeRenderer.rect(playerBarX, playerHPBarY, BAR_WIDTH, BAR_HEIGHT);
        shapeRenderer.setColor(0.8f, 0.2f, 0.2f, 1);
        shapeRenderer.rect(playerBarX, playerHPBarY,
                BAR_WIDTH * (float) playerHP / playerMaxHP, BAR_HEIGHT);

        // Player Mana bar
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 1);
        shapeRenderer.rect(playerBarX, playerManaBarY, BAR_WIDTH, MANA_BAR_HEIGHT);
        shapeRenderer.setColor(0.2f, 0.4f, 0.8f, 1);
        shapeRenderer.rect(playerBarX, playerManaBarY,
                BAR_WIDTH * (float) playerMana / playerMaxMana, MANA_BAR_HEIGHT);

        // Enemy bars
        float enemyBarX = ENEMY_STATUS_X + 120;
        float enemyHPBarY = STATUS_PANEL_Y + 135;
        float enemyManaBarY = STATUS_PANEL_Y + 100;

        // Enemy HP bar
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 1);
        shapeRenderer.rect(enemyBarX, enemyHPBarY, BAR_WIDTH, BAR_HEIGHT);
        shapeRenderer.setColor(0.8f, 0.2f, 0.2f, 1);
        shapeRenderer.rect(enemyBarX, enemyHPBarY,
                BAR_WIDTH * (float) enemyHP / enemyMaxHP, BAR_HEIGHT);

        // Enemy Mana bar
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 1);
        shapeRenderer.rect(enemyBarX, enemyManaBarY, BAR_WIDTH, MANA_BAR_HEIGHT);
        shapeRenderer.setColor(0.2f, 0.4f, 0.8f, 1);
        shapeRenderer.rect(enemyBarX, enemyManaBarY,
                BAR_WIDTH * (float) enemyMana / enemyMaxMana, MANA_BAR_HEIGHT);
    }

    private void drawSkillBar() {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Skill bar background
        shapeRenderer.setColor(0.1f, 0.06f, 0.06f, 0.9f);
        shapeRenderer.rect(SKILL_BAR_X, SKILL_BAR_Y, SKILL_BAR_WIDTH, SKILL_BAR_HEIGHT);

        // Skill buttons using skillButtonTextures
        for (int i = 0; i < 5; i++) {
            float skillX = SKILL_BAR_X + 20 + i * (SKILL_BUTTON_SIZE + SKILL_BUTTON_SPACING);
            float skillY = SKILL_BAR_Y + 8;

            // Button background
            boolean canUse = skillEnabled[i] && combatState == CombatState.PLAYER_TURN && !isAnimating;
            shapeRenderer.setColor(canUse ?
                    new Color(0.15f, 0.1f, 0.1f, 1) :
                    new Color(0.05f, 0.05f, 0.05f, 1));
            shapeRenderer.rect(skillX, skillY, SKILL_BUTTON_SIZE, SKILL_BUTTON_SIZE);

            // Button border
            shapeRenderer.setColor(canUse ?
                    new Color(0.4f, 0.25f, 0.25f, 1) :
                    new Color(0.2f, 0.2f, 0.2f, 1));
            shapeRenderer.rect(skillX, skillY, SKILL_BUTTON_SIZE, 2);
            shapeRenderer.rect(skillX, skillY + SKILL_BUTTON_SIZE - 2, SKILL_BUTTON_SIZE, 2);
            shapeRenderer.rect(skillX, skillY, 2, SKILL_BUTTON_SIZE);
            shapeRenderer.rect(skillX + SKILL_BUTTON_SIZE - 2, skillY, 2, SKILL_BUTTON_SIZE);
        }

        shapeRenderer.end();

        // Draw skill button textures and info
        batch.begin();
        for (int i = 0; i < 5; i++) {
            float skillX = SKILL_BAR_X + 20 + i * (SKILL_BUTTON_SIZE + SKILL_BUTTON_SPACING);
            float skillY = SKILL_BAR_Y + 8;

            boolean canUse = skillEnabled[i] && combatState == CombatState.PLAYER_TURN && !isAnimating;

            // Draw skill button texture
            if (skillButtonTextures[i] != null) {
                batch.setColor(canUse ? Color.WHITE : Color.GRAY);
                batch.draw(skillButtonTextures[i], skillX + 4, skillY + 4,
                        SKILL_BUTTON_SIZE - 8, SKILL_BUTTON_SIZE - 8);
                batch.setColor(Color.WHITE);
            }

            // Draw skill info
            font.setColor(Color.LIGHT_GRAY);
            font.draw(batch, skillNames[i], skillX + 2, skillY - 2);

            if (skillManaCost[i] > 0) {
                font.setColor(Color.CYAN);
                font.draw(batch, "" + skillManaCost[i], skillX + 50, skillY + 15);
            }
        }
        font.draw(batch, "Thanh kỹ năng", 607, 350);
        batch.end();
    }

    private final GlyphLayout layout = new GlyphLayout(); // Khai báo 1 lần trong class để tái sử dụng

    private void drawCombatLog() {
        batch.begin();

        font.setColor(Color.YELLOW);

        // Turn indicator – auto center
        String turnText = getTurnText();
        layout.setText(titleFont, turnText);
        float turnX = (SCREEN_WIDTH - layout.width) / 2;
        titleFont.setColor(Color.RED);
        titleFont.draw(batch, layout, turnX, 150);

        // Combat log – auto center
        layout.setText(inputFont, combatLog);
        float logX = (SCREEN_WIDTH - layout.width) / 2;
        inputFont.draw(batch, layout, logX, 50);

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


    private void handlePlayerAction(int skillIdx) {
        // Play click sound
        if (gameController.getEffectManager() != null) {
            gameController.getEffectManager().playClickSound();
        }

        // Reset word effects
        showWordDisplay = false;
        showDamageEffect = false;
        showHealEffect = false;

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

        for (Texture texture : playerIdleTextures) {
            if (texture != null) texture.dispose();
        }
        for (Texture texture : enemyIdleTextures) {
            if (texture != null) texture.dispose();
        }
        if (backgroundTexture != null) backgroundTexture.dispose();

        for (Texture texture : playerSkillTextures) {
            if (texture != null) texture.dispose();
        }
        for (Texture texture : enemySkillTextures) {
            if (texture != null) texture.dispose();
        }
        for (Texture texture : skillButtonTextures) {
            if (texture != null) texture.dispose();
        }
        for (Texture texture : effectTextures) {
            if (texture != null) texture.dispose();
        }
    }
}