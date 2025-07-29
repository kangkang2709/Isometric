package ctu.game.isometric.view.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;

public class DefeatRenderer extends UIRenderer {
    private boolean isEnded;
    private Rectangle continueButtonBounds;
    private float animationTime = 0f;
    private float glowIntensity = 0f;
    private boolean buttonHovered = false;
    private GlyphLayout layout = new GlyphLayout();

    // Màu sắc theo phong cách FF7 Remake
    private final Color PANEL_COLOR = new Color(0.15f, 0.05f, 0.05f, 0.85f);
    private final Color BORDER_COLOR = new Color(0.8f, 0.3f, 0.3f, 0.7f);
    private final Color TITLE_COLOR = new Color(1f, 0.4f, 0.4f, 1f);
    private final Color BUTTON_COLOR = new Color(0.3f, 0.1f, 0.1f, 1f);
    private final Color BUTTON_HOVER_COLOR = new Color(0.5f, 0.2f, 0.2f, 1f);
    private final Color DEFEAT_GLOW = new Color(0.7f, 0.2f, 0.2f, 0.5f);

    public DefeatRenderer(SpriteBatch batch, BitmapFont font, BitmapFont titleFont,
                          BitmapFont inputFont, ShapeRenderer shapeRenderer) {
        super(batch, font, titleFont, inputFont, shapeRenderer);
    }

    public void setEnded(boolean isEnded) {
        this.isEnded = isEnded;
    }

    public void update(float delta) {
        // Cập nhật animation
        animationTime += delta;
        glowIntensity = 0.5f + 0.5f * MathUtils.sin(animationTime * 2.5f);
    }

    @Override
    public void render() {
        update(Gdx.graphics.getDeltaTime());

        float panelWidth = 600, panelHeight = 400;
        float panelX = (SCREEN_WIDTH - panelWidth) / 2;
        float panelY = (SCREEN_HEIGHT - panelHeight) / 2;

        // Vẽ hiệu ứng glow cho nền
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(DEFEAT_GLOW.r, DEFEAT_GLOW.g, DEFEAT_GLOW.b, 0.3f * glowIntensity);
        shapeRenderer.rect(panelX - 15, panelY - 15, panelWidth + 30, panelHeight + 30);
        shapeRenderer.end();

        // Vẽ nền panel
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(PANEL_COLOR);
        shapeRenderer.rect(panelX, panelY, panelWidth, panelHeight);
        shapeRenderer.end();

        // Vẽ viền panel
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(3f);
        shapeRenderer.setColor(BORDER_COLOR.r, BORDER_COLOR.g, BORDER_COLOR.b, BORDER_COLOR.a * glowIntensity);
        shapeRenderer.rect(panelX, panelY, panelWidth, panelHeight);
        shapeRenderer.end();

        batch.begin();
        // Vẽ tiêu đề
        titleFont.setColor(TITLE_COLOR);
        layout.setText(titleFont, "BẠN ĐÃ BỊ HẠ GỤC!");
        titleFont.draw(batch, "BẠN ĐÃ BỊ HẠ GỤC!",
                panelX + (panelWidth - layout.width) / 2,
                panelY + panelHeight - 40);

        // Draw defeat message
        if (!isEnded) {
            font.setColor(Color.WHITE);
            String message = "Bạn còn cơ hội, Cleric Klein đã đưa bạn về để chữa trị.";
            layout.setText(font, message);
            font.draw(batch, message,
                    panelX + (panelWidth - layout.width) / 2,
                    panelY + panelHeight / 2 + 30);
        }
        batch.end();

        // Vẽ nút tiếp tục
        float buttonWidth = 200, buttonHeight = 50;
        float buttonX = SCREEN_WIDTH / 2 - buttonWidth / 2;
        float buttonY = panelY + 50;
        continueButtonBounds = new Rectangle(buttonX, buttonY, buttonWidth, buttonHeight);

        // Kiểm tra hover
        if (Gdx.input.getX() >= buttonX && Gdx.input.getX() <= buttonX + buttonWidth &&
                SCREEN_HEIGHT - Gdx.input.getY() >= buttonY && SCREEN_HEIGHT - Gdx.input.getY() <= buttonY + buttonHeight) {
            buttonHovered = true;
        } else {
            buttonHovered = false;
        }

        // Vẽ nút với hiệu ứng hover
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(buttonHovered ? BUTTON_HOVER_COLOR : BUTTON_COLOR);
        shapeRenderer.rect(buttonX, buttonY, buttonWidth, buttonHeight);
        shapeRenderer.end();

        // Viền nút
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(2f);
        shapeRenderer.setColor(BORDER_COLOR.r, BORDER_COLOR.g, BORDER_COLOR.b,
                buttonHovered ? 1f : 0.7f * glowIntensity);
        shapeRenderer.rect(buttonX, buttonY, buttonWidth, buttonHeight);
        shapeRenderer.end();

        batch.begin();
        font.setColor(Color.WHITE);
        layout.setText(font, "Tiếp tục");
        font.draw(batch, "Tiếp tục",
                buttonX + (buttonWidth - layout.width) / 2,
                buttonY + (buttonHeight + layout.height) / 2);
        batch.end();
    }

    public Rectangle getContinueButtonBounds() {
        return continueButtonBounds;
    }
}