package ctu.game.isometric.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import ctu.game.isometric.model.entity.Character;
import ctu.game.isometric.model.game.GameSave;

import java.text.SimpleDateFormat;
import java.util.*;

public class GameSaveController {
    private static final String SAVE_DIRECTORY = "saves/";
    private static final int MAX_SAVE_FILES = 5;
    private final ObjectMapper objectMapper;

    public GameSaveController() {
        // Configure ObjectMapper
        this.objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        // Configure visibility to use fields directly
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        // Create save directory if it doesn't exist
        FileHandle dir = Gdx.files.local("saves/dictionary/");
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }





    public boolean saveGame(Character character, String saveName) {
        try {
            maintainSaveLimit();

            // Create a serializable copy of the character
            Character saveCharacter = createSerializableCopy(character);

            // Create GameSave object
            GameSave gameSave = new GameSave();
            gameSave.setCharacter(saveCharacter);
            gameSave.setSaveDate(new Date());
            gameSave.setWordFilePath("saves/dictionary/" + character.getWordFilePath() + ".json");

            // Generate file name
            String baseName = character.getName();
            String filename;

            FileHandle existingSave = null;
            FileHandle dir = Gdx.files.local(SAVE_DIRECTORY);

            // Find existing save file with the same character name
            for (FileHandle file : dir.list()) {
                if (file.name().startsWith(baseName + "_") && file.name().endsWith(".json")) {
                    existingSave = file;
                    break;
                }
            }

            filename = (existingSave != null) ? existingSave.name() : saveName;
            if (filename.isEmpty()) {
                filename = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            }
            if (!filename.toLowerCase().endsWith(".json")) {
                filename += ".json";
            }

            // Save JSON file
            FileHandle file = Gdx.files.local(SAVE_DIRECTORY + filename);
            file.writeString(objectMapper.writeValueAsString(gameSave), false);

            // Save learned words if available
            saveLearnedWords(character);

            // Log success
            Gdx.app.log("GameSaveService", "Game saved to: " + file.path());
            return true;

        } catch (Exception e) {
            Gdx.app.error("GameSaveService", "Error saving game: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void saveLearnedWords(Character character) {
        try {
            if (character.getWordFilePath() == null) {
                Gdx.app.error("GameSaveService", "Word file path is null. Skipping save.");
                return;
            }

            Set<String> combinedWords = new HashSet<>();

            if (character.getLearnedWords() != null) {
                combinedWords.addAll(character.getLearnedWords());
            }
            if (character.getNewlearneWords() != null) {
                combinedWords.addAll(character.getNewlearneWords());
            }

            FileHandle file = Gdx.files.local("saves/dictionary/" + character.getWordFilePath() + ".json");
            file.writeString(objectMapper.writeValueAsString(combinedWords), false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void maintainSaveLimit() {
        FileHandle dir = Gdx.files.local(SAVE_DIRECTORY);
        FileHandle[] files = dir.list(".json");

        if (files.length >= MAX_SAVE_FILES) {
            // Sort files by last modified time (oldest first)
            List<FileHandle> sortedFiles = new ArrayList<>();
            for (FileHandle file : files) {
                sortedFiles.add(file);
            }

            sortedFiles.sort(Comparator.comparingLong(FileHandle::lastModified));

            // Delete oldest files until we're under the limit
            int filesToDelete = sortedFiles.size() - MAX_SAVE_FILES + 1; // +1 for the new save
            for (int i = 0; i < filesToDelete; i++) {
                FileHandle oldestFile = sortedFiles.get(i);
                Gdx.app.log("GameSaveService", "Deleting old save: " + oldestFile.name());
                oldestFile.delete();
            }
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
        copy.setScore(original.getScore());
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

        copy.setWordFilePath(original.getWordFilePath());


        return copy;
    }
    public Set<String> loadLearnedWords(Character character,String fileName) {
        try {
            if (character.getWordFilePath() == null) {
                Gdx.app.error("GameSaveService", "Word file path is null. Cannot load dictionary.");
                return new HashSet<>();
            }

            FileHandle file = Gdx.files.local(fileName);

            if (!file.exists()) {
                Gdx.app.log("GameSaveService", "Dictionary file does not exist: " + file.path());
                return new HashSet<>();
            }

            String json = file.readString();
            Set<String> learnedWords = objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(Set.class, String.class));

            Gdx.app.log("GameSaveService", "Loaded " + learnedWords.size() + " words from dictionary");
            return learnedWords;
        } catch (Exception e) {
            Gdx.app.error("GameSaveService", "Error loading dictionary: " + e.getMessage());
            e.printStackTrace();
            return new HashSet<>();
        }
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

    public boolean deleteSave(String fileName) {
        try {
            FileHandle file = Gdx.files.local("saves/" + fileName);
            if (file.exists()) {
                file.delete();
                return true;
            }
            return false;
        } catch (Exception e) {
            System.err.println("Error deleting save file: " + e.getMessage());
            return false;
        }
    }
}