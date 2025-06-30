package ctu.game.isometric.animation;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

public class ParticleEffectActor {
    private final ParticleEffect effect;
    private Vector2 position = new Vector2();

    public ParticleEffectActor(String effectFile, String imagesDir, float x, float y) {
        effect = new ParticleEffect();
        effect.load(Gdx.files.internal(effectFile), Gdx.files.internal(imagesDir));
        setPosition(x, y);
        effect.start();
    }

    public void setPosition(float x, float y) {
        position.set(x, y);
        effect.setPosition(x, y);
    }

    public void update(float delta) {
        effect.update(delta);
        if (effect.isComplete()) {
            effect.reset();
        }
    }

    public void render(SpriteBatch batch) {
        effect.draw(batch);
    }

    public void dispose() {
        effect.dispose();
    }
}