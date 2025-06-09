package ctu.game.isometric.view.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import ctu.game.isometric.model.entity.Character;
import ctu.game.isometric.model.entity.NPC;
import ctu.game.isometric.util.AnimationManager;

import java.util.Map;

public class NPCRenderer {
    private Map<Integer, NPC> npcs;
    private AnimationManager animationManager;
    private MapRenderer mapRenderer;
    private float stateTime;

    // Button textures
    private TextureRegion interactButton;
    private TextureRegion backstoryButton;
    private Character player;
    private int interactionDistance = 1; // Grid units

    // Button dimensions
    private final int BUTTON_SIZE = 20;
    private final int BUTTON_SPACING = 5;



    public NPCRenderer(Map<Integer, NPC> npcs, MapRenderer mapRenderer, Character player) {
        this.npcs = npcs;
        this.mapRenderer = mapRenderer;
        this.stateTime = 0f;
        this.animationManager = mapRenderer.getAnimationManager();
        this.player = player;

        // Load button textures
        Texture interactTexture = new Texture(Gdx.files.internal("ui/interact_button.png"));
        Texture backstoryTexture = new Texture(Gdx.files.internal("ui/backstory_button.png"));
        interactButton = new TextureRegion(interactTexture);
        backstoryButton = new TextureRegion(backstoryTexture);
    }

    public void update(float delta) {
        stateTime += delta;
    }

    public void render(SpriteBatch batch) {
        for (NPC npc : npcs.values()) {
            renderNPC(npc, batch);

            // Check if player is close enough to show action buttons
            if (isPlayerNearNPC(npc)) {
                renderActionButtons(npc, batch);
            }
        }
    }

    private boolean isPlayerNearNPC(NPC npc) {
        // Calculate manhattan distance between player and NPC
        float dx = Math.abs(player.getGridX() - npc.getXPosition());
        float dy = Math.abs(player.getGridY() - npc.getYPosition());
        return dx + dy <= interactionDistance;
    }

    private void renderActionButtons(NPC npc, SpriteBatch batch) {
        int gridX = npc.getXPosition();
        int gridY = npc.getYPosition();

        // Convert grid position to isometric screen position
        float[] screenPos = mapRenderer.toIsometric(gridX, gridY);
        float isoX = screenPos[0];
        float isoY = screenPos[1];

        // Position buttons above the NPC
        float buttonY = isoY + 80; // Adjust based on NPC height

        // Draw interact button
        batch.draw(interactButton,
                isoX - BUTTON_SIZE - BUTTON_SPACING + 40,
                buttonY - 36,
                BUTTON_SIZE,
                BUTTON_SIZE);

        // Draw backstory button
        batch.draw(backstoryButton,
                isoX + BUTTON_SPACING + 40,
                buttonY -36,
                BUTTON_SIZE,
                BUTTON_SIZE);
    }

    private void renderNPC(NPC npc, SpriteBatch batch) {
        int gridX = npc.getXPosition();
        int gridY = npc.getYPosition();

        // Convert grid position to isometric screen position
        float[] screenPos = mapRenderer.toIsometric(gridX, gridY);
        float isoX = screenPos[0];
        float isoY = screenPos[1];

        // Get animation frame based on NPC's behavior state
        TextureRegion currentFrame = animationManager.getNpcFrame(
                String.valueOf(npc.getNpcID()),
                npc.getBehaviorState(),
                stateTime
        );

        // Only draw if we have a valid frame
        if (currentFrame != null) {
            batch.draw(currentFrame,
                    isoX,
                    isoY + 4,
                    64,
                    64);
        }
    }


    // Method to preload animations for all NPCs
    public void loadNPCAnimations() {
        for (NPC npc : npcs.values()) {
            String npcId = String.valueOf(npc.getNpcID());
            String idlePath = "npc/" + npcId + "/Idle.png";
            String dialoguePath = "npc/" + npcId + "/Dialogue.png";

            // Load animations for this NPC
            animationManager.loadNpcAnimations(npcId, idlePath, dialoguePath);
        }
    }

    public void dispose() {
        animationManager.dispose();
        interactButton.getTexture().dispose();
        backstoryButton.getTexture().dispose();
    }

    // Set interaction distance (in grid cells)
    public void setInteractionDistance(int distance) {
        this.interactionDistance = distance;
    }


}