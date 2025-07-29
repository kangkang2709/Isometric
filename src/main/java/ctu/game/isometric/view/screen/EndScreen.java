package ctu.game.isometric.view.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;

public class EndScreen implements Screen {
    private final Runnable onEndCallback;
    private float timer = 0f;
    private float effectTimer = 0f;
    private boolean callbackCalled = false;

    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont titleFont;
    private BitmapFont font;
    private GlyphLayout layout;
    private Texture backgroundTexture;
    private Texture vignette;

    // UI animation variables
    private float alpha = 0f;
    private float titleScale = 0.5f;
    private float panelAlpha = 0f;
    private float buttonAlpha = 0f;
    private boolean continueButtonHovered = false;
    private boolean menuButtonHovered = false;
    private Rectangle continueButtonBounds;
    private Rectangle menuButtonBounds;

    // UI colors for FF7 Remake style
    private final Color DARK_BG = new Color(0.05f, 0.02f, 0.08f, 1f);
    private final Color PANEL_COLOR = new Color(0.1f, 0.05f, 0.15f, 0.85f);
    private final Color BORDER_COLOR = new Color(0.4f, 0.2f, 0.5f, 0.7f);
    private final Color TITLE_COLOR = new Color(0.7f, 0.3f, 0.3f, 1f);
    private final Color BUTTON_COLOR = new Color(0.15f, 0.08f, 0.2f, 0.9f);
    private final Color BUTTON_HOVER_COLOR = new Color(0.25f, 0.15f, 0.3f, 0.9f);

    private Sound gameOverSound;
    private boolean soundPlayed = false;

    public EndScreen(Runnable onEndCallback, BitmapFont titleFont, BitmapFont font) {
        this.onEndCallback = onEndCallback;
        this.titleFont = titleFont;
        this.font = font;
        backgroundTexture = new Texture(Gdx.files.internal("backgrounds/black.png"));
        vignette = new Texture(Gdx.files.internal("backgrounds/vignette.png"));

        // Load âm thanh game over
        try {
            gameOverSound = Gdx.audio.newSound(Gdx.files.internal("audio/musics/Defeat.mp3"));
        } catch (Exception e) {
            System.err.println("Không thể tải âm thanh: " + e.getMessage());
        }
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        layout = new GlyphLayout();

        // Thiết lập InputProcessor
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                // Chuyển đổi tọa độ màn hình sang tọa độ world
                float mouseX = screenX;
                float mouseY = Gdx.graphics.getHeight() - screenY;

                // Kiểm tra nếu các nút được nhấn
                if (buttonAlpha >= 0.5f && (
                        (continueButtonBounds != null && continueButtonBounds.contains(mouseX, mouseY)) ||
                                (menuButtonBounds != null && menuButtonBounds.contains(mouseX, mouseY)))) {
                    if (onEndCallback != null) {
                        onEndCallback.run();
                    }
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public void render(float delta) {
        update(delta);

        // Xóa màn hình với màu tối
        Gdx.gl.glClearColor(DARK_BG.r, DARK_BG.g, DARK_BG.b, DARK_BG.a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

        batch.begin();

        // Vẽ nền với hiệu ứng chuyển động nhẹ
        float bgOffsetX = MathUtils.sin(effectTimer * 0.3f) * 10;
        float bgOffsetY = MathUtils.cos(effectTimer * 0.2f) * 10;
        batch.setColor(0.5f, 0.5f, 0.5f, 0.3f);
        batch.draw(backgroundTexture, bgOffsetX, bgOffsetY, screenWidth, screenHeight);

        // Vẽ vignette effect
        batch.setColor(1, 1, 1, 0.8f);
        batch.draw(vignette, 0, 0, screenWidth, screenHeight);

        batch.setColor(1, 1, 1, 1);
        batch.end();

        // Vẽ panel chính
        float panelWidth = screenWidth * 0.7f;
        float panelHeight = screenHeight * 0.6f;
        float panelX = (screenWidth - panelWidth) / 2;
        float panelY = screenHeight * 0.2f;

        // Vẽ hiệu ứng glow
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.3f, 0.1f, 0.4f, 0.2f * MathUtils.sin(effectTimer * 2) * 0.5f + 0.5f);
        shapeRenderer.rect(panelX - 10, panelY - 10, panelWidth + 20, panelHeight + 20);
        shapeRenderer.end();

        // Vẽ panel
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(PANEL_COLOR.r, PANEL_COLOR.g, PANEL_COLOR.b, panelAlpha * PANEL_COLOR.a);
        shapeRenderer.rect(panelX, panelY, panelWidth, panelHeight);
        shapeRenderer.end();

        // Vẽ viền panel
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(2f);
        shapeRenderer.setColor(BORDER_COLOR.r, BORDER_COLOR.g, BORDER_COLOR.b,
                panelAlpha * BORDER_COLOR.a * (0.7f + 0.3f * MathUtils.sin(effectTimer * 3)));
        shapeRenderer.rect(panelX, panelY, panelWidth, panelHeight);
        shapeRenderer.end();

        // Vẽ tiêu đề và nội dung
        batch.begin();

        // Vẽ "Game Over" với hiệu ứng scale và glow
        titleFont.setColor(TITLE_COLOR.r, TITLE_COLOR.g, TITLE_COLOR.b, alpha);
        String gameOverText = "GAME OVER";
        layout.setText(titleFont, gameOverText);
        float titleWidth = layout.width * titleScale;
        float titleHeight = layout.height * titleScale;

        // Vẽ hiệu ứng glow cho text
        titleFont.setColor(TITLE_COLOR.r, TITLE_COLOR.g, TITLE_COLOR.b, alpha * 0.5f);
        float glowSize = 2f + MathUtils.sin(effectTimer * 2) * 1f;
        for (int i = 0; i < 360; i += 45) {
            float glowX = MathUtils.cosDeg(i) * glowSize;
            float glowY = MathUtils.sinDeg(i) * glowSize;
            titleFont.draw(batch, gameOverText,
                    (screenWidth - titleWidth) / 2 + glowX,
                    screenHeight * 0.75f + titleHeight / 2 + glowY,
                    titleWidth, Align.center, false);
        }

        // Vẽ text chính
        titleFont.setColor(TITLE_COLOR);
        titleFont.draw(batch, gameOverText,
                (screenWidth - titleWidth) / 2,
                screenHeight * 0.75f + titleHeight / 2,
                titleWidth, Align.center, false);

        // Vẽ thông điệp
        font.setColor(1, 1, 1, panelAlpha * 0.9f);
        String message = "Failure has come, but the journey is not over yet...";
        font.draw(batch, message, panelX + 40, panelY + panelHeight - 60, panelWidth - 80, Align.center, true);

        batch.end();

        // Vẽ các nút
        if (panelAlpha > 0.7f) {
            drawButtons(screenWidth, screenHeight, panelX, panelY, panelWidth);
        }

    }

    private void update(float delta) {
        timer += delta;
        effectTimer += delta;

        // Animation cho các hiệu ứng
        if (timer < 1.0f) {
            alpha = Interpolation.fade.apply(timer);
        } else {
            alpha = 1.0f;
        }

        if (timer > 1.5f && timer < 3.0f) {
            panelAlpha = Interpolation.fade.apply((timer - 1.5f) / 1.5f);
        } else if (timer >= 3.0f) {
            panelAlpha = 1.0f;
        }

        if (timer > 3.0f && timer < 4.0f) {
            buttonAlpha = Interpolation.fade.apply((timer - 3.0f) / 1.0f);
        } else if (timer >= 4.0f) {
            buttonAlpha = 1.0f;
        }

        // Scale animation cho tiêu đề
        if (timer < 2.0f) {
            titleScale = 0.5f + Interpolation.bounceOut.apply(timer / 2.0f) * 0.5f;
        } else {
            titleScale = 1.0f + MathUtils.sin(effectTimer) * 0.02f;
        }

        // Phát âm thanh một lần
        if (!soundPlayed && gameOverSound != null && timer > 0.2f) {
            gameOverSound.play(0.7f);
            soundPlayed = true;
        }

        // Kiểm tra hover cho các nút
        updateButtonState();
    }

    private void drawButtons(int screenWidth, int screenHeight, float panelX, float panelY, float panelWidth) {
        float buttonWidth = 200;
        float buttonHeight = 50;
        float buttonSpacing = 20;
        float buttonsY = panelY + 60;

        // Nút "Thử lại"
        float continueButtonX = panelX + (panelWidth / 2) - buttonWidth - (buttonSpacing / 2);
        continueButtonBounds = new Rectangle(continueButtonX, buttonsY, buttonWidth, buttonHeight);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(continueButtonHovered ? BUTTON_HOVER_COLOR : BUTTON_COLOR);
        shapeRenderer.rect(continueButtonX, buttonsY, buttonWidth, buttonHeight);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(2f);
        shapeRenderer.setColor(BORDER_COLOR.r, BORDER_COLOR.g, BORDER_COLOR.b,
                buttonAlpha * (continueButtonHovered ? 1f : 0.7f));
        shapeRenderer.rect(continueButtonX, buttonsY, buttonWidth, buttonHeight);
        shapeRenderer.end();

        // Nút "Trở về Menu"
        float menuButtonX = panelX + (panelWidth / 2) + (buttonSpacing / 2);
        menuButtonBounds = new Rectangle(menuButtonX, buttonsY, buttonWidth, buttonHeight);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(menuButtonHovered ? BUTTON_HOVER_COLOR : BUTTON_COLOR);
        shapeRenderer.rect(menuButtonX, buttonsY, buttonWidth, buttonHeight);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(BORDER_COLOR.r, BORDER_COLOR.g, BORDER_COLOR.b,
                buttonAlpha * (menuButtonHovered ? 1f : 0.7f));
        shapeRenderer.rect(menuButtonX, buttonsY, buttonWidth, buttonHeight);
        shapeRenderer.end();

        // Vẽ text cho các nút
        batch.begin();
        font.setColor(1, 1, 1, buttonAlpha);

        layout.setText(font, "RETRY");
        font.draw(batch, "RETRY",
                continueButtonX + (buttonWidth - layout.width) / 2,
                buttonsY + (buttonHeight + layout.height) / 2);

        layout.setText(font, "BACK TO MENU");
        font.draw(batch, "BACK TO MENU",
                menuButtonX + (buttonWidth - layout.width) / 2,
                buttonsY + (buttonHeight + layout.height) / 2);
        batch.end();
    }

    private void updateButtonState() {
        if (buttonAlpha < 0.5f) return;

        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();

        continueButtonHovered = continueButtonBounds != null && continueButtonBounds.contains(mouseX, mouseY);
        menuButtonHovered = menuButtonBounds != null && menuButtonBounds.contains(mouseX, mouseY);
    }

    @Override
    public void resize(int width, int height) {
        // Có thể cập nhật các kích thước dựa trên kích thước màn hình mới
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (backgroundTexture != null) backgroundTexture.dispose();
        if (vignette != null) vignette.dispose();
        if (gameOverSound != null) gameOverSound.dispose();
        if (layout != null) layout = null;



    }
}