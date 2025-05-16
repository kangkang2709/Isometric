package ctu.game.isometric.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;
import ctu.game.isometric.model.game.GameState;

import java.util.ArrayList;
import java.util.List;

import static ctu.game.isometric.util.FontGenerator.generateVietNameseFont;

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
    private String menuTitle = "Thiết Lập";

    private Texture sliderKnobTexture;    // For sliders
    private Texture sliderBarTexture;

    // Mouse interaction properties
    private List<Rectangle> buttonRectangles; // Store button positions for hit detection
    private List<Rectangle> sliderRectangles; // Store slider positions for hit detection
    private boolean isDraggingSlider = false;
    private int draggingSliderIndex = -1;

    public SettingsMenuController(GameController gameController) {
        this.gameController = gameController;
        this.menuOptions = new ArrayList<>();
        this.selectedIndex = 0;
        this.buttonRectangles = new ArrayList<>();
        this.sliderRectangles = new ArrayList<>();

        this.font = generateVietNameseFont("GrenzeGotisch.ttf", 30);

        this.titleFont = generateVietNameseFont("GrenzeGotisch.ttf", 50);

        this.shapeRenderer = new ShapeRenderer();

        // Load textures
        sliderKnobTexture = new Texture(Gdx.files.internal("ui/slider_knob.png"));
        sliderBarTexture = new Texture(Gdx.files.internal("ui/slider_bar.png"));

        // Add adjustable options connected to MusicController
        addMenuOption("Âm Thanh: ", MenuOption.OptionType.TOGGLE,
                () -> gameController.getMusicController().setEnabled(
                        !gameController.getMusicController().isEnabled()));

        addMenuOption("Âm Lượng", MenuOption.OptionType.SLIDER,
                () -> {
                    // Volume adjustment logic is handled elsewhere
                });

        // Add back option
        addMenuOption("Quay Lại", MenuOption.OptionType.BUTTON,
                () -> {
                    gameController.setState(GameState.MAIN_MENU);
                });

        // Set menu dimensions
        menuWidth = 400f;
        menuX = Gdx.graphics.getWidth() / 2 - menuWidth / 2;
        menuY = Gdx.graphics.getHeight() / 2 - menuHeight / 2 -150;


        // In the constructor, update:
        float extraHeightForSliders = 0;
        for (MenuOption option : menuOptions) {
            if (option.getType() == MenuOption.OptionType.SLIDER) {
                extraHeightForSliders += 20; // Extra space for each slider
            }
        }
        menuHeight = (menuOptions.size() * itemHeight) + (padding * 3) + 60 + extraHeightForSliders;

        // Initialize toggle states based on current settings
        menuOptions.get(0).setToggled(gameController.getMusicController().isEnabled());
        menuOptions.get(1).setValue(gameController.getMusicController().getVolume());

        // Initialize rectangles for hit detection
        for (int i = 0; i < menuOptions.size(); i++) {
            buttonRectangles.add(new Rectangle());
            sliderRectangles.add(new Rectangle());
        }
    }

    public void update(float delta) {
        animationTime += delta;
        selectionPulse = (float) Math.sin(animationTime * 5) * 0.2f + 0.8f;
    }

    public void addMenuOption(String name, MenuOption.OptionType type, Runnable onChange) {
        menuOptions.add(new MenuOption(name, type, onChange));
        buttonRectangles.add(new Rectangle());
        sliderRectangles.add(new Rectangle());
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
        titleFont.draw(batch, menuTitle, menuX, menuY + menuHeight - padding,
                menuWidth, Align.center, false);

        // Draw menu options with buttons
        float y = menuY + menuHeight - padding - 60 - itemHeight / 2;
        float buttonWidth = menuWidth - padding * 4;
        float buttonHeight = itemHeight - 10;

        for (int i = 0; i < menuOptions.size(); i++) {
            MenuOption option = menuOptions.get(i);
            boolean isSelected = i == selectedIndex;

            // Button rectangle position
            float buttonX = menuX + padding * 2;
            float buttonY = y - buttonHeight;

            // Update rectangle for hit detection
            buttonRectangles.get(i).set(buttonX, buttonY, buttonWidth, buttonHeight);

            // Draw button background
//            batch.draw(buttonBg, buttonX, buttonY, buttonWidth, buttonHeight);

            // Prepare text color based on selection
            if (isSelected) {
                font.setColor(Color.YELLOW);
            } else {
                font.setColor(Color.WHITE);
            }

            // Create display text
            String displayText = option.getName();
            if (option.getType() == MenuOption.OptionType.TOGGLE) {
                displayText += ": " + (option.isToggled() ? "Bật" : "Tắt");
            } else if (option.getType() == MenuOption.OptionType.SLIDER) {
                displayText += ": " + (int) (option.getValue() * 100) + "%";
            }

            // Draw text centered on button
            font.draw(batch, displayText, buttonX, y + font.getCapHeight() / 2,
                    buttonWidth, Align.center, false);

            // Draw slider if applicable
            // Modify the render method's slider section:
            if (option.getType() == MenuOption.OptionType.SLIDER) {
                float sliderWidth = buttonWidth - 40;
                float sliderX = buttonX + 20;
                float sliderY = buttonY - 15; // Increase this value to move slider further down
                float sliderHeight = 15;

                // Update slider rectangle for hit detection
                sliderRectangles.get(i).set(sliderX, sliderY, sliderWidth, sliderHeight);

                // Draw slider bar
                batch.draw(sliderBarTexture, sliderX, sliderY, sliderWidth, sliderHeight);

                // Draw slider knob
                float knobSize = 25;
                float knobX = sliderX + (sliderWidth * option.getValue()) - (knobSize / 2);
                float knobY = sliderY - (knobSize - sliderHeight) / 2;
                batch.draw(sliderKnobTexture, knobX, knobY, knobSize, knobSize);

                // Add extra space after slider options
                y -= 35; // Extra space for slider controls
            }

            y -= itemHeight + 10; // Add some extra spacing between buttons
        }

        batch.end();
        batch.setProjectionMatrix(originalMatrix);

        if (wasBatchDrawing) {
            batch.begin();
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
        // Dispose textures
        if (sliderKnobTexture != null) sliderKnobTexture.dispose();
        if (sliderBarTexture != null) sliderBarTexture.dispose();
    }

    public class MenuOption {
        public enum OptionType {TOGGLE, SLIDER, BUTTON}

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