package ctu.game.isometric.model.game;

import ctu.game.isometric.model.entity.Character;
import java.util.Date;

public class GameSave {
    private Character character;
    private Date saveDate;
    private String saveName;
    private String wordFilePath;

    // Getters and setters
    public Character getCharacter() {
        return character;
    }

    public void setCharacter(Character character) {
        this.character = character;
    }

    public Date getSaveDate() {
        return saveDate;
    }

    public void setSaveDate(Date saveDate) {
        this.saveDate = saveDate;
    }

    public String getSaveName() {
        return saveName;
    }

    public void setSaveName(String saveName) {
        this.saveName = saveName;
    }

    public String getWordFilePath() {
        return wordFilePath;
    }

    public void setWordFilePath(String wordFilePath) {
        this.wordFilePath = wordFilePath;
    }
}