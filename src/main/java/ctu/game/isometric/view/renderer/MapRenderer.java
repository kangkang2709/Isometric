package ctu.game.isometric.view.renderer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import ctu.game.isometric.model.world.IsometricMap;
import ctu.game.isometric.util.AnimationManager;
import ctu.game.isometric.util.AssetManager;
import ctu.game.isometric.model.entity.Character;

import java.util.ArrayList;
import java.util.List;

public class MapRenderer {
    private IsometricMap map;
    private AssetManager assetManager;
    private float offsetX, offsetY;
    private AnimationManager animationManager;
    private Character character;

    public MapRenderer(IsometricMap map, AssetManager assetManager, Character character) {
        this.map = map;
        this.assetManager = assetManager;
        this.character = character;
        this.animationManager = assetManager.getAnimationManager();
        this.offsetX = 640; // Half of 1280
        this.offsetY = 150;
    }

    public void render(SpriteBatch batch) {
        Texture tileTexture = assetManager.getTexture(map.getTileTexturePath());
        TextureRegion tileRegion = new TextureRegion(tileTexture, 0, 0, map.getTileWidth(), map.getTileHeight());
        int[][] mapData = map.getMapData();

        // Draw tiles in correct isometric order (back to front)
        for (int sum = mapData.length + mapData[0].length - 2; sum >= 0; sum--) {
            for (int y = Math.min(mapData.length - 1, sum); y >= Math.max(0, sum - mapData[0].length + 1); y--) {
                int x = sum - y;
                if (x >= 0 && x < mapData[0].length) {
                    int tileType = mapData[y][x];

                    if (tileType != 0) {
                        float[] iso = toIsometric(x, y);
                        batch.draw(tileRegion, iso[0], iso[1], map.getTileWidth(), map.getTileHeight());
                    }
                }
            }
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

        // Get the tile texture
        Texture tileTexture = assetManager.getTexture(map.getTileTexturePath());

        // Define adjacent tiles in correct drawing order
        int[][] adjacentTiles = {
                {characterX - 1, characterY - 1}, // northwest
                {characterX, characterY - 1},     // north
                {characterX - 1, characterY},     // west
                {characterX, characterY},         // center
                {characterX + 1, characterY - 1}, // northeast
                {characterX - 1, characterY + 1}, // southwest
                {characterX + 1, characterY},     // east
                {characterX, characterY + 1},     // south
                {characterX + 1, characterY + 1}  // southeast
        };

        // Sort tiles by sum of coordinates (lower sum = draw first)
        java.util.Arrays.sort(adjacentTiles, (a, b) -> Integer.compare(a[0] + a[1], b[0] + b[1]));

        for (int[] tile : adjacentTiles) {
            int x = tile[0];
            int y = tile[1];

            // Check if tile is within bounds and walkable
            if (x >= 0 && x < walkableTiles[0].length &&
                    y >= 0 && y < walkableTiles.length &&
                    walkableTiles[y][x]) {
                float[] iso = toIsometric(x, y);
                batch.draw(tileTexture, iso[0], iso[1], map.getTileWidth(), map.getTileHeight());
            }
        }

        // Restore original color
        batch.setColor(originalColor);
    }

    public float[] toIsometric(float x, float y) {
        // More precise calculation for diamond tiles
        float isoX = (x - y) * (map.getTileWidth() / 2.0f) + offsetX;
        float isoY = (x + y) * (map.getTileHeight() / 2.0f) + offsetY;
        return new float[]{isoX, isoY};
    }

    public float getOffsetX() {
        return offsetX;
    }

    public float getOffsetY() {
        return offsetY;
    }
}