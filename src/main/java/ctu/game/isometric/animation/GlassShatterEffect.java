package ctu.game.isometric.animation;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;

public class GlassShatterEffect extends ImpactEffect {
    private static final int PIECES = 12;
    float[] px = new float[PIECES], py = new float[PIECES], vx = new float[PIECES], vy = new float[PIECES], angle = new float[PIECES], va = new float[PIECES];
    float alpha = 1f, cx, cy, time = 0f;
    Texture tex;
    TextureRegion region;

    public GlassShatterEffect(float x, float y, Texture tex) {
        this.cx = x; this.cy = y; this.tex = tex;
        for (int i = 0; i < PIECES; ++i) {
            float angleRad = (float)(i * Math.PI * 2 / PIECES) + (float)(Math.random() * 0.4f - 0.2f);
            float speed = 110 + (float)Math.random()*80;
            vx[i] = (float)Math.cos(angleRad) * speed;
            vy[i] = (float)Math.sin(angleRad) * speed - MathUtils.random(30f);
            px[i] = cx; py[i] = cy;
            angle[i] = MathUtils.random(0f, 360f);
            va[i] = MathUtils.random(-240f, 240f); // angular velocity
            this.region = new TextureRegion(tex);
        }
    }
    @Override
    public void update(float delta) {
        time += delta;
        alpha = 1f - time * 1.8f;
        for (int i = 0; i < PIECES; ++i) {
            px[i] += vx[i] * delta;
            py[i] += vy[i] * delta;
            vy[i] += 230 * delta; // gravity
            angle[i] += va[i] * delta;
        }
        if (alpha <= 0f) finished = true;
    }
    @Override
    public void render(SpriteBatch batch) {
        batch.setColor(0.9f,0.98f,1f,Math.max(0,alpha));
        for (int i = 0; i < PIECES; ++i)
            batch.draw(region, px[i], py[i], 5, 5, 10, 10, 1, 1, angle[i]);
        batch.setColor(1,1,1,1);
    }
}