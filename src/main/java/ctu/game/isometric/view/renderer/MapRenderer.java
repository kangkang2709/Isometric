package ctu.game.isometric.view.renderer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.renderers.IsometricTiledMapRenderer;
import ctu.game.isometric.model.entity.Character;
import ctu.game.isometric.model.world.IsometricMap;
import ctu.game.isometric.util.AnimationManager;
import ctu.game.isometric.util.AssetManager;

public class MapRenderer {
    private IsometricMap map;
    private AssetManager assetManager;
    private float offsetX, offsetY;
    private AnimationManager animationManager;
    private Character character;
    private IsometricTiledMapRenderer tiledMapRenderer;
    private OrthographicCamera camera;

    // In MapRenderer.java - modify constructor to take an existing camera
    public MapRenderer(IsometricMap map, AssetManager assetManager, Character character, OrthographicCamera camera) {
        this.map = map;
        this.assetManager = assetManager;
        this.character = character;
        this.animationManager = assetManager.getAnimationManager();
        this.offsetX = 640;
        this.offsetY = 150;

        // Use the provided camera instead of creating a new one
        this.camera = camera;

        // Create the tiled map renderer
        this.tiledMapRenderer = new IsometricTiledMapRenderer(map.getTiledMap());
    }

    public float[] toIsometric(float x, float y) {
        float isoX = (x + y) * (map.getTileWidth() / 2.0f);  // Đảo cả phép tính trên X
        float isoY = (y - x) * (map.getTileHeight() / 2.0f);  // Đảo cả phép tính trên Y
        return new float[]{isoX, isoY};
    }

    public void render(SpriteBatch batch) {
        // Update camera position based on character with proper vertical offset
        float[] isoPos = toIsometric(character.getGridX(), character.getGridY());
        // Add vertical offset to camera to better position character in view
        camera.position.set(isoPos[0], isoPos[1], 0);
        camera.update();

        // End SpriteBatch (if it's begun) to use the renderer
        boolean batchWasDrawing = batch.isDrawing();
        if (batchWasDrawing) {
            batch.end();
        }

        // Render the tiled map
        tiledMapRenderer.setView(camera);
        tiledMapRenderer.render();

        // Start batch again if it was drawing before
        if (batchWasDrawing) {
            batch.begin();
            batch.setProjectionMatrix(camera.combined);
        }
    }

    public void renderWalkableTileHighlights(SpriteBatch batch, boolean[][] walkableTiles, float stateTime) {
        // Get character position
        int characterX = (int) Math.floor(character.getGridX());
        int characterY = (int) Math.floor(character.getGridY());

        // Store original color
        Color originalColor = new Color(batch.getColor());

        // Set highlight color (semi-transparent green)
        batch.setColor(0.2f, 1f, 0.2f, 0.5f);

        // Define adjacent tiles in correct drawing order for isometric depth
        int[][] adjacentTiles = {
                {characterX - 1, characterY - 1}, // left_up
                {characterX, characterY - 1},     // up
                {characterX - 1, characterY},     // left
                {characterX, characterY},         // center
                {characterX + 1, characterY - 1}, // right_up
                {characterX - 1, characterY + 1}, // left_down
                {characterX + 1, characterY},     // right
                {characterX, characterY + 1},     // down
                {characterX + 1, characterY + 1}  // right_down
        };

        // Sort by isometric depth (higher x+y is further back, then by y)
        java.util.Arrays.sort(adjacentTiles, (a, b) -> {
            int sumComparison = Integer.compare(a[0] + a[1], b[0] + b[1]);
            if (sumComparison != 0) {
                return sumComparison;
            }
            return Integer.compare(a[1], b[1]); // Compare y-values for tiles with the same sum
        });

        // Get the base layer for highlighting
        TiledMapTileLayer baseLayer = (TiledMapTileLayer) map.getTiledMap().getLayers().get(0);

        // Ensure walkableTiles array matches map dimensions
        if (walkableTiles != null &&
                walkableTiles.length == map.getMapHeight() &&
                walkableTiles[0].length == map.getMapWidth()) {

            for (int[] tile : adjacentTiles) {
                int x = tile[0];
                int y = tile[1];

                // Check if tile is within bounds and walkable
                if (x >= 0 && x < walkableTiles[0].length &&
                        y >= 0 && y < walkableTiles.length &&
                        walkableTiles[y][x]) {

                    TiledMapTileLayer.Cell cell = baseLayer.getCell(x, y);
                    if (cell != null) {
                        TiledMapTile mapTile = cell.getTile();
                        if (mapTile != null) {
                            TextureRegion tileRegion = mapTile.getTextureRegion();
                            float[] iso = toIsometric(x, y);

                            // Draw at the center position, adjusting for tile dimensions
                            float tileWidth = map.getTileWidth();
                            float tileHeight = map.getTileHeight();
                            batch.draw(tileRegion,
                                    iso[0],
                                    iso[1],
                                    tileWidth,
                                    tileHeight);
                        }
                    }
                }
            }
        }

        // Restore original color
        batch.setColor(originalColor);
    }

    public float getOffsetX() {
        return offsetX;
    }

    public float getOffsetY() {
        return offsetY;
    }

    public IsometricMap getMap() {
        return map;
    }

    public void setMap(IsometricMap map) {
        this.map = map;
    }
}