package ctu.game.isometric.view.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import ctu.game.isometric.controller.GameController;
import ctu.game.isometric.util.FontGenerator;

/**
 * A class to display level up notifications on the screen
 */
public class LevelUpNotification {
    private static final float NOTIFICATION_DURATION = 3.0f; // 3 seconds
    private String notificationMessage = null;
    private float notificationTimer = 0f;
    private BitmapFont font;
    private GlyphLayout layout;
    private GameController gameController;

    public LevelUpNotification(GameController gameController) {
        this.gameController = gameController;
        this.font = FontGenerator.generateVietNameseFont("Tektur-Bold.ttf", 36);
        this.layout = new GlyphLayout();
        font.setColor(Color.YELLOW);
    }

    /**
     * Display a level up notification
     * @param level The new level reached
     */
    public void showLevelUp(int level) {
        notificationMessage = "LEVEL UP! You reached level " + level + "!";
        notificationTimer = NOTIFICATION_DURATION;
    }

    /**
     * Update the notification timer
     * @param delta Time elapsed since last frame
     */
    public void update(float delta) {
        if (notificationTimer > 0) {
            notificationTimer -= delta;
            if (notificationTimer <= 0) {
                notificationMessage = null; // Clear the message when time is up
            }
        }
    }

    /**
     * Render the level up notification
     * @param batch SpriteBatch for rendering
     */
    public void render(SpriteBatch batch) {
        if (notificationMessage != null) {
            layout.setText(font, notificationMessage);
            float x = (Gdx.graphics.getWidth() - layout.width) / 2;
            float y = (Gdx.graphics.getHeight() / 2) + 50;

            // Draw shadow for better visibility
            font.setColor(Color.BLACK);
            font.draw(batch, notificationMessage, x + 2, y - 2);

            // Draw actual text
            font.setColor(Color.YELLOW);
            font.draw(batch, notificationMessage, x, y);
        }
    }

    /**
     * Check if a notification is currently active
     * @return true if a notification is being displayed
     */
    public boolean isActive() {
        return notificationTimer > 0;
    }
}
