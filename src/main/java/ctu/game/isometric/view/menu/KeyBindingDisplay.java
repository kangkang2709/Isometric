package ctu.game.isometric.view.menu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Align;
import ctu.game.isometric.controller.GameController;

import java.util.ArrayList;
import java.util.List;

import static ctu.game.isometric.util.FontGenerator.generateVietNameseFont;

public class KeyBindingDisplay {
    private GameController gameController;
    private BitmapFont titleFont;
    private BitmapFont keyFont;
    private BitmapFont descriptionFont;
    private ShapeRenderer shapeRenderer;

    private List<KeyBinding> keyBindings;
    private float scrollOffset = 0;
    private final float SCROLL_SPEED = 50f;
    private final float LINE_HEIGHT = 40f;
    private final float PADDING = 20f;

    private boolean visible = false;

    public KeyBindingDisplay(GameController gameController) {
        this.gameController = gameController;
        this.titleFont = gameController.getBigCommonFont();
        this.keyFont = gameController.getCommonFont();
        this.descriptionFont = gameController.getCommonFont();
        this.shapeRenderer = new ShapeRenderer();

        initializeKeyBindings();
    }

    private void initializeKeyBindings() {
        keyBindings = new ArrayList<>();

        // Movement keys
// Movement keys
        keyBindings.add(new KeyBinding("Movement", "", Color.YELLOW));
        keyBindings.add(new KeyBinding("W / UP", "Move up", Color.WHITE));
        keyBindings.add(new KeyBinding("S / DOWN", "Move down", Color.WHITE));
        keyBindings.add(new KeyBinding("A / LEFT", "Move left", Color.WHITE));
        keyBindings.add(new KeyBinding("D / RIGHT", "Move right", Color.WHITE));
        keyBindings.add(new KeyBinding("Left Click", "Move in certain maps", Color.WHITE));

// Interface keys
        keyBindings.add(new KeyBinding("Interface", "", Color.YELLOW));
        keyBindings.add(new KeyBinding("ESC", "Open pause menu", Color.WHITE));
        keyBindings.add(new KeyBinding("TAB", "Toggle exploration UI", Color.WHITE));
        keyBindings.add(new KeyBinding("I", "Open/close inventory", Color.WHITE));
        keyBindings.add(new KeyBinding("V", "Open dictionary", Color.WHITE));
        keyBindings.add(new KeyBinding("Q", "Open quest tracker", Color.WHITE));
        keyBindings.add(new KeyBinding("F1", "Open character status", Color.WHITE));
        keyBindings.add(new KeyBinding("F3", "Open/close achievements", Color.WHITE));
        keyBindings.add(new KeyBinding("1", "Toggle darkness", Color.WHITE));
        keyBindings.add(new KeyBinding("2", "Toggle path visualization", Color.WHITE));

// Action keys
        keyBindings.add(new KeyBinding("Actions", "", Color.YELLOW));
        keyBindings.add(new KeyBinding("E / SPACE", "Interact/Confirm with environment or items", Color.WHITE));
        keyBindings.add(new KeyBinding("F", "Interact with NPC", Color.WHITE));
        keyBindings.add(new KeyBinding("ENTER", "Confirm/Continue", Color.WHITE));

// UI specific keys
        keyBindings.add(new KeyBinding("Special UI", "", Color.YELLOW));
        keyBindings.add(new KeyBinding("UP/DOWN", "Navigate menu", Color.WHITE));
        keyBindings.add(new KeyBinding("Mouse Wheel", "Zoom in/out on some maps", Color.WHITE));

// Debug keys (optional)
        keyBindings.add(new KeyBinding("Debug (Test Only)", "", Color.ORANGE));
// keyBindings.add(new KeyBinding("F8", "Teleport to tavern", Color.GRAY));
        keyBindings.add(new KeyBinding("F9", "Teleport to main map", Color.GRAY));
        keyBindings.add(new KeyBinding("F10", "Teleport to DARK Dungeon", Color.GRAY));
        keyBindings.add(new KeyBinding("F11", "Teleport to 3D Dungeon", Color.GRAY));
        keyBindings.add(new KeyBinding("F12", "Change weather", Color.GRAY));

    }

    public void show() {
        visible = true;
        scrollOffset = 0;
    }

    public void hide() {
        visible = false;
    }

    public boolean isVisible() {
        return visible;
    }

    public void scrollUp() {
        scrollOffset = Math.max(0, scrollOffset - SCROLL_SPEED);
    }

    public void scrollDown() {
        float maxScroll = Math.max(0, (keyBindings.size() * LINE_HEIGHT) - (Gdx.graphics.getHeight() - 200));
        scrollOffset = Math.min(maxScroll, scrollOffset + SCROLL_SPEED);
    }

    public void render(SpriteBatch batch) {
        if (!visible) return;

        Matrix4 originalMatrix = new Matrix4(batch.getProjectionMatrix());
        boolean wasBatchDrawing = batch.isDrawing();

        if (wasBatchDrawing) {
            batch.end();
        }

        batch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0,
                Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));

        shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());

        // Draw background
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 0, 0, 0.8f);
        shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // Draw content panel
        float panelX = 100;
        float panelY = 0;
        float panelWidth = Gdx.graphics.getWidth() - 200;
        float panelHeight = Gdx.graphics.getHeight();

        shapeRenderer.setColor(0.1f, 0.1f, 0.2f, 0.9f);
        shapeRenderer.rect(panelX, panelY, panelWidth, panelHeight);

        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();

        // Draw title
        titleFont.draw(batch, "KEY BINDING", panelX, panelY + panelHeight - 30,
                panelWidth, Align.center, false);

        // Draw key bindings
        float currentY = panelY + panelHeight - 80 + scrollOffset;

        for (KeyBinding binding : keyBindings) {
            if (currentY > panelY - LINE_HEIGHT && currentY < panelY + panelHeight) {
                if (binding.isHeader()) {
                    // Draw header
                    titleFont.setColor(binding.color);
                    titleFont.draw(batch, binding.key, panelX + PADDING, currentY);
                } else {
                    // Draw key binding
                    keyFont.setColor(Color.CYAN);
                    keyFont.draw(batch, binding.key, panelX + PADDING, currentY);

                    descriptionFont.setColor(binding.color);
                    descriptionFont.draw(batch, binding.description, panelX + PADDING + 150, currentY);
                }
            }
            currentY -= LINE_HEIGHT;
        }

        // Draw scroll hint
        descriptionFont.setColor(Color.LIGHT_GRAY);
        descriptionFont.draw(batch, "ESC: Quay lại  |  ↑/↓: Cuộn  |  Mouse Wheel: Cuộn",
                300, 30, Gdx.graphics.getWidth(), Align.center, false);

        batch.setProjectionMatrix(originalMatrix);

        if (!wasBatchDrawing) {
            batch.end();
        }
    }

    public boolean handleInput(int keycode) {
        if (!visible) return false;

        switch (keycode) {
            case Keys.ESCAPE:
                hide();
                return true;
            case Keys.UP:
                scrollUp();
                return true;
            case Keys.DOWN:
                scrollDown();
                return true;
            default:
                return false;
        }
    }

    public boolean handleScroll(float amountY) {
        if (!visible) return false;

        if (amountY > 0) {
            scrollDown();
        } else {
            scrollUp();
        }
        return true;
    }

    public void dispose() {
        if (titleFont != null) titleFont.dispose();
        if (keyFont != null) keyFont.dispose();
        if (descriptionFont != null) descriptionFont.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
    }

    private static class KeyBinding {
        String key;
        String description;
        Color color;

        public KeyBinding(String key, String description, Color color) {
            this.key = key;
            this.description = description;
            this.color = color;
        }

        public boolean isHeader() {
            return description.isEmpty();
        }
    }
}