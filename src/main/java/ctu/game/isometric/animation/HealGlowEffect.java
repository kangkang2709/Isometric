package ctu.game.isometric.animation;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class HealGlowEffect extends ImpactEffect {
    private float x, y, w, h;
    private float alpha = 1f;
    private float scale = 1f;
    private float maxScale = 1.5f;
    private float fadeSpeed = 1.5f;
    private float scaleSpeed = 0.5f;
    private float rotation = 0f;
    private float rotationSpeed = 30f; // degrees per second
    private Texture glowTexture;

    public HealGlowEffect(float x, float y, float w, float h, Texture glowTexture) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.glowTexture = glowTexture;
    }

    @Override
    public void update(float delta) {
        alpha -= delta * fadeSpeed;
        scale += delta * scaleSpeed;
        rotation += delta * rotationSpeed;
        if (alpha <= 0f) finished = true;
    }

    @Override
    public void render(SpriteBatch batch) {
        if (glowTexture == null || alpha <= 0f) return;

        batch.setColor(1f, 1f, 1f, Math.max(0, alpha));
        float centerX = x + w / 2;
        float centerY = y + h / 2;

        batch.draw(
                glowTexture,
                centerX, centerY,
                w / 2, h / 2, // origin
                w, h,
                scale, scale, // scale up
                rotation,
                0, 0,
                glowTexture.getWidth(), glowTexture.getHeight(),
                false, false
        );
        batch.setColor(1f, 1f, 1f, 1f);
    }
}
