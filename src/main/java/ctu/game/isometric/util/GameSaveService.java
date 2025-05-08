package ctu.game.isometric.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import ctu.game.isometric.model.entity.Character;
import ctu.game.isometric.model.game.GameSave;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GameSaveService {
    private static final String SAVE_DIRECTORY = "saves/";
    private final ObjectMapper objectMapper;

    public GameSaveService() {
        // Configure ObjectMapper
        this.objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        // Configure visibility to use fields directly
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        // Create save directory if it doesn't exist
        FileHandle dir = Gdx.files.local(SAVE_DIRECTORY);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public boolean saveGame(Character character, String saveName) {
        try {
            // Create a clean character copy without LibGDX objects
            Character saveCharacter = createSerializableCopy(character);

            // Create and populate GameSave
            GameSave gameSave = new GameSave();
            gameSave.setCharacter(saveCharacter);
            gameSave.setSaveDate(new Date());

            // Generate filename
            String filename = saveName.isEmpty() ?
                    new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()) :
                    saveName;

            if (!filename.toLowerCase().endsWith(".json")) {
                filename += ".json";
            }

            FileHandle file = Gdx.files.local(SAVE_DIRECTORY + filename);
            file.writeString(objectMapper.writeValueAsString(gameSave), false);

            Gdx.app.log("GameSaveService", "Game saved to: " + file.path());
            return true;
        } catch (Exception e) {
            Gdx.app.error("GameSaveService", "Error saving game: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private Character createSerializableCopy(Character original) {
        Character copy = new Character();

        // Copy basic properties
        copy.setName(original.getName());
        copy.setHealth(original.getHealth());
        copy.setGender(original.getGender());
        copy.setDamage((float)original.getDamage());
        copy.setMoveSpeed(original.getMoveSpeed());

        // Copy position
        copy.setGridX(original.getGridX());
        copy.setGridY(original.getGridY());
        copy.setTargetX(original.getTargetX());
        copy.setTargetY(original.getTargetY());
        copy.setDirection(original.getDirection());

        // Copy any other essential character data
        // (Items, stats, quests, etc. - add as needed)

        if (original.getFlags() != null) {
            copy.setFlags(new ArrayList<>(original.getFlags()));
        }

        // Copy quests if present
        if (original.getQuests() != null) {
            copy.setQuests(new ArrayList<>(original.getQuests()));
        }

        // Copy items if present
        if (original.getItems() != null) {
            copy.setItems(new HashMap<>(original.getItems()));
        }

        // Copy status effects if present
        if (original.getStatus() != null) {
            Map<String, List<String>> statusCopy = new HashMap<>();
            for (Map.Entry<String, List<String>> entry : original.getStatus().entrySet()) {
                statusCopy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
            copy.setStatus(statusCopy);
        }


        return copy;
    }

    public GameSave loadGame(String filename) {
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }
        try {
            FileHandle file = Gdx.files.local(SAVE_DIRECTORY + filename);
            String json = file.readString();
            return objectMapper.readValue(json, GameSave.class);
        } catch (Exception e) {
            Gdx.app.error("GameSaveService", "Error loading game: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public String[] getSaveFiles() {
        FileHandle dir = Gdx.files.local(SAVE_DIRECTORY);
        FileHandle[] files = dir.list(".json");
        String[] filenames = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            filenames[i] = files[i].name();
        }
        return filenames;
    }
}