package ctu.game.isometric.view.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;

public class CreditsScreen implements Screen {
    private final Runnable onEndCallback;
    private SpriteBatch batch;
    private BitmapFont font;
    private Texture background;

    // Nội dung credits
    private final String[] creditLines;

    // Vị trí và tốc độ scroll
    private float scrollY;
    private float scrollSpeed = 60f; // pixel mỗi giây

    // Bộ đếm thời gian
    private float endDelayTimer = 0f;
    private boolean creditsFinished = false;
    private boolean callbackCalled = false;
    private final float END_DELAY = 1f; // đợi 3 giây sau khi hết credits

    public CreditsScreen(Runnable onEndCallback, BitmapFont font) {
        this.font = font;
        this.onEndCallback = onEndCallback;
        // Khởi tạo nội dung credits
        this.creditLines = new String[]{

                "Labyrinth of Wisdom",
                "",
                "",
                "A game by",
                "",
                "GAME DESIGN",
                "",
                "Nguyen Minh Khanh",
                "",
                "PROGRAMMER",
                "",
                "Nguyễn Minh Khánh",
                "",
                "TESTER",
                "",
                "Nguyen Minh Khanh",
                "Le Nhu Phung",
                "",
                "SUPERVISING INSTRUCTOR",
                "",
                "Nguyen Cong Danh",
                "",
                "DIRECTOR & HELPER",
                "",
                "Le Nhu Phung",
                "Nguyen Ngoc Xuan Nhi",
                "Trinh Phan Ke Van",
                "",
                "",
                "THANK YOU FOR PLAYING"
        };
    }

    @Override
    public void show() {
        batch = new SpriteBatch();

        // Tải background - điều chỉnh đường dẫn theo dự án
        background = new Texture(Gdx.files.internal("backgrounds/credits_bg.png"));

        // Khởi tạo vị trí bắt đầu của credits (dưới màn hình)
        scrollY = -500f;
    }

    @Override
    public void render(float delta) {
        // Xóa màn hình
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();

        // Vẽ background cố định
        batch.draw(background, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // Tính tổng chiều cao của văn bản credits
        float totalTextHeight = creditLines.length * font.getLineHeight() * 1.5f;

        // Vẽ văn bản credits cuộn
        float currentY = scrollY;
        for (String line : creditLines) {
            font.draw(batch, line, 0, currentY, Gdx.graphics.getWidth(), Align.center, false);
            currentY += font.getLineHeight() * 1.5f;
        }

        batch.end();

        // Cuộn văn bản lên trên
        scrollY += scrollSpeed * delta;

        // Kiểm tra xem credits đã cuộn hết chưa
        if (!creditsFinished && scrollY > Gdx.graphics.getHeight() + totalTextHeight) {
            creditsFinished = true;
        }

        // Nếu credits đã kết thúc, bắt đầu đếm thời gian chờ
        if (creditsFinished) {
            endDelayTimer += delta;
            if (endDelayTimer >= END_DELAY && !callbackCalled) {
                callbackCalled = true;
                if (onEndCallback != null) {
                    onEndCallback.run();
                }
            }
        }

        // Bỏ qua credits bằng phím Enter
        if (!callbackCalled && Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            callbackCalled = true;
            if (onEndCallback != null) {
                onEndCallback.run();
            }
        }
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (font != null) font.dispose();
        if (background != null) background.dispose();
    }
}