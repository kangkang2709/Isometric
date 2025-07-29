package ctu.game.isometric.view.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import ctu.game.isometric.model.game.Items;
import ctu.game.isometric.model.game.Reward;
import ctu.game.isometric.util.AssetManager;

public class RewardRenderer extends UIRenderer {
    private Items item;
    private Reward reward;
    private AssetManager assetManager;
    private Rectangle continueButtonBounds;
    private float animationTime = 0f;
    private float glowIntensity = 0f;
    private boolean buttonHovered = false;
    private GlyphLayout layout = new GlyphLayout();

    // Màu sắc theo phong cách FF7 Remake
    private final Color PANEL_COLOR = new Color(0.05f, 0.1f, 0.2f, 0.85f);
    private final Color BORDER_COLOR = new Color(0.4f, 0.6f, 0.9f, 0.7f);
    private final Color TITLE_COLOR = new Color(1f, 0.95f, 0.75f, 1f);
    private final Color BUTTON_COLOR = new Color(0.2f, 0.3f, 0.5f, 1f);
    private final Color BUTTON_HOVER_COLOR = new Color(0.3f, 0.4f, 0.7f, 1f);
    private final Color ITEM_GLOW = new Color(1f, 0.9f, 0.4f, 0.7f);

    public RewardRenderer(SpriteBatch batch, BitmapFont font, BitmapFont titleFont,
                          BitmapFont inputFont, ShapeRenderer shapeRenderer, AssetManager assetManager) {
        super(batch, font, titleFont, inputFont, shapeRenderer);
        this.assetManager = assetManager;
    }

    public void setReward(Items item, Reward reward) {
        this.item = item;
        this.reward = reward;
        this.animationTime = 0f;
    }

    public void update(float delta) {
        // Cập nhật animation
        animationTime += delta;
        glowIntensity = 0.5f + 0.5f * MathUtils.sin(animationTime * 3f);
    }

    @Override
    public void render() {
        update(Gdx.graphics.getDeltaTime());

        float panelWidth = 700, panelHeight = 450;
        float panelX = (SCREEN_WIDTH - panelWidth) / 2;
        float panelY = (SCREEN_HEIGHT - panelHeight) / 2;

        // Vẽ hiệu ứng glow cho nền
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(BORDER_COLOR.r, BORDER_COLOR.g, BORDER_COLOR.b, 0.2f * glowIntensity);
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
        layout.setText(titleFont, "CHIẾN THẮNG");
        titleFont.draw(batch, "CHIẾN THẮNG",
                panelX + (panelWidth - layout.width) / 2,
                panelY + panelHeight - 40);

        // Hiển thị hình ảnh vật phẩm với hiệu ứng
        if (item != null) {
            Texture itemTexture = assetManager.loadTexture(item.getItemName(), item.getTexturePath());
            if (itemTexture != null) {
                // Vẽ hiệu ứng glow cho vật phẩm
                float itemX = panelX + 150;
                float itemY = panelY + panelHeight/2 - 50;
                float itemSize = 96 + 8 * glowIntensity; // Kích thước thay đổi nhẹ theo animation

                // Vẽ glow
                batch.setColor(ITEM_GLOW.r, ITEM_GLOW.g, ITEM_GLOW.b, ITEM_GLOW.a * glowIntensity);
                batch.draw(itemTexture,
                        itemX - (itemSize-96)/2, itemY - (itemSize-96)/2,
                        itemSize, itemSize);

                // Vẽ vật phẩm
                batch.setColor(Color.WHITE);
                batch.draw(itemTexture, itemX, itemY, 96, 96);
            }

            // Thông tin vật phẩm
            font.setColor(Color.GOLD);
            font.draw(batch, item.getItemName() + " x" + reward.getAmount(),
                    panelX + 280, panelY + panelHeight/2 + 40);

            font.setColor(Color.WHITE);
            // Chia nhỏ mô tả thành nhiều dòng nếu cần
            String description = reward.getDescription();
            float maxWidth = 350;
            layout.setText(font, description);

            if (layout.width > maxWidth) {
                String[] words = description.split(" ");
                StringBuilder currentLine = new StringBuilder();
                float y = panelY + panelHeight/2;

                for (String word : words) {
                    layout.setText(font, currentLine + " " + word);
                    if (layout.width > maxWidth && currentLine.length() > 0) {
                        font.draw(batch, currentLine.toString(), panelX + 280, y);
                        y -= font.getLineHeight() + 5;
                        currentLine = new StringBuilder(word);
                    } else {
                        if (currentLine.length() > 0) currentLine.append(" ");
                        currentLine.append(word);
                    }
                }

                if (currentLine.length() > 0) {
                    font.draw(batch, currentLine.toString(), panelX + 280, y);
                }
            } else {
                font.draw(batch, description, panelX + 280, panelY + panelHeight/2);
            }
        }

        batch.end();

        // Vẽ nút tiếp tục
        float buttonWidth = 220, buttonHeight = 60;
        float buttonX = SCREEN_WIDTH / 2 - buttonWidth / 2;
        float buttonY = panelY + 60;
        continueButtonBounds = new Rectangle(buttonX, buttonY, buttonWidth, buttonHeight);

        // Kiểm tra hover
        if (Gdx.input.getX() >= buttonX && Gdx.input.getX() <= buttonX + buttonWidth &&
                SCREEN_HEIGHT - Gdx.input.getY() >= buttonY && SCREEN_HEIGHT - Gdx.input.getY() <= buttonY + buttonHeight) {
            buttonHovered = true;
        } else {
            buttonHovered = false;
        }

        // Vẽ nút với hiệu ứng hover
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

    public void dispose() {
        // Không nên dispose assetManager tại đây vì có thể được sử dụng ở nơi khác
    }
}