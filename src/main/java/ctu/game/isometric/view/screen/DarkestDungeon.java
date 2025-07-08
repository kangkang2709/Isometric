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
    private int playerHP = 4, playerMaxHP = 60;
    private int playerMana = 25, playerMaxMana = 50;
    private int playerATK = 15, playerDEF = 8;
    private int enemyHP = 4, enemyMaxHP = 40;
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
    private final float MAX_SCALE = 1.5f;

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
    private Texture[] enemyIdleTextures = new Texture[2]; // For idle animation
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
        this.currentPlayerTexture = playerIdleTextures[0];
        this.currentEnemyTexture = enemyIdleTextures[0];
        this.currentEffectTexture = null;
        this.showEffect = false;
        this.effectOnPlayer = false;
        this.currentSkill = -1;
        this.enemyAction = -1;
        this.playerCurrentX = playerStartX;
        this.playerCurrentY = playerStartY;
        this.enemyCurrentX = enemyStartX;
        this.enemyCurrentY = enemyStartY;

        this.combatLog = "Combat begins! Choose your action.";

        this.victory = false;
        defeated = false;

        currentLevel = gameController.getCharacter().getLevel();
        newLevel = currentLevel;

        item = null;
        reward = null;

        inputWord = "";
    }

    int currentLevel = 0;
    int newLevel = 0;

    BitmapFont titleFont;

    @Override
    public void show() {


        batch = new SpriteBatch();
        font = generateVietNameseFont("GrenzeGotisch.ttf", 20);
        titleFont = generateVietNameseFont("GrenzeGotisch.ttf", 26);
        inputFont = generateVietNameseFont("ModernAntiqua-Regular.ttf", 20);
        shapeRenderer = new ShapeRenderer();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, SCREEN_WIDTH, SCREEN_HEIGHT);

        combatLog = "Combat begins! Choose your action.";

        loadTextures();

        currentPlayerTexture = playerIdleTextures[0];
        currentEnemyTexture = enemyIdleTextures[0];


        Gdx.input.setInputProcessor(new com.badlogic.gdx.InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    isPaused = !isPaused;
                    return true;
                }

                if (isPaused) {
                    if (keycode == Input.Keys.Q) {
                        Gdx.app.exit();
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
                if (victory) {
                    if (continueButtonBounds != null && continueButtonBounds.contains(screenX, screenY)) {
                        gameController.getCharacter().addItem(item, reward.getAmount());
                        gameController.getCharacter().setHealth(playerHP);
                        gameController.getCharacter().setMana(playerMana);
                        Timer.schedule(new Timer.Task() {
                            @Override
                            public void run() {
                                gameController.setCompletedEvent();
                                gameController.setState(GameState.EXPLORING);
                                gameController.getCharacter().setHealth(playerHP);
                                gameController.getCharacter().setMana(playerMana);
                                gameController.getMapRenderer().setZoomed(true);
                                gameController.setRenderCharacter(true);
                                game.changeScreen("GAME");

                                if (newLevel > currentLevel) gameController.showLevelUpNotification();
                            }
                        }, 0.5f);
                    }

                    return true;

                } else if (defeated) {
                    if (continueButtonBounds != null && continueButtonBounds.contains(screenX, screenY)) {
                        gameController.setState(GameState.MAIN_MENU);
                        game.changeScreen("GAME_OVER");
                        return true;
                    }

                    return true; // Ngăn xử lý các input khác khi đã thua
                } else {
                    if (isPaused) {
                        // Các giá trị này phải giống trong drawPauseMenu()
                        float menuWidth = 400;
                        float menuHeight = 300;
                        float menuX = (SCREEN_WIDTH - menuWidth) / 2;
                        float menuY = (SCREEN_HEIGHT - menuHeight) / 2;

                        float buttonWidth = 240;
                        float buttonHeight = 40;

                        float continueButtonX = menuX + (menuWidth - buttonWidth) / 2;
                        float continueButtonY = menuY + 160;

                        float menuButtonY = menuY + 110;
                        float quitButtonY = menuY + 60;

                        // Kiểm tra click vào "Continue"
                        if (screenX >= continueButtonX && screenX <= continueButtonX + buttonWidth &&
                                screenY >= continueButtonY && screenY <= continueButtonY + buttonHeight) {
                            isPaused = false;
                            return true;
                        }
                        if (screenX >= continueButtonX && screenX <= continueButtonX + buttonWidth &&
                                screenY >= menuButtonY && screenY <= menuButtonY + buttonHeight) {
                            gameController.setState(GameState.MAIN_MENU);
                            game.changeScreen("GAME");
                            return true;
                        }
                        // Kiểm tra click vào "Quit Game"
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
        });
// Add this to the InputAdapter in the show() method

    }

    int experience = 0;

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
        enemyIdleTextures[0] = new Texture("dungeon/enemy_idle1.png");
        enemyIdleTextures[1] = new Texture("dungeon/enemy_idle2.png");

        // Enemy skill textures
        enemySkillTextures[0] = new Texture("dungeon/enemy_attack.png");
        enemySkillTextures[1] = new Texture("dungeon/enemy_special.png");
        enemySkillTextures[2] = new Texture("dungeon/enemy_heal.png");

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

        backgroundTexture = new Texture("backgrounds/black.png");
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

            if (showInputField && !isPaused) {
                drawInputField();
            }
            if (isPaused) {
                drawPauseMenu();
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
        inputFont.draw(batch, "PRESS ENTER TO SUBMIT WORD", fieldX, fieldY + 70);
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
        titleFont.draw(batch, "VICTORY", panelX + 250, panelY + panelHeight - 40);

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
        font.draw(batch, "Continue", buttonX + 70, buttonY + 33);
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
        titleFont.draw(batch, "You have been defeated!", panelX + 210, panelY + panelHeight - 40);

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
        font.draw(batch, "Continue", buttonX + 70, buttonY + 33);
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

    private void updateCombat(float delta) {
        // Update skill availability
        updateWordDisplay(delta);
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
            if (combatEndDelayTimer >= 1.5f) { // Delay 1.5 seconds
                pendingCombatEnd = false;
                combatEndDelayTimer = 0f;

                if (playerHP <= 0) {
                    combatState = CombatState.COMBAT_END;
                    combatLog = "Defeat! You have fallen in battle.";
                    victory = false;
                    defeated = true;
                } else if (enemyHP <= 0) {
                    combatState = CombatState.COMBAT_END;
                    combatLog = "Victory! The enemy has been defeated!";
                    victory = true;
                    defeated = false;
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

    private void updateMoveToCenter(float progress) {
        float easeProgress = Interpolation.pow2Out.apply(progress);

        // Only move for attack skills, not heal/defend
        boolean shouldMove = isPlayerAction ?
                (currentSkill == 0 || currentSkill == 1 || currentSkill == 2) :
                (enemyAction == 0 || enemyAction == 1);

        if (shouldMove) {
            if (isPlayerAction) {
                // Player moves to center-left, enemy moves closer
                playerCurrentX = MathUtils.lerp(playerStartX, SCREEN_WIDTH * 0.4f, easeProgress);
                playerCurrentY = MathUtils.lerp(playerStartY, COMBAT_CENTER_Y, easeProgress);
                playerScale = MathUtils.lerp(1.0f, MAX_SCALE, easeProgress);

                enemyCurrentX = MathUtils.lerp(enemyStartX, SCREEN_WIDTH * 0.6f, easeProgress);
                enemyCurrentY = MathUtils.lerp(enemyStartY, COMBAT_CENTER_Y, easeProgress);
                enemyScale = MathUtils.lerp(1.0f, MAX_SCALE, easeProgress);
            } else {
                // Enemy moves to center-right, player moves closer
                enemyCurrentX = MathUtils.lerp(enemyStartX, SCREEN_WIDTH * 0.6f, easeProgress);
                enemyCurrentY = MathUtils.lerp(enemyStartY, COMBAT_CENTER_Y, easeProgress);
                enemyScale = MathUtils.lerp(1.0f, MAX_SCALE, easeProgress);

                playerCurrentX = MathUtils.lerp(playerStartX, SCREEN_WIDTH * 0.4f, easeProgress);
                playerCurrentY = MathUtils.lerp(playerStartY, COMBAT_CENTER_Y, easeProgress);
                playerScale = MathUtils.lerp(1.0f, MAX_SCALE, easeProgress);
            }
        }
    }

    private void updateSkillEffect(float progress) {
        // Add some shake effect during skill execution
        float shake = (float) Math.sin(progress * 20) * 1f;

        if (isPlayerAction) {
            playerCurrentX += shake;
        } else {
            enemyCurrentX += shake;
        }
    }

    private void updateMoveBack(float progress) {
        float easeProgress = Interpolation.pow2In.apply(progress);

        // Only move back for attack skills, not heal/defend
        boolean shouldMove = isPlayerAction ?
                (currentSkill == 0 || currentSkill == 1 || currentSkill == 2) :
                (enemyAction == 0 || enemyAction == 1);

        if (shouldMove) {
            // Move both characters back to original positions and reset scales
            playerCurrentX = MathUtils.lerp(SCREEN_WIDTH * 0.4f, playerStartX, easeProgress);
            playerCurrentY = MathUtils.lerp(COMBAT_CENTER_Y, playerStartY, easeProgress);
            playerScale = MathUtils.lerp(MAX_SCALE, 1.0f, easeProgress);

            enemyCurrentX = MathUtils.lerp(SCREEN_WIDTH * 0.6f, enemyStartX, easeProgress);
            enemyCurrentY = MathUtils.lerp(COMBAT_CENTER_Y, enemyStartY, easeProgress);
            enemyScale = MathUtils.lerp(MAX_SCALE, 1.0f, easeProgress);
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
                combatLog = "You attack for " + damage + " damage!";
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
                    combatLog = "Word '" + randomWord + "' deals " + wordDamage + " damage!";

                    // Show word display effect
                    displayWord = randomWord;
                    showWordDisplay = true;
                    wordDisplayTimer = 0f;
                    showDamageEffect = true;
                } else {
                    int basicDamage = MathUtils.random(playerATK - 2, playerATK + 2) - enemyDEF;
                    basicDamage = Math.max(1, basicDamage);
                    enemyHP = Math.max(0, enemyHP - basicDamage);
                    combatLog = "No learned words! Basic attack for " + basicDamage + " damage!";
                }
                break;
            case 2: // TypeW (Input word)
                playerMana -= 5;
                showInputField = true;
                waitingForInput = true;
                inputWord = "";
                combatLog = "   Type a word and press ENTER\nInvalid words will deal damage to you!";
                return;
            case 3: // Heal
                playerMana -= 10;
                int heal = MathUtils.random(15, 25);
                playerHP = Math.min(playerMaxHP, playerHP + heal);
                combatLog = "You heal for " + heal + " HP!";
                showHealEffect = true;
                break;
            case 4: // Defend
                playerMana = Math.min(playerMaxMana, playerMana + 3);
                playerDEF += 2;
                combatLog = "You defend and recover 3 mana! DEF increased!";
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
            combatLog = "'" + word + "' is valid! Deals " + wordDamage + " damage!";

            showDamageEffect = true;
            currentEffectTexture = effectTextures[0];
            effectOnPlayer = false;

            if (gameController.getCharacter().updateDict(word))
                gameController.getDictionaryView().addNewWord(word);
        } else {
            int selfDamage = MathUtils.random(3, 8);
            playerHP = Math.max(0, playerHP - selfDamage);
            combatLog = "'" + word + "' is invalid! You take " + selfDamage + " damage!";

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
                combatLog = enemyName + " attacks for " + damage + " damage!";
                break;
            case 1: // Special
                if (enemyMana >= 8) {
                    enemyMana -= 8;
                    int specialDamage = MathUtils.random(enemyATK + 5, enemyATK + 10) - playerDEF;
                    specialDamage = Math.max(1, specialDamage);
                    playerHP = Math.max(0, playerHP - specialDamage);
                    combatLog = enemyName + " uses special attack for " + specialDamage + " damage!";
                } else {
                    damage = MathUtils.random(enemyATK - 2, enemyATK + 2) - playerDEF;
                    damage = Math.max(1, damage);
                    playerHP = Math.max(0, playerHP - damage);
                    combatLog = enemyName + " attacks for " + damage + " damage!";
                }
                break;
            case 2: // Heal
                if (enemyMana >= 12) {
                    enemyMana -= 12;
                    int heal = MathUtils.random(10, 20);
                    enemyHP = Math.min(enemyMaxHP, enemyHP + heal);
                    combatLog = enemyName + " heals for " + heal + " HP!";
                } else {
                    damage = MathUtils.random(enemyATK - 2, enemyATK + 2) - playerDEF;
                    damage = Math.max(1, damage);
                    playerHP = Math.max(0, playerHP - damage);
                    combatLog = enemyName + " attacks for " + damage + " damage!";
                }
                break;
        }
    }

    private void drawBackground() {
        batch.begin();
        // Top half - combat area
        batch.draw(backgroundTexture, 0, BOTTOM_HALF_HEIGHT, SCREEN_WIDTH, TOP_HALF_HEIGHT);
        // Bottom half - UI area
        batch.setColor(0.1f, 0.1f, 0.15f, 1);
        batch.draw(backgroundTexture, 0, 0, SCREEN_WIDTH, BOTTOM_HALF_HEIGHT);
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
            if (!effectOnPlayer && !isPlayerAction && enemyAction == 0) {
                batch.draw(currentEffectTexture,
                        effectX - effectSize / 2, effectY + effectSize / 2,
                        effectSize / 2, -effectSize / 2,
                        effectSize, effectSize,
                        1, 1, 0,
                        0, 0, currentEffectTexture.getWidth(), currentEffectTexture.getHeight(),
                        false, true);
            } else {
                batch.draw(currentEffectTexture,
                        effectX - effectSize / 2, effectY - effectSize / 2,
                        effectSize, effectSize);
            }
        }

        batch.end();
    }

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
        titleFont.draw(batch, layout, turnX, 150);

        // Combat log – auto center
        layout.setText(inputFont, combatLog);
        float logX = (SCREEN_WIDTH - layout.width) / 2;
        inputFont.draw(batch, layout, logX, 50);

        batch.end();
    }


    private void drawPauseMenu() {
        // Constants
        float menuWidth = 400;
        float menuHeight = 300;
        float menuX = (SCREEN_WIDTH - menuWidth) / 2;
        float menuY = (SCREEN_HEIGHT - menuHeight) / 2;

        float buttonWidth = 240;
        float buttonHeight = 40;

        float continueButtonX = menuX + (menuWidth - buttonWidth) / 2;
        float continueButtonY = menuY + 160;

        float menuButtonY = menuY + 110;
        float quitButtonY = menuY + 60;

        // 1. Draw semi-transparent overlay
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 0, 0, 0.7f);
        shapeRenderer.rect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        // 2. Draw menu background
        shapeRenderer.setColor(0.1f, 0.1f, 0.15f, 0.95f);
        shapeRenderer.rect(menuX, menuY, menuWidth, menuHeight);

        // 3. Draw border
        shapeRenderer.setColor(0.3f, 0.3f, 0.4f, 1);
        shapeRenderer.rect(menuX, menuY, menuWidth, 3);
        shapeRenderer.rect(menuX, menuY + menuHeight - 3, menuWidth, 3);
        shapeRenderer.rect(menuX, menuY, 3, menuHeight);
        shapeRenderer.rect(menuX + menuWidth - 3, menuY, 3, menuHeight);

        // 4. Draw button backgrounds
        shapeRenderer.setColor(0.2f, 0.2f, 0.25f, 1f);
        shapeRenderer.rect(continueButtonX, continueButtonY, buttonWidth, buttonHeight);
        shapeRenderer.rect(continueButtonX, menuButtonY, buttonWidth, buttonHeight);
        shapeRenderer.rect(continueButtonX, quitButtonY, buttonWidth, buttonHeight);
        shapeRenderer.end();

        // 5. Draw text
        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "PAUSED", menuX + 130, menuY + 250);

        font.draw(batch, "Continue - ESC", continueButtonX + 60, continueButtonY + 28);
        font.draw(batch, "Main Menu - Q", continueButtonX + 60, menuButtonY + 28);
        font.draw(batch, "Quit Game - Q", continueButtonX + 60, quitButtonY + 28);
        batch.end();
    }


    private String getTurnText() {
        switch (combatState) {
            case PLAYER_TURN:
                return isAnimating ? "Executing Action..." : "Your Turn";
            case ENEMY_TURN:
                return isAnimating ? "Enemy Acting..." : "Enemy Turn";
            case COMBAT_END:
                return "Combat Ended";
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