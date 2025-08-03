package ctu.game.isometric.view.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Interpolation;
import ctu.game.isometric.IsometricGame;
import ctu.game.isometric.animation.DissolveShaderManager;
import ctu.game.isometric.controller.GameController;

import static ctu.game.isometric.IsometricGame.getGameController;

public class SplashScreen implements Screen {
    private final IsometricGame game;
    private SpriteBatch batch;
    private Texture splashTexture;
    private Texture logoTexture;
    private float timer = 0;
    private final float SPLASH_DURATION = 4.0f; // Tăng thời gian để hiển thị đầy đủ hiệu ứng

    // Logo dissolve effect parameters
    private final float LOGO_DISSOLVE_DURATION = 2.0f; // Thời gian dissolve effect
    private final float LOGO_DISPLAY_DURATION = 1.5f; // Thời gian hiển thị logo sau khi dissolve xong
    private float logoAlpha = 0f;
    private float logoScale = 1f;
    private boolean logoDissolveComplete = false;

    public SplashScreen(IsometricGame game) {
        this.game = game;
        batch = new SpriteBatch();
        splashTexture = new Texture(Gdx.files.internal("backgrounds/black.png"));
        logoTexture = new Texture(Gdx.files.internal("ui/logo.png")); // Thêm logo của game

        // Khởi tạo DissolveShaderManager nếu chưa có
        if (DissolveShaderManager.getDissolveShader() == null) {
            DissolveShaderManager.initialize();
        }
    }

    @Override
    public void render(float delta) {
        // Clear screen
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Update timer và dissolve shader
        timer += delta;
        DissolveShaderManager.update(delta);

        batch.begin();

        // Vẽ background splash
        batch.draw(splashTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // Vẽ logo với dissolve effect
        renderLogoWithDissolveEffect();

        batch.end();

        // Chuyển sang GameScreen sau khi hoàn tất
        if (timer >= SPLASH_DURATION) {
            GameScreen gameScreen = new GameScreen(game, getGameController());
            game.setGameScreen(gameScreen);
            game.setScreen(gameScreen);
            dispose();
        }
    }

    private void renderLogoWithDissolveEffect() {
        if (logoTexture == null) return;

        // Tính toán vị trí logo ở giữa màn hình
        float logoWidth = 500;
        float logoHeight = 150;
        float logoX = (Gdx.graphics.getWidth() - logoWidth) / 2;
        float logoY = (Gdx.graphics.getHeight() - logoHeight) / 2 + 50; // Hơi lệch lên trên

        // Giai đoạn dissolve effect (2 giây đầu)
        if (timer <= LOGO_DISSOLVE_DURATION && !logoDissolveComplete) {
            float dissolveProgress = timer / LOGO_DISSOLVE_DURATION;
            float dissolveAmount = Interpolation.fade.apply(dissolveProgress);

            // Hiệu ứng scale nhẹ khi xuất hiện
            logoScale = 0.8f + dissolveProgress * 0.2f;
            logoAlpha = 1.0f;

            // Lưu shader gốc
            ShaderProgram originalShader = batch.getShader();

            // Áp dụng dissolve shader
            batch.setShader(DissolveShaderManager.getDissolveShader());
            ShaderProgram shader = batch.getShader();

            // Set shader uniforms cho hiệu ứng xuất hiện
            shader.setUniformf("u_dissolveAmount", dissolveAmount);
            shader.setUniformf("u_dissolveEdgeWidth", 0.25f);
            shader.setUniform3fv("u_dissolveEdgeColor", new float[]{0.2f, 0.8f, 1.0f}, 0, 3); // Màu xanh dương nhẹ
            shader.setUniformf("u_time", DissolveShaderManager.getShaderTime());
            shader.setUniformf("u_intensity", 1.5f);
            shader.setUniformf("u_edgeSharpness", 0.5f);
            shader.setUniform2fv("u_dissolveDirection", new float[]{0.0f, 1.0f}, 0, 2);

            // Bind noise texture
            DissolveShaderManager.getNoiseTexture().bind(1);
            shader.setUniformi("u_dissolveTexture", 1);

            // Bind burn texture nếu có
            Texture burnOverlay = DissolveShaderManager.getBurnTexture();
            if (burnOverlay != null) {
                burnOverlay.bind(2);
                shader.setUniformi("u_burnTexture", 2);
                shader.setUniformi("u_useBurnTexture", 1);
            } else {
                shader.setUniformi("u_useBurnTexture", 0);
            }

            // Bind main texture
            logoTexture.bind(0);
            shader.setUniformi("u_texture", 0);

            // Vẽ logo với shader
            batch.setColor(1, 1, 1, logoAlpha);
            batch.draw(logoTexture, logoX, logoY, logoWidth, logoHeight);

            // Khôi phục shader gốc
            batch.setShader(originalShader);

            // Đánh dấu hoàn thành dissolve khi đến 95%
            if (dissolveProgress >= 0.95f) {
                logoDissolveComplete = true;
            }
        }
        // Giai đoạn hiển thị bình thường sau dissolve
        else if (timer <= LOGO_DISSOLVE_DURATION + LOGO_DISPLAY_DURATION) {
            float displayProgress = (timer - LOGO_DISSOLVE_DURATION) / LOGO_DISPLAY_DURATION;

            // Hiệu ứng glow nhẹ
            logoAlpha = 1.0f;
            logoScale = 0;

            batch.setColor(1, 1, 1, logoAlpha);
            batch.draw(logoTexture, logoX, logoY, logoWidth, logoHeight);
        }
        // Giai đoạn fade out cuối
        else {
            float fadeProgress = (timer - LOGO_DISSOLVE_DURATION - LOGO_DISPLAY_DURATION) /
                    (SPLASH_DURATION - LOGO_DISSOLVE_DURATION - LOGO_DISPLAY_DURATION);
            logoAlpha = Math.max(0, 1.0f - fadeProgress);

            if (logoAlpha > 0) {
                batch.setColor(1, 1, 1, logoAlpha);
                batch.draw(logoTexture, logoX, logoY, logoWidth, logoHeight);
            }
        }

        // Reset color
        batch.setColor(1, 1, 1, 1);
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void show() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        batch.dispose();
        splashTexture.dispose();
        if (logoTexture != null) {
            logoTexture.dispose();
        }
    }
}