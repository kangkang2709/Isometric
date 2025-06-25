package ctu.game.isometric.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Disposable;
import ctu.game.isometric.model.entity.Enemy;
import ctu.game.isometric.model.game.Items;
import edu.mit.jwi.item.IItem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AssetManager implements Disposable {
    private Map<String, Texture> textures;
    private AnimationManager animationManager;

    public AssetManager() {
        textures = new HashMap<>();
        animationManager = new AnimationManager();
    }

    public Map<String, Texture> getTextures() {
        return textures;
    }

    public void setTextures(Map<String, Texture> textures) {
        this.textures = textures;
    }

    public void loadAssets() {
        // Load all necessary textures

        loadTexture("characters/idle.png");
        loadTexture("characters/walk.png");

        loadTexture("ui/dialog_box.png");
        loadTexture("new_run", "textures/new_run.png");
        loadTexture("enemy_hightlight", "textures/enemy_hightlight.png");
        loadTexture("quiz_hightlight", "textures/quiz_hightlight.png");
        loadTexture("item_hightlight", "textures/item_hightlight.png");
    }

    public Map<String, Texture> loadAllItems(List<Items> items) {
        for (Items item : items) {
            if (!textures.containsKey(item.getItemName())) {
                if (item.getTexturePath() != null) { // Ensure texture path is not null
                    Texture texture = loadTexture(item.getItemName(), item.getTexturePath());
                    texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                    textures.put(item.getItemName(), texture);
                }
            }
        }
        return textures; // Return all loaded item textures
    }

    public Map<String, Texture> loadAllEnemy(List<Enemy> enemies) {
        for (Enemy enemy : enemies) {
            if (!textures.containsKey(enemy.getEnemyName())) {
                if (enemy.getTexturePath() != null) { // Ensure texture path is not null
                    Texture texture = loadTexture(enemy.getEnemyName(), enemy.getTexturePath());
                    textures.put(enemy.getEnemyName(), texture);
                }
            }
        }
        return textures; // Return all loaded item textures
    }


    private Texture loadTexture(String name, String path) {
        Texture texture = new Texture(Gdx.files.internal(path));
        textures.put(name, texture);
        return texture;
    }

    private void loadTexture(String path) {
        Texture texture = new Texture(Gdx.files.internal(path));
        textures.put(path, texture);

    }

    public Texture getTexture(String path) {
        return textures.get(path);
    }

    public AnimationManager getAnimationManager() {
        return animationManager;
    }

    @Override
    public void dispose() {
        for (Texture texture : textures.values()) {
            texture.dispose();
        }
        textures.clear();
        if (animationManager != null) {
            animationManager.dispose();
        }
    }
}