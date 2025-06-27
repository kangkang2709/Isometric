package ctu.game.isometric.animation;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class GlassCrackEffect extends ImpactEffect {
    private float x, y, w, h, alpha = 1f, fadeSpeed = 2f;
    private Texture crackTexture;
    private float rotation;
    public GlassCrackEffect(float x, float y, float w, float h, Texture crackTexture, float rotation) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.crackTexture = crackTexture;
        this.rotation = rotation;
    }

    @Override
    public void update(float delta) {
        alpha -= delta * fadeSpeed; // Fade out
        if (alpha <= 0f) finished = true;
    }

    @Override
    public void render(SpriteBatch batch) {
        if (crackTexture == null || alpha <= 0f) return;
        batch.setColor(1f, 1f, 1f, Math.max(0, alpha));
        batch.draw(crackTexture, x + w/2, y + h/2, w/2, h/2, w, h, 1, 1, rotation, 0, 0, crackTexture.getWidth(), crackTexture.getHeight(), false, false);
        batch.setColor(1f, 1f, 1f, 1f);
    }
}