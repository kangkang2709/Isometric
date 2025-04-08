package ctu.game.isometric.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Disposable;
import java.util.HashMap;
import java.util.Map;

public class AssetManager implements Disposable {
    private Map<String, Texture> textures;

    public AssetManager() {
        textures = new HashMap<>();
    }

    public void loadAssets() {
        // Load all necessary textures
        loadTexture("tiles/grass.png");
        loadTexture("characters/player.png");
        loadTexture("ui/dialog_box.png");


        loadTexture("ui/arrow_up.png");
        loadTexture("ui/arrow_down.png");
        loadTexture("ui/arrow_left.png");
        loadTexture("ui/arrow_right.png");
    }

    private void loadTexture(String path) {
        textures.put(path, new Texture(Gdx.files.internal(path)));
    }

    public Texture getTexture(String path) {
        return textures.get(path);
    }

    @Override
    public void dispose() {
        for (Texture texture : textures.values()) {
            texture.dispose();
        }
        textures.clear();
    }
}