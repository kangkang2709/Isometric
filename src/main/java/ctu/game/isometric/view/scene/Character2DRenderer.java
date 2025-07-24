package ctu.game.isometric.view.scene;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class Character2DRenderer {
    private SpriteBatch spriteBatch;
    private OrthographicCamera camera2D;
    private Matrix4 projectionMatrix3D;

    public Character2DRenderer(PerspectiveCamera camera3D) {
        spriteBatch = new SpriteBatch();
        camera2D = new OrthographicCamera();
        camera2D.setToOrtho(false, 1280, 720);

        // Lưu projection matrix 3D để tính toán vị trí 2D
        projectionMatrix3D = camera3D.combined.cpy();
    }

    public Vector2 worldToScreen(Vector3 worldPos, PerspectiveCamera camera3D) {
        Vector3 screenPos = camera3D.project(worldPos.cpy());
        return new Vector2(screenPos.x, screenPos.y);
    }

    public void renderCharacter(Texture texture, Vector3 worldPosition,
                                PerspectiveCamera camera3D, float scale) {
        Vector2 screenPos = worldToScreen(worldPosition, camera3D);

        spriteBatch.begin();
        float width = 128 * scale;
        float height = 128 * scale;
        spriteBatch.draw(texture,
                screenPos.x - width/2 , screenPos.y - height/2,
                width, height);
        spriteBatch.end();
    }

    public void dispose() {
        spriteBatch.dispose();
    }
}