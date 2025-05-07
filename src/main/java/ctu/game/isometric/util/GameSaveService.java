package ctu.game.isometric.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import ctu.game.isometric.model.entity.Character;
import ctu.game.isometric.model.game.GameSave;

import java.text.SimpleDateFormat;
import java.util.Date;

public class GameSaveService {
    private static final String SAVE_DIRECTORY = "saves/";
    private final ObjectMapper objectMapper;

    public GameSaveService() {
        this.objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        FileHandle dir = Gdx.files.local(SAVE_DIRECTORY);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /**
     * Save game to the specified filename or generate a timestamp-based name
     */
    public boolean saveGame(Character character, String saveName) {
        try {
            GameSave gameSave = new GameSave();
            gameSave.setCharacter(character);
            gameSave.setSaveDate(new Date());

            String filename = saveName.isEmpty() ?
                new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()) :
                saveName;

            // Ensure filename has .json extension
            if (!filename.toLowerCase().endsWith(".json")) {
                filename += ".json";
            }

            FileHandle file = Gdx.files.local(SAVE_DIRECTORY + filename);
            file.writeString(objectMapper.writeValueAsString(gameSave), false);

            Gdx.app.log("GameSaveService", "Game saved to: " + file.path());
            return true;
        } catch (Exception e) {
            Gdx.app.error("GameSaveService", "Error saving game: " + e.getMessage());
            return false;
        }
    }

    /**
     * List all available save files
     */
    public String[] getSaveFiles() {
        FileHandle dir = Gdx.files.local(SAVE_DIRECTORY);
        FileHandle[] files = dir.list(".json");
        String[] filenames = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            filenames[i] = files[i].name();
        }
        return filenames;
    }

    /**
     * Load a game from the specified filename
     */
    public GameSave loadGame(String filename) {
        try {
            FileHandle file = Gdx.files.local(SAVE_DIRECTORY + filename);
            String json = file.readString();
            return objectMapper.readValue(json, GameSave.class);
        } catch (Exception e) {
            Gdx.app.error("GameSaveService", "Error loading game: " + e.getMessage());
            return null;
        }
    }
}