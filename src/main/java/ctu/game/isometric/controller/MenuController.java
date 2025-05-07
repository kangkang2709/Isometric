package ctu.game.isometric.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;
import ctu.game.isometric.model.game.GameState;

import java.util.ArrayList;
import java.util.List;

public class MenuController {

    private GameController gameController;
    private List<MenuItem> menuItems;
    private int selectedIndex;
    private BitmapFont titleFont;
    private BitmapFont itemFont;

    // Menu positioning and dimensions
    private float menuX;
    private float menuY;
    private float menuWidth;
    private float menuHeight;
    private float itemHeight = 50f;
    private float padding = 20f;

    private TextureRegion buttonTexture;
    private TextureRegion buttonSelectedTexture;
    private float buttonPadding = 10f;

    // Visual elements
    private ShapeRenderer shapeRenderer;
    private TextureRegion menuBackground;

    private GlyphLayout layout = new GlyphLayout();

    private String menuTitle = "GAME MENU";

    // Animation properties
    private float animationTime = 0;
    private float selectionPulse = 0;

    public MenuController(GameController gameController) {
        this.gameController = gameController;
        this.selectedIndex = 0;
        this.menuItems = new ArrayList<>();

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Creepster-Regular.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        // Initialize fonts

        parameter.size = 42;
        parameter.color = Color.WHITE;
        this.titleFont = generator.generateFont(parameter);

        parameter.size = 32;
        parameter.color = Color.WHITE;
        this.itemFont = generator.generateFont(parameter);
        generator.dispose();

        // Initialize rendering tools
        this.shapeRenderer = new ShapeRenderer();

        // Load button textures
        this.buttonTexture = new TextureRegion(new Texture(Gdx.files.internal("ui/button.png")));
        this.buttonSelectedTexture = new TextureRegion(new Texture(Gdx.files.internal("ui/button_selected.png")));

        // Add default menu items
        addMenuItem("Resume Game", () -> gameController.returnToPreviousState());
        addMenuItem("Options", this::showOptionsMenu);
        addMenuItem("Back To Main Menu", () -> {
            gameController.setCurrentState(GameState.MAIN_MENU);
            gameController.resetGame();
        });
        addMenuItem("Quit Game", () -> Gdx.app.exit());

        // Set menu position (center of screen)
        menuWidth = 400f;
        menuHeight = (menuItems.size() * (itemHeight + buttonPadding)) + (padding * 3) + 60; // Extra space for title
        menuX = Gdx.graphics.getWidth() / 2 - menuWidth / 2;
        menuY = Gdx.graphics.getHeight() / 2 - menuHeight / 2;
    }

    private void showOptionsMenu() {
        System.out.println("Options selected");
    }

    private void showCreditsMenu() {
        System.out.println("Credits selected");
    }

    public void update(float delta) {
        animationTime += delta;
        selectionPulse = (float) Math.sin(animationTime * 5) * 0.2f + 0.8f;
    }

    public void addMenuItem(String text, Runnable action) {
        menuItems.add(new MenuItem(text, action));
    }

    public void selectNextItem() {
        selectedIndex = (selectedIndex + 1) % menuItems.size();
    }

    public void selectPreviousItem() {
        selectedIndex = (selectedIndex - 1 + menuItems.size()) % menuItems.size();
    }

    public void activateSelectedItem() {
        if (selectedIndex >= 0 && selectedIndex < menuItems.size()) {
            menuItems.get(selectedIndex).activate();
        }
    }

    public void render(SpriteBatch batch) {
        Matrix4 originalMatrix = new Matrix4(batch.getProjectionMatrix());

        boolean wasBatchDrawing = batch.isDrawing();
        if (wasBatchDrawing) {
            batch.end();
        }

        // Reset to default orthographic projection for UI rendering
        batch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0,
                Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));

        // Shape rendering with the same projection
        shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Draw darkened background overlay
        shapeRenderer.setColor(0, 0, 0, 0.7f);
        shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // Draw menu panel background
        shapeRenderer.setColor(0.2f, 0.2f, 0.3f, 0.9f);
        shapeRenderer.rect(menuX, menuY, menuWidth, menuHeight);

        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();

        // Draw title
        titleFont.draw(batch, menuTitle, menuX, menuY + menuHeight - padding,
                menuWidth, Align.center, false);

        // Draw menu items with button backgrounds
        float buttonWidth = menuWidth - (padding * 2);
        float y = menuY + menuHeight - padding - 60;

        for (int i = 0; i < menuItems.size(); i++) {
            MenuItem item = menuItems.get(i);

            // Update button position
            item.setPosition(menuX + padding, y - itemHeight, buttonWidth, itemHeight);
            Rectangle bounds = item.getBounds();

            // Draw button background
            TextureRegion buttonBg = (i == selectedIndex) ? buttonSelectedTexture : buttonTexture;
            batch.draw(buttonBg, bounds.x, bounds.y, bounds.width, bounds.height);

            // Draw text centered on button
            Color textColor = (i == selectedIndex) ? Color.YELLOW : Color.WHITE;
            itemFont.setColor(textColor);

            layout.setText(itemFont, item.getText());
            float textX = bounds.x + (bounds.width - layout.width) / 2;
            float textY = bounds.y + (bounds.height + layout.height) / 2;

            itemFont.draw(batch, item.getText(), textX, textY);

            y -= (itemHeight + buttonPadding);
        }

        batch.setProjectionMatrix(originalMatrix);

        if (!wasBatchDrawing) {
            batch.end();
        }
    }


    public boolean handleMouseClick(int screenX, int screenY) {
        // Convert screen coordinates to our UI coordinate system
        float y = Gdx.graphics.getHeight() - screenY; // Flip Y coordinate

        // Check if the click is within any menu item
        for (int i = 0; i < menuItems.size(); i++) {
            Rectangle bounds = menuItems.get(i).getBounds();

            if (bounds.contains(screenX, y)) {
                // Select and activate this item
                selectedIndex = i;
                System.out.println("Selected: " + menuItems.get(i).getText());
                menuItems.get(i).activate();
                return true;
            }
        }

        return false;
    }

    public boolean handleMouseMove(int screenX, int screenY) {
        // Convert screen coordinates to our UI coordinate system
        float y = Gdx.graphics.getHeight() - screenY; // Flip Y coordinate

        // Check if mouse is over any menu item
        for (int i = 0; i < menuItems.size(); i++) {
            Rectangle bounds = menuItems.get(i).getBounds();

            if (bounds.contains(screenX, y)) {
                // Highlight this item
                if (selectedIndex != i) {
                    selectedIndex = i;
                    return true;
                }
                break;
            }
        }

        return false;
    }

    public void resize(int width, int height) {
        menuX = width / 2 - menuWidth / 2;
        menuY = height / 2 - menuHeight / 2;
    }

    public void dispose() {
        if (titleFont != null) titleFont.dispose();
        if (itemFont != null) itemFont.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (buttonTexture != null && buttonTexture.getTexture() != null) buttonTexture.getTexture().dispose();
        if (buttonSelectedTexture != null && buttonSelectedTexture.getTexture() != null) buttonSelectedTexture.getTexture().dispose();
    }

    private static class MenuItem {
        private String text;
        private Runnable action;
        private Rectangle bounds; // Rectangle to define button boundaries

        public MenuItem(String text, Runnable action) {
            this.text = text;
            this.action = action;
            this.bounds = new Rectangle();
        }

        public void setPosition(float x, float y, float width, float height) {
            this.bounds.x = x;
            this.bounds.y = y;
            this.bounds.width = width;
            this.bounds.height = height;
        }

        public Rectangle getBounds() {
            return bounds;
        }

        public String getText() {
            return text;
        }

        public void activate() {
            if (action != null) {
                action.run();
            }
        }
    }
}