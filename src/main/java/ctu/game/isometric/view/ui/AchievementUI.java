package ctu.game.isometric.view.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import ctu.game.isometric.controller.AchievementManager;
import ctu.game.isometric.controller.GameController;
import ctu.game.isometric.model.entity.Character;
import ctu.game.isometric.model.game.Achievement;

import java.util.HashMap;
import java.util.Map;

import static ctu.game.isometric.util.FontGenerator.generateVietNameseFont;

public class AchievementUI {
    private final GameController gameController;
    private final AchievementManager achievementManager;
    private final Character character;

    private boolean active;
    private Viewport viewport;
    private BitmapFont titleFont, regularFont;
    private GlyphLayout layout;
    private Texture whiteTexture;
    private Texture panelTexture;
    private Texture closeButtonTexture;
    private Map<String, Texture> iconCache = new HashMap<>();

    private Rectangle closeButtonRect;
    private int currentPage = 0;
    private static final int ACHIEVEMENTS_PER_PAGE = 5;

    public AchievementUI(GameController gameController) {
        this.gameController = gameController;
        this.achievementManager = gameController.getAchievementManager();
        this.character = gameController.getCharacter();

        initializeUI();
    }

    private void initializeUI() {
        viewport = new FitViewport(1280, 720);
        titleFont = generateVietNameseFont("Tektur-Bold.ttf", 24);
        regularFont = generateVietNameseFont("Tektur-Bold.ttf", 16);
        layout = new GlyphLayout();

        // Create white texture for drawing colored rectangles
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        whiteTexture = new Texture(pixmap);
        pixmap.dispose();

        // Load UI textures
        panelTexture = new Texture(Gdx.files.internal("ui/message_box.png"));
        closeButtonTexture = new Texture(Gdx.files.internal("ui/close_button.png"));

        // Initialize close button
        closeButtonRect = new Rectangle(1180, 620, 50, 50);
    }

    public void render(SpriteBatch batch) {
        if (!active) return;

        viewport.apply();

        Matrix4 prevMatrix = batch.getProjectionMatrix().cpy();
        Color prevColor = batch.getColor().cpy();
        batch.setProjectionMatrix(viewport.getCamera().combined);

        // Save batch state
        boolean batchWasDrawing = batch.isDrawing();
        if (!batchWasDrawing) {
            batch.begin();
        }

        try {
            // Draw semi-transparent background
            batch.setColor(0f, 0f, 0f, 0.7f);
            batch.draw(whiteTexture, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());

            // Draw achievements panel
            float panelWidth = 800;
            float panelHeight = 600;
            float panelX = (viewport.getWorldWidth() - panelWidth) / 2;
            float panelY = (viewport.getWorldHeight() - panelHeight) / 2;

            batch.setColor(Color.WHITE);
            batch.draw(panelTexture, panelX - 20, panelY - 20, panelWidth + 40, panelHeight + 40);

            // Draw title
            drawCenteredText(batch, titleFont, "THÀNH TỰU", viewport.getWorldWidth() / 2, panelY + panelHeight - 10, new Color(1, 0.9f, 0.3f, 1));

            // Draw close button
            batch.draw(closeButtonTexture, closeButtonRect.x, closeButtonRect.y, closeButtonRect.width, closeButtonRect.height);

            // Get achievements
            Array<Achievement> achievements = achievementManager.getCharacterAchievements();

            if (achievements.size == 0) {
                drawCenteredText(batch, regularFont, "Bạn chưa có thành tựu nào!", viewport.getWorldWidth() / 2, panelY + panelHeight / 2, Color.WHITE);

                // End batch if we started it
                if (!batchWasDrawing) {
                    batch.end();
                }

                batch.setProjectionMatrix(prevMatrix);
                return;
            }

            // Calculate pagination
            int totalPages = (achievements.size + ACHIEVEMENTS_PER_PAGE - 1) / ACHIEVEMENTS_PER_PAGE;
            currentPage = Math.min(currentPage, totalPages - 1);

            // Draw achievements
            float startY = panelY + panelHeight - 120;
            float startX = panelX + 50;
            float achievementHeight = 90;
            float achievementWidth = panelWidth - 100;

            int startIndex = currentPage * ACHIEVEMENTS_PER_PAGE;
            int endIndex = Math.min(startIndex + ACHIEVEMENTS_PER_PAGE, achievements.size);

            for (int i = startIndex; i < endIndex; i++) {
                Achievement achievement = achievements.get(i);
                float achY = startY - (i - startIndex) * achievementHeight - 10;

                // Draw achievement background with different colors based on unlock status
                if (achievement.isUnlocked()) {
                    batch.setColor(0.2f, 0.4f, 0.2f, 0.8f);  // Green tint for unlocked
                } else {
                    batch.setColor(0.2f, 0.2f, 0.4f, 0.8f);  // Blue tint for locked
                }
                batch.draw(whiteTexture, startX, achY, achievementWidth, achievementHeight - 10);

                // Draw achievement border
                batch.setColor(0.8f, 0.7f, 0.2f, 1);
                drawRect(batch, startX, achY, achievementWidth, achievementHeight - 10, 2);

                // Draw achievement icon
                Texture iconTexture = getAchievementIcon(achievement.getIconPath());
                if (iconTexture != null) {
                    batch.setColor(Color.WHITE);
                    batch.draw(iconTexture, startX + 15, achY + 15, 60, 60);
                }

                // Draw achievement title
                regularFont.setColor(new Color(1, 0.9f, 0.3f, 1));
                regularFont.draw(batch, achievement.getTitle(), startX + 90, achY + achievementHeight - 25);

                // Draw achievement description
                regularFont.setColor(Color.WHITE);
                drawWrappedText(batch, regularFont, achievement.getDescription(), startX + 90, achY + achievementHeight - 50, achievementWidth - 110);

                // Draw unlock status
                String statusText = achievement.isUnlocked() ? "ĐÃ MỞ KHÓA" : "CHƯA MỞ KHÓA";
                Color statusColor = achievement.isUnlocked() ? new Color(0.3f, 1f, 0.3f, 1) : new Color(1f, 0.5f, 0.3f, 1);
                regularFont.setColor(statusColor);
                regularFont.draw(batch, statusText, startX + achievementWidth - 150, achY + achievementHeight - 25);

                // Draw progress if applicable
                if (achievement.getTargetValue() > 1) {
                    float progressWidth = achievementWidth - 120;
                    float progressHeight = 10;
                    float progressX = startX + 90;
                    float progressY = achY + 15;

                    // Progress background
                    batch.setColor(0.3f, 0.3f, 0.3f, 1);
                    batch.draw(whiteTexture, progressX, progressY - 7, progressWidth, progressHeight);

                    // Progress fill
                    float progressPercentage = (float) achievement.getCurrentValue() / achievement.getTargetValue();
                    batch.setColor(0.3f, 0.9f, 0.3f, 1);
                    batch.draw(whiteTexture, progressX, progressY - 7, progressWidth * progressPercentage, progressHeight);

                    // More descriptive progress text based on type
                    String progressDesc = "Tiến độ: ";
                    String progressText = progressDesc + achievement.getCurrentValue() + " / " + achievement.getTargetValue();
                    regularFont.setColor(Color.WHITE);
                    regularFont.draw(batch, progressText, progressX + 470, progressY + 25);
                }
            }

            // Draw pagination controls if needed
            if (totalPages > 1) {
                float paginationY = panelY + 30;

                // Previous page button
                if (currentPage > 0) {
                    regularFont.setColor(Color.WHITE);
                    Rectangle prevRect = new Rectangle(panelX + panelWidth / 2 - 100, paginationY, 80, 30);
                    drawButton(batch, prevRect, "<<");
                }

                // Page indicator
                drawCenteredText(batch, regularFont, (currentPage + 1) + "/" + totalPages,
                        viewport.getWorldWidth() / 2, paginationY + 20, Color.WHITE);

                // Next page button
                if (currentPage < totalPages - 1) {
                    regularFont.setColor(Color.WHITE);
                    Rectangle nextRect = new Rectangle(panelX + panelWidth / 2 + 20, paginationY, 80, 30);
                    drawButton(batch, nextRect, ">>");
                }
            }
        } finally {
            // Always restore original batch state before returning
            batch.setColor(prevColor);  // Restore original color

            if (!batchWasDrawing) {
                batch.end();  // Only end if we began it
            }

            batch.setProjectionMatrix(prevMatrix);  // Always restore matrix
        }
    }

    public boolean handleInput(int X, int screenY) {
        if (!active) return false;
        float Y = Gdx.graphics.getHeight() - screenY;


        // Check if close button is clicked
        if (closeButtonRect.contains(X, Y)) {
            active = false;
            return true;
        }

        // Handle pagination
        Array<Achievement> achievements = achievementManager.getCharacterAchievements();
        int totalPages = (achievements.size + ACHIEVEMENTS_PER_PAGE - 1) / ACHIEVEMENTS_PER_PAGE;

        float panelX = (viewport.getWorldWidth() - 800) / 2;
        float paginationY = (viewport.getWorldHeight() - 600) / 2 + 30;

        // Previous page button
        if (currentPage > 0) {
            Rectangle prevRect = new Rectangle(panelX + 400 - 100, paginationY, 80, 30);
            if (prevRect.contains(X, Y)) {
                currentPage--;
                return true;
            }
        }

        // Next page button
        if (currentPage < totalPages - 1) {
            Rectangle nextRect = new Rectangle(panelX + 400 + 20, paginationY, 80, 30);
            if (nextRect.contains(X, Y)) {
                currentPage++;
                return true;
            }
        }


        return false;
    }

    private Texture getAchievementIcon(String path) {
        if (path == null) return null;

        if (!iconCache.containsKey(path)) {
            try {
                iconCache.put(path, new Texture(Gdx.files.internal(path)));
            } catch (Exception e) {
                Gdx.app.error("AchievementUI", "Could not load icon: " + path);
                return null;
            }
        }
        return iconCache.get(path);
    }

    private void drawCenteredText(SpriteBatch batch, BitmapFont font, String text, float x, float y, Color color) {
        layout.setText(font, text);
        font.setColor(color);
        font.draw(batch, text, x - layout.width / 2, y);
    }

    private void drawRect(SpriteBatch batch, float x, float y, float width, float height, float thickness) {
        batch.draw(whiteTexture, x, y, width, thickness); // Bottom
        batch.draw(whiteTexture, x, y, thickness, height); // Left
        batch.draw(whiteTexture, x + width - thickness, y, thickness, height); // Right
        batch.draw(whiteTexture, x, y + height - thickness, width, thickness); // Top
    }

    private void drawWrappedText(SpriteBatch batch, BitmapFont font, String text, float x, float y, float maxWidth) {
        String[] lines = text.split("\n");
        float lineHeight = font.getLineHeight();
        float currentY = y;

        for (String line : lines) {
            String[] words = line.split(" ");
            StringBuilder wrappedLine = new StringBuilder();

            for (String word : words) {
                String testLine = wrappedLine.length() == 0 ? word : wrappedLine + " " + word;
                layout.setText(font, testLine);

                if (layout.width > maxWidth) {
                    font.draw(batch, wrappedLine.toString(), x, currentY);
                    currentY -= lineHeight;
                    wrappedLine = new StringBuilder(word);
                } else {
                    wrappedLine = new StringBuilder(testLine);
                }
            }

            if (wrappedLine.length() > 0) {
                font.draw(batch, wrappedLine.toString(), x, currentY);
                currentY -= lineHeight;
            }
        }
    }

    private void drawButton(SpriteBatch batch, Rectangle rect, String text) {
        // Button background
        batch.setColor(0.3f, 0.3f, 0.5f, 0.9f);
        batch.draw(whiteTexture, rect.x, rect.y, rect.width, rect.height);

        // Button border
        batch.setColor(0.8f, 0.7f, 0.2f, 1);
        drawRect(batch, rect.x, rect.y, rect.width, rect.height, 1);

        // Button text
        drawCenteredText(batch, regularFont, text, rect.x + rect.width / 2, rect.y + rect.height - 10, Color.WHITE);
    }

    public void show() {
        active = true;
        currentPage = 0;
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
    }

    public void hide() {
        active = false;
    }

    public boolean isActive() {
        return active;
    }

    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    public void dispose() {
        whiteTexture.dispose();
        panelTexture.dispose();
        closeButtonTexture.dispose();
        regularFont.dispose();
        titleFont.dispose();
        for (Texture texture : iconCache.values()) {
            texture.dispose();
        }
        iconCache.clear();
    }
}