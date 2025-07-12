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

    // Enhanced cursor and input handling
    private int cursorPosition = 0;
    private float lastClickTime = 0;
    private static final float DOUBLE_CLICK_TIME = 0.3f;
    private boolean isTextSelected = false;
    private int selectionStart = 0;
    private int selectionEnd = 0;

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
        cursorPosition = 0;
        isTextSelected = false;
        selectionStart = 0;
        selectionEnd = 0;
        lastClickTime = 0;

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

        // Handle mouse/touch input
        if (Gdx.input.justTouched()) {
            int x = Gdx.input.getX();
            int y = Gdx.graphics.getHeight() - Gdx.input.getY(); // Invert Y

            // Handle name input box clicks with cursor positioning
            if (nameInputBox.contains(x, y)) {
                if (!isNameFieldActive) {
                    isNameFieldActive = true;
                    cursorPosition = playerName.length(); // Place cursor at end
                } else {
                    // Calculate cursor position based on click location
                    setCursorPositionFromClick(x);
                }

                // Handle double-click to select all
                float currentTime = System.currentTimeMillis() / 1000f;
                if (currentTime - lastClickTime < DOUBLE_CLICK_TIME) {
                    selectAllText();
                }
                lastClickTime = currentTime;
            } else {
                isNameFieldActive = false;
                clearSelection();
            }

            // Gender selection
            if (maleButton.contains(x, y)) {
                playerGender = "MALE";
                currentAvatar = maleAvatar;
            }

            if (femaleButton.contains(x, y)) {
                playerGender = "FEMALE";
                currentAvatar = femaleAvatar;
            }

            // Confirm button
            if (confirmButton.contains(x, y) && playerName.length() > 0) {
                confirmCharacter();
            }
        }
    }

    // Calculate cursor position based on mouse click position
    private void setCursorPositionFromClick(int clickX) {
        if (playerName.isEmpty()) {
            cursorPosition = 0;
            clearSelection();
            return;
        }

        float textStartX = nameInputBox.x + 10; // Text padding
        float clickOffset = clickX - textStartX;

        // Find the closest character position
        layout.setText(font, "");
        float currentWidth = 0;

        for (int i = 0; i <= playerName.length(); i++) {
            if (i < playerName.length()) {
                layout.setText(font, playerName.substring(0, i + 1));
                float nextWidth = layout.width;

                if (clickOffset <= (currentWidth + nextWidth) / 2) {
                    cursorPosition = i;
                    clearSelection();
                    return;
                }
                currentWidth = nextWidth;
            } else {
                cursorPosition = playerName.length();
                clearSelection();
            }
        }
    }

    // Select all text functionality
    private void selectAllText() {
        if (!playerName.isEmpty()) {
            isTextSelected = true;
            selectionStart = 0;
            selectionEnd = playerName.length();
            cursorPosition = playerName.length();
        }
    }

    // Clear text selection
    private void clearSelection() {
        isTextSelected = false;
        selectionStart = 0;
        selectionEnd = 0;
    }

    // Delete selected text
    private void deleteSelectedText() {
        if (isTextSelected && selectionStart != selectionEnd) {
            int start = Math.min(selectionStart, selectionEnd);
            int end = Math.max(selectionStart, selectionEnd);

            StringBuilder sb = new StringBuilder(playerName);
            sb.delete(start, end);
            playerName = sb.toString();
            cursorPosition = start;
            clearSelection();
        }
    }

    public boolean handleInput(int keycode) {
        if (keycode == Input.Keys.ESCAPE) {
            gameController.setState(GameState.MAIN_MENU);
            return true;
        }

        // Handle name field activation
        if (keycode == Input.Keys.ENTER || keycode == Input.Keys.TAB) {
            isNameFieldActive = !isNameFieldActive;
            if (isNameFieldActive) {
                cursorPosition = playerName.length();
                clearSelection();
            }
            return true;
        }

        // Only handle text editing when name field is active
        if (!isNameFieldActive) {
            return false;
        }

        // Cursor movement
        if (keycode == Input.Keys.LEFT) {
            if (isTextSelected) {
                cursorPosition = Math.min(selectionStart, selectionEnd);
                clearSelection();
            } else if (cursorPosition > 0) {
                cursorPosition--;
            }
            return true;
        }

        if (keycode == Input.Keys.RIGHT) {
            if (isTextSelected) {
                cursorPosition = Math.max(selectionStart, selectionEnd);
                clearSelection();
            } else if (cursorPosition < playerName.length()) {
                cursorPosition++;
            }
            return true;
        }

        // Home and End keys
        if (keycode == Input.Keys.HOME) {
            cursorPosition = 0;
            clearSelection();
            return true;
        }

        if (keycode == Input.Keys.END) {
            cursorPosition = playerName.length();
            clearSelection();
            return true;
        }

        // Enhanced backspace with cursor and selection support
        if (keycode == Input.Keys.BACKSPACE) {
            if (isTextSelected) {
                deleteSelectedText();
            } else if (cursorPosition > 0 && playerName.length() > 0) {
                StringBuilder sb = new StringBuilder(playerName);
                sb.deleteCharAt(cursorPosition - 1);
                playerName = sb.toString();
                cursorPosition--;
            }
            return true;
        }

        // Delete key
        if (keycode == Input.Keys.FORWARD_DEL) {
            if (isTextSelected) {
                deleteSelectedText();
            } else if (cursorPosition < playerName.length()) {
                StringBuilder sb = new StringBuilder(playerName);
                sb.deleteCharAt(cursorPosition);
                playerName = sb.toString();
            }
            return true;
        }

        // Select all with Ctrl+A
        if (keycode == Input.Keys.A && (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) ||
                Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT))) {
            selectAllText();
            return true;
        }

//        // Copy with Ctrl+C
//        if (keycode == Input.Keys.C && (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) ||
//                Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT))) {
//            if (isTextSelected) {
//                int start = Math.min(selectionStart, selectionEnd);
//                int end = Math.max(selectionStart, selectionEnd);
//                String selectedText = playerName.substring(start, end);
//                Gdx.app.getClipboard().setContents(selectedText);
//            }
//            return true;
//        }
//
//        // Paste with Ctrl+V
//        if (keycode == Input.Keys.V && (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) ||
//                Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT))) {
//            String clipboardText = Gdx.app.getClipboard().getContents();
//            if (clipboardText != null && !clipboardText.isEmpty()) {
//                insertTextAtCursor(clipboardText);
//            }
//            return true;
//        }

        return false;
    }

    // Insert text at cursor position
    private void insertTextAtCursor(String text) {
        // Delete selected text first if any
        if (isTextSelected) {
            deleteSelectedText();
        }

        // Filter and validate the text
        StringBuilder validText = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (isValidVietnameseCharacter(c) && canAddCharacter()) {
                validText.append(c);
            }
        }

        if (validText.length() > 0) {
            StringBuilder sb = new StringBuilder(playerName);
            sb.insert(cursorPosition, validText.toString());

            // Normalize the Unicode string
            String normalized = java.text.Normalizer.normalize(
                    sb.toString(),
                    java.text.Normalizer.Form.NFC
            );

            playerName = normalized;
            cursorPosition += validText.length();
            clampCursorPosition();
        }
    }

    public boolean handleTyped(char c) {
        if (!isNameFieldActive) {
            return false;
        }

        // Skip control characters
        if (c < 32 || c == 127) {
            return false;
        }

        // For Vietnamese input with cursor positioning
        if (isValidVietnameseCharacter(c) && canAddCharacter()) {
            // Delete selected text first if any
            if (isTextSelected) {
                deleteSelectedText();
            }

            StringBuilder sb = new StringBuilder(playerName);
            sb.insert(cursorPosition, c);

            // Normalize the Unicode string
            String normalized = java.text.Normalizer.normalize(
                    sb.toString(),
                    java.text.Normalizer.Form.NFC
            );

            playerName = normalized;
            cursorPosition++;
            clampCursorPosition();
            return true;
        }

        return false;
    }

    private boolean isValidVietnameseCharacter(char c) {
        // Allow Vietnamese characters, including those with diacritics
        return java.lang.Character.isLetterOrDigit(c) ||
                c == ' ' ||
                isVietnameseDiacritic(c) ||
                isVietnameseVowel(c);
    }

    private boolean isVietnameseDiacritic(char c) {
        // Common Vietnamese diacritical marks
        return (c >= '\u0300' && c <= '\u036F') || // Combining diacritical marks
                (c >= '\u1EA0' && c <= '\u1EF9');   // Vietnamese extended characters
    }

    private boolean isVietnameseVowel(char c) {
        String vietnameseVowels = "àáảãạâầấẩẫậăằắẳẵặèéẻẽẹêềếểễệìíỉĩịòóỏõọôồốổỗộơờớởỡợùúủũụưừứửữựỳýỷỹỵđ";
        String upperVietnameseVowels = vietnameseVowels.toUpperCase();
        return vietnameseVowels.indexOf(c) != -1 || upperVietnameseVowels.indexOf(c) != -1;
    }

    // Helper method to check if we can add more characters
    private boolean canAddCharacter() {
        return playerName.length() < MAX_NAME_LENGTH;
    }

    // Helper method to ensure cursor stays within bounds
    private void clampCursorPosition() {
        cursorPosition = Math.max(0, Math.min(cursorPosition, playerName.length()));
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

        // Draw text selection background if active
        if (isNameFieldActive && isTextSelected && selectionStart != selectionEnd) {
            int start = Math.min(selectionStart, selectionEnd);
            int end = Math.max(selectionStart, selectionEnd);

            String beforeSelection = playerName.substring(0, start);
            String selectedText = playerName.substring(start, end);

            layout.setText(font, beforeSelection);
            float selectionStartX = nameInputBox.x + 10 + layout.width;

            layout.setText(font, selectedText);
            float selectionWidth = layout.width;

            shapeRenderer.setColor(new Color(0.3f, 0.5f, 1f, 0.5f)); // Blue selection background
            shapeRenderer.rect(selectionStartX, nameInputBox.y + 2, selectionWidth, nameInputBox.height - 4);
        }

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
                " * Điều khiển: ENTER (bật/tắt nhập), BACKSPACE (xóa), ESC (thoát), Ctrl+A (chọn tất cả), Ctrl+C/V (sao chép/dán).", 10, 80);

        // Draw input text with positioned cursor and selection
        font.setColor(Color.BLACK);

        if (isNameFieldActive) {
            // Draw text with cursor positioning
            String beforeCursor = playerName.substring(0, Math.min(cursorPosition, playerName.length()));
            String afterCursor = playerName.substring(Math.min(cursorPosition, playerName.length()));

            float textX = nameInputBox.x + 10;
            float textY = nameInputBox.y + 28;

            // Draw text before cursor
            font.draw(batch, beforeCursor, textX, textY);

            // Calculate cursor position for rendering
            layout.setText(font, beforeCursor);
            float cursorX = textX + layout.width;

            // Draw cursor if active and should be visible (and no selection)
            if (showCursor && !isTextSelected) {
                font.setColor(Color.BLACK);
                font.draw(batch, "|", cursorX, textY);
            }

            // Draw text after cursor
            layout.setText(font, "|");
            float cursorWidth = (showCursor && !isTextSelected) ? layout.width : 0;
            font.setColor(Color.BLACK);
            font.draw(batch, afterCursor, cursorX + cursorWidth, textY);

        } else {
            // Just draw the text normally when not active
            font.draw(batch, playerName, nameInputBox.x + 10, nameInputBox.y + 28);
        }

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
        character.setName(playerName.replaceAll(" ", "_"));

        character.setWordFilePath(playerName + "_dictionary");

        character.setGender(Gender.valueOf(playerGender));
        // Signal that character has been created

        gameController.setCreated(true);
        gameController.changeBoard();
        gameController.getAchievementManager().setProgressForCharater();
    }

    public void dispose() {
        if (font != null) font.dispose();
        if (subtitleFont != null) subtitleFont.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (maleAvatar != null) maleAvatar.dispose();
        if (femaleAvatar != null) femaleAvatar.dispose();
        initialized = false;
    }
}