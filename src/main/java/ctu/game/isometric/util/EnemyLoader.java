package ctu.game.isometric.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import ctu.game.isometric.model.entity.Enemy;

import java.util.HashMap;
import java.util.Map;

public class EnemyLoader {
    private static Map<Integer, Enemy> enemies = new HashMap<>();
    private static boolean initialized = false;

    public static void initialize() {
        if (initialized) return;

        try {
            JsonReader jsonReader = new JsonReader();
            JsonValue enemiesJson = jsonReader.parse(Gdx.files.internal("game/enemies.json"));

            for (JsonValue enemyJson : enemiesJson) {
                Enemy enemy = new Enemy();
                enemy.setEnemyID(enemyJson.getInt("enemyID"));
                enemy.setEnemyName(enemyJson.getString("enemyName"));
                enemy.setEnemyDescription(enemyJson.getString("enemyDescription"));
                enemy.setTexturePath(enemyJson.getString("texturePath"));
                enemy.setHealth(enemyJson.getInt("health"));
                enemy.setAttackPower(enemyJson.getInt("attackPower"));

                if (enemyJson.has("rewardID")) {
                    enemy.setRewardID(enemyJson.getInt("rewardID"));
                }

                enemies.put(enemy.getEnemyID(), enemy);
            }
            initialized = true;
        } catch (Exception e) {
            Gdx.app.error("EnemyLoader", "Error loading enemies", e);
        }
    }

    public static Enemy getEnemyById(int id) {
        if (!initialized) initialize();

        Enemy template = enemies.get(id);
        if (template == null) {
            return createDefaultEnemy();
        }

        // Return a copy of the enemy to prevent modifying the template
        Enemy enemy = new Enemy();
        enemy.setEnemyID(template.getEnemyID());
        enemy.setEnemyName(template.getEnemyName());
        enemy.setEnemyDescription(template.getEnemyDescription());
        enemy.setTexturePath(template.getTexturePath());
        enemy.setHealth(template.getHealth());
        enemy.setAttackPower(template.getAttackPower());
        enemy.setRewardID(template.getRewardID());

        return enemy;
    }

    private static Enemy createDefaultEnemy() {
        Enemy enemy = new Enemy();
        enemy.setEnemyID(0);
        enemy.setEnemyName("Unknown Enemy");
        enemy.setEnemyDescription("A mysterious creature.");
        enemy.setTexturePath("enemy/default.png");
        enemy.setHealth(50);
        enemy.setAttackPower(10);
        return enemy;
    }
}