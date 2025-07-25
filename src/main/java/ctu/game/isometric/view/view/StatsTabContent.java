package ctu.game.isometric.view.view;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import ctu.game.isometric.model.entity.Character;

class StatsTabContent {
    private Character character;
    private BitmapFont font;
    private GlyphLayout layout = new GlyphLayout();
    private Texture statBarTexture;

    private static final Color STAT_BAR_COLOR = new Color(0.12f, 0.65f, 0.89f, 1f);
    private static final Color STAT_TEXT_COLOR = new Color(0.9f, 0.9f, 0.9f, 1f);
    private static final Color STAT_NAME_COLOR = new Color(0.7f, 0.85f, 1f, 1f);

    private Map<String, Float> animatedStats = new HashMap<>();

    public StatsTabContent(Character character, BitmapFont font) {
        this.character = character;
        this.font = font;

        // Create a simple texture for stat bars
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        statBarTexture = new Texture(pixmap);
        pixmap.dispose();

        // Initialize stats
        updateCharacter(character);
    }

    public void updateCharacter( Character character) {
        this.character = character;
        // Initialize animated stats with current values
        animatedStats.put("strength", (float) character.getDamage());
        animatedStats.put("defense", (float) character.getDefend());
        // Add other stats as needed
    }

    public void render(SpriteBatch batch, Rectangle bounds) {
        float startY = bounds.y + bounds.height - 50;
        float startX = bounds.x + 40;
        float lineHeight = 36;
        float barMaxWidth = 240;

        // Draw section title
        font.setColor(STAT_NAME_COLOR);
        font.draw(batch, "CHARACTER STATISTICS", startX, startY);
        startY -= 40;

        // Draw each stat with a stylized bar
        drawStatBar(batch, "Strength", (int)character.getDamage(), 50, startX, startY, barMaxWidth);
        startY -= lineHeight;

        drawStatBar(batch, "Defense", (int)character.getDefend(), 50, startX, startY, barMaxWidth);
        startY -= lineHeight;

        // Examples of other stats (these would need real data from character model)
//        drawStatBar(batch, "Agility", 15, 50, startX, startY, barMaxWidth);
//        startY -= lineHeight;

        // Additional stats section
        startY -= 20;
        font.setColor(STAT_NAME_COLOR);
        font.draw(batch, "ADDITIONAL STATS", startX, startY);
        startY -= 40;

        // Additional stats as text
        String[] statLabels = {
                "Words Learned: " + character.getLearnedWords().size(),
                "Fallen Count: " + character.getEttempFlags().getOrDefault("fallen", 0),
                "Wrong Word: " + character.getEttempFlags().getOrDefault("wrongWord", 0),
        };

        for (String label : statLabels) {
            font.setColor(STAT_TEXT_COLOR);
            font.draw(batch, label, startX, startY);
            startY -= lineHeight;
        }
    }

    private void drawStatBar(SpriteBatch batch, String statName, int value, int maxValue,
                             float x, float y, float maxWidth) {
        // Calculate bar width based on stat value
        float percentage = (float) value / maxValue;
        float barWidth = percentage * maxWidth;

        // Draw stat name
        font.setColor(STAT_NAME_COLOR);
        layout.setText(font, statName);
        font.draw(batch, statName, x, y);

        // Draw bar background
        batch.setColor(0.2f, 0.2f, 0.2f, 0.5f);
        batch.draw(statBarTexture, x + 150, y - 14, maxWidth, 16);

        // Draw filled bar with color based on percentage
        batch.setColor(STAT_BAR_COLOR);
        batch.draw(statBarTexture, x + 150, y - 14, barWidth, 16);

        // Draw stat value
        font.setColor(STAT_TEXT_COLOR);
        String valueText = Integer.toString(value);
        layout.setText(font, valueText);
        font.draw(batch, valueText, x + 150 + maxWidth + 10, y);
    }

    public boolean handleClick(float screenX, float screenY, Rectangle bounds) {
        // No interactive elements in stats tab currently
        return false;
    }
    public void dispose() {
        if (statBarTexture != null) {
            statBarTexture.dispose();
        }
        // Dispose of other resources if needed
    }
}