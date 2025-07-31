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

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public class GameSaveController {
    private static final String SAVE_DIRECTORY = "saves/";
    private static final int MAX_SAVE_FILES = 5;
    private final ObjectMapper objectMapper;

    private static final String ENCRYPTION_KEY = "YourSecretKey123"; // Change this to something unique
    private static final String ALGORITHM = "AES";
    private String checksum;

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

    public boolean saveGame(Character character, String saveName, List<EventManager> eventManager) {
        try {

            // Create a serializable copy of the character
            Character saveCharacter = createSerializableCopy(character);
            // Create GameSave object
            GameSave gameSave = new GameSave();
            gameSave.setCharacter(saveCharacter);
            gameSave.setSaveDate(new Date());

            if (eventManager != null) {
                List<String> allCompletedEvents = new ArrayList<>();
                List<Integer> allDefeatedEnemies = new ArrayList<>();

                for (EventManager manager : eventManager) {
                    if (manager.getMapName().equals("board")) {
                        // Skip board events
                        continue;
                    }

                    if (manager != null) {
                        if (manager.getListIdCompletedEvents() != null) {
                            allCompletedEvents.addAll(manager.getListIdCompletedEvents());
                        }
                        if (manager.getListIdDefeatedEnemies() != null) {
                            allDefeatedEnemies.addAll(manager.getListIdDefeatedEnemies());
                        }
                    }
                }

                gameSave.setListIdCompletedEvents(allCompletedEvents);
                gameSave.setListIdDefeatedEnemies(allDefeatedEnemies);
            }

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

            // Only maintain save limit if creating a new save file
            if (existingSave == null) {
                maintainSaveLimit();
            }

            filename = (existingSave != null) ? existingSave.name() : saveName;
            if (filename.isEmpty()) {
                filename = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            }
            if (!filename.toLowerCase().endsWith(".json")) {
                filename += ".json";
            }

            // Convert to JSON without checksum first
            String jsonData = objectMapper.writeValueAsString(gameSave);

            // Generate checksum
            String checksum = generateChecksum(jsonData);

            // Add checksum to GameSave object
            setChecksum(checksum);

            // Regenerate JSON with checksum included
            jsonData = objectMapper.writeValueAsString(gameSave);

            // Encrypt data
            String encryptedData = encrypt(jsonData);

            // Save encrypted file
            FileHandle file = Gdx.files.local(SAVE_DIRECTORY + filename);
            file.writeString(encryptedData, false);

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

    private String encrypt(String data) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(ENCRYPTION_KEY.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            Gdx.app.error("GameSaveService", "Encryption error: " + e.getMessage());
            throw new RuntimeException("Encryption failed", e);
        }
    }

    private String decrypt(String encryptedData) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(ENCRYPTION_KEY.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            Gdx.app.error("GameSaveService", "Decryption error: " + e.getMessage());
            throw new RuntimeException("Decryption failed", e);
        }
    }

    private String generateChecksum(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            Gdx.app.error("GameSaveService", "Checksum generation error: " + e.getMessage());
            throw new RuntimeException("Checksum generation failed", e);
        }
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
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
        copy.setMaxHealth(original.getMaxHealth());
        copy.setMana(original.getMana());
        copy.setMaxMana(original.getMaxMana());
        copy.setGender(original.getGender());
        copy.setDamage((float) original.getDamage());
        copy.setMoveSpeed(original.getMoveSpeed());

        // Copy position
        copy.setGridX(original.getGridX());
        copy.setGridY(original.getGridY());
        copy.setTargetX(original.getTargetX());
        copy.setTargetY(original.getTargetY());
        copy.setDirection(original.getDirection());
        copy.setScore(original.getScore());
        copy.setDefend(original.getDefend());
        copy.setLevel(original.getLevel());
        copy.setExp(original.getExp());
        copy.setMana(original.getMana());
        copy.setMapName(original.getMapName());
        copy.setBonusRolls(original.getBonusRolls());

        copy.setCurrentObject(original.getCurrentObject());
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

        // Copy achievements if present
        if (original.getAchievements() != null) {
            copy.setAchievements(new HashSet<>(original.getAchievements()));
        }

        if (original.getAttempFlags() != null) {
            copy.setAttempFlags(new HashMap<>(original.getAttempFlags()));
        }

        if (original.getIsTutorials() != null) {
            copy.setIsTutorials(new HashMap<>(original.getIsTutorials()));
        }

        copy.setQuestTracker(original.getQuestTracker());

        copy.setWordFilePath(original.getWordFilePath());

        return copy;
    }

    public Set<String> loadLearnedWords(Character character, String fileName) {
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
            String fileContent = file.readString();
            String jsonData;

            // Check if file is encrypted (encrypted content starts with base64 characters)
            if (fileContent.startsWith("{")) {
                // Unencrypted JSON file - use directly
                jsonData = fileContent;
                Gdx.app.log("GameSaveService", "Loading unencrypted save (legacy format)");
            } else {
                // Encrypted file - decrypt first
                jsonData = decrypt(fileContent);
            }

            // Parse GameSave
            GameSave gameSave = objectMapper.readValue(jsonData, GameSave.class);

            // Check if checksum exists
            if (getChecksum() != null) {
                // Extract and verify checksum
                String storedChecksum = getChecksum();

                // Temporarily remove checksum for verification
                setChecksum(null);

                // Regenerate JSON without the checksum field
                String dataToVerify = objectMapper.writeValueAsString(gameSave);

                // Generate new checksum from data
                String calculatedChecksum = generateChecksum(dataToVerify);

                // Verify checksum
                if (!calculatedChecksum.equals(storedChecksum)) {
                    Gdx.app.error("GameSaveService", "Save file has been tampered with!");
                    return null;
                }

                // Restore checksum
                setChecksum(storedChecksum);
            } else {
                Gdx.app.log("GameSaveService", "Loading save without checksum (legacy format)");
            }

            return gameSave;

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
            String name = fileName.split("_")[0] + "_dictionary.json";
            FileHandle dic = Gdx.files.local("saves/dictionary/" + name);
            if (file.exists()) {
                file.delete();
                if (dic.exists()) {
                    dic.delete();
                }
                return true;
            }

            return false;
        } catch (Exception e) {
            System.err.println("Error deleting save file: " + e.getMessage());
            return false;
        }
    }
}