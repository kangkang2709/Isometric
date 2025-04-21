package ctu.game.isometric.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Align;
import ctu.game.isometric.model.game.GameState;

import java.util.ArrayList;
import java.util.List;

public class SettingsMenuController {
    private GameController gameController;
    private List<MenuOption> menuOptions;
    private int selectedIndex;
    private BitmapFont titleFont;
    private BitmapFont font;
    private ShapeRenderer shapeRenderer;
    private float menuX;
    private float menuY;
    private float menuWidth;
    private float menuHeight;
    private float itemHeight = 50f;
    private float padding = 20f;

    // Animation properties
    private float animationTime = 0;
    private float selectionPulse = 0;
    private String menuTitle = "SETTINGS";

    public SettingsMenuController(GameController gameController) {
        this.gameController = gameController;
        this.menuOptions = new ArrayList<>();
        this.selectedIndex = 0;
        this.font = new BitmapFont();
        font.getData().setScale(1.5f);

        this.titleFont = new BitmapFont();
        titleFont.getData().setScale(2.0f);
        titleFont.setColor(Color.GOLD);

        this.shapeRenderer = new ShapeRenderer();

        // Add adjustable options connected to MusicController
        addMenuOption("Music", MenuOption.OptionType.TOGGLE,
                () -> gameController.getMusicController().setEnabled(
                        !gameController.getMusicController().isEnabled()));

        addMenuOption("Volume", MenuOption.OptionType.SLIDER,
                () -> {
                    // Volume adjustment logic is handled elsewhere
                });

//        // Add back option
        addMenuOption("Back", MenuOption.OptionType.BUTTON,
                () -> gameController.returnToPreviousState());

        // Set menu dimensions
        menuWidth = 400f;
        menuHeight = (menuOptions.size() * itemHeight) + (padding * 3) + 60; // Extra for title
        menuX = Gdx.graphics.getWidth() / 2 - menuWidth / 2;
        menuY = Gdx.graphics.getHeight() / 2 - menuHeight / 2;

        // Initialize toggle states based on current settings
        menuOptions.get(0).setToggled(gameController.getMusicController().isEnabled());
        menuOptions.get(1).setValue(gameController.getMusicController().getVolume());
    }

    public void update(float delta) {
        animationTime += delta;
        selectionPulse = (float) Math.sin(animationTime * 5) * 0.2f + 0.8f;
    }

    public void addMenuOption(String name, MenuOption.OptionType type, Runnable onChange) {
        menuOptions.add(new MenuOption(name, type, onChange));
    }

    public void selectNextItem() {
        selectedIndex = (selectedIndex + 1) % menuOptions.size();
    }

    public void selectPreviousItem() {
        selectedIndex = (selectedIndex - 1 + menuOptions.size()) % menuOptions.size();
    }

    public void activateSelectedItem() {
        if (selectedIndex >= 0 && selectedIndex < menuOptions.size()) {
            MenuOption option = menuOptions.get(selectedIndex);
            if (option.getType() == MenuOption.OptionType.TOGGLE) {
                option.toggle();
            } else if (option.getType() == MenuOption.OptionType.BUTTON) {
                option.activate();
            }
        }
    }

    public void adjustSelectedOption(boolean increase) {
        if (selectedIndex >= 0 && selectedIndex < menuOptions.size()) {
            MenuOption option = menuOptions.get(selectedIndex);
            if (option.getType() == MenuOption.OptionType.TOGGLE) {
                        option.toggle();
//                if (option.getName().equals("Music")) {
//                    gameController.getMusicController().setEnabled(option.isToggled());
//                }
            } else if (option.getType() == MenuOption.OptionType.SLIDER) {
                float newValue = option.getValue() + (increase ? 0.02f : -0.02f);
                newValue = Math.max(0f, Math.min(1f, newValue)); // Clamp between 0 and 1
                option.setValue(newValue);
                if (option.getName().equals("Volume")) {
                    gameController.getMusicController().setVolume(option.getValue());
                }
            }
        }
    }

    public void render(SpriteBatch batch) {
        Matrix4 originalMatrix = new Matrix4(batch.getProjectionMatrix());
        batch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0,
                Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));

        boolean wasBatchDrawing = batch.isDrawing();
        if (wasBatchDrawing) {
            batch.end();
        }

        // Render background and borders
        shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Darkened background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 0, 0, 0.7f);
        shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // Menu panel
        shapeRenderer.setColor(0.2f, 0.2f, 0.3f, 0.9f);
        shapeRenderer.rect(menuX, menuY, menuWidth, menuHeight);
        shapeRenderer.end();

        // Border
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.5f, 0.5f, 0.7f, 1);
        shapeRenderer.rect(menuX, menuY, menuWidth, menuHeight);
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();

        // Draw title
        titleFont.draw(batch, menuTitle, menuX, menuY + menuHeight - padding - 30,
                menuWidth, Align.center, false);

        // Draw menu options
        float y = menuY + menuHeight - padding - 60 - itemHeight / 2;

        for (int i = 0; i < menuOptions.size(); i++) {
            MenuOption option = menuOptions.get(i);

            if (i == selectedIndex) {
                font.setColor(Color.YELLOW);
            } else {
                font.setColor(Color.WHITE);
            }

            String displayText = option.getName();

            if (option.getType() == MenuOption.OptionType.TOGGLE) {
                displayText += ": " + (option.isToggled() ? "ON" : "OFF");
            } else if (option.getType() == MenuOption.OptionType.SLIDER) {
                displayText += ": " + (int) (option.getValue() * 100) + "%";
            }

            font.draw(batch, displayText, menuX + padding, y,
                    menuWidth - padding * 2, Align.center, false);

            // Draw slider if applicable
            if (option.getType() == MenuOption.OptionType.SLIDER) {
                batch.end(); // End batch before using shapeRenderer

                float sliderWidth = 200;
                float sliderX = menuX + (menuWidth - sliderWidth) / 2;
                float sliderY = y - font.getCapHeight() / 2f - 30;

                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                shapeRenderer.setColor(0.3f, 0.3f, 0.3f, 0.8f);
                shapeRenderer.rect(sliderX, sliderY, sliderWidth, 10);

                shapeRenderer.setColor(0.7f, 0.7f, 1.0f, 0.8f);
                shapeRenderer.rect(sliderX, sliderY, sliderWidth * option.getValue(), 10);
                shapeRenderer.end();

                batch.begin(); // Resume batch after drawing shapes
            }

            y -= itemHeight;
        }

        batch.end();
        batch.setProjectionMatrix(originalMatrix);

        if (wasBatchDrawing) {
            batch.begin(); // Restore drawing state if it was originally drawing
        }
    }


    public void resize(int width, int height) {
        menuX = width / 2 - menuWidth / 2;
        menuY = height / 2 - menuHeight / 2;
    }

    public void dispose() {
        if (font != null) {
            font.dispose();
        }
        if (titleFont != null) {
            titleFont.dispose();
        }
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
    }

    public class MenuOption {
        public enum OptionType { TOGGLE, SLIDER, BUTTON }
        private String name;
        private OptionType type;
        private float value; // For sliders
        private boolean toggled; // For toggles
        private Runnable onChange; // Callback for when the value changes

        public MenuOption(String name, OptionType type, Runnable onChange) {
            this.name = name;
            this.type = type;
            this.onChange = onChange;
            this.value = 0f;
            this.toggled = false;
        }

        public String getName() {
            return name;
        }

        public OptionType getType() {
            return type;
        }

        public float getValue() {
            return value;
        }

        public void setValue(float value) {
            this.value = value;
            if (onChange != null) onChange.run();
        }

        public boolean isToggled() {
            return toggled;
        }

        public void setToggled(boolean toggled) {
            this.toggled = toggled;
        }

        public void toggle() {
            this.toggled = !toggled;
            if (onChange != null) onChange.run();
        }

        public void activate() {
            if (onChange != null) onChange.run();
        }
    }
}