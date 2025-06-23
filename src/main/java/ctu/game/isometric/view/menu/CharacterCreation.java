package ctu.game.isometric.view.menu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import ctu.game.isometric.controller.GameController;
import ctu.game.isometric.model.entity.Character;
import ctu.game.isometric.model.entity.Gender;
import ctu.game.isometric.model.game.GameState;

import static ctu.game.isometric.util.FontGenerator.generateVietNameseFont;

public class CharacterCreation {
    private GameController gameController;
    private boolean initialized = false;

    private BitmapFont font;
    private BitmapFont subtitleFont;
    private ShapeRenderer shapeRenderer;
    private GlyphLayout layout;

    private String playerName = "";
    private String playerGender = "MALE"; // Default
    private static final int MAX_NAME_LENGTH = 17;

    // Static textures
    private Texture maleAvatar;
    private Texture femaleAvatar;
    private Texture currentAvatar;

    private Rectangle nameInputBox;
    private Rectangle maleButton;
    private Rectangle femaleButton;
    private Rectangle confirmButton;

    private boolean isNameFieldActive = false;
    private float cursorBlinkTime = 0;
    private boolean showCursor = false;

    public CharacterCreation(GameController gameController) {
        this.gameController = gameController;
        initResources();
    }

    private void initResources() {
        // Initialize resources needed for rendering
        if (!initialized) {
            this.font = generateVietNameseFont("GrenzeGotisch.ttf", 30);
            this.subtitleFont = generateVietNameseFont("GrenzeGotisch.ttf", 17);
            shapeRenderer = new ShapeRenderer();
            layout = new GlyphLayout();

            // Load static avatars
            try {
                maleAvatar = new Texture(Gdx.files.internal("characters/male_avatar.png"));
                maleAvatar.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                femaleAvatar = new Texture(Gdx.files.internal("characters/female_avatar.png"));
                femaleAvatar.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                currentAvatar = maleAvatar;
            } catch (Exception e) {
                Gdx.app.error("CharacterCreation", "Failed to load avatars", e);
            }

            initialized = true;
        }

        // Define UI element positions - always update these in case of resize
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

        nameInputBox = new Rectangle(screenWidth / 2 - 150, screenHeight / 2 + 50, 300, 40);
        maleButton = new Rectangle(screenWidth / 2 - 160, screenHeight / 2 - 20, 150, 40);
        femaleButton = new Rectangle(screenWidth / 2 + 10, screenHeight / 2 - 20, 150, 40);
        confirmButton = new Rectangle(screenWidth / 2 - 100, screenHeight / 2 - 100, 200, 40);
    }

    public void reset() {
        // Reset text fields and selection
        playerName = "";
        playerGender = "MALE";
        currentAvatar = maleAvatar;
        isNameFieldActive = false;
        cursorBlinkTime = 0;
        showCursor = false;

        // No need to recreate resources if already initialized
        if (!initialized) {
            initResources();
        }
    }

    public void update(float delta) {
        // Handle cursor blinking
        cursorBlinkTime += delta;
        if (cursorBlinkTime > 0.5f) {
            cursorBlinkTime = 0;
            showCursor = !showCursor;
        }

        // Handle input
        if (Gdx.input.justTouched()) {
            int x = Gdx.input.getX();
            int y = Gdx.graphics.getHeight() - Gdx.input.getY(); // Invert Y

            isNameFieldActive = nameInputBox.contains(x, y);

            if (maleButton.contains(x, y)) {
                playerGender = "MALE";
                currentAvatar = maleAvatar;
            }

            if (femaleButton.contains(x, y)) {
                playerGender = "FEMALE";
                currentAvatar = femaleAvatar;
            }

            if (confirmButton.contains(x, y) && playerName.length() > 0) {
                confirmCharacter();
            }
        }

        // Handle text input
//        handleTextInput();
    }


    public boolean handleInput(int keycode) {
        if (keycode == Input.Keys.ESCAPE) {
            gameController.setState(GameState.MAIN_MENU);
            return true;
        }

        // Handle name field activation
        if (keycode == Input.Keys.ENTER) {
            isNameFieldActive = !isNameFieldActive;
            return true;
        }

        return false;
    }

    public boolean handleTextInput(char character) {
        if (!isNameFieldActive) {
            return false;
        }

        // Convert to int để kiểm tra chính xác
        int charCode = (int) character;

        // Handle backspace (ASCII 8)
        if (charCode == 8 && playerName.length() > 0) {
            playerName = playerName.substring(0, playerName.length() - 1);
            return true;
        }

        // Handle enter (ASCII 10 hoặc 13)
        if (charCode == 10 || charCode == 13) {
            isNameFieldActive = false;
            return true;
        }

        // Chỉ chấp nhận ASCII từ 32-126 (printable characters)
        // Và loại trừ các ký tự đặc biệt
        if (isValidInputCharacter(charCode) && playerName.length() < MAX_NAME_LENGTH) {
            // Đảm bảo chỉ thêm 1 ký tự
            char upperChar = java.lang.Character.toUpperCase(character);

            // Kiểm tra để tránh duplicate
            if (playerName.length() == 0 || playerName.charAt(playerName.length() - 1) != upperChar) {
                playerName += upperChar;
            }
            return true;
        }

        return false;
    }

    private boolean isValidInputCharacter(int charCode) {
        // A-Z: 65-90, a-z: 97-122, 0-9: 48-57, space: 32
        return (charCode >= 65 && charCode <= 90) ||   // A-Z
                (charCode >= 97 && charCode <= 122) ||  // a-z
                (charCode >= 48 && charCode <= 57) ||   // 0-9
                (charCode == 32);                       // space
    }

    public void render(SpriteBatch batch) {
        // Save original projection matrix and batch state
        Matrix4 originalMatrix = batch.getProjectionMatrix().cpy();
        boolean batchWasDrawing = batch.isDrawing();

        if (batchWasDrawing) {
            batch.end();
        }

        // Configure and use ShapeRenderer
        shapeRenderer.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, 1280, 720));
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Draw name input box
        shapeRenderer.setColor(isNameFieldActive ? new Color(0.9f, 0.9f, 1f, 1) : new Color(0.8f, 0.8f, 0.8f, 1));
        shapeRenderer.rect(nameInputBox.x, nameInputBox.y, nameInputBox.width, nameInputBox.height);

        // Draw gender buttons
        shapeRenderer.setColor(playerGender.equals("MALE") ? new Color(0.7f, 0.9f, 1f, 1) : new Color(0.8f, 0.8f, 0.8f, 1));
        shapeRenderer.rect(maleButton.x, maleButton.y, maleButton.width, maleButton.height);

        shapeRenderer.setColor(playerGender.equals("FEMALE") ? new Color(1f, 0.7f, 0.9f, 1) : new Color(0.8f, 0.8f, 0.8f, 1));
        shapeRenderer.rect(femaleButton.x, femaleButton.y, femaleButton.width, femaleButton.height);

        // Draw confirm button
        shapeRenderer.setColor(playerName.length() > 0 ? new Color(0.7f, 1f, 0.7f, 1) : new Color(0.6f, 0.6f, 0.6f, 1));
        shapeRenderer.rect(confirmButton.x, confirmButton.y, confirmButton.width, confirmButton.height);

        shapeRenderer.end();

        // Start batch with original matrix
        batch.begin();
        batch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, 1280, 720));

        // Draw text elements
        float titleY = Gdx.graphics.getHeight() - 100;
        layout.setText(font, "Tạo nhân vật mới:");
        font.setColor(Color.WHITE);
        font.draw(batch, "Khởi tạo nhân vật", Gdx.graphics.getWidth() / 2 - layout.width / 2, titleY);

        font.draw(batch, "Tên: " + playerName.length() + "/" + MAX_NAME_LENGTH, nameInputBox.x, nameInputBox.y + nameInputBox.height + 20);
        subtitleFont.draw(batch, " * QUY TẮC NHẬP TÊN NGƯỜI CHƠI: Chỉ cho phép chữ cái tiếng Anh (A-Z), số (0-9) và dấu cách.\n" +
                " * Độ dài tối đa: 17 ký tự. Dấu tiếng Việt sẽ bị tự động loại bỏ nên vui lòng tắt UNIKEY hoặc bộ gõ Tiếng Việt.\n" +
                " * Điều khiển: ENTER (bật/tắt nhập), BACKSPACE (xóa), ESC (thoát).", 10, 80);

        // Draw input text with cursor
        font.setColor(Color.BLACK);
        String displayText = playerName;
        if (isNameFieldActive && showCursor) {
            displayText += "|";
        }
        font.draw(batch, displayText, nameInputBox.x + 10, nameInputBox.y + 28);

        // Draw gender options
        font.setColor(Color.BLACK);
        layout.setText(font, "Male");
        font.draw(batch, "Nam", maleButton.x + maleButton.width / 2 - layout.width / 2, maleButton.y + 28);

        layout.setText(font, "Female");
        font.draw(batch, "Nữ", femaleButton.x + femaleButton.width / 2 - layout.width / 2, femaleButton.y + 28);

        // Draw confirm button
        layout.setText(font, "Confirm");
        font.draw(batch, "Xác nhận", confirmButton.x + confirmButton.width / 2 - layout.width / 2, confirmButton.y + 28);

        // Draw character avatar
        if (currentAvatar != null) {
            float x = Gdx.graphics.getWidth() / 2f + 200;
            float y = Gdx.graphics.getHeight() / 2f;
            batch.draw(currentAvatar, x, y - 50, 150, 200);
        }

        // If batch wasn't drawing originally, end it
        if (!batchWasDrawing) {
            batch.end();
        }
    }

    private void confirmCharacter() {
        // Update character with name and gender
        Character character = gameController.getCharacter();
        character.setName(playerName);

        character.setWordFilePath(playerName + "_dictionary");

        character.setGender(Gender.valueOf(playerGender));
        // Signal that character has been created

        gameController.setCreated(true);
        gameController.getAchievementManager().setProgressForCharater();
    }

    public void dispose() {
        if (font != null) font.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (maleAvatar != null) maleAvatar.dispose();
        if (femaleAvatar != null) femaleAvatar.dispose();
        initialized = false;
    }
}