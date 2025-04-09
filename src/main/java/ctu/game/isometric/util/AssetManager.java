package ctu.game.isometric.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Disposable;
import java.util.HashMap;
import java.util.Map;

public class AssetManager implements Disposable {
    private Map<String, Texture> textures;
    private AnimationManager animationManager;

    public AssetManager() {
        textures = new HashMap<>();
        animationManager = new AnimationManager();
    }

    public void loadAssets() {
        // Load all necessary textures

        loadTexture("tiles/grass.png");
        loadTexture("characters/player.png");
        loadTexture("ui/dialog_box.png");

    }

    private void loadTexture(String path) {
        Texture texture = new Texture(Gdx.files.internal(path));
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        texture.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);

        textures.put(path,texture);

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
    }
}