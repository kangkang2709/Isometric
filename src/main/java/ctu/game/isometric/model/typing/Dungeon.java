
        package ctu.game.isometric.model.typing;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import ctu.game.isometric.model.entity.Character;
import ctu.game.isometric.model.game.Items;
import ctu.game.isometric.model.game.Reward;
import ctu.game.isometric.model.world.IsometricMap;
import ctu.game.isometric.util.AnimationManager;
import ctu.game.isometric.util.AssetManager;
import ctu.game.isometric.util.ItemLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class Dungeon {
    String name;
    String description;
    IsometricMap map;
    List<Items> items;
    List<EnemyDungeon> enemies;

    static AnimationManager animationManager;

    public static void setAnimationManager(AnimationManager animationManager) {
        Dungeon.animationManager = animationManager;
    }

    final int maxEnemies = 10;
    final int maxItems = 10;

    private int currentEnemyIndex = 0;

    boolean isActive;
    boolean isStarted;
    boolean isFailed;
    boolean isFinished;
    Reward reward;

    Texture enemyTexture;

    // Delay fields
    private float spawnDelay = 1.0f; // seconds
    private float spawnTimer = 0f;

    public Texture getEnemyTexture() {
        return enemyTexture;
    }

    public void setEnemyTexture(Texture enemyTexture) {
        this.enemyTexture = enemyTexture;
    }

    public void loadItems() {
        try {
            this.items = ItemLoader.getAllItemsWithout("N/A", "quest");
            if (this.items == null) {
                this.items = new ArrayList<>();
            }
            System.out.println("Items loaded: " + items.size());
        } catch (Exception e) {
            System.err.println("Error loading items: " + e.getMessage());
            this.items = new ArrayList<>();
        }
    }

    public void loadEnemy() {
        this.enemies = new ArrayList<>();
        for (int i = 0; i < maxEnemies; i++) {
            EnemyDungeon enemy = new EnemyDungeon(0, 0, i);
            enemies.add(enemy);
        }
    }

    public Dungeon(String name, String description, IsometricMap map) {
        this.name = name;
        this.description = description;
        this.map = map;
        this.items = new ArrayList<>();
        this.enemies = new ArrayList<>();
        loadItems();
        loadEnemy();
    }

    public float[] toIsometric(float x, float y) {
        float isoX = (x + y) * (64 / 2.0f);
        float isoY = (y - x) * (32 / 2.0f);
        return new float[]{isoX, isoY};
    }

    // In Dungeon.java, update the render method:
    public void render(SpriteBatch batch) {
        if (currentEnemyIndex < enemies.size()) {
            EnemyDungeon enemy = enemies.get(currentEnemyIndex);
            // Use smooth position for isometric conversion
            float[] isoCoords = toIsometric(enemy.getRenderX(), enemy.getRenderY());
            float drawX = isoCoords[0];
            float drawY = isoCoords[1];
            if (animationManager != null && enemy.getDirection() != null) {
                com.badlogic.gdx.graphics.g2d.Animation<com.badlogic.gdx.graphics.g2d.TextureRegion> anim = animationManager.getDungeonEnemyAnimation(enemy.getDirection());
                if (anim != null) {
                    com.badlogic.gdx.graphics.g2d.TextureRegion frame = anim.getKeyFrame(enemy.stateTime, true);
                    batch.draw(frame, drawX, drawY, 32, 32);
                } else {
                    batch.draw(enemyTexture, drawX, drawY, 32, 32);
                }
            } else {
                batch.draw(enemyTexture, drawX, drawY, 32, 32);
            }
        }
    }

    public void update(float delta, Character character) {
        if (isActive && currentEnemyIndex < enemies.size()) {
            EnemyDungeon enemy = enemies.get(currentEnemyIndex);
            enemy.stateTime += delta; // Update animation time
            if (!enemy.isActive) {
                spawnTimer += delta;
                if (spawnTimer >= spawnDelay) {
                    enemy.isActive = true;
                }
                return;
            }
            boolean[][] walkableTiles = map.getWalkableCache();
            enemy.moveToCharacterAI(character, walkableTiles, delta);
            if (enemy.isTouchCharacter(character)) {
                enemy.isActive = false;
                character.decreaseHealth(1);
                currentEnemyIndex++;
                if (currentEnemyIndex < enemies.size()) {
                    spawnEnemy(character);
                } else {
                    end();
                }
            }
        }
    }

    private void spawnEnemy(Character character) {
        if (currentEnemyIndex < enemies.size()) {
            EnemyDungeon enemy = enemies.get(currentEnemyIndex);
            boolean[][] walkableTiles = map.getWalkableCache();
            Random rand = new Random();
            Set<String> wordsSet = character.getLearnedWords();
            List<String> wordsList = new ArrayList<>(wordsSet);
            int width = walkableTiles[0].length;
            int height = walkableTiles.length;

            // Collect all valid spawn positions
            List<int[]> validPositions = new ArrayList<>();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (walkableTiles[y][x] && (x != character.getGridX() || y != character.getGridY())) {
                        validPositions.add(new int[]{x, y});
                    }
                }
            }

            // Pick a random valid position
            if (!validPositions.isEmpty()) {
                int[] pos = validPositions.get(rand.nextInt(validPositions.size()));
                enemy.x = pos[0];
                enemy.y = pos[1];
            } else {
                enemy.x = 0;
                enemy.y = 0;
            }

            enemy.setWord(!wordsList.isEmpty() ? wordsList.get(rand.nextInt(wordsList.size())) : "default");
            enemy.isActive = false; // Not active yet, wait for delay
            enemy.stateTime = 0f;   // Reset animation time
            spawnTimer = 0f; // Reset timer for delay
        }
    }

    public void end() {
        isActive = false;
        isFinished = true;
        // Any other end logic
    }

    public void start(Character character) {
        if (isActive) {
            isStarted = true;
            isFailed = false;
            isFinished = false;
            for (EnemyDungeon enemy : enemies) {
                enemy.isActive = false;
                enemy.stateTime = 0f;
            }
            currentEnemyIndex = 0;
            spawnEnemy(character);
        }
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}