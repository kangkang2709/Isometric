package ctu.game.isometric.view.renderer;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import ctu.game.isometric.model.world.IsometricMap;
import ctu.game.isometric.util.AssetManager;

public class MapRenderer {
    private IsometricMap map;
    private AssetManager assetManager;
    private float offsetX, offsetY;

    public MapRenderer(IsometricMap map, AssetManager assetManager) {
        this.map = map;
        this.assetManager = assetManager;
        this.offsetX = 640; // Half of 1280
        this.offsetY = 150;
    }

    public void render(SpriteBatch batch) {
        Texture tileTexture = assetManager.getTexture(map.getTileTexturePath());
        int[][] mapData = map.getMapData();

        for (int y = 0; y < mapData.length; y++) {
            for (int x = 0; x < mapData[y].length; x++) {
                if (mapData[y][x] == 1) {
                    // Convert to isometric coordinates
                    float isoX = (x - y) * map.getTileWidth() / 2f + offsetX;
                    float isoY = (x + y) * map.getTileHeight() / 2f + offsetY;

                    batch.draw(tileTexture, isoX, isoY);
                }
            }
        }
    }

    public float getOffsetX() { return offsetX; }
    public float getOffsetY() { return offsetY; }
}