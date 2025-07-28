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
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static List<NPC> ORIGINAL_NPC_DATA = null;

    final GameController gameController;
    private Map<Integer, NPC> npcs = new HashMap<>();
    private int[][] npcPositions;
    private final int[][] originalNpcPositions;
    private final Map<Integer, NPC> originalNpcs;

    static {
        MAPPER.setPropertyNamingStrategy(com.fasterxml.jackson.databind.PropertyNamingStrategy.LOWER_CAMEL_CASE);
    }

    public Map<Integer, NPC> getOriginalNpcs() {
        return originalNpcs;
    }

    protected NPCManager(GameController gameController) {
        this.gameController = gameController;

        // Load JSON data only once per class
        if (ORIGINAL_NPC_DATA == null) {
            loadNPCDataFromJson();
        }

        // Initialize original data
        this.originalNpcs = new HashMap<>();
        this.originalNpcPositions = new int[ORIGINAL_NPC_DATA.size() + 1][2];

        // Populate original data
        for (NPC npc : ORIGINAL_NPC_DATA) {
            try {
                // Deep copy NPC using JSON serialization
                String npcJson = MAPPER.writeValueAsString(npc);
                NPC npcCopy = MAPPER.readValue(npcJson, NPC.class);
                originalNpcs.put(npc.getNpcID(), npcCopy);
                originalNpcPositions[npc.getNpcID()][0] = npc.getXPosition();
                originalNpcPositions[npc.getNpcID()][1] = npc.getYPosition();
            } catch (IOException e) {
                System.err.println("Failed to copy NPC data: " + e.getMessage());
            }
        }

        // Initialize current NPCs from original data
        resetNPCManager();
    }

    public void removeNPC(int npcId) {
        NPC removedNpc = npcs.remove(npcId);
        if (removedNpc != null) {
            npcPositions[npcId][0] = 0;
            npcPositions[npcId][1] = 0;
            System.out.println("Removed NPC: " + removedNpc.getMapName());
        } else {
            System.err.println("NPC with ID " + npcId + " not found");
        }
    }

    public void resetNPCManager() {
        npcs.clear();
        npcPositions = new int[originalNpcPositions.length][2];

        // Deep copy from original data
        for (Map.Entry<Integer, NPC> entry : originalNpcs.entrySet()) {
            try {
                String npcJson = MAPPER.writeValueAsString(entry.getValue());
                NPC npcCopy = MAPPER.readValue(npcJson, NPC.class);
                npcs.put(entry.getKey(), npcCopy);
            } catch (IOException e) {
                System.err.println("Failed to reset NPC: " + e.getMessage());
            }
        }
        for (int i = 0; i < originalNpcPositions.length; i++) {
            System.arraycopy(originalNpcPositions[i], 0, npcPositions[i], 0, 2);
        }

        System.out.println("NPC Manager has been reset and NPCs reloaded.");
    }

    public void changeNPCPosition(int npcId, int x, int y) {
        NPC npc = npcs.get(npcId);
        if (npc != null) {
            npc.setxPosition(x);
            npc.setyPosition(y);
            npcPositions[npcId][0] = x;
            npcPositions[npcId][1] = y;
        } else {
            System.err.println("NPC with ID " + npcId + " not found");
        }
    }

    private static void loadNPCDataFromJson() {
        try {
            InputStream inputStream = NPCManager.class.getResourceAsStream("/game/npc.json");
            if (inputStream == null) {
                System.err.println("Could not find npc.json resource");
                return;
            }

            ORIGINAL_NPC_DATA = MAPPER.readValue(inputStream, new TypeReference<List<NPC>>() {});
            System.out.println("Loaded " + ORIGINAL_NPC_DATA.size() + " NPCs from JSON");

        } catch (IOException e) {
            System.err.println("Failed to load NPCs from JSON: " + e.getMessage());
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
        npcs.clear();
    }
}