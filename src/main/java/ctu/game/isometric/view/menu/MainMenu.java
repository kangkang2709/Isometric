package ctu.game.isometric.view.menu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import ctu.game.isometric.controller.GameController;
import ctu.game.isometric.model.game.GameState;

import static ctu.game.isometric.util.FontGenerator.generateVietNameseFont;

public class MainMenu {
    private GameController gameController;

    private Texture[] backgroundLayers;
    private float[] layerSpeeds;
    private float[] layerOffsets;
    private float[] layerPulsePhases;
    private float[] layerPulseAmplitudes;

    private BitmapFont font;

    private Texture buttonNormal;
    private Texture buttonSelected;

    private final int screenWidth = 1280;
    private final int screenHeight = 720;

    private String[] menuOptions = {"Chơi mới", "Tiếp Tục", "Tùy Chỉnh", "Thoát"};
    private int selectedOption = 0;

    private Rectangle[] buttonRects;

    private final int BUTTON_WIDTH = 250;
    private final int BUTTON_HEIGHT = 60;

    private float inputCooldown = 0;
    private final float INPUT_DELAY = 0.2f;

    private Texture titleTexture;
    private float titleY;
    private float titleTargetY;
    private boolean titleAnimationComplete = false;
    private final float TITLE_ANIMATION_SPEED = 100f; // pixels per second

    public MainMenu(GameController gameController) {
        this.font = generateVietNameseFont("GrenzeGotisch.ttf", 35);
        this.font.setColor(Color.WHITE);
        this.gameController = gameController;

        initializeParallaxBackground();

        buttonNormal = new Texture(Gdx.files.internal("ui/button.png"));
        buttonSelected = new Texture(Gdx.files.internal("ui/button_selected.png"));

        buttonRects = new Rectangle[menuOptions.length];
        int menuX = (screenWidth - BUTTON_WIDTH) / 2;
        int totalMenuHeight = (menuOptions.length * BUTTON_HEIGHT) + ((menuOptions.length - 1) * 20);

        int startY = (screenHeight + totalMenuHeight) / 2 -170;
        int spacing = 80;

        // Thêm vào constructor sau dòng khởi tạo buttonRects
        titleTexture = new Texture(Gdx.files.internal("ui/title.png")); // Thay đổi đường dẫn texture của bạn
        titleY = screenHeight + 100; // Bắt đầu từ ngoài màn hình
        titleTargetY = startY + 150; // Vị trí cuối cùng của title

        for (int i = 0; i < menuOptions.length; i++) {
            buttonRects[i] = new Rectangle(menuX, startY - (i * spacing), BUTTON_WIDTH, BUTTON_HEIGHT);
        }

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 0.3f); // Màu đen với alpha 0.5 (50% trong suốt)
        pixmap.fill();
        transparentPanel = new Texture(pixmap);
        pixmap.dispose();
    }
    Texture transparentPanel;

    public void update(float delta) {
        if (inputCooldown > 0) {
            inputCooldown -= delta;
        }

        updateParallaxBackground(delta);
        updateTitleAnimation(delta);
//        handleInput();
    }

    private void updateTitleAnimation(float delta) {
        if (!titleAnimationComplete) {
            titleY -= TITLE_ANIMATION_SPEED * delta;
            if (titleY <= titleTargetY) {
                titleY = titleTargetY;
                titleAnimationComplete = true;
            }
        }
    }
    public void render(SpriteBatch batch) {
        Matrix4 originalMatrix = new Matrix4(batch.getProjectionMatrix());

        batch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        renderParallaxBackground(batch);

        // Vẽ title
        float titleX = (screenWidth - titleTexture.getWidth()) / 2;
        batch.draw(titleTexture, titleX, titleY);


        float padding = 20f;
        float panelX = buttonRects[0].x - padding;
        float panelY = buttonRects[buttonRects.length - 1].y - padding;
        float panelWidth = buttonRects[0].width + padding * 2;
        float panelHeight = (buttonRects[0].y + buttonRects[0].height - buttonRects[buttonRects.length - 1].y) + padding * 2;
        batch.draw(transparentPanel, panelX, panelY, panelWidth, panelHeight);

        GlyphLayout layout = new GlyphLayout();
        for (int i = 0; i < menuOptions.length; i++) {
            Rectangle buttonRect = buttonRects[i];


            if (i == selectedOption) {
                batch.draw(buttonSelected, buttonRect.x, buttonRect.y, buttonRect.width, buttonRect.height);

            }
//            Texture buttonTexture = (i == selectedOption) ? buttonSelected : buttonNormal;

            layout.setText(font, menuOptions[i]);
            float textWidth = layout.width;
            float textX = buttonRect.x + (buttonRect.width - textWidth) / 2;
            float textY = buttonRect.y + buttonRect.height - 15;

            font.draw(batch, menuOptions[i], textX, textY);
        }

        batch.setProjectionMatrix(originalMatrix);
    }

    private void initializeParallaxBackground() {
        backgroundLayers = new Texture[4];
        layerSpeeds = new float[4];
        layerOffsets = new float[4];
        layerPulsePhases = new float[4];
        layerPulseAmplitudes = new float[4];

        backgroundLayers[0] = new Texture(Gdx.files.internal("backgrounds/pause/bg_layer_1.png"));
        backgroundLayers[0].setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        backgroundLayers[1] = new Texture(Gdx.files.internal("backgrounds/pause/bg_layer_2.png"));
        backgroundLayers[1].setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        backgroundLayers[2] = new Texture(Gdx.files.internal("backgrounds/pause/bg_layer_3.png"));
        backgroundLayers[2].setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        backgroundLayers[3] = new Texture(Gdx.files.internal("backgrounds/pause/bg_layer_4.png"));
        backgroundLayers[3].setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);


        layerSpeeds[0] = 1.0f;
        layerSpeeds[1] = 0.5f;
        layerSpeeds[2] = 0.5f;
        layerSpeeds[3] = 1.0f;

        // Điều chỉnh biên độ dao động theo trục X
        layerPulseAmplitudes[0] = 10f;
        layerPulseAmplitudes[1] = 5f;
        layerPulseAmplitudes[2] = 5f;
        layerPulseAmplitudes[3] = 5f;

        for (int i = 0; i < layerOffsets.length; i++) {
            layerOffsets[i] = 0f;
            layerPulsePhases[i] = (float) (Math.random() * Math.PI * 2);
        }
    }

    private void renderParallaxBackground(SpriteBatch batch) {
        for (int i = 0; i < backgroundLayers.length; i++) {
            Texture layer = backgroundLayers[i];

            // Tạo hiệu ứng pulse ngang (trục X)
            float xOffset = (float) Math.sin(layerOffsets[i] + layerPulsePhases[i]) * layerPulseAmplitudes[i];

            // Không cần hiệu ứng dọc nữa
            batch.draw(layer, xOffset -20, 0, 1400, screenHeight);
        }
    }

    private void updateParallaxBackground(float delta) {
        for (int i = 0; i < layerOffsets.length; i++) {
            layerOffsets[i] += layerSpeeds[i] * delta;
        }
    }

    public boolean handleClick(int mouseX, int mouseY) {
        mouseY = screenHeight - mouseY;

        for (int i = 0; i < buttonRects.length; i++) {
            if (buttonRects[i].contains(mouseX, mouseY)) {
                selectedOption = i;
                selectOption(i);
                return true;
            }
        }
        return false;
    }

    public boolean handleKey(int keyCode) {
        if (inputCooldown > 0) return false;

        if (keyCode == Input.Keys.UP || keyCode == Input.Keys.W) {
            selectedOption = (selectedOption - 1 + menuOptions.length) % menuOptions.length;
            inputCooldown = INPUT_DELAY;
            return true;
        } else if (keyCode == Input.Keys.DOWN || keyCode == Input.Keys.S) {
            selectedOption = (selectedOption + 1) % menuOptions.length;
            inputCooldown = INPUT_DELAY;
            return true;
        } else if (keyCode == Input.Keys.ENTER || keyCode == Input.Keys.SPACE) {
            selectOption(selectedOption);
            inputCooldown = INPUT_DELAY;
            return true;
        }

        return false;
    }
    public boolean handleMouseMove(int mouseX, int mouseY) {
        mouseY = screenHeight - mouseY;

        for (int i = 0; i < buttonRects.length; i++) {
            if (buttonRects[i].contains(mouseX, mouseY)) {
                if (selectedOption != i) {
                    selectedOption = i;
                    return true; // Có thay đổi lựa chọn do di chuột
                }
                break;
            }
        }
        return false;
    }


    private void selectOption(int option) {
        switch (option) {
            case 0:
                gameController.setCurrentState(GameState.CHARACTER_CREATION);
                gameController.setPreviousState(GameState.MAIN_MENU);
                break;
            case 1:
                gameController.setState(GameState.LOAD_GAME);
                break;
            case 2:
                gameController.setState(GameState.SETTINGS);
                break;
            case 3:
                Gdx.app.exit();
                break;
        }
    }

    public void dispose() {
        for (Texture layer : backgroundLayers) {
            if (layer != null) {
                layer.dispose();
            }
        }

        font.dispose();
        buttonNormal.dispose();
        titleTexture.dispose();
        buttonSelected.dispose();
    }
}
