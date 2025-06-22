package ctu.game.isometric.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import ctu.game.isometric.model.entity.NPC;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NPCManager {
    final GameController gameController;
    private final Map<Integer, NPC> npcs = new HashMap<>();
    int[][] npcPositions;

    protected NPCManager(GameController gameController) {
        this.gameController = gameController;
        loadNPCs();
    }


    private void loadNPCs() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            // Configure ObjectMapper to match property names exactly
            mapper.setPropertyNamingStrategy(com.fasterxml.jackson.databind.PropertyNamingStrategy.LOWER_CAMEL_CASE);

            InputStream inputStream = getClass().getResourceAsStream("/game/npc.json");
            if (inputStream == null) {
                System.err.println("Could not find npc.json resource");
                return;
            }

            List<NPC> npcList = mapper.readValue(inputStream, new TypeReference<>() {
            });

            npcs.clear();
            npcPositions = new int[npcList.size() + 1][2];
            for (NPC npc : npcList) {
                npcs.put(npc.getNpcID(), npc);
                // Store NPC positions in a 2D array
                npcPositions[npc.getNpcID()][0] = npc.getXPosition();
                npcPositions[npc.getNpcID()][1] = npc.getYPosition();
            }


            System.out.println("Loaded " + npcs.size() + " NPCs successfully");
        } catch (IOException e) {
            System.err.println("Failed to load NPCs: " + e.getMessage());
        }
    }

    public int[][] getNpcPositions() {
        return npcPositions;
    }


    public Map<Integer, NPC> getNpcs() {
        return npcs;
    }

    public NPC getNPCById(int npcId) {
        return npcs.get(npcId);
    }


    public void dispose() {
        // Dispose of any resources if needed
        npcs.clear();
    }
}