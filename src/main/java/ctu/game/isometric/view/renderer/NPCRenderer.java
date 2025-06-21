package ctu.game.isometric.view.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import ctu.game.isometric.model.entity.Character;
import ctu.game.isometric.model.entity.NPC;
import ctu.game.isometric.util.AnimationManager;

import java.util.Map;

import static ctu.game.isometric.util.FontGenerator.generateVietNameseFont;

public class NPCRenderer {
    private Map<Integer, NPC> npcs;
    private AnimationManager animationManager;
    private MapRenderer mapRenderer;
    private float stateTime;

    // Button textures
    private TextureRegion interactButton;
    private TextureRegion backstoryButton;
    private Character player;
    // Grid units

    // Button dimensions
    private final int BUTTON_SIZE = 20;
    private final int BUTTON_SPACING = 5;

    private BitmapFont font;

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

        this.font = new BitmapFont(Gdx.files.internal("fonts/IMFellEnglishSC-Regular.fnt"));
        this.font.getRegion().getTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

    }

    public void update(float delta) {
        stateTime += delta;
    }


    public void render(SpriteBatch batch) {
        String currentMapName = mapRenderer.getMap().getMapName();
        boolean isMainMap = currentMapName.equalsIgnoreCase("main");

        for (NPC npc : npcs.values()) {
            // Render if we're on main map (all NPCs) or if NPC belongs to current map
            if (isMainMap || currentMapName.equalsIgnoreCase(npc.getMapName())) {
                renderNPC(npc, batch);
                // Check if player is close enough to show action buttons
                if (isPlayerNearNPC(npc)) {
                    renderActionButtons(npc, batch);
                }
            }
        }
    }


    private boolean isPlayerNearNPC(NPC npc) {
        // Calculate manhattan distance between player and NPC
        float dx = Math.abs(player.getGridX() - npc.getXPosition());
        float dy = Math.abs(player.getGridY() - npc.getYPosition());
        return Math.max(dx, dy) <= 1;
    }

    private void renderActionButtons(NPC npc, SpriteBatch batch) {
        // Use player's position instead of NPC's position
        int gridX = (int) player.getGridX();
        int gridY = (int) player.getGridY();

        // Convert player's grid position to isometric screen position
        float[] screenPos = mapRenderer.toIsometric(gridX, gridY);
        float isoX = screenPos[0];
        float isoY = screenPos[1];

        // Position buttons above the player
        float buttonY = isoY + 80; // Adjust based on player height

        // Draw interact button
        batch.draw(interactButton,
                isoX - BUTTON_SIZE - BUTTON_SPACING + 40,
                buttonY - 36,
                BUTTON_SIZE,
                BUTTON_SIZE);

        // Draw backstory button
        batch.draw(backstoryButton,
                isoX + BUTTON_SPACING + 40,
                buttonY - 36,
                BUTTON_SIZE,
                BUTTON_SIZE);
    }

    private void renderNPC(NPC npc, SpriteBatch batch) {
        int gridX = npc.getXPosition();
        int gridY = npc.getYPosition();

        float[] screenPos = mapRenderer.toIsometric(gridX, gridY);
        float isoX = screenPos[0];
        float isoY = screenPos[1];

        TextureRegion currentFrame = animationManager.getNpcFrame(
                String.valueOf(npc.getNpcID()),
                npc.getBehaviorState(),
                stateTime
        );

        if (currentFrame != null) {
            batch.draw(currentFrame, isoX, isoY + 4, 64, 64);
        }

        // Draw NPC name above the sprite
        String name = npc.getNpcName();
        float nameX = isoX; // Centered
        float nameY = isoY + 58; // Above the sprite
        font.draw(batch, name, nameX, nameY);
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
        this.font.dispose();
        interactButton.getTexture().dispose();
        backstoryButton.getTexture().dispose();
    }


}