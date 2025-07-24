package ctu.game.isometric.view.scene;

import com.badlogic.gdx.graphics.Color;

public class FloatingText {
    private String text;
    private float x, y;
    private float alpha;
    private float duration;
    private float maxDuration;
    private Color color;
    private float velocityY;

    public FloatingText(String text, float x, float y, Color color, float duration) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.color = color.cpy();
        this.duration = duration;
        this.maxDuration = duration;
        this.alpha = 1.0f;
        this.velocityY = 30f; // Move upward
    }

    public void update(float delta) {
        duration -= delta;
        y += velocityY * delta;
        alpha = duration / maxDuration;
    }

    public boolean isFinished() {
        return duration <= 0;
    }

    // Getters
    public String getText() { return text; }
    public float getX() { return x; }
    public float getY() { return y; }
    public float getAlpha() { return alpha; }
    public Color getColor() { return color; }
}