package ctu.game.isometric.view.renderer;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Matrix4;
import ctu.game.isometric.controller.GameController;
import ctu.game.isometric.model.entity.Character;
import ctu.game.isometric.model.entity.Gender;
import ctu.game.isometric.util.AssetManager;
import ctu.game.isometric.util.AnimationManager;

public class CharacterRenderer {
    private Character character;
    private AssetManager assetManager;
    private AnimationManager animationManager;
    private MapRenderer mapRenderer;
    private GameController gameController;

    private Texture darkBackgroundTexture;
    private SpriteBatch darkBackgroundBatch = new SpriteBatch();

    private static final float OFFSET_Y = 17.5f;
    private static final float OFFSET_X = -3f;
    private static final float OFFSET_PLAYER_X = 10;
    private static final float OFFSET_PLAYER_Y = -5;

    public CharacterRenderer(Character character, AssetManager assetManager, MapRenderer mapRenderer) {
        this.character = character;
        this.assetManager = assetManager;
        this.mapRenderer = mapRenderer;
        this.animationManager = assetManager.getAnimationManager();

        Gender characterGender = character.getGender();
        if (characterGender == null || characterGender.equals(Gender.MALE)) {
            this.animationManager.loadKnockedDownAnimation("characters/knocked_down_male.png");
            this.animationManager.loadCharacterAnimations("characters/idle.png", "characters/walk.png");
        } else {
            this.animationManager.loadKnockedDownAnimation("characters/knocked_down_female.png");
            this.animationManager.loadCharacterAnimations("characters/female_idle.png", "characters/female_walk.png");
        }
        darkBackgroundMatrix = new Matrix4().idt().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        darkBackgroundMatrix = new Matrix4().idt().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        darkBackgroundMatrix = new Matrix4().idt().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        darkBackgroundTexture = assetManager.loadTexture("dark", "textures/darkness2.png");
    }

    Matrix4 darkBackgroundMatrix;

    public void renderDarkBackgroundWithMinimap(SpriteBatch batch) {
        if (darkBackgroundTexture != null) {
            // Save the current batch transformation matrix
            Matrix4 originalMatrix = batch.getProjectionMatrix().cpy();

            // Set identity matrix for screen-space rendering
            batch.setProjectionMatrix(darkBackgroundMatrix);

            // Draw the dark background in screen coordinates
            batch.draw(darkBackgroundTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

            // Render the minimap mask on top of the dark background
            mapRenderer.renderMinimap(batch);

            // Restore the original transformation matrix
            batch.setProjectionMatrix(originalMatrix);
        }
    }


    public void render(SpriteBatch batch) {
        float gridX = character.getGridX();
        float gridY = character.getGridY();

        float[] screenPos = mapRenderer.toIsometric(gridX, gridY);

        float isoX = screenPos[0];
        float isoY = screenPos[1];

        if (mapRenderer.getMap().getMapName().equals("board")) {
            isoY += OFFSET_Y;
            isoX += OFFSET_X;
        }

        String direction = translateDirection(character.getDirection());

        if (direction.equals("knocked_down")) {
            isoY += 14;
            isoX += 14;
        }

        TextureRegion currentFrame = animationManager.getCharacterFrame(
                direction,
                character.isMoving(),
                character.getAnimationTime()
        );


        batch.draw(currentFrame, isoX + OFFSET_PLAYER_X, isoY + OFFSET_PLAYER_Y);
        if (mapRenderer.isRenderDarknessWithLight()) {
            renderDarkBackgroundWithMinimap(batch);
        }
    }

    public GameController getGameController() {
        return gameController;
    }

    public void setGameController(GameController gameController) {
        this.gameController = gameController;
    }

    private String translateDirection(String direction) {
        switch (direction) {
            case "up":
                return "up";
            case "down":
                return "down";
            case "left":
                return "left_down";
            case "right":
                return "right_down";
            case "left_up":
                return "left_up";
            case "right_up":
                return "right_up";
            case "left_down":
                return "left_down";
            case "knocked_down":
                return "knocked_down";
            default:
                return "right_down";
        }
    }

    public void dispose() {
        animationManager.dispose();
        if (darkBackgroundBatch != null) {
            darkBackgroundBatch.dispose();
        }
    }
}