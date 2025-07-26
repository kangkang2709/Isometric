package ctu.game.isometric.view.renderer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

public class BorderRenderer {
    private static final Color INNER_GLOW = new Color(0.2f, 0.6f, 0.8f, 0.9f);
    private static final Color OUTER_GLOW = new Color(0.2f, 0.6f, 0.8f, 0f);

    public static void drawBorder(ShapeRenderer renderer, Rectangle rect, float borderWidth) {
        // Inner border
        renderer.setColor(INNER_GLOW);
        drawHollowRect(renderer, rect, borderWidth);

        // Glow effect
        for (int i = 1; i <= 3; i++) {
            float alpha = 0.3f * (1f - (i / 4f));
            renderer.setColor(new Color(OUTER_GLOW.r, OUTER_GLOW.g, OUTER_GLOW.b, alpha));
            Rectangle glowRect = new Rectangle(
                    rect.x - i, rect.y - i,
                    rect.width + (i * 2), rect.height + (i * 2)
            );
            drawHollowRect(renderer, glowRect, borderWidth);
        }
    }

    private static void drawHollowRect(ShapeRenderer renderer, Rectangle rect, float borderWidth) {
        // Top
        renderer.rect(rect.x, rect.y + rect.height - borderWidth, rect.width, borderWidth);
        // Bottom
        renderer.rect(rect.x, rect.y, rect.width, borderWidth);
        // Left
        renderer.rect(rect.x, rect.y, borderWidth, rect.height);
        // Right
        renderer.rect(rect.x + rect.width - borderWidth, rect.y, borderWidth, rect.height);
    }
}