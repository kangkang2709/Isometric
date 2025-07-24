package ctu.game.isometric.view.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;

public class CombatEnvironment3D {
    private ModelBatch modelBatch;
    private Environment environment;
    private PerspectiveCamera camera3D;
    private ModelInstance floorInstance;
    private ModelInstance[] wallInstances;

    public CombatEnvironment3D() {
        setupCamera();
        setupLighting();
        loadModels();
    }

    private void setupCamera() {
        camera3D = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera3D.position.set(0f, 8f, 12f);
        camera3D.lookAt(0f, 0f, 0f);
        camera3D.near = 1f;
        camera3D.far = 300f;
        camera3D.update();
    }

    private void setupLighting() {
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));
    }

    private void loadModels() {
        ModelBuilder modelBuilder = new ModelBuilder();
        modelBatch = new ModelBatch();

        // Tạo sàn 3D
        // Load texture (đảm bảo file tồn tại trong assets)
        Texture floorTexture = new Texture(Gdx.files.internal("ui/grass.png"));
        floorTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);

// Create material with texture
        Material floorMaterial = new Material(TextureAttribute.createDiffuse(floorTexture));

// Create the floor model using the textured material
        Model floorModel = modelBuilder.createBox(
                50f, 0.5f, 15f,
                floorMaterial,
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates
        );

// Create instance
        floorInstance = new ModelInstance(floorModel);

        floorInstance.transform.setToTranslation(0, -0.25f, 0);

        // Tạo tường backdrop
//        wallInstances = new ModelInstance[3];
//        Model wallModel = modelBuilder.createBox(20f, 12f, 0.5f,
//                new Material(ColorAttribute.createDiffuse(0.2f, 0.15f, 0.1f, 1f)),
//                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
//
////        wallInstances[0] = new ModelInstance(wallModel);
////        wallInstances[0].transform.setToTranslation(0, 6f, -7.5f);
////
////        // Tường bên trái
////        Model sideWallModel = modelBuilder.createBox(0.5f, 12f, 15f,
////                new Material(ColorAttribute.createDiffuse(0.15f, 0.1f, 0.08f, 1f)),
////                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
////
////        wallInstances[1] = new ModelInstance(sideWallModel);
////        wallInstances[1].transform.setToTranslation(0f, 6f, 0);
////
////        wallInstances[2] = new ModelInstance(sideWallModel);
////        wallInstances[2].transform.setToTranslation(0f, 6f, 0);
    }

    public void render() {
        modelBatch.begin(camera3D);
        modelBatch.render(floorInstance, environment);
        if( wallInstances != null) {
            for (ModelInstance wall : wallInstances) {
                modelBatch.render(wall, environment);
            }
        }

        modelBatch.end();
    }

    public void update(float delta) {
        camera3D.update();
    }

    public PerspectiveCamera getCamera() {
        return camera3D;
    }

    public void dispose() {
        modelBatch.dispose();
        floorInstance.model.dispose();
        for (ModelInstance wall : wallInstances) {
            wall.model.dispose();
        }
    }
}