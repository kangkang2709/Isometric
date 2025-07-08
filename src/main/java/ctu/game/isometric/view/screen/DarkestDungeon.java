package ctu.game.isometric.view.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import ctu.game.isometric.IsometricGame;
import ctu.game.isometric.controller.GameController;
import ctu.game.isometric.model.entity.Character;
import ctu.game.isometric.util.AssetManager;

public class DarkestDungeon implements Screen {
    private SpriteBatch batch;
    private BitmapFont font;
    private ShapeRenderer shapeRenderer;
    private OrthographicCamera camera;

    // Combat state
    private enum CombatState {
        PLAYER_TURN, ENEMY_TURN, COMBAT_END, ANIMATING, TURN_TRANSITION
    }

    // Animation state for skill animations
    private enum AnimationState {
        IDLE, SKILL_START, SKILL_EFFECT, SKILL_END
    }

    private CombatState combatState = CombatState.PLAYER_TURN;
    private AnimationState animState = AnimationState.IDLE;
    private float animationTimer = 0;
    private float turnTransitionTimer = 0;
    private final float SKILL_ZOOM_DURATION = 0.6f;
    private final float SKILL_EFFECT_DURATION = 1.0f;
    private final float TURN_TRANSITION_DURATION = 1.2f;
    private String combatLog = "";

    // Character stats
    private int playerHP = 45, playerMaxHP = 60;
    private int playerMana = 25, playerMaxMana = 50;
    private int enemyHP = 40, enemyMaxHP = 40;
    private int enemyMana = 20, enemyMaxMana = 30;
    private String playerName = "Plague Doctor";
    private String enemyName = "Cactoid Vertephile";

    // Screen dimensions
    private final float SCREEN_WIDTH = 1280;
    private final float SCREEN_HEIGHT = 720;

    // UI Layout constants
    private final float TOP_BAR_HEIGHT = 60;
    private final float BOTTOM_HUD_HEIGHT = 200;
    private final float SIDE_MARGIN = 40;

    // Character info panels
    private final float INFO_PANEL_WIDTH = 280;
    private final float INFO_PANEL_HEIGHT = 160;
    private final float PLAYER_INFO_X = SIDE_MARGIN;
    private final float ENEMY_INFO_X = SCREEN_WIDTH - INFO_PANEL_WIDTH - SIDE_MARGIN;
    private final float INFO_PANEL_Y = 20;

    // Health/Mana bars
    private final float BAR_WIDTH = 240;
    private final float BAR_HEIGHT = 14;
    private final float MANA_BAR_HEIGHT = 12;

    // Skill bars
    private final float SKILL_BAR_WIDTH = 400;
    private final float SKILL_BAR_HEIGHT = 80;
    private final float PLAYER_SKILL_BAR_X = (SCREEN_WIDTH / 2) - SKILL_BAR_WIDTH - 20;
    private final float ENEMY_SKILL_BAR_X = (SCREEN_WIDTH / 2) + 20;
    private final float SKILL_BAR_Y = BOTTOM_HUD_HEIGHT - 100;

    private final float SKILL_BUTTON_SIZE = 64;
    private final float SKILL_BUTTON_SPACING = 12;

    // Skills configuration
    private String[] skillIcons = {"⚔️", "🔥", "⚡", "💉", "🛡️"};
    private String[] skillNames = {"Attack", "Flame", "Lightning", "Heal", "Defend"};
    private int[] skillManaCost = {0, 5, 5, 10, 0};
    private boolean[] skillEnabled = {true, true, true, true, true};

    // Enemy skills
    private String[] enemySkillIcons = {"⚔️", "🌊", "❤️"};
    private String[] enemySkillNames = {"Attack", "Special", "Heal"};
    private int[] enemySkillCost = {0, 8, 12};

    // Character positions
    private final float COMBAT_AREA_Y = TOP_BAR_HEIGHT;
    private final float COMBAT_AREA_HEIGHT = SCREEN_HEIGHT - TOP_BAR_HEIGHT - BOTTOM_HUD_HEIGHT;
    private final float COMBAT_CENTER_Y = COMBAT_AREA_Y + COMBAT_AREA_HEIGHT / 2;

    private float playerX = SCREEN_WIDTH * 0.3f;
    private float playerY = COMBAT_CENTER_Y;
    private float enemyX = SCREEN_WIDTH * 0.7f;
    private float enemyY = COMBAT_CENTER_Y;

    private final float CHAR_WIDTH = 100;
    private final float CHAR_HEIGHT = 180;

    // Camera zoom effect
    private float targetZoom = 1.0f;
    private float currentZoom = 1.0f;
    private final float ZOOM_SPEED = 3.0f;
    private final float SKILL_ZOOM_FACTOR = 0.7f;

    // Textures
    private Texture playerIdleTexture;
    private Texture[] playerSkillTextures = new Texture[5];
    private Texture enemyIdleTexture;
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

    IsometricGame game;
    GameController gameController;
    Character player;
    AssetManager assetManager;

    public DarkestDungeon(IsometricGame game, GameController gameController) {
        this.gameController = gameController;
        this.game = game;
        this.player = gameController.getCharacter();
        this.assetManager = game.getAssetManager();
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        font = new BitmapFont();
        shapeRenderer = new ShapeRenderer();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, SCREEN_WIDTH, SCREEN_HEIGHT);

        font.getData().setScale(1.2f);
        combatLog = "Combat begins! Choose your action.";

        loadTextures();

        currentPlayerTexture = playerIdleTexture;
        currentEnemyTexture = enemyIdleTexture;
    }

    private void loadTextures() {
        // Player textures
        playerIdleTexture = new Texture("dungeon/idle1.png");
        playerSkillTextures[0] = new Texture("dungeon/player_attack.png");
        playerSkillTextures[1] = new Texture("dungeon/player_flame.png");
        playerSkillTextures[2] = new Texture("dungeon/player_lightning.png");
        playerSkillTextures[3] = new Texture("dungeon/player_heal.png");
        playerSkillTextures[4] = new Texture("dungeon/player_defend.png");

        // Enemy textures
        enemyIdleTexture = new Texture("dungeon/enemy_idle.png");
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

    @Override
    public void render(float delta) {
        updateCombat(delta);

        Gdx.gl.glClearColor(0.05f, 0.05f, 0.08f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

        drawBackground();
        drawCharacters();
        drawUI();

        handleInput();
    }

    private void updateCombat(float delta) {
        animationTimer += delta;

        // Update skill availability based on mana
        for (int i = 0; i < skillManaCost.length; i++) {
            skillEnabled[i] = (playerMana >= skillManaCost[i]);
        }

        // Handle turn transitions
        if (combatState == CombatState.TURN_TRANSITION) {
            turnTransitionTimer += delta;
            if (turnTransitionTimer >= TURN_TRANSITION_DURATION) {
                turnTransitionTimer = 0;
                if (playerHP <= 0) {
                    combatState = CombatState.COMBAT_END;
                    combatLog = "Defeat! You have fallen in battle.";
                } else if (enemyHP <= 0) {
                    combatState = CombatState.COMBAT_END;
                    combatLog = "Victory! The enemy has been defeated!";
                } else {
                    combatState = (combatState == CombatState.PLAYER_TURN) ?
                            CombatState.ENEMY_TURN : CombatState.PLAYER_TURN;
                }
            }
            return;
        }

        // Handle skill animations
        if (animState == AnimationState.SKILL_START && animationTimer > SKILL_ZOOM_DURATION) {
            animState = AnimationState.SKILL_EFFECT;
            animationTimer = 0;
            showEffect = true;
            applySkillEffects();
        }

        if (animState == AnimationState.SKILL_EFFECT && animationTimer > SKILL_EFFECT_DURATION) {
            animState = AnimationState.SKILL_END;
            animationTimer = 0;
            showEffect = false;
        }

        if (animState == AnimationState.SKILL_END && animationTimer > SKILL_ZOOM_DURATION) {
            animState = AnimationState.IDLE;
            animationTimer = 0;
            currentPlayerTexture = playerIdleTexture;
            currentEnemyTexture = enemyIdleTexture;
            targetZoom = 1.0f;

            // Transition to next turn
            combatState = CombatState.TURN_TRANSITION;
            if (combatState == CombatState.ANIMATING) {
                startEnemyTurn();
            }
        }

        // Auto enemy turn
        if (combatState == CombatState.ENEMY_TURN && animState == AnimationState.IDLE) {
            startEnemyTurn();
        }
    }



    private void applySkillEffects() {
        if (combatState == CombatState.ANIMATING) {
            applyPlayerSkillEffects();
        } else if (combatState == CombatState.ENEMY_TURN) {
            applyEnemySkillEffects();
        }
    }

    private void applyPlayerSkillEffects() {
        switch (currentSkill) {
            case 0: // Attack
                int damage = MathUtils.random(8, 12);
                enemyHP = Math.max(0, enemyHP - damage);
                combatLog = "You attack for " + damage + " damage!";
                break;
            case 1: // Flame
                playerMana -= 5;
                int flameDamage = MathUtils.random(12, 18);
                enemyHP = Math.max(0, enemyHP - flameDamage);
                combatLog = "Flame spell deals " + flameDamage + " damage!";
                break;
            case 2: // Lightning
                playerMana -= 5;
                int lightningDamage = MathUtils.random(14, 20);
                enemyHP = Math.max(0, enemyHP - lightningDamage);
                combatLog = "Lightning bolt strikes for " + lightningDamage + " damage!";
                break;
            case 3: // Heal
                playerMana -= 10;
                int heal = MathUtils.random(15, 25);
                playerHP = Math.min(playerMaxHP, playerHP + heal);
                combatLog = "You heal for " + heal + " HP!";
                break;
            case 4: // Defend
                playerMana = Math.min(playerMaxMana, playerMana + 3);
                combatLog = "You defend and recover 3 mana!";
                break;
        }
    }

    private void applyEnemySkillEffects() {
        switch (enemyAction) {
            case 0: // Attack
                int damage = MathUtils.random(8, 14);
                playerHP = Math.max(0, playerHP - damage);
                combatLog = enemyName + " attacks for " + damage + " damage!";
                break;
            case 1: // Special
                if (enemyMana >= 8) {
                    enemyMana -= 8;
                    int specialDamage = MathUtils.random(15, 22);
                    playerHP = Math.max(0, playerHP - specialDamage);
                    combatLog = enemyName + " uses special attack for " + specialDamage + " damage!";
                } else {
                    // Fallback to normal attack
                    int fallbackDamage = MathUtils.random(6, 10);
                    playerHP = Math.max(0, playerHP - fallbackDamage);
                    combatLog = enemyName + " attacks for " + fallbackDamage + " damage!";
                }
                break;
            case 2: // Heal
                if (enemyMana >= 12) {
                    enemyMana -= 12;
                    int heal = MathUtils.random(12, 18);
                    enemyHP = Math.min(enemyMaxHP, enemyHP + heal);
                    combatLog = enemyName + " heals for " + heal + " HP!";
                } else {
                    enemyMana = Math.min(enemyMaxMana, enemyMana + 5);
                    combatLog = enemyName + " recovers mana!";
                }
                break;
        }
    }

    private void drawBackground() {
        batch.begin();
        batch.draw(backgroundTexture, 0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
        batch.end();
    }

    private void drawCharacters() {
        batch.begin();

        // Draw player
        batch.draw(currentPlayerTexture,
                playerX - CHAR_WIDTH/2, playerY - CHAR_HEIGHT/2,
                CHAR_WIDTH, CHAR_HEIGHT);

        // Draw enemy
        batch.draw(currentEnemyTexture,
                enemyX - CHAR_WIDTH/2, enemyY - CHAR_HEIGHT/2,
                CHAR_WIDTH, CHAR_HEIGHT);

        // Draw effects
        if (showEffect && currentEffectTexture != null) {
            float effectX = effectOnPlayer ? playerX : enemyX;
            float effectY = effectOnPlayer ? playerY : enemyY;
            batch.draw(currentEffectTexture,
                    effectX - 60, effectY - 60, 120, 120);
        }

        batch.end();
    }

    private void drawUI() {
        // Draw HUD background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.02f, 0.02f, 0.05f, 0.95f);
        shapeRenderer.rect(0, 0, SCREEN_WIDTH, BOTTOM_HUD_HEIGHT);
        shapeRenderer.setColor(0.02f, 0.02f, 0.05f, 0.95f);
        shapeRenderer.rect(0, SCREEN_HEIGHT - TOP_BAR_HEIGHT, SCREEN_WIDTH, TOP_BAR_HEIGHT);
        shapeRenderer.end();

        drawInfoPanels();
        drawSkillBars();
        drawText();
    }

    private void drawInfoPanels() {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Player info panel
        shapeRenderer.setColor(0.08f, 0.08f, 0.12f, 0.9f);
        shapeRenderer.rect(PLAYER_INFO_X, INFO_PANEL_Y, INFO_PANEL_WIDTH, INFO_PANEL_HEIGHT);
        shapeRenderer.setColor(0.3f, 0.15f, 0.15f, 0.8f);
        shapeRenderer.rect(PLAYER_INFO_X, INFO_PANEL_Y + INFO_PANEL_HEIGHT - 2, INFO_PANEL_WIDTH, 2);

        // Enemy info panel
        shapeRenderer.setColor(0.08f, 0.08f, 0.12f, 0.9f);
        shapeRenderer.rect(ENEMY_INFO_X, INFO_PANEL_Y, INFO_PANEL_WIDTH, INFO_PANEL_HEIGHT);
        shapeRenderer.setColor(0.3f, 0.15f, 0.15f, 0.8f);
        shapeRenderer.rect(ENEMY_INFO_X, INFO_PANEL_Y + INFO_PANEL_HEIGHT - 2, INFO_PANEL_WIDTH, 2);

        drawHealthManaBar();

        shapeRenderer.end();
    }

    private void drawHealthManaBar() {
        // Player bars
        float playerBarX = PLAYER_INFO_X + 20;
        float playerHPBarY = INFO_PANEL_Y + 80;
        float playerManaBarY = INFO_PANEL_Y + 50;

        // Player HP
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 1);
        shapeRenderer.rect(playerBarX, playerHPBarY, BAR_WIDTH, BAR_HEIGHT);
        shapeRenderer.setColor(0.8f, 0.2f, 0.2f, 1);
        shapeRenderer.rect(playerBarX, playerHPBarY,
                BAR_WIDTH * (float)playerHP / playerMaxHP, BAR_HEIGHT);

        // Player Mana
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 1);
        shapeRenderer.rect(playerBarX, playerManaBarY, BAR_WIDTH, MANA_BAR_HEIGHT);
        shapeRenderer.setColor(0.2f, 0.4f, 0.8f, 1);
        shapeRenderer.rect(playerBarX, playerManaBarY,
                BAR_WIDTH * (float)playerMana / playerMaxMana, MANA_BAR_HEIGHT);

        // Enemy bars
        float enemyBarX = ENEMY_INFO_X + 20;
        float enemyHPBarY = INFO_PANEL_Y + 80;
        float enemyManaBarY = INFO_PANEL_Y + 50;

        // Enemy HP
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 1);
        shapeRenderer.rect(enemyBarX, enemyHPBarY, BAR_WIDTH, BAR_HEIGHT);
        shapeRenderer.setColor(0.8f, 0.2f, 0.2f, 1);
        shapeRenderer.rect(enemyBarX, enemyHPBarY,
                BAR_WIDTH * (float)enemyHP / enemyMaxHP, BAR_HEIGHT);

        // Enemy Mana
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 1);
        shapeRenderer.rect(enemyBarX, enemyManaBarY, BAR_WIDTH, MANA_BAR_HEIGHT);
        shapeRenderer.setColor(0.2f, 0.4f, 0.8f, 1);
        shapeRenderer.rect(enemyBarX, enemyManaBarY,
                BAR_WIDTH * (float)enemyMana / enemyMaxMana, MANA_BAR_HEIGHT);
    }

    private void drawSkillBars() {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Player skill bar
        shapeRenderer.setColor(0.1f, 0.06f, 0.06f, 0.9f);
        shapeRenderer.rect(PLAYER_SKILL_BAR_X, SKILL_BAR_Y, SKILL_BAR_WIDTH, SKILL_BAR_HEIGHT);

        // Enemy skill bar
        shapeRenderer.setColor(0.06f, 0.06f, 0.1f, 0.9f);
        shapeRenderer.rect(ENEMY_SKILL_BAR_X, SKILL_BAR_Y, SKILL_BAR_WIDTH, SKILL_BAR_HEIGHT);

        // Player skill buttons
        for (int i = 0; i < 5; i++) {
            float skillX = PLAYER_SKILL_BAR_X + 20 + i * (SKILL_BUTTON_SIZE + SKILL_BUTTON_SPACING);
            float skillY = SKILL_BAR_Y + 8;

            shapeRenderer.setColor(skillEnabled[i] ?
                    new Color(0.15f, 0.1f, 0.1f, 1) :
                    new Color(0.05f, 0.05f, 0.05f, 1));
            shapeRenderer.rect(skillX, skillY, SKILL_BUTTON_SIZE, SKILL_BUTTON_SIZE);

            // Border
            shapeRenderer.setColor(skillEnabled[i] ?
                    new Color(0.4f, 0.25f, 0.25f, 1) :
                    new Color(0.2f, 0.2f, 0.2f, 1));
            shapeRenderer.rect(skillX, skillY, SKILL_BUTTON_SIZE, 2);
            shapeRenderer.rect(skillX, skillY + SKILL_BUTTON_SIZE - 2, SKILL_BUTTON_SIZE, 2);
            shapeRenderer.rect(skillX, skillY, 2, SKILL_BUTTON_SIZE);
            shapeRenderer.rect(skillX + SKILL_BUTTON_SIZE - 2, skillY, 2, SKILL_BUTTON_SIZE);
        }

        // Enemy skill buttons (visual only)
        for (int i = 0; i < 3; i++) {
            float skillX = ENEMY_SKILL_BAR_X + 20 + i * (SKILL_BUTTON_SIZE + SKILL_BUTTON_SPACING);
            float skillY = SKILL_BAR_Y + 8;

            boolean canUse = (enemyMana >= enemySkillCost[i]);
            shapeRenderer.setColor(canUse ?
                    new Color(0.1f, 0.1f, 0.15f, 1) :
                    new Color(0.05f, 0.05f, 0.05f, 1));
            shapeRenderer.rect(skillX, skillY, SKILL_BUTTON_SIZE, SKILL_BUTTON_SIZE);

            // Border
            shapeRenderer.setColor(canUse ?
                    new Color(0.25f, 0.25f, 0.4f, 1) :
                    new Color(0.2f, 0.2f, 0.2f, 1));
            shapeRenderer.rect(skillX, skillY, SKILL_BUTTON_SIZE, 2);
            shapeRenderer.rect(skillX, skillY + SKILL_BUTTON_SIZE - 2, SKILL_BUTTON_SIZE, 2);
            shapeRenderer.rect(skillX, skillY, 2, SKILL_BUTTON_SIZE);
            shapeRenderer.rect(skillX + SKILL_BUTTON_SIZE - 2, skillY, 2, SKILL_BUTTON_SIZE);
        }

        shapeRenderer.end();
    }

    private void drawText() {
        batch.begin();

        // Turn indicator
        font.setColor(Color.ORANGE);
        font.getData().setScale(1.6f);
        String turnText = getTurnText();
        font.draw(batch, turnText, SCREEN_WIDTH / 2 - 80, SCREEN_HEIGHT - 20);

        // Character info
        font.setColor(Color.WHITE);
        font.getData().setScale(1.3f);
        font.draw(batch, playerName, PLAYER_INFO_X + 20, INFO_PANEL_Y + 140);
        font.draw(batch, enemyName, ENEMY_INFO_X + 20, INFO_PANEL_Y + 140);

        font.getData().setScale(1.0f);
        // Player stats
        font.draw(batch, "HP: " + playerHP + "/" + playerMaxHP, PLAYER_INFO_X + 20, INFO_PANEL_Y + 110);
        font.draw(batch, "Mana: " + playerMana + "/" + playerMaxMana, PLAYER_INFO_X + 20, INFO_PANEL_Y + 35);

        // Enemy stats
        font.draw(batch, "HP: " + enemyHP + "/" + enemyMaxHP, ENEMY_INFO_X + 20, INFO_PANEL_Y + 110);
        font.draw(batch, "Mana: " + enemyMana + "/" + enemyMaxMana, ENEMY_INFO_X + 20, INFO_PANEL_Y + 35);

        // Skill icons and costs
        drawSkillInfo();

        // Combat log
        font.setColor(Color.YELLOW);
        font.getData().setScale(1.2f);
        font.draw(batch, combatLog, SCREEN_WIDTH / 2 - 200, SCREEN_HEIGHT - 100);

        batch.end();
    }

    private void drawSkillInfo() {
        // Player skills
        for (int i = 0; i < 5; i++) {
            float skillX = PLAYER_SKILL_BAR_X + 20 + i * (SKILL_BUTTON_SIZE + SKILL_BUTTON_SPACING);
            float skillY = SKILL_BAR_Y + 8;

            font.setColor(skillEnabled[i] ? Color.WHITE : Color.GRAY);
            font.getData().setScale(1.8f);
            font.draw(batch, skillIcons[i], skillX + 18, skillY + 45);

            font.getData().setScale(0.8f);
            font.setColor(Color.LIGHT_GRAY);
            font.draw(batch, skillNames[i], skillX + 2, skillY - 2);

            if (skillManaCost[i] > 0) {
                font.setColor(Color.CYAN);
                font.draw(batch, "" + skillManaCost[i], skillX + 50, skillY + 15);
            }
        }

        // Enemy skills
        for (int i = 0; i < 3; i++) {
            float skillX = ENEMY_SKILL_BAR_X + 20 + i * (SKILL_BUTTON_SIZE + SKILL_BUTTON_SPACING);
            float skillY = SKILL_BAR_Y + 8;

            boolean canUse = (enemyMana >= enemySkillCost[i]);
            font.setColor(canUse ? Color.WHITE : Color.GRAY);
            font.getData().setScale(1.8f);
            font.draw(batch, enemySkillIcons[i], skillX + 18, skillY + 45);

            font.getData().setScale(0.8f);
            font.setColor(Color.LIGHT_GRAY);
            font.draw(batch, enemySkillNames[i], skillX + 2, skillY - 2);

            if (enemySkillCost[i] > 0) {
                font.setColor(Color.CYAN);
                font.draw(batch, "" + enemySkillCost[i], skillX + 50, skillY + 15);
            }
        }
    }

    private String getTurnText() {
        switch (combatState) {
            case PLAYER_TURN:
                return "Your Turn";
            case ENEMY_TURN:
                return "Enemy Turn";
            case ANIMATING:
                return "Executing Action...";
            case TURN_TRANSITION:
                return "Turn Changing...";
            case COMBAT_END:
                return "Combat Ended";
            default:
                return "";
        }
    }

    private void handleInput() {
        if (Gdx.input.justTouched() && combatState == CombatState.PLAYER_TURN) {
            Vector3 touchPos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(touchPos);

            for (int i = 0; i < 5; i++) {
                float skillX = PLAYER_SKILL_BAR_X + 20 + i * (SKILL_BUTTON_SIZE + SKILL_BUTTON_SPACING);
                float skillY = SKILL_BAR_Y + 8;

                if (skillEnabled[i] && touchPos.x >= skillX && touchPos.x <= skillX + SKILL_BUTTON_SIZE &&
                        touchPos.y >= skillY && touchPos.y <= skillY + SKILL_BUTTON_SIZE) {
                    handlePlayerAction(i);
                    break;
                }
            }
        } else if (Gdx.input.justTouched() && combatState == CombatState.COMBAT_END) {
            game.setScreen(new DarkestDungeon(game, gameController));
        }
    }

    private void handlePlayerAction(int skillIdx) {
        combatState = CombatState.ANIMATING;
        animState = AnimationState.SKILL_START;
        animationTimer = 0;
        currentSkill = skillIdx;
        targetZoom = SKILL_ZOOM_FACTOR;

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
        combatState = CombatState.ANIMATING;
        animState = AnimationState.SKILL_START;
        animationTimer = 0;
        targetZoom = SKILL_ZOOM_FACTOR;

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

    private int chooseEnemyAction() {
        // Simple AI with mana consideration
        if (enemyHP < enemyMaxHP * 0.3f && enemyMana >= 12) {
            return 2; // Heal
        } else if (enemyMana >= 8 && MathUtils.random() < 0.6f) {
            return 1; // Special attack
        } else {
            return 0; // Normal attack
        }
    }

    @Override
    public void resize(int width, int height) {
        camera.setToOrtho(false, width, height);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
        shapeRenderer.dispose();

        // Dispose textures
        if (playerIdleTexture != null) playerIdleTexture.dispose();
        if (enemyIdleTexture != null) enemyIdleTexture.dispose();
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