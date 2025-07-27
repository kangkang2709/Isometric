package ctu.game.isometric.view.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;

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

        // Tạo terrain từ heightmap
        createHeightmapTerrain(modelBuilder);
    }

    private void createHeightmapTerrain(ModelBuilder modelBuilder) {
        // Load heightmap texture (grayscale image)
        Texture heightmapTexture = new Texture(Gdx.files.internal("terrain/heightmap1.png"));
        Pixmap heightmapPixmap = new Pixmap(Gdx.files.internal("terrain/heightmap1.png"));

        // Load diffuse texture for terrain
        Texture terrainTexture = new Texture(Gdx.files.internal("ui/grass.png"));
        terrainTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);

        // Terrain parameters
        int terrainWidth = 64;  // Number of vertices in width
        int terrainDepth = 64;  // Number of vertices in depth
        float terrainScale = 50f; // World size scale
        float heightScale = 8f;   // Maximum height variation

        // Create vertices and indices for terrain mesh
        float[] vertices = createTerrainVertices(heightmapPixmap, terrainWidth, terrainDepth, terrainScale, heightScale);
        short[] indices = createTerrainIndices(terrainWidth, terrainDepth);

        // Create mesh
        // Create mesh
        Mesh terrainMesh = new Mesh(true, vertices.length / 8, indices.length,
                new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
                new VertexAttribute(VertexAttributes.Usage.Normal, 3, "a_normal"),
                new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord0"));

        terrainMesh.setVertices(vertices);
        terrainMesh.setIndices(indices);

// Create material
        Material terrainMaterial = new Material(TextureAttribute.createDiffuse(terrainTexture));

// Create model from mesh
        modelBuilder.begin();
        modelBuilder.part("terrain", terrainMesh, GL20.GL_TRIANGLES, terrainMaterial);
        Model terrainModel = modelBuilder.end();
        floorInstance = new ModelInstance(terrainModel);

// Cleanup
        heightmapPixmap.dispose();
    }


    private float[] createTerrainVertices(Pixmap heightmap, int width, int depth, float scale, float heightScale) {
        float[] vertices = new float[width * depth * 8]; // position(3) + normal(3) + texCoord(2)
        int vertexIndex = 0;

        for (int z = 0; z < depth; z++) {
            for (int x = 0; x < width; x++) {
                // Calculate world position
                float worldX = (x / (float)(width - 1) - 0.5f) * scale;
                float worldZ = (z / (float)(depth - 1) - 0.5f) * scale;

                // Sample height from heightmap
                int pixelX = (int)((x / (float)(width - 1)) * (heightmap.getWidth() - 1));
                int pixelZ = (int)((z / (float)(depth - 1)) * (heightmap.getHeight() - 1));
                int pixel = heightmap.getPixel(pixelX, pixelZ);
                float height = ((pixel & 0xff) / 255f) * heightScale;

                // Position
                vertices[vertexIndex++] = worldX;
                vertices[vertexIndex++] = height;
                vertices[vertexIndex++] = worldZ;

                // Calculate normal (simplified - you might want to improve this)
                Vector3 normal = calculateNormal(heightmap, x, z, width, depth, heightScale);
                vertices[vertexIndex++] = normal.x;
                vertices[vertexIndex++] = normal.y;
                vertices[vertexIndex++] = normal.z;

                // Texture coordinates
                vertices[vertexIndex++] = x / (float)(width - 1) * 4f; // Repeat texture 4 times
                vertices[vertexIndex++] = z / (float)(depth - 1) * 4f;
            }
        }

        return vertices;
    }

    private Vector3 calculateNormal(Pixmap heightmap, int x, int z, int width, int depth, float heightScale) {
        // Sample neighboring heights for normal calculation
        float heightL = getHeight(heightmap, x - 1, z, width, depth, heightScale);
        float heightR = getHeight(heightmap, x + 1, z, width, depth, heightScale);
        float heightD = getHeight(heightmap, x, z - 1, width, depth, heightScale);
        float heightU = getHeight(heightmap, x, z + 1, width, depth, heightScale);

        Vector3 normal = new Vector3(heightL - heightR, 2.0f, heightD - heightU);
        normal.nor();
        return normal;
    }

    private float getHeight(Pixmap heightmap, int x, int z, int width, int depth, float heightScale) {
        // Clamp coordinates
        x = Math.max(0, Math.min(width - 1, x));
        z = Math.max(0, Math.min(depth - 1, z));

        int pixelX = (int)((x / (float)(width - 1)) * (heightmap.getWidth() - 1));
        int pixelZ = (int)((z / (float)(depth - 1)) * (heightmap.getHeight() - 1));
        int pixel = heightmap.getPixel(pixelX, pixelZ);
        return ((pixel & 0xff) / 255f) * heightScale;
    }

    private short[] createTerrainIndices(int width, int depth) {
        short[] indices = new short[(width - 1) * (depth - 1) * 6];
        int index = 0;

        for (int z = 0; z < depth - 1; z++) {
            for (int x = 0; x < width - 1; x++) {
                int topLeft = z * width + x;
                int topRight = topLeft + 1;
                int bottomLeft = (z + 1) * width + x;
                int bottomRight = bottomLeft + 1;

                // First triangle
                indices[index++] = (short)topLeft;
                indices[index++] = (short)bottomLeft;
                indices[index++] = (short)topRight;

                // Second triangle
                indices[index++] = (short)topRight;
                indices[index++] = (short)bottomLeft;
                indices[index++] = (short)bottomRight;
            }
        }

        return indices;
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