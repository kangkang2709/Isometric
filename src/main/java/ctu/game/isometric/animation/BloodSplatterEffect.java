package ctu.game.isometric.animation;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

public class BloodSplatterEffect extends ImpactEffect {
    private static final int DROPS = 14;
    float[] px = new float[DROPS], py = new float[DROPS], vx = new float[DROPS], vy = new float[DROPS], size = new float[DROPS];
    float alpha = 1f, cx, cy, time = 0f;
    Texture tex;

    public BloodSplatterEffect(float x, float y, Texture tex) {
        this.cx = x; this.cy = y; this.tex = tex;
        for (int i = 0; i < DROPS; ++i) {
            float angle = (float)(i * Math.PI * 2 / DROPS) + (float)(Math.random()*0.6f - 0.3f);
            float speed = 70 + (float)Math.random()*50;
            vx[i] = (float)Math.cos(angle) * speed;
            vy[i] = (float)Math.sin(angle) * speed - MathUtils.random(30f);
            px[i] = cx; py[i] = cy;
            size[i] = MathUtils.random(5f, 9f);
        }
    }
    @Override
    public void update(float delta) {
        time += delta;
        alpha = 1f - time * 2.1f;
        for (int i = 0; i < DROPS; ++i) {
            px[i] += vx[i] * delta;
            py[i] += vy[i] * delta;
            vy[i] += 350 * delta;
        }
        if (alpha <= 0f) finished = true;
    }
    @Override
    public void render(SpriteBatch batch) {
        batch.setColor(0.8f,0,0,Math.max(0,alpha)); // Đỏ máu
        for (int i = 0; i < DROPS; ++i)
            batch.draw(tex, px[i], py[i], size[i], size[i]);
        batch.setColor(1,1,1,1);
    }
}