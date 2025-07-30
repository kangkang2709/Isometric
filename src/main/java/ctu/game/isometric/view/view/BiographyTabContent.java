package ctu.game.isometric.view.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import ctu.game.isometric.model.entity.Character;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;

import java.util.Map;

class BiographyTabContent {
    private Character character;
    private BitmapFont font;
    private BitmapFont headerFont;
    private GlyphLayout layout = new GlyphLayout();
    private Texture checkboxEmptyTexture;
    private Texture checkboxFilledTexture;
    private Texture panelBackground;
    private Texture scrollBarTexture;
    private Texture scrollHandleTexture;

    // Scrolling properties
    private float scrollPosition = 0;
    private float maxScrollPosition = 0;
    private boolean isDraggingScroll = false;
    private float lastDragY;
    private static final float SCROLL_SPEED = 15f;

    private static final Color HEADER_COLOR = new Color(0.12f, 0.65f, 0.89f, 1f);
    private static final Color TEXT_COLOR = new Color(0.9f, 0.9f, 0.9f, 1f);
    private static final Color COMPLETED_COLOR = new Color(0.2f, 0.9f, 0.4f, 1f);

    private Map<String, String> objectiveDescriptions;
    private Map<String, String> subobjectiveDescriptions;
    private String[] objectiveKeys = {
            "intro", "forest_done", "god_intro", "klein_meet", "dungeon_call", "dungeon_entry","boss","return"
    };

    public BiographyTabContent(Character character, BitmapFont font, Map<String, String> objectives) {
        this.character = character;
        this.font = font;
        this.headerFont = font;
        this.objectiveDescriptions = objectives;

        // Create textures
        createCheckboxTextures();
        createPanelBackground();
        createScrollBarTextures();
    }

    private void createCheckboxTextures() {
        Pixmap emptyPixmap = new Pixmap(24, 24, Pixmap.Format.RGBA8888);
        emptyPixmap.setColor(0.5f, 0.5f, 0.6f, 1f);
        emptyPixmap.drawRectangle(0, 0, 23, 23);
        emptyPixmap.drawRectangle(1, 1, 21, 21);
        checkboxEmptyTexture = new Texture(emptyPixmap);
        emptyPixmap.dispose();

        Pixmap filledPixmap = new Pixmap(24, 24, Pixmap.Format.RGBA8888);
        filledPixmap.setColor(0.2f, 0.7f, 1.0f, 1f);
        filledPixmap.drawRectangle(0, 0, 23, 23);
        filledPixmap.drawRectangle(1, 1, 21, 21);
        filledPixmap.setColor(0.2f, 0.9f, 0.4f, 1f);
        filledPixmap.fillRectangle(4, 4, 16, 16);
        checkboxFilledTexture = new Texture(filledPixmap);
        filledPixmap.dispose();
    }

    private void createPanelBackground() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0.1f, 0.12f, 0.16f, 0.7f);
        pixmap.fill();
        panelBackground = new Texture(pixmap);
        pixmap.dispose();
    }

    private void createScrollBarTextures() {
        Pixmap barPixmap = new Pixmap(8, 1, Pixmap.Format.RGBA8888);
        barPixmap.setColor(0.3f, 0.3f, 0.4f, 0.5f);
        barPixmap.fill();
        scrollBarTexture = new Texture(barPixmap);
        barPixmap.dispose();

        Pixmap handlePixmap = new Pixmap(8, 40, Pixmap.Format.RGBA8888);
        handlePixmap.setColor(0.5f, 0.6f, 0.8f, 0.8f);
        handlePixmap.fill();
        handlePixmap.setColor(0.6f, 0.8f, 1.0f, 1f);
        handlePixmap.drawRectangle(0, 0, 7, 39);
        scrollHandleTexture = new Texture(handlePixmap);
        handlePixmap.dispose();
    }

    public void updateCharacter(Character character) {
        this.character = character;
    }
    OrthographicCamera camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

    public void render(SpriteBatch batch, Rectangle bounds) {
        float contentHeight = calculateContentHeight(bounds);
        float visibleHeight = bounds.height - 120;
        maxScrollPosition = Math.max(0, contentHeight - visibleHeight);

        scrollPosition = Math.max(0, Math.min(scrollPosition, maxScrollPosition));

        headerFont.setColor(HEADER_COLOR);
        headerFont.draw(batch, "JOURNEY PROGRESS", bounds.x + 30, bounds.y + bounds.height - 50);

        batch.setColor(Color.WHITE);
        batch.draw(panelBackground,
                bounds.x + 20,
                bounds.y + 20,
                bounds.width - 40,
                bounds.height - 90);

        camera.position.set(camera.viewportWidth / 2, camera.viewportHeight / 2, 0);
        camera.update();

        Rectangle scissorBounds = new Rectangle(
                bounds.x + 20,
                bounds.y + 20,
                bounds.width - 50,
                bounds.height - 90
        );

        Rectangle scissors = new Rectangle();
        ScissorStack.calculateScissors(
                camera,
                batch.getTransformMatrix(),
                scissorBounds,
                scissors
        );

        batch.flush();
        boolean scissorSuccess = ScissorStack.pushScissors(scissors);

        if (scissorSuccess) {
            drawScrollableContent(batch, bounds);
            batch.flush();
            ScissorStack.popScissors();
        }

        if (maxScrollPosition > 0) {
            drawScrollBar(batch, bounds);
        }
    }

    private float calculateContentHeight(Rectangle bounds) {
        float maxWidth = bounds.width - 80;
        float height = 0;

        String introText = "You are an adventurer who found yourself in a mysterious world. " +
                "Your journey will lead you through forests, villages and dungeons as you " +
                "discover the secrets of this realm.";
        layout.setText(font, introText, Color.WHITE, maxWidth, Align.left, true);
        height += layout.height + 40;

        height += 40; // Space for "MAIN OBJECTIVES" header

        for (String key : objectiveKeys) {
            if (objectiveDescriptions.containsKey(key)) {
                String description = objectiveDescriptions.get(key);
                layout.setText(font, description, Color.WHITE, maxWidth, Align.left, true);
                height += layout.height + 20;

                // Account for sub-objectives height
                boolean isCompleted = character.getFlags() != null && character.getFlags().contains(key);
                boolean isInProgress = character.getCurrentObject() != null &&
                        character.getCurrentObject().equals(key);

                // Add height for sub-objectives if they exist and should be displayed
                if ((isInProgress || isCompleted) && character.getSubObjectives(key) != null) {
                    for (Map.Entry<String, String> subObj : character.getSubObjectives(key).entrySet()) {
                        layout.setText(font, subObj.getValue(), Color.WHITE, maxWidth - 20, Align.left, true);
                        height += layout.height + 15; // Match spacing in drawScrollableContent
                    }
                }
            }
        }

        return height;
    }

    private void drawScrollableContent(SpriteBatch batch, Rectangle bounds) {
        float startY = bounds.y + bounds.height - 110 + scrollPosition;
        float startX = bounds.x + 30;
        float checkboxSize = 24;
        float textX = startX + checkboxSize + 15;
        float maxWidth = bounds.width - 80;

        font.setColor(TEXT_COLOR);
        String introText = "You are an adventurer who found yourself in a mysterious world. " +
                "Your journey will lead you through forests, villages and dungeons as you " +
                "discover the secrets of this realm.";

        layout.setText(font, introText, Color.WHITE, maxWidth, Align.left, true);
        font.draw(batch, layout, textX - checkboxSize - 15, startY);

        startY -= layout.height + 40;

        font.setColor(HEADER_COLOR);
        font.draw(batch, "MAIN OBJECTIVES", startX, startY);
        startY -= 40;

        for (String key : objectiveKeys) {
            if (objectiveDescriptions.containsKey(key)) {
                boolean isCompleted = character.getFlags() != null &&
                        character.getFlags().contains(key);
                boolean isInProgress = character.getCurrentObject() != null &&
                        character.getCurrentObject().equals(objectiveDescriptions.get(key));

                batch.setColor(Color.WHITE);
                batch.draw(isCompleted ? checkboxFilledTexture : checkboxEmptyTexture,
                        startX, startY - checkboxSize, checkboxSize, checkboxSize);

                // Choose color based on objective status
                if (isCompleted) {
                    font.setColor(COMPLETED_COLOR);
                } else if (isInProgress) {
                    font.setColor(new Color(1f, 0.8f, 0.2f, 1f)); // Yellow/gold for in-progress
                } else {
                    font.setColor(TEXT_COLOR);
                }

                String description = objectiveDescriptions.get(key);
                layout.setText(font, description, font.getColor(), maxWidth, Align.left, true);
                font.draw(batch, layout, textX, startY);

                // Draw in-progress indicator
                if (isInProgress) {
                    System.out.println("Drawing in-progress indicator for objective: " + key);
                    String inProgressText = " In Progress";
                    font.draw(batch, inProgressText, textX + maxWidth - 150, startY);
                }

                // Handle sub-objectives if this objective is in progress or completed
                if ((isInProgress || isCompleted) && character.getSubObjectives(key) != null) {
                    startY -= layout.height + 10;

                    for (Map.Entry<String, String> subObj : character.getSubObjectives(key).entrySet()) {
                        // If main objective is complete, automatically mark sub-objectives as complete
                        boolean subCompleted = isCompleted || character.getFlags().contains(subObj.getKey());

                        // Indent the sub-objective
                        batch.setColor(Color.WHITE);
                        batch.draw(subCompleted ? checkboxFilledTexture : checkboxEmptyTexture,
                                startX + 20, startY - checkboxSize, checkboxSize, checkboxSize);

                        font.setColor(subCompleted ? COMPLETED_COLOR : TEXT_COLOR);

                        layout.setText(font, subObj.getValue(), font.getColor(), maxWidth - 20, Align.left, true);
                        font.draw(batch, layout, textX + 20, startY);

                        startY -= layout.height + 15;
                    }
                } else {
                    startY -= layout.height + 20;
                }
            }
        }
    }

    private void drawScrollBar(SpriteBatch batch, Rectangle bounds) {
        float barWidth = 8;
        float barHeight = bounds.height - 110;
        float barX = bounds.x + bounds.width - 35;
        float barY = bounds.y + 30;

        // Draw scrollbar track
        batch.setColor(new Color(0.3f, 0.3f, 0.4f, 0.5f));
        batch.draw(scrollBarTexture, barX, barY, barWidth, barHeight);

        // Calculate visible ratio (viewport height / content height)
        float contentHeight = calculateContentHeight(bounds);
        float visibleHeight = bounds.height - 120;
        float visibleRatio = Math.min(1, visibleHeight / contentHeight);

        // Calculate handle height and position
        float handleHeight = Math.max(40, barHeight * visibleRatio);

        // Position the handle based on current scroll position
        float availableScrollSpace = barHeight - handleHeight;
        float scrollPercentage = maxScrollPosition > 0 ? scrollPosition / maxScrollPosition : 0;
        float handleY = barY + availableScrollSpace * (1 - scrollPercentage);

        // Draw handle with highlight color
        batch.setColor(new Color(0.6f, 0.8f, 1.0f, 0.8f));
        batch.draw(scrollHandleTexture, barX, handleY, barWidth, handleHeight);

        // Draw handle border for better visibility
        batch.setColor(new Color(0.7f, 0.9f, 1.0f, 1.0f));
        batch.draw(scrollBarTexture, barX, handleY, barWidth, 2); // Top border
        batch.draw(scrollBarTexture, barX, handleY + handleHeight - 2, barWidth, 2); // Bottom border
    }

    public boolean handleClick(float screenX, float screenY, Rectangle bounds) {
        float barWidth = 8;
        float barHeight = bounds.height - 110;
        float barX = bounds.x + bounds.width - 35;
        float barY = bounds.y + 30;

        if (screenX >= barX && screenX <= barX + barWidth &&
                screenY >= barY && screenY <= barY + barHeight) {

            isDraggingScroll = true;
            lastDragY = screenY;

            float clickPosRatio = (screenY - barY) / barHeight;
            scrollPosition = clickPosRatio * maxScrollPosition;
            return true;
        }

        return false;
    }

    public boolean handleDrag(float screenX, float screenY) {
        if (isDraggingScroll && maxScrollPosition > 0) {
            float deltaY = screenY - lastDragY;
            lastDragY = screenY;

            scrollPosition += deltaY * 1.5f;

            scrollPosition = Math.max(0, Math.min(scrollPosition, maxScrollPosition));
            return true;
        }
        return false;
    }

    public boolean handleScroll(float amount) {
        if (maxScrollPosition > 0) {
            scrollPosition += amount * SCROLL_SPEED;
            scrollPosition = Math.max(0, Math.min(scrollPosition, maxScrollPosition));
            return true;
        }
        return false;
    }

    public void endDrag() {
        isDraggingScroll = false;
    }

    public void dispose() {
        if (checkboxEmptyTexture != null) checkboxEmptyTexture.dispose();
        if (checkboxFilledTexture != null) checkboxFilledTexture.dispose();
        if (panelBackground != null) panelBackground.dispose();
        if (scrollBarTexture != null) scrollBarTexture.dispose();
        if (scrollHandleTexture != null) scrollHandleTexture.dispose();
    }
}