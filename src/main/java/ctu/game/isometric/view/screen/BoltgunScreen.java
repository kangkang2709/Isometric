package ctu.game.isometric.view.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
import com.badlogic.gdx.utils.UBJsonReader;

public class BoltgunScreen implements Screen {
    PerspectiveCamera camera;
    ModelBatch modelBatch;
    Model playerWeaponModel, enemyModel;
    ModelInstance weaponInstance, enemyInstance;

    float time = 0;

    @Override
    public void show() {
        // Camera setup
        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(0f, 1.5f, 0f);
        camera.lookAt(0f, 1.5f, 10f);
        camera.up.set(0, 1, 0);
        camera.near = 0.1f;
        camera.far = 100f;
        camera.update();

        modelBatch = new ModelBatch();

        // Player weapon (simple box)

        // Player weapon (simple box)
        ObjLoader objLoader = new ObjLoader();
        FileHandle fileHandle = Gdx.files.internal("enemies/hands.obj");

        playerWeaponModel = objLoader.loadModel(fileHandle);
        weaponInstance = new ModelInstance(playerWeaponModel);
        weaponInstance.transform.setToTranslation(0f, 1.2f, 10f);

// Load enemy model from file
        objLoader = new ObjLoader();
        fileHandle = Gdx.files.internal("enemies/model.obj");
        enemyModel = objLoader.loadModel(fileHandle);
        enemyInstance = new ModelInstance(enemyModel);
        enemyInstance.transform.setToTranslation(0f, 1f, 10f);
    }


    @Override
    public void render(float delta) {
        time += delta;

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);

        modelBatch.begin(camera);
        modelBatch.render(weaponInstance);
        modelBatch.render(enemyInstance);
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

    @Override
    public void dispose() {
        modelBatch.dispose();
        playerWeaponModel.dispose();
        enemyModel.dispose();
    }
}