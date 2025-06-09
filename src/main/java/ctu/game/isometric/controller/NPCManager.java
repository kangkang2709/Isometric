package ctu.game.isometric.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import ctu.game.isometric.model.entity.NPC;
import ctu.game.isometric.model.game.Quest;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NPCManager {
    private GameController gameController;
    private Map<Integer, NPC> npcs = new HashMap<>();

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

            List<NPC> npcList = mapper.readValue(inputStream, new TypeReference<List<NPC>>() {});

            npcs.clear();
            for (NPC npc : npcList) {
                npcs.put(npc.getNpcID(), npc);
            }

            System.out.println("Loaded " + npcs.size() + " NPCs successfully");
        } catch (IOException e) {
            System.err.println("Failed to load NPCs: " + e.getMessage());
            e.printStackTrace();
        }
    }
}