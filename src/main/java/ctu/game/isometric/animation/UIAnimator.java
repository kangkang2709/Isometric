package ctu.game.isometric.animation;


import com.badlogic.gdx.math.Interpolation;

public class UIAnimator {

    public float lerpValue(float current, float target, float speed) {
        return current + (target - current) * Math.min(1.0f, speed);
    }

    public float animateValue(float progress, Interpolation interpolation) {
        return interpolation.apply(progress);
    }
}