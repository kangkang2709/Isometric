package ctu.game.isometric.animation;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public abstract class ImpactEffect {
    boolean finished = false;
    abstract void update(float delta);
    abstract void render(SpriteBatch batch);
    boolean isFinished() { return finished; }
}