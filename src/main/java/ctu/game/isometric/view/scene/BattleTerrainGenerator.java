package ctu.game.isometric.view.scene;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;

public class BattleTerrainGenerator {
    private static final int TERRAIN_SIZE = 32;
    private static final float TERRAIN_SCALE = 0.8f;
    private static final float HEIGHT_SCALE = 1.5f;

    private Model terrainModel;
    private ModelInstance terrainInstance;
    private Texture grassTexture;
    private float[][] heightMap;

    public BattleTerrainGenerator() {
        generateHeightMap();
        loadTextures();
        generateTerrain();
    }

    private void generateHeightMap() {
        heightMap = new float[TERRAIN_SIZE][TERRAIN_SIZE];
        for (int x = 0; x < TERRAIN_SIZE; x++) {
            for (int z = 0; z < TERRAIN_SIZE; z++) {
                heightMap[x][z] = generateNoise(x * 0.1f, z * 0.1f) * HEIGHT_SCALE;
            }
        }
    }

    private float generateNoise(float x, float y) {
        return (float) (Math.sin(x * 2) * Math.cos(y * 1.5) * 0.3 +
                Math.sin(x * 4) * Math.cos(y * 3) * 0.1);
    }

    private void loadTextures() {
        grassTexture = new Texture("ui/grass.png");
        grassTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
    }

    private void generateTerrain() {
        ModelBuilder modelBuilder = new ModelBuilder();
        MeshBuilder meshBuilder = new MeshBuilder();

        meshBuilder.begin(VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates,
                GL20.GL_TRIANGLES);

        for (int x = 0; x < TERRAIN_SIZE - 1; x++) {
            for (int z = 0; z < TERRAIN_SIZE - 1; z++) {
                float h00 = heightMap[x][z];
                float h10 = heightMap[x + 1][z];
                float h01 = heightMap[x][z + 1];
                float h11 = heightMap[x + 1][z + 1];

                float x0 = (x - TERRAIN_SIZE / 2f) * TERRAIN_SCALE;
                float x1 = ((x + 1) - TERRAIN_SIZE / 2f) * TERRAIN_SCALE;
                float z0 = (z - TERRAIN_SIZE / 2f) * TERRAIN_SCALE;
                float z1 = ((z + 1) - TERRAIN_SIZE / 2f) * TERRAIN_SCALE;

                Vector3 normal = new Vector3(0, 1, 0);

                short i1 = meshBuilder.vertex(x0, h00, z0, normal.x, normal.y, normal.z, 0, 0);
                short i2 = meshBuilder.vertex(x1, h10, z0, normal.x, normal.y, normal.z, 1, 0);
                short i3 = meshBuilder.vertex(x1, h11, z1, normal.x, normal.y, normal.z, 1, 1);
                short i4 = meshBuilder.vertex(x0, h01, z1, normal.x, normal.y, normal.z, 0, 1);

                meshBuilder.triangle(i1, i2, i4);
                meshBuilder.triangle(i2, i3, i4);
            }
        }

        Mesh terrainMesh = meshBuilder.end();

        Material material = new Material();
        if (grassTexture != null) {
            material.set(TextureAttribute.createDiffuse(grassTexture));
        } else {
            material.set(ColorAttribute.createDiffuse(0.3f, 0.7f, 0.3f, 1f));
        }

        modelBuilder.begin();
        modelBuilder.part("terrain", terrainMesh, GL20.GL_TRIANGLES, material);
        terrainModel = modelBuilder.end();
        terrainInstance = new ModelInstance(terrainModel);
    }

    public ModelInstance getTerrainInstance() {
        return terrainInstance;
    }

    public void dispose() {
        if (terrainModel != null) terrainModel.dispose();
        if (grassTexture != null) grassTexture.dispose();
    }
}