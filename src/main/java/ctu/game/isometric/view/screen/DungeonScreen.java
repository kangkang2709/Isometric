package ctu.game.isometric.view.screen;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;

public class DungeonScreen implements Screen {

    PerspectiveCamera camera;
    ModelBatch modelBatch;
    Model model;
    ModelInstance modelInstance;

    float time;

    @Override
    public void show() {
        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(30f, 30f, 30f);  // Camera nhìn chéo từ xa
        camera.lookAt(0, 0, 0);
        camera.up.set(0, 0, 1);              // Trục Z là lên
        camera.near = 1f;
        camera.far = 300f;
        camera.update();

        modelBatch = new ModelBatch();

        ModelBuilder builder = new ModelBuilder();
        model = builder.createCylinder(
                10f, 20f, 10f, 32,
                new Material(ColorAttribute.createDiffuse(Color.BLUE)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal
        );

        modelInstance = new ModelInstance(model);
        modelInstance.transform.setToTranslation(0, 0, 0); // Vị trí gốc
    }


    @Override
    public void render(float delta) {
        time += delta;

        // Xoay enemy để test animation
        modelInstance.transform.setToRotation(0, 0, 1, time * 45);
        modelInstance.transform.trn(0, 0, 0);

        // Bắt buộc bật depth testing
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthFunc(GL20.GL_LEQUAL);

        // Clear màn hình + depth buffer
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        modelBatch.begin(camera);
        modelBatch.render(modelInstance);
        modelBatch.end();
    }


    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {
        modelBatch.dispose();
        model.dispose();
    }
}

